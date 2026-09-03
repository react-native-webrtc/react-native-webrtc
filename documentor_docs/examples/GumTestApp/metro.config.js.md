# Metro Configuration Documentation: `examples/GumTestApp/metro.config.js`

## Overview

The `metro.config.js` file serves as the configuration file for **Metro**, the JavaScript bundler used by React Native applications. Located in the `examples/GumTestApp` directory, this file configures specific transformation behavior applied by Metro when bundling JavaScript code for the `GumTestApp` application.

---

## File Details

* **File Path:** `examples/GumTestApp/metro.config.js`
* **Module Format:** CommonJS (`module.exports`)

---

## Configuration Breakdown

The file exports a single configuration object containing a `transformer` block.

```javascript
module.exports = {
  transformer: {
    getTransformOptions: async () => ({
      transform: {
        experimentalImportSupport: false,
        inlineRequires: false,
      },
    }),
  },
};
```

### Key Properties

#### 1. `module.exports`
Exports the configuration object so that Metro can read it at build time or during development server startup.

#### 2. `transformer`
An object defining settings for Metro's code transformation pipeline.

#### 3. `getTransformOptions`
* **Type:** `async () => Promise<Object>`
* **Description:** An asynchronous function that returns configuration options specifically for code transformation. Metro invokes this function when processing files.

#### 4. `transform` Options Object
Returned inside `getTransformOptions`, this object specifies the following flags:

* **`experimentalImportSupport: false`**
  * **Value:** `false`
  * **Effect:** Disables experimental ES module import support transformations.

* **`inlineRequires: false`**
  * **Value:** `false`
  * **Effect:** Disables inline `require` calls. Top-level imports or require statements will not be automatically transformed into deferred inline `require` calls inside functions.

---

## How It Works

1. **Initialization:** When the React Native CLI or Metro bundler starts up for `GumTestApp`, it loads `metro.config.js`.
2. **Transform Request:** When Metro transforms JavaScript/TypeScript files, it calls `getTransformOptions()`.
3. **Applying Settings:** Metro receives the returned `transform` object with `experimentalImportSupport` and `inlineRequires` set to `false`, ensuring that code is bundled according to these specific transformation settings.