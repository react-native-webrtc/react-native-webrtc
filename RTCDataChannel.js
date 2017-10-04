'use strict';

import {NativeModules, DeviceEventEmitter} from 'react-native';
import base64 from 'base64-js';
import EventTarget from 'event-target-shim';
import MessageEvent from './MessageEvent';
import RTCDataChannelEvent from './RTCDataChannelEvent';

const {WebRTCModule} = NativeModules;

type RTCDataChannelInit = {
  ordered?: boolean;
  maxPacketLifeTime?: number;
  maxRetransmits?: number;
  protocol?: string;
  negotiated?: boolean;
  id?: number;
  // deprecated:
  maxRetransmitTime?: number,
};

type RTCDataChannelState =
  'connecting' |
  'open' |
  'closing' |
  'closed';

const DATA_CHANNEL_EVENTS = [
  'open',
  'message',
  'bufferedamountlow',
  'close',
  'error',
];

class ResourceInUse extends Error {}

export default class RTCDataChannel extends EventTarget(DATA_CHANNEL_EVENTS) {

  _peerConnectionId: number;

  binaryType: 'arraybuffer' = 'arraybuffer'; // we only support 'arraybuffer'
  bufferedAmount: number = 0;
  bufferedAmountLowThreshold: number = 0;
  id: number;
  label: string;
  maxPacketLifeTime: ?number = null;
  maxRetransmits: ?number = null;
  negotiated: boolean = false;
  ordered: boolean = true;
  protocol: string = '';
  readyState: RTCDataChannelState = 'connecting';

  onopen: ?Function;
  onmessage: ?Function;
  onbufferedamountlow: ?Function;
  onerror: ?Function;
  onclose: ?Function;

  constructor(
      peerConnectionId: number,
      label: string,
      dataChannelDict: RTCDataChannelInit) {
    super();

    this._peerConnectionId = peerConnectionId;

    this.label = label;

    // The standard defines dataChannelDict as optional for
    // RTCPeerConnection#createDataChannel and that is how we have implemented
    // the method in question. However, the method will (1) allocate an
    // RTCDataChannel.id if the caller has not specified a value and (2)
    // pass it to RTCDataChannel's constructor via dataChannelDict.
    // Consequently, dataChannelDict is not optional for RTCDataChannel's
    // constructor.
    this.id = ('id' in dataChannelDict) ? dataChannelDict.id : -1;
    this.ordered = !!dataChannelDict.ordered;
    this.maxPacketLifeTime = dataChannelDict.maxPacketLifeTime;
    this.maxRetransmits = dataChannelDict.maxRetransmits;
    this.protocol = dataChannelDict.protocol || '';
    this.negotiated = !!dataChannelDict.negotiated;

    this._registerEvents();
  }

  send(data: string | ArrayBuffer | ArrayBufferView) {
    if (typeof data === 'string') {
      WebRTCModule.dataChannelSend(this._peerConnectionId, this.id, data, 'text');
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
    WebRTCModule.dataChannelSend(this._peerConnectionId, this.id, base64.fromByteArray(data), 'binary');
  }

  close() {
    if (this.readyState === 'closing' || this.readyState === 'closed') {
      return;
    }
    this.readyState = 'closing';
    WebRTCModule.dataChannelClose(this._peerConnectionId, this.id);
  }

  _unregisterEvents() {
    this._subscriptions.forEach(e => e.remove());
    this._subscriptions = [];
  }

  _registerEvents() {
    this._subscriptions = [
      DeviceEventEmitter.addListener('dataChannelStateChanged', ev => {
        if (ev.peerConnectionId !== this._peerConnectionId
            || ev.id !== this.id) {
          return;
        }
        this.readyState = ev.state;
        if (this.readyState === 'open') {
          this.dispatchEvent(new RTCDataChannelEvent('open', {channel: this}));
        } else if (this.readyState === 'close') {
          this.dispatchEvent(new RTCDataChannelEvent('close', {channel: this}));
          this._unregisterEvents();
        }
      }),
      DeviceEventEmitter.addListener('dataChannelReceiveMessage', ev => {
        if (ev.peerConnectionId !== this._peerConnectionId
            || ev.id !== this.id) {
          return;
        }
        let data = ev.data;
        if (ev.type === 'binary') {
          data = base64.toByteArray(ev.data).buffer;
        }
        this.dispatchEvent(new MessageEvent('message', {data}));
      }),
    ];
  }

}
