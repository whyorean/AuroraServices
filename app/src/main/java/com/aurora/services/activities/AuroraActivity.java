package com.aurora.services.activities;

import android.content.ComponentName;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.aurora.services.R;
import com.aurora.services.adapters.PermissionAdapter;
import com.aurora.services.dialogs.GenericDialog;
import com.aurora.services.task.ConvertTask;
import com.scottyab.rootbeer.RootBeer;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
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
    @BindView(R.id.permission_recycler)
    RecyclerView permissionRecycler;
    @BindView(R.id.button_convertApp)
    Button convertApp;
    @BindView(R.id.system_desc)
    TextView systemDesc;
    @BindView(R.id.status_su)
    TextView status_su;
    @BindView(R.id.status_bb)
    TextView status_bb;
    @BindView(R.id.status_sm)
    TextView status_sm;
    @BindView(R.id.status_sa)
    TextView status_sa;

    private PermissionAdapter mPermissionAdapter;
    private CompositeDisposable mDisposable = new CompositeDisposable();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_aurora);
        ButterKnife.bind(this);
        setupRecycler();
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
        if (mPermissionAdapter != null)
            mPermissionAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onDestroy() {
        mDisposable.dispose();
        super.onDestroy();
    }

    private void setupRecycler() {
        mPermissionAdapter = new PermissionAdapter(
                this,
                getResources().getStringArray(R.array.permission_names),
                getResources().getStringArray(R.array.permission_values));
        permissionRecycler.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));
        permissionRecycler.setAdapter(mPermissionAdapter);
    }

    private void setupSystem() {
        checkBusyBoxRoot();
        if (isSystemApp()) {
            status_sa.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_available, 0);
            systemDesc.setText(getResources().getString(R.string.system_summary));
            convertApp.setVisibility(View.GONE);
        } else {
            convertApp.setOnClickListener(v -> showSystemDialog());
        }
    }

    private boolean isSystemApp() {
        return (getApplicationInfo().flags
                & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0;
    }

    private void checkBusyBoxRoot() {
        RootBeer mRootBeer = new RootBeer(this);
        boolean isRootAvail = mRootBeer.isRooted();
        boolean isBusyBoxAvail = mRootBeer.checkForBusyBoxBinary();
        boolean isSUManagerAvail = mRootBeer.detectRootManagementApps();

        if (isRootAvail)
            status_su.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_available, 0);
        if (isBusyBoxAvail)
            status_bb.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_available, 0);
        if (isSUManagerAvail)
            status_sm.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_available, 0);

        if (isRootAvail && isBusyBoxAvail)
            convertApp.setEnabled(true);
        else {
            if (mRootBeer.isRootedWithoutBusyBoxCheck())
                Log.i(getPackageName(), "BusyBox not available");
            if (mRootBeer.checkForMagiskBinary())
                Log.i(getPackageName(), "Magisk Binary Detected");
        }
    }

    private void initConversion() {
        mDisposable.add(Observable.fromCallable(() -> new ConvertTask(this).convert())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(success -> {
                    if (success) {
                        Toast.makeText(this, "Successful", Toast.LENGTH_LONG).show();
                        Shell.SU.run("reboot");
                    } else
                        Toast.makeText(this, "UnSuccessful", Toast.LENGTH_LONG).show();
                }));
    }

    private void showSystemDialog() {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        Fragment prev = getSupportFragmentManager().findFragmentByTag("dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);
        GenericDialog dialog = new GenericDialog();
        dialog.setTitle(getResources().getString(R.string.dialog_title));
        dialog.setMessage(getResources().getString(R.string.dialog_msg));
        dialog.setPositiveButton(getResources().getString(R.string.action_now), v -> {
            initConversion();
            dialog.dismiss();
        });
        dialog.setNegativeButton(getResources().getString(R.string.action_later), v -> {
            dialog.dismiss();
        });
        dialog.show(ft, "dialog");
    }

    private void showRemoveDialog() {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        Fragment prev = getSupportFragmentManager().findFragmentByTag("dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);
        GenericDialog dialog = new GenericDialog();
        dialog.setTitle(getResources().getString(R.string.remove_dialog_title));
        dialog.setMessage(getResources().getString(R.string.remove_dialog_msg));
        dialog.setPositiveButton(getResources().getString(R.string.action_now), v -> {
            removeLauncherIcon();
            dialog.dismiss();
        });
        dialog.setNegativeButton(getResources().getString(R.string.action_later), v -> {
            dialog.dismiss();
        });
        dialog.show(ft, "dialog");
    }

    private void removeLauncherIcon() {
        PackageManager p = getPackageManager();
        ComponentName componentName = new ComponentName(this, getClass());
        p.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
    }

}
