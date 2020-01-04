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

import com.aurora.services.R;
import com.aurora.services.manager.LogManager;
import com.aurora.services.model.item.LogItem;
import com.aurora.services.utils.Log;
import com.aurora.services.utils.Util;
import com.mikepenz.fastadapter.adapters.FastItemAdapter;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class LogSheet extends BaseBottomSheet {

    public static final String TAG = "WHITELIST_SHEET";

    @BindView(R.id.recycler)
    RecyclerView recyclerView;

    private FastItemAdapter<LogItem> fastItemAdapter;
    private LogManager logManager;

    private CompositeDisposable disposable = new CompositeDisposable();

    @Nullable
    @Override
    protected View onCreateContentView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.sheet_log, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    protected void onContentViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onContentViewCreated(view, savedInstanceState);
        logManager = new LogManager(requireContext());
        setupRecycler();
        fetchLoggedApps();
    }

    private void setupRecycler() {
        fastItemAdapter = new FastItemAdapter<>();
        fastItemAdapter.setOnClickListener((view, repoItemIAdapter, repoItem, integer) -> false);
        fastItemAdapter.setOnPreClickListener((view, repoItemIAdapter, repoItem, integer) -> true);

        recyclerView.setAdapter(fastItemAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
    }

    private void fetchLoggedApps() {
        disposable.add(Observable.fromCallable(this::getInstalledPackages)
                .subscribeOn(Schedulers.io())
                .flatMap(apps -> Observable
                        .fromIterable(apps)
                        .map(packageName -> Util.getAppByPackageName(requireContext().getPackageManager(), packageName)))
                .map(LogItem::new)
                .toList()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(logItems -> {
                    fastItemAdapter.add(logItems);
                }, throwable -> Log.e(throwable.getMessage())));
    }

    private List<String> getInstalledPackages() throws PackageManager.NameNotFoundException {
        final List<String> packageList = new ArrayList<>();
        final PackageManager packageManager = requireContext().getPackageManager();

        for (String packageName : logManager.getAllPackages()) {

            final PackageInfo packageInfo = packageManager.getPackageInfo(packageName, 0);

            if (packageInfo == null)
                continue;

            if (packageInfo.applicationInfo != null && !packageInfo.applicationInfo.enabled//Filter Disabled Apps
                    || (packageManager.getLaunchIntentForPackage(packageName)) == null) //Filter Non-Launchable Apps
                continue;

            packageList.add(packageName);
        }
        return packageList;
    }

    @Override
    public void onDestroy() {
        disposable.clear();
        super.onDestroy();
    }
}
