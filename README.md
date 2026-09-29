<div align="center">
  <p>
    <!---<img src="docs/logo.svg" alt="Logo" width="200em"> --->
    <img src="doc/logo.svg" alt="Logo" width="200em">

  </p>
  <p align="center">
    <h1 style="border: none;">
        Android Translation Layer (ATL)
    </h1>
    <h2 style="border: none;">
    Wine for Android
    </h2>
    <a href="https://gitlab.com/android_translation_layer/android_translation_layer/issues">Report Bug</a>
    ·
    <a href="https://gitlab.com/android_translation_layer/android_translation_layer/issues">Request Feature</a>
  </p>
</div>

## Table of Contents

[TOC]

## About the Project

![Angry Birds 3.2.0, Worms 2 Armageddon, and Gravity Defied running side by side by side](https://gitlab.com/android_translation_layer/android_translation_layer/-/raw/master/screenshot.png)

ATL is a translation layer that allows running Android apps on a Linux system. It is different from projects such as Waydroid in that it does not rely on a virtual machine running Android in the background. Instead our philosophy is to reuse as much of the standard Linux stack as possible, decoupling the app from android as early as possible.

### Powered By

ATL is powered by the following projects:

- [bionic_translation](https://gitlab.com/android_translation_layer/bionic_translation/) to translate from bionic to libc/musel
- and more...

## Getting Started

<!--
 * [Flathub Installation](#flathub-installation)

### ATL in Action

Try it yourself by running XXX on your device. Simply install it from flathub!

[![Get it on Flathub][flathub-badge]][flathub]

-->

### Building ATL

See our [build documentation](https://gitlab.com/android_translation_layer/android_translation_layer/-/blob/master/doc/Build.md) to get started. We provide information to build ATL from source on different distributions including Fedora, Debian and Alpine.

### Run in builddir

After building ATL you can run apps directly from the build directory.

```sh
cd builddir
RUN_FROM_BUILDDIR= LD_LIBRARY_PATH=./ ./android-translation-layer [path to apk] [-l activity to launch]

```

For an example of a full game working that can be distributed along this:

```sh
RUN_FROM_BUILDDIR= LD_LIBRARY_PATH=./ ./android-translation-layer /path/to/test_apks/org.happysanta.gd_29.apk -l org/happysanta/gd/GDActivity
```

Or for a sample app using OpenGL from native code to do it's rendering:

```sh
RUN_FROM_BUILDDIR= LD_LIBRARY_PATH=./ ./android-translation-layer path/to/test_apks/gles3jni.apk -l com/android/gles3jni/GLES3JNIActivity
```

Note: the test apks are available at <https://gitlab.com/android_translation_layer/atl_test_apks>.

### Run after installation

To run with the default data dir `~/.local/share/android_translation_layer/`:

```sh
android-translation-layer [path to apk] [-l activity to launch]
```

For custom data dir:

```sh
ANDROID_APP_DATA_DIR=[data dir] android-translation-layer [path to apk] [-l activity to launch]
```

### Installing an App

You can pass `--install` on the cmdline to "install" an apk instead of launching it. This will copy
the apk to `_installed_apks_` in the data dir (`~/.local/share/android_translation_layer/` by default),
and use the xdp portal to install a `.desktop` file.

### Tweaks

#### Resolution Changes

Some apps don't like runtime changes to resolution. To sidestep this, we allow for specifying the initial resolution.
example with custom width/height:

```sh
android-translation-layer path/to/org.happysanta.gd_29.apk -l org/happysanta/gd/GDActivity -w 540 -h 960
```

### Potential issues

- On X11, Gtk might decide to use GLX, which completely messes up our EGL-dependent code.
Use GDK_DEBUG=gl-egl to force the use of EGL.
- On Apple Silicon, the page size is non-standard. Upstream ART is only recently getting patches
to support such non-standard page size, so the version we use obviously doesn't have any. While there
will probably still be some issues with native libraries, you can work around the issue of AOT-compiled
code not working by adding `-X '-Xnoimage-dex2oat' -X '-Xusejit:false'` to the atl cmdline, which will
force the use of an interpreter. Make sure to clear `~/.cache/art/` since AOT-compiled oat files will
still be used if they were generated previously.

### Tips

- the correct format for changing verbosity of messages going through android's logging library is `ANDROID_LOG_TAGS=*:v` (where `*` is "all tags" and `v` is "verbosity `verbose` or lesser"  
(note that specifying anything other than `*` as the tag will not work with the host version of liblog)

## Roadmap

- fix issues mentioned above

- fix ugly hacks

- implement more stuff (there is a lot of it, and it won't get done if nobody helps... ideally pick a simple-ish application and stub/implement stuff until it works)

- explore using bubblewrap to enforce the security policies that google helpfully forces apps to comply with (and our own security policies, like no internet access for apps which really shouldn't need it and are not scummy enough to refuse to launch without it)

## Contributing

If you are trying to launch a random app, chances are that we are missing implementations for some stuff that it needs, and we also don't have (sufficiently real looking) stubs for the stuff it says it needs but doesn't really.

The workflow is basically to see where it fails (usually a Class or Method was not found) and to create stubs which sufficiently satisfy the app so that it continues trying to launch.

Once the app launches, you may find that some functionality (UI elements, ...) is missing. To enable such functionality, you need to convert the relevant stubs to actual implementation. You can look at simple widgets (e.g. TextView, or ImageView) to see how to implement a widget such that it shows up as a Gtk Widget.

For more specific instructions, see [doc/QuickHelp.md](https://gitlab.com/android_translation_layer/android_translation_layer/-/blob/master/doc/QuickHelp.md).  
For general description of the architecture, see [doc/Architecture.md](https://gitlab.com/android_translation_layer/android_translation_layer/-/blob/master/doc/Architecture.md).

If you want to contribute, and find the codebase overwhelming, don't hesitate to open an issue so we can help you out and possibly write more documentation.

<!-- LICENSE -->

## License

Distributed under the GNU General Public License v3. See `LICENSE` for more information.
