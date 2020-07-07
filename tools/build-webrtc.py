from __future__ import print_function

import argparse
import errno
import os
import shutil
import subprocess
import sys


# Constants

ANDROID_CPU_ABI_MAP = {
    'arm'   : 'armeabi-v7a',
    'arm64' : 'arm64-v8a',
    'x86'   : 'x86',
    'x64'   : 'x86_64'
}
ANDROID_BUILD_CPUS = ['arm', 'arm64', 'x86', 'x64']
IOS_BUILD_ARCHS = ['arm64','x64']

def build_gn_args(platform_args):
    return "--args='" + ' '.join(GN_COMMON_ARGS + platform_args) + "'"

GN_COMMON_ARGS = [
    'is_component_build=false',
    'rtc_libvpx_build_vp9=true',
    'is_debug=%s',
    'target_cpu="%s"'
]

_GN_IOS_ARGS = [
    'enable_dsyms=true',
    'enable_ios_bitcode=%s',
    'ios_deployment_target="11.0"',
    'ios_enable_code_signing=false',
    'target_os="ios"',
    'use_xcode_clang=true'
]
GN_IOS_ARGS = build_gn_args(_GN_IOS_ARGS)

_GN_ANDROID_ARGS = [
    'target_os="android"'
]
GN_ANDROID_ARGS = build_gn_args(_GN_ANDROID_ARGS)


# Utilities

def sh(cmd, env=None):
    print('Running cmd: %s' % cmd)
    try:
        subprocess.check_call(cmd, env=env, shell=True, stdin=sys.stdin, stdout=sys.stdout, stderr=subprocess.STDOUT)
    except subprocess.CalledProcessError as e:
        sys.exit(e.returncode)
    except KeyboardInterrupt:
        pass

def mkdirp(path):
    try:
        os.makedirs(path)
    except OSError as e:
        if e.errno != errno.EEXIST:
            raise

def rmr(path):
    try:
        shutil.rmtree(path)
    except OSError as e:
        if e.errno != errno.ENOENT:
            raise


# The Real Deal

def setup(target_dir, platform):
    mkdirp(target_dir)
    os.chdir(target_dir)

    # Maybe fetch depot_tools
    depot_tools_dir = os.path.join(target_dir, 'depot_tools')
    if not os.path.isdir(depot_tools_dir):
        print('Fetching Chromium depot_tools...')
        sh('git clone https://chromium.googlesource.com/chromium/tools/depot_tools.git')

    # Prepare environment
    env = os.environ.copy()
    env['PATH'] = '%s:%s' % (env['PATH'], depot_tools_dir)

    # Maybe fetch WebRTC
    webrtc_dir = os.path.join(target_dir, 'webrtc', platform)
    if not os.path.isdir(webrtc_dir):
        mkdirp(webrtc_dir)
        os.chdir(webrtc_dir)
        print('Fetching WebRTC for %s...' % platform)
        sh('fetch --nohooks webrtc_%s' % platform, env)

    # Run glient
    sh('gclient sync', env)

    # Install dependencies
    if platform == 'android':
        webrtc_dir = os.path.join(target_dir, 'webrtc', platform, 'src')
        os.chdir(webrtc_dir)
        sh('./build/install-build-deps.sh')


