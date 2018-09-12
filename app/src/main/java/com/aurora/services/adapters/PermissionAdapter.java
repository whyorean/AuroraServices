package com.aurora.services.adapters;


import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.aurora.services.R;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;

public class PermissionAdapter extends RecyclerView.Adapter<PermissionAdapter.ViewHolder> {

    private AppCompatActivity mActivity;
    private Resources mResources;
    private String permissionNames[];
    private String permissionValues[];

    public PermissionAdapter(AppCompatActivity mActivity, String[] permissionNames, String[] permissionValues) {
        this.mActivity = mActivity;
        this.permissionNames = permissionNames;
        this.permissionValues = permissionValues;
        mResources = mActivity.getResources();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).
                inflate(R.layout.item_permissions, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.permission_name.setText(permissionNames[position]);
        holder.permission_value.setText(permissionValues[position]);
        if (mActivity.checkCallingOrSelfPermission(permissionValues[position]) != PackageManager.PERMISSION_GRANTED) {
            holder.permission_status.setText(mResources.getString(R.string.perm_not_granted));
            holder.permission_status.setTextColor(mResources.getColor(R.color.colorRed));
            holder.permission_layout.setOnClickListener(v -> {
                askPermission(permissionValues[position]);
            });
        }
    }

    private void askPermission(String permissionValue) {
        ActivityCompat.requestPermissions(mActivity, new String[]{permissionValue}, 1);
    }

    @Override
    public int getItemCount() {
        return permissionNames.length;
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.permission_name)
        TextView permission_name;
        @BindView(R.id.permission_value)
        TextView permission_value;
        @BindView(R.id.permission_status)
        TextView permission_status;
        @BindView(R.id.permission_layout)
        RelativeLayout permission_layout;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
