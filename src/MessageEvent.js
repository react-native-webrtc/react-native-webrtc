'use strict';

export default class MessageEvent {
  type: string;
  data: string | ArrayBuffer | Blob;
  origin: string;
  constructor(type, eventInitDict) {
    this.type = type.toString();
    Object.assign(this, eventInitDict);
  }
}
