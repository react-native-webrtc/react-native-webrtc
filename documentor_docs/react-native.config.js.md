# Technical Documentation: `react-native.config.js`

## Overview

The `react-native.config.js` file is a configuration file for the React Native CLI. Its primary purpose in this codebase is to detect whether a specific command-line flag (`--use-react-native-macos`) was passed during execution. If the flag is present, it dynamically updates the command-line arguments (`process.argv`) to redirect Metro bundler configuration and exports a configuration object that points to the React Native macOS package.

---

## Key Components

### 1. Strict Mode Directive
```javascript
'use strict';
```
Enables JavaScript strict mode to enforce stricter parsing and error handling in the script.

### 2. Flag Constant
```javascript
const macSwitch = '--use-react-native-macos';
```
Defines a constant string representing the target command-line argument flag: `--use-react-native-macos`.

### 3. Conditional Argument Modification & Export
```javascript
if (process.argv.includes(macSwitch)) {
    process.argv = process.argv.filter(arg => arg !== macSwitch);
    process.argv.push('--config=metro.config.macos.js');
    module.exports = {
        reactNativePath: 'node_modules/react-native-macos'
    };
}
```
Checks for the presence of the `macSwitch` flag within `process.argv` and executes specific argument transformations and module exports if found.

---

## How It Works

1. **Check Command-Line Arguments**: The script checks if `process.argv` contains the string `--use-react-native-macos`.
2. **Filter Out Switch**: If present, it removes `--use-react-native-macos` from `process.argv` using `Array.prototype.filter`.
3. **Inject Metro Config Flag**: It appends `--config=metro.config.macos.js` to `process.argv` to instruct the CLI to use the macOS-specific Metro configuration file.
4. **Export Configuration**: It assigns an object to `module.exports` specifying the custom path to the React Native framework:
   * **`reactNativePath`**: Set to `'node_modules/react-native-macos'`.

*Note: If `--use-react-native-macos` is not included in `process.argv`, no modifications are made to `process.argv`, and `module.exports` remains unset by this block.*