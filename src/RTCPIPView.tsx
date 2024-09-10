import { Component, forwardRef } from 'react';
import ReactNative, { UIManager } from 'react-native';

import RTCView, { RTCIOSPIPOptions, RTCVideoViewProps } from './RTCView';

export interface RTCPIPViewProps extends RTCVideoViewProps {
  iosPIP?: RTCIOSPIPOptions & {
    fallbackView?: Component;
  };
}

/**
 * A convenience wrapper around RTCView to handle the fallback view as a prop.
 */
const RTCPIPView = forwardRef<Component, RTCPIPViewProps>((props, ref) => {
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