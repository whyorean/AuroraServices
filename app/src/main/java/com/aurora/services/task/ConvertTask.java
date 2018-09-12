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
    static private final String PKG_NAME = "AuroraServices";
    static private final String PERM_NAME = "permissions_com.aurora.services.xml";
    static private final String PKG_EXT = ".apk";
    static private final String BUSYBOX = "busybox";

    private List<String> suResult = new ArrayList<>();
    private Context mContext;

    public ConvertTask(Context mContext) {
        this.mContext = mContext;
    }

    public boolean convert() {
        if (Shell.SU.available()) {
            suResult.clear();
            suResult = Shell.SU.run(getCommands());
            return true;
        } else
            return false;
    }

    private List<String> getCommands() {
        List<String> commands = new ArrayList<>();
        String from = mContext.getPackageResourcePath();
        String apkPath = getAPKPath();
        String permPath = getPermissionPath() + PERM_NAME;
        String apkDir = new File(apkPath).getParent();

        //Mount system as RW
        commands.add(getBusyboxCommand(MOUNT_RW));

        //Create APK Directory in priv-apps
        commands.add(getBusyboxCommand("mkdir " + apkDir));
        commands.add(getBusyboxCommand("chmod 755 " + apkDir));

        //Copy APK and give permissions
        commands.add(getBusyboxCommand("cp " + from + " " + apkPath));
        commands.add(getBusyboxCommand("chmod 644 " + apkPath));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            //Copy Whitelist permissions
            commands.add(getBusyboxCommand("cp " + getPermissionFromAsset() + " " + permPath));
            commands.add(getBusyboxCommand("chmod 644 " + permPath));

            commands.add(getBusyboxCommand("chown system " + apkPath));
            commands.add(getBusyboxCommand("chgrp system " + apkPath));
        }

        //Mount system as RO
        commands.add(getBusyboxCommand(MOUNT_RO));
        return commands;
    }

    private String getAPKPath() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return PRIVATE_APP_PATH + PKG_NAME + File.separator + PKG_NAME + PKG_EXT;
        } else {
            return PRIVATE_APP_PATH + PKG_NAME + PKG_EXT;
        }
    }

    private String getPermissionPath() {
        return PERMISSION_PATH;
    }

    private String getPermissionFromAsset() {
        try {
            InputStream in = mContext.getAssets().open(PERM_NAME);
            File outFile = new File(mContext.getFilesDir().getPath(), PERM_NAME);
            OutputStream out = new FileOutputStream(outFile);
            copyFile(in, out);
            in.close();
            out.flush();
            out.close();
            return mContext.getFilesDir().getPath() + File.separator + PERM_NAME;
        } catch (IOException e) {
            Log.e("AuroraAsset", "Failed to copy permission file", e);
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

    protected String getBusyboxCommand(String command) {
        return BUSYBOX + " " + command;
    }

}
