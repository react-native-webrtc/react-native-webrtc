# Technical Documentation: `WebRTCModule+RTCDataChannel.h`

## Overview

The `WebRTCModule+RTCDataChannel.h` header file defines Objective-C categories that extend two classes: `RTCDataChannel` and `WebRTCModule`. These extensions provide properties and methods needed to associate WebRTC data channels with peer connections and to convert data channel states into string representations.

---

## Header Imports

The file relies on two internal header dependencies:

```objc
#import "DataChannelWrapper.h"
#import "WebRTCModule.h"
```

* **`DataChannelWrapper.h`**: Imports the interface and delegate protocol definitions for wrapping WebRTC data channel events and delegate callbacks.
* **`WebRTCModule.h`**: Imports the main interface definition for `WebRTCModule`.

---

## Key Components

### 1. `RTCDataChannel (React)` Category

This category extends the native `RTCDataChannel` class to add React Native context tracking.

```objc
@interface RTCDataChannel (React)

@property(nonatomic, strong) NSNumber *peerConnectionId;

@end
```

#### Properties

| Property Name | Type | Attributes | Description |
| :--- | :--- | :--- | :--- |
| `peerConnectionId` | `NSNumber *` | `nonatomic`, `strong` | Stores the unique identifier of the `RTCPeerConnection` instance associated with this specific `RTCDataChannel`. |

---

### 2. `WebRTCModule (RTCDataChannel)` Category

This category extends `WebRTCModule` to handle data channel operations and conforms to the `DataChannelWrapperDelegate` protocol.

```objc
@interface WebRTCModule (RTCDataChannel)<DataChannelWrapperDelegate>

- (NSString *)stringForDataChannelState:(RTCDataChannelState)state;

@end
```

#### Protocol Conformance
* **`DataChannelWrapperDelegate`**: Indicates that `WebRTCModule` implements callback methods specified by the `DataChannelWrapperDelegate` protocol to handle data channel events.

#### Methods

##### `stringForDataChannelState:`

```objc
- (NSString *)stringForDataChannelState:(RTCDataChannelState)state;
```

* **Purpose**: Converts a native `RTCDataChannelState` enum value into its corresponding human-readable `NSString` representation.
* **Parameters**:
  * `state` (`RTCDataChannelState`): The state enum value of the data channel.
* **Return Value**: An `NSString *` representing the given state.

---

## How It Works

1. **Association**: By adding the `peerConnectionId` property via the `RTCDataChannel (React)` category, the module can map an existing `RTCDataChannel` object directly to its parent peer connection ID.
2. **State Conversion**: The `WebRTCModule (RTCDataChannel)` category provides the `stringForDataChannelState:` method to translate underlying native state enum values (`RTCDataChannelState`) into `NSString` format.
3. **Delegate Implementation**: `WebRTCModule` adopts the `DataChannelWrapperDelegate` protocol to receive notifications and lifecycle events emitted by instances of `DataChannelWrapper`.