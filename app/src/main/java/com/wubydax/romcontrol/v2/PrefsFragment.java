package com.wubydax.romcontrol.v2;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import com.wubydax.romcontrol.v2.prefs.OpenAppPreference;
import com.wubydax.romcontrol.v2.prefs.UriSelectionPreference;
import com.wubydax.romcontrol.v2.utils.Constants;

/*      Created by Roberto Mariani and Anna Berkovitch, 2015-2016
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
@SuppressWarnings("WeakerAccess")
public class PrefsFragment extends PreferenceFragment implements Preference.OnPreferenceClickListener,
        UriSelectionPreference.OnUriSelectionRequestedListener {
    private static final String LOG_TAG = PrefsFragment.class.getSimpleName();
    private Context mContext;
    private SharedPreferences mSharedPreferences;
    private String mUriPreferenceKey;

    public PrefsFragment() {
        //empty public constructor
    }

    static PrefsFragment newInstance(String prefName) {
        PrefsFragment prefsFragment = new PrefsFragment();
        Bundle args = new Bundle();
        args.putString(Constants.PREF_NAME_KEY, prefName);
        prefsFragment.setArguments(args);
        return prefsFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = MyApp.getContext();
        String prefName = getArguments().getString(Constants.PREF_NAME_KEY);
        int prefId = mContext.getResources().getIdentifier(prefName, "xml", mContext.getPackageName());
        if (prefId != 0) {
            getPreferenceManager().setSharedPreferencesName(prefName);
            addPreferencesFromResource(prefId);
            mSharedPreferences = getPreferenceManager().getSharedPreferences();
            iteratePrefs(getPreferenceScreen());
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }
        switch (requestCode) {
            case 46:
                Settings.System.putString(mContext.getContentResolver(), mUriPreferenceKey, data.getData().toString());
                mSharedPreferences.edit().putString(mUriPreferenceKey, data.getData().toString()).apply();
                ((UriSelectionPreference) findPreference(mUriPreferenceKey)).attemptToSetIcon(data.getData().toString());
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);

        }

    }

    private void iteratePrefs(PreferenceGroup preferenceGroup) {
        for (int i = 0; i < preferenceGroup.getPreferenceCount(); i++) {
            Preference preference = preferenceGroup.getPreference(i);
            if (preference instanceof PreferenceGroup) {
                if (preference instanceof PreferenceScreen) {
                    preference.setOnPreferenceClickListener(this);
                }
                if (((PreferenceGroup) preference).getPreferenceCount() > 0) {
                    iteratePrefs((PreferenceGroup) preference);
                }
            } else if (preference instanceof OpenAppPreference) {
                if (!((OpenAppPreference) preference).isInstalled()) {
                    if(preferenceGroup.removePreference(preference)) {
                        i--;
                    }
                }
            } else if (preference instanceof UriSelectionPreference) {
                ((UriSelectionPreference) preference).setOnUriSelectionRequestedListener(this);
            }
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (((PreferenceScreen) preference).getPreferenceCount() > 0) {
            setUpNestedPreferenceLayout((PreferenceScreen) preference);
        } else if(preference.getIntent() != null) {
            if(MyApp.getContext().getPackageManager().resolveActivity(preference.getIntent(), 0) != null) {
                startActivity(preference.getIntent());
            }
        }
        return true;
    }

    private void setUpNestedPreferenceLayout(PreferenceScreen preference) {
        final Dialog dialog = preference.getDialog();
        if (dialog != null) {
            LinearLayout rootView = (LinearLayout) dialog.findViewById(android.R.id.list).getParent();
            View decorView = dialog.getWindow().getDecorView();
            if (decorView != null && rootView != null) {
                Toolbar toolbar = (Toolbar) LayoutInflater.from(getActivity()).inflate(R.layout.nested_preference_toolbar_layout, rootView, false);
                toolbar.setTitle(preference.getTitle());
                decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
                rootView.addView(toolbar, 0);
                toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                });
            }
        }
    }


    @Override
    public void onUriSelectionRequested(String key) {
        mUriPreferenceKey = key;
        Intent getContentIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        getContentIntent.setType("image/*");
        startActivityForResult(getContentIntent, 46);
    }


}
