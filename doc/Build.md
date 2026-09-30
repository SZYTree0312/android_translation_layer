## Dependencies

An [Alpine container](#alpine-container) is also available with all dependencies installed..

### Debian
```sh
sudo apt install libasound2-dev libavcodec-dev libcap-dev  libdrm-dev libglib2.0-dev libgtk-4-dev libgudev-1.0-dev libopenxr-dev libportal-dev libsqlite3-dev libwebkitgtk-6.0-dev libsecret-1-dev
```

### Fedora
```sh
sudo dnf install java-17-openjdk-devel 'pkgconfig(gtk4)' 'pkgconfig(libbsd)' 'pkgconfig(libportal)' 'pkgconfig(sqlite3)' 'pkgconfig(libwebp)' 'pkgconfig(liblz4)' 'pkgconfig(openxr)' 'pkgconfig(webkitgtk-6.0)' 'pkgconfig(libsecret-1)'
```

### Alpine Edge

Alpine has ATL already packaged in their testing repository. This means that if you are running Alpine edge --- either as your host OS or in a container --- you can install ATL or its development dependencies through `apk`.

You will need to add the testing repository to your apk configuration:

```sh
echo "https://dl-cdn.alpinelinux.org/alpine/edge/testing" | sudo tee -a /etc/apk/repositories
```

#### Installing the ATL package

With this setup installing ATL becomes trivial:

```sh
sudo apk add android-translation-layer
```

#### Installing ATL from source

If you want to build ATL from source, you can take advantage of the packages for its dependencies. With this you can skip the Additional Dependency section below.

```sh
sudo apk add build-base meson java-common openjdk8-jdk \
    pc:alsa pc:glib-2.0 pc:gtk4 pc:gudev-1.0 pc:libportal \
    pc:openxr pc:vulkan pc:webkitgtk-6.0 pc:libsecret-1 ffmpeg-dev \
    bionic_translation-dev art_standalone-dev libandroidfw-dev
```

## Additional Dependencies
### wolfSSL
If your distro ships wolfSSL with JNI enabled already, you can just install the package and skip this step.
```sh
git clone https://github.com/wolfSSL/wolfssl.git
cd wolfssl
git checkout v5.8.2-stable
autoreconf -i
./configure --enable-shared --disable-opensslall --disable-opensslextra --enable-aescbc-length-checks --enable-curve25519 --enable-ed25519 --enable-ed25519-stream --enable-oldtls --enable-base64encode --enable-tlsx --enable-scrypt --disable-examples --enable-crl --enable-jni --enable-sessioncerts
make
sudo make install
```

### bionic_translation
If your distro ships this already (e.g. `bionic_translation` on Alpine), you can just install the package and skip this step.
```sh
git clone https://gitlab.com/android_translation_layer/bionic_translation.git
cd bionic_translation
meson setup builddir
cd builddir
meson compile
sudo meson install
```

### art_standalone
If your distro ships this already (e.g. `art_standalone` on Alpine), you can just install the package and skip this step.
```sh
git clone https://gitlab.com/android_translation_layer/art_standalone.git
cd art_standalone
make ____LIBDIR=lib
sudo make ____LIBDIR=lib install
```
Note: adjust `____LIBDIR` depending on your distro (e.g some distros use `lib32`,`lib` or `lib`,`lib64` for multilib).

### libOpenSLES
Optional (not required for Android Translation Layer itself but some apps depend on it).

If your distro ships this already (e.g. `libopensles-standalone` on Alpine), you can just install the package and skip this step.
```sh
git clone https://gitlab.com/android_translation_layer/libopensles-standalone.git
cd libopensles-standalone
meson setup builddir
cd builddir
meson compile
sudo meson install
```

### Alpine Container

Containerization can be used for a consistent development environment across Linux distributions.

Devcontainers provide an ephemeral environment kept in sync with the repository. If you prefer a persistent environment, consider using Toolbox instead.

Either method will provide an Alpine environment ready for building.

#### Devcontainers

Install Docker Engine: https://docs.docker.com/engine/

Follow instructions for your preferred implementation:
- Visual Studio Code: https://code.visualstudio.com/docs/devcontainers/containers
- Intellij IDEA Ultimate: https://www.jetbrains.com/help/idea/connect-to-devcontainer.html
- DevPod: https://devpod.sh/
- Dev Container CLI: https://github.com/devcontainers/cli

#### Toolbox

Note: A similar method works with Distrobox, a Toolbox alternative available on most Linux distributions.

Install Toolbox:

```sh
sudo dnf install toolbox
```

Build image using podman:
```sh
podman build -t atl_dev .devcontainer
```

Create toolbox:
```sh
toolbox create --image atl_dev atl_dev
```

## Build
```sh
git clone https://gitlab.com/android_translation_layer/android_translation_layer.git
cd android_translation_layer
meson setup builddir
cd builddir
meson compile
```

## Install
```sh
cd builddir
meson install
```
