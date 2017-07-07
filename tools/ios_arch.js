'use strict';

const fs = require('fs');
const exec = require('child_process').execSync;

const WEBRTC_BIN_PATH = '../ios/WebRTC.framework';
const ARCH_TYPES = ['i386', 'x86_64', 'armv7', 'arm64'];

/* === Example to strip simulator archs for Apple Store Submission ===
 *
 * Step 1. extract all archs first
 *    `node ios_arch.js --extract`
 * Step 2. re-package binary without simulator archs
 *    `node ios_arch.js --device`
 * 
 * That's it!
 * Remember to delete generated/backed up files (WebRTC-*) from step 1 if needed
 *
 * === How to Verify ===
 *
 * You can check current archs use this command:
 *     `file $YOUR_PATH/WebRTC.framework/WebRTC`
 */

// TODO: consider add a handy option to extract/package/delete automatically

if (process.argv[2] === '--extract' || process.argv[2] === '-e') {
  console.log('Extracting...');
  // extract all archs, you might want to delete it later.
  ARCH_TYPES.forEach(elm => {
    exec(`lipo -extract ${elm} WebRTC -o WebRTC-${elm}`, {cwd: WEBRTC_BIN_PATH});
  });
  exec('cp WebRTC WebRTC-all', {cwd: WEBRTC_BIN_PATH}); // make a backup
} else if (process.argv[2] === '--simulator' || process.argv[2] === '-s') {
  // re-package simulator related archs only. ( i386, x86_64 )
  console.log('Compiling simulator...');
  exec(`lipo -o WebRTC -create WebRTC-x86_64 WebRTC-i386`, {cwd: WEBRTC_BIN_PATH});
} else if (process.argv[2] === '--device' || process.argv[2] === '-d') {
  // re-package device related archs only. ( armv7, arm64 )
  console.log('Compiling device...');
  exec(`lipo -o WebRTC -create WebRTC-armv7 WebRTC-arm64`, {cwd: WEBRTC_BIN_PATH});
} else {
  console.log('Unknow options');
}

console.log(exec('ls -ahl | grep WebRTC-', {cwd: WEBRTC_BIN_PATH}).toString().trim());
console.log('Done! Remember to delete generated files ("WebRTC-*") if needed.');
