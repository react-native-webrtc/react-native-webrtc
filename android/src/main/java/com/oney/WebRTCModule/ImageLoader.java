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

  private static class Cache {
    private final HashMap<String, VideoFrame.Buffer> map;
    private final ArrayList<String> order;
    private final int capacity;
    
    public Cache(final int capacity) {
      this.map = new HashMap<>(capacity);
      this.order = new ArrayList<>(capacity);
      this.capacity = capacity;
    }

    private synchronized void shiftKeyToEnd(final String key) {
      order.remove(key);
      order.add(key);
    }

    private synchronized boolean maybeHandleExistingItem(final String key, final VideoFrame.Buffer buffer) {
      VideoFrame.Buffer current = map.get(key);
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
      String oldestKey = order.remove(0);
      buffer.retain();
      if (oldestKey != null) {
        VideoFrame.Buffer oldestBuffer = map.remove(oldestKey);
        if (oldestBuffer != null) {
          oldestBuffer.release();
        }
      }
      map.put(key, buffer);
      order.add(key);
    }

    private synchronized void handleAdd(final String key, final VideoFrame.Buffer buffer) {
      buffer.retain();
      map.put(key, buffer);
    }

    public synchronized void store(final String key, final VideoFrame.Buffer buffer) {
      if (buffer == null) {
        return;
      }
      if (maybeHandleExistingItem(key, buffer)) {
        return;
      }
      int currentSize = map.size();
      if (currentSize == capacity) {
        handleKickAndAdd(key, buffer);
      } else {
        handleAdd(key, buffer);
      }
    }

    public synchronized @Nullable VideoFrame.Buffer retrieve(final String key) {
      return map.get(key);
    }
  }

  private static final String RESOURCE_SCHEME = "res";
  private static final String ANDROID_RESOURCE_SCHEME = "android.resource";
  private static final String HTTP_SCHEME = "http";
  private static final int CACHE_CAPACITY = 5;

  private static final Cache cache = new Cache(CACHE_CAPACITY);

  private final Context context;
  private final String asset;
  private final LoadSuccess onSuccess;
  private final LoadFail onFail;

  private boolean isReadyToLoad;

  private @Nullable InputStream stream;
  private @Nullable Bitmap bitmap;

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

  private void resolved() {
    this.isReadyToLoad = true;
    if (stream != null) {
      try {
        stream.close();
      } catch (Exception e) {
        // no-op
      }
    }
  }

  private void fail(final String message) {
    resolved();
    ThreadUtils.runOnExecutor(() -> {
      onFail.fail(message);
    });
  }

  private void success(VideoFrame.Buffer image) {
    resolved();
    ThreadUtils.runOnExecutor(() -> {
      onSuccess.success(image);
    });
  }

  private void streamToFrame() {
    if (stream == null) {
      fail("no asset stream");
      return;
    }
    bitmap = BitmapUtils.bitmapFromStream(stream);
    if (bitmap == null) {
      fail("could not convert stream to bitmap");
      return;
    }
    // TODO: move into bufferFromBitmap, log it
    if (!BitmapUtils.hasEnoughMemoryToConvert(bitmap)) {
      fail("not enough memory to handle bitmap");
      return;
    }
    VideoFrame.Buffer buffer = BitmapUtils.bufferFromBitmap(bitmap);
    if (buffer == null) {
      fail("could not convert bitmap to buffer");
      return;
    }
    bitmap.recycle();
    cache.store(asset, buffer);
    success(buffer);
  }

  private void loadHttp(final Uri uri) {
    Executors.newSingleThreadExecutor().execute(() -> {
      try {
        URL url = new URL(uri.toString());
        stream = url.openStream();
        streamToFrame();
      } catch (Exception e) {
        fail("failed to open http(s) asset");
      }
    });
  }

  private void loadResrouce(final Uri uri) {
    Executors.newSingleThreadExecutor().execute(() -> {
      int id = AssetUtils.getAssetResourceId(context, uri);
      try {
        stream = context.getResources().openRawResource(id);
        streamToFrame();
      } catch (Exception e) {
        fail("failed to open resource asset");
      }
    });
  }

  public synchronized void load() {
    if (!this.isReadyToLoad) {
      return;
    }
    this.isReadyToLoad = false;

    VideoFrame.Buffer cached = cache.retrieve(asset);
    if (cached != null) {
      success(cached);
      return;
    }
    Uri uri = AssetUtils.assetStringToUri(context, asset);
    String scheme = AssetUtils.getAssetUriScheme(uri);
    if (scheme.startsWith(HTTP_SCHEME)) {
      loadHttp(uri);
    } else if (scheme.equals(RESOURCE_SCHEME) || scheme.equals(ANDROID_RESOURCE_SCHEME)) {
      loadResrouce(uri);
    } else {
      fail("ussuported asset uri");
    }
  }
}