package com.wubydax.romcontrol.v2.prefs;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.TypedArray;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.AttributeSet;

import com.wubydax.romcontrol.v2.R;
import com.wubydax.romcontrol.v2.utils.Utils;

import java.util.Arrays;
import java.util.List;

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
public class MyListPreference extends ListPreference implements Preference.OnPreferenceChangeListener {
    private final String mPackageToKill, mDependentValue;
    private final boolean mIsSilent;
    private final boolean mIsRebootRequired;
    private final String mReverseDependencyKey;
    private ContentResolver mContentResolver;
    private List<CharSequence> mEntries, mValues;


    public MyListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContentResolver = context.getContentResolver();
        mEntries = Arrays.asList(getEntries());
        mValues = Arrays.asList(getEntryValues());
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.Preference);
        mPackageToKill = typedArray.getString(R.styleable.Preference_packageNameToKill);
        mIsSilent = typedArray.getBoolean(R.styleable.Preference_isSilent, true);
        mIsRebootRequired = typedArray.getBoolean(R.styleable.Preference_rebootDevice, false);
        mDependentValue = typedArray.getString(R.styleable.Preference_dependentValue);
        mReverseDependencyKey = typedArray.getString(R.styleable.Preference_reverseDependency);
        typedArray.recycle();
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
        String dbValue = Settings.System.getString(mContentResolver, getKey());
        String value = "";
        if (!restoreValue) {
            if (dbValue != null) {
                value = dbValue;
                persistString(value);
            } else {
                if (defaultValue != null) {
                    value = (String) defaultValue;
                    Settings.System.putString(mContentResolver, getKey(), (String) defaultValue);
                }
            }
        } else {
            value = getPersistedString(null);
            if (dbValue != null && !dbValue.equals(value)) {
                persistString(dbValue);
                value = dbValue;
            }
        }

        int index = mValues.indexOf(value);
        if (index != -1) {
            setSummary(mEntries.get(index));
            setValue(value);
        }
    }

    @Override
    public void setValue(String value) {
        String oldValue = getValue();
        super.setValue(value);
        if(!value.equals(oldValue)) {
            notifyDependencyChange(shouldDisableDependents());
        }
    }

    @Override
    public boolean shouldDisableDependents() {
        return super.shouldDisableDependents() || getValue() == null || getValue().equals(mDependentValue);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Settings.System.putString(mContentResolver, getKey(), (String) newValue);

        int index = mValues.indexOf(newValue);
        if (index != -1) {
            setSummary(mEntries.get(index));
        }
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
