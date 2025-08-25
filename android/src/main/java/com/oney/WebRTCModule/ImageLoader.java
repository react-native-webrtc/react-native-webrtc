package com.oney.WebRTCModule;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import androidx.annotation.Nullable;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.HashMap;
import org.webrtc.VideoFrame;

public class ImageLoader {
  @FunctionalInterface
  public interface LoadSuccess {
    void success(VideoFrame.Buffer image);
  }
  @FunctionalInterface
  public interface LoadFail {
    void fail(String reason);
  }
  @FunctionalInterface
  public interface ThrowingSupplier<T> {
      T get() throws Exception;
  }

  private static class Cache {
    private final HashMap<String, VideoFrame.Buffer> map;
    private final ArrayList<String> order;
    private final int capacity;
    
    public Cache(final int capacity) {
      if (capacity <= 0) {
        throw new IllegalArgumentException("capacity must be 1 or greater");
      }
      this.map = new HashMap<>(capacity);
      this.order = new ArrayList<>(capacity);
      this.capacity = capacity;
    }

    private synchronized void shiftKeyToEnd(final String key) {
      order.remove(key);
      order.add(key);
    }

    private synchronized boolean maybeHandleExistingItem(final String key, final VideoFrame.Buffer buffer) {
      final VideoFrame.Buffer current = map.get(key);
      if (current == null) {
        return false;
      }
      shiftKeyToEnd(key);
      if (current != buffer) {
        buffer.retain();
        current.release();
        map.put(key, buffer);
      }
      return true;
    }

    private synchronized void handleKickAndAdd(final String key, final VideoFrame.Buffer buffer) {
      final String oldestKey = order.remove(0);
      buffer.retain();
      if (oldestKey != null) {
        final VideoFrame.Buffer oldestBuffer = map.remove(oldestKey);
        if (oldestBuffer != null) {
          oldestBuffer.release();
        }
      }
      order.add(key);
      map.put(key, buffer);
    }

    private synchronized void handleAdd(final String key, final VideoFrame.Buffer buffer) {
      buffer.retain();
      order.add(key);
      map.put(key, buffer);
    }

    public synchronized void store(final String key, final VideoFrame.Buffer buffer) {
      if (buffer == null) {
        return;
      }
      if (maybeHandleExistingItem(key, buffer)) {
        return;
      }
      final int currentSize = map.size();
      if (currentSize == capacity) {
        handleKickAndAdd(key, buffer);
      } else {
        handleAdd(key, buffer);
      }
    }

    public synchronized @Nullable VideoFrame.Buffer retrieve(final String key) {
      VideoFrame.Buffer buffer = map.get(key);
      if (buffer != null) {
        buffer.retain();
      }
      return buffer;
    }
  }

  private static class Util {
    private static void closeStream(final @Nullable InputStream stream) {
      if (stream != null) {
        try {
          stream.close();
        } catch (Exception e) {
          // no-op
        }
      }
    }
    private static void recycleBitmap(final @Nullable Bitmap bitmap) {
      if (bitmap != null && !bitmap.isRecycled()) {
        try {
          bitmap.recycle();
        } catch (Exception e) {
          // no-op
        }
      }
    }
    private static @Nullable VideoFrame.Buffer getFrameBufferAndRecycleBitmap(final Bitmap bitmap) {
      VideoFrame.Buffer buffer = null;
      try {
        buffer = BitmapUtils.bufferFromBitmap(bitmap);
      } catch (Exception e) {
        e.printStackTrace();
      } finally {
        Util.recycleBitmap(bitmap);
        return buffer;
      }
    }
    private static @Nullable Bitmap getBitmapAndCloseStream(final ThrowingSupplier<InputStream> streamSupplier) {
      InputStream stream = null;
      Bitmap bitmap = null;
      try {
        stream = streamSupplier.get();
        bitmap = BitmapUtils.bitmapFromStream(stream);
      } catch (Exception e) {
        e.printStackTrace();
      } finally {
        Util.closeStream(stream);
        return bitmap;
      }
    }
  }

