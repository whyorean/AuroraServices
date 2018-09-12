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
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;

/**
 * This service provides an API via AIDL IPC for the main F-Droid app to install/delete packages.
 */
public class PrivilegedService extends Service {

    public static final String TAG = "PrivilegedExtension";
    private static final String BROADCAST_ACTION_INSTALL =
            "com.aurora.services.ACTION_INSTALL_COMMIT";
    private static final String BROADCAST_ACTION_UNINSTALL =
            "com.aurora.services.ACTION_UNINSTALL_COMMIT";
    private static final String BROADCAST_SENDER_PERMISSION =
            "android.permission.INSTALL_PACKAGES";
    private static final String EXTRA_LEGACY_STATUS = "android.content.pm.extra.LEGACY_STATUS";

    private AccessProtectionHelper accessProtectionHelper;

    private Method installMethod;
    private Method deleteMethod;

    private IPrivilegedCallback mCallback;

    Context context = this;

    private boolean hasPrivilegedPermissionsImpl() {
        boolean hasInstallPermission =
                getPackageManager().checkPermission(Manifest.permission.INSTALL_PACKAGES, getPackageName())
                        == PackageManager.PERMISSION_GRANTED;
        boolean hasDeletePermission =
                getPackageManager().checkPermission(Manifest.permission.DELETE_PACKAGES, getPackageName())
                        == PackageManager.PERMISSION_GRANTED;

        return hasInstallPermission && hasDeletePermission;
    }

    private void installPackageImpl(Uri packageURI, int flags, String installerPackageName,
                                    final IPrivilegedCallback callback) {
        // Internal callback from the system
        IPackageInstallObserver.Stub installObserver = new IPackageInstallObserver.Stub() {
            @Override
            public void packageInstalled(String packageName, int returnCode) throws RemoteException {
                // forward this internal callback to our callback
                try {
                    callback.handleResult(packageName, returnCode);
                } catch (RemoteException e1) {
                    Log.e(TAG, "RemoteException", e1);
                }
            }
        };

        // execute internal method
        try {
            installMethod.invoke(getPackageManager(), packageURI, installObserver,
                    flags, installerPackageName);
        } catch (Exception e) {
            Log.e(TAG, "Android not compatible!", e);
            try {
                callback.handleResult(null, 0);
            } catch (RemoteException e1) {
                Log.e(TAG, "RemoteException", e1);
            }
        }
    }

