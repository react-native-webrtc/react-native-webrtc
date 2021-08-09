package com.oney.WebRTCModule;

import android.graphics.Bitmap;
import android.util.Base64;
import android.util.Log;
import java.nio.ByteBuffer;

import org.webrtc.VideoTrack;
import org.webrtc.MediaStream;
import org.webrtc.EglBase;
import org.webrtc.RendererCommon;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLDisplay;

/**
 * Created by eric on 11/04/17.
 */
public class EasyrtcSingleFrameCapturer {

    public interface BitmapListener {
        public void gotBitmap(Bitmap theBitmap);
    }

    private static boolean firstTimeOnly = true;


    // the below pixelBuffer code is based on from
    // https://github.com/CyberAgent/android-gpuimage/blob/master/library/src/jp/co/cyberagent/android/gpuimage/PixelBuffer.java
    //
    class PixelBuffer implements org.webrtc.VideoRenderer.Callbacks {
        final static String TAG = "PixelBuffer";
        final static boolean LIST_CONFIGS = false;

        int mWidth, mHeight;
        EGL10 mEGL;
        EGLDisplay mEGLDisplay;
        boolean gotFrame = false;
        String mThreadOwner;
        BitmapListener listener;
        android.app.Activity activity;


        public PixelBuffer(android.app.Activity activity, BitmapListener listener) {
            this.listener = listener;
            this.activity = activity;
        }


        private static final String VERTEX_SHADER_STRING =
                "varying vec2 interp_tc;\n"
                        + "attribute vec4 in_pos;\n"
                        + "attribute vec4 in_tc;\n"
                        + "\n"
                        + "uniform mat4 texMatrix;\n"
                        + "\n"
                        + "void main() {\n"
                        + "    gl_Position = in_pos;\n"
                        + "    interp_tc = (texMatrix * in_tc).xy;\n"
                        + "}\n";


