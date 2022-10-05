package com.liux.musicplayer.ui.settings;

import static com.liux.musicplayer.utils.UriTransform.getPath;

import android.Manifest;
import android.app.Activity;
import android.app.AppOpsManager;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.preference.CheckBoxPreference;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SeekBarPreference;
import androidx.preference.SwitchPreference;

import com.blankj.utilcode.util.PathUtils;
import com.blankj.utilcode.util.RegexUtils;
import com.blankj.utilcode.util.TimeUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.liux.musicplayer.R;
import com.liux.musicplayer.ui.MainActivity;
import com.liux.musicplayer.utils.MusicUtils;
import com.liux.musicplayer.utils.UploadDownloadUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {
    //Preference控件
    private CheckBoxPreference switch_storage_permission;
    private CheckBoxPreference switch_layer_permission;
    private CheckBoxPreference switch_web_playlist;
    private CheckBoxPreference switch_desk_lyric;
    private CheckBoxPreference switch_desk_lyric_lock;
    private SwitchPreference switch_new_appearance;
    private Preference setMainFolder;
    private Preference clickGotoAppDetails;
    private EditTextPreference dPlayList;
    private EditTextPreference webPlayList;
    private EditTextPreference MainFolder;
    private EditTextPreference cacheList;
    private Preference About;
    private Preference Close;
    private SeekBarPreference seekBarTiming;

    private final static String TAG = "SettingFragment";
    //注册Activity回调
    ActivityResultLauncher<Intent> gotoAppInfo = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            int resultCode = result.getResultCode();
            if (resultCode == -1) {
                if (checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE))
                    switch_storage_permission.setChecked(true);
                if (checkFloatPermission(getContext())) switch_layer_permission.setChecked(true);
            }
        }
    });
    //注册Activity回调，用于处理权限申请
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Toast.makeText(getActivity(), R.string.permission_granted, Toast.LENGTH_LONG).show();
                    if (checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE))
                        switch_storage_permission.setChecked(true);
                } else {
                    //准备前往信息页的Intent
                    Intent intent = new Intent("/");
                    ComponentName cm = new ComponentName("com.android.settings", "com.android.settings.applications.InstalledAppDetails");
                    intent.setComponent(cm);
                    intent.setAction("android.intent.action.VIEW");
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.setData(Uri.parse("package:com.liux.musicplayer"));
                    //调用
                    gotoAppInfo.launch(intent);
                }
            });
    private final ActivityResultLauncher<Intent> requestOverlayPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), isGranted -> {
                if (checkFloatPermission(getContext())) {
                    Toast.makeText(getActivity(), R.string.permission_granted, Toast.LENGTH_LONG).show();
                    switch_layer_permission.setChecked(true);
                } else {
                    Toast.makeText(getActivity(), R.string.permission_not_granted, Toast.LENGTH_LONG).show();
                }
            });
    //用于接受系统文件管理器返回目录的回调
    ActivityResultLauncher<Intent> getFolderIntent = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            int resultCode = result.getResultCode();
            if (resultCode == -1) {
                Intent data = result.getData();
                assert data != null;
                Uri uri = data.getData();
                setNewMainFolder(uri);
            }
        }

        //根据接收的选择结果保存设置并显示
        private void setNewMainFolder(Uri uri) {
            Toast.makeText(getActivity(), uri.toString(), Toast.LENGTH_LONG).show();
            Uri docUri = DocumentsContract.buildDocumentUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri));
            //TODO：选择特殊文件夹时崩溃
            String path = getPath(getContext(), docUri);
            path = path.replace("/storage/emulated/0", "/sdcard");
            //获取SharedPreferences对象
            SharedPreferences sp = getContext().getSharedPreferences("com.liux.musicplayer_preferences", Activity.MODE_PRIVATE);
            //获取Editor对象
            SharedPreferences.Editor editor = sp.edit();
            editor.putString("mainFolder", path);
            //editor.putString("mainFolder", uri.toString());
            editor.apply();
            MainFolder.setSummary(sp.getString("mainFolder", "/storage/emulated/0/Android/data/com.liux.musicplayer/Music/"));
            setMainFolder.setSummary(MainFolder.getSummary());
            //Toast.makeText(getActivity(), sp.getString("mainFolder","---"), Toast.LENGTH_LONG).show();
        }
    });

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Context context = getActivity();
        assert context != null;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        setPreferencesFromResource(R.xml.root_preferences, rootKey);
        findPreference("debug").setVisible(false);
        //绑定控件
        switch_new_appearance = findPreference("isNewAppearance");
        switch_storage_permission = findPreference("storage_permission");
        switch_layer_permission = findPreference("layer_permission");
        switch_web_playlist = findPreference("isUseWebPlayList");
        switch_desk_lyric = findPreference("isShowLyric");
        switch_desk_lyric_lock = findPreference("deskLyricLock");
        MainFolder = findPreference("mainFolder");
        cacheList = findPreference("cacheList");
        clickGotoAppDetails = findPreference("gotoAppDetails");
        dPlayList = findPreference("playList");
        setMainFolder = findPreference("setMainFolder");
        webPlayList = findPreference("webPlayList");
        About = findPreference("info");
        Close = findPreference("exit");
        seekBarTiming = findPreference("timing");
        prefs.registerOnSharedPreferenceChangeListener(this); // 注册
        if (checkFloatPermission(getContext()))
            switch_layer_permission.setChecked(true);
        if (checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE))
            switch_storage_permission.setChecked(true);
        //选择主文件目录
        MainFolder.setSummary(prefs.getString("mainFolder", "/storage/emulated/0/Android/data/com.liux.musicplayer/Music/"));
        //MainFolder.setSummaryProvider(EditTextPreference.SimpleSummaryProvider.getInstance());
        dPlayList.setSummaryProvider(EditTextPreference.SimpleSummaryProvider.getInstance());
        setMainFolder.setSummary(MainFolder.getSummary());
        initPreferenceListener();
        try {
            if (!((MainActivity) getActivity()).getMusicService().isTiming())
                seekBarTiming.setValue(0);
        } catch (Exception ignored) {
        }
    }

    private void initPreferenceListener() {
        switch_new_appearance.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                ((MainActivity) getActivity()).setNewAppearance((boolean) newValue);
                return true;
            }
        });
        switch_desk_lyric.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                ((MainActivity) getActivity()).getMusicService().setDesktopLyric((boolean) newValue);
                return true;
            }
        });
        Close.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(@NonNull Preference preference) {
                ((MainActivity) requireActivity()).getMusicService().stopForeground(true);
                System.exit(0);
                return false;
            }
        });
        clickGotoAppDetails.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(@NonNull Preference preference) {
                Intent intent = new Intent("/");
                ComponentName cm = new ComponentName("com.android.settings", "com.android.settings.applications.InstalledAppDetails");
                intent.setComponent(cm);
                intent.setAction("android.intent.action.VIEW");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setData(Uri.parse("package:com.liux.musicplayer"));
                //调用
                gotoAppInfo.launch(intent);
                return true;
            }
        });
        //设置权限开关的监听
        switch_storage_permission.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                if (newValue == Boolean.TRUE) {
                    return askPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
                } else {
                    return !checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
                }
            }
        });
        switch_layer_permission.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                if (newValue == Boolean.TRUE) {
                    return requestSettingCanDrawOverlays();
                } else {
                    return !checkFloatPermission(getContext());
                }
            }
        });
        switch_web_playlist.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                if ((boolean) newValue) {
                    SharedPreferences sp = getContext().getSharedPreferences("com.liux.musicplayer_preferences", Activity.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sp.edit();
                    String url = sp.getString("WebPlayListUrl", "NULL");
                    if (!RegexUtils.isURL(url)) {
                        Toast.makeText(getContext(), "请先设置正确的URL", Toast.LENGTH_SHORT).show();
                        return false;
                    } else {
                        OkHttpClient client = new OkHttpClient();
                        Request request = new Request.Builder()
                                .url(url)
                                .get()//default
                                .build();
                        Call call = client.newCall(request);
                        call.enqueue(new Callback() {
                            @Override
                            public void onFailure(Call call, IOException e) {
                                Log.d(TAG, "onFailure: ");
                                requireActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(requireActivity(), "获取播放列表失败", Toast.LENGTH_SHORT).show();
                                        switch_web_playlist.setChecked(false);
                                    }
                                });
                            }

                            @Override
                            public void onResponse(Call call, Response response) throws IOException {
                                String result = response.body().string();
                                int id = response.code();
                                Log.d(TAG, "onResponse: " + result);
                                requireActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (id == 200) {
                                            httpResultHandle(result, 0);
                                        } else {
                                            Toast.makeText(requireActivity(), "获取播放列表失败:" + id, Toast.LENGTH_SHORT).show();
                                            switch_web_playlist.setChecked(false);
                                        }
                                    }
                                });
                            }
                        });
                        return true;
                    }
                } else {
                    ((MainActivity) getActivity()).getMusicService().setWebPlayMode(false);
                    return true;
                }
            }
        });
        //监听权限开关按钮
        setMainFolder.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(@NonNull Preference preference) {
                //系统调用Action属性
                try {
                    getFolderIntent.launch(new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE));
                } catch (Exception e) {
                    Toast.makeText(getActivity(), "没有正确打开文件管理器", Toast.LENGTH_SHORT).show();
                }
                return false;
            }
        });
        About.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(@NonNull Preference preference) {
                popInfo();
                return false;
            }
        });
        seekBarTiming.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                if ((int) newValue > 0) {
                    ((MainActivity) getActivity()).getMusicService().startTiming();
                } else {
                    ((MainActivity) getActivity()).getMusicService().stopTiming();
                }
                return true;
            }
        });
    }

    private void popInfo() {
        String versionName = "";
        try {
            versionName = requireActivity().getPackageManager().getPackageInfo(requireActivity().getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        AlertDialog alertInfoDialog = null;
        alertInfoDialog = new AlertDialog.Builder(getContext())
                .setTitle(R.string.app_name)
                .setMessage(getString(R.string.appInfo).replace("\\n", "\n")
                        + versionName)
                .setIcon(R.mipmap.ic_launcher)
                .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .setNeutralButton(R.string.title_debug, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        findPreference("debug").setVisible(true);
                    }
                })
                .setNegativeButton(R.string.title_checkUpdate, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        checkUpdate();
                    }
                })
                .create();
        alertInfoDialog.show();
    }

    private void checkUpdate() {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("https://brownlzy.github.io/MyOtaInfo/MusicPlayer/updateinfo.json")
                .get()//default
                .build();
        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.d(TAG, "onFailure: ");
                requireActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(requireActivity(), "获取更新信息失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String result = response.body().string();
                int id = response.code();
                Log.d(TAG, "onResponse: " + result);
                requireActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (id == 200) {
                            updateHandle(result);
                        } else {
                            Toast.makeText(requireActivity(), "获取更新信息失败:" + id, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }

        });
    }

    static class UpdateInfo {
        String lastVersionName;
        int lastVersionCode;
        String filename;
        String size;
        String changLog;
    }

    private void updateHandle(String result) {
        int versionCode = 0;
        try {
            versionCode = requireActivity().getPackageManager().getPackageInfo(requireActivity().getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        Gson gson = new Gson();
        UpdateInfo updateInfo = gson.fromJson(result, UpdateInfo.class);

        if (updateInfo == null || versionCode == 0)
            Toast.makeText(requireActivity(), "获取更新信息失败:" + versionCode, Toast.LENGTH_SHORT).show();
        else if (updateInfo.lastVersionCode > versionCode) {
            AlertDialog alertInfoDialog = null;
            alertInfoDialog = new AlertDialog.Builder(getContext())
                    .setTitle(R.string.title_update)
                    .setMessage(getString(R.string.title_lastVersion) + updateInfo.lastVersionName + "\n"
                            + getString(R.string.title_size) + updateInfo.size + "\n"
                            + getString(R.string.title_changlog) + "\n"
                            + updateInfo.changLog.replace("\\n", "\n"))
                    .setIcon(R.mipmap.ic_launcher)
                    .setPositiveButton(R.string.update, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Toast.makeText(requireActivity(), "安装包下载中", Toast.LENGTH_SHORT).show();
                            acquireDownload(requireActivity(), "https://brownlzy.github.io/MyOtaInfo/MusicPlayer/" + updateInfo.filename);
                        }
                    })
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
                    .create();
            alertInfoDialog.show();
        } else {
            Toast.makeText(requireActivity(), "当前已是最新版", Toast.LENGTH_SHORT).show();
        }
    }

    //检查权限是否获取成功
    public boolean checkPermission(String permission) {
        //拒绝修改开关
        return ContextCompat.checkSelfPermission(getContext(), permission) == PackageManager.PERMISSION_GRANTED;
    }

    //判断是否开启悬浮窗权限   context可以用你的Activity.或者this
    public static boolean checkFloatPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AppOpsManager appOpsMgr = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
            if (appOpsMgr == null)
                return false;
            int mode = appOpsMgr.checkOpNoThrow("android:system_alert_window", android.os.Process.myUid(), context
                    .getPackageName());
            return mode == AppOpsManager.MODE_ALLOWED || mode == AppOpsManager.MODE_IGNORED;
        } else {
            return Settings.canDrawOverlays(context);
        }

    }

    //请求权限
    public boolean askPermission(String permission) {
        if (ContextCompat.checkSelfPermission(getContext(), permission) == PackageManager.PERMISSION_GRANTED) {
            //Toast.makeText(getActivity(), R.string.permission_granted, Toast.LENGTH_LONG).show();
            return true;
        } else {
            requestPermissionLauncher.launch(permission);
            //Toast.makeText(getActivity(), R.string.asking_permission, Toast.LENGTH_LONG).show();
            return false;
        }
    }

    //权限打开
    private boolean requestSettingCanDrawOverlays() {
        if (checkFloatPermission(getContext()))
            return true;
        else {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            intent.setData(Uri.parse("package:com.liux.musicplayer"));
            requestOverlayPermissionLauncher.launch(intent);
            return false;
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case "isShowLyric":
                switch_desk_lyric.setChecked(sharedPreferences.getBoolean("isShowLyric", false));
                break;
            case "deskLyricLock":
                switch_desk_lyric_lock.setChecked(sharedPreferences.getBoolean("deskLyricLock", false));
                break;
            case "playList":
                dPlayList.setText(sharedPreferences.getString("playList", ""));
                break;
            case "nowId":
                ((EditTextPreference) findPreference("nowId")).setText(sharedPreferences.getString("nowId", "0"));
                break;
            case "playOrder":
                ((EditTextPreference) findPreference("playOrder")).setText(sharedPreferences.getString("playOrder", "0"));
                break;
            case "timing":
                seekBarTiming.setValue(sharedPreferences.getInt("timing", 0));
                break;
            case "cacheList":
                cacheList.setText(sharedPreferences.getString("cacheList", "[]"));
                break;
        }
    }

    public void httpResultHandle(String result, int funId) {
        switch (funId) {
            case 0:
                webPlayList.setText(result);
                ((MainActivity) getActivity()).getMusicService().setWebPlayMode(true);
                break;
            default:
                break;
        }
    }

    /**
     * 调用系统下载器下载文件
     *
     * @param context ()
     * @return boolean 是否成功调用系统下载
     */
    private boolean acquireDownload(Context context, String url) {
        Log.i(getClass().toString() + "//acquireDownload()", "Download requested");
        String fileName = url.substring(url.lastIndexOf('/') + 1);
        File localFile = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName);
        if (localFile.exists()) {
            Uri uri = FileProvider.getUriForFile(context, requireActivity().getPackageName(), localFile);
            Log.d(getClass().toString() + "//acquireDownload()", "File exists");
            Log.d(getClass().toString() + "//acquireDownload()", uri.toString());
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
            context.startActivity(intent);
            return true;
        } else {
            Log.d(getClass().toString() + "//acquireDownload()", "Download file");
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setTitle(context.getResources().getString(R.string.app_name));
            request.setDescription(context.getResources().getString(R.string.notification_downloading_latest_version));
            request.setVisibleInDownloadsUi(true);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
            File cloudFile = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName);
            request.setDestinationUri(Uri.fromFile(cloudFile));
            DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            if (downloadManager != null) {
                Long requestID = downloadManager.enqueue(request);
                context.registerReceiver(new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        DownloadManager.Query query = new DownloadManager.Query();
                        query.setFilterById(requestID);
                        Cursor cursor = downloadManager.query(query);
                        if (cursor.moveToFirst()) {
                            int columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                            if (DownloadManager.STATUS_SUCCESSFUL == cursor.getInt(columnIndex)) {
                                int columnIndexUri = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI);
                                String downloadFileName = cursor.getString(columnIndexUri);
                                if (downloadFileName != null) {
                                    downloadFileName = downloadFileName.substring(downloadFileName.lastIndexOf('/') + 1);
                                    File downloadFile = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), downloadFileName);
                                    Uri uri = FileProvider.getUriForFile(context, requireActivity().getPackageName(), downloadFile);
                                    Log.i(getClass().toString() + "//acquireDownload()", uri.toString());
                                    Intent installIntent = new Intent(Intent.ACTION_VIEW);
                                    installIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    installIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                    installIntent.setDataAndType(uri, "application/vnd.android.package-archive");
                                    context.startActivity(installIntent);
                                }
                            }
                        }
                    }
                }, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
                return true;
            }
        }
        return false;
    }
}