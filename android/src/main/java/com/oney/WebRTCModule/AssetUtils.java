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
  private static final String TAG = AssetUtils.class.getSimpleName();

  public static final int INVALID_RESOURCE_ID = 0;

  public static Uri assetStringToUri(final Context context, final String asset) {
    try {
      Uri uri = Uri.parse(asset);
      if (uri.getScheme() == null) {
        uri = ResourceDrawableIdHelper.getInstance().getResourceDrawableUri(context, asset);
      }
      return uri;
    } catch (Exception e) {
      return Uri.EMPTY;
    }
  }

  public static String getAssetUriScheme(final Uri uri) {
    String scheme = uri.getScheme();
    if (scheme == null) {
      scheme = "";
    } else {
      scheme = scheme.toLowerCase();
    }
    return scheme;
  }

  public static int getAssetResourceId(final Context context, final Uri uri) {
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
}