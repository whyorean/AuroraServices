#!/bin/bash
#
# Script to prepare an update.zip containing F-Droid and the Privileged Extension.

set -e

PROG_DIR=$(dirname $(realpath $0))

TMP_DIR=$(mktemp -d -t fdroidprivext.tmp.XXXXXXXX)
trap "rm -rf $TMP_DIR" EXIT

function error() {
	echo "*** ERROR: " $@
	usage
}

function usage() {
	cat << EOFU
Usage: $0 variant [binaries]
where:
 - variant is one of: debug, release
 - binaries is set for using prebuilt apks,
   not set by default.
EOFU
	exit 1
}

# Parse input
VARIANT="$1"
[[ -z "$VARIANT" ]] && error "Missing variant"

BINARIES="$2"

GPG="gpg --keyring $PROG_DIR/f-droid.org-signing-key.gpg --no-default-keyring --trust-model always"

VERSION=$(grep versionCode=\"\[[:digit:]]\*\" app/src/main/AndroidManifest.xml | cut -d \" -f 2)
GITVERSION=$(git describe --tags --always)

FDROID_APK=F-Droid.apk
PRIVEXT_APK=org.fdroid.fdroid.privileged_${VERSION}.apk
FPE_NAME=F-DroidPrivilegedExtension

# Collect files
mkdir -p $TMP_DIR/META-INF/com/google/android/
cp app/src/main/scripts/update-binary $TMP_DIR/META-INF/com/google/android/
cp app/src/main/scripts/80-fdroid.sh $TMP_DIR/
cp app/src/main/permissions_org.fdroid.fdroid.privileged.xml $TMP_DIR/

if [ -z $BINARIES ] ; then
    cd $PROG_DIR
    ./gradlew assemble$(echo $VARIANT | tr 'dr' 'DR')
    OUT_DIR=$PROG_DIR/app/build/outputs/apk
    if [ $VARIANT == "debug" ]; then
	cp $OUT_DIR/$FPE_NAME-debug.apk $TMP_DIR/$FPE_NAME.apk
    elif [ -f $OUT_DIR/$FPE_NAME-release.apk ]; then
	cp $OUT_DIR/$FPE_NAME-release.apk $TMP_DIR/$FPE_NAME.apk
    else
        echo "No signed debug or release APK found in $OUT_DIR!"
        exit 1
    fi
else
	curl -L https://f-droid.org/repo/$PRIVEXT_APK > $TMP_DIR/$PRIVEXT_APK
	curl -L https://f-droid.org/repo/${PRIVEXT_APK}.asc > $TMP_DIR/${PRIVEXT_APK}.asc
	$GPG --verify $TMP_DIR/${PRIVEXT_APK}.asc
	rm $TMP_DIR/${PRIVEXT_APK}.asc
	mv $TMP_DIR/$PRIVEXT_APK $TMP_DIR/$FPE_NAME.apk
fi

# For both
curl -L https://f-droid.org/$FDROID_APK > $TMP_DIR/$FDROID_APK
curl -L https://f-droid.org/${FDROID_APK}.asc > $TMP_DIR/${FDROID_APK}.asc
$GPG --verify $TMP_DIR/${FDROID_APK}.asc
rm $TMP_DIR/${FDROID_APK}.asc
test -e $TMP_DIR/F-Droid.apk || mv $TMP_DIR/$FDROID_APK $TMP_DIR/F-Droid.apk

# Make zip
if [ -z $BINARIES ]; then
	ZIPBASE=${FPE_NAME}-${GITVERSION}
else
	ZIPBASE=${FPE_NAME}FromBinaries-${GITVERSION}
fi
if [ $VARIANT == "debug" ]; then
	ZIP=${ZIPBASE}-debug.zip
else
	ZIP=${ZIPBASE}.zip
fi
OUT_DIR=$PROG_DIR/app/build/distributions
mkdir -p $OUT_DIR
[ -f $OUT_DIR/$ZIP ] && rm -f $OUT_DIR/$ZIP
pushd $TMP_DIR
zip -r $OUT_DIR/$ZIP .
popd
