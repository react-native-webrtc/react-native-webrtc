package com.oney.WebRTCModule;

import android.content.Context;
import android.net.Uri;
import android.graphics.Bitmap;
import androidx.annotation.Nullable;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.Executors;
import org.webrtc.VideoFrame;

public class ImageLoader {
  @FunctionalInterface
  public interface LoadSuccess {
    void success(VideoFrame.Buffer image, int width, int height);
  }
  @FunctionalInterface
  public interface LoadFail {
    void fail(String reason);
  }

  private static final String RESOURCE_SCHEME = "res";
  private static final String ANDROID_RESOURCE_SCHEME = "android.resource";
  private static final String HTTP_SCHEME = "http";

  private final Context context;
  private final String asset;
  private final LoadSuccess onSuccess;
  private final LoadFail onFail;

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
  }

  private void clean() {
    if (stream != null) {
      stream.close();
    }
  }

  private void fail(final String message) {
    ThreadUtils.runOnExecutor(() -> {
      clean();
      onFail.fail(message);
    });
  }

  private void success(VideoFrame.Buffer image, final int width, final int height) {
    ThreadUtils.runOnExecutor(() -> {
      clean();
      onSuccess.success(image, width, height);
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
    int height = bitmap.getHeight();
    int width = bitmap.getWidth();
    bitmap.recycle();
    success(buffer, width, height);
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

  public void load() {
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