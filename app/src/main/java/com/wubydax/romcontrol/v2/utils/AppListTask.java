package com.wubydax.romcontrol.v2.utils;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.AsyncTask;

import com.wubydax.romcontrol.v2.MyApp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
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


public class AppListTask extends AsyncTask<Void, Void, List<AppInfo>> {
    private  OnListCreatedListener mOnListCreatedListener;
    @Override
    protected List<AppInfo> doInBackground(Void... voids) {
        PackageManager packageManager = MyApp.getContext().getPackageManager();
        ArrayList<AppInfo> appList = new ArrayList<>();
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> resolveInfoList = packageManager.queryIntentActivities(intent, 0);

        for (ResolveInfo resolveInfo : resolveInfoList) {
            AppInfo appInfo = new AppInfo();
            appInfo.mAppName = resolveInfo.activityInfo.loadLabel(packageManager).toString();
            appInfo.mIcon = resolveInfo.activityInfo.loadIcon(packageManager);
            appInfo.mPackageName = resolveInfo.activityInfo.packageName;
            Intent explicitIntent = new Intent();
            explicitIntent.setComponent(new ComponentName(appInfo.mPackageName, resolveInfo.activityInfo.name));
            appInfo.mIntent = explicitIntent;
            appList.add(appInfo);
        }
        Collections.sort(appList, new Comparator<AppInfo>() {

            @Override
            public int compare(AppInfo lhs, AppInfo rhs) {
                return String.CASE_INSENSITIVE_ORDER.compare(lhs.mAppName, rhs.mAppName);
            }

        });
        return appList;

    }

    @Override
    protected void onPostExecute(List<AppInfo> appInfo) {
        if(mOnListCreatedListener != null) {
            mOnListCreatedListener.onListCreated(appInfo);
        }
    }

    public void setOnListCreatedListener(OnListCreatedListener onListCreatedListener) {
        mOnListCreatedListener = onListCreatedListener;
    }

    public interface OnListCreatedListener {
        void onListCreated(List<AppInfo> appInfoList);
    }
}
