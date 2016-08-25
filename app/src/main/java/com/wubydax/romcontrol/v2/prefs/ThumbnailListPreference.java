package com.wubydax.romcontrol.v2.prefs;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.preference.Preference;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;

import com.wubydax.romcontrol.v2.R;
import com.wubydax.romcontrol.v2.utils.Utils;

import java.util.Arrays;

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

@SuppressWarnings("unused")
public class ThumbnailListPreference extends DialogPreference implements AdapterView.OnItemClickListener, Preference.OnPreferenceChangeListener {
    private final String mPackageToKill;
    private final boolean mIsSilent;
    private final boolean mIsRebootRequired;
    private final String mReverseDependencyKey;
    private Context mContext;
    private Drawable[] mThumbnailsArray;
    private CharSequence[] mEntriesList, mEntryValuesList;
    private ImageView mIconView;
    private int mSelectedPosition;
    private String mDependentValue;


    public ThumbnailListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        TypedArray typedArray = mContext.obtainStyledAttributes(attrs, R.styleable.ThumbnailListPreference, 0, 0);
        mEntriesList = typedArray.getTextArray(R.styleable.ThumbnailListPreference_entryList);
        mEntryValuesList = typedArray.getTextArray(R.styleable.ThumbnailListPreference_entryValuesList);
        TypedArray generalTypedArray = context.obtainStyledAttributes(attrs, R.styleable.Preference);
        mIsRebootRequired = generalTypedArray.getBoolean(R.styleable.Preference_rebootDevice, false);
        mPackageToKill = generalTypedArray.getString(R.styleable.Preference_packageNameToKill);
        mIsSilent = generalTypedArray.getBoolean(R.styleable.Preference_isSilent, true);
        mDependentValue = generalTypedArray.getString(R.styleable.Preference_dependentValue);
        mReverseDependencyKey = generalTypedArray.getString(R.styleable.Preference_reverseDependency);
        int resId = typedArray.getResourceId(R.styleable.ThumbnailListPreference_drawableArray, 0);
        if (resId != 0) {
            TypedArray resourceArray = context.getResources().obtainTypedArray(resId);
            mThumbnailsArray = new Drawable[resourceArray.length()];
            for (int i = 0; i < resourceArray.length(); i++) {
                mThumbnailsArray[i] = resourceArray.getDrawable(i);
            }
            resourceArray.recycle();
        }
        typedArray.recycle();
        generalTypedArray.recycle();

        setDialogLayoutResource(R.layout.thumbnail_preference_dialog_view);
        setWidgetLayoutResource(R.layout.thumbnail_preference_icon);

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
        return a.getString(index);
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        String value = Settings.System.getString(mContext.getContentResolver(), getKey());
        if (value == null) {
            value = defaultValue != null ? (String) defaultValue : "1";
            Settings.System.putString(mContext.getContentResolver(), getKey(), value);
        }
        persistString(value);
    }



    @Override
    public boolean shouldDisableDependents() {
        return super.shouldDisableDependents() || getPersistedString(null) == null || getPersistedString(null).equals(mDependentValue);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        mSelectedPosition = Arrays.asList(mEntryValuesList).indexOf(getPersistedString("1"));
        if (mSelectedPosition == -1) {
            mSelectedPosition = 0;
        }
        mIconView = (ImageView) view.findViewById(R.id.thumbnailIcon);
        mIconView.setImageDrawable(mThumbnailsArray[mSelectedPosition]);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        ListView lv = (ListView) view.findViewById(R.id.thumbnailListView);
        lv.setOnItemClickListener(this);
        lv.setFastScrollEnabled(true);
        lv.setFadingEdgeLength(1);
        lv.setDivider(null);
        lv.setDividerHeight(0);
        lv.setScrollingCacheEnabled(false);
        ListAdapter adapter = new ListAdapter(mContext, mEntriesList, mEntryValuesList, mThumbnailsArray, mSelectedPosition);
        lv.setAdapter(adapter);
    }

    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);
        AlertDialog dialog = (AlertDialog) getDialog();
        dialog.show();
        Button ok = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        ok.setVisibility(View.GONE);
    }


    public CharSequence[] getEntries() {
        return mEntriesList;
    }

    public void setEntries(CharSequence[] entries) {
        mEntriesList = entries;
    }

    public CharSequence[] getEntryValues() {
        return mEntryValuesList;
    }

    public void setEntryValues(CharSequence[] entryValues) {
        mEntryValuesList = entryValues;
    }

    public Drawable[] getDrawableArray() {
        return mThumbnailsArray;
    }

    public void setDrawableArray(Drawable[] drawableArray) {
        mThumbnailsArray = drawableArray;
    }


    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        String oldValue = getPersistedString(null);
        String newValue = mEntryValuesList[i].toString();
        persistString(newValue);
        if(oldValue != null && !oldValue.equals(newValue)) {
            notifyDependencyChange(shouldDisableDependents());
        }
        Settings.System.putString(mContext.getContentResolver(), getKey(), newValue);
        mSelectedPosition = i;
        getDialog().dismiss();
        mIconView.setImageDrawable(mThumbnailsArray[i]);
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

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return false;
    }

    private static class ListAdapter extends BaseAdapter {
        Context c;
        CharSequence[] mEntries, mValues;
        Drawable[] mThumbnails;
        int mSelectedPosition;

        ListAdapter(Context context, CharSequence[] entries, CharSequence[] values, Drawable[] thumbnails, int selectedPosition) {
            c = context;
            this.mEntries = entries;
            this.mValues = values;
            this.mThumbnails = thumbnails;
            this.mSelectedPosition = selectedPosition;

        }

        @Override
        public int getCount() {
            if (mEntries != null) {
                return mEntries.length;
            } else {
                return 0;
            }
        }

        @Override
        public Object getItem(int i) {
            return null;
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder vh;

            if (view == null) {
                LayoutInflater li = (LayoutInflater) c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = li.inflate(R.layout.thumbnail_item_view, viewGroup, false);
                vh = new ViewHolder(view);
                view.setTag(vh);
            } else {
                vh = (ViewHolder) view.getTag();

            }
            vh.mTextView.setText(mEntries[i]);
            vh.mImageView.setImageDrawable(mThumbnails[i]);
            vh.mRadioButton.setChecked(i == mSelectedPosition);
            return view;
        }

        private class ViewHolder {
            RadioButton mRadioButton;
            TextView mTextView;
            ImageView mImageView;

            ViewHolder(View v) {
                mRadioButton = (RadioButton) v.findViewById(R.id.thumbnailRadioButton);
                mTextView = (TextView) v.findViewById(R.id.thumbnailText);
                mImageView = (ImageView) v.findViewById(R.id.thumbnailImage);
            }
        }
    }
}
