package com.aurora.services.manager;

import android.content.Context;

import com.aurora.services.Constants;
import com.aurora.services.utils.PrefUtil;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class LogManager {

    private Context context;
    private Gson gson;

    public LogManager(Context context) {
        this.context = context;
        this.gson = new Gson();
    }

    public void addToStats(String packageName) {
        List<String> statsList = getAllPackages();
        if (!statsList.contains(packageName)) {
            statsList.add(packageName);
            saveStatsList(statsList);
        }
    }

    public void removeFromStats(String packageName) {
        List<String> packageList = getAllPackages();
        if (packageList.contains(packageName)) {
            packageList.remove(packageName);
            saveStatsList(packageList);
        }
    }

    public void clear() {
        saveStatsList(new ArrayList<>());
    }

    private void saveStatsList(List<String> statsList) {
        PrefUtil.putString(context, Constants.PREFERENCE_STATS_LIST, gson.toJson(statsList));
    }

    public List<String> getAllPackages() {
        String rawList = PrefUtil.getString(context, Constants.PREFERENCE_STATS_LIST);
        Type type = new TypeToken<List<String>>() {
        }.getType();
        List<String> statsList = gson.fromJson(rawList, type);

        if (statsList == null) {
            statsList = new ArrayList<>();
        }

        return statsList;
    }
}
