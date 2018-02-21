LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := permissions_org.fdroid.fdroid.privileged.xml
LOCAL_MODULE_CLASS := ETC
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_PATH := $(TARGET_OUT_ETC)/permissions
LOCAL_SRC_FILES := $(LOCAL_MODULE)
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_PACKAGE_NAME := F-DroidPrivilegedExtension
LOCAL_MODULE_TAGS := optional
LOCAL_PRIVILEGED_MODULE := true
LOCAL_SDK_VERSION := current
# Keep IPackageInstallObserver and IPackageDeleteObserver
LOCAL_PROGUARD_ENABLED := disabled
LOCAL_SRC_FILES := $(call all-java-files-under, java) \
                   $(call all-Iaidl-files-under, aidl)
LOCAL_AIDL_INCLUDES := $(LOCAL_PATH)/aidl
LOCAL_REQUIRED_MODULES := permissions_org.fdroid.fdroid.privileged.xml
include $(BUILD_PACKAGE)
