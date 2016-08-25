package com.wubydax.romcontrol.v2.prefs;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.TypedArray;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;

import com.wubydax.romcontrol.v2.R;
import com.wubydax.romcontrol.v2.utils.Utils;

import java.util.ArrayList;

/*      Created by Roberto Mariani and Anna Berkovitch, 13/06/2016
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

public class MySwitchPreference extends SwitchPreference implements Preference.OnPreferenceChangeListener,
        ReverseDependencyMonitor {
    private ContentResolver mContentResolver;
    private String mPackageToKill;
    private boolean mIsSilent, mIsRebootRequired;
    private ArrayList<Preference> mReverseDependents;
    private String mReverseDependencyKey;

    public MySwitchPreference(Context context, AttributeSet attrs) {
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
        int dbInt = 0;
        try {
            dbInt = Settings.System.getInt(mContentResolver, getKey());
        } catch (Settings.SettingNotFoundException e) {
            if (defaultValue != null) {
                dbInt = (boolean) defaultValue ? 1 : 0;
                Settings.System.putInt(mContentResolver, getKey(), dbInt);
            }
        }
        persistBoolean(dbInt != 0);
        setChecked(dbInt != 0);

    }


    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean isTrue = (boolean) newValue;
        int dbInt = isTrue ? 1 : 0;
        Settings.System.putInt(mContentResolver, getKey(), dbInt);
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

        if (mReverseDependents != null && mReverseDependents.size() > 0) {
            for (Preference pref : mReverseDependents) {
                pref.setEnabled(!isTrue);
            }
        }

        return true;
    }


    @Override
    public void registerReverseDependencyPreference(Preference preference) {
        if (mReverseDependents == null) {
            mReverseDependents = new ArrayList<>();
        }
        if (preference != null && !mReverseDependents.contains(preference)) {
            mReverseDependents.add(preference);
            preference.setEnabled(!isChecked());
            Log.d("daxgirl", "registerReverseDependencyPreference preference is " + preference.getClass().getSimpleName());
        }

    }
}
