package com.aurora.services.task;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import eu.chainfire.libsuperuser.Shell;

public class ConvertTask {

    static private final String MOUNT_RW = "mount -o rw,remount,rw /system";
    static private final String MOUNT_RO = "mount -o ro,remount,ro /system";
    static private final String PRIVATE_APP_PATH = "/system/priv-app/";
    static private final String PERMISSION_PATH = "/system/etc/permissions/";
    static private final String PKG_NAME = "AuroraServices.apk";
    static private final String PERM_NAME = "permissions_com.aurora.services.xml";

    private List<String> resultList = new ArrayList<>();
    private Context context;

    public ConvertTask(Context mContext) {
        this.context = mContext;
    }

    public boolean convert() {
        if (Shell.SU.available()) {
            resultList.clear();
            resultList = Shell.SU.run(getCommands());
            return true;
        } else
            return false;
    }

    private List<String> getCommands() {
        List<String> commands = new ArrayList<>();
        String from = context.getPackageResourcePath();
        String apkPath = getAPKPath();
        String permPath = getPermissionPath() + PERM_NAME;
        String apkDir = new File(apkPath).getParent();

        //Mount system as RW
        commands.add(MOUNT_RW);

        //Create APK Directory in priv-apps
        commands.add("mkdir " + apkDir);
        commands.add("chmod 755 " + apkDir);

        //Copy APK and give permissions
        commands.add("cp " + from + " " + apkPath);
        commands.add("chmod 644 " + apkPath);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            //Copy Whitelist permissions
            commands.add("cp " + getPermissionFromAsset() + " " + permPath);
            commands.add("chmod 644 " + permPath);

            commands.add("chown system " + apkPath);
            commands.add("chgrp system " + apkPath);
        }

        //Mount system as RO
        commands.add(MOUNT_RO);
        return commands;
    }

    private String getAPKPath() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return PRIVATE_APP_PATH + PKG_NAME + "/" + PKG_NAME;
        } else {
            return PRIVATE_APP_PATH + PKG_NAME;
        }
    }

    private String getPermissionPath() {
        return PERMISSION_PATH;
    }

    private String getPermissionFromAsset() {
        try {
            InputStream in = context.getAssets().open(PERM_NAME);
            File outFile = new File(context.getFilesDir().getPath(), PERM_NAME);
            OutputStream out = new FileOutputStream(outFile);
            copyFile(in, out);
            in.close();
            out.flush();
            out.close();
            return context.getFilesDir().getPath() + "/" + PERM_NAME;
        } catch (IOException e) {
            Log.e("Aurora Services", "Failed to copy permission file", e);
            return "";
        }
    }

    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }

}
