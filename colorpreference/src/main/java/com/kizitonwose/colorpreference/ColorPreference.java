package com.kizitonwose.colorpreference;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.Preference;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.GridLayout;


public class ColorPreference extends Preference {
    private int[] mColorChoices = {};
    private int mValue = 0;
    private int mItemLayoutId = R.layout.pref_color_layout;
    private int mItemLayoutLargeId = R.layout.pref_color_layout_large;
    private int mNumColumns = 5;
    private View mPreviewView;
    private int mColorShape = 1;
    private boolean showDialog = true;
    private int previewSize = 1;

    public ColorPreference(Context context) {
        super(context);
        initAttrs(null, 0);
    }

    public ColorPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        initAttrs(attrs, 0);
    }

    public ColorPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initAttrs(attrs, defStyle);
    }

    private void initAttrs(AttributeSet attrs, int defStyle) {
        TypedArray a = getContext().getTheme().obtainStyledAttributes(
                attrs, R.styleable.ColorPreference, defStyle, defStyle);

        try {
            //mItemLayoutId = a.getResourceId(R.styleable.ColorPreference_itemLayout, mItemLayoutId);
            mNumColumns = a.getInteger(R.styleable.ColorPreference_numColumns, mNumColumns);
            mColorShape = a.getInteger(R.styleable.ColorPreference_colorShape, 1);
            previewSize = a.getInteger(R.styleable.ColorPreference_viewSize, 1);
            showDialog = a.getBoolean(R.styleable.ColorPreference_showDialog, true);
            int choicesResId = a.getResourceId(R.styleable.ColorPreference_colorChoices,
                    R.array.default_color_choice_values);
            if (choicesResId > 0) {
                String[] choices = a.getResources().getStringArray(choicesResId);

                mColorChoices = new int[choices.length];
                for (int i = 0; i < choices.length; i++) {
                    mColorChoices[i] = Color.parseColor(choices[i]);
                }
            }

        } finally {
            a.recycle();
        }

        setWidgetLayoutResource(previewSize == 1 ? mItemLayoutId : mItemLayoutLargeId);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        mPreviewView = view.findViewById(R.id.color_view);
        ColorUtils.setColorViewValue(mPreviewView, mValue, false, mColorShape);
    }

    public void setValue(int value) {
        if (callChangeListener(value)) {
            mValue = value;
            persistInt(value);
            notifyChanged();
        }
    }

    @Override
    protected void onClick() {
        super.onClick();

        if (showDialog) {
            ColorDialogFragment fragment = ColorDialogFragment.newInstance();
            fragment.setPreference(this);

            Activity activity = (Activity) getContext();
            activity.getFragmentManager().beginTransaction()
                    .add(fragment, getFragmentTag())
                    .commit();
        }
    }

    @Override
    protected void onAttachedToActivity() {
        super.onAttachedToActivity();
        if (showDialog) {
            Activity activity = (Activity) getContext();
            ColorDialogFragment fragment = (ColorDialogFragment) activity
                    .getFragmentManager().findFragmentByTag(getFragmentTag());
            if (fragment != null) {
                // re-bind preference to fragment
                fragment.setPreference(this);
            }
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInt(index, 0);
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        setValue(restoreValue ? getPersistedInt(0) : (Integer) defaultValue);
    }

    public String getFragmentTag() {
        return "color_" + getKey();
    }

    public int getValue() {
        return mValue;
    }

    public int getNumColumns() {
        return mNumColumns;
    }

    public int[] getColorChoices() {
        return mColorChoices;
    }

    public int getColorShape() {
        return mColorShape;
    }

    public static class ColorDialogFragment extends DialogFragment {
        private ColorPreference mPreference;
        private GridLayout mColorGrid;

        public ColorDialogFragment() {
        }

        public static ColorDialogFragment newInstance() {
            return new ColorDialogFragment();
        }

        public void setPreference(ColorPreference preference) {
            mPreference = preference;
            repopulateItems();
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            repopulateItems();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
            View rootView = layoutInflater.inflate(R.layout.dialog_colors, null);

            mColorGrid = (GridLayout) rootView.findViewById(R.id.color_grid);
            mColorGrid.setColumnCount(mPreference.mNumColumns);
            repopulateItems();

            return new AlertDialog.Builder(getActivity())
                    .setView(rootView)
                    .create();
        }

        private void repopulateItems() {
            if (mPreference == null || mColorGrid == null) {
                return;
            }

            Context context = mColorGrid.getContext();
            mColorGrid.removeAllViews();
            for (final int color : mPreference.mColorChoices) {
                View itemView = LayoutInflater.from(context)
                        .inflate(R.layout.grid_item_color, mColorGrid, false);

                ColorUtils.setColorViewValue(itemView.findViewById(R.id.color_view), color,
                        color == mPreference.getValue(), mPreference.mColorShape);
                itemView.setClickable(true);
                itemView.setFocusable(true);
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mPreference.setValue(color);
                        dismiss();
                    }
                });

                mColorGrid.addView(itemView);
            }

            sizeDialog();
        }

        @Override
        public void onStart() {
            super.onStart();
            sizeDialog();
        }

        private void sizeDialog() {
            if (mPreference == null || mColorGrid == null) {
                return;
            }

            Dialog dialog = getDialog();
            if (dialog == null) {
                return;
            }

            final Resources res = mColorGrid.getContext().getResources();
            DisplayMetrics dm = res.getDisplayMetrics();

            // Can't use Integer.MAX_VALUE here (weird issue observed otherwise on 4.2)
            mColorGrid.measure(
                    View.MeasureSpec.makeMeasureSpec(dm.widthPixels, View.MeasureSpec.AT_MOST),
                    View.MeasureSpec.makeMeasureSpec(dm.heightPixels, View.MeasureSpec.AT_MOST));
            int width = mColorGrid.getMeasuredWidth();
            int height = mColorGrid.getMeasuredHeight();

            int extraPadding = res.getDimensionPixelSize(R.dimen.color_grid_extra_padding);

            width += extraPadding;
            height += extraPadding;

            dialog.getWindow().setLayout(width, height);
        }
    }






}