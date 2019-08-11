/*
 * Copyright (C) 2016 Dominik Schürmann <dominik@dominikschuermann.de>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http//www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.aurora.services;

import android.util.Pair;

import java.util.Arrays;
import java.util.HashSet;

/**
 * Only apps signed using a certificate with a SHA-256 hash listed here
 * can access the Privileged Extension!
 * <ol>
 * <li>Get SHA-256 of certificate as lowercase without colons with
 * <code> keytool -printcert -jarfile com.example.apk | sed -n 's,SHA256:\s*\([A-F0-9:]*\),\1,p' | sed 's,:,,g' | tr A-f a-f </code>
 * </li>
 * <li>Add here with Application ID</li>
 * </ol>
 */
public class ClientWhitelist {

    public static HashSet<Pair<String, String>> whitelist = new HashSet<>(Arrays.asList(
            new Pair<>("com.aurora.store", "4c626157ad02bda3401a7263555f68a79663fc3e13a4d4369a12570941aa280f"),
            new Pair<>("com.aurora.store", "5c83c7672b929955dc0a1db89a5e6ae4389e2eae7ec939956041694e5815f532"),
            new Pair<>("com.aurora.adroid", "4c626157ad02bda3401a7263555f68a79663fc3e13a4d4369a12570941aa280f"),
            new Pair<>("com.aurora.adroid", "b5315cb1906a87020fb7f2c344ceaf21068f0aade0b38ada32e7f65b624f0a3b")
    ));
}