        @Override
        public void renderFrame(final org.webrtc.VideoRenderer.I420Frame i420Frame) {
            Log.d(TAG, "entered renderFrame");
            //
            // we only want to grab a single frame but our method may get called
            // a few times before we're done.
            //
            if (gotFrame || i420Frame.width == 0 || i420Frame.height == 0) {
                Log.d(TAG, "Already got frame so taking honourable exit");
                org.webrtc.VideoRenderer.renderFrameDone(i420Frame);
                return;
            }
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    int width = i420Frame.width;
                    int height = i420Frame.height;
                    Log.d(TAG, "about to call initWithSize");
                    initWithSize(width, height);
                    Bitmap bitmap = toBitmap(i420Frame);
                    org.webrtc.VideoRenderer.renderFrameDone(i420Frame);
                    gotFrame = true;
                    listener.gotBitmap(bitmap);
                    destroy();
                }
            });
        }

        private int buildARGB(int r, int g, int b) {
            return (0xff << 24) |(r << 16) | (g << 8) | b;
        }

        private Bitmap toBitmap(org.webrtc.VideoRenderer.I420Frame frame) {

            if (!frame.yuvFrame) {

                EglBase eglBase = EglUtils.getRootEglBase();
                if(firstTimeOnly) {
                    eglBase.createDummyPbufferSurface();
                    firstTimeOnly = false;
                }
                eglBase.makeCurrent();
                TextureToRGB textureToRGB = new TextureToRGB();
                int numPixels = mWidth *mHeight;
                final int bytesPerPixel = 4;
                ByteBuffer framebuffer = ByteBuffer.allocateDirect(numPixels*bytesPerPixel);

                final float frameAspectRatio = (float) frame.rotatedWidth() / (float) frame.rotatedHeight();

                final float[] rotatedSamplingMatrix =
                        RendererCommon.rotateTextureMatrix(frame.samplingMatrix, frame.rotationDegree);
                final float[] layoutMatrix = RendererCommon.getLayoutMatrix(
                        false, frameAspectRatio, (float) mWidth / mHeight);
                final float[] texMatrix = RendererCommon.multiplyMatrices(rotatedSamplingMatrix, layoutMatrix);

                textureToRGB.convert(framebuffer, mWidth, mHeight, frame.textureId, texMatrix);

                byte [] frameBytes = framebuffer.array();
                int [] dataARGB = new int[numPixels];
                for(int i = 0, j = 0; j < numPixels; i+=bytesPerPixel, j++) {
                    //
                    // data order in frameBytes is red, green, blue, alpha, red, green, ....
                    //
                    dataARGB[j] = buildARGB(frameBytes[i] & 0xff,frameBytes[i+1] &0xff,frameBytes[i+2] &0xff);
                }

                Bitmap bitmap = Bitmap.createBitmap(dataARGB, mWidth, mHeight, Bitmap.Config.ARGB_8888);
                return bitmap;
            }
            else {
                return null;
            }
        }

        private void initWithSize(final int width, final int height) {
            mWidth = width;
            mHeight = height;

            // Record thread owner of OpenGL context
            mThreadOwner = Thread.currentThread().getName();
        }


        public void destroy() {
        }


        private int getConfigAttrib(final EGLConfig config, final int attribute) {
            int[] value = new int[1];
            return mEGL.eglGetConfigAttrib(mEGLDisplay, config,
                    attribute, value) ? value[0] : 0;
        }


    }


    final private static String TAG = "frameCapturer";
    org.webrtc.VideoRenderer renderer;

    private  EasyrtcSingleFrameCapturer(final android.app.Activity activity, MediaStream mediaStream, final BitmapListener gotFrameListener) {
        if( mediaStream.videoTracks.size() == 0) {
            Log.e(TAG, "No video track to capture from");
            return;
        }

        final VideoTrack videoTrack = mediaStream.videoTracks.get( 0 );
        final PixelBuffer vg = new PixelBuffer(activity, new BitmapListener() {

            @Override
            public void gotBitmap(final Bitmap bitmap) {
                activity.runOnUiThread(new Runnable(){
                    public void run() {
                        videoTrack.removeRenderer(renderer);
                        try {
                            gotFrameListener.gotBitmap(bitmap);
                        } catch( Exception e1) {
                            Log.e(TAG, "Exception in gotBitmap callback:" + e1.getMessage());
                            e1.printStackTrace(System.err);
                        }
                    }
                });

            }
        });
        renderer = new org.webrtc.VideoRenderer(vg);
        videoTrack.addRenderer(renderer);
    }

    /**
     * This constructor builds an object which captures a frame from mediastream to a Bitmap.
     * @param mediaStream The input media mediaStream.
     * @param gotFrameListener A callback which will receive the Bitmap.
     */
    public static void toBitmap(android.app.Activity activity, MediaStream mediaStream, final BitmapListener gotFrameListener) {
        new EasyrtcSingleFrameCapturer(activity, mediaStream, gotFrameListener);
    }

    /**
     * This method captures a frame from the supplied media stream to a jpeg file written to the supplied outputStream.
     * @param mediaStream  the source media stream
     * @param quality the quality of the jpeq 0 to 100.
     * @param outputStream the output stream the jpeg file will be written to.
     * @param done a runnable that will be invoked when the outputstream has been written to.
     * @return The frame capturer. You should keep a reference to the frameCapturer until the done object is invoked.
     */
    public static void toOutputStream(android.app.Activity activity, MediaStream mediaStream, final int quality, final java.io.OutputStream outputStream, final Runnable done) {
        BitmapListener gotFrameListener = new BitmapListener() {

            @Override
            public void gotBitmap(Bitmap theBitmap) {
                theBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
                try {
                    done.run();
                } catch( Exception e1) {
                    Log.e(TAG, "Exception in toOutputStream done callback:" + e1.getMessage());
                    e1.printStackTrace(System.err);
                }

            }
        };
        toBitmap(activity, mediaStream, gotFrameListener);
    }

    /**
     * This method captures a frame from the supplied mediastream to a dataurl written to a StringBuilder.
     * @param mediaStream  the source media stream
     * @param quality the quality of the jpeq 0 to 100.
     * @param output a StringBuilder which will be the recipient of the dataurl.
     * @param done a runnable that will be invoked when the dataurl is built.
     * @return The frame capturer. You should keep a reference to the frameCapturer until the done object is invoked.
     */
    public static void toDataUrl(android.app.Activity activity, MediaStream mediaStream, final int quality, final StringBuilder output, final Runnable done) {

        final java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
        Runnable convertToUrl = new Runnable() {

            @Override
            public void run() {
                output.append("data:image/jpeg;base64,");
                output.append(Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT));
                try {
                    done.run();
                } catch( Exception e1) {
                    Log.e(TAG, "Exception in toDataUrl done callback:" + e1.getMessage());
                    e1.printStackTrace(System.err);
                }
            }
        };
        toOutputStream(activity, mediaStream, quality, outputStream, convertToUrl);
    }
}
