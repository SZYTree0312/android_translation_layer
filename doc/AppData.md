# App Data

As mentioned, the default data dir is `~/.local/share/android_translation_layer/`. The data for each
app is then stored in `~/.local/share/android_translation_layer/[apk-name]_`. What this means
in practice is that:
 - we pass this directory to the app where AOSP would pass `/data/data/[app-id]`, and extract native
libs in `lib/`
 - we pass this directory to the app where AOSP would pass `/storage/emulated/0` (this means OBBs
will be under `Android/obb/[app-id]/`, and various litter that the app would happily dump on the
"sdcard" on AOSP will end up here as well)
 - we pass this directory as an additional resource directory to `libandroidfw`, so you can
for example put something in `assets/file.txt` and if the app tries to load `assets/file.txt` from
it's apk file, it will be preferentially loaded from here instead (do note that some apps read files
from the apk by themselves, and some apps do weird things like only load the file in order to find
it's offset in the apk to then read it out by themselves, so not only will this not always work but
you will sometimes find out by having the app crash)
 - we extract some additional files from the apk here for our purposes

The reason that we don't use `[app-id]` for the directory name is simply that we don't have access
to the app id at an early enough point. However, this also allows you to have multiple versions
of the same app not clash. Feel free to rename the apk to `[app-id]_[version].apk`, or simply
`[app-id].apk` if you wish to replace it with a different version later and reuse the data dir.