
package com.aokp.backup.backup;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.provider.Settings;
import android.util.Log;
import com.aokp.backup.R;
import com.aokp.backup.util.SVal;
import com.aokp.backup.util.Tools;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Kitkat extends Backup {

    public static final int CAT_STATUSBAR = 0;
    public static final int NUM_CATS = 1;
    public static final String NULL_CONSTANT = "**null**";

    static String rcUser;



    public Kitkat(Context c, String name) {
        super(c, name);
        mSuCommands = new ArrayList<String>();
    }

    public Kitkat(Context c, File zip) throws IOException {
        super(c, zip);
        mSuCommands = new ArrayList<String>();
    }

    @Override
    public int getNumCats() {
        return NUM_CATS;
    }

    @Override
    public List<String> getSuCommands() {
        return mSuCommands;
    }

    @Override
    public String[] getSettingsCategory(int categoryIndex) {
        Resources res = mContext.getResources();
        switch (categoryIndex) {
            case CAT_STATUSBAR:
                return res.getStringArray(R.array.kitkat_cat_statusbar);
            default:
                return null;
        }
    }

    @Override
    public boolean handleBackupSpecialCase(String setting) {
        String outDir = Tools.getTempBackupDirectory(mContext, false).getAbsolutePath();

        boolean found = false;
        if (setting.equals("navigation_bar_icons")) {
            ContentResolver resolver = mContext.getContentResolver();
            for (int i = 0; i < 7; i++) {
                String iconSetting = "navigation_custom_app_icon_" + i;
                String iconValue = Settings.System.getString(resolver, iconSetting);
                if (iconValue != null) {
                    if (iconValue.length() > 0 && !iconValue.equalsIgnoreCase(NULL_CONSTANT)) {
                        mBackupValues.add(new SVal(iconSetting, iconValue));
                        File icon = new File("/data/data/com.android.settings/files/navbar_icon_" + i + ".png");
                        if (icon.exists()) {
                            String cmd = "cp  " + icon.getAbsolutePath() + " " + outDir;
                            mSuCommands.add(cmd);
                            found = true;
                        }
                    }
                }
            }

        } else if (setting.equals("lockscreen_wallpaper")) {
            String path = "/data/data/com.aokp.romcontrol/files/lockscreen_wallpaper.jpg";
            if (new File(path).exists()) {
                mSuCommands.add("cp " + path + " " + outDir);
                mSuCommands.addAll(Tools.getChmodAndOwnCommand(new File(outDir, "lockscreen_wallpaper.jpg"), "0660", Tools.getMyPid(mContext)));
                found = true;
            }


        } else if (setting.equals("notification_wallpaper")) {
            File file = new File("/data/data/com.aokp.romcontrol/files/notification_wallpaper.jpg");
            if (!file.exists()) {
                file = new File("/data/data/com.aokp.romcontrol/files/notification_wallpaper.png");
            }
            if (file.exists()) {
                mSuCommands.add("cp " + file + " " + outDir);
                mSuCommands.addAll(Tools.getChmodAndOwnCommand(new File(outDir, file.getName()), "0660", Tools.getMyPid(mContext)));

                found = true;
            }
        } else if (setting.equals("lockscreen_icons")) {
            ContentResolver resolver = mContext.getContentResolver();
            for (int i = 0; i < 8; i++) {
                String set = "lockscreen_custom_app_icon_" + i;
                String val = Settings.System.getString(resolver, set);
                if (val != null && !val.equalsIgnoreCase(NULL_CONSTANT)) {
                    mBackupValues.add(new SVal(set, val));
                    File f = new File("/data/data/com.aokp.romcontrol/files/lockscreen_icon_" + i + ".png");

                    if (f.exists()) {
                        found = true;
                        mSuCommands.add("cp " + f.getAbsoluteFile() + " " + outDir);
                    }
                }
            }
        }

        if (found) {
            mSpecialCaseKeys.add(new SVal(setting, "1"));
        }

        return found;

    }

    public boolean okayToRestore() {
        boolean result = false;

        if (Tools.getROMVersion().contains("aicp")) {
            result = true;
        }

        return result;
    }

    public boolean handleRestoreSpecialCase(SVal sval) {
        if (rcUser == null) {
            rcUser = Tools.getRomControlPid();
        }

        String setting = sval.getKey();
        String value = sval.getValue();

        if (setting.equals("disable_boot_animation") && value.equals("1")) {
            if (new File("/system/media/bootanimation.zip").exists()) {
                mSuCommands.add("mv /system/media/bootanimation.zip /system/media/bootanimation.unicorn");
            }
            return true;
        } else if (setting.equals("disable_boot_audio") && value.equals("1")) {
            if (new File("/system/media/boot_audio.mp3").exists()) {
                mSuCommands.add("mv /system/media/boot_audio.mp3 /system/media/boot_audio.unicorn");
            }

            return true;
        } else if (setting.equals("navigation_bar_icons")) {
            File outDir = Tools.getTempRestoreDirectory(mContext, false);

            for (int i = 0; i < 7; i++) {
//                String settingName = "navigation_custom_app_icon_" + i;
                String iconName = "navbar_icon_" + i + ".png";
                File source = new File(outDir, iconName);
                File target = new File(rcFilesDir, iconName);

                // delete the current icon since we're restoring some
//                if (settingsFromFile.containsKey(settingName)) {
                mSuCommands.add("cp " + source.getAbsolutePath() + " " + target.getAbsolutePath());
                List<String> chmodCommands = Tools.getChmodAndOwnCommand(target, "0664", rcUser);
                for (String cmd : chmodCommands) {
                    mSuCommands.add(cmd);
                }
//                Tools.chmodAndOwn(target, "0660", rcUser);
//                    restoreSetting(settingsFromFile.get(settingName));
//                } else {
//                    Shell.SU.run("rm " + target.getAbsolutePath());
//                    restoreSetting(settingName, "", false);
//                }

            }
            return true;
        } else if (setting.equals("lockscreen_wallpaper")) {
            File outDir = Tools.getTempRestoreDirectory(mContext, false);

            File source = new File(outDir, "lockscreen_wallpaper.jpg");
            File target = new File(rcFilesDir, "lockscreen_wallpaper.jpg");
            mSuCommands.add("rm " + target.getAbsolutePath());
            mSuCommands.add("cp " + source.getAbsolutePath() + " "
                    + target.getAbsolutePath());
            List<String> chmodCommands = Tools.getChmodAndOwnCommand(target, "0664", rcUser);
            for (String cmd : chmodCommands) {
                mSuCommands.add(cmd);
            }
//            Tools.chmodAndOwn(target, "0660", rcUser);

            return true;
        } else if (setting.equals("notification_wallpaper")) {
            File outDir = Tools.getTempRestoreDirectory(mContext, false);

            File source = new File(outDir, "notification_wallpaper.jpg");
            File target = new File(rcFilesDir, "notification_wallpaper.jpg");
            mSuCommands.add("rm " + target.getAbsolutePath());
            mSuCommands.add("cp " + source.getAbsolutePath() + " "
                    + target.getAbsolutePath());
            List<String> chmodCommands = Tools.getChmodAndOwnCommand(target, "0664", rcUser);
            for (String cmd : chmodCommands) {
                mSuCommands.add(cmd);
            }
//            Tools.chmodAndOwn(target, "0660", rcUser);

            return true;
        }

        return false;
    }
}