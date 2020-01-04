package com.aurora.services;

public class Constants {

    public static final String SHARED_PREFERENCES_KEY = "com.aurora.services";
    public static final String TAG = "Aurora Service";

    public static final String BROADCAST_ACTION_INSTALL = "com.aurora.services.ACTION_INSTALL_COMMIT";
    public static final String BROADCAST_ACTION_UNINSTALL = "com.aurora.services.ACTION_UNINSTALL_COMMIT";
    public static final String BROADCAST_SENDER_PERMISSION = "android.permission.INSTALL_PACKAGES";

    public static final String PREFERENCE_WHITELIST_PACKAGE_LIST = "PREFERENCE_WHITELIST_PACKAGE_LIST";
    public static final String PREFERENCE_STATS_LIST = "PREFERENCE_STATS_LIST";
}
