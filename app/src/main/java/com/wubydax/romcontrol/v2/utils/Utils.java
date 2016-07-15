package com.wubydax.romcontrol.v2.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.PowerManager;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.widget.Toast;

import com.stericson.RootShell.exceptions.RootDeniedException;
import com.stericson.RootShell.execution.Command;
import com.stericson.RootTools.RootTools;
import com.wubydax.romcontrol.v2.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.TimeoutException;

import static com.wubydax.romcontrol.v2.MyApp.getContext;

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

@SuppressWarnings({"ResultOfMethodCallIgnored", "unused"})
public class Utils {


    private static final String LOG_TAG = "RomControlUtils";


    static void copyAssetFolder() {


        try {
            String[] scriptsInAssets = getContext().getAssets().list(Constants.SCRIPTS_FOLDER);
            Log.d(LOG_TAG, "copyAssetFolder " + scriptsInAssets[0]);
            File scriptsFilesDir = new File(Constants.FILES_SCRIPTS_FOLDER_PATH);
            //Checking if the "scripts" directory exists in files
            if (!scriptsFilesDir.exists()) {
                new File(Constants.FILES_SCRIPTS_FOLDER_PATH).mkdirs();
            }
            for (String file : scriptsInAssets) {
                //If the file name contains  a dot, it's most probably a single file and not dir. So treating it as copying file
                Log.d(LOG_TAG, "copyAssetFolder " + file);
                if (file.contains(".")) {
                    copyAsset(scriptsInAssets, Constants.SCRIPTS_FOLDER + File.separator + file, Constants.FILES_SCRIPTS_FOLDER_PATH + File.separator + file);
                } else {
                    //Otherwise treating as copying dir
                    copyAssetFolder();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void copyAsset(String[] scriptsInAssets, String from, String to) {
        boolean isCopied = false;
        InputStream in;
        OutputStream out;
        ArrayList<File> scriptsFiles = new ArrayList<>();
        //Creating list of File objects inside assets
        for (String scriptsInAsset : scriptsInAssets) {
            File f = new File(Constants.FILES_SCRIPTS_FOLDER_PATH + File.separator + scriptsInAsset);
            scriptsFiles.add(f);
        }
        for (int j = 0; j < scriptsFiles.size(); j++) {
            //If the file doesn't exist in files dir, we copy it
            if (!scriptsFiles.get(j).exists()) {
                try {
                    in = getContext().getAssets().open(from);
                    new File(to).createNewFile();
                    out = new FileOutputStream(to);
                    copyFile(in, out);
                    in.close();
                    out.flush();
                    out.close();
                    isCopied = true;
                } catch (Exception e) {
                    e.printStackTrace();
                    isCopied = false;
                }
            }
        }
        //If the file was just copied, we make it executable
        if (isCopied) {

            try {
                Command c = new Command(0, "chmod -R 755 " + Constants.FILES_SCRIPTS_FOLDER_PATH);
                RootTools.getShell(false).add(c);

            } catch (IOException | RootDeniedException | TimeoutException e) {
                e.printStackTrace();
            }
        }

    }

    //Actual copying of the file
    private static void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }

    public static Drawable getIconDrawable(Uri uri) {
        Drawable drawable = null;
        if (uri != null) {
            try {
                InputStream inputStream = getContext().getContentResolver().openInputStream(uri);
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth() / 5, bitmap.getHeight() / 5, false);
                drawable = new BitmapDrawable(getContext().getResources(), scaledBitmap);
                bitmap.recycle();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        return drawable;
    }

    static Drawable getDrawable(View rootView) {
        rootView.setDrawingCacheEnabled(true);
        Bitmap bitmap = Bitmap.createBitmap(rootView.getDrawingCache());
        rootView.setDrawingCacheEnabled(false);
        Bitmap blurredBitmap = getBlurredImage(bitmap, 0.3f, 50);
        return new BitmapDrawable(getContext().getResources(), blurredBitmap);
    }

    private static float getDegreesForRotation(int rotation) {
        switch (rotation) {
            case Surface.ROTATION_90:
                return 270f;
            case Surface.ROTATION_180:
                return 180f;
            case Surface.ROTATION_270:
                return 90f;
        }
        return 0f;
    }

    /*Stack Blur Algorithm by Mario Klingemann <mario@quasimondo.com>*/
    private static Bitmap getBlurredImage(Bitmap originalBitmap, float scaleMeasure, int blurRadius) {

        int width = Math.round(originalBitmap.getWidth() * scaleMeasure);
        int height = Math.round(originalBitmap.getHeight() * scaleMeasure);
        originalBitmap = Bitmap.createScaledBitmap(originalBitmap, width, height, false);

        Bitmap newBitmap = originalBitmap.copy(originalBitmap.getConfig(), true);

        if (blurRadius < 1) {
            return (null);
        }

        int w = newBitmap.getWidth();
        int h = newBitmap.getHeight();

        int[] pix = new int[w * h];
        newBitmap.getPixels(pix, 0, w, 0, 0, w, h);

        int wm = w - 1;
        int hm = h - 1;
        int wh = w * h;
        int div = blurRadius + blurRadius + 1;

        int r[] = new int[wh];
        int g[] = new int[wh];
        int b[] = new int[wh];
        int rSum, gSum, bSum, x, y, i, p, yp, yi, yw;
        int vMin[] = new int[Math.max(w, h)];

        int divSum = (div + 1) >> 1;
        divSum *= divSum;
        int dv[] = new int[256 * divSum];
        for (i = 0; i < 256 * divSum; i++) {
            dv[i] = (i / divSum);
        }

        yw = yi = 0;

        int[][] stack = new int[div][3];
        int stackPointer;
        int stackStart;
        int[] sir;
        int rbs;
        int r1 = blurRadius + 1;
        int routSum, goutSum, boutSum;
        int rinSum, ginSum, binSum;

        for (y = 0; y < h; y++) {
            rinSum = ginSum = binSum = routSum = goutSum = boutSum = rSum = gSum = bSum = 0;
            for (i = -blurRadius; i <= blurRadius; i++) {
                p = pix[yi + Math.min(wm, Math.max(i, 0))];
                sir = stack[i + blurRadius];
                sir[0] = (p & 0xff0000) >> 16;
                sir[1] = (p & 0x00ff00) >> 8;
                sir[2] = (p & 0x0000ff);
                rbs = r1 - Math.abs(i);
                rSum += sir[0] * rbs;
                gSum += sir[1] * rbs;
                bSum += sir[2] * rbs;
                if (i > 0) {
                    rinSum += sir[0];
                    ginSum += sir[1];
                    binSum += sir[2];
                } else {
                    routSum += sir[0];
                    goutSum += sir[1];
                    boutSum += sir[2];
                }
            }
            stackPointer = blurRadius;

            for (x = 0; x < w; x++) {

                r[yi] = dv[rSum];
                g[yi] = dv[gSum];
                b[yi] = dv[bSum];

                rSum -= routSum;
                gSum -= goutSum;
                bSum -= boutSum;

                stackStart = stackPointer - blurRadius + div;
                sir = stack[stackStart % div];

                routSum -= sir[0];
                goutSum -= sir[1];
                boutSum -= sir[2];

                if (y == 0) {
                    vMin[x] = Math.min(x + blurRadius + 1, wm);
                }
                p = pix[yw + vMin[x]];

                sir[0] = (p & 0xff0000) >> 16;
                sir[1] = (p & 0x00ff00) >> 8;
                sir[2] = (p & 0x0000ff);

                rinSum += sir[0];
                ginSum += sir[1];
                binSum += sir[2];

                rSum += rinSum;
                gSum += ginSum;
                bSum += binSum;

                stackPointer = (stackPointer + 1) % div;
                sir = stack[(stackPointer) % div];

                routSum += sir[0];
                goutSum += sir[1];
                boutSum += sir[2];

                rinSum -= sir[0];
                ginSum -= sir[1];
                binSum -= sir[2];

                yi++;
            }
            yw += w;
        }
        for (x = 0; x < w; x++) {
            rinSum = ginSum = binSum = routSum = goutSum = boutSum = rSum = gSum = bSum = 0;
            yp = -blurRadius * w;
            for (i = -blurRadius; i <= blurRadius; i++) {
                yi = Math.max(0, yp) + x;

                sir = stack[i + blurRadius];

                sir[0] = r[yi];
                sir[1] = g[yi];
                sir[2] = b[yi];

                rbs = r1 - Math.abs(i);

                rSum += r[yi] * rbs;
                gSum += g[yi] * rbs;
                bSum += b[yi] * rbs;

                if (i > 0) {
                    rinSum += sir[0];
                    ginSum += sir[1];
                    binSum += sir[2];
                } else {
                    routSum += sir[0];
                    goutSum += sir[1];
                    boutSum += sir[2];
                }

                if (i < hm) {
                    yp += w;
                }
            }
            yi = x;
            stackPointer = blurRadius;
            for (y = 0; y < h; y++) {
                pix[yi] = (0xff000000 & pix[yi]) | (dv[rSum] << 16) | (dv[gSum] << 8) | dv[bSum];

                rSum -= routSum;
                gSum -= goutSum;
                bSum -= boutSum;

                stackStart = stackPointer - blurRadius + div;
                sir = stack[stackStart % div];

                routSum -= sir[0];
                goutSum -= sir[1];
                boutSum -= sir[2];

                if (x == 0) {
                    vMin[y] = Math.min(y + r1, hm) * w;
                }
                p = x + vMin[y];

                sir[0] = r[p];
                sir[1] = g[p];
                sir[2] = b[p];

                rinSum += sir[0];
                ginSum += sir[1];
                binSum += sir[2];

                rSum += rinSum;
                gSum += ginSum;
                bSum += binSum;

                stackPointer = (stackPointer + 1) % div;
                sir = stack[stackPointer];

                routSum += sir[0];
                goutSum += sir[1];
                boutSum += sir[2];

                rinSum -= sir[0];
                ginSum -= sir[1];
                binSum -= sir[2];

                yi += w;
            }
        }

        newBitmap.setPixels(pix, 0, w, 0, 0, w, h);

        return (newBitmap);
    }

    public static void showKillPackageDialog(final String packageName, Context context) {
        try {
            ApplicationInfo applicationInfo = getContext().getPackageManager().getApplicationInfo(packageName, 0);
            String appLabel = applicationInfo.loadLabel(getContext().getPackageManager()).toString();
            Drawable appIcon = applicationInfo.loadIcon(getContext().getPackageManager());
            new AlertDialog.Builder(context)
                    .setTitle(R.string.app_reboot_required_dialog_title)
                    .setMessage(String.format(Locale.getDefault(), getContext().getString(R.string.app_reboot_required_dialog_message), appLabel))
                    .setIcon(appIcon)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            killPackage(packageName);
                        }
                    })
                    .create().show();
        } catch (PackageManager.NameNotFoundException e) {
            Toast.makeText(getContext(), "App not installed", Toast.LENGTH_SHORT).show();
        }

    }

    public static void showRebootRequiredDialog(final Context context) {
        new AlertDialog.Builder(context)
                .setTitle(R.string.reboot_required_dialog_title)
                .setMessage(R.string.reboot_required_dialog_message)
                .setNegativeButton(R.string.reboot_later_negative_button, null)
                .setPositiveButton(R.string.reboot_now_dialog_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                        powerManager.reboot(null);
                    }
                })
                .create()
                .show();
    }


    public static boolean isPackageInstalled(String packageName) {
        try {
            getContext().getPackageManager().getPackageInfo(packageName, PackageManager.GET_META_DATA);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static void killPackage(String packageNameToKill) {
        Command command = new Command(0, "pkill " + packageNameToKill);
        try {
            RootTools.getShell(true).add(command);
        } catch (IOException | TimeoutException | RootDeniedException e) {
            e.printStackTrace();
        }
    }
}
