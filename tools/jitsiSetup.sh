cd /react-native-webrtc/tools
mkdir output

sudo python build-webrtc.py --setup --android output
sudo python build-webrtc.py --sync --android output

cd output/build_webrtc/webrtc/android/src/ 
git remote add titusOrigin https://github.com/tmoldovan8x8/webrtc.git
git fetch titusOrigin
git checkout titusOrigin/feature/e2ee_add_encryptor_decryptor
cd ../../../../..

