import type { ViewProps } from 'react-native';
import type { DirectEventHandler, Float, Int32 } from 'react-native/Libraries/Types/CodegenTypes';
import codegenNativeComponent from 'react-native/Libraries/Utilities/codegenNativeComponent';

export interface NativeProps extends ViewProps {
    streamURL?: string;
    mirror?: boolean;
    objectFit?: string;
    zOrder?: Int32;
    onDimensionsChange?: DirectEventHandler<Readonly<{ width: Int32; height: Int32 }>>;
    iosPIPEnabled?: boolean;
    iosPIPStartAutomatically?: boolean;
    iosPIPStopAutomatically?: boolean;
    iosPIPPreferredWidth?: Float;
    iosPIPPreferredHeight?: Float;
}

export default codegenNativeComponent<NativeProps>('RTCVideoView');
