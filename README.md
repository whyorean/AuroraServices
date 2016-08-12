# F-Droid Privileged Extension

When F-Droid is installed as a normal Android app, installing, updating, and removing apps can only be done by requesting the Android operating system to do this.
F-Droid cannot execute this operations on itself.
Thus, the operating system shows a screen on every install/update to get confirmation from the user that he/she really wants to install this app.
This is a security feature of Android to prevent the installation of malware without user intervention.

The downside for F-Droid is that this prevents us from updating apps in the background, which is an essential feature of a modern package manager. 

Here comes the F-Droid Privileged Extension into play.
To have the same privileges as other pre-installed package managers, such as Google Play, i.e., installing/updating apps in the background, F-Droid needs so called "privileged permissions".
To get these we provide an extension to F-Droid which must be either shipped with your Android distribution/rom or installed into the system.


## How to use this?

More information be found in the [wiki page](https://f-droid.org/wiki/page/Privileged_Extension).


## Direct download

You can [download the extension from our repo](https://f-droid.org/app/org.fdroid.fdroid.privileged).


## Building with Gradle

Build a complete "update.zip" to flash to a device to install F-Droid and the Privileged Extension:

    ./gradlew updateZipWithFDroidRelease

Build an "update.zip" to flash to a device to install just the Privileged Extension:

    ./gradlew updateZipDebug

Build the standlone APK using:

    ./gradlew assembleRelease

In order to have final, signed release versions that are ready for installing, a release signing key must be set up in _signing.properties_ with these contents:

    key.store=/path/to/release-keystore.jks
    key.store.password=mysecurestorepw
    key.alias=release
    key.alias.password=mysecurekeypw
