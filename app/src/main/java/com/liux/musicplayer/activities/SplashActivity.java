package com.liux.musicplayer.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.widget.ImageView;

import androidx.fragment.app.FragmentActivity;

import com.liux.musicplayer.R;
import com.liux.musicplayer.media.MusicLibrary;
import com.liux.musicplayer.media.SimpleMusicService;
import com.liux.musicplayer.models.User;
import com.liux.musicplayer.utils.CrashHandlers;
import com.liux.musicplayer.utils.SharedPrefs;

import java.io.File;

public class SplashActivity  extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        SharedPrefs.init(getApplication());
        MusicLibrary.init();
        User.init(getApplicationContext());
        if(User.isLogin)
            ((ImageView)findViewById(R.id.backgroundPic)).setImageURI(Uri.fromFile(new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                    User.userData.userName+User.userData.loginTime)));
        startService(new Intent(SplashActivity.this,SimpleMusicService.class));
    }


    @Override
    protected void onResume() {
        super.onResume();
        /*
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                SharedPrefs.init(getApplication());
                MusicLibrary.init();
                Intent intent = new Intent();
                intent.setClass(SplashActivity.this, SimpleMusicService.class);
                startService(intent);
                overridePendingTransition(0, 0);
            }
        });*/
        new Handler()
                .postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        //startService(new Intent(SplashActivity.this,SimpleMusicService.class));
                        startActivity(new Intent(SplashActivity.this, MainActivity.class));
                        finish();
                    }
                },100);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, 0);
    }
}