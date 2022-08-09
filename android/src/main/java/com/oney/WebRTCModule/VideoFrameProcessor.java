package com.oney.WebRTCModule;

import org.webrtc.*;
import org.webrtc.YuvConverter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Color;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.os.Build;
import android.os.Environment;

public class VideoFrameProcessor implements VideoProcessor {
    private SurfaceTextureHelper textureHelper;
    private VideoSink mSink;

    public VideoFrameProcessor(String name, SurfaceTextureHelper textureHelper) {
        this.textureHelper = textureHelper;
    }

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
        // frame.retain();
        // YuvFrame yuvFrame = new YuvFrame(frame, YuvFrame.PROCESSING_NONE,
        // frame.getTimestampNs());
        // Bitmap bit2 = yuvFrame.getBitmap();

        // VideoFrame outputFrame = bitmap2videoFrame(bit2, bit2.getWidth(),
        // bit2.getHeight(), frame);
        // // Send VideoFrame back to WebRTC
        // mSink.onFrame(outputFrame);
        // outputFrame.release();
        // frame.release();
        // if (bit2 != null) {
        // bit2.recycle();
        // }
        // bit2 = null;
        // yuvFrame = null;

        mSink.onFrame(new VideoFrame(frame.getBuffer().toI420(), 0, frame.getTimestampNs()));
        frame.release();
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
                    bgBitmap.setPixel(x, y, whiteColor);
                } else {
                    bgBitmap.setPixel(x, y, blackColor);
                }
            }
        }
        return bgBitmap;
    }

    private VideoFrame bitmap2videoFrame(Bitmap bitmap, int width, int height, VideoFrame frame) {

        if (bitmap == null)
            return new VideoFrame(frame.getBuffer().toI420(), 0, frame.getTimestampNs());

        int[] textures = new int[1];
        long start = System.nanoTime();
        GLES20.glGenTextures(0, textures, 1);

        // TextureBuffer 생성
        Matrix transform = new Matrix();
        YuvConverter yuvConverter = new YuvConverter();
        TextureBufferImpl buffer = new TextureBufferImpl(width, height,
                VideoFrame.TextureBuffer.Type.RGB,
                textures[0],
                transform,
                textureHelper.getHandler(),
                yuvConverter,
                null);

        // 텍스처에 특정 Bitmap bitmap 이미지 로드

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_NEAREST);

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_NEAREST);

        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

        // 텍스처버퍼를 i420 버퍼로 변경
        VideoFrame.I420Buffer i420buffer = yuvConverter.convert(buffer);
        // buffer.toI420();
        long timestamp = System.nanoTime() - start;
        // 비디오 프레임 생성
        VideoFrame videoFrame = new VideoFrame(i420buffer, 180, timestamp);
        if (bitmap != null) {
            bitmap.recycle();
        }
        bitmap = null;
        return videoFrame;

    }

}