    private void deletePackageImpl(String packageName, int flags, final IPrivilegedCallback callback) {
        if (isDeviceOwner(packageName)) {
            Log.e(TAG, "Cannot delete " + packageName + ". This app is the device owner.");
            return;
        }

        // Internal callback from the system
        IPackageDeleteObserver.Stub deleteObserver = new IPackageDeleteObserver.Stub() {
            @Override
            public void packageDeleted(String packageName, int returnCode) throws RemoteException {
                // forward this internal callback to our callback
                try {
                    callback.handleResult(packageName, returnCode);
                } catch (RemoteException e1) {
                    Log.e(TAG, "RemoteException", e1);
                }
            }
        };

        // execute internal method
        try {
            deleteMethod.invoke(getPackageManager(), packageName, deleteObserver, flags);
        } catch (Exception e) {
            Log.e(TAG, "Android not compatible!", e);
            try {
                callback.handleResult(null, 0);
            } catch (RemoteException e1) {
                Log.e(TAG, "RemoteException", e1);
            }
        }
    }

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final int returnCode = intent.getIntExtra(
                    EXTRA_LEGACY_STATUS, PackageInstaller.STATUS_FAILURE);
            final String packageName = intent.getStringExtra(
                    PackageInstaller.EXTRA_PACKAGE_NAME);
            try {
                mCallback.handleResult(packageName, returnCode);
            } catch (RemoteException e1) {
                Log.e(TAG, "RemoteException", e1);
            }
        }
    };

    /**
    * Below function is copied mostly as-is from
    * https://android.googlesource.com/platform/packages/apps/PackageInstaller/+/06163dec5a23bb3f17f7e6279f6d46e1851b7d16
    */
    @TargetApi(24)
    private void doPackageStage(Uri packageURI) {
        final PackageManager pm = getPackageManager();
        final PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(
                PackageInstaller.SessionParams.MODE_FULL_INSTALL);
        final PackageInstaller packageInstaller = pm.getPackageInstaller();
        PackageInstaller.Session session = null;
        try {
            final int sessionId = packageInstaller.createSession(params);
            final byte[] buffer = new byte[65536];
            session = packageInstaller.openSession(sessionId);
            final InputStream in = getContentResolver().openInputStream(packageURI);
            final OutputStream out = session.openWrite("PackageInstaller", 0, -1 /* sizeBytes, unknown */);
            try {
                int c;
                while ((c = in.read(buffer)) != -1) {
                    out.write(buffer, 0, c);
                }
                session.fsync(out);
            } finally {
                IoUtils.closeQuietly(in);
                IoUtils.closeQuietly(out);
            }
            // Create a PendingIntent and use it to generate the IntentSender
            Intent broadcastIntent = new Intent(BROADCAST_ACTION_INSTALL);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    this /*context*/,
                    sessionId,
                    broadcastIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            session.commit(pendingIntent.getIntentSender());
        } catch (IOException e) {
            Log.d(TAG, "Failure", e);
            Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        } finally {
            IoUtils.closeQuietly(session);
        }
    }

    private final IPrivilegedService.Stub binder = new IPrivilegedService.Stub() {
        @Override
        public boolean hasPrivilegedPermissions() {
            boolean callerIsAllowed = accessProtectionHelper.isCallerAllowed();
            return callerIsAllowed && hasPrivilegedPermissionsImpl();
        }

        @Override
        public void installPackage(Uri packageURI, int flags, String installerPackageName,
                                   IPrivilegedCallback callback) {
            if (!accessProtectionHelper.isCallerAllowed()) {
                return;
            }

            if (Build.VERSION.SDK_INT >= 24) {
                doPackageStage(packageURI);
                mCallback = callback;
            } else {
                installPackageImpl(packageURI, flags, installerPackageName, callback);
            }
        }

        @Override
        public void deletePackage(String packageName, int flags, IPrivilegedCallback callback) {
            if (!accessProtectionHelper.isCallerAllowed()) {
                return;
            }
            if (Build.VERSION.SDK_INT >= 24) {
                mCallback = callback;
                final PackageManager pm = getPackageManager();
                final PackageInstaller packageInstaller = pm.getPackageInstaller();

                /*
                * The client app used to set this to F-Droid, but we need it to be set to
                * this package's package name to be able to uninstall from here.
                */
                pm.setInstallerPackageName(packageName, "com.aurora.services");
                // Create a PendingIntent and use it to generate the IntentSender
                Intent broadcastIntent = new Intent(BROADCAST_ACTION_UNINSTALL);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                        context, // context
                        0, // arbitary
                        broadcastIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT);
                packageInstaller.uninstall(packageName, pendingIntent.getIntentSender());
            } else {
                deletePackageImpl(packageName, flags, callback);
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        accessProtectionHelper = new AccessProtectionHelper(this);

        // get internal methods via reflection
        try {
            Class<?>[] installTypes = {
                    Uri.class, IPackageInstallObserver.class, int.class,
                    String.class,
            };
            Class<?>[] deleteTypes = {
                    String.class, IPackageDeleteObserver.class,
                    int.class,
            };

            PackageManager pm = getPackageManager();
            installMethod = pm.getClass().getMethod("installPackage", installTypes);
            deleteMethod = pm.getClass().getMethod("deletePackage", deleteTypes);
        } catch (NoSuchMethodException e) {
            Log.e(TAG, "Android not compatible!", e);
            stopSelf();
        }

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BROADCAST_ACTION_INSTALL);
        registerReceiver(
                mBroadcastReceiver, intentFilter, BROADCAST_SENDER_PERMISSION, null /*scheduler*/);
        IntentFilter intentFilter2 = new IntentFilter();
        intentFilter2.addAction(BROADCAST_ACTION_UNINSTALL);
        registerReceiver(
                mBroadcastReceiver, intentFilter2, BROADCAST_SENDER_PERMISSION, null /*scheduler*/);
    }

    /**
     * Checks if an app is the current device owner.
     *
     * @param packageName to check
     * @return true if it is the device owner app
     */
    private boolean isDeviceOwner(String packageName) {

        DevicePolicyManager manager =
                (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        return manager.isDeviceOwnerApp(packageName);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBroadcastReceiver);
    }

}