def build(target_dir, platform, debug, bitcode):
    build_dir = os.path.join(target_dir, 'build', platform)
    build_type = 'Debug' if debug else 'Release'
    depot_tools_dir = os.path.join(target_dir, 'depot_tools')
    webrtc_dir = os.path.join(target_dir, 'webrtc', platform, 'src')

    if not os.path.isdir(webrtc_dir):
        print('WebRTC source not found, did you forget to run --setup?')
        sys.exit(1)

    # Prepare environment
    env = os.environ.copy()
    path_parts = [env['PATH'], depot_tools_dir]
    if platform == 'android':
        # Same as . build/android/envsetup.sh
        android_sdk_root = os.path.join(webrtc_dir, 'third_party/android_tools/sdk')
        path_parts.append(os.path.join(android_sdk_root, 'platform-tools'))
        path_parts.append(os.path.join(android_sdk_root, 'tools'))
        path_parts.append(os.path.join(webrtc_dir, 'build/android'))
    env['PATH'] = ':'.join(path_parts)

    os.chdir(webrtc_dir)

    # Run glient
    sh('gclient sync', env)

    # Cleanup old build
    rmr('out')

    # Run GN
    if platform == 'ios':
        for arch in IOS_BUILD_ARCHS:
            gn_out_dir = 'out/%s-%s' % (build_type, arch)
            gn_args = GN_IOS_ARGS % (str(debug).lower(), arch, str(bitcode).lower())
            gn_cmd = 'gn gen %s %s' % (gn_out_dir, gn_args)
            sh(gn_cmd, env)
    else:
        for cpu in ANDROID_BUILD_CPUS:
            gn_out_dir = 'out/%s-%s' % (build_type, cpu)
            gn_args = GN_ANDROID_ARGS % (str(debug).lower(), cpu)
            gn_cmd = 'gn gen %s %s' % (gn_out_dir, gn_args)
            sh(gn_cmd, env)

    # Build with Ninja
    if platform == 'ios':
        for arch in IOS_BUILD_ARCHS:
            gn_out_dir = 'out/%s-%s' % (build_type, arch)
            ninja_cmd = 'ninja -C %s framework_objc' % gn_out_dir
            sh(ninja_cmd, env)
    else:
        for cpu in ANDROID_BUILD_CPUS:
            gn_out_dir = 'out/%s-%s' % (build_type, cpu)
            ninja_cmd = 'ninja -C %s libwebrtc libjingle_peerconnection_so' % gn_out_dir
            sh(ninja_cmd, env)

    # Cleanup build dir
    rmr(build_dir)
    mkdirp(build_dir)

    # Copy build artifacts to build directory
    if platform == 'ios':
        # Framework
        gn_out_dir = 'out/%s-%s' % (build_type, IOS_BUILD_ARCHS[0])
        shutil.copytree(os.path.join(gn_out_dir, 'WebRTC.framework'), os.path.join(build_dir, 'WebRTC.framework'))
        out_lib_path = os.path.join(build_dir, 'WebRTC.framework', 'WebRTC')
        os.unlink(out_lib_path)
        slice_paths = [os.path.join('out/%s-%s' % (build_type, arch), 'WebRTC.framework', 'WebRTC') for arch in IOS_BUILD_ARCHS]
        sh('lipo %s -create -output %s' % (' '.join(slice_paths), out_lib_path))
        # dSYM
        shutil.copytree(os.path.join(gn_out_dir, 'WebRTC.dSYM'), os.path.join(build_dir, 'WebRTC.dSYM'))
        out_dsym_path = os.path.join(build_dir, 'WebRTC.dSYM', 'Contents', 'Resources', 'DWARF', 'WebRTC')
        os.unlink(out_dsym_path)
        dsym_slice_paths = [os.path.join('out/%s-%s' % (build_type, arch), 'WebRTC.dSYM', 'Contents', 'Resources', 'DWARF', 'WebRTC') for arch in IOS_BUILD_ARCHS]
        sh('lipo %s -create -output %s' % (' '.join(dsym_slice_paths), out_dsym_path))
    else:
        gn_out_dir = 'out/%s-%s' % (build_type, ANDROID_BUILD_CPUS[0])
        shutil.copy(os.path.join(gn_out_dir, 'lib.java/sdk/android/libwebrtc.jar'), build_dir)

        for cpu in ANDROID_BUILD_CPUS:
            lib_dir = os.path.join(build_dir, 'lib', ANDROID_CPU_ABI_MAP[cpu])
            mkdirp(lib_dir)
            gn_out_dir = 'out/%s-%s' % (build_type, cpu)
            shutil.copy(os.path.join(gn_out_dir, 'libjingle_peerconnection_so.so'), lib_dir)

        os.chdir(build_dir)
        sh('jar cvfM libjingle_peerconnection.so.jar lib')
        rmr('lib')


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument('dir', help='Target directory')
    parser.add_argument('--setup', help='Prepare the target directory for building', action='store_true')
    parser.add_argument('--build', help='Build WebRTC in the target directory', action='store_true')
    parser.add_argument('--ios', help='Use iOS as the target platform', action='store_true')
    parser.add_argument('--android', help='Use Android as the target platform', action='store_true')
    parser.add_argument('--debug', help='Make a Debug build (defaults to false)', action='store_true')
    parser.add_argument('--bitcode', help='Enable bitcode (defaults to false)', action='store_true')

    args = parser.parse_args()

    if not (args.setup or args.build):
        print('--setup or --build must be specified!')
        sys.exit(1)

    if args.setup and args.build:
        print('--setup and --build cannot be specified at the same time!')
        sys.exit(1)

    if not (args.ios or args.android):
        print('--ios or --android must be specified!')
        sys.exit(1)

    if args.ios and args.android:
        print('--ios and --android cannot be specified at the same time!')
        sys.exit(1)

    if not os.path.isdir(args.dir):
        print('The specified directory does not exist!')
        sys.exit(1)

    target_dir = os.path.abspath(os.path.join(args.dir, 'build_webrtc'))
    platform = 'ios' if args.ios else 'android'

    if args.setup:
        setup(target_dir, platform)
        print('WebRTC setup for %s completed in %s' % (platform, target_dir))
        sys.exit(0)

    if args.build:
        build(target_dir, platform, args.debug, args.bitcode)
        print('WebRTC build for %s completed in %s' % (platform, target_dir))
        sys.exit(0)

