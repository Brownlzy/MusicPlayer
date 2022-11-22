package com.liux.musicplayer.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;

import com.liux.musicplayer.R;
import com.liux.musicplayer.media.MusicLibrary;
import com.liux.musicplayer.services.MusicService;
import com.liux.musicplayer.utils.SharedPrefs;
import com.liux.musicplayer.utils.User;

import java.io.File;

public class SplashActivity  extends FragmentActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        SharedPrefs.init(getApplication());
        MusicLibrary.init();
        User.init(getApplicationContext());
        if(User.isLogin){
            int type=SharedPrefs.getSplashType();
            ((ImageView)findViewById(R.id.backgroundPic)).setImageURI(Uri.fromFile(new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                    User.userData.userName+(type==0?"":"_custom"))));
        }
        startService(new Intent(SplashActivity.this, MusicService.class));
    }


    @Override
    protected void onResume() {
        super.onResume();
        new Handler()
                .postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        startMainActivity();
                    }
                },(User.isLogin&&SharedPrefs.getIsNeedFastStart())?100:2100);
    }

    public void startMainActivity(){
        int verCode=SharedPrefs.getVersionCode();
        if(verCode>0&&verCode<30) {
            Toast.makeText(SplashActivity.this, "检测到低版本数据，请清除数据后使用", Toast.LENGTH_LONG).show();
        }else {
            if(verCode==30&&User.isLogin) {
                User.logout(this);
                Toast.makeText(SplashActivity.this, "由于登录系统修改，重新登录后才能正常显示开屏图片", Toast.LENGTH_LONG).show();
            }
            if(verCode==-1) SharedPrefs.cleanOldData();
            SharedPrefs.putVersionCode();
            Intent intent =new Intent(SplashActivity.this, MainActivity.class);
            intent.putExtra("splash", true);
            startActivity(intent);
            overridePendingTransition(0, 0);
            finish();
        }
    }
    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, 0);
    }
}