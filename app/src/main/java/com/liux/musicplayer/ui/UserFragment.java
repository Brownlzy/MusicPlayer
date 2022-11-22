package com.liux.musicplayer.ui;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.blankj.utilcode.util.FileUtils;
import com.blankj.utilcode.util.TimeUtils;
import com.liux.musicplayer.BuildConfig;
import com.liux.musicplayer.R;
import com.liux.musicplayer.activities.MainActivity;
import com.liux.musicplayer.activities.SplashActivity;
import com.liux.musicplayer.activities.UserActivity;
import com.liux.musicplayer.models.UserData;
import com.liux.musicplayer.utils.ClipboardUtils;
import com.liux.musicplayer.utils.CustomDialogUtils;
import com.liux.musicplayer.utils.PermissionUtils;
import com.liux.musicplayer.utils.SharedPrefs;
import com.liux.musicplayer.utils.UriTransform;
import com.liux.musicplayer.utils.User;

import java.io.File;

public class UserFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener{
    private SharedPreferences prefs;
    private Preference login,logout,userLevel,joinTime,loginTime,expiredTime,setImagePath,viewImage,clearImagePath;
    private ListPreference splashType;
    //用于接受系统文件管理器返回目录的回调
    ActivityResultLauncher<Intent> getImageIntent = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        int resultCode = result.getResultCode();
        if (resultCode == -1) {
            Intent data = result.getData();
            assert data != null;
            Uri uri = data.getData();
            setImage(uri);
        }
    });

    private void setImage(Uri uri) {
        try {
            String path=UriTransform.getRealPathFromUri(getContext(), uri);
//        Toast.makeText(getContext(), UriTransform.getRealPathFromUri(getContext(),uri), Toast.LENGTH_SHORT).show();
            FileUtils.copy(path,
                    new File(getContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                            User.userData.userName+"_custom").getPath()
            );
            prefs.edit().putString("splashPicPath",path).apply();
            setImagePath.setSummary(path);
        }catch (Exception e){
            Toast.makeText(getContext(), "读取图片失败", Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.user_preferences, rootKey);
        prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        prefs.registerOnSharedPreferenceChangeListener(this); // 注册
        login=findPreference("login");
        logout=findPreference("logout");
        userLevel=findPreference("userLevel");
        joinTime=findPreference("joinTime");
        loginTime=findPreference("loginTime");
        expiredTime=findPreference("expiredTime");
        splashType=findPreference("splashType");
        setImagePath=findPreference("splashPicPath");
        viewImage=findPreference("viewSplashPic");
        clearImagePath=findPreference("clearLocalSplash");
        initSetting();
    }

    private void initSetting() {
        if(User.isLogin){
            login.setVisible(false);
            userLevel.setSummary(String.valueOf(User.userData.level));
            joinTime.setSummary(TimeUtils.millis2String(Long.parseLong(User.userData.join)));
            loginTime.setSummary(TimeUtils.millis2String(Long.parseLong(User.userData.loginTime)));
            expiredTime.setSummary(TimeUtils.millis2String(Long.parseLong(User.userData.expired)));
            if(FileUtils.isFileExists(new File(getContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                    User.userData.userName+"_custom"))
                    &&!prefs.getString("splashPicPath","null").equals("null"))
                setImagePath.setSummary(prefs.getString("splashPicPath","null"));
        }else {
            logout.setVisible(false);
            userLevel.setVisible(false);
            joinTime.setVisible(false);
            loginTime.setVisible(false);
            expiredTime.setVisible(false);
            
            splashType.setEnabled(false);
            setImagePath.setEnabled(false);
            viewImage.setEnabled(false);
            clearImagePath.setEnabled(false);
        }
        setImagePath.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(@NonNull Preference preference) {
                if(PermissionUtils.checkPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    //Intent.ACTION_GET_CONTENT = "android.intent.action.GET_CONTENT"
                    intent.setType("image/*");
                    getImageIntent.launch(intent);
                }else {
                    showNoPermissionDialog();
                }
                return false;
            }
        });
        viewImage.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(@NonNull Preference preference) {
                ((UserActivity)getActivity()).setSplashPic();
                return false;
            }
        });
        logout.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(@NonNull Preference preference) {
                if(User.isLogin)
                    showSureLogout();
                else
                    Toast.makeText(getContext(), "你已成功注销，请手动重启", Toast.LENGTH_LONG).show();
                return false;
            }
        });
        login.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(@NonNull Preference preference) {
                if(!User.isLogin)
                    showLoginDialog();
                else
                    Toast.makeText(getContext(), "你已成功登录，若开屏图片下载完毕请手动重启", Toast.LENGTH_LONG).show();
                return false;
            }
        });
        clearImagePath.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(@NonNull Preference preference) {
                SharedPrefs.cleanSplashPath();
                FileUtils.delete(new File(getContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES), User.userData.userName+"_custom"));
                setImagePath.setSummary(null);
                return false;
            }
        });
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key){

        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    private void showNoPermissionDialog() {
        AlertDialog alertInfoDialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.no_permission)
                .setMessage(R.string.no_permission_info_2)
                .setIcon(R.mipmap.ic_launcher)
                .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .create();
        alertInfoDialog.show();
    }

    public void showLoginDialog() {
        DialogInterface.OnClickListener pos=new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(CustomDialogUtils.editText.getText().toString().trim().length()>=4)
                    User.login(getContext(),CustomDialogUtils.editText.getText().toString().trim(),false);
                else
                    Toast.makeText(getContext(), "用户名长度最小为4", Toast.LENGTH_SHORT).show();
            }
        };
        DialogInterface.OnClickListener normal=new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(CustomDialogUtils.editText.getText().toString().trim().length()>=4){
                    showRegisterDialog(CustomDialogUtils.editText.getText().toString().trim());
                } else
                    Toast.makeText(getContext(), "用户名长度最小为4", Toast.LENGTH_SHORT).show();
            }
        };
        CustomDialogUtils.editTextDialog(getContext(),
                getString(R.string.inputUserName),
                R.drawable.ic_round_account_circle_24,
                "登录",
                getString(R.string.cancel),
                "注册",
                pos,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                },
                normal);

    }
    private void showRegisterDialog(String userName) {
        String userHash=User.getUserHash(userName);
        AlertDialog.Builder codeDialog = new AlertDialog.Builder(getContext());
        codeDialog.setTitle(R.string.copyToRegister);
        codeDialog.setMessage(userHash);
        codeDialog.setCancelable(false);
        codeDialog.setIcon(R.drawable.ic_round_account_circle_24);
        codeDialog.setNeutralButton(R.string.send_error_log_by_mail,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent data=new Intent(Intent.ACTION_SENDTO);
                        data.setData(Uri.parse("mailto:brownmusicplayer@outlook.com"));
                        data.putExtra(Intent.EXTRA_SUBJECT, "[BrownMusic]SignUp");
                        data.putExtra(Intent.EXTRA_TEXT, "From BrownMusic v"+ BuildConfig.VERSION_NAME+" ("+BuildConfig.VERSION_CODE+")\n"+
                                "==============================\nuserHash:\n"+userHash);
                        startActivity(data);
                    }
                });
        codeDialog.setPositiveButton(R.string.copy,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ClipboardUtils.copyText(getContext(),userHash);
                    }
                });
        codeDialog.setNegativeButton(R.string.cancel,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
        codeDialog.show();
    }

    private void showSureLogout(){
        AlertDialog alertInfoDialog = null;
        alertInfoDialog = new AlertDialog.Builder(getContext())
                .setTitle(R.string.sure)
                .setMessage(R.string.sure_to_logout)
                .setIcon(R.drawable.ic_round_account_circle_24)
                .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        User.logout(getContext());
                    }
                })
                .setNeutralButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .create();
        alertInfoDialog.show();
    }
}
