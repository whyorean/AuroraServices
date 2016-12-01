# F-Droid Privileged Extension

This enables F-Droid to install and delete apps without needing "Unknown Sources" to be enabled (e.g. just like Google Play does).  It also enables F-Droid to install updates in the background without the user having to click "install".

When F-Droid is installed as a normal Android app, installing, updating, and removing apps can only be done by sending requests to Android operating system.  F-Droid cannot execute these operations itself.  Android shows a screen on every install/update/delete to confirm this is what the user actually wants.  This is a security feature of Android to prevent apps or websites from installing malware without user intervention.

F-Droid Privileged Extension has elevated permissions which allow it to do installs and deletes.  It gives only F-Droid access to its install and delete commands.  In order for F-Droid Privileged Extension to get these "privileged" powers, it must be installed as part of your system by either being flashed as an _update.zip_ or by being built into an Android device or ROM. On Android 4 and older, it can be installed directly if you have root on your device.


## Design

F-Droid Privileged Extension is designed on the principals of "least privilege", so that elevated powers are only granted where they are absolutely needed, and those powers are limited as much as possible.  Therefore, the code that runs with increased powers is very small and easy to audit.  This is in contrast to how typical built-in app stores are granted all of the privileges available to a "system priv-app". 

Advantages of this design:

* "Unknown Sources" can remain disabled
* Can easily be built into devices and ROMs
* Reduced disk usage in the system partition
* System updates don't remove F-Droid


## How do I install it on my device?

The best way to install F-Droid Privileged Extension is to flash the _update.zip_ file using the standard mechanism for flashing updates to the ROM. This requires the device have an unlocked bootloader. A custom Recovery firmware is recommended. This is the same procedure as flashing "gapps" after flashing a ROM onto your device.

Installing the F-Droid Privileged Extension directly from the F-Droid app requires root access and is only possible on Android versions older than 5.0. It is not possible on Android 5.1, 6.0, and newer. To install the extension open the settings inside the F-Droid app, enable "Expert mode" and then enable "Privileged Extension". It will lead you to the extension app which will guide you through the installation process.

There are potential risks to rooting and unlocking your device, including:

* often requires using random, unverifed software
* bootloader unlock often voids warranty
* official updates might stop working with unlocked bootloader
* other functionality may break, like Android Pay, DRM-protected content playing, camera enhancements, etc.


## How do I build it into my ROM?

F-Droid Privileged Extension is designed to be built into ROMs and signed by the ROM key.  F-Droid only gets permissions via F-Droid Privileged Extension's internal key check, not via having a matching signing key or via `"signature" protectionLevel`.  This git repo includes an _Android.mk_ so it can be directly included via `repo`.   Add `FDroidPrivilegedExtension` to the `PRODUCT_PACKAGES` list to include it in the system image, and use a `repo` manifest like this:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<manifest>

  <remote name="fdroid" fetch="https://gitlab.com/fdroid/" />
  <project path="packages/apps/FDroidPrivilegedExtension"
           name="privileged-extension.git" remote="fdroid"
           revision="refs/tags/0.2" />

</manifest>
```

By default, F-Droid Privileged Extension trusts only the official F-Droid builds, and we recommend that https://f-droid.org/FDroid.apk is also included in the ROM. You can verify the binaries using both the APK signature and the PGP key: https://f-droid.org/FDroid.apk.asc

APK signing certificate SHA-256 fingerprint:
```
43238d512c1e5eb2d6569f4a3afbf5523418b82e0a3ed1552770abb9a9c9ccab
```

PGP signing key fingerprint is:
```
37D2 C987 89D8 3119 4839  4E3E 41E7 044E 1DBA 2E89
```

There is more documentation on this here:
https://f-droid.org/wiki/page/Release_Channels_and_Signing_Keys


## Direct download

You can [download the extension from our repo](https://f-droid.org/app/org.fdroid.fdroid.privileged).


## Building with Gradle

Build a complete "update.zip" to flash to a device to install F-Droid and the Privileged Extension:

    ./gradlew updateZipWithFDroidRelease

Build an "update.zip" to flash to a device to install just the Privileged Extension:

    ./gradlew assembleUpdateZipDebug

Build the standlone APK using:

    ./gradlew assembleRelease

In order to have final, signed release versions that are ready for installing, a release signing key must be set up in _signing.properties_ with these contents:

    key.store=/path/to/release-keystore.jks
    key.store.password=mysecurestorepw
    key.alias=release
    key.alias.password=mysecurekeypw
