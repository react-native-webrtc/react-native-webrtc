import { Component, forwardRef } from 'react';
import ReactNative, { UIManager } from 'react-native';

import  { NativeVideoViewProps, NativeRTCVideoView , } from './RTCView';

export interface RTCPIPViewProps extends NativeVideoViewProps {
    /**
   * Picture in picture options for this view. Disabled if not supplied.
   *
   * Note: this should only be generally only used with remote video tracks,
   * as the local camera may stop while in the background.
   *
   * iOS only. Requires iOS 15.0 or above, and the PIP background mode capability.
   * @deprecated
   */
  iosPIP?: RTCIOSPIPOptions & {
    fallbackView?: Component;
  };
}

export interface RTCIOSPIPOptions {
  /**
   * Whether PIP can be launched from this view.
   *
   * Defaults to true.
   */
  enabled?: boolean;

  /**
   * The preferred size of the PIP window.
   */
  preferredSize?: {
    width: number;
    height: number;
  },

  /**
   * Indicates whether Picture in Picture starts automatically
   * when the controller embeds its content inline and the app
   * transitions to the background.
   *
   * Defaults to true.
   *
   * See: AVPictureInPictureController.canStartPictureInPictureAutomaticallyFromInline
   */
  startAutomatically?: boolean;

  /**
   * Indicates whether Picture in Picture should stop automatically
   * when the app returns to the foreground.
   *
   * Defaults to true.
   */
  stopAutomatically?: boolean;
}

type RTCViewInstance = InstanceType<typeof NativeRTCVideoView>;

/**
 * A convenience wrapper around RTCView to handle the fallback view as a prop.
 * @deprecated Use RTCView instead.
 */
const RTCPIPView = forwardRef<RTCViewInstance, RTCPIPViewProps>((props, ref) => {
    const rtcViewProps = { ...props };
    const fallbackView = rtcViewProps.iosPIP?.fallbackView;

    delete rtcViewProps.iosPIP?.fallbackView;

    return (
        <NativeRTCVideoView ref={ref}
            {...rtcViewProps}>
            {fallbackView}
        </NativeRTCVideoView>
    );
});

/**
 * @deprecated
 */
export function startIOSPIP(ref) {
    UIManager.dispatchViewManagerCommand(
        ReactNative.findNodeHandle(ref.current),
        UIManager.getViewManagerConfig('RTCVideoView').Commands.startIOSPIP,
        []
    );
}

/**
 * @deprecated
 */
export function stopIOSPIP(ref) {
    UIManager.dispatchViewManagerCommand(
        ReactNative.findNodeHandle(ref.current),
        UIManager.getViewManagerConfig('RTCVideoView').Commands.stopIOSPIP,
        []
    );
}

export default RTCPIPView;