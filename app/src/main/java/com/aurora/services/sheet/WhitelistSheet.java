package com.aurora.services.sheet;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aurora.services.BuildConfig;
import com.aurora.services.R;
import com.aurora.services.manager.WhitelistManager;
import com.aurora.services.model.item.WhiteListItem;
import com.aurora.services.utils.Log;
import com.aurora.services.utils.Util;
import com.mikepenz.fastadapter.adapters.FastItemAdapter;
import com.mikepenz.fastadapter.select.SelectExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class WhitelistSheet extends BaseBottomSheet {

    public static final String TAG = "WHITELIST_SHEET";

    @BindView(R.id.recycler)
    RecyclerView recyclerView;

    private FastItemAdapter<WhiteListItem> fastItemAdapter;
    private SelectExtension<WhiteListItem> selectExtension;
    private WhitelistManager whitelistManager;
    private CompositeDisposable disposable = new CompositeDisposable();

    @Nullable
    @Override
    protected View onCreateContentView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.sheet_whitelist, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    protected void onContentViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onContentViewCreated(view, savedInstanceState);
        whitelistManager = new WhitelistManager(requireContext());
        setupRecycler();
        fetchInstallerApps();
    }

    private void setupRecycler() {
        whitelistManager = new WhitelistManager(requireContext());
        fastItemAdapter = new FastItemAdapter<>();
        selectExtension = new SelectExtension<>(fastItemAdapter);
        selectExtension.setMultiSelect(true);

        fastItemAdapter.setOnClickListener((view, repoItemIAdapter, repoItem, integer) -> false);
        fastItemAdapter.setOnPreClickListener((view, repoItemIAdapter, repoItem, integer) -> true);

        selectExtension.setMultiSelect(true);
        selectExtension.setSelectionListener((item, selected) -> {
            if (whitelistManager.isWhitelisted(item.getApp().getPackageName())) {
                whitelistManager.removeFromWhitelist(item.getApp().getPackageName());
                Log.d("Whitelisted : %s", item.getApp().getDisplayName());
            } else {
                whitelistManager.addToWhitelist(item.getApp().getPackageName());
                Log.d("Blacklisted : %s", item.getApp().getDisplayName());
            }
        });

        fastItemAdapter.addExtension(selectExtension);
        fastItemAdapter.addEventHook(new WhiteListItem.CheckBoxClickEvent());

        selectExtension.setMultiSelect(true);

        recyclerView.setAdapter(fastItemAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
    }

    private void fetchInstallerApps() {
        disposable.add(Observable.fromIterable(getInstallerPackages())
                .subscribeOn(Schedulers.io())
                .filter(packageName -> !packageName.equals(BuildConfig.APPLICATION_ID))
                .map(packageName -> Util.getAppByPackageName(requireContext().getPackageManager(), packageName))
                .map(app -> new WhiteListItem(app, whitelistManager.isWhitelisted(app.getPackageName())))
                .toList()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(whiteListItems -> {
                    fastItemAdapter.add(whiteListItems);
                }, throwable -> Log.e(throwable.getMessage())));
    }


    private List<String> getInstallerPackages() {
        final List<String> packageList = new ArrayList<>();
        final PackageManager packageManager = requireContext().getPackageManager();

        for (PackageInfo packageInfo : packageManager.getInstalledPackages(PackageManager.GET_META_DATA | PackageManager.GET_PERMISSIONS)) {
            final String packageName = packageInfo.packageName;

            if (packageInfo.applicationInfo != null && !packageInfo.applicationInfo.enabled//Filter Disabled Apps
                    || (packageManager.getLaunchIntentForPackage(packageName)) == null) //Filter Non-Launchable Apps
                continue;

            if (packageInfo.requestedPermissions == null)
                continue;

            final List<String> stringList = Arrays.asList(packageInfo.requestedPermissions);

            if (!stringList.contains("android.permission.INSTALL_PACKAGES"))
                continue;

            packageList.add(packageName);
        }
        return packageList;
    }

    @Override
    public void onDestroy() {
        disposable.dispose();
        super.onDestroy();
    }
}
