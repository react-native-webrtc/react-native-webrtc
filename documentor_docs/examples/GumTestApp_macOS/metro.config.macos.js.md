# Technical Documentation: `examples/GumTestApp_macOS/metro.config.macos.js`

## Overview

The `metro.config.macos.js` file is a Metro bundler configuration file designed for development purposes, such as running integration tests during local development or within Continuous Integration (CI) environments. 

Its primary function is to configure how Metro resolves modules for the macOS target, specifically re-routing imports of standard `react-native` to the `react-native-macos` package and blacklisting standard `react-native` files to prevent module resolution conflicts.

---

## Dependencies & Imports

The file imports two dependencies using Node.js CommonJS `require` statements:

1. **`path`**: Node.js built-in utility module used for resolving file system paths.
2. **`blacklist`**: Imported from `metro-config/src/defaults/blacklist`. A helper function provided by Metro to create regular expressions that exclude specific file paths from being processed by the bundler.

---

## Key Components & Constants

### `rnmPath`
```javascript
const rnmPath = path.resolve(__dirname, 'node_modules/react-native-macos');
```
* **Type**: `string` (Absolute file path)
* **Description**: Resolves the absolute path to the `react-native-macos` dependency located in the project's `node_modules` directory relative to the configuration file (`__dirname`).

---

## Configuration Export

The module exports a configuration object via `module.exports` containing a `resolver` configuration block for Metro:

```javascript
module.exports = {
  resolver: {
    extraNodeModules: {
      'react-native': rnmPath,
    },
    platforms: ['macos', 'ios', 'android'],
    blacklistRE: blacklist([/node_modules\/react-native\/.*/]),
  },
};
```

### `resolver` Configuration Options

1. **`extraNodeModules`**
   * **Value**: `{ 'react-native': rnmPath }`
   * **Purpose**: Maps any import statement requesting `'react-native'` directly to the path defined by `rnmPath` (`node_modules/react-native-macos`). This ensures that imports targeting standard React Native use the macOS-specific implementation instead.

2. **`platforms`**
   * **Value**: `['macos', 'ios', 'android']`
   * **Purpose**: Specifies the list of target platform extensions that Metro should support when resolving platform-specific files (e.g., `.macos.js`, `.ios.js`, `.android.js`).

3. **`blacklistRE`**
   * **Value**: `blacklist([/node_modules\/react-native\/.*/])`
   * **Purpose**: Applies a regular expression to ignore and blacklist any files matching the path pattern `/node_modules/react-native/.*`. This prevents Metro from indexing or bundling standard `react-native` modules, ensuring no conflicts occur with `react-native-macos`.

---

## How It Works

1. When Metro is launched using this configuration file, it initializes the module resolver using the exported `resolver` object.
2. **Path Resolution Setup**: Metro computes `rnmPath` to locate the absolute path of `react-native-macos`.
3. **Module Substitution**: Whenever code requests `require('react-native')` or `import ... from 'react-native'`, `extraNodeModules` redirects Metro to look inside `node_modules/react-native-macos`.
4. **File Exclusion**: `blacklistRE` instructs Metro's crawler to ignore files located within `node_modules/react-native/`.
5. **Platform Resolution**: The bundler includes `'macos'` alongside standard mobile platforms (`'ios'`, `'android'`) when resolving platform-specific extensions.