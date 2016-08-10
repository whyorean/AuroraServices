/*
 * Copyright (C) 2015-2016 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.fdroid.fdroid.privileged;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.IPackageDeleteObserver;
import android.content.pm.IPackageInstallObserver;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import java.lang.reflect.Method;

/**
 * This service provides an API via AIDL IPC for the main F-Droid app to install/delete packages.
 */
public class PrivilegedService extends Service {

    public static final String TAG = "PrivilegedExtension";

    private AccessProtectionHelper accessProtectionHelper;

    private Method installMethod;
    private Method deleteMethod;

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

            installPackageImpl(packageURI, flags, installerPackageName, callback);
        }

        @Override
        public void deletePackage(String packageName, int flags, IPrivilegedCallback callback) {
            if (!accessProtectionHelper.isCallerAllowed()) {
                return;
            }

            deletePackageImpl(packageName, flags, callback);
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
    }

}
