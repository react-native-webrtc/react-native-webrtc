package com.oney.WebRTCModule;

import org.webrtc.*;

import java.io.ByteArrayOutputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Color;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;

public class VideoFrameProcessor implements VideoProcessor {
    private SurfaceTextureHelper textureHelper;
    private VideoSink mSink;

    public VideoFrameProcessor(SurfaceTextureHelper textureHelper) {
        this.textureHelper = textureHelper;
    }

    @Override
    public void onCapturerStarted(boolean success) {

    }

    @Override
    public void onCapturerStopped() {

    }

    @Override
    public void setSink(VideoSink sink) {
        mSink = sink;
    }

    @Override
    public void onFrameCaptured(VideoFrame frame) {
        // do all image processing to the
        int rotation = 180;
        VideoFrame outputFrame = new VideoFrame(frame.getBuffer(), rotation, frame.getTimestampNs());

        // Send VideoFrame back to WebRTC
        mSink.onFrame(outputFrame);
    }
    
    
    private Bitmap blackAndWhite(Bitmap image) {
        int blackColor = Color.parseColor("#000000");
        int whiteColor = Color.parseColor("#FFFFFF");
        int width = image.getWidth();
        int height = image.getHeight();
        Bitmap bgBitmap = Bitmap.createBitmap(width, height, image.getConfig());

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // sets the color of the pixel, depending if background or not
                int bgPixel = image.getPixel(x, y);
                if (bgPixel >= 128) {
                    bgBitmap.setPixel(x, y, blackColor);
                } else {
                    bgBitmap.setPixel(x, y, whiteColor);
                }
            }
        }
        return bgBitmap;
    }

    private byte[] I420BuffertoNV21(VideoFrame.I420Buffer image) {
        byte[] nv21;
        ByteBuffer yBuffer = image.getDataY();
        ByteBuffer uBuffer = image.getDataU();
        ByteBuffer vBuffer = image.getDataV();
        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();
        nv21 = new byte[ySize + uSize + vSize];
        // U and V are swapped
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);
        return nv21;
    }

    private Bitmap createBitmapFromByteArray(byte[] data) {
        return BitmapFactory.decodeByteArray(data, 0, data.length);
    }

    private VideoFrame bitmap2videoFrame(Bitmap bitmap, long timestampNs) {
        YuvConverter yuvConverter = new YuvConverter();
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
        TextureBufferImpl buffer = new TextureBufferImpl(bitmap.getWidth(), bitmap.getHeight(),
                VideoFrame.TextureBuffer.Type.RGB, textures[0], new Matrix(), textureHelper.getHandler(),
                yuvConverter, null);
        VideoFrame.I420Buffer i420Buf = yuvConverter.convert(buffer);
        VideoFrame CONVERTED_FRAME = new VideoFrame(i420Buf, 180, timestampNs);
        return CONVERTED_FRAME;
    }
}