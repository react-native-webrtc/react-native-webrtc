# Technical Documentation: `src/Constraints.ts`

## Overview

The `src/Constraints.ts` module defines TypeScript type definitions used to represent constraints for media tracks (such as audio or video streams). It provides a structured format to specify desired, exact, or range-based boundaries for media properties like dimensions, frame rates, and device identifiers.

---

## Purpose

The primary purpose of this file is to provide type safety and IntelliSense support when building constraint objects for media streams. It models the standard criteria used to configure media track properties, allowing properties to be defined either as simple primitive values or as constraint objects containing specific parameters (e.g., `exact`, `ideal`, `min`, `max`).

---

## Key Components

The file contains three type definitions: one exported primary type and two unexported helper types.

### 1. `MediaTrackConstraints` (Exported)

This is the main exported type defining the overall shape of the constraints object. All properties within this type are optional.

```typescript
export type MediaTrackConstraints = {
    width?: ConstrainNumber;
    height?: ConstrainNumber;
    frameRate?: ConstrainNumber;
    facingMode?: ConstrainString;
    deviceId?: ConstrainString;
    groupId?: ConstrainString;
}
```

#### Properties:

| Property | Type | Description |
| :--- | :--- | :--- |
| `width` | `ConstrainNumber` *(optional)* | Specifies constraints for the video track width (in pixels). |
| `height` | `ConstrainNumber` *(optional)* | Specifies constraints for the video track height (in pixels). |
| `frameRate` | `ConstrainNumber` *(optional)* | Specifies constraints for the track frame rate (in frames per second). |
| `facingMode` | `ConstrainString` *(optional)* | Specifies constraints for the camera direction (e.g., facing direction). |
| `deviceId` | `ConstrainString` *(optional)* | Specifies constraints for a unique device identifier. |
| `groupId` | `ConstrainString` *(optional)* | Specifies constraints for a group identifier shared across related devices. |

---

### 2. `ConstrainNumber` (Internal)

A helper type used for numeric constraints (`width`, `height`, `frameRate`).

```typescript
type ConstrainNumber = number | {
    exact?: number,
    ideal?: number,
    max?: number,
    min?: number,
}
```

It allows a value to be set in one of two ways:
1. **Direct primitive:** A single `number` value.
2. **Constraint object:** An object containing any combination of the following optional numeric criteria:
   - `exact`: A mandatory exact `number` requirement.
   - `ideal`: A preferred `number` target.
   - `max`: The maximum acceptable `number`.
   - `min`: The minimum acceptable `number`.

---

### 3. `ConstrainString` (Internal)

A helper type used for string constraints (`facingMode`, `deviceId`, `groupId`).

```typescript
type ConstrainString = string | {
    exact?: string,
    ideal?: string,
}
```

It allows a value to be set in one of two ways:
1. **Direct primitive:** A single `string` value.
2. **Constraint object:** An object containing any combination of the following optional string criteria:
   - `exact`: A mandatory exact `string` requirement.
   - `ideal`: A preferred `string` target.

---

## How It Works

This file contains type declarations only and produces no runtime JavaScript code output upon compilation. It is used exclusively by TypeScript at compile time to validate constraint objects.

### Valid Object Shape Examples

Because properties are optional and support union types, objects adhering to `MediaTrackConstraints` can take several forms:

#### Using Primitive Values
```typescript
import { MediaTrackConstraints } from './Constraints';

const simpleConstraints: MediaTrackConstraints = {
    width: 1920,
    height: 1080,
    frameRate: 30,
    facingMode: 'user'
};
```

#### Using Constraint Objects
```typescript
import { MediaTrackConstraints } from './Constraints';

const detailedConstraints: MediaTrackConstraints = {
    width: { min: 640, ideal: 1280, max: 1920 },
    height: { min: 480, ideal: 720, max: 1080 },
    frameRate: { ideal: 60 },
    deviceId: { exact: "device-id-string" }
};
```

#### Mixed Usage
```typescript
import { MediaTrackConstraints } from './Constraints';

const mixedConstraints: MediaTrackConstraints = {
    width: 1280,
    frameRate: { min: 15, max: 30 },
    facingMode: { ideal: "environment" }
};
```