/*
 * Copyright (C) 2011 Sergey Margaritov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wubydax.romcontrol.v2.prefs;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Color;
import android.graphics.Outline;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.Preference;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.wubydax.romcontrol.v2.R;
import com.wubydax.romcontrol.v2.utils.Utils;

/**
 * A preference type that allows a user to choose a time
 *
 * @author Sergey Margaritov
 */
@SuppressWarnings("JavaDoc")
public class ColorPickerPreference
        extends
        Preference
        implements
        Preference.OnPreferenceClickListener,
        ColorPickerDialog.OnColorChangedListener {

    private final String mPackageToKill;
    private final boolean mIsSilent;
    private final boolean mIsRebootRequired;
    private View mView;
    private ColorPickerDialog mDialog;
    private int mValue = Color.WHITE;
    private float mDensity = 0;
    private boolean mAlphaSliderEnabled;
    private boolean mHexValueEnabled;
    private boolean mIsInitialSetup;
    private String mReverseDependencyKey;


    public ColorPickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.Preference);
        mPackageToKill = typedArray.getString(R.styleable.Preference_packageNameToKill);
        mIsSilent = typedArray.getBoolean(R.styleable.Preference_isSilent, true);
        mIsRebootRequired = typedArray.getBoolean(R.styleable.Preference_rebootDevice, false);
        mReverseDependencyKey = typedArray.getString(R.styleable.Preference_reverseDependency);
        typedArray.recycle();
    }


    static String convertToARGB(int color) {
        String alpha = Integer.toHexString(Color.alpha(color));
        String red = Integer.toHexString(Color.red(color));
        String green = Integer.toHexString(Color.green(color));
        String blue = Integer.toHexString(Color.blue(color));

        if (alpha.length() == 1) {
            alpha = "0" + alpha;
        }

        if (red.length() == 1) {
            red = "0" + red;
        }

        if (green.length() == 1) {
            green = "0" + green;
        }

        if (blue.length() == 1) {
            blue = "0" + blue;
        }

        return "#" + alpha + red + green + blue;
    }

    /**
     * For custom purposes. Not used by ColorPickerPreference
     *
     * @param color
     * @return A string representing the hex value of color,
     * without the alpha value
     * @author Charles Rosaaen
     */
    static String convertToRGB(int color) {
        String red = Integer.toHexString(Color.red(color));
        String green = Integer.toHexString(Color.green(color));
        String blue = Integer.toHexString(Color.blue(color));

        if (red.length() == 1) {
            red = "0" + red;
        }

        if (green.length() == 1) {
            green = "0" + green;
        }

        if (blue.length() == 1) {
            blue = "0" + blue;
        }

        return "#" + red + green + blue;
    }

    /**
     * For custom purposes. Not used by ColorPickerPreference
     *
     * @param argb
     * @throws NumberFormatException
     * @author Unknown
     */
    static int convertToColorInt(String argb) throws IllegalArgumentException {

        if (!argb.startsWith("#")) {
            argb = "#" + argb;
        }

        return Color.parseColor(argb);
    }

    //Edited by Anna Berkovitch on July, 1st, 2015
    //Added the ability to set defaultValue as hex string
    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        int colorInt;

        String mHexDefaultValue = a.getString(index);
        if (mHexDefaultValue != null && mHexDefaultValue.startsWith("#")) {
            colorInt = convertToColorInt(mHexDefaultValue);
        } else {
            colorInt = a.getColor(index, Color.WHITE);
        }


        return colorInt;
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        int color;
        try {
            color = Settings.System.getInt(getContext().getContentResolver(), getKey());
        } catch (Settings.SettingNotFoundException e) {
            color = restoreValue ? getPersistedInt(mValue) : (int) defaultValue;
            Settings.System.putInt(getContext().getContentResolver(), getKey(), color);
        }
        mIsInitialSetup = true;
        onColorChanged(color);
    }

    private void init(AttributeSet attrs) {
        mDensity = getContext().getResources().getDisplayMetrics().density;
        setOnPreferenceClickListener(this);
        if (attrs != null) {
            mAlphaSliderEnabled = attrs.getAttributeBooleanValue(null, "alphaSlider", true);
            mHexValueEnabled = attrs.getAttributeBooleanValue(null, "hexValue", true);

        }
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        mView = view;
        setPreviewColor();
    }

    @Override
    protected void onAttachedToActivity() {
        super.onAttachedToActivity();
        if (!TextUtils.isEmpty(mReverseDependencyKey)) {
            Preference preference = findPreferenceInHierarchy(mReverseDependencyKey);
            if (preference != null && (preference instanceof MySwitchPreference || preference instanceof MyCheckBoxPreference)) {
                ReverseDependencyMonitor reverseDependencyMonitor = (ReverseDependencyMonitor) preference;
                reverseDependencyMonitor.registerReverseDependencyPreference(this);
            }
        }
    }

    private void setPreviewColor() {
        int size = Math.round(getContext().getResources().getDimension(R.dimen.button_size));
        if (mView == null) return;
        ImageView iView = new ImageView(getContext());
        LinearLayout widgetFrameView = ((LinearLayout) mView.findViewById(android.R.id.widget_frame));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
        params.gravity = Gravity.CENTER;
        params.setMargins(12, 12, 12, 12);

        iView.setLayoutParams(params);

        if (widgetFrameView == null) return;
        widgetFrameView.setVisibility(View.VISIBLE);
        widgetFrameView.setPadding(
                0, 0, 0, 0);
        // remove already create preview image
        int count = widgetFrameView.getChildCount();
        if (count > 0) {
            widgetFrameView.removeViews(0, count);
        }
        widgetFrameView.addView(iView);
        widgetFrameView.setMinimumWidth(0);
        iView.setBackground(new AlphaPatternDrawable((int) (5 * mDensity)));
        iView.setImageBitmap(getPreviewBitmap());
        // by Anna Berkovitch, edited on 16/06/2015, added Outline to make the preview round
        final ViewOutlineProvider viewOutlineProvider = new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                int size = getContext().getResources().getDimensionPixelSize(R.dimen.button_size);
                outline.setOval(0, 0, size, size);

            }
        };
        iView.setOutlineProvider(viewOutlineProvider);
        iView.setClipToOutline(true);
        iView.setElevation(10.0f);
    }

    private Bitmap getPreviewBitmap() {
        int d = Math.round(getContext().getResources().getDimension(R.dimen.button_size)); //30dip
        int color = mValue;
        Bitmap bm = Bitmap.createBitmap(d, d, Config.ARGB_8888);
        int w = bm.getWidth();
        int h = bm.getHeight();
        int c;
        for (int i = 0; i < w; i++) {
            for (int j = i; j < h; j++) {
                //by Anna Berkovitch, on 16/06/2015, removed grey rim around the preview bitmap. changed to ? color:color
                c = (i <= 1 || j <= 1 || i >= w - 2 || j >= h - 2) ? color : color;
                bm.setPixel(i, j, c);
                if (i != j) {
                    bm.setPixel(j, i, c);
                }
            }
        }


        return bm;
    }

    @Override
    public void onColorChanged(int color) {
        persistInt(color);
        Settings.System.putInt(getContext().getContentResolver(), getKey(), color);
        mValue = color;
        setPreviewColor();
        if (!mIsInitialSetup) {
            if (mIsRebootRequired) {
                Utils.showRebootRequiredDialog(getContext());
            } else {
                if (mPackageToKill != null) {
                    if (Utils.isPackageInstalled(mPackageToKill)) {
                        if (mIsSilent) {
                            Utils.killPackage(mPackageToKill);
                        } else {
                            Utils.showKillPackageDialog(mPackageToKill, getContext());
                        }
                    }
                }
            }
        }
        mIsInitialSetup = false;
    }

    // setColor added by Anna Berkovitch on 22/06/2015 to setColor onResume after preferences being changed by Settings.System int value
    public void setColor(int color) {
        mValue = color;
        setPreviewColor();
    }

    public boolean onPreferenceClick(Preference preference) {
        showDialog(null);
        return false;
    }

    private void showDialog(Bundle state) {
        mDialog = new ColorPickerDialog(getContext(), mValue);
        mDialog.setOnColorChangedListener(this);
        if (mAlphaSliderEnabled) {
            mDialog.setAlphaSliderVisible(true);
        }
        if (mHexValueEnabled) {
            mDialog.setHexValueEnabled(true);
        }
        if (state != null) {
            mDialog.onRestoreInstanceState(state);
        }
        // added by Anna Berkovitch on 16/06/2015 to set dialog bg according to theme
        assert mDialog.getWindow() != null;
        mDialog.getWindow().setBackgroundDrawableResource(R.drawable.inset_dialog_bg);
        mDialog.show();

    }

    /**
     * Toggle Alpha Slider visibility (by default it's disabled)
     *
     * @param enable
     */
//    public void setAlphaSliderEnabled(boolean enable) {
//        mAlphaSliderEnabled = enable;
//    }

    /**
     * Toggle Hex Value visibility (by default it's disabled)
     */
//    public void setHexValueEnabled(boolean enable) {
//        mHexValueEnabled = enable;
//    }
    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        if (mDialog == null || !mDialog.isShowing()) {
            return superState;
        }

        final SavedState myState = new SavedState(superState);
        myState.dialogBundle = mDialog.onSaveInstanceState();
        return myState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state == null || !(state instanceof SavedState)) {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());
        showDialog(myState.dialogBundle);
    }

    private static class SavedState extends BaseSavedState {
        @SuppressWarnings("unused")
        public static final Creator<SavedState> CREATOR =
                new Creator<SavedState>() {
                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }

                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
        Bundle dialogBundle;

        @SuppressLint("ParcelClassLoader")
        SavedState(Parcel source) {
            super(source);
            dialogBundle = source.readBundle();
        }

        SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeBundle(dialogBundle);
        }
    }
}