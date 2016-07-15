package com.wubydax.romcontrol.v2.utils;

import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import com.wubydax.romcontrol.v2.MainActivity;
import com.wubydax.romcontrol.v2.MyApp;
import com.wubydax.romcontrol.v2.R;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;
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

public class BackupRestoreIntentService extends IntentService {


    private static final String LOG_TAG = BackupRestoreIntentService.class.getSimpleName();


    public BackupRestoreIntentService() {
        super("RomControlWorkingThread");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(LocalReceiver.BACKUP_COMPLETE_ACTION);
        intentFilter.addAction(LocalReceiver.RESTORE_COMPLETE_ACTION);
        registerReceiver(new LocalReceiver(), intentFilter);
        if (intent != null) {
            switch (intent.getAction()) {
                case Constants.SERVICE_INTENT_ACTION_BACKUP:
                    handleMainFolder();
                    iteratePrefsAndBackup();
                    break;
                case Constants.SERVICE_INTENT_ACTION_RESTORE:
                    String filePath = intent.getStringExtra(Constants.BACKUP_FILE_PATH_EXTRA_KEY);
                    restoreValues(filePath);
                    break;
            }
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void restoreValues(String filePath) {
        File filesFolder = new File(getFilesDir().getAbsolutePath());
        File[] filesInData = filesFolder.listFiles();
        if (filesInData.length > 0) {
            for (File file : filesInData) {
                String fileName = file.getName();
                if (!fileName.equals("scripts") && !fileName.contains("rList")) {
                    file.delete();
                }
            }
        }
        FileInputStream fileInputStream;
        BufferedReader bufferedReader;
        File backUpFile = new File(filePath);
        try {
            fileInputStream = new FileInputStream(backUpFile);
            bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (!line.equals("")) {
                    if (line.contains("###")) {
                        String[] data = line.split("###");
                        Settings.System.putString(getContentResolver(), data[0], data[1]);
                    } else {
                        File file = new File(getFilesDir() + File.separator + line);
                        try {
                            file.createNewFile();
                            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(file), 16 * 1024);
                            bufferedOutputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                }
                Log.d(LOG_TAG, "restoreValues " + line);
            }
            fileInputStream.close();
            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            sendBroadcast(new Intent(LocalReceiver.RESTORE_COMPLETE_ACTION));
        }

    }

    private void iteratePrefsAndBackup() {
        File prefsFolder = new File(Constants.SHARED_PREFS_FOLDER_PATH);
        File[] allPrefFilesList = prefsFolder.listFiles();
        StringBuilder stringBuilder = new StringBuilder();
        for (File file : allPrefFilesList) {
            if (!file.getName().contains(getPackageName() + "_preferences")) {
                String prefName = file.getName().substring(0, file.getName().length() - 4);
                SharedPreferences sharedPreferences = getSharedPreferences(prefName, MODE_PRIVATE);
                Map<String, ?> prefMap = sharedPreferences.getAll();
                for (Map.Entry<String, ?> singlePref : prefMap.entrySet()) {
                    String key = singlePref.getKey();
                    String dbValue = Settings.System.getString(getContentResolver(), key);
                    if (dbValue != null) {
                        stringBuilder.append(key).append("###").append(dbValue).append("\n");
                    }
                }
            }
        }
        File filesFolder = new File(getFilesDir().getAbsolutePath());
        File[] filesInData = filesFolder.listFiles();
        if (filesInData.length > 0) {
            for (File file : filesInData) {
                String fileName = file.getName();
                if (!fileName.equals("scripts") && !fileName.contains("rList")) {
                    stringBuilder.append(fileName).append("\n");
                }
            }
        }
        makeBackup(stringBuilder.toString());
    }

    private void makeBackup(String backupString) {
        String currentDate = new SimpleDateFormat(getString(R.string.backup_file_prefix_date_format), Locale.ENGLISH).format(Calendar.getInstance().getTime());
        String fileName = currentDate + "_" + Build.DISPLAY;
        String backupFileName = fileName + getString(R.string.backup_file_suffix);
        File newBackupFile = new File(Constants.BACKUP_FOLDER_PATH + File.separator + backupFileName);

        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(newBackupFile));
            bufferedWriter.write(backupString);
            bufferedWriter.close();
            Intent intent = new Intent(LocalReceiver.BACKUP_COMPLETE_ACTION);
            intent.putExtra(Constants.BACKUP_FILE_PATH_EXTRA_KEY, String.format(Locale.getDefault(), getString(R.string.backup_complete_toast), newBackupFile.getAbsoluteFile()));
            sendBroadcast(intent);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void handleMainFolder() {
        File mainFolder = new File(Constants.BACKUP_FOLDER_PATH);
        if (!mainFolder.exists()) {
            if (mainFolder.mkdirs()) {
                Log.d(LOG_TAG, "handleMainFolder main folder created successfully");
            } else {
                Log.d(LOG_TAG, "handleMainFolder problem creating main folder");
            }
        } else {
            Log.d(LOG_TAG, "handleMainFolder main folder exists");
        }
    }

    private class UiThreadRunnable implements Runnable {
        private String mMessage;

        UiThreadRunnable(String message) {
            mMessage = message;
        }

        @Override
        public void run() {
            Toast.makeText(MyApp.getContext(), mMessage, Toast.LENGTH_SHORT).show();
        }
    }

    private class LocalReceiver extends BroadcastReceiver {
        static final String BACKUP_COMPLETE_ACTION = "com.wubydax.action.BACKUP_COMPLETE";
        static final String RESTORE_COMPLETE_ACTION = "com.wubydax.action.RESTORE_COMPLETE";

        @Override
        public void onReceive(Context context, Intent intent) {
            Handler handler = new Handler();
            switch (intent.getAction()) {
                case BACKUP_COMPLETE_ACTION:
                    String filePath = intent.getStringExtra(Constants.BACKUP_FILE_PATH_EXTRA_KEY);
                    handler.post(new UiThreadRunnable(filePath));
                    break;
                case RESTORE_COMPLETE_ACTION:
                    String complete = getString(R.string.restore_complete_toast);
                    handler.post(new UiThreadRunnable(complete));
                    Intent startMainActivity = new Intent(context, MainActivity.class);
                    startMainActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(startMainActivity);
                    break;
            }
            unregisterReceiver(this);
        }
    }


}
