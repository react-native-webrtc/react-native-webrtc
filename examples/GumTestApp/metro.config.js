const path = require('path');
const { getDefaultConfig } = require('@react-native/metro-config');
const exclusionList = require('metro-config/src/defaults/exclusionList');
const pkg = require('../../package.json');

const root = path.join(__dirname, '../..');

const config = getDefaultConfig(__dirname);

config.watchFolders = [root];

const modules = [
  // AssetsRegistry is used internally by React Native to handle asset imports
  // This needs to be a singleton so all assets are registered to a single registry
  '@react-native/assets-registry',
  ...Object.keys({ ...pkg.peerDependencies }),
];

const extraNodeModules = modules.reduce((acc, name) => {
  acc[name] = path.join(__dirname, 'node_modules', name);
  return acc;
}, {});

extraNodeModules[pkg.name] = path.join(root);

// We need to make sure that only one version is loaded for peerDependencies
// So we block them at the root, and alias them to the versions in example project's node_modules
const blockList = modules.map(
  (m) =>
    new RegExp(`^${escape(path.join(root, 'node_modules', m))}\\/.*$`)
)

console.log('blockList', blockList);
console.log('extraNodeModules', extraNodeModules);

config.resolver.extraNodeModules = extraNodeModules;

config.resolver.blockList = exclusionList(blockList);

/**
 * Metro configuration
 * https://facebook.github.io/metro/docs/configuration
 *
 * @type {import('metro-config').MetroConfig}
 */
module.exports = config;