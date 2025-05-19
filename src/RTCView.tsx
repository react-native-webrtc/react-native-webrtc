import React from 'react';
import {
    findNodeHandle,
    NativeMethods,
    NativeSyntheticEvent,
    Platform,
    requireNativeComponent,
    UIManager,
    ViewProps,
} from 'react-native';

/**
 * Native prop validation was removed from RN in:
 * https://github.com/facebook/react-native/commit/8dc3ba0444c94d9bbb66295b5af885bff9b9cd34
 *
 * So we list them here for documentation purposes.
 */
interface RTCVideoViewBaseProps extends ViewProps {
  /**
   * Indicates whether the video specified by {@link #streamURL} should be
   * mirrored during rendering. Commonly, applications choose to mirror the
   * user-facing camera.
   *
   * mirror: boolean
   */
  mirror?: boolean;

  /**
   * In the fashion of
   * https://www.w3.org/TR/html5/embedded-content-0.html#dom-video-videowidth
   * and https://www.w3.org/TR/html5/rendering.html#video-object-fit,
   * resembles the CSS style object-fit.
   *
   * objectFit: 'contain' | 'cover'
   *
   * Defaults to 'cover'.
   */
  objectFit?: 'contain' | 'cover';

  /**
   * URL / id of the stream that should be rendered.
   *
   * streamURL: string
   */
  streamURL?: string;
  /**
   * Similarly to the CSS property z-index, specifies the z-order of this
   * RTCView in the stacking space of all RTCViews. When RTCViews overlap,
   * zOrder determines which one covers the other. An RTCView with a larger
   * zOrder generally covers an RTCView with a lower one.
   *
   * Non-overlapping RTCViews may safely share a z-order (because one does not
   * have to cover the other).
   *
   * The support for zOrder is platform-dependent and/or
   * implementation-specific. Thus, specifying a value for zOrder is to be
   * thought of as giving a hint rather than as imposing a requirement. For
   * example, video renderers such as RTCView are commonly implemented using
   * OpenGL and OpenGL views may have different numbers of layers in their
   * stacking space. Android has three: a layer bellow the window (aka
   * default), a layer bellow the window again but above the previous layer
   * (aka media overlay), and above the window. Consequently, it is advisable
   * to limit the number of utilized layers in the stacking space to the
   * minimum sufficient for the desired display. For example, a video call
   * application usually needs a maximum of two zOrder values: 0 for the
   * remote video(s) which appear in the background, and 1 for the local
   * video(s) which appear above the remote video(s).
   *
   * zOrder: number
   */
  zOrder?: number;
  /**
   * Indicates whether this view can manage Picture in Picture.
   * Only one view should be allowed to manage Picture in Picture
   * Defaults to false.
   */
  pictureInPictureEnabled?: boolean;
  /**
   * Indicates whether Picture in Picture starts automatically
   * when the controller embeds its content inline and the app
   * transitions to the background.
   *
   * Defaults to true.
   *
   * iOS: AVPictureInPictureController.canStartPictureInPictureAutomaticallyFromInline
   */
  autoStartPictureInPicture?: boolean;
  /**
   * The preferred size of the PIP window.
   */
  pictureInPicturePreferredSize?: {
    width: number;
    height: number;
  };
  /**
   * Indicates whether Picture in Picture should stop automatically
   * when the app returns to the foreground.
   *
   * Defaults to true.
   */
  autoStopPictureInPicture?: boolean;
}

interface NativeVideoViewProps extends RTCVideoViewBaseProps {
  onPictureInPictureChange?: (
    event: NativeSyntheticEvent<{ isInPictureInPicture: boolean }>
  ) => void;
}

interface RTCVideoViewProps extends RTCVideoViewBaseProps {
  onPictureInPictureChange?: (isInPictureInPicture: boolean) => void;
}

const NativeRTCVideoView =
  requireNativeComponent<NativeVideoViewProps>('RTCVideoView');

type CommandName = 'startPictureInPicture' | 'stopPictureInPicture';

type RefType = React.Component<NativeVideoViewProps> & Readonly<NativeMethods>;

class RTCView extends React.PureComponent<RTCVideoViewProps> {
    private readonly ref: React.RefObject<RefType>;

    constructor(props: RTCVideoViewProps) {
        super(props);
        this.onPictureInPictureChange = this.onPictureInPictureChange.bind(this);
        this.ref = React.createRef<RefType>();
    }

    /**
   * Programmatically start Picture In Picture
   */
    public startPictureInPicture() {
        try {
            const node = this.handle;

            UIManager.dispatchViewManagerCommand(
                node,
                this.getCommand('startPictureInPicture'),
                []
            );
        } catch (error) {
            console.warn(error);
        }
    }
    /**
   * Programmatically stop Picture In Picture
   * @ios
   */
    public stopPictureInPicture() {
        try {
            const node = this.handle;

            UIManager.dispatchViewManagerCommand(
                node,
                this.getCommand('stopPictureInPicture'),
                []
            );
        } catch (error) {
            console.warn(error);
        }
    }

    private getCommand(commandName: CommandName): string | number {
        const config = UIManager.getViewManagerConfig('RTCVideoView');
        const command = config.Commands[commandName];

        return Platform.OS === 'android' ? command.toString() : command;
    }

    private get handle(): number {
        const nodeHandle = findNodeHandle(this.ref.current);

        if (nodeHandle === null || nodeHandle === -1) {
            throw new Error('RTCView not found in react three.');
        }

        return nodeHandle;
    }

    private onPictureInPictureChange(
        event: NativeSyntheticEvent<{ isInPictureInPicture: boolean }>
    ) {
        this.props.onPictureInPictureChange?.(
            event.nativeEvent.isInPictureInPicture
        );
    }

    render(): React.ReactNode {
        const { ...props } = this.props;

        return (
            <NativeRTCVideoView
                {...props}
                ref={this.ref}
                onPictureInPictureChange={this.onPictureInPictureChange}
            />
        );
    }
}

export { RTCVideoViewProps, NativeRTCVideoView , NativeVideoViewProps };

export default RTCView;
