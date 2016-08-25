package com.wubydax.romcontrol.v2.prefs;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.preference.Preference;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SectionIndexer;
import android.widget.TextView;

import com.wubydax.romcontrol.v2.R;
import com.wubydax.romcontrol.v2.utils.AppInfo;
import com.wubydax.romcontrol.v2.utils.AppListTask;
import com.wubydax.romcontrol.v2.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/*      Created by Roberto Mariani and Anna Berkovitch, 27/06/15
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
@SuppressWarnings({"deprecation", "unchecked"})
public class IntentDialogPreference extends DialogPreference implements AdapterView.OnItemClickListener, AppListTask.OnListCreatedListener {
    private final String mPackageToKill;
    private final boolean mIsSilent;
    private final boolean mIsRebootRequired;
    private final String mReverseDependencyKey;
    private boolean mIsSearch, mIsInitialSetup;
    private Context mContext;
    private ListView mListView;
    private ProgressBar mProgressBar;
    private AppListAdapter mAppListAdapter;
    private AppListTask mAppListTask;
    private String mSeparator;
    private PackageManager mPackageManager;

    public IntentDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
        mPackageManager = context.getPackageManager();
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.IntentDialogPreference);
        mSeparator = typedArray.getString(R.styleable.IntentDialogPreference_intentSeparator);
        mSeparator = mSeparator != null ? mSeparator : "##";
        mIsSearch = typedArray.getBoolean(R.styleable.IntentDialogPreference_showSearch, true);
        TypedArray generalTypedArray = context.obtainStyledAttributes(attrs, R.styleable.Preference);
        mIsRebootRequired = generalTypedArray.getBoolean(R.styleable.Preference_rebootDevice, false);
        mPackageToKill = generalTypedArray.getString(R.styleable.Preference_packageNameToKill);
        mIsSilent = generalTypedArray.getBoolean(R.styleable.Preference_isSilent, true);
        mReverseDependencyKey = generalTypedArray.getString(R.styleable.Preference_reverseDependency);

        typedArray.recycle();
        generalTypedArray.recycle();

        setDialogLayoutResource(R.layout.intent_dialog_layout);
        setWidgetLayoutResource(R.layout.intent_preference_app_icon);
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
    protected void onBindView(View view) {
        super.onBindView(view);
        String value = getPersistedString(null);
        ImageView prefAppIcon = (ImageView) view.findViewById(R.id.iconForApp);
        prefAppIcon.setImageDrawable(getAppIcon(value));
        setSummary(value != null && value.split(mSeparator).length == 2 ? getAppName(value) : "");
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        mListView = (ListView) view.findViewById(R.id.appsList);
        mListView.setOnItemClickListener(this);
        mListView.setFastScrollEnabled(true);
        mListView.setFadingEdgeLength(1);
        mListView.setDivider(null);
        mListView.setDividerHeight(0);
        mListView.setScrollingCacheEnabled(false);
        mProgressBar = (ProgressBar) view.findViewById(R.id.progressBar);
        if (mIsSearch) {
            EditText search = (EditText) view.findViewById(R.id.searchApp);
            search.setVisibility(View.VISIBLE);
            createList();
            search.addTextChangedListener(new TextWatcher() {

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (mAppListAdapter != null) {
                        mAppListAdapter.getFilter().filter(s);
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });
        } else {
            createList();
        }


    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (mAppListTask != null && mAppListTask.getStatus() == AsyncTask.Status.RUNNING) {
            mAppListTask.cancel(true);
            mAppListTask = null;
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getString(index);
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        mIsInitialSetup = true;
        String value = Settings.System.getString(getContext().getContentResolver(), getKey());
        if (value == null) {
            if (defaultValue != null && ((String) defaultValue).split(mSeparator).length == 2) {
                value = (String) defaultValue;
                Settings.System.putString(getContext().getContentResolver(), getKey(), value);
            }
        }
        if (value != null) {
            persistString(value);
        }
    }


    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);
        AlertDialog dialog = (AlertDialog) getDialog();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        dialog.show();
        Button ok = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        ok.setVisibility(View.GONE);
    }


    private String getAppName(String value) {
        String appName = null;
        if (value != null) {
            String[] split = value.split(mSeparator);
            String pkgName = split[0];
            String activity = split[1];
            Intent intent = new Intent();
            intent.setComponent(new ComponentName(pkgName, activity));
            ResolveInfo resolveInfo = mPackageManager.resolveActivity(intent, 0);
            if (resolveInfo != null) {
                appName = resolveInfo.activityInfo.loadLabel(mPackageManager).toString();
            }

        }

        return appName;
    }


    private Drawable getAppIcon(String intentString) {
        Drawable appIcon = mContext.getResources().getDrawable(R.mipmap.ic_launcher);
        if (intentString != null) {
            String[] splitValue = intentString.split(mSeparator);
            String pkg = splitValue[0];
            String activity = splitValue[1];
            Intent intent = new Intent();
            intent.setComponent(new ComponentName(pkg, activity));
            ResolveInfo resolveInfo = mPackageManager.resolveActivity(intent, 0);
            if (resolveInfo != null) {
                appIcon = resolveInfo.activityInfo.loadIcon(mPackageManager);
            }
        }
        return appIcon;

    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        AppInfo appInfo = (AppInfo) parent.getItemAtPosition(position);
        Intent intent = appInfo.mIntent;
        ResolveInfo ri = mPackageManager.resolveActivity(intent, 0);
        String intentString = String.format("%1$s%2$s%3$s", appInfo.mPackageName, mSeparator, ri.activityInfo.name);
        setSummary(intentString == null ? "" : appInfo.mAppName);
        persistString(intentString);
        getDialog().dismiss();

    }

    @Override
    protected boolean persistString(String value) {
        if (getKey() != null) {
            Settings.System.putString(getContext().getContentResolver(), getKey(), value);

            if (mIsRebootRequired && !mIsInitialSetup) {
                Utils.showRebootRequiredDialog(getContext());
            } else {
                if (mPackageToKill != null && !mIsInitialSetup) {
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
        return super.persistString(value);
    }

    private void createList() {
        AppListTask appListTask = new AppListTask();
        appListTask.setOnListCreatedListener(this);
        appListTask.execute();
        mProgressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void onListCreated(List<com.wubydax.romcontrol.v2.utils.AppInfo> appInfoList) {
        mAppListAdapter = new AppListAdapter(appInfoList);
        mProgressBar.setVisibility(View.GONE);
        mListView.setAdapter(mAppListAdapter);
    }

    private class AppListAdapter extends BaseAdapter implements SectionIndexer, Filterable {

        List<AppInfo> mAppList, filteredList;
        private HashMap<String, Integer> alphaIndexer;
        private String[] sections;

        AppListAdapter(List<AppInfo> appList) {

            this.mAppList = appList;
            filteredList = mAppList;
            //adding Indexer to display the first letter of an app while using fast scroll
            alphaIndexer = new HashMap<>();
            for (int i = 0; i < filteredList.size(); i++) {
                String s = filteredList.get(i).mAppName;
                String s1 = s.substring(0, 1).toUpperCase();
                if (!alphaIndexer.containsKey(s1))
                    alphaIndexer.put(s1, i);
            }

            Set<String> sectionLetters = alphaIndexer.keySet();
            ArrayList<String> sectionList = new ArrayList<>(sectionLetters);
            Collections.sort(sectionList);
            sections = new String[sectionList.size()];
            for (int i = 0; i < sectionList.size(); i++)
                sections[i] = sectionList.get(i);

        }

        @Override
        public Object[] getSections() {
            return sections;
        }

        @Override
        public int getPositionForSection(int sectionIndex) {
            return alphaIndexer.get(sections[sectionIndex]);
        }

        @Override
        public int getSectionForPosition(int position) {
            for (int i = sections.length - 1; i >= 0; i--) {
                if (position >= alphaIndexer.get(sections[i])) {
                    return i;
                }
            }
            return 0;
        }

        @Override
        public Filter getFilter() {
            return new Filter() {

                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults fr = new FilterResults();
                    ArrayList<AppInfo> ai = new ArrayList<>();

                    for (int i = 0; i < mAppList.size(); i++) {
                        String label = mAppList.get(i).mAppName;
                        if (label.toLowerCase().contains(constraint.toString().toLowerCase())) {
                            ai.add(mAppList.get(i));
                        }
                    }

                    fr.count = ai.size();
                    fr.values = ai;

                    return fr;
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    filteredList = (List<AppInfo>) results.values;
                    notifyDataSetChanged();
                }
            };
        }

        @Override
        public int getCount() {
            if (filteredList != null) {
                return filteredList.size();
            }
            return 0;
        }

        @Override
        public AppInfo getItem(int position) {
            if (filteredList != null) {
                return filteredList.get(position);
            }
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(mContext);
                convertView = inflater.inflate(R.layout.app_item, parent, false);
                ViewHolder viewHolder = new ViewHolder();
                viewHolder.mAppNames = (TextView) convertView.findViewById(R.id.appName);
                viewHolder.mAppPackage = (TextView) convertView.findViewById(R.id.appPackage);
                viewHolder.mAppIcon = (ImageView) convertView.findViewById(R.id.appIcon);
                convertView.setTag(viewHolder);
            }
            final ViewHolder holder = (ViewHolder) convertView.getTag();
            final AppInfo appInfo = filteredList.get(position);

            holder.mAppNames.setText(appInfo.mAppName);
            holder.mAppPackage.setText(appInfo.mPackageName);
            holder.mAppIcon.setImageDrawable(appInfo.mIcon);

            return convertView;
        }

        class ViewHolder {
            TextView mAppNames;
            TextView mAppPackage;
            ImageView mAppIcon;
        }
    }


}
