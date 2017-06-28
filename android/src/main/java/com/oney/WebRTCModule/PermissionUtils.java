package com.oney.WebRTCModule;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ResultReceiver;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;


/**
 * Helper module for dealing with dynamic permissions, introduced in Android M (API level 23).
 */
public class PermissionUtils {
    /**
     * Constants for internal fields in the <tt>Bundle</tt> exchanged between the activity
     * requesting the permissions and the auxiliary activity we spawn for this purpose.
     */
    private static final String GRANT_RESULT = "GRANT_RESULT";
    private static final String PERMISSION = "PERMISSION";
    private static final String REQUEST_CODE = "REQUEST_CODE";
    private static final String RESULT_RECEIVER = "RESULT_RECEIVER";

    /**
     * Incrementing counter for permissions requests. Each request must have a unique numeric code.
     */
    private static int requestCode;

    /**
     * Requests the given permission. The callback will we called with the requested permission and
     * the granted result. See: https://developer.android.com/reference/android/content/pm/PackageManager.html#PERMISSION_GRANTED
     * for the result constants.
     *
     * @param context Application context / activity.
     * @param permission Permission which the application is interested in requesting.
     * @param callback Callback where the results will be reported.
     */
    public static void requestPermission(Context context,
                                         String permission,
                                         final Callback callback) {
        ResultReceiver resultReceiver = new ResultReceiver(new Handler(Looper.getMainLooper())) {
            @Override
            protected void onReceiveResult (int resultCode, Bundle resultData) {
                String permission = resultData.getString(PERMISSION);
                int grantResult = resultData.getInt(GRANT_RESULT);

                callback.invoke(permission, grantResult);
            }
        };

        requestCode++;

        Intent permissionsIntent = new Intent(context, PermissionHelperActivity.class);
        permissionsIntent.putExtra(RESULT_RECEIVER, resultReceiver);
        permissionsIntent.putExtra(PERMISSION, permission);
        permissionsIntent.putExtra(REQUEST_CODE, requestCode);
        permissionsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        context.startActivity(permissionsIntent);
    }

    /**
     * Helper activity for requesting permissions. Android only allows requesting permissions from
     * an activity and the result is reported in the <tt>onRequestPermissionsResult</tt> method.
     * Since this package is a library we create an auxiliary activity and communicate back the
     * results using a <tt>ResultReceiver</tt>.
     */
    public static class PermissionHelperActivity extends Activity {
        int requestCode;
        ResultReceiver resultReceiver;
        String permission;

        /**
         * {@inheritDoc}
         */
        @Override
        public void onRequestPermissionsResult(int requestCode,
                                               String permissions[],
                                               int[] grantResults) {
            Bundle resultData = new Bundle();
            resultData.putString(PERMISSION, permissions[0]);
            resultData.putInt(GRANT_RESULT, grantResults[0]);
            resultReceiver.send(requestCode, resultData);
            finish();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void onStart() {
            super.onStart();

            resultReceiver = this.getIntent().getParcelableExtra(RESULT_RECEIVER);
            permission = this.getIntent().getStringExtra(PERMISSION);
            requestCode = this.getIntent().getIntExtra(REQUEST_CODE, 0);

            // Here we test for the target SDK version with which *the app* was compiled. If we use
            // Build.VERSION.SDK_INT that would give us the API version of the device itself, not
            // the version the app was compiled for. When compiled for API level < 23 we must still
            // use old permissions model, regardless of the Android version on the device.
            final int targetSdkVersion = getApplicationInfo().targetSdkVersion;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                    && targetSdkVersion >= Build.VERSION_CODES.M) {
                ActivityCompat.requestPermissions(
                    this,new String[]{permission}, requestCode);
            } else {
                // In the old permissions model the permissions are granted at install time, we can
                // check for them directly.
                Bundle resultData = new Bundle();
                resultData.putString(PERMISSION, permission);
                resultData.putInt(
                    GRANT_RESULT, ContextCompat.checkSelfPermission(this, permission));
                resultReceiver.send(requestCode, resultData);
                finish();
            }
        }
    }

    /**
     * Helper class for reporting back permission results.
     * See: https://developer.android.com/reference/android/content/pm/PackageManager.html#PERMISSION_GRANTED
     * for the result constants.
     */
    public interface Callback {
        void invoke(String permission, int grantResult);
    }
}
