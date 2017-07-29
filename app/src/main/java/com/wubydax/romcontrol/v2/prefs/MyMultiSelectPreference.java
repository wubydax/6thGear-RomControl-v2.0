package com.wubydax.romcontrol.v2.prefs;

import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.preference.Preference;
import android.provider.Settings;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RadioButton;

import com.wubydax.romcontrol.v2.R;
import com.wubydax.romcontrol.v2.utils.MultiSelectAdapter;
import com.wubydax.romcontrol.v2.utils.SelectionItem;
import com.wubydax.romcontrol.v2.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/*
Created by Roberto Mariani & Anna Berkovitch on 27-Jul-17.

This file is part of 6thGear-RomControl-v2.0

        6thGear-RomControl-v2.0 is free software: you can redistribute it and/or modify
        it under the terms of the GNU General Public License as published by
        the Free Software Foundation, either version 3 of the License, or
        (at your option) any later version.

        6thGear-RomControl-v2.0 is distributed in the hope that it will be useful,
        but WITHOUT ANY WARRANTY; without even the implied warranty of
        MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
        GNU General Public License for more details.

        You should have received a copy of the GNU General Public License
        along with 6thGear-RomControl-v2.0.  If not, see <http://www.gnu.org/licenses/>.
*/


public class MyMultiSelectPreference extends DialogPreference implements MultiSelectAdapter.OnItemSelectedListener {
    private final String mPackageToKill;
    private final boolean mIsSilent;
    private final boolean mIsRebootRequired;
    private final String mReverseDependencyKey;
    private ContentResolver mContentResolver;
    private String[] mEntries, mValues;
    private MultiSelectAdapter mMultiSelectAdapter;
    private String mValue;
    private int mCount;
    private RadioButton mRadioButton;

    public MyMultiSelectPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContentResolver = context.getContentResolver();
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.MyMultiSelectPreference);
        mEntries = context.getResources().getStringArray(typedArray.getResourceId(R.styleable.MyMultiSelectPreference_multiEntryList, -1));
        mValues = context.getResources().getStringArray(typedArray.getResourceId(R.styleable.MyMultiSelectPreference_multiValuesList, -1));
        typedArray.recycle();
        TypedArray generalTypedArray = context.obtainStyledAttributes(attrs, R.styleable.Preference);
        mPackageToKill = generalTypedArray.getString(R.styleable.Preference_packageNameToKill);
        mIsSilent = generalTypedArray.getBoolean(R.styleable.Preference_isSilent, true);
        mReverseDependencyKey = generalTypedArray.getString(R.styleable.Preference_reverseDependency);
        mIsRebootRequired = generalTypedArray.getBoolean(R.styleable.Preference_rebootDevice, false);

        generalTypedArray.recycle();

        setDialogLayoutResource(R.layout.multi_select_preference_dialog_layout);


        if (mEntries == null || mValues == null || mEntries.length != mValues.length || mEntries.length == 0)
            throw new IllegalArgumentException("Data for preference is missing or improperly formatted. Please verify the arrays are all present and are all of equal size");

    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getString(index);
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        String value = Settings.System.getString(mContentResolver, getKey());
        if (value == null) {
            value = defaultValue != null ? (String) defaultValue : "";
            Settings.System.putString(mContentResolver, getKey(), value);
        }
        persistString(value);
        mValue = value;
        setSummary(getSummaryString());
    }

    private String getSummaryString() {
        StringBuilder stringBuilder = new StringBuilder();
        if (!TextUtils.isEmpty(mValue)) {
            String[] values = mValue.split(",");
            if (values.length > 0) {
                for (String value : values) {
                    if (stringBuilder.length() > 0) {
                        stringBuilder.append(", ");
                    }
                    stringBuilder.append(mEntries[getEntryForValue(value)]);
                }
            }
        }
        return stringBuilder.toString();
    }

    private int getEntryForValue(String value) {
        List<String> listValues = Arrays.asList(mValues);
        return listValues.indexOf(value);
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
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        mMultiSelectAdapter = new MultiSelectAdapter(getData(), this);
        mCount = 0;


        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.multi_select_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        recyclerView.setAdapter(mMultiSelectAdapter);
        mRadioButton = (RadioButton) view.findViewById(R.id.select_all_radio_button);
        mMultiSelectAdapter.setSelectedItems(mValue);
        mRadioButton.setChecked(mCount == mEntries.length);
        view.findViewById(R.id.select_all_main_container).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mRadioButton.setChecked(!mRadioButton.isChecked());
                mMultiSelectAdapter.selectAll(mRadioButton.isChecked());
                mCount = mRadioButton.isChecked() ? mEntries.length : 0;
            }
        });


    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            mValue = mMultiSelectAdapter.getSelectedItems();
            persistString(mValue);
            Settings.System.putString(mContentResolver, getKey(), mValue);
            setSummary(getSummaryString());
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
        super.onClick(dialog, which);
    }

    private ArrayList<SelectionItem> getData() {
        ArrayList<SelectionItem> list = new ArrayList<>();
        for (int i = 0; i < mEntries.length; i++) {
            SelectionItem selectionItem = new SelectionItem();
            selectionItem.entry = mEntries[i];
            selectionItem.value = mValues[i];
            list.add(selectionItem);
        }
        return list;
    }

    @Override
    public void onItemSelected(boolean isSelected) {
        if (isSelected) {
            mCount++;
            if (mCount == mEntries.length) mRadioButton.setChecked(true);
        } else {
            if (mCount == mEntries.length) mRadioButton.setChecked(false);
            mCount--;
        }
    }
}
