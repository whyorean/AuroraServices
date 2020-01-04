package com.aurora.services.manager;

import android.content.Context;

import com.aurora.services.Constants;
import com.aurora.services.utils.PrefUtil;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class WhitelistManager {

    private Context context;
    private Gson gson;

    public WhitelistManager(Context context) {
        this.context = context;
        this.gson = new Gson();
    }

    public void addToWhitelist(String packageName) {
        List<String> stringList = getWhitelistedPackages();
        if (!stringList.contains(packageName)) {
            stringList.add(packageName);
            saveWhitelist(stringList);
        }
    }

    public void addToWhitelist(List<String> packageNameList) {
        List<String> stringList = getWhitelistedPackages();
        for (String packageName : packageNameList) {
            if (!stringList.contains(packageName)) {
                stringList.add(packageName);
            }
        }
        saveWhitelist(stringList);
    }


    public void removeFromWhitelist(String packageName) {
        List<String> stringList = getWhitelistedPackages();
        if (stringList.contains(packageName)) {
            stringList.remove(packageName);
            saveWhitelist(stringList);
        }
    }

    public boolean isWhitelisted(String packageName) {
        return getWhitelistedPackages().contains(packageName);
    }

    public void clear() {
        saveWhitelist(new ArrayList<>());
    }

    private void saveWhitelist(List<String> stringList) {
        PrefUtil.putString(context, Constants.PREFERENCE_WHITELIST_PACKAGE_LIST, gson.toJson(stringList));
    }

    public List<String> getWhitelistedPackages() {
        String rawList = PrefUtil.getString(context, Constants.PREFERENCE_WHITELIST_PACKAGE_LIST);
        Type type = new TypeToken<List<String>>() {
        }.getType();
        List<String> stringList = gson.fromJson(rawList, type);

        if (stringList == null) {
            stringList = new ArrayList<>();
        }

        return stringList;
    }
}
