package com.aurora.services.receivers;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import com.aurora.services.activities.AuroraActivity;

public class AppLaunchReceiver extends BroadcastReceiver {

    private static final ComponentName LAUNCHER_COMPONENT_NAME = new ComponentName(
            "com.aurora.services", "com.aurora.services.activities.AuroraActivity");

    private static final String LAUNCHER_NUMBER = "287672"; //T9 of Aurora

    @Override
    public void onReceive(Context context, Intent intent) {
        String phoneNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
        if (LAUNCHER_NUMBER.equals(phoneNumber)) {
            setResultData(null);
            if (!isLauncherIconVisible(context)) {
                Intent mIntent = new Intent(context, AuroraActivity.class);
                mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(mIntent);
            }
        }

    }

    private boolean isLauncherIconVisible(Context context) {
        int enabledSetting = context.getPackageManager().getComponentEnabledSetting(LAUNCHER_COMPONENT_NAME);
        return enabledSetting != PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
    }

}