package com.oney.WebRTCModule;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.InputStream;
import java.nio.ByteBuffer;
import org.webrtc.JavaI420Buffer;
import org.webrtc.VideoFrame;
import org.webrtc.YuvHelper;

public class BitmapUtils {
  private static final float AVAILABLE_MEMORY_ADJUSTMENT_RATIO = 0.8f;

  public static boolean hasEnoughMemoryToConvert(final Bitmap bitmap) {
    if (bitmap == null) {
      return true;
    }
    final int width = bitmap.getWidth();
    final int height = bitmap.getHeight();
    final int strideY = width;
    final int strideUV = (width + 1) / 2;
    final long maxMemory = Runtime.getRuntime().maxMemory();
    final long totalMemory = Runtime.getRuntime().totalMemory();
    final long freeMemory = Runtime.getRuntime().freeMemory();
    final long availableMemory = maxMemory - totalMemory + freeMemory;
    final long requiredMemory = (strideY * height) + 2 * (strideUV * ((height + 1) / 2)) + (width * height * 4);
    return requiredMemory < availableMemory * AVAILABLE_MEMORY_ADJUSTMENT_RATIO;
  }

  public static Bitmap bitmapFromStream(InputStream stream) {
    try {
      return BitmapFactory.decodeStream(stream);
    } catch (Exception e) {
      return null;
    }
  }

  public static VideoFrame.Buffer bufferFromBitmap(final Bitmap bitmap) {
    if (bitmap == null) {
      return null;
    }
    try {
      final int width = bitmap.getWidth();
      final int height = bitmap.getHeight();
      final int strideY = width;
      final int strideUV = (width + 1) / 2;

      final ByteBuffer dataY = ByteBuffer.allocateDirect(strideY * height);
      final ByteBuffer dataU = ByteBuffer.allocateDirect(strideUV * ((height + 1) / 2));
      final ByteBuffer dataV = ByteBuffer.allocateDirect(strideUV * ((height + 1) / 2));

      final ByteBuffer rgbaBuffer = ByteBuffer.allocateDirect(width * height * 4);
      bitmap.copyPixelsToBuffer(rgbaBuffer);
      rgbaBuffer.rewind();

      YuvHelper.ABGRToI420(rgbaBuffer, width * 4, dataY, strideY, dataU, strideUV, dataV, strideUV, width, height);
      rgbaBuffer.clear();

      return JavaI420Buffer.wrap(width, height, dataY, strideY, dataU, strideUV, dataV, strideUV, null);
    } catch (Exception e) {
      return null;
    }
  }
}
