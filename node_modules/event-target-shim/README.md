# event-target-shim

[![Build Status](https://travis-ci.org/mysticatea/event-target-shim.svg?branch=master)](https://travis-ci.org/mysticatea/event-target-shim)
[![Coverage Status](https://coveralls.io/repos/mysticatea/event-target-shim/badge.svg?branch=master&service=github)](https://coveralls.io/github/mysticatea/event-target-shim?branch=master)
[![Dependency Status](https://david-dm.org/mysticatea/event-target-shim.svg)](https://david-dm.org/mysticatea/event-target-shim)
[![devDependency Status](https://david-dm.org/mysticatea/event-target-shim/dev-status.svg)](https://david-dm.org/mysticatea/event-target-shim#info=devDependencies)<br>
[![npm version](https://img.shields.io/npm/v/event-target-shim.svg)](https://www.npmjs.com/package/event-target-shim)
[![Downloads/month](https://img.shields.io/npm/dm/event-target-shim.svg)](https://www.npmjs.com/package/event-target-shim)

An implementation of [W3C EventTarget interface](http://www.w3.org/TR/2000/REC-DOM-Level-2-Events-20001113/events.html#Events-EventTarget), plus few extensions.

- This provides `EventTarget` constructor that can inherit for your custom object.
- This provides an utility that defines properties of attribute listeners (e.g. `obj.onclick`).

```js
// The prototype of this class has getters and setters of `onmessage` and `onerror`.
class Foo extends EventTarget("message", "error") {
    //...
}
```

## Installation

```
npm install --save event-target-shim
```

Or download from `dist` directory.

## Usage

### Basic

```js
//-----------------------------------------------------------------------------
// import (with browserify, webpack, etc...).
const EventTarget = require("event-target-shim");

//-----------------------------------------------------------------------------
// define a custom type.
class Foo extends EventTarget {
}

//-----------------------------------------------------------------------------
// add event listeners.
let foo = new Foo();
foo.addEventListener("foo", event => {
    console.log(event.hello);
});
foo.addEventListener("foo", event => {
    if (event.hello !== "hello") {
        // event implements Event interface.
        event.preventDefault();
    }
});

//-----------------------------------------------------------------------------
// dispatch an event.
let event = document.createEvent("CustomEvent");
event.initCustomEvent("foo", /*bubbles*/ false, /*cancelable*/ false, /*detail*/ null);
event.hello = "hello";
foo.dispatchEvent(event);

//-----------------------------------------------------------------------------
// dispatch an event simply (non standard).
foo.dispatchEvent({type: "foo", hello: "hello"});

//-----------------------------------------------------------------------------
// dispatch a cancelable event.
if (!foo.dispatchEvent({type: "foo", cancelable: true, hello: "hey"})) {
    console.log("defaultPrevented");
}

//-----------------------------------------------------------------------------
// If `window.EventTarget` exists, `EventTarget` inherits from `window.EventTarget`.
if (foo instanceof window.EventTarget) {
    console.log("yay!");
}
```

### The Extension for Attribute Listeners

```js
//-----------------------------------------------------------------------------
// import (with browserify, webpack, etc...).
const EventTarget = require("event-target-shim");

//-----------------------------------------------------------------------------
// define a custom type with attribute listeners.
class Foo extends EventTarget("message", "error") {
}
// or non-variadic
class Foo extends EventTarget(["message", "error"]) {
}

//-----------------------------------------------------------------------------
// add event listeners.
let foo = new Foo();
foo.onmessage = event => {
    console.log(event.data);
};
foo.onerror = event => {
    console.log(event.message);
};
foo.addEventListener("message", event => {
    console.log(event.data);
});

//-----------------------------------------------------------------------------
// dispatch a event simply (non standard).
foo.dispatchEvent({type: "message", data: "hello"});
foo.dispatchEvent({type: "error", message: "an error"});
```

### Use in ES5

- Basic.

  ```js
  function Foo() {
      EventTarget.call(this);
  }

  Foo.prototype = Object.create(EventTarget.prototype, {
      constructor: {
          value: Foo,
          configurable: true,
          writable: true
      },

      //....
  });
  ```

- With attribute listeners.

  ```js
  function Foo() {
      EventTarget.call(this);
  }

  Foo.prototype = Object.create(EventTarget("message", "error").prototype, {
  // or
  // Foo.prototype = Object.create(EventTarget(["message", "error"]).prototype, {
      constructor: {
          value: Foo,
          configurable: true,
          writable: true
      },

      //....
  });
  ```

### Use with RequireJS

```js
require(["https://cdn.rawgit.com/mysticatea/event-target-shim/v1.1.0/dist/event-target-shim.min.js"], function(EventTarget) {
    //...
});
```

## API

```ts
declare class EventTarget {
    constructor();
    addEventListener(type: string, listener?: (event: Event) => void, capture: boolean = false): void;
    removeEventListener(type: string, listener?: (event: Event) => void, capture: boolean = false): void;
    dispatchEvent(event: Event | {type: string, cancelable?: boolean}): void;
}

// Define EventTarget type with attribute listeners.
declare function EventTarget(...types: string[]): EventTarget;
declare function EventTarget(types: string[]): EventTarget;
```
