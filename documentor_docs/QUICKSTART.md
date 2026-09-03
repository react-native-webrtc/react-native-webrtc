# Quickstart Guide

> **Note:** The provided codebase context contains configuration files and CLI flag logic, but does not include full installation commands (e.g., package manager installation commands) or environment variable specifications. Below is the quickstart information strictly derived from the available configuration snippets.

---

## Supported Platforms

* `macos`
* `ios`
* `android`

---

## Configuration & CLI Options

### Using `react-native-macos`

When running CLI commands or development scripts (such as running integration tests locally or in CI), you can use the macOS flag:

```bash
--use-react-native-macos
```

When passed, the configuration automatically:
* Replaces the default CLI config with `--config=metro.config.macos.js`.
* Sets `reactNativePath` to `node_modules/react-native-macos`.

---

## Configuration Summary

* **Metro Configuration:** 
  * Resolves `react-native` to `node_modules/react-native-macos`.
  * Blacklists `node_modules/react-native/.*`.
* **Babel Configuration:**
  * Uses preset: `module:metro-react-native-babel-preset`.

---

*Note: If additional setup steps, prerequisites, or installation commands are required, they are not present in the repository snippets provided.*