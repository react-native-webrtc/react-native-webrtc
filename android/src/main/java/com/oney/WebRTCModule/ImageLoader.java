package com.oney.WebRTCModule;

import android.content.res.AssetFileDescriptor;
import android.content.Context;
import android.net.Uri;
import androidx.annotation.Nullable;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.HashMap;
import org.webrtc.JavaI420Buffer;
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

  public static class Asset {
    public final String src;
    public final int width;
    public final int height;
    public final int yStride;
    public final int ySize;
    public final int uvStride;
    public final int uvSize;
    public final boolean cache;
    Asset(final String src, final int width, final int height, final boolean cache) {
      this.src = src;
      this.width = width;
      this.height = height;
      this.yStride = width;
      this.ySize = yStride * height;
      this.uvStride = (width + 1) / 2;
      this.uvSize = uvStride * ((height + 1) / 2);
      this.cache = cache;
    }
  }

  private static final String HTTP_SCHEME = "http";
  private static final int CACHE_CAPACITY = 3;

  private static final Cache cache = new Cache(CACHE_CAPACITY);

  private final Context context;
  private final Asset asset;
  private final LoadSuccess onSuccess;
  private final LoadFail onFail;

  private boolean isReadyToLoad;

  public ImageLoader(final Context context, final Asset asset, final LoadSuccess onSuccess, final LoadFail onFail) {
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

  public static ImageLoader.Asset makeAsset(final String src, final int width, final int height, final boolean cache) {
    return new ImageLoader.Asset(src, width, height, cache);
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

  private void loadHttp(final Uri uri) {
    ByteBuffer yPlane = ByteBuffer.allocateDirect(asset.ySize);
    ByteBuffer uPlane = ByteBuffer.allocateDirect(asset.uvSize);
    ByteBuffer vPlane = ByteBuffer.allocateDirect(asset.uvSize);

    try (InputStream stream = new URL(uri.toString()).openStream();
          ReadableByteChannel channel = Channels.newChannel(stream)) {
      readFully(channel, yPlane, asset.ySize);
      yPlane.flip();
      readFully(channel, uPlane, asset.uvSize);
      uPlane.flip();
      readFully(channel, vPlane, asset.uvSize);
      vPlane.flip();
    } catch (Exception e) {
      e.printStackTrace();
      fail("could not load http(s) asset");
      return;
    }

    VideoFrame.Buffer buffer = JavaI420Buffer.wrap(asset.width, asset.height, yPlane, asset.yStride, uPlane, asset.uvStride, vPlane, asset.uvStride, null);

    if (asset.cache) {
      cache.store(asset.src, buffer);
    }
  
    success(buffer);
  }

  private static void readFully(ReadableByteChannel channel, ByteBuffer buffer, int size) throws IOException {
    int remaining = size;
    while (remaining > 0) {
      int bytesRead = channel.read(buffer);
      if (bytesRead == -1) {
        throw new IOException("Unexpected end of stream");
      }
      remaining -= bytesRead;
    }
  }

  private void loadResource(final int resId) {
    ByteBuffer yPlane, uPlane, vPlane;

    try (AssetFileDescriptor afd = context.getResources().openRawResourceFd(resId)) {
      FileDescriptor fd = afd.getFileDescriptor();
      long startOffset = afd.getStartOffset();
      long totalSize = afd.getLength();

      try (FileChannel channel = new FileInputStream(fd).getChannel()) {
        MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, startOffset, totalSize);
        yPlane = (ByteBuffer) buffer.slice().limit(asset.ySize);
        buffer.position(asset.ySize);
        uPlane = (ByteBuffer) buffer.slice().limit(asset.uvSize);
        buffer.position(asset.ySize + asset.uvSize);
        vPlane = (ByteBuffer) buffer.slice().limit(asset.uvSize);
      }
    } catch (Exception e) {
      e.printStackTrace();
      fail("could not load resource");
      return;
    }

    VideoFrame.Buffer buffer = JavaI420Buffer.wrap(asset.width, asset.height, yPlane, asset.yStride, uPlane, asset.uvStride, vPlane, asset.uvStride, null);

    if (asset.cache) {
      cache.store(asset.src, buffer);
    }

    success(buffer);
  }

  private void doLoad() {
    final Uri uri = AssetUtils.assetStringToUri(context, asset.src);
    final String scheme = AssetUtils.getAssetUriScheme(uri);
    final int resId = context.getResources().getIdentifier(asset.src, "raw", context.getPackageName());
    if (scheme.startsWith(HTTP_SCHEME)) {
      Executors.newSingleThreadExecutor().execute(() -> { loadHttp(uri); });
    } else if (resId > 0) {
      Executors.newSingleThreadExecutor().execute(() -> { loadResource(resId); });
    } else {
      fail("unsupported asset");
    }
  }

  private boolean maybeResolveCached() {
    final VideoFrame.Buffer cached = cache.retrieve(asset.src);
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
