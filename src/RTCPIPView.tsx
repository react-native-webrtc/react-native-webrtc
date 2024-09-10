import { Component, forwardRef } from 'react';

import RTCView, { RTCIOSPIPOptions, RTCVideoViewProps } from './RTCView';
// @ts-ignore
import ReactNative, { UIManager } from 'react-native';

export interface RTCPIPViewProps extends RTCVideoViewProps {
  iosPIP?: RTCIOSPIPOptions & {
    fallbackView?: Component;
  };
}

/**
 * A convenience wrapper around RTCView to handle the fallback view as a prop.
 */
const RTCPIPView = forwardRef((props: RTCPIPViewProps, ref) => {
  const rtcViewProps = { ...props };
  const fallbackView = rtcViewProps.iosPIP?.fallbackView;

  delete rtcViewProps.iosPIP?.fallbackView;

  return (
    // @ts-ignore
    <RTCView ref={ref} 
      {...rtcViewProps}>
      {fallbackView}
    </RTCView>
  );
});

export function startIOSPIP(ref) {
  UIManager.dispatchViewManagerCommand(
    ReactNative.findNodeHandle(ref.current),
    // @ts-ignore
    UIManager.RTCVideoView.Commands.startIOSPIP,
    []
  );
}

export function stopIOSPIP(ref) {
  UIManager.dispatchViewManagerCommand(
    ReactNative.findNodeHandle(ref.current),
    // @ts-ignore
    UIManager.RTCVideoView.Commands.stopIOSPIP,
    []
  );
}

export default RTCPIPView;