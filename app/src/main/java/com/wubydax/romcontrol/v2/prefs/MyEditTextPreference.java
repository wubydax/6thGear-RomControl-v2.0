package com.wubydax.romcontrol.v2.prefs;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.TypedArray;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.AttributeSet;

import com.wubydax.romcontrol.v2.R;
import com.wubydax.romcontrol.v2.utils.Utils;

/*      Created by Roberto Mariani and Anna Berkovitch, 08/06/15
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
public class MyEditTextPreference extends EditTextPreference implements Preference.OnPreferenceChangeListener {
    private final boolean mIsSilent;
    private final String mPackageToKill;
    private final boolean mIsRebootRequired;
    private ContentResolver mContentResolver;
    private String mReverseDependencyKey;

    public MyEditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.Preference);
        mPackageToKill = typedArray.getString(R.styleable.Preference_packageNameToKill);
        mIsSilent = typedArray.getBoolean(R.styleable.Preference_isSilent, true);
        mIsRebootRequired = typedArray.getBoolean(R.styleable.Preference_rebootDevice, false);
        mReverseDependencyKey = typedArray.getString(R.styleable.Preference_reverseDependency);
        typedArray.recycle();
        mContentResolver = context.getContentResolver();
        setOnPreferenceChangeListener(this);
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
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        super.onSetInitialValue(restoreValue, defaultValue);
        String value = "";

        if (!restoreValue && defaultValue != null) {
            String dbValue = Settings.System.getString(mContentResolver, getKey());
            if (dbValue != null && !dbValue.equals(defaultValue)) {
                value = dbValue;
            } else if (dbValue == null) {
                value = (String) defaultValue;
                Settings.System.putString(mContentResolver, getKey(), (String) defaultValue);
            }
        } else {
            value = getPersistedString(null);
        }
        setSummary(value);

    }

    @Override
    public String getText() {
        String value = Settings.System.getString(mContentResolver, getKey());
        String persistedString = getPersistedString(null);
        if (value != null) {
            if (value.equals(persistedString)) {
                return persistedString;
            } else {
                persistString(value);
                return value;
            }
        } else {
            return persistedString;
        }
    }

    @Override
    protected void onAttachedToHierarchy(PreferenceManager preferenceManager) {
        super.onAttachedToHierarchy(preferenceManager);
        String value = Settings.System.getString(mContentResolver, getKey());
        String persistedString = getPersistedString(null);
        if (value != null && !value.equals(persistedString)) {
            persistString(value);
            setSummary(value);
        }
    }


    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Settings.System.putString(mContentResolver, getKey(), (String) newValue);
        setSummary((String) newValue);
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
        return true;
    }
}