  private static final String RESOURCE_SCHEME = "res";
  private static final String ANDROID_RESOURCE_SCHEME = "android.resource";
  private static final String HTTP_SCHEME = "http";
  private static final int CACHE_CAPACITY = 3;

  private static final Cache cache = new Cache(CACHE_CAPACITY);

  private final Context context;
  private final String asset;
  private final LoadSuccess onSuccess;
  private final LoadFail onFail;

  private boolean isReadyToLoad;

  public ImageLoader(final Context context, final String asset, final LoadSuccess onSuccess, final LoadFail onFail) {
    if (context == null) {
      throw new IllegalArgumentException("context cannot be null");
    }
    if (asset == null) {
      throw new IllegalArgumentException("asset cannot be null");
    }
    if (onSuccess == null) {
      throw new IllegalArgumentException("onSuccess cannot be null");
    }
    if (onFail == null) {
      throw new IllegalArgumentException("onFail cannot be null");
    }
    this.context = context;
    this.asset = asset;
    this.onSuccess = onSuccess;
    this.onFail = onFail;
    this.isReadyToLoad = true;
  }

  private synchronized void fail(final String message) {
    this.isReadyToLoad = true;
    ThreadUtils.runOnExecutor(() -> {
      onFail.fail(message);
    });
  }

  private synchronized void success(VideoFrame.Buffer image) {
    this.isReadyToLoad = true;
    ThreadUtils.runOnExecutor(() -> {
      onSuccess.success(image);
      image.release();
    });
  }

  private void loadAsset(final ThrowingSupplier<InputStream> streamSupplier, final String streamSupplierFailureMessage) {
    Executors.newSingleThreadExecutor().execute(() -> {
      final Bitmap bitmap;
      final VideoFrame.Buffer buffer;
      bitmap = Util.getBitmapAndCloseStream(streamSupplier);
      if (bitmap == null) {
        fail(streamSupplierFailureMessage);
        return;
      }
      if (!BitmapUtils.hasEnoughMemoryToConvert(bitmap)) {
        Util.recycleBitmap(bitmap);
        fail("not enough memory to handle asset");
        return;
      }
      buffer = Util.getFrameBufferAndRecycleBitmap(bitmap);
      if (buffer == null) {
        fail("could not convert bitmap to buffer");
        return;
      }
      cache.store(asset, buffer);
      success(buffer);
    });
  }

  private void loadHttp(final Uri uri) {
    loadAsset(() -> {
      final URL url = new URL(uri.toString());
      return url.openStream();
    }, "failed to open http(s) asset as bitmap");
  }

  private void loadResrouce(final Uri uri) {
    loadAsset(() -> {
      final int id = AssetUtils.getAssetResourceId(context, uri);
      return context.getResources().openRawResource(id);
    }, "failed to open resource asset as bitmap");
  }

  private void doLoad() {
    final Uri uri = AssetUtils.assetStringToUri(context, asset);
    final String scheme = AssetUtils.getAssetUriScheme(uri);
    if (scheme.startsWith(HTTP_SCHEME)) {
      loadHttp(uri);
    } else if (scheme.equals(RESOURCE_SCHEME) || scheme.equals(ANDROID_RESOURCE_SCHEME)) {
      loadResrouce(uri);
    } else {
      fail("unsupported asset uri");
    }
  }

  private boolean maybeResolveCached() {
    final VideoFrame.Buffer cached = cache.retrieve(asset);
    if (cached != null) {
      success(cached);
      return true;
    }
    return false;
  }

  public synchronized void load() {
    if (!this.isReadyToLoad) {
      return;
    }
    this.isReadyToLoad = false;
    if (!maybeResolveCached()) {
      doLoad();
    }
  }
}
