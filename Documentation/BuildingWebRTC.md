# Building WebRTC

需要使用脚本 `tools/build-webrtc.py` 构建本模块需要的webrtc多平台（mac,ios,android）依赖库。  
需要vpn且带宽要足，命令行和git里需要设置http_proxy,https_proxy

## 构建前的准备

携带参数 `--setup` 运行脚本 build-webrtc.py 将会下载构建所必须的所有工具.  
运行该脚本时必须指定一个空目录;该脚本会在指定的目录下创建构建目录：build_webrtc；所有的源代码，构建后的目标库都在这个目录下。  
`--setup` 这个过程一般只需要执行一次，后面不需要重新执行（除非工具包过时了）  

### iOS 构建环境准备

```
python build-webrtc.py --setup --ios /path/to/blank_dir
```

### Android 构建环境准备

注意:   
1. 需要jdk8. Ubuntu安装java环境 `apt-get install default-jdk-headless`
2. Android版本只能目前只能在linux机器上进行(不要做无畏的尝试)，推荐Ubuntu;

```
python build-webrtc.py --setup --android /path/to/blank_dir
# 如果是第一次，则忽略这一步
```

## 选择要构建的WebRTC版本
当上述步骤完成后，就意味着构建环境准备好了，此时要选择一个webrtc版本把代码下载下来（通常选择最新的稳定版本）

```
cd /path/to/blank_dir/build_webrtc/webrtc/ios/src/
git checkout -b build-M97 refs/remotes/branch-heads/4692
gclient sync -D
```
到此，代码就下载完成了，接下来开始构建!  

## 开始构建
如果你不是第一次构建，本地存在多个分支，当git切换不同的分支后，需要先同步代码:
```
python build-webrtc.py --sync --ios /path/to/blank_dir
```

### iOS
开始构建:

```
python build-webrtc.py --build --ios /path/to/blank_dir
```

所有最终的构建库都在这个目录里: `/path/to/blank_dir/build_webrtc/build/ios/`

### Android
开始构建:

```
python build-webrtc.py --build --android ~/src/
```
所有最终的构建库都在这个目录里: `/path/to/blank_dir/build_webrtc/build/android/`
