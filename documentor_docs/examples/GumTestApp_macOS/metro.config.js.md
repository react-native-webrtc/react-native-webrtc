# Technical Documentation: `examples/GumTestApp_macOS/metro.config.js`

## Overview

The `examples/GumTestApp_macOS/metro.config.js` file provides configuration settings for **Metro**, the JavaScript bundler used by React Native. This file defines options for the code transformation pipeline used when building or running the `GumTestApp_macOS` target application.

---

## File Details

* **File Path:** `examples/GumTestApp_macOS/metro.config.js`
* **Module System:** CommonJS (`module.exports`)

---

## Structure and Key Components

The file exports a single configuration object via `module.exports`. Below is a breakdown of the properties defined in the object:

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

### 1. `transformer`
The `transformer` object configures how Metro converts source code files into bundled JavaScript.

### 2. `transformer.getTransformOptions`
* **Type:** `async` function
* **Description:** An asynchronous function invoked by Metro to retrieve transformation-specific settings during the bundling process.
* **Return Value:** Resolves to an object containing a `transform` key.

### 3. `transform` Configuration Object
The object returned by `getTransformOptions` configures specific code transformation flags:

* **`experimentalImportSupport` (`false`)**:
  * Setting this to `false` disables Metro's experimental support for optimizing and syntax-transforming ES import statements.
* **`inlineRequires` (`false`)**:
  * Setting this to `false` disables the automatic conversion of top-level `import` or `require` calls into inline `require()` calls inside execution scopes. Module imports remain at their declared scope level during transformation.

---

## How It Works

1. When Metro starts up for the `GumTestApp_macOS` project, it reads `metro.config.js`.
2. During the code transformation phase, Metro calls the `getTransformOptions()` asynchronous function.
3. The function resolves with the configuration object specifying `experimentalImportSupport: false` and `inlineRequires: false`.
4. Metro applies these explicit settings to its transformation pipeline when processing files for the bundle.