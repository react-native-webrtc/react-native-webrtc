console.log("Babel config loaded!");

const path = require('path');
const pkg = require('../../package.json');
const root = path.join(__dirname, '../..');

const alias = {
  '@stream-io/react-native-webrtc': path.join(root, "src/index.ts"),
};

// console.log({ alias });

module.exports = {
  presets: ['module:@react-native/babel-preset'],
  overrides: [
    {
      exclude: /\/node_modules\//,
      plugins: [
        [
          require.resolve('babel-plugin-module-resolver'),
          {
            extensions: ['.tsx', '.ts', '.jsx', '.js', '.json'],
            alias,
          },
        ],
      ],
    },
  ]
};
