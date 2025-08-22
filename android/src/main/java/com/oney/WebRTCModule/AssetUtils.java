package com.oney.WebRTCModule;

import android.content.Context;
import android.net.Uri;
import android.graphics.Bitmap;
import androidx.annotation.Nullable;
import org.webrtc.VideoFrame;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.List;
import com.facebook.react.views.imagehelper.ResourceDrawableIdHelper;

public class AssetUtils {
  public interface AssetLoadRresultHandler {
    void success(VideoFrame.Buffer image, int width, int height);
    void fail(String reason);
  }

  private static final String TAG = AssetUtils.class.getSimpleName();

  private static final String RESOURCE_SCHEME = "res";
  private static final String ANDROID_RESOURCE_SCHEME = "android.resource";
  private static final String HTTP_SCHEME = "http";

  private static final int INVALID_RESOURCE_ID = 0;

  private static final int MAX_BUFFER_SIZE = 20 * 1024 * 1024;

  private static @Nullable Uri getAssetUri(final Context context, final @Nullable String asset) {
    try {
      Uri uri = Uri.parse(asset);
      if (uri.getScheme() == null) {
        uri = ResourceDrawableIdHelper.getInstance().getResourceDrawableUri(context, asset);
      }
      return uri;
    } catch (Exception e) {
      return null;
    }
  }

  private static String getScheme(final Uri uri) {
    String scheme = uri.getScheme();
    if (scheme == null) {
      scheme = "";
    } else {
      scheme = scheme.toLowerCase();
    }
    return scheme;
  }

  private static int getResourceId(final Context context, final Uri uri) {
    List<String> pieces = uri.getPathSegments();
    if (pieces.size() == 1) {
        try {
            int id = Integer.parseInt(pieces.get(0));
            return id;
        }
        catch (NumberFormatException e) {
            // no-op
        }
    } else if (pieces.size() == 2) {
        return context.getResources().getIdentifier(pieces.get(0), pieces.get(1), context.getPackageName());
    }
    return INVALID_RESOURCE_ID;
  }

  private static void fail(final String message, final @Nullable AssetLoadRresultHandler handler) {
    ThreadUtils.runOnExecutor(() -> {
        if (handler != null) {
          handler.fail(message);
        }
      });
  }

  private static void success(VideoFrame.Buffer image, final int width, final int height, final @Nullable AssetLoadRresultHandler handler) {
    ThreadUtils.runOnExecutor(() -> {
        if (handler != null) {
          handler.success(image, width, height);
        } else {
          image.release();
        }
      });
  }

  public static void loadResourceStream(final InputStream stream, final @Nullable AssetLoadRresultHandler handler) {
    if (stream == null) {
      fail("no input stream", handler);
      return;
    }
    Bitmap bitmap = BitmapUtils.bitmapFromStream(stream);
    if (bitmap == null) {
      fail("could not convert stream to bitmap", handler);
      return;
    }
    if (!BitmapUtils.hasEnoughMemoryToConvert(bitmap)) {
      fail("not enough memory to handle bitmap", handler);
      return;
    }
    VideoFrame.Buffer buffer = BitmapUtils.bufferFromBitmap(bitmap);
    if (buffer == null) {
      fail("could not convert bitmap to buffer", handler);
      return;
    }
    int height = bitmap.getHeight();
    int width = bitmap.getWidth();
    bitmap.recycle();
    success(buffer, width, height, handler);
  }

  private static void loadLocalResource(final Context context, final int id, final @Nullable AssetLoadRresultHandler handler) {
    Executors.newSingleThreadExecutor().execute(() -> {
      try {
        loadResourceStream(context.getResources().openRawResource(id), handler);
      } catch (Exception e) {
        fail("failed to open resource", handler);
      }
    });
  }

  private static void loadRemoteResource(final Uri uri, final @Nullable AssetLoadRresultHandler handler) {
    Executors.newSingleThreadExecutor().execute(() -> {
      try {
        URL url = new URL(uri.toString());
        loadResourceStream(url.openStream(), handler);
      } catch (Exception e) {
        fail("failed to open url", handler);
      }
    });
  }

  public static void load(final Context context, final @Nullable String asset, final @Nullable AssetLoadRresultHandler handler) {
    Uri uri = getAssetUri(context, asset);
    if (uri == null || uri == Uri.EMPTY) {
      fail("could not get asset uri", handler);
      return;
    }
    String scheme = getScheme(uri);
    if (scheme.startsWith(HTTP_SCHEME)) {
      loadRemoteResource(uri, handler);
      return;
    } else if (scheme.equals(RESOURCE_SCHEME) || scheme.equals(ANDROID_RESOURCE_SCHEME)) {
      int id = getResourceId(context, uri);
      if (id == INVALID_RESOURCE_ID) {
        fail("invalid resource id", handler);
        return;
      }
      loadLocalResource(context, id, handler);
      return;
    } else {
      fail("ussuported asset source scheme", handler);
      return;
    }
  }
}
