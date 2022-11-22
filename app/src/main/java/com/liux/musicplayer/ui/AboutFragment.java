package com.liux.musicplayer.ui;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.blankj.utilcode.util.FileUtils;
import com.liux.musicplayer.BuildConfig;
import com.liux.musicplayer.R;
import com.liux.musicplayer.activities.MainActivity;
import com.liux.musicplayer.utils.CleanDataUtils;
import com.liux.musicplayer.utils.CrashHandlers;
import com.liux.musicplayer.utils.CustomDialogUtils;
import com.liux.musicplayer.utils.SharedPrefs;
import com.liux.musicplayer.utils.UpdateUtils;
import com.liux.musicplayer.utils.User;

public class AboutFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener{
    private Preference officialWebsite;
    private Preference lastErrorLog;
    private CheckBoxPreference showDebug;
    private Preference supportMe;
    private Preference exit;
    private Preference clearCache;
    private Preference checkUpdate;
    private SharedPreferences prefs;
    private Preference newsBoard;

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.about_preferences, rootKey);
        prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        prefs.registerOnSharedPreferenceChangeListener(this); // 注册
        /**LastErrorLog*/
        lastErrorLog=findPreference("lastErrorLog");
        if(!prefs.getString("lastErrorLog", "null").equals("null")&&
                FileUtils.isFileExists(requireContext().getExternalCacheDir() + "/log/" + prefs.getString("lastErrorLog", "null"))) {
            lastErrorLog.setSummary(prefs.getString("lastErrorLog", "null"));
        }else {
            lastErrorLog.setVisible(false);
        }
        lastErrorLog.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(@NonNull Preference preference) {
                if (lastErrorLog.getSummary() != null && !lastErrorLog.getSummary().equals("null")) {
                    if (FileUtils.isFileExists(requireContext().getExternalCacheDir() + "/log/" + lastErrorLog.getSummary()))
                        CrashHandlers.checkIfExistsLastCrash(getActivity(),true);
                    else {
                        Toast.makeText(requireContext(), "日志文件不存在", Toast.LENGTH_SHORT).show();
                        lastErrorLog.setSummary("null");
                        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(requireContext()).edit();
                        editor.putString("lastErrorLog", "null");
                        editor.apply();
                    }
                }
                return false;
            }
        });
        /**ShowDebug*/
        showDebug=findPreference("showDebug");
        if(User.isLogin&&User.userData.level<=1)
            showDebug.setVisible(true);
        else
            showDebug.setVisible(false);
        /**Official Website*/
        officialWebsite=findPreference("officialWebsite");
        officialWebsite.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(@NonNull Preference preference) {
                CustomDialogUtils.openUrl(getContext(),"https://brownlzy.github.io/MyOtaInfo/MusicPlayer/manual.html");
                return false;
            }
        });
        /**Support Me*/
        supportMe=findPreference("supportMe");
        supportMe.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(@NonNull Preference preference) {
                CustomDialogUtils.openUrl(getContext(),"https://brownlzy.github.io/MyOtaInfo/MusicPlayer/pic/wx.png");
                return false;
            }
        });
        /**Exit*/
        exit=findPreference("exit");
        exit.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(@NonNull Preference preference) {
                AlertDialog alertDialog=new AlertDialog.Builder(getContext())
                        .setIcon(R.drawable.ic_round_exit_to_app_24)
                        .setTitle(R.string.exitApp)
                        .setMessage(R.string.sure_to_exit)
                        .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent(getActivity(), MainActivity.class);
                                intent.putExtra("exit", true);
                                startActivity(intent);
                            }
                        })
                        .setNeutralButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .create();
                alertDialog.show();
                return false;
            }
        });
        /**Clear Cache*/
        clearCache=findPreference("clearCache");
        clearCache.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(@NonNull Preference preference) {
                CleanDataUtils.clearAllCache(requireContext());
                String clearSize = CleanDataUtils.getTotalCacheSize(requireContext());
                clearCache.setSummary(clearSize);
                lastErrorLog.setVisible(false);
                return true;
            }
        });
        /**Check Update*/
        checkUpdate=findPreference("checkUpdate");
        if(SharedPrefs.getLastVersionCode()> BuildConfig.VERSION_CODE){
            checkUpdate.setSummary(getString(R.string.findNewVersion)+
                    SharedPrefs.getLastVersionName()+" ("+SharedPrefs.getLastVersionCode()+")");
        }
        checkUpdate.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(@NonNull Preference preference) {
                UpdateUtils.checkUpdate(getContext(),true);
                return false;
            }
        });
        /**News Board*/
        newsBoard=findPreference("newsBoard");
        newsBoard.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(@NonNull Preference preference) {
                UpdateUtils.checkNews(getContext(),true);
                return false;
            }
        });

    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key){
            case "lastVersionCode":
//            case "lastVersionName":
                if(SharedPrefs.getLastVersionCode()> BuildConfig.VERSION_CODE){
                    checkUpdate.setSummary(getString(R.string.findNewVersion)+
                            SharedPrefs.getLastVersionName()+" ("+SharedPrefs.getLastVersionCode()+")");
                }else {
                    checkUpdate.setSummary(null);
                }
                break;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        String totalCacheSize = CleanDataUtils.getTotalCacheSize(requireContext());
        clearCache.setSummary(totalCacheSize);
    }
}
