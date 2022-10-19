package com.liux.musicplayer.ui;

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
import android.os.Handler;
import android.os.Looper;
import android.os.ResultReceiver;
import android.provider.DocumentsContract;
import android.provider.Settings;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.CheckBoxPreference;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SeekBarPreference;

import com.blankj.utilcode.util.FileUtils;
import com.blankj.utilcode.util.RegexUtils;
import com.blankj.utilcode.util.TimeUtils;
import com.google.gson.Gson;
import com.liux.musicplayer.R;
import com.liux.musicplayer.activities.MainActivity;
import com.liux.musicplayer.models.User;
import com.liux.musicplayer.utils.CrashHandlers;
import com.liux.musicplayer.utils.RSAUtils;
import com.liux.musicplayer.utils.SharedPrefs;
import com.liux.musicplayer.utils.UpdateUtils;
import com.liux.musicplayer.viewmodels.MyViewModel;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    private CheckBoxPreference switch_new_appearance;
    private Preference setMainFolder;
    private Preference clickGotoAppDetails;
    private Preference lastErrorLog;
    private Preference lastCrash;
    private Preference crashMe;
    private Preference About;
    private Preference userLogin;
    private Preference Close;
    private EditTextPreference rsaPublicKey;
    private EditTextPreference rsaDecodeTest;
    private EditTextPreference rsaPrivateKey;
    private EditTextPreference rsaEncodeTest;
    private EditTextPreference playingList;
    private EditTextPreference allSongList;
    private EditTextPreference webAllSongList;
    private EditTextPreference MainFolder;
    private EditTextPreference cacheList;
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
    private MyViewModel myViewModel;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Context context = getActivity();
        myViewModel = new ViewModelProvider(MainActivity.mainActivity).get(MyViewModel.class);
        assert context != null;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        setPreferencesFromResource(R.xml.root_preferences, rootKey);
        findPreference("debug").setVisible(false);
        findPreference("rsa").setVisible(false);
        //绑定控件
        switch_new_appearance = findPreference("isNewAppearance");
        switch_storage_permission = findPreference("storage_permission");
        switch_layer_permission = findPreference("layer_permission");
        switch_web_playlist = findPreference("isUseWebPlayList");
        switch_desk_lyric = findPreference("isShowLyric");
        switch_desk_lyric_lock = findPreference("deskLyricLock");
        rsaPublicKey = findPreference("rsa_public_key");
        rsaDecodeTest = findPreference("rsa_decode_test");
        rsaPrivateKey = findPreference("rsa_private_key");
        rsaEncodeTest = findPreference("rsa_encode_test");
        MainFolder = findPreference("mainFolder");
        cacheList = findPreference("cacheList");
        clickGotoAppDetails = findPreference("gotoAppDetails");
        lastCrash = findPreference("lastCrash");
        lastErrorLog = findPreference("lastErrorLog");
        crashMe = findPreference("crashMe");
        playingList = findPreference("playingList");
        setMainFolder = findPreference("setMainFolder");
        webAllSongList = findPreference("webAllSongList");
        allSongList = findPreference("allSongList");
        About = findPreference("info");
        Close = findPreference("exit");
        userLogin = findPreference("user");
        seekBarTiming = findPreference("timing");

        prefs.registerOnSharedPreferenceChangeListener(this); // 注册

        if (checkFloatPermission(getContext()))
            switch_layer_permission.setChecked(true);
        if (checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE))
            switch_storage_permission.setChecked(true);
        //错误日志
        lastCrash.setSummary(String.valueOf(prefs.getBoolean("lastCrash", false)));
        lastErrorLog.setSummary(prefs.getString("lastErrorLog", "null"));
        //选择主文件目录
        MainFolder.setSummary(prefs.getString("mainFolder", "/storage/emulated/0/Android/data/com.liux.musicplayer/Music/"));
        //MainFolder.setSummaryProvider(EditTextPreference.SimpleSummaryProvider.getInstance());
        playingList.setSummaryProvider(EditTextPreference.SimpleSummaryProvider.getInstance());
        setMainFolder.setSummary(MainFolder.getSummary());
        //获取当前是否在倒计时
        myViewModel.getmMediaController().sendCommand("IS_TIMING", null, timingResult);
        if (User.isLogin){
            userLogin.setTitle(User.userData.userName);
        }else{
            userLogin.setTitle(R.string.user_unlogin);
            findPreference("exp").setVisible(false);
            switch_new_appearance.setChecked(false);
        }
        //else if(SHA256Util.getSHA256StrJava(userRecord+getActivity().getPackageName()).equals())
        if (prefs.getString("rsa_private_key", "").equals("")
                || prefs.getString("rsa_public_key", "").equals("")) {
            try {
                Map<String, String> keMap = RSAUtils.genKeyPair();
                rsaPublicKey.setText(keMap.get("publicKey"));
                rsaPrivateKey.setText(keMap.get("privateKey"));
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }
        initPreferenceListener();
    }

    private final ResultReceiver timingResult = new ResultReceiver(new Handler(Looper.getMainLooper())) {
        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            if (!resultData.getBoolean("IS_TIMING", false))
                seekBarTiming.setValue(0);
        }
    };

    private void initPreferenceListener() {
        findPreference("newsBoard").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(@NonNull Preference preference) {
                UpdateUtils.checkNews(getContext(),true);
                return false;
            }
        });
        rsaDecodeTest.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                try {
                    Toast.makeText(getContext(), RSAUtils.publicKeyDecrypt((String) newValue, rsaPublicKey.getText()), Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(getContext(), "RSA Test Failed", Toast.LENGTH_SHORT).show();
                }
                return false;
            }
        });
        rsaEncodeTest.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                try {
                    //Toast.makeText(getContext(), RSAUtils.privateKeyEncrypt((String) newValue,rsaPrivateKey.getText()), Toast.LENGTH_SHORT).show();
                    rsaEncodeTest.setText(RSAUtils.privateKeyEncrypt((String) newValue, rsaPrivateKey.getText()));
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(getContext(), "RSA Test Failed", Toast.LENGTH_SHORT).show();
                }
                return false;
            }
        });
        userLogin.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(@NonNull Preference preference) {
                if(User.isLogin){
                    AlertDialog alertInfoDialog = null;
                    alertInfoDialog = new AlertDialog.Builder(getContext())
                            .setTitle(R.string.user_logined)
                            .setMessage(getString(R.string.userName)+User.userData.userName+"\n"
                                        +getString(R.string.userLevel)+User.userData.level+"\n"
                                        +getString(R.string.loginTime)+ TimeUtils.millis2String(Long.parseLong(User.userData.loginTime))+"\n"
                                        +getString(R.string.expiredTime)+TimeUtils.millis2String(Long.parseLong(User.userData.expired))
                            )
                            .setIcon(R.drawable.ic_round_account_circle_24)
                            .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                }
                            })
                            .setNeutralButton(R.string.logout, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    User.logout(getContext());
                                }
                            })
                            .create();
                    alertInfoDialog.show();
                }else {
                    userLogin.setTitle(R.string.user_unlogin);
                    findPreference("rsa").setVisible(false);
                    findPreference("exp").setVisible(false);
                    findPreference("debug").setVisible(false);
                    showLoginDialog();
                }
                return false;
            }
        });
        crashMe.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(@NonNull Preference preference) {
                //TODO: delete this
                List<Integer> aBug = new ArrayList<>();
                aBug.get(2);
                return false;
            }
        });
        lastErrorLog.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(@NonNull Preference preference) {
                if (lastErrorLog.getSummary() != null && !lastErrorLog.getSummary().equals("null")) {
                    if (FileUtils.isFileExists(requireContext().getExternalCacheDir() + "/log/" + lastErrorLog.getSummary()))
                        CrashHandlers.shareErrorLog((String) lastErrorLog.getSummary(), requireActivity());
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
        switch_new_appearance.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                if(User.isLogin) {
                    ((MainActivity) getActivity()).setNewAppearance((boolean) newValue);
                    return true;
                }else {
                    Toast.makeText(getContext(), "此功能仅限注册用户使用！请先登录", Toast.LENGTH_SHORT).show();
                    return false;
                }
            }
        });
        switch_desk_lyric.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                Intent lyricIntent;
                if ((boolean) newValue)
                    lyricIntent = new Intent("com.liux.musicplayer.OPEN_LYRIC");
                else
                    lyricIntent = new Intent("com.liux.musicplayer.CLOSE_LYRIC");
                getActivity().sendBroadcast(lyricIntent);
                return true;
            }
        });
        switch_desk_lyric_lock.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                Intent lyricIntent;
                if ((boolean) newValue)
                    lyricIntent = new Intent("com.liux.musicplayer.LOCK_LYRIC");
                else
                    lyricIntent = new Intent("com.liux.musicplayer.UNLOCK_LYRIC");
                getActivity().sendBroadcast(lyricIntent);
                return true;
            }
        });
        Close.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(@NonNull Preference preference) {
                //((MainActivity) requireActivity()).getMusicService().stopForeground(true);
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
                    //((MainActivity) getActivity()).getMusicService().setWebPlayMode(false);
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
                    Bundle bundle = new Bundle();
                    bundle.putBoolean("isStart", true);
                    myViewModel.getmMediaController().getTransportControls().sendCustomAction("TIMING", bundle);
                    //((MainActivity) getActivity()).getMusicService().startTiming();
                } else {
                    Bundle bundle = new Bundle();
                    bundle.putBoolean("isStart", false);
                    myViewModel.getmMediaController().getTransportControls().sendCustomAction("TIMING", bundle);
                    //((MainActivity) getActivity()).getMusicService().stopTiming();
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
        AlertDialog.Builder alertInfoDialog = null;
        alertInfoDialog = new AlertDialog.Builder(getContext());
        alertInfoDialog.setTitle(R.string.app_name);
        alertInfoDialog.setMessage(getString(R.string.appInfo).replace("\\n", "\n")
                        + versionName);
        alertInfoDialog.setIcon(R.mipmap.ic_launcher);
        alertInfoDialog.setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
        if(User.isLogin&&User.userData.level<=2) {
            alertInfoDialog.setNegativeButton(R.string.title_debug, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    findPreference("debug").setVisible(true);
                    if(User.isLogin&&User.userData.level<=0)
                        findPreference("rsa").setVisible(true);
                }
            });
        }
        alertInfoDialog.setNeutralButton(R.string.title_checkUpdate, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        UpdateUtils.checkUpdate(getContext(),true);
                    }
                });
        alertInfoDialog.show();
    }

    public void showLoginDialog() {
        /*@setView 装入一个EditView
         */
        final EditText editText = new EditText(getContext());
        AlertDialog.Builder inputDialog = new AlertDialog.Builder(getContext());
        inputDialog.setTitle(R.string.inputUserName).setView(editText);
        inputDialog.setIcon(R.drawable.ic_round_account_circle_24);
        inputDialog.setPositiveButton("登录",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(editText.getText().toString().trim().length()>=4)
                            User.login(getContext(),editText.getText().toString().trim(),false);
                        else
                            Toast.makeText(getContext(), "用户名长度最小为4", Toast.LENGTH_SHORT).show();
                    }
                });
        inputDialog.setNeutralButton("注册",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(editText.getText().toString().trim().length()>=4){
                            showRegisterDialog(editText.getText().toString().trim());
                        } else
                            Toast.makeText(getContext(), "用户名长度最小为4", Toast.LENGTH_SHORT).show();
                    }
                });
        inputDialog.setNegativeButton("取消",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        inputDialog.show();
    }

    private void showRegisterDialog(String userName) {
        final EditText editText = new EditText(getContext());
        editText.setOnClickListener(null);
        editText.setText(User.getUserHash(userName));
        AlertDialog.Builder inputDialog = new AlertDialog.Builder(getContext());
        inputDialog.setTitle(R.string.copyToRegister).setView(editText);
        inputDialog.setIcon(R.drawable.ic_round_account_circle_24);
        inputDialog.setPositiveButton(R.string.confirm,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
        inputDialog.show();
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
            case "playingList":
                playingList.setText(sharedPreferences.getString("playingList", ""));
                break;
            case "webAllSongList":
                webAllSongList.setText(sharedPreferences.getString("webAllSongList", ""));
                break;
            case "allSongList":
                allSongList.setText(sharedPreferences.getString("allSongList", ""));
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
            case "lastCrash":
                lastCrash.setSummary(String.valueOf(sharedPreferences.getBoolean("lastCrash", false)));
                lastErrorLog.setSummary(sharedPreferences.getString("lastErrorLog", "null"));
                break;

        }
    }

    public void httpResultHandle(String result, int funId) {
        switch (funId) {
            case 0:
                webAllSongList.setText(result);
                //((MainActivity) getActivity()).getMusicService().setWebPlayMode(true);
                break;
            default:
                break;
        }
    }

}