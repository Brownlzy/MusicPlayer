package com.liux.musicplayer.ui;

import static com.liux.musicplayer.utils.UriTransform.getPath;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ResultReceiver;
import android.provider.DocumentsContract;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
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
import com.google.gson.reflect.TypeToken;
import com.liux.musicplayer.R;
import com.liux.musicplayer.activities.AboutActivity;
import com.liux.musicplayer.activities.MainActivity;
import com.liux.musicplayer.activities.UserActivity;
import com.liux.musicplayer.media.MusicLibrary;
import com.liux.musicplayer.models.Song;
import com.liux.musicplayer.utils.CrashHandlers;
import com.liux.musicplayer.utils.PermissionUtils;
import com.liux.musicplayer.utils.RSAUtils;
import com.liux.musicplayer.utils.SharedPrefs;
import com.liux.musicplayer.utils.User;
import com.liux.musicplayer.viewmodels.MyViewModel;

import java.io.IOException;
import java.lang.reflect.Type;
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
    private final static String TAG = "SettingFragment";
    //Preference控件
    private CheckBoxPreference switch_storage_permission;
    private CheckBoxPreference switch_layer_permission;
    //注册Activity回调
    ActivityResultLauncher<Intent> gotoAppInfo = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            int resultCode = result.getResultCode();
            if (resultCode == -1) {
                if (PermissionUtils.checkPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE))
                    switch_storage_permission.setChecked(true);
                if (PermissionUtils.checkFloatPermission(getContext()))
                    switch_layer_permission.setChecked(true);
            }
        }
    });
    //注册Activity回调，用于处理权限申请
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Toast.makeText(getActivity(), R.string.permission_granted, Toast.LENGTH_LONG).show();
                    if (PermissionUtils.checkPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE))
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
                if (PermissionUtils.checkFloatPermission(getContext())) {
                    Toast.makeText(getActivity(), R.string.permission_granted, Toast.LENGTH_LONG).show();
                    switch_layer_permission.setChecked(true);
                } else {
                    Toast.makeText(getActivity(), R.string.permission_not_granted, Toast.LENGTH_LONG).show();
                }
            });
    private CheckBoxPreference switch_web_playlist;
    private CheckBoxPreference switch_desk_lyric;
    private CheckBoxPreference switch_desk_lyric_lock;
    private CheckBoxPreference switch_new_appearance;
    private CheckBoxPreference switch_fast_start;
    private Preference setMainFolder;
    private Preference clickGotoAppDetails;
    private Preference lastErrorLog;
    private Preference lastCrash;
    private Preference crashMe;
    private Preference About;
    private Preference userLogin;
    private Preference Close;
    private Preference autoRun;
    private EditTextPreference newUser;
    private EditTextPreference rsaPublicKey;
    private EditTextPreference rsaDecodeTest;
    private EditTextPreference rsaPrivateKey;
    private EditTextPreference rsaEncodeTest;
    private EditTextPreference playingList;
    private EditTextPreference allSongList;
    private EditTextPreference webAllSongList;
    private EditTextPreference MainFolder;
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
    private EditTextPreference cacheList;
    private SeekBarPreference seekBarTiming;
    private final ResultReceiver timingResult = new ResultReceiver(new Handler(Looper.getMainLooper())) {
        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            if (!resultData.getBoolean("IS_TIMING", false))
                seekBarTiming.setValue(0);
        }
    };
    private SharedPreferences prefs;
    private MyViewModel myViewModel;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Context context = getActivity();
        myViewModel = new ViewModelProvider(MainActivity.mainActivity).get(MyViewModel.class);
        assert context != null;
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.registerOnSharedPreferenceChangeListener(this); // 注册

        setPreferencesFromResource(R.xml.root_preferences, rootKey);
        findPreference("debug").setVisible(false);
        findPreference("rsa").setVisible(false);
        //绑定控件
        switch_new_appearance = findPreference("isNewAppearance");
        switch_fast_start = findPreference("isNeedFastStart");
        switch_storage_permission = findPreference("storage_permission");
        switch_layer_permission = findPreference("layer_permission");
        autoRun = findPreference("autoRun");
        switch_web_playlist = findPreference("isUseWebPlayList");
        switch_desk_lyric = findPreference("isShowLyric");
        switch_desk_lyric_lock = findPreference("deskLyricLock");
        newUser = findPreference("newUser");
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
        webAllSongList = findPreference("SONGLIST_webAllSongList");
        allSongList = findPreference("allSongList");
        About = findPreference("info");
        Close = findPreference("exit");
        userLogin = findPreference("user");
        seekBarTiming = findPreference("timing");

        if (PermissionUtils.checkFloatPermission(getContext()))
            switch_layer_permission.setChecked(true);
        if (PermissionUtils.checkPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE))
            switch_storage_permission.setChecked(true);
        //错误日志
        lastCrash.setSummary(String.valueOf(prefs.getBoolean("lastCrash", false)));
        lastErrorLog.setSummary(prefs.getString("lastErrorLog", "null"));
        //选择主文件目录
        MainFolder.setSummary(prefs.getString("mainFolder", "/storage/emulated/0/Android/data/com.liux.musicplayer/Music/"));
        //MainFolder.setSummaryProvider(EditTextPreference.SimpleSummaryProvider.getInstance());
        //playingList.setSummaryProvider(EditTextPreference.SimpleSummaryProvider.getInstance());
        setMainFolder.setSummary(MainFolder.getSummary());
        //获取当前是否在倒计时
        myViewModel.getmMediaController().sendCommand("IS_TIMING", null, timingResult);
        if (User.isLogin) {
            userLogin.setTitle(User.userData.userName);
        } else {
            userLogin.setTitle(R.string.user_unlogin);
            findPreference("exp").setVisible(false);
            switch_new_appearance.setChecked(false);
        }
        //else if(SHA256Util.getSHA256StrJava(userRecord+getActivity().getPackageName()).equals())
        if (prefs.getString("rsa_private_key", "").equals("")
                || prefs.getString("rsa_public_key", "").equals("")) {
            try {
                Map<String, String> keMap = RSAUtils.genKeyPair();
                rsaPublicKey.setText(keMap.get("publicKey").replace("\n", "").replace(" ", ""));
                rsaPrivateKey.setText(keMap.get("privateKey").replace("\n", "").replace(" ", ""));
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }
        initPreferenceListener();
    }

    private void initPreferenceListener() {
        autoRun.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(@NonNull Preference preference) {
                PermissionUtils.startToAutoStartSetting(getContext());
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
        newUser.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                try {
                    rsaEncodeTest.setText(RSAUtils.privateKeyEncrypt((String) newValue, rsaPrivateKey.getText()));
                    rsaDecodeTest.setText(RSAUtils.publicKeyDecrypt(rsaEncodeTest.getText(), rsaPublicKey.getText()));
                    if (rsaDecodeTest.getText().equals(newValue)) {
                        newUser.setText("{\"publicKey\":\"" + rsaPublicKey.getText().replace("\n", "").replace(" ", "")
                                + "\",\"userHashRSA\":\"" + rsaEncodeTest.getText().replace("\n", "").replace(" ", "")
                                + "\",\"join\":\"" + TimeUtils.getNowMills() + "\",\"expired\":\"\",\"level\":2,\"userSplash\":\"\"}");
                    } else throw new Exception("Decode Failed");
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                }
                return false;
            }
        });
        userLogin.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(@NonNull Preference preference) {
                startActivity(new Intent(getContext(), UserActivity.class));
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
                        CrashHandlers.checkIfExistsLastCrash(getActivity(), true);
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
                if (User.isLogin) {
                    ((MainActivity) getActivity()).setNewAppearance((boolean) newValue);
                    return true;
                } else {
                    Toast.makeText(getContext(), "此功能仅限注册用户使用！请先登录", Toast.LENGTH_SHORT).show();
                    return false;
                }
            }
        });
        switch_fast_start.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                if (User.isLogin || !((boolean) newValue)) {
                    return true;
                } else {
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
                    lyricIntent = new Intent(getContext().getPackageName() + ".OPEN_LYRIC");
                else
                    lyricIntent = new Intent(getContext().getPackageName() + ".CLOSE_LYRIC");
                getActivity().sendBroadcast(lyricIntent);
                return true;
            }
        });
        switch_desk_lyric_lock.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                Intent lyricIntent;
                if ((boolean) newValue)
                    lyricIntent = new Intent(getContext().getPackageName() + ".LOCK_LYRIC");
                else
                    lyricIntent = new Intent(getContext().getPackageName() + ".UNLOCK_LYRIC");
                getActivity().sendBroadcast(lyricIntent);
                return true;
            }
        });
        Close.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(@NonNull Preference preference) {
                AlertDialog alertDialog = new AlertDialog.Builder(getContext())
                        .setIcon(R.drawable.ic_round_exit_to_app_24)
                        .setTitle(R.string.exitApp)
                        .setMessage(R.string.sure_to_exit)
                        .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                getActivity().finish();
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        System.exit(0);
                                    }
                                }, 1000);
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
                    return PermissionUtils.askPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE, requestPermissionLauncher);
                } else {
                    return !PermissionUtils.checkPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE);
                }
            }
        });
        switch_layer_permission.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                if (newValue == Boolean.TRUE) {
                    return PermissionUtils.requestSettingCanDrawOverlays(getContext(), requestOverlayPermissionLauncher);
                } else {
                    return !PermissionUtils.checkFloatPermission(getContext());
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
                                            if (!httpResultHandle(result, 0)) {
                                                Toast.makeText(requireActivity(), "解析播放列表失败:" + id, Toast.LENGTH_SHORT).show();
                                                switch_web_playlist.setChecked(false);
                                            }
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
                    new Handler()
                            .postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    MusicLibrary.refreshAllSongListList();
                                }
                            }, (User.isLogin && SharedPrefs.getIsNeedFastStart()) ? 100 : 2100);
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
                //popInfo();
                Intent intent = new Intent(getContext(), AboutActivity.class);
                startActivity(intent);
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
            case "SONGLIST_webAllSongList":
                webAllSongList.setText(sharedPreferences.getString("SONGLIST_webAllSongList", ""));
                break;
            case "SONGLIST_allSongList":
                allSongList.setText(sharedPreferences.getString("SONGLIST_allSongList", ""));
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

    public boolean httpResultHandle(String result, int funId) {
        switch (funId) {
            case 0:
                //webAllSongList.setText(result);
                Gson gson = new Gson();
                Type songListType = new TypeToken<ArrayList<Song>>() {
                }.getType();
                List<Song> aList = gson.fromJson(result, songListType);
                if (aList != null) {
                    SharedPrefs.saveSongListByName(aList, "webAllSongList");
                    List<MusicLibrary.SongList> songLists = SharedPrefs.getAllSonglistList();
                    songLists.removeIf(songList -> songList.n.equals("webAllSongList"));
                    songLists.add(1, new MusicLibrary.SongList("webAllSongList", "", aList.size()));
                    SharedPrefs.putAllSonglistList(songLists);
                    MusicLibrary.refreshAllSongListList();
                    return true;
                }
                break;
            default:
                break;
        }
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (PreferenceManager.getDefaultSharedPreferences(getContext()).getBoolean("showDebug", false)) {
            findPreference("debug").setVisible(true);
            if (User.isLogin && User.userData.level <= 0)
                findPreference("rsa").setVisible(true);
        } else {
            findPreference("debug").setVisible(false);
            findPreference("rsa").setVisible(false);
        }
    }
}