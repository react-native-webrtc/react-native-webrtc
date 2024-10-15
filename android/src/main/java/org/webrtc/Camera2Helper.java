/*
 * Copyright 2023-2024 LiveKit, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.webrtc;

import android.hardware.camera2.CameraManager;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A helper to access package-protected methods used in [Camera2Session]
 * <p>
 * Note: cameraId as used in the Camera2XXX classes refers to the id returned
 * by [CameraManager.getCameraIdList].
 */
public class Camera2Helper {

    @Nullable
    public static List<CameraEnumerationAndroid.CaptureFormat> getSupportedFormats(CameraManager cameraManager, @Nullable String cameraId) {
        return Camera2Enumerator.getSupportedFormats(cameraManager, cameraId);
    }

    public static Size findClosestCaptureFormat(CameraManager cameraManager, @Nullable String cameraId, int width, int height) {
        List<CameraEnumerationAndroid.CaptureFormat> formats = getSupportedFormats(cameraManager, cameraId);

        List<Size> sizes = new ArrayList<>();
        if (formats != null) {
            for (CameraEnumerationAndroid.CaptureFormat format : formats) {
                sizes.add(new Size(format.width, format.height));
            }
        }

        return CameraEnumerationAndroid.getClosestSupportedSize(sizes, width, height);
    }
}
