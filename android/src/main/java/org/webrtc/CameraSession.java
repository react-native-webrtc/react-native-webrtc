/*
 *  Copyright 2016 The WebRTC project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package org.webrtc;

public interface CameraSession {
  // Callbacks are fired on the camera thread.
  public interface CreateSessionCallback {
    void onDone(CameraSession session);
    void onFailure(String error);
  }

  // Events are fired on the camera thread.
  public interface Events {
    void onCameraOpening();
    void onCameraError(CameraSession session, String error);
    void onCameraDisconnected(CameraSession session);
    void onCameraClosed(CameraSession session);
    void onByteBufferFrameCaptured(
        CameraSession session, byte[] data, int width, int height, int rotation, long timestamp);
    void onTextureFrameCaptured(CameraSession session, int width, int height, int oesTextureId,
        float[] transformMatrix, int rotation, long timestamp);
  }

  /**
   * Stops the capture. Waits until no more calls to capture observer will be made.
   * If waitCameraStop is true, also waits for the camera to stop.
   */
  void stop();
}
