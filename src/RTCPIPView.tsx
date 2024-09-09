import { Component } from 'react';

import RTCView, { RTCIOSPIPOptions, RTCVideoViewProps } from './RTCView';

export interface RTCPIPViewProps extends RTCVideoViewProps {
  iosPIP?: RTCIOSPIPOptions & {
    fallbackView?: Component;
  };
}

/**
 * A convenience wrapper around RTCView to handle the fallback view as a prop.
 */
const RTCPIPView = (props: RTCPIPViewProps) => {
    const rtcViewProps = { ...props };
    const fallbackView = rtcViewProps.iosPIP?.fallbackView;

    delete rtcViewProps.iosPIP?.fallbackView;

    return (
        <RTCView {...rtcViewProps}>
            {fallbackView}
        </RTCView>
    );
};

export default RTCPIPView;