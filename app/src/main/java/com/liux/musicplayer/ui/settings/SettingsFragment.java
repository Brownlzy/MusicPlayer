package com.liux.musicplayer.ui.settings;

import static com.liux.musicplayer.util.UriTransform.getPath;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.liux.musicplayer.R;

public class SettingsFragment extends PreferenceFragmentCompat {
    //三个Preference控件
    private SwitchPreferenceCompat switch_permission;
    private Preference setMainFolder;
    private EditTextPreference MainFolder;
    //注册Activity回调
    ActivityResultLauncher<Intent> gotoAppInfo = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            int resultCode = result.getResultCode();
            if (resultCode == -1) {
                if (checkPermission()) switch_permission.setChecked(true);
            }
        }
    });
    //注册Activity回调，用于处理权限申请
    private ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Toast.makeText(getActivity(), R.string.permission_granted, Toast.LENGTH_LONG).show();
                    switch_permission.setChecked(true);
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
    //用于接受系统文件管理器返回目录的回调
    ActivityResultLauncher<Intent> getFolderIntent = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            int resultCode = result.getResultCode();
            if (resultCode == -1) {
                Intent data = result.getData();
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
        //绑定权限选项
        switch_permission = findPreference("permission");
        MainFolder = findPreference("mainFolder");
        setMainFolder = findPreference("setMainFolder");
        //隐藏手动输入文件路径的设置选项
        MainFolder.setVisible(false);
        //设置权限开关的监听
        switch_permission.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                if ((Boolean) newValue == Boolean.TRUE) {
                    return askPermission();
                } else {
                    return !checkPermission();
                }
            }
        });
        if (checkPermission()) switch_permission.setChecked(true);

        // 获取SharedPreferences对象
        SharedPreferences sp = getContext().getSharedPreferences("com.liux.musicplayer_preferences", Activity.MODE_PRIVATE);
        // 获取Editor对象
        SharedPreferences.Editor editor = sp.edit();
        //选择主文件目录
        MainFolder.setSummary(sp.getString("mainFolder", "/storage/emulated/0/Android/data/com.liux.musicplayer/Music/"));
        setMainFolder.setSummary(MainFolder.getSummary());
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
    }

    //检查权限是否获取成功
    public boolean checkPermission() {
        if (ContextCompat.checkSelfPermission(
                getContext(), Manifest.permission.READ_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            //拒绝修改开关
            return false;
        }
    }

    //请求权限
    public boolean askPermission() {
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getActivity(), R.string.permission_granted, Toast.LENGTH_LONG).show();
            return true;
        } else {
            requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            Toast.makeText(getActivity(), R.string.asking_permission, Toast.LENGTH_LONG).show();
            return false;
        }
    }
}