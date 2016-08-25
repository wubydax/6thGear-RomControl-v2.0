package com.wubydax.romcontrol.v2.prefs;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.text.TextUtils;
import android.util.AttributeSet;

import com.wubydax.romcontrol.v2.R;

/**
 * Created by Anna Berkovitch on 18/08/2016.
 */

public class MyPreferenceCategory extends PreferenceCategory {

    private String mReverseDependencyKey;

    public MyPreferenceCategory(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.Preference);
        mReverseDependencyKey = typedArray.getString(R.styleable.Preference_reverseDependency);
        typedArray.recycle();
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
}
