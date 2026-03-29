import { forwardRef, type ElementRef } from 'react';

import RTCView, { RTCVideoViewProps } from './RTCView';

const RTCPIPView = forwardRef<ElementRef<typeof RTCView>, RTCVideoViewProps>((props, ref) => (
    <RTCView ref={ref} {...props} />
));

// eslint-disable-next-line @typescript-eslint/no-empty-function
export function startIOSPIP() {}

// eslint-disable-next-line @typescript-eslint/no-empty-function
export function stopIOSPIP() {}

export default RTCPIPView;
