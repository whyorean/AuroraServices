package com.aurora.services.utils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;

import com.aurora.services.Constants;
import com.aurora.services.PrivilegedService;
import com.aurora.services.model.App;

import org.apache.commons.lang3.StringUtils;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class Util {

    public static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(
                Constants.SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE);
    }


    public static String millisToDay(long millis) {
        millis = Calendar.getInstance().getTimeInMillis() - millis;
        int days = (int) TimeUnit.MILLISECONDS.toDays(millis);
        switch (days) {
            case 0:
                return "Today";
            case 1:
                return "Yesterday";
            default:
                return days + "ago";
        }
    }

    public static StringBuilder millisToTime(long millis) {
        millis = Calendar.getInstance().getTimeInMillis() - millis;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        String hh = hours >= 1 ? hours + "hr" : "";
        return new StringBuilder()
                .append(hh.isEmpty() ? "" : hh)
                .append(StringUtils.SPACE)
                .append(minutes)
                .append(StringUtils.SPACE)
                .append(minutes >= 1 ? "minutes ago" : "minute ago");
    }

    public static StringBuilder millisToTimeDuration(long millis) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        String hh = hours >= 1 ? hours + "hr" : "";
        return new StringBuilder()
                .append("For")
                .append(StringUtils.SPACE)
                .append(hh.isEmpty() ? "" : hh)
                .append(StringUtils.SPACE)
                .append(minutes)
                .append(StringUtils.SPACE)
                .append(minutes >= 1 ? "minutes" : "minute");
    }

    public static App getAppByPackageName(PackageManager packageManager, String packageName) {
        try {
            final PackageInfo packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_META_DATA);

            if (packageInfo == null)
                return null;

            final ApplicationInfo applicationInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
            final App app = new App();
            app.setPackageName(packageInfo.packageName);
            app.setDisplayName(packageManager.getApplicationLabel(packageInfo.applicationInfo).toString());

            final Bitmap bitmap = ImageUtil.drawableToBitmap(packageManager.getApplicationIcon(packageName));
            final String base64 = ImageUtil.convert(bitmap);
            app.setIconBase64(base64);

            final String installer = packageManager.getInstallerPackageName(packageName);
            app.setInstaller(installer != null ? installer : "unknown");
            app.setInstalledTime(packageInfo.firstInstallTime);
            app.setInstallLocation(packageInfo.applicationInfo.sourceDir);
            app.setLastUpdated(packageInfo.lastUpdateTime);
            app.setVersionName(packageInfo.versionName);
            app.setVersionCode((long) packageInfo.versionCode);

            final String description = String.valueOf(applicationInfo.loadDescription(packageManager));
            app.setDescription(description);
            app.setTargetSDK(applicationInfo.targetSdkVersion);

            if (packageInfo.splitNames != null && packageInfo.splitNames.length > 0)
                app.setSplit(true);
            if ((packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0)
                app.setSystem(true);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                app.setCategory(getCategoryString(packageInfo.applicationInfo.category));
            }
            return app;
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    public static String getCategoryString(Integer categoryId) {
        switch (categoryId) {
            case ApplicationInfo.CATEGORY_AUDIO:
                return "Audio";
            case ApplicationInfo.CATEGORY_GAME:
                return "Game";
            case ApplicationInfo.CATEGORY_IMAGE:
                return "Images";
            case ApplicationInfo.CATEGORY_MAPS:
                return "Maps";
            case ApplicationInfo.CATEGORY_NEWS:
                return "News";
            case ApplicationInfo.CATEGORY_SOCIAL:
                return "Social";
            case ApplicationInfo.CATEGORY_VIDEO:
                return "Video";
            case ApplicationInfo.CATEGORY_PRODUCTIVITY:
                return "Productivity";
            case ApplicationInfo.CATEGORY_UNDEFINED:
                return "Undefined";
            default:
                return "Unknown";
        }
    }

    public static void restartService(Context context) {
        try {
            context.startService(new Intent(context, PrivilegedService.class));
        } catch (IllegalStateException e) {
            Log.e(e.getMessage());
        }
    }
}
