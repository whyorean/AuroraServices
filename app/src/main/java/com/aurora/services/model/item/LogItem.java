package com.aurora.services.model.item;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.aurora.services.R;
import com.aurora.services.model.App;
import com.aurora.services.utils.ImageUtil;
import com.aurora.services.utils.Util;
import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.items.AbstractItem;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class LogItem extends AbstractItem<LogItem.ViewHolder> {

    private App app;
    private String packageName;

    public LogItem(App app) {
        this.app = app;
    }

    @Override
    public int getLayoutRes() {
        return R.layout.item_log;
    }

    @NotNull
    @Override
    public ViewHolder getViewHolder(@NotNull View view) {
        return new ViewHolder(view);
    }

    @Override
    public int getType() {
        return R.id.fastadapter_item;
    }

    public static class ViewHolder extends FastAdapter.ViewHolder<LogItem> {

        @BindView(R.id.img_icon)
        ImageView img;
        @BindView(R.id.line1)
        TextView line1;
        @BindView(R.id.line2)
        TextView line2;
        @BindView(R.id.line3)
        TextView line3;

        private Context context;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            context = itemView.getContext();
        }

        @Override
        public void bindView(@NotNull LogItem item, @NotNull List<?> list) {
            final App app = item.getApp();

            line1.setText(app.getDisplayName());
            line2.setText(app.getPackageName());
            line3.setText(StringUtils.joinWith(" • ", Util.millisToDay(app.getInstalledTime()), Util.millisToTime(app.getInstalledTime())));
            img.setImageBitmap(ImageUtil.convert(app.getIconBase64()));
        }

        @Override
        public void unbindView(@NotNull LogItem item) {
            line1.setText(null);
            line2.setText(null);
            line3.setText(null);
            img.setImageBitmap(null);
        }
    }
}
