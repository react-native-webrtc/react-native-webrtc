import { Component, forwardRef } from 'react';
import ReactNative, { UIManager } from 'react-native';

import RTCView, { RTCIOSPIPOptions, RTCVideoViewProps } from './RTCView';

export interface RTCPIPViewProps extends RTCVideoViewProps {
  iosPIP?: RTCIOSPIPOptions & {
    fallbackView?: Component;
  };
}

type RTCViewInstance = InstanceType<typeof RTCView>;

/**
 * A convenience wrapper around RTCView to handle the fallback view as a prop.
 */
const RTCPIPView = forwardRef<RTCViewInstance, RTCPIPViewProps>((props, ref) => {
    const rtcViewProps = { ...props };
    const fallbackView = rtcViewProps.iosPIP?.fallbackView;

    delete rtcViewProps.iosPIP?.fallbackView;

    return (
        <RTCView ref={ref}
            {...rtcViewProps}>
            {fallbackView}
        </RTCView>
    );
});

export function startIOSPIP(ref) {
    UIManager.dispatchViewManagerCommand(
        ReactNative.findNodeHandle(ref.current),
        UIManager.getViewManagerConfig('RTCVideoView').Commands.startIOSPIP,
        []
    );
}

export function stopIOSPIP(ref) {
    UIManager.dispatchViewManagerCommand(
        ReactNative.findNodeHandle(ref.current),
        UIManager.getViewManagerConfig('RTCVideoView').Commands.stopIOSPIP,
        []
    );
}

export default RTCPIPView;