package com.oney.WebRTCModule;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.InputStream;
import java.nio.ByteBuffer;
import org.webrtc.JavaI420Buffer;
import org.webrtc.VideoFrame;
import org.webrtc.YuvHelper;

import android.util.Log;

public class BitmapUtils {
  private static final float AVAILABLE_MEMORY_ADJUSTMENT_RATIO = 0.8f;

  public static boolean hasEnoughMemoryToConvert(Bitmap bitmap) {
    if (bitmap == null) {
      return true;
    }
    int width = bitmap.getWidth();
    int height = bitmap.getHeight();
    int strideY = width;
    int strideUV = (width + 1) / 2;
    long maxMemory = Runtime.getRuntime().maxMemory();
    long totalMemory = Runtime.getRuntime().totalMemory();
    long freeMemory = Runtime.getRuntime().freeMemory();
    long availableMemory = maxMemory - totalMemory + freeMemory;
    long requiredMemory = (strideY * height) + 2 * (strideUV * ((height + 1) / 2)) + (width * height * 4);
    Log.d("asdf", width + " " + height + " " + strideUV + " " + maxMemory + " " + totalMemory + " " + freeMemory + " " + availableMemory + " " + requiredMemory);
    return requiredMemory < availableMemory * AVAILABLE_MEMORY_ADJUSTMENT_RATIO;
  }

  public static Bitmap bitmapFromStream(InputStream stream) {
    try {
      return BitmapFactory.decodeStream(stream);
    } catch (Exception e) {
      return null;
    }
  }

  public static VideoFrame.Buffer bufferFromBitmap(Bitmap bitmap) {
    if (bitmap == null) {
      return null;
    }

    int width = bitmap.getWidth();
    int height = bitmap.getHeight();
    int strideY = width;
    int strideUV = (width + 1) / 2;

    ByteBuffer dataY = ByteBuffer.allocateDirect(strideY * height);
    ByteBuffer dataU = ByteBuffer.allocateDirect(strideUV * ((height + 1) / 2));
    ByteBuffer dataV = ByteBuffer.allocateDirect(strideUV * ((height + 1) / 2));

    ByteBuffer rgbaBuffer = ByteBuffer.allocateDirect(width * height * 4);
    bitmap.copyPixelsToBuffer(rgbaBuffer);
    rgbaBuffer.rewind();

    YuvHelper.ABGRToI420(rgbaBuffer, width * 4, dataY, strideY, dataU, strideUV, dataV, strideUV, width, height);
    rgbaBuffer.clear();

    return JavaI420Buffer.wrap(width, height, dataY, strideY, dataU, strideUV, dataV, strideUV, () -> {
      Log.d("BitmapUtils", "I420 buffer released");
    });
  }
}
