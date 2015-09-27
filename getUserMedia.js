'use strict';

var getUserMediaImpl = require('./getUserMediaImpl');

navigator.getUserMedia = getUserMediaImpl;
