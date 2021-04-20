FROM debian:stable-slim

ENV DEBIAN_FRONTEND=noninteractive

#This is required fo bypassing the tzdata installation
ENV TZ=Europe/Bucharest
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

#Install packages
RUN apt-get update \
	&& apt-get install -y sudo \
				git \
				python \
				curl \
				lsb-release \
				vim \
				ninja-build \
				pkg-config \
				gcc-arm-linux-gnueabihf \
				g++-8-arm-linux-gnueabihf \
				ca-certificates \
	&& apt-get clean

#Install JAVA
RUN mkdir -p /usr/share/man/man1
WORKDIR /opt
RUN curl -L -o openjdk.tar.gz https://download.java.net/java/GA/jdk11/13/GPL/openjdk-11.0.1_linux-x64_bin.tar.gz \
	&& mkdir jdk \
	&& tar zxf openjdk.tar.gz -C jdk --strip-components=1 \
	&& ln -sf /opt/jdk/bin/* /usr/local/bin/

# clone react-native-webrtc repository
WORKDIR /
RUN git clone https://github.com/react-native-webrtc/react-native-webrtc.git
WORKDIR /react-native-webrtc/tools
RUN mkdir output