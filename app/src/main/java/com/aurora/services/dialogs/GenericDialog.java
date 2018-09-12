package com.aurora.services.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.aurora.services.R;
import com.aurora.services.fragments.AuroraDialog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialogFragment;
import butterknife.BindView;
import butterknife.ButterKnife;

public class GenericDialog extends AppCompatDialogFragment {

    @BindView(R.id.button_positive)
    Button buttonPositive;
    @BindView(R.id.button_negative)
    Button buttonNegative;
    @BindView(R.id.dialog_title)
    TextView title;
    @BindView(R.id.dialog_message)
    TextView message;

    private String txt_title;
    private String txt_message;

    private String txt_button_positive;
    private String txt_button_negative;

    private View.OnClickListener mOnClickListener_positive;
    private View.OnClickListener mOnClickListener_negative;


    public void setTitle(String txt_title) {
        this.txt_title = txt_title;
    }

    public void setMessage(String txt_message) {
        this.txt_message = txt_message;
    }

    public void setPositiveButton(String txt_button_positive, View.OnClickListener mOnClickListener_positive) {
        this.txt_button_positive = txt_button_positive;
        this.mOnClickListener_positive = mOnClickListener_positive;
    }

    public void setNegativeButton(String txt_button_negative, View.OnClickListener mOnClickListener_negative) {
        this.txt_button_negative = txt_button_negative;
        this.mOnClickListener_negative = mOnClickListener_negative;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_generic, container, false);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AuroraDialog(getContext(), R.style.Theme_Aurora_Dialog);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);
        drawViews();
    }

    public void drawViews() {
        if (txt_title != null) {
            title.setText(txt_title);
            title.setVisibility(View.VISIBLE);
        }

        if (txt_message != null) {
            message.setText(txt_message);
            message.setVisibility(View.VISIBLE);
        }

        if (txt_button_positive != null) {
            buttonPositive.setText(txt_button_positive);
            buttonPositive.setOnClickListener(mOnClickListener_positive);
            buttonPositive.setVisibility(View.VISIBLE);
        }

        if (txt_button_negative != null) {
            buttonNegative.setText(txt_button_negative);
            buttonNegative.setOnClickListener(mOnClickListener_negative);
            buttonNegative.setVisibility(View.VISIBLE);
        }
    }

}
