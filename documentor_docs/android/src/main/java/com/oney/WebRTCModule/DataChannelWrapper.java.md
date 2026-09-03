# Technical Documentation Guide: `DataChannelWrapper.java`

**File Path:** `android/src/main/java/com/oney/WebRTCModule/DataChannelWrapper.java`

---

## 1. Overview

`DataChannelWrapper` is a package-private helper class in the `com.oney.WebRTCModule` package. It acts as a bridge between the native WebRTC `org.webrtc.DataChannel` observer interface and the React Native module environment (`WebRTCModule`).

It implements the `DataChannel.Observer` interface to listen for underlying WebRTC data channel events—such as state changes, message receipts, and buffer changes—transforms the event payloads into React Native `WritableMap` objects, and emits them to the JavaScript layer using `WebRTCModule.sendEvent()`.

---

## 2. Class Signature

```java
package com.oney.WebRTCModule;

// ... imports ...

class DataChannelWrapper implements DataChannel.Observer
```

* **Access Modifier:** Package-private
* **Interfaces Implemented:** `org.webrtc.DataChannel.Observer`

---

## 3. Member Variables

| Variable Name | Type | Description |
| :--- | :--- | :--- |
| `reactTag` | `String` | Unique identifier used by the React Native layer to reference this specific data channel. |
| `mDataChannel` | `DataChannel` | The underlying native WebRTC `DataChannel` instance being wrapped and observed. |
| `peerConnectionId` | `int` | The ID of the associated `PeerConnection` object that created or owns this data channel. |
| `webRTCModule` | `WebRTCModule` | Reference to the core React Native module instance used to send events back to JavaScript. |

---

## 4. Constructor

```java
DataChannelWrapper(WebRTCModule webRTCModule, int peerConnectionId, String reactTag, DataChannel dataChannel)
```

### Parameters:
* **`webRTCModule`** (`WebRTCModule`): The instance responsible for dispatching events to React Native.
* **`peerConnectionId`** (`int`): Identifier of the parent peer connection.
* **`reactTag`** (`String`): Identifier for the React Native instance corresponding to this data channel.
* **`dataChannel`** (`DataChannel`): The native WebRTC `DataChannel` object to observe.

---

## 5. Helper Methods

### `getDataChannel()`
```java
public DataChannel getDataChannel()
```
* **Returns:** The underlying `DataChannel` instance (`mDataChannel`).

### `getReactTag()`
```java
public String getReactTag()
```
* **Returns:** The `reactTag` identifier string associated with this wrapper.

### `dataChannelStateString()`
```java
@Nullable
public String dataChannelStateString(DataChannel.State dataChannelState)
```
Maps WebRTC `DataChannel.State` enum values to lower-case string representations expected by the React Native layer.

* **State Mapping:**
  * `DataChannel.State.CONNECTING` $\rightarrow$ `"connecting"`
  * `DataChannel.State.OPEN` $\rightarrow$ `"open"`
  * `DataChannel.State.CLOSING` $\rightarrow$ `"closing"`
  * `DataChannel.State.CLOSED` $\rightarrow$ `"closed"`
* **Returns:** String representation of the state, or `null` if the state does not match any listed enum value.

---

## 6. Observer Interface Implementations (Event Forwarding)

`DataChannelWrapper` overrides three callbacks defined in `DataChannel.Observer`. Each method converts native event data into a `WritableMap` and emits an event via `webRTCModule.sendEvent()`.

---

### 1. `onBufferedAmountChange(long amount)`

Fired when the data channel's buffered amount of outbound data changes.

* **Event Name:** `dataChannelDidChangeBufferedAmount`
* **Map Parameters:**
  * `"reactTag"` (`String`): The React Native tag.
  * `"peerConnectionId"` (`int`): Parent connection ID.
  * `"bufferedAmount"` (`double`): The new buffered amount, converted from `long` to `double`.

```java
@Override
public void onBufferedAmountChange(long amount) {
    WritableMap params = Arguments.createMap();
    params.putString("reactTag", reactTag);
    params.putInt("peerConnectionId", peerConnectionId);
    params.putDouble("bufferedAmount", Long.valueOf(amount).doubleValue());

    webRTCModule.sendEvent("dataChannelDidChangeBufferedAmount", params);
}
```

---

### 2. `onMessage(DataChannel.Buffer buffer)`

Fired when a message is received on the data channel.

#### Buffer Extraction Logic:
1. Checks if `buffer.data` (a `ByteBuffer`) has a supporting backing array (`hasArray()`).
2. If `true`, extracts `buffer.data.array()`.
3. If `false`, allocates a byte array equal to `buffer.data.remaining()` bytes and copies the data using `buffer.data.get(bytes)`.

#### Data Type Handling:
* **Binary Messages (`buffer.binary == true`):**
  * Type set to `"binary"`.
  * Encodes the byte array into a Base64 string without line wraps (`Base64.NO_WRAP`).
* **Text Messages (`buffer.binary == false`):**
  * Type set to `"text"`.
  * Decodes the byte array into a String using `StandardCharsets.UTF_8`.

* **Event Name:** `dataChannelReceiveMessage`
* **Map Parameters:**
  * `"reactTag"` (`String`): The React Native tag.
  * `"peerConnectionId"` (`int`): Parent connection ID.
  * `"type"` (`String`): `"binary"` or `"text"`.
  * `"data"` (`String`): Base64 string for binary data, UTF-8 string for text.

```java
@Override
public void onMessage(DataChannel.Buffer buffer) {
    // ... payload extraction and conversion ...
    webRTCModule.sendEvent("dataChannelReceiveMessage", params);
}
```

---

### 3. `onStateChange()`

Fired when the execution state of `mDataChannel` changes (e.g., from `CONNECTING` to `OPEN`).

* **Event Name:** `dataChannelStateChanged`
* **Map Parameters:**
  * `"reactTag"` (`String`): The React Native tag.
  * `"peerConnectionId"` (`int`): Parent connection ID.
  * `"id"` (`int`): The integer ID of the native data channel (`mDataChannel.id()`).
  * `"state"` (`String`): String representation of current state obtained via `dataChannelStateString(mDataChannel.state())`.

```java
@Override
public void onStateChange() {
    WritableMap params = Arguments.createMap();
    params.putString("reactTag", reactTag);
    params.putInt("peerConnectionId", peerConnectionId);
    params.putInt("id", mDataChannel.id());
    params.putString("state", dataChannelStateString(mDataChannel.state()));

    webRTCModule.sendEvent("dataChannelStateChanged", params);
}
```

---

## 7. Event Summary Reference Table

| Native Observer Callback | React Native Event Name | Payload Structure |
| :--- | :--- | :--- |
| `onBufferedAmountChange` | `dataChannelDidChangeBufferedAmount` | `{ reactTag: String, peerConnectionId: Int, bufferedAmount: Double }` |
| `onMessage` | `dataChannelReceiveMessage` | `{ reactTag: String, peerConnectionId: Int, type: "binary" \| "text", data: String }` |
| `onStateChange` | `dataChannelStateChanged` | `{ reactTag: String, peerConnectionId: Int, id: Int, state: String }` |