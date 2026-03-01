import * as base64 from 'base64-js';
import { EventTarget, defineEventAttribute } from 'event-target-shim';
import { NativeModules } from 'react-native';

import { addListener, removeListener } from './EventEmitter';
import MessageEvent from './MessageEvent';
import RTCDataChannelEvent from './RTCDataChannelEvent';

const { WebRTCModule } = NativeModules;

type RTCDataChannelState = 'connecting' | 'open' | 'closing' | 'closed';

type DataChannelEventMap = {
    bufferedamountlow: RTCDataChannelEvent<'bufferedamountlow'>;
    close: RTCDataChannelEvent<'close'>;
    closing: RTCDataChannelEvent<'closing'>;
    error: RTCDataChannelEvent<'error'>;
    message: MessageEvent<'message'>;
    open: RTCDataChannelEvent<'open'>;
};

export default class RTCDataChannel extends EventTarget<DataChannelEventMap> {
    _peerConnectionId: number;
    _reactTag: string;

    _bufferedAmount: number;
    _id: number;
    _label: string;
    _maxPacketLifeTime?: number;
    _maxRetransmits?: number;
    _negotiated: boolean;
    _ordered: boolean;
    _protocol: string;
    _readyState: RTCDataChannelState;

    binaryType = 'arraybuffer'; // we only support 'arraybuffer'
    bufferedAmountLowThreshold = 0;

    constructor(info) {
        super();

        this._peerConnectionId = info.peerConnectionId;
        this._reactTag = info.reactTag;

        this._bufferedAmount = 0;
        this._label = info.label;
        this._id = info.id === -1 ? null : info.id; // null until negotiated.
        this._ordered = Boolean(info.ordered);
        this._maxPacketLifeTime = info.maxPacketLifeTime;
        this._maxRetransmits = info.maxRetransmits;
        this._protocol = info.protocol || '';
        this._negotiated = Boolean(info.negotiated);
        this._readyState = info.readyState;

        this._registerEvents();
    }

    get bufferedAmount(): number {
        return this._bufferedAmount;
    }

    get label(): string {
        return this._label;
    }

    get id(): number {
        return this._id;
    }

    get ordered(): boolean {
        return this._ordered;
    }

    get maxPacketLifeTime(): number | undefined {
        return this._maxPacketLifeTime;
    }

    get maxRetransmits(): number | undefined {
        return this._maxRetransmits;
    }

    get protocol(): string {
        return this._protocol;
    }

    get negotiated(): boolean {
        return this._negotiated;
    }

    get readyState(): string {
        return this._readyState;
    }

    send(data: string): void;
    send(data: ArrayBuffer): void;
    send(data: ArrayBufferView): void;
    send(data: string | ArrayBuffer | ArrayBufferView): void {
        if (typeof data === 'string') {
            WebRTCModule.dataChannelSend(this._peerConnectionId, this._reactTag, data, 'text');

            return;
        }

        // Safely convert the buffer object to an Uint8Array for base64-encoding
        if (ArrayBuffer.isView(data)) {
            data = new Uint8Array(data.buffer, data.byteOffset, data.byteLength);
        } else if (data instanceof ArrayBuffer) {
            data = new Uint8Array(data);
        } else {
            throw new TypeError('Data must be either string, ArrayBuffer, or ArrayBufferView');
        }

        const base64data = base64.fromByteArray(data as Uint8Array);

        WebRTCModule.dataChannelSend(this._peerConnectionId, this._reactTag, base64data, 'binary');
    }

    close(): void {
        if (this._readyState === 'closing' || this._readyState === 'closed') {
            return;
        }

        WebRTCModule.dataChannelClose(this._peerConnectionId, this._reactTag);
    }

    _registerEvents(): void {
        addListener(this, 'dataChannelStateChanged', (ev: any) => {
            if (ev.reactTag !== this._reactTag) {
                return;
            }

            this._readyState = ev.state;

            if (this._id === null && ev.id !== -1) {
                this._id = ev.id;
            }

            if (this._readyState === 'open') {
                this.dispatchEvent(new RTCDataChannelEvent('open', { channel: this }));
            } else if (this._readyState === 'closing') {
                this.dispatchEvent(new RTCDataChannelEvent('closing', { channel: this }));
            } else if (this._readyState === 'closed') {
                this.dispatchEvent(new RTCDataChannelEvent('close', { channel: this }));

                // This DataChannel is done, clean up event handlers.
                removeListener(this);

                WebRTCModule.dataChannelDispose(this._peerConnectionId, this._reactTag);
            }
        });

        addListener(this, 'dataChannelReceiveMessage', (ev: any) => {
            if (ev.reactTag !== this._reactTag) {
                return;
            }

            let data = ev.data;

            if (ev.type === 'binary') {
                data = base64.toByteArray(ev.data).buffer;
            }

            this.dispatchEvent(new MessageEvent('message', { data }));
        });

        addListener(this, 'dataChannelDidChangeBufferedAmount', (ev: any) => {
            if (ev.reactTag !== this._reactTag) {
                return;
            }

            this._bufferedAmount = ev.bufferedAmount;

            if (this._bufferedAmount < this.bufferedAmountLowThreshold) {
                this.dispatchEvent(new RTCDataChannelEvent('bufferedamountlow', { channel: this }));
            }
        });
    }
}

/**
 * Define the `onxxx` event handlers.
 */
const proto = RTCDataChannel.prototype;

defineEventAttribute(proto, 'bufferedamountlow');
defineEventAttribute(proto, 'close');
defineEventAttribute(proto, 'closing');
defineEventAttribute(proto, 'error');
defineEventAttribute(proto, 'message');
defineEventAttribute(proto, 'open');
