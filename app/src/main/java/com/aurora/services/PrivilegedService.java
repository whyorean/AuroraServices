/*
 * Copyright (C) 2015-2016 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright 2007, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.aurora.services;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.app.Service;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.IPackageDeleteObserver;
import android.content.pm.IPackageInstallObserver;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.widget.Toast;

import com.aurora.services.manager.LogManager;
import com.aurora.services.utils.CommonUtils;
import com.aurora.services.utils.IOUtils;
import com.aurora.services.utils.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.List;

public class PrivilegedService extends Service {

    public static PrivilegedService instance = null;

    private AccessProtectionHelper helper;
    private LogManager logManager;
    private Method installMethod;
    private Method deleteMethod;

    private IPrivilegedCallback iPrivilegedCallback;

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final int returnCode = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1);
            final String packageName = intent.getStringExtra(PackageInstaller.EXTRA_PACKAGE_NAME);
            try {
                if (returnCode == 0)
                    logManager.addToStats(packageName);
                iPrivilegedCallback.handleResult(packageName, returnCode);
            } catch (RemoteException e1) {
                Log.e("RemoteException -> %s", e1);
            }
        }
    };

    private final IPrivilegedService.Stub binder = new IPrivilegedService.Stub() {

        @Override
        public boolean hasPrivilegedPermissions() {
            boolean callerIsAllowed = helper.isCallerAllowed();
            return callerIsAllowed && hasPrivilegedPermissionsImpl();
        }

        @Override
        public void installPackage(Uri packageURI, int flags, String installerPackageName,
                                   IPrivilegedCallback callback) {
            if (!helper.isCallerAllowed()) {
                return;
            }

            if (Build.VERSION.SDK_INT >= 24) {
                doPackageStage(packageURI);
                iPrivilegedCallback = callback;
            } else {
                installPackageImpl(packageURI, flags, installerPackageName, callback);
            }
        }

        @Override
        public void installSplitPackage(List<Uri> uriList, int flags, String installerPackageName,
                                        IPrivilegedCallback callback) {
            if (!helper.isCallerAllowed()) {
                return;
            }

            doSplitPackageStage(uriList);
            iPrivilegedCallback = callback;
        }

        @Override
        public void deletePackage(String packageName, int flags, IPrivilegedCallback callback) {

            if (!helper.isCallerAllowed()) {
                return;
            }

            if (Build.VERSION.SDK_INT >= 24) {
                iPrivilegedCallback = callback;
                final PackageManager packageManager = getPackageManager();
                final PackageInstaller packageInstaller = packageManager.getPackageInstaller();

                packageManager.setInstallerPackageName(packageName, "com.aurora.services");

                final Intent uninstallIntent = new Intent(Constants.BROADCAST_ACTION_UNINSTALL);
                final PendingIntent pendingIntent = PendingIntent.getBroadcast(
                        PrivilegedService.this,
                        0,
                        uninstallIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT);

                packageInstaller.uninstall(packageName, pendingIntent.getIntentSender());
            } else {
                deletePackageImpl(packageName, flags, callback);
            }
        }
    };

    public static boolean isAvailable() {
        try {
            return instance != null;
        } catch (NullPointerException e) {
            return false;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        helper = new AccessProtectionHelper(this);
        logManager = new LogManager(this);

        if (Build.VERSION.SDK_INT < 24) {
            try {
                Class<?>[] installTypes = {
                        Uri.class, IPackageInstallObserver.class, int.class,
                        String.class,
                };
                Class<?>[] deleteTypes = {
                        String.class, IPackageDeleteObserver.class,
                        int.class,
                };

                final PackageManager packageManager = getPackageManager();
                installMethod = packageManager.getClass().getMethod("installPackage", installTypes);
                deleteMethod = packageManager.getClass().getMethod("deletePackage", deleteTypes);
            } catch (NoSuchMethodException e) {
                Log.e("Android not compatible! -> %s", e.getMessage());
                stopSelf();
            }
        }

        final IntentFilter installIntent = new IntentFilter();
        installIntent.addAction(Constants.BROADCAST_ACTION_INSTALL);

        final IntentFilter uninstallIntent = new IntentFilter();
        uninstallIntent.addAction(Constants.BROADCAST_ACTION_UNINSTALL);

        registerReceiver(broadcastReceiver, installIntent, Constants.BROADCAST_SENDER_PERMISSION, null);
        registerReceiver(broadcastReceiver, uninstallIntent, Constants.BROADCAST_SENDER_PERMISSION, null);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @TargetApi(24)
    private void doPackageStage(Uri uri) {
        final PackageManager packageManager = getPackageManager();
        final PackageInstaller packageInstaller = packageManager.getPackageInstaller();
        final PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(
                PackageInstaller.SessionParams.MODE_FULL_INSTALL);

        try {
            final byte[] buffer = new byte[65536];
            final int sessionId = packageInstaller.createSession(params);
            final PackageInstaller.Session session = packageInstaller.openSession(sessionId);
            final InputStream inputStream = getContentResolver().openInputStream(uri);
            final OutputStream outputStream = session.openWrite("PackageInstaller",
                    0,
                    -1);
            try {
                int c;
                while ((c = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, c);
                }
                session.fsync(outputStream);
            } finally {
                CommonUtils.closeQuietly(inputStream);
                CommonUtils.closeQuietly(outputStream);
            }

            // Create a PendingIntent and use it to generate the IntentSender
            final Intent installIntent = new Intent(Constants.BROADCAST_ACTION_INSTALL);
            final PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    this /*context*/,
                    sessionId,
                    installIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);

            session.commit(pendingIntent.getIntentSender());
            CommonUtils.closeQuietly(session);
        } catch (IOException e) {
            Log.e("Failure -> %s", e);
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void doSplitPackageStage(List<Uri> uriList) {
        final PackageManager packageManager = getPackageManager();
        final PackageInstaller packageInstaller = packageManager.getPackageInstaller();

        try {
            final PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
            final int sessionId = packageInstaller.createSession(params);
            final PackageInstaller.Session session = packageInstaller.openSession(sessionId);

            for (Uri uri : uriList) {
                final File file = new File(uri.getPath());
                final InputStream inputStream = new FileInputStream(file);
                final OutputStream outputStream = session.openWrite(file.getName(), 0, file.length());
                IOUtils.copy(inputStream, outputStream);
                session.fsync(outputStream);
                CommonUtils.closeQuietly(inputStream);
                CommonUtils.closeQuietly(outputStream);
            }

            final Intent installIntent = new Intent(Constants.BROADCAST_ACTION_INSTALL);
            final PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    this,
                    sessionId,
                    installIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);

            session.commit(pendingIntent.getIntentSender());
            CommonUtils.closeQuietly(session);
        } catch (IOException e) {
            Log.e("Failure -> %s", e);
            Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private boolean hasPrivilegedPermissionsImpl() {
        boolean hasInstallPermission = getPackageManager()
                .checkPermission(Manifest.permission.INSTALL_PACKAGES, getPackageName())
                == PackageManager.PERMISSION_GRANTED;
        boolean hasDeletePermission = getPackageManager()
                .checkPermission(Manifest.permission.DELETE_PACKAGES, getPackageName())
                == PackageManager.PERMISSION_GRANTED;
        return hasInstallPermission && hasDeletePermission;
    }

    private void installPackageImpl(Uri packageURI, int flags, String installerPackageName, final IPrivilegedCallback callback) {

        IPackageInstallObserver.Stub installObserver = new IPackageInstallObserver.Stub() {
            @Override
            public void packageInstalled(String packageName, int returnCode) {
                try {
                    callback.handleResult(packageName, returnCode);
                } catch (RemoteException e1) {
                    Log.e("RemoteException -> %s", e1);
                }
            }
        };

        try {
            installMethod.invoke(getPackageManager(), packageURI, installObserver, flags, installerPackageName);
        } catch (Exception e) {
            Log.e("Android not compatible! -> %s", e);
            try {
                callback.handleResult(null, 0);
            } catch (RemoteException e1) {
                Log.e("RemoteException -> %s", e1);
            }
        }
    }

    private void deletePackageImpl(String packageName, int flags, final IPrivilegedCallback callback) {

        if (isDeviceOwner(packageName)) {
            Log.e("Cannot delete %s. This app is the device owner.", packageName);
            return;
        }

        IPackageDeleteObserver.Stub deleteObserver = new IPackageDeleteObserver.Stub() {
            @Override
            public void packageDeleted(String packageName, int returnCode) {
                try {
                    callback.handleResult(packageName, returnCode);
                } catch (RemoteException e1) {
                    Log.e("RemoteException -> %s", e1);
                }
            }
        };

        // execute internal method
        try {
            deleteMethod.invoke(getPackageManager(), packageName, deleteObserver, flags);
        } catch (Exception e) {
            Log.e("Android not compatible! -> %s", e);
            try {
                callback.handleResult(null, 0);
            } catch (RemoteException e1) {
                Log.e("RemoteException -> %s", e1);
            }
        }
    }

    private boolean isDeviceOwner(String packageName) {
        final Object object = getSystemService(Context.DEVICE_POLICY_SERVICE);
        final DevicePolicyManager manager = (DevicePolicyManager) object;
        return manager.isDeviceOwnerApp(packageName);
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(broadcastReceiver);
        super.onDestroy();
    }
}
