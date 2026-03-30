import type { HostComponent, ViewProps } from 'react-native';
import type { DirectEventHandler, Float, Int32 } from 'react-native/Libraries/Types/CodegenTypes';
import codegenNativeCommands from 'react-native/Libraries/Utilities/codegenNativeCommands';
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

export interface NativeCommands {
    startIOSPIP: (viewRef: React.ElementRef<HostComponent<NativeProps>>) => void;
    stopIOSPIP: (viewRef: React.ElementRef<HostComponent<NativeProps>>) => void;
}

export const Commands: NativeCommands = codegenNativeCommands<NativeCommands>({
    supportedCommands: ['startIOSPIP', 'stopIOSPIP'],
});

export default codegenNativeComponent<NativeProps>('RTCVideoView');
