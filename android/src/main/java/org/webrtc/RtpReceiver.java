/*
 *  Copyright 2015 The WebRTC project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package org.webrtc;

/** Java wrapper for a C++ RtpReceiverInterface. */
public class RtpReceiver {
  /** Java wrapper for a C++ RtpReceiverObserverInterface*/
  public static interface Observer {
    // Called when the first audio or video packet is received.
    public void onFirstPacketReceived(MediaStreamTrack.MediaType media_type);
  }

  final long nativeRtpReceiver;
  private long nativeObserver;

  private MediaStreamTrack cachedTrack;

  public RtpReceiver(long nativeRtpReceiver) {
    this.nativeRtpReceiver = nativeRtpReceiver;
    long track = nativeGetTrack(nativeRtpReceiver);
    // We can assume that an RtpReceiver always has an associated track.
    cachedTrack = new MediaStreamTrack(track);
  }

  public MediaStreamTrack track() {
    return cachedTrack;
  }

  public boolean setParameters(RtpParameters parameters) {
    return nativeSetParameters(nativeRtpReceiver, parameters);
  }

  public RtpParameters getParameters() {
    return nativeGetParameters(nativeRtpReceiver);
  }

  public String id() {
    return nativeId(nativeRtpReceiver);
  }

  public void dispose() {
    cachedTrack.dispose();
    if (nativeObserver != 0) {
      nativeUnsetObserver(nativeRtpReceiver, nativeObserver);
      nativeObserver = 0;
    }
    free(nativeRtpReceiver);
  }

  public void SetObserver(Observer observer) {
    // Unset the existing one before setting a new one.
    if (nativeObserver != 0) {
      nativeUnsetObserver(nativeRtpReceiver, nativeObserver);
    }
    nativeObserver = nativeSetObserver(nativeRtpReceiver, observer);
  }

  // This should increment the reference count of the track.
  // Will be released in dispose().
  private static native long nativeGetTrack(long nativeRtpReceiver);

  private static native boolean nativeSetParameters(
      long nativeRtpReceiver, RtpParameters parameters);

  private static native RtpParameters nativeGetParameters(long nativeRtpReceiver);

  private static native String nativeId(long nativeRtpReceiver);

  private static native void free(long nativeRtpReceiver);

  private static native long nativeSetObserver(long nativeRtpReceiver, Observer observer);

  private static native long nativeUnsetObserver(long nativeRtpReceiver, long nativeObserver);
};
