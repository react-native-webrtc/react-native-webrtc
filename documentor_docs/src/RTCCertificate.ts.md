# Technical Documentation: `src/RTCCertificate.ts`

## Overview

The `src/RTCCertificate.ts` module defines a data model for representing an RTC (Real-Time Communication) certificate, including its unique identifier, expiration timestamp, and associated cryptographic fingerprints. 

It exposes a custom TypeScript type `RTCCertificateFingerprint` and a default exported class `RTCCertificate`.

---

## Types

### `RTCCertificateFingerprint`

Represents a cryptographic fingerprint associated with the certificate.

```typescript
export type RTCCertificateFingerprint = {
    algorithm: string;
    value: string;
};
```

#### Properties

| Property | Type | Description |
| :--- | :--- | :--- |
| `algorithm` | `string` | The hash algorithm used to generate the fingerprint (e.g., `'sha-256'`). |
| `value` | `string` | The string representation of the fingerprint hash value. |

---

## Classes

### `RTCCertificate` (Default Export)

The `RTCCertificate` class encapsulates the details of an RTC certificate.

```typescript
export default class RTCCertificate
```

#### Internal Class Properties

| Property | Type | Description |
| :--- | :--- | :--- |
| `_expires` | `number` | Internal field storing the expiration timestamp. |
| `_fingerprints` | `RTCCertificateFingerprint[]` | Internal field storing an array of certificate fingerprints. |
| `_id` | `string` | Internal field storing the certificate's unique identifier. |

---

### Constructor

#### `constructor(info)`

Initializes a new instance of `RTCCertificate`.

```typescript
constructor(info: { 
    certificateId: string, 
    expires: number, 
    fingerprints: RTCCertificateFingerprint[] 
})
```

##### Parameters

* `info`: An object containing the required properties to populate the certificate instance:
  * `info.certificateId` (`string`): Assigned to internal property `_id`.
  * `info.expires` (`number`): Assigned to internal property `_expires`.
  * `info.fingerprints` (`RTCCertificateFingerprint[]`): Assigned to internal property `_fingerprints`.

---

### Getters and Instance Methods

#### `expires` (Getter)

Accessor that retrieves the certificate's expiration timestamp.

```typescript
get expires(): number
```

* **Returns**: `number` - The value stored in `_expires`.

---

#### `getFingerprints()`

Retrieves the list of fingerprints associated with the certificate.

```typescript
getFingerprints(): RTCCertificateFingerprint[]
```

* **Returns**: `RTCCertificateFingerprint[]` - An array of fingerprint objects stored in `_fingerprints`.

---

## Usage Example

```typescript
import RTCCertificate, { RTCCertificateFingerprint } from './src/RTCCertificate';

// Define fingerprints
const fingerprints: RTCCertificateFingerprint[] = [
    {
        algorithm: 'sha-256',
        value: 'AA:BB:CC:DD:EE:FF:00:11:22:33:44:55:66:77:88:99:00:11:22:33:44:55:66:77:88:99:AA:BB:CC:DD:EE:FF'
    }
];

// Instantiate the certificate
const cert = new RTCCertificate({
    certificateId: 'cert-12345',
    expires: 1735689600000,
    fingerprints: fingerprints
});

// Access the expiration timestamp via getter
console.log(cert.expires); // Output: 1735689600000

// Access fingerprints via method
const certFingerprints = cert.getFingerprints();
console.log(certFingerprints[0].algorithm); // Output: 'sha-256'
```