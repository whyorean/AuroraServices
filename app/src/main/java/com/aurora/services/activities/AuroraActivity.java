package com.aurora.services.activities;

import android.Manifest;
import android.content.ComponentName;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.aurora.services.R;
import com.aurora.services.task.ConvertTask;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.scottyab.rootbeer.RootBeer;

import butterknife.BindView;
import butterknife.ButterKnife;
import eu.chainfire.libsuperuser.Shell;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class AuroraActivity extends AppCompatActivity {

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.btn_convert)
    Button btnConvert;
    @BindView(R.id.text_warning)
    TextView textDesc;
    @BindView(R.id.text_enabled)
    TextView textEnabled;
    @BindView(R.id.text_permission)
    TextView textPermission;

    private CompositeDisposable disposable = new CompositeDisposable();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_aurora);
        ButterKnife.bind(this);
        setupSystem();
        setSupportActionBar(toolbar);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_remove_icon:
                showRemoveDialog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        disposable.dispose();
        super.onDestroy();
    }


    private void setupSystem() {
        checkBusyBoxRoot();
        if (isSystemApp()) {
            textEnabled.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_available, 0);
            textDesc.setText(getResources().getString(R.string.system_summary));
            btnConvert.setVisibility(View.GONE);
        } else {
            btnConvert.setOnClickListener(v -> showSystemDialog());
        }

        if (isPermissionGranted()) {
            textPermission.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_available, 0);
        } else {
            askPermissions();
        }
    }

    private boolean isSystemApp() {
        return (getApplicationInfo().flags
                & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0;
    }

    private void checkBusyBoxRoot() {
        RootBeer rootBeer = new RootBeer(this);
        boolean isRootAvail = rootBeer.isRooted();
        boolean isSUManagerAvail = rootBeer.detectRootManagementApps();

        if (isRootAvail && isSUManagerAvail)
            btnConvert.setEnabled(true);
    }

    private void initConversion() {
        disposable.add(Observable.fromCallable(() -> new ConvertTask(this)
                .convert())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(success -> {
                    if (success) {
                        Toast.makeText(this, "Successful", Toast.LENGTH_SHORT).show();
                        Shell.Pool.SU.run("reboot");
                    } else
                        Toast.makeText(this, "Failed", Toast.LENGTH_LONG).show();
                }));
    }

    private void showSystemDialog() {
        MaterialAlertDialogBuilder mBuilder = new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.dialog_title))
                .setMessage(getString(R.string.dialog_msg))
                .setPositiveButton(getString(R.string.action_now), (dialog, which) -> {
                    initConversion();
                    dialog.dismiss();
                })
                .setNegativeButton(getString(R.string.action_later), (dialog, which) -> {
                    dialog.dismiss();
                });
        mBuilder.create();
        mBuilder.show();
    }

    private void showRemoveDialog() {
        MaterialAlertDialogBuilder mBuilder = new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.dialog_title))
                .setMessage(getString(R.string.remove_dialog_msg))
                .setPositiveButton(getString(R.string.action_now), (dialog, which) -> {
                    removeLauncherIcon();
                    dialog.dismiss();
                })
                .setNegativeButton(getString(R.string.action_later), (dialog, which) -> {
                    dialog.dismiss();
                });
        mBuilder.create();
        mBuilder.show();
    }

    private void removeLauncherIcon() {
        PackageManager packageManager = getPackageManager();
        ComponentName componentName = new ComponentName(this, getClass());
        packageManager.setComponentEnabledSetting(componentName,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }

    private boolean isPermissionGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void askPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                1337);
    }
}
