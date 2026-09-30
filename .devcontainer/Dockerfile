FROM quay.io/toolbx-images/alpine-toolbox

RUN apk add alpine-sdk openssh

RUN echo "https://dl-cdn.alpinelinux.org/alpine/edge/testing" | tee -a /etc/apk/repositories

RUN apk add build-base meson java-common openjdk8-jdk \
    pc:alsa pc:glib-2.0 pc:gtk4 pc:gudev-1.0 pc:libportal \
    pc:openxr pc:vulkan pc:webkitgtk-6.0 ffmpeg-dev \
    bionic_translation-dev art_standalone-dev skia-sharp-dev
