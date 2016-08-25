package com.wubydax.romcontrol.v2.prefs;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.TypedArray;
import android.preference.Preference;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.wubydax.romcontrol.v2.R;
import com.wubydax.romcontrol.v2.utils.Utils;

import java.util.Locale;

/*      Created by Roberto Mariani and Anna Berkovitch, 30/06/2016
        This program is free software: you can redistribute it and/or modify
        it under the terms of the GNU General Public License as published by
        the Free Software Foundation, either version 3 of the License, or
        (at your option) any later version.

        This program is distributed in the hope that it will be useful,
        but WITHOUT ANY WARRANTY; without even the implied warranty of
        MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
        GNU General Public License for more details.

        You should have received a copy of the GNU General Public License
        along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

public class MySeekBarPreference extends Preference implements SeekBar.OnSeekBarChangeListener {
    private static final String LOG_TAG = MySeekBarPreference.class.getSimpleName();
    private final String mPackageToKill;
    private final boolean mIsSilent;
    private final boolean mIsRebootRequired;
    private int mMinValue, mMaxValue, mDefaultValue;
    private String mUnitValue, mFormat = "%d%s";
    private TextView mValueText;
    private ContentResolver mContentResolver;
    private String mReverseDependencyKey;


    public MySeekBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContentResolver = context.getContentResolver();
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.MySeekBarPreference);
        mMaxValue = typedArray.getInt(R.styleable.MySeekBarPreference_maxValue, 100);
        mMinValue = typedArray.getInt(R.styleable.MySeekBarPreference_minValue, 0);
        TypedArray generalTypedArray = context.obtainStyledAttributes(attrs, R.styleable.Preference);
        mIsRebootRequired = generalTypedArray.getBoolean(R.styleable.Preference_rebootDevice, false);
        mPackageToKill = generalTypedArray.getString(R.styleable.Preference_packageNameToKill);
        mIsSilent = generalTypedArray.getBoolean(R.styleable.Preference_isSilent, true);
        mReverseDependencyKey = generalTypedArray.getString(R.styleable.Preference_reverseDependency);
        mDefaultValue = mMaxValue / 2;
        mUnitValue = typedArray.getString(R.styleable.MySeekBarPreference_unitsValue);
        if (mUnitValue == null) {
            mUnitValue = "";
        }
        typedArray.recycle();
        generalTypedArray.recycle();
        setWidgetLayoutResource(R.layout.seekbar_preference_layout);
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

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInt(index, mDefaultValue);
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        int value;
        try {
            value = Settings.System.getInt(mContentResolver, getKey());
        } catch (Settings.SettingNotFoundException e) {
            value = !restorePersistedValue && defaultValue != null ? (int) defaultValue : getPersistedInt(mDefaultValue);
            Settings.System.putInt(getContext().getContentResolver(), getKey(), value);
        }
        persistInt(value);
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        LinearLayout view = (LinearLayout) super.onCreateView(parent);
        view.setOrientation(LinearLayout.VERTICAL);
        View widgetView = view.findViewById(android.R.id.widget_frame);

        widgetView.setPadding(0, 0, 0, 0);
        return view;
    }

    @Override
    protected void onBindView(View view) {
        SeekBar seekBar = (SeekBar) view.findViewById(R.id.seekBarPrefSlider);
        seekBar.setMax(mMaxValue - mMinValue);
        TextView maxText = (TextView) view.findViewById(R.id.maxValueText);
        TextView minText = (TextView) view.findViewById(R.id.minValueText);
        mValueText = (TextView) view.findViewById(R.id.valueText);
        maxText.setText(String.format(Locale.getDefault(), mFormat, mMaxValue, mUnitValue));
        minText.setText(String.format(Locale.getDefault(), mFormat, mMinValue, mUnitValue));
        mValueText.setText(String.format(Locale.getDefault(), mFormat, getPersistedInt(mMaxValue / 2), mUnitValue));
        seekBar.setOnSeekBarChangeListener(this);
        seekBar.setProgress(getPersistedInt(mDefaultValue) - mMinValue);
        super.onBindView(view);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        int updatedProgress = mMinValue + progress;
        mValueText.setText(String.format(Locale.getDefault(), mFormat, updatedProgress, mUnitValue));
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        persistInt(seekBar.getProgress() + mMinValue);
        onPreferenceChange(seekBar.getProgress() + mMinValue);
    }

    private void onPreferenceChange(int newValue) {
        Settings.System.putInt(mContentResolver, getKey(), newValue);
        Log.d(LOG_TAG, "onPreferenceChange is called and reboot required is " + mIsRebootRequired);
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
}
