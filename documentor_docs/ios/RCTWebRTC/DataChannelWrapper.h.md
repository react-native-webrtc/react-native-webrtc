# Documentation: `ios/RCTWebRTC/DataChannelWrapper.h`

## Overview

The `DataChannelWrapper.h` header file defines the interface for `DataChannelWrapper` and its associated delegate protocol `DataChannelWrapperDelegate`. This class serves as a wrapper around the native WebRTC `RTCDataChannel` object within the iOS implementation of `react-native-webrtc`. 

It bridges the underlying WebRTC data channel instance with React Native by maintaining identifiers (such as `reactTag` and `pcId`) and exposing event callbacks via a delegate protocol.

---

## Header Imports & Annotations

* **Imports**:
  * `<Foundation/Foundation.h>`: Core Objective-C framework types and protocols.
  * `<WebRTC/RTCDataChannel.h>`: WebRTC framework header containing `RTCDataChannel` and `RTCDataBuffer` definitions.
* **Nullability**:
  * Utilizes `NS_ASSUME_NONNULL_BEGIN` and `NS_ASSUME_NONNULL_END` to enforce non-nullability by default for all pointers unless explicitly annotated otherwise.

---

## Protocols

### `DataChannelWrapperDelegate`

A delegate protocol conforming to `<NSObject>` that defines callback methods for handling events originating from a `DataChannelWrapper`.

#### Delegate Methods

```objc
- (void)dataChannelDidChangeState:(DataChannelWrapper *)dataChannelWrapper;
```
* **Description**: Triggered when the underlying WebRTC data channel's state changes (e.g., connecting, open, closing, closed).
* **Parameters**:
  * `dataChannelWrapper`: The `DataChannelWrapper` instance reporting the state change.

---

```objc
- (void)dataChannel:(DataChannelWrapper *)dataChannelWrapper didReceiveMessageWithBuffer:(RTCDataBuffer *)buffer;
```
* **Description**: Triggered when a message is received on the data channel.
* **Parameters**:
  * `dataChannelWrapper`: The `DataChannelWrapper` instance receiving the data.
  * `buffer`: The `RTCDataBuffer` object containing the raw data/payload received.

---

```objc
- (void)dataChannel:(DataChannelWrapper *)dataChannelWrapper didChangeBufferedAmount:(uint64_t)amount;
```
* **Description**: Triggered when the amount of data currently buffered to send changes.
* **Parameters**:
  * `dataChannelWrapper`: The `DataChannelWrapper` instance whose buffer amount changed.
  * `amount`: The updated buffered amount as an unsigned 64-bit integer (`uint64_t`).

---

## Classes

### `DataChannelWrapper`

`DataChannelWrapper` inherits from `NSObject`. It encapsulates an `RTCDataChannel` instance alongside metadata used to identify and map the channel across the native-to-JavaScript boundary.

#### Initializer

```objc
- (instancetype)initWithChannel:(RTCDataChannel *)channel reactTag:(NSString *)tag;
```
* **Description**: Creates and initializes a new `DataChannelWrapper` instance.
* **Parameters**:
  * `channel`: The native `RTCDataChannel` object to wrap.
  * `tag`: A unique `NSString` representing the React Native tag for this data channel.
* **Returns**: An initialized `DataChannelWrapper` instance.

---

#### Properties

| Property | Type | Attributes | Description |
| :--- | :--- | :--- | :--- |
| `pcId` | `NSNumber *` | `nonatomic, nonnull, copy` | The identifier for the Peer Connection (`RTCPeerConnection`) associated with this data channel. |
| `channel` | `RTCDataChannel *` | `nonatomic, nonnull, readonly` | Readonly reference to the wrapped native `RTCDataChannel` object. |
| `reactTag` | `NSString *` | `nonatomic, nonnull, readonly` | Readonly string representing the React Native identifier assigned to this wrapper. |
| `delegate` | `id<DataChannelWrapperDelegate>` | `nonatomic, nullable, weak` | Weak reference to an object implementing `DataChannelWrapperDelegate` to receive data channel event updates. |