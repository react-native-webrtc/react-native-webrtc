'use strict';

const base64 = require('base64-js');
const EventTarget = require('event-target-shim');
const {DeviceEventEmitter, NativeModules} = require('react-native');

const MessageEvent = require('./MessageEvent');
const RTCDataChannelEvent = require('./RTCDataChannelEvent');

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

const dataChannelIds = new Set();

let nextDataChannelId = 0;

const DATA_CHANNEL_EVENTS = [
  'open',
  'message',
  'bufferedamountlow',
  'close',
  'error',
];

class ResourceInUse extends Error {}

class RTCDataChannel extends EventTarget(DATA_CHANNEL_EVENTS) {

  binaryType: 'arraybuffer' = 'arraybuffer'; // we only support 'arraybuffer'
  bufferedAmount: number = 0;
  bufferedAmountLowThreshold: number = 0;
  id: string;
  label: string;
  maxPacketLifeTime: ?number = null;
  maxRetransmits: ?number = null;
  negotiated: boolean = false;
  ordered: boolean = true;
  protocol: string = '';
  readyState: RTCDataChannelState = 'connecting';

  opopen: ?Function;
  onmessage: ?Function
  onbufferedamountlow: ?Function;
  onerror: ?Function;
  onclose: ?Function;

  constructor(peerConnectionId: number, label: string, options?: ?RTCDataChannelInit) {
    super();

    if (options && 'id' in options) {
      if (typeof options.id !== 'number') {
        throw new TypeError('DataChannel id must be a number: ' + options.id);
      }
      if (dataChannelIds.contains(options.id)) {
        throw new ResourceInUse('DataChannel id already in use: ' + options.id);
      }
      this.id = options.id;
    } else {
      this.id = nextDataChannelId++;
    }
    dataChannelIds.add(this.id);

    this.label = label;

    if (options) {
      this.ordered = !!options.ordered;
      this.maxPacketLifeTime = options.maxPacketLifeTime;
      this.maxRetransmits = options.maxRetransmits;
      this.protocol = options.protocol || '';
      this.negotiated = !!options.negotiated;
    }

    WebRTCModule.dataChannelInit(peerConnectionId, this.id, label, options);
    this._registerEvents();
  }

  send(data: string | ArrayBuffer | ArrayBufferView) {
    if (typeof data === 'string') {
      WebRTCModule.dataChannelSend(this.id, data, 'text');
      return;
    }

    if (ArrayBuffer.isView(data)) {
      data = data.buffer;
    }
    if (!(data instanceof ArrayBuffer)) {
      throw new TypeError('Data must be either string, ArrayBuffer, or ArrayBufferView');
    }
    WebRTCModule.dataChannelSend(this.id, base64.fromByteArray(new Uint8Array(data)), 'binary');
  }

  close() {
    if (this.readyState === 'closing' || this.readyState === 'closed') {
      return;
    }
    this.readyState = 'closing';
    WebRTCModule.dataChannelClose(this._id);
  }

  _unregisterEvents() {
    this._subscriptions.forEach(e => e.remove());
    this._subscriptions = [];
  }

  _registerEvents() {
    this._subscriptions = [
      DeviceEventEmitter.addListener('dataChannelStateChanged', ev => {
        if (ev.id !== this.id) {
          return;
        }
        this.readyState = ev.state;
        if (this.readyState === 'open') {
          this.dispatchEvent(new RTCDataChannelEvent('open', {channel: this}));
        }
        if (this.readyState === 'close') {
          this.dispatchEvent(new RTCDataChannelEvent('close', {channel: this}));
          this._unregisterEvents();
        }
      }),
      DeviceEventEmitter.addListener('dataChannelReceiveMessage', ev => {
        if (ev.id !== this.id) {
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

module.exports = RTCDataChannel;
