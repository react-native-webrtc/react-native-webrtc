import { type ReactNode, forwardRef, type ElementRef } from 'react';

import RTCView, { RTCIOSPIPOptions, RTCVideoViewProps } from './RTCView';

// Platform-specific spec — resolved by RN bundler at runtime
// eslint-disable-next-line @typescript-eslint/no-var-requires
const { Commands } = require('./NativeRTCVideoView');

export interface RTCPIPViewProps extends RTCVideoViewProps {
    iosPIP?: RTCIOSPIPOptions & {
        fallbackView?: ReactNode;
    };
}

type RTCViewRef = ElementRef<typeof RTCView>;

/**
 * A convenience wrapper around RTCView to handle the fallback view as a prop.
 */
const RTCPIPView = forwardRef<RTCViewRef, RTCPIPViewProps>((props, ref) => {
    const rtcViewProps = { ...props };
    const fallbackView = rtcViewProps.iosPIP?.fallbackView;

    delete rtcViewProps.iosPIP?.fallbackView;

    return (
        <RTCView ref={ref} {...rtcViewProps}>
            {fallbackView}
        </RTCView>
    );
});

export function startIOSPIP(ref) {
    Commands.startIOSPIP(ref.current);
}

export function stopIOSPIP(ref) {
    Commands.stopIOSPIP(ref.current);
}

export default RTCPIPView;
