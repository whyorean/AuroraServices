/*
 * Copyright (C) 2016 Dominik Schürmann <dominik@dominikschuermann.de>
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

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Binder;

import com.aurora.services.manager.WhitelistManager;
import com.aurora.services.utils.Log;

import lombok.Data;

@Data
public class AccessProtectionHelper {

    private PackageManager packageManager;
    private WhitelistManager whitelistManager;

    public AccessProtectionHelper(Context context) {
        this.packageManager = context.getPackageManager();
        this.whitelistManager = new WhitelistManager(context);
    }

    public String getCallerPackageName() {
        final int uid = Binder.getCallingUid();
        final String[] callingPackages = packageManager.getPackagesForUid(uid);
        if (callingPackages == null)
            return "Unknown";
        else
            return callingPackages[0];
    }

    public boolean isCallerAllowed() {
        return isUidAllowed(Binder.getCallingUid());
    }

    private boolean isUidAllowed(int uid) {
        String[] callingPackages = packageManager.getPackagesForUid(uid);
        if (callingPackages == null) {
            throw new RuntimeException("No packages associated to caller UID!");
        }

        String currentPkg = callingPackages[0];
        return isPackageAllowed(currentPkg);
    }

    private boolean isPackageAllowed(String packageName) {
        Log.i("Checking if package is allowed to access Aurora Services: %s", packageName);

        if (whitelistManager.isWhitelisted(packageName)) {
            Log.i("Package is allowed to access Aurora Services");
            return true;
        } else {
            Log.e("Package is NOT allowed to access Aurora Services");
            return false;
        }
    }
}
