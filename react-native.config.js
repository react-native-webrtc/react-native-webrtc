'use strict';

module.exports = {
    project: {
        android: {
            unstable_reactLegacyComponentNames: [ 'RTCVideoView' ]
        },
        ios: {
            unstable_reactLegacyComponentNames: [ 'RTCVideoView' ]
        }
    }
};

const macSwitch = '--use-react-native-macos';

if (process.argv.includes(macSwitch)) {
    process.argv = process.argv.filter(arg => arg !== macSwitch);
    process.argv.push('--config=metro.config.macos.js');
    module.exports.reactNativePath = 'node_modules/react-native-macos';
}
