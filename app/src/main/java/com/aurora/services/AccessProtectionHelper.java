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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Binder;
import android.util.Log;
import android.util.Pair;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashSet;

public class AccessProtectionHelper {

    Context context;
    PackageManager pm;
    HashSet<Pair<String, String>> whitelist;

    AccessProtectionHelper(Context context) {
        this(context, ClientWhitelist.whitelist);
    }

    AccessProtectionHelper(Context context, HashSet<Pair<String, String>> whitelist) {
        this.context = context;
        this.pm = context.getPackageManager();
        this.whitelist = whitelist;
    }

    /**
     * Checks if process that binds to this service (i.e. the package name corresponding to the
     * process) is in the whitelist.
     *
     * @return true if process is allowed to use this service
     */
    public boolean isCallerAllowed() {
        return isUidAllowed(Binder.getCallingUid());
    }

    private boolean isUidAllowed(int uid) {
        String[] callingPackages = pm.getPackagesForUid(uid);
        if (callingPackages == null) {
            throw new RuntimeException("Should not happen. No packages associated to caller UID!");
        }

        // is calling package allowed to use this service?
        // NOTE: No support for sharedUserIds
        // callingPackages contains more than one entry when sharedUserId has been used
        // No plans to support sharedUserIds due to many bugs connected to them:
        // http://java-hamster.blogspot.de/2010/05/androids-shareduserid.html
        String currentPkg = callingPackages[0];
        return isPackageAllowed(currentPkg);
    }

    public boolean isPackageAllowed(String packageName) {
        Log.d(PrivilegedService.TAG, "Checking if package is allowed to access privileged extension: " + packageName);

        try {
            byte[] currentPackageCert = getPackageCertificate(packageName);

            for (Pair whitelistEntry : whitelist) {
                String whitelistPackageName = (String) whitelistEntry.first;
                String whitelistHashString = (String) whitelistEntry.second;
                byte[] whitelistHash = hexStringToByteArray(whitelistHashString);

                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] packageHash = digest.digest(currentPackageCert);

                String packageHashString = new BigInteger(1, packageHash).toString(16);
                Log.d(PrivilegedService.TAG, "Allowed cert hash: " + whitelistHashString);
                Log.d(PrivilegedService.TAG, "Package cert hash: " + packageHashString);

                boolean packageNameMatches = packageName.equals(whitelistPackageName);
                boolean packageCertMatches = Arrays.equals(whitelistHash, packageHash);
                if (packageNameMatches && packageCertMatches) {
                    Log.d(PrivilegedService.TAG, "Package is allowed to access the privileged extension!");
                    return true;
                }
            }
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e.getMessage());
        }

        Log.e(PrivilegedService.TAG, "Package is NOT allowed to access the privileged extension!");
        return false;
    }

    private byte[] getPackageCertificate(String packageName) {
        try {
            // we do check the byte array of *all* signatures
            @SuppressLint("PackageManagerGetSignatures")
            PackageInfo pkgInfo = pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES);

            // NOTE: Silly Android API naming: Signatures are actually certificates
            Signature[] certificates = pkgInfo.signatures;
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            for (Signature cert : certificates) {
                outputStream.write(cert.toByteArray());
            }

            // Even if an apk has several certificates, these certificates should never change
            // Google Play does not allow the introduction of new certificates into an existing apk
            // Also see this attack: http://stackoverflow.com/a/10567852
            return outputStream.toByteArray();
        } catch (PackageManager.NameNotFoundException | IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

}
