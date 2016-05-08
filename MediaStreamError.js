'use strict';

class MediaStreamError {

  name: string;
  message: ?string;
  constraintName: ?string;

  constructor(error) {
    this.name = error.name;
    this.message = error.message;
    this.constraintName = error.constraintName;
  }
}

module.exports = MediaStreamError;
