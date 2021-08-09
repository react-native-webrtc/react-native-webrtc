package com.oney.WebRTCModule;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import org.webrtc.*;
/**
 * Class for converting OES textures RGBA. It should be constructed on a thread with
 * an active EGL context, and only be used from that thread. It is used by the EasyrtcSingleFrameCapturer.
 */
class TextureToRGB {
    // Vertex coordinates in Normalized Device Coordinates, i.e.
    // (-1, -1) is bottom-left and (1, 1) is top-right.
    private static final FloatBuffer DEVICE_RECTANGLE = GlUtil.createFloatBuffer(new float[] {
            -1.0f, -1.0f, // Bottom left.
            1.0f, -1.0f, // Bottom right.
            -1.0f, 1.0f, // Top left.
            1.0f, 1.0f, // Top right.
    });

    // Texture coordinates - (0, 0) is bottom-left and (1, 1) is top-right.
    private static final FloatBuffer TEXTURE_RECTANGLE = GlUtil.createFloatBuffer(new float[] {
            0.0f, 0.0f, // Bottom left.
            1.0f, 0.0f, // Bottom right.
            0.0f, 1.0f, // Top left.
            1.0f, 1.0f // Top right.
    });


    private static final String VERTEX_SHADER =
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

    private static final String FRAGMENT_SHADER =
            "#extension GL_OES_EGL_image_external : require\n"
                    + "precision mediump float;\n"
                    + "varying vec2 interp_tc;\n"
                    + "\n"
                    + "uniform samplerExternalOES oesTex;\n"
                    + "\n"
                    + "void main() {\n"
                    + "  gl_FragColor = texture2D(oesTex, interp_tc);\n"
                    + "}\n";
    // clang-format on

    private final GlTextureFrameBuffer textureFrameBuffer;
    private final GlShader shader;
    private final int texMatrixLoc;
    //private final ThreadUtils.ThreadChecker threadChecker = new ThreadUtils.ThreadChecker();
    private boolean released = false;

    /**
     * This class should be constructed on a thread that has an active EGL context.
     */
    public TextureToRGB() {
        //threadChecker.checkIsOnValidThread();
        textureFrameBuffer = new GlTextureFrameBuffer(GLES20.GL_RGBA);
        shader = new GlShader(VERTEX_SHADER, FRAGMENT_SHADER);
        shader.useProgram();
        texMatrixLoc = shader.getUniformLocation("texMatrix");



        GLES20.glUniform1i(shader.getUniformLocation("oesTex"), 0);
        GlUtil.checkNoGLES2Error("Initialize fragment shader uniform values.");
        // Initialize vertex shader attributes.
        shader.setVertexAttribArray("in_pos", 2, DEVICE_RECTANGLE);
        // If the width is not a multiple of 4 pixels, the texture
        // will be scaled up slightly and clipped at the right border.
        shader.setVertexAttribArray("in_tc", 2, TEXTURE_RECTANGLE);
    }

    public void convert(ByteBuffer buf, int width, int height, int srcTextureId,
                        float[] transformMatrix) {
        //threadChecker.checkIsOnValidThread();
        if (released) {
            throw new IllegalStateException("TextureToRGB.convert called on released object");
        }

        int size = width * height;
        if (buf.capacity() < size) {
            throw new IllegalArgumentException("TextureToRGB.convert called with too small buffer");
        }
        // Produce a frame buffer starting at top-left corner, not
        // bottom-left.
        transformMatrix =
                RendererCommon.multiplyMatrices(transformMatrix, RendererCommon.verticalFlipMatrix());

        final int frameBufferWidth = width;
        final int frameBufferHeight =height;
        textureFrameBuffer.setSize(frameBufferWidth, frameBufferHeight);

        // Bind our framebuffer.
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, textureFrameBuffer.getFrameBufferId());
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, srcTextureId);
        GLES20.glUniformMatrix4fv(texMatrixLoc, 1, false, transformMatrix, 0);
        GLES20.glViewport(0, 0, width, height);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glReadPixels(
                0, 0, frameBufferWidth, frameBufferHeight, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buf);

        // Restore normal framebuffer.
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        // Unbind texture. Reportedly needed on some devices to get
        // the texture updated from the camera.
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
        GlUtil.checkNoGLES2Error("TextureToRGB.convert");
    }

    public void release() {
        //threadChecker.checkIsOnValidThread();
        released = true;
        shader.release();
        textureFrameBuffer.release();
    }
}
