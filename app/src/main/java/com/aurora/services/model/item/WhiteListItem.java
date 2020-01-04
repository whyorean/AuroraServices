package com.aurora.services.model.item;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aurora.services.R;
import com.aurora.services.manager.WhitelistManager;
import com.aurora.services.model.App;
import com.aurora.services.utils.ImageUtil;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.items.AbstractItem;
import com.mikepenz.fastadapter.listeners.ClickEventHook;
import com.mikepenz.fastadapter.select.SelectExtension;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class WhiteListItem extends AbstractItem<WhiteListItem.ViewHolder> {

    private App app;
    private String packageName;
    private boolean checked;

    public WhiteListItem(App app, boolean checked) {
        this.app = app;
        this.checked = checked;
    }

    @Override
    public int getLayoutRes() {
        return R.layout.item_checkbox;
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

    public static class ViewHolder extends FastAdapter.ViewHolder<WhiteListItem> {
        @BindView(R.id.img)
        ImageView img;
        @BindView(R.id.line1)
        TextView line1;
        @BindView(R.id.line2)
        TextView line2;
        @BindView(R.id.checkbox)
        MaterialCheckBox checkBox;

        private Context context;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            context = itemView.getContext();
        }

        @Override
        public void bindView(@NotNull WhiteListItem item, @NotNull List<?> list) {
            final App app = item.getApp();

            line1.setText(app.getDisplayName());
            line2.setText(app.getPackageName());
            checkBox.setChecked(new WhitelistManager(context).isWhitelisted(app.getPackageName()));
            img.setImageBitmap(ImageUtil.convert(app.getIconBase64()));
        }

        @Override
        public void unbindView(@NotNull WhiteListItem item) {
            line1.setText(null);
            line2.setText(null);
            img.setImageBitmap(null);
        }
    }

    public static final class CheckBoxClickEvent extends ClickEventHook<WhiteListItem> {
        @Nullable
        public View onBind(@NotNull RecyclerView.ViewHolder viewHolder) {
            return viewHolder instanceof ViewHolder
                    ? ((ViewHolder) viewHolder).checkBox
                    : null;
        }

        @Override
        public void onClick(@NotNull View view, int position, @NotNull FastAdapter<WhiteListItem> fastAdapter, @NotNull WhiteListItem item) {
            SelectExtension<WhiteListItem> selectExtension = fastAdapter.getExtension(SelectExtension.class);
            if (selectExtension != null) {
                selectExtension.toggleSelection(position);
                item.checked = !item.checked;
            }
        }
    }
}
