package com.wubydax.romcontrol.v2.utils;

import android.os.AsyncTask;

import com.stericson.RootTools.RootTools;

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
public class SuTask extends AsyncTask<Void, Void, Boolean> {
    private OnSuCompletedListener mOnSuCompletedListener;

    @Override
    protected Boolean doInBackground(Void... params) {
        Utils.copyAssetFolder();
        return RootTools.isAccessGiven();
    }

    @Override
    protected void onPostExecute(Boolean isGranted) {
        if (mOnSuCompletedListener != null) {

                mOnSuCompletedListener.onTaskCompleted(isGranted);

        }
    }

    public void setOnSuCompletedListener(OnSuCompletedListener listener) {
        mOnSuCompletedListener = listener;
    }

    public interface OnSuCompletedListener {
        void onTaskCompleted(boolean isGranted);
    }
}
