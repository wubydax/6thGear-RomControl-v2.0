package com.wubydax.romcontrol.v2.prefs;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.AttributeSet;

import com.wubydax.romcontrol.v2.utils.Utils;

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
public class UriSelectionPreference extends Preference {

    private OnUriSelectionRequestedListener mOnUriSelectionRequestedListener;
    private ContentResolver mContentResolver;


    public UriSelectionPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContentResolver = context.getContentResolver();
    }

    public OnUriSelectionRequestedListener getOnUriSelectionRequestedListener() {
        return mOnUriSelectionRequestedListener;
    }

    public void setOnUriSelectionRequestedListener(OnUriSelectionRequestedListener onUriSelectionRequestedListener) {
        mOnUriSelectionRequestedListener = onUriSelectionRequestedListener;
    }

    @Override
    protected void onAttachedToHierarchy(PreferenceManager preferenceManager) {
        super.onAttachedToHierarchy(preferenceManager);
        String uriString = Settings.System.getString(mContentResolver, getKey());
        if (uriString != null) {
            persistString(uriString);
            attemptToSetIcon(uriString);
        }
    }


    public void attemptToSetIcon(String uriString) {
        Uri uri = Uri.parse(uriString);
        if (uri != null) {
            SetImage setImage = new SetImage();
            setImage.execute(uri);
        }
    }

    @Override
    protected void onClick() {
        if (mOnUriSelectionRequestedListener != null) {
            mOnUriSelectionRequestedListener.onUriSelectionRequested(getKey());
        }

    }

    public interface OnUriSelectionRequestedListener {
        void onUriSelectionRequested(String key);
    }

    private class SetImage extends AsyncTask<Uri, Void, Drawable> {


        @Override
        protected Drawable doInBackground(Uri... params) {
            return Utils.getIconDrawable(params[0]);
        }

        @Override
        protected void onPostExecute(Drawable drawable) {
            setIcon(drawable);
        }


    }

}
