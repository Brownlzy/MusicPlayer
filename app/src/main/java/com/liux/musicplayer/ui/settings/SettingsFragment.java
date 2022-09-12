package com.liux.musicplayer.ui.settings;

import static com.liux.musicplayer.util.UriTransform.getPath;

import android.Manifest;
import android.app.Activity;
import android.app.AppOpsManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.Settings;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.preference.CheckBoxPreference;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.liux.musicplayer.R;

public class SettingsFragment extends PreferenceFragmentCompat {
    //Preference控件
    private CheckBoxPreference switch_storage_permission;
    private CheckBoxPreference switch_layer_permission;
    private Preference setMainFolder;
    private Preference clickGotoAppDetails;
    private EditTextPreference dPlayList;
    private EditTextPreference MainFolder;
    private Preference About;
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
            Uri docUri = DocumentsContract.buildDocumentUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri));
            //TODO：选择特殊文件夹时崩溃
            String path = getPath(getContext(), docUri);
            //Toast.makeText(getActivity(), uri.toString()+"\n"+path, Toast.LENGTH_LONG).show();
            //获取SharedPreferences对象
            SharedPreferences sp = getContext().getSharedPreferences("com.liux.musicplayer_preferences", Activity.MODE_PRIVATE);
            //获取Editor对象
            SharedPreferences.Editor editor = sp.edit();
            editor.putString("mainFolder", path);
            editor.apply();
            MainFolder.setSummary(sp.getString("mainFolder", "/storage/emulated/0/Android/data/com.liux.musicplayer/Music/"));
            setMainFolder.setSummary(MainFolder.getSummary());
            //Toast.makeText(getActivity(), sp.getString("mainFolder","---"), Toast.LENGTH_LONG).show();
        }
    });

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);
        //绑定（res/xml/root_preferences.xml）控件
        switch_storage_permission = findPreference("storage_permission");
        switch_layer_permission = findPreference("layer_permission");
        MainFolder = findPreference("mainFolder");
        clickGotoAppDetails = findPreference("gotoAppDetails");
        dPlayList = findPreference("playList");
        setMainFolder = findPreference("setMainFolder");
        About = findPreference("info");
        //为控件设置监听器，触发后执行重写的方法
        //前往应用详情页
        clickGotoAppDetails.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(@NonNull Preference preference) {
                Intent intent = new Intent("/");
                ComponentName cm = new ComponentName("com.android.settings", "com.android.settings.applications.InstalledAppDetails");
                intent.setComponent(cm);
                intent.setAction("android.intent.action.VIEW");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setData(Uri.parse("package:com.liux.musicplayer"));//设置Intent用于作为参数
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
        //检查权限获得情况，并体现在开关上
        if (checkFloatPermission(getContext()))
            switch_layer_permission.setChecked(true);
        if (checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE))
            switch_storage_permission.setChecked(true);
        //findPreference("bugfix").setVisible(false);
/*
        // 获取SharedPreferences对象
        SharedPreferences sp = getContext().getSharedPreferences("com.liux.musicplayer_preferences", Activity.MODE_PRIVATE);
        // 获取Editor对象，可以用put方法修改设置，但要apply提交
        SharedPreferences.Editor editor = sp.edit();
*/
        //选择主文件目录
        //SharedPreferences类用于读取设置
        SharedPreferences sp = getContext().getSharedPreferences("com.liux.musicplayer_preferences", Activity.MODE_PRIVATE);
        MainFolder.setSummary(sp.getString("mainFolder", "/storage/emulated/0/Android/data/com.liux.musicplayer/Music/"));
        //MainFolder.setSummaryProvider(EditTextPreference.SimpleSummaryProvider.getInstance());
        dPlayList.setSummaryProvider(EditTextPreference.SimpleSummaryProvider.getInstance());
        setMainFolder.setSummary(MainFolder.getSummary());
        //监听权限开关按钮
        setMainFolder.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(@NonNull Preference preference) {
                try {
                    //启动之前注册的回调，并设置参数
                    getFolderIntent.launch(new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE));
                } catch (Exception e) {
                    //报错
                    Toast.makeText(getActivity(), "没有正确打开文件管理器", Toast.LENGTH_SHORT).show();
                }
                return false;
            }
        });

        About.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(@NonNull Preference preference) {
                popInfo();//弹出关于窗口
                return false;
            }
        });
    }


    private void popInfo() {
        AlertDialog alertInfoDialog = new AlertDialog.Builder(getContext())
                .setTitle(R.string.app_name)//标题，R类是由res文件夹下内容生成的
                .setMessage(R.string.appInfo)//内容
                .setIcon(R.mipmap.ic_launcher)
                .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {//设置监听器
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //按钮点击后执行
                    }
                })
                .create();
        alertInfoDialog.show();
    }

    //检查权限是否获取成功
    public boolean checkPermission(String permission) {
        //根据返回结果选择拒绝修改开关
        return ContextCompat.checkSelfPermission(getContext(), permission) == PackageManager.PERMISSION_GRANTED;
    }

    //判断是否开启悬浮窗权限
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

    //请求权限（先检查再请求）
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

    //同上
    private boolean requestSettingCanDrawOverlays() {
        if (checkFloatPermission(getContext()))
            return true;
        else {
            //未成功，跳到设置页面让用户设置
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            intent.setData(Uri.parse("package:com.liux.musicplayer"));
            requestOverlayPermissionLauncher.launch(intent);
            return false;
        }
    }
}