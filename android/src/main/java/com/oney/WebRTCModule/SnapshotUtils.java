package com.oney.WebRTCModule;

import com.facebook.react.bridge.ReactContext;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import java.util.Base64;
import java.util.UUID;

class SnapshotUtils {
    private static final String TAG = WebRTCModule.TAG;

    public static final String RCT_CAMERA_SAVE_TARGET_MEMORY = "memory";
    public static final String RCT_CAMERA_SAVE_TARGET_DISK = "disk";
    public static final String RCT_CAMERA_SAVE_TARGET_CAMERA_ROLL = "cameraRoll";
    public static final String RCT_CAMERA_SAVE_TARGET_TEMP = "temp";

    public static String encodeToBase64String(byte[] pic) {
        return Base64.getEncoder().encodeToString(pic);
    }

    public static synchronized String savePicture(ReactContext reactContext, Bitmap bitmap, String saveTarget, double maxJpegQuality, int maxSize) throws IOException {
        // --- only resize if image larger than maxSize
        bitmap = resizeBitmap(bitmap, maxSize);
        int jpegQuality = (int) (100 * maxJpegQuality);

        String filename = UUID.randomUUID().toString();
        File file = null;
        switch (saveTarget) {
            case RCT_CAMERA_SAVE_TARGET_CAMERA_ROLL: {
                file = getOutputCameraRollFile(filename);
                writePictureToFile(bitmap, file, jpegQuality);
                addToMediaStore(reactContext, file.getAbsolutePath());
                break;
            }
            case RCT_CAMERA_SAVE_TARGET_DISK: {
                file = getOutputMediaFile(filename);
                writePictureToFile(bitmap, file, jpegQuality);
                break;
            }
            case RCT_CAMERA_SAVE_TARGET_TEMP: {
                file = getTempMediaFile(reactContext, filename);
                writePictureToFile(bitmap, file, jpegQuality);
                break;
            }
            default: {
                // --- memory
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, jpegQuality, stream);
                byte[] jpeg = stream.toByteArray();
                return encodeToBase64String(jpeg);
            }
        }
        return Uri.fromFile(file).toString();
    }

    public static Bitmap resizeBitmap(Bitmap bitmap, int maxSize) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        // only resize if image larger than maxSize
        if (width > maxSize && height > maxSize) {
            Matrix matrix = new Matrix();
            Rect originalRect = new Rect(0, 0, width, height);
            Rect scaledRect = scaleDimension(originalRect, maxSize);
            Log.d(TAG, "scaled width = " + scaledRect.width() + ", scaled height = " + scaledRect.height());
            // calculate the scale
            float scaleWidth = ((float) scaledRect.width()) / width;
            float scaleHeight = ((float) scaledRect.height()) / height;
            matrix.postScale(scaleWidth, scaleHeight);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        }

        return bitmap;
    }

    public static String writePictureToFile(Bitmap bitmap, File file, int jpegQuality) throws IOException {
        FileOutputStream finalOutput = new FileOutputStream(file, false);
        bitmap.compress(Bitmap.CompressFormat.JPEG, jpegQuality, finalOutput);
        finalOutput.close();
        return file.getAbsolutePath();
    }

    public static File getOutputMediaFile(String fileName) {
        // Get environment directory type id from requested media type.
        String environmentDirectoryType;
        environmentDirectoryType = Environment.DIRECTORY_PICTURES;
        return getOutputFile(
                fileName + ".jpeg",
                Environment.getExternalStoragePublicDirectory(environmentDirectoryType)
        );
    }

    public static File getOutputCameraRollFile(String fileName) {
        return getOutputFile(
                fileName + ".jpeg",
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
        );
    }

    public static File getOutputFile(String fileName, File storageDir) {
        // Create the storage directory if it does not exist
        if (!storageDir.exists()) {
            if (!storageDir.mkdirs()) {
                Log.e(TAG, "failed to create directory:" + storageDir.getAbsolutePath());
                return null;
            }
        }
        return new File(String.format("%s%s%s", storageDir.getPath(), File.separator, fileName));
    }

    public static File getTempMediaFile(ReactContext reactContext, String fileName) {
        try {
            File outputDir = reactContext.getCacheDir();
            File outputFile;
            outputFile = File.createTempFile(fileName, ".jpg", outputDir);
            return outputFile;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            return null;
        }
    }

    public static void addToMediaStore(ReactContext reactContext, String path) {
        MediaScannerConnection.scanFile(reactContext, new String[]{path}, null, null);
    }

    public static Rect scaleDimension(Rect originalRect, int maxSize) {
        int originalWidth = originalRect.width();
        int originalHeight = originalRect.height();
        int newWidth = originalWidth;
        int newHeight = originalHeight;
        // first check if we need to scale width
        if (originalWidth > maxSize) {
            //scale width to fit
            newWidth = maxSize;
            //scale height to maintain aspect ratio
            newHeight = (newWidth * originalHeight) / originalWidth;
        }
        // then check if we need to scale even with the new height
        if (newHeight > maxSize) {
            //scale height to fit instead
            newHeight = maxSize;
            //scale width to maintain aspect ratio
            newWidth = (newHeight * originalWidth) / originalHeight;
        }
        return new Rect(0, 0, newWidth, newHeight);
    }

}