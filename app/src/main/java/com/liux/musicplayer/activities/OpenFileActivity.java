package com.liux.musicplayer.activities;

import android.Manifest;
import android.app.ActivityOptions;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.media.session.MediaControllerCompat;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.splashscreen.SplashScreen;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.blankj.utilcode.util.FileUtils;
import com.liux.musicplayer.BuildConfig;
import com.liux.musicplayer.R;
import com.liux.musicplayer.media.MediaBrowserHelper;
import com.liux.musicplayer.media.MusicLibrary;
import com.liux.musicplayer.models.Song;
import com.liux.musicplayer.services.MusicService;
import com.liux.musicplayer.ui.InfoFragment;
import com.liux.musicplayer.utils.CrashHandlers;
import com.liux.musicplayer.utils.PermissionUtils;
import com.liux.musicplayer.utils.SharedPrefs;
import com.liux.musicplayer.utils.UriTransform;

public class OpenFileActivity extends FragmentActivity {
    private static final String TAG = "OpenMusicFile";
    private Song song;
    InfoFragment infoFragment;
    String path;
    //注册Activity回调
    ActivityResultLauncher<Intent> gotoAppInfo = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            int resultCode = result.getResultCode();
            Log.e(TAG, String.valueOf(resultCode));
            if (PermissionUtils.checkPermission(OpenFileActivity.this,Manifest.permission.READ_EXTERNAL_STORAGE))
                showInfo();
            else
                finish();
            if (resultCode == -1) {
            }
        }
    });
    //注册Activity回调，用于处理权限申请
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Toast.makeText(this, R.string.permission_granted, Toast.LENGTH_LONG).show();
                    showInfo();
                } else {
                    Toast.makeText(this, R.string.asking_permission, Toast.LENGTH_LONG).show();
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
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_music_file);
        CrashHandlers crashHandlers = CrashHandlers.getInstance();
        crashHandlers.init(this);
        SharedPrefs.init(getApplication());
        MusicLibrary.init();
        if(BuildConfig.DEBUG)
            findViewById(R.id.tabDebugText).setVisibility(View.VISIBLE);
        Intent intent = getIntent();
        String action = intent.getAction();
        path = "null";
        Bundle bundle=new Bundle();
        if (Intent.ACTION_VIEW.equals(action)) {
            Uri uri = intent.getData();

            if (uri != null) {
                String scheme= uri.getScheme();
                Log.e(TAG,scheme);
                String host=uri.getHost();
                Log.e(TAG,host);
                String port=uri.getPort()+"";
                Log.e(TAG,port);
                path=uri.getPath();
                Log.e(TAG,path);
                if(!FileUtils.isFileExists(path))
                    path= UriTransform.getRealPathFromUri(this,uri);
                Log.e(TAG,path);
                bundle.putString("path",path);
            }else {
                finish();
            }
        }
        startService(new Intent(OpenFileActivity.this, MusicService.class));
        setContentView(R.layout.activity_open_music_file);
        infoFragment =new InfoFragment();
        infoFragment.setArguments(bundle);
        Log.e(TAG,bundle.toString());
        if(!PermissionUtils.checkPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)){
            PermissionUtils.askPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE,requestPermissionLauncher);
        }else {
            showInfo();
        }
    }

    private void showInfo() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, infoFragment);
        fragmentTransaction.commit();
        String finalPath = path;
        findViewById(R.id.play).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                song = new Song(finalPath);
                MusicLibrary.putSongToTreeMap(song);
                MediaBrowserConnection mMediaBrowserHelper = new MediaBrowserConnection(getApplication().getApplicationContext());
                mMediaBrowserHelper.onStart();
            }
        });
    }

    @Override
    public void finish() {
        super.finish();
        //overridePendingTransition(0, 0);
    }

    private class MediaBrowserConnection extends MediaBrowserHelper {
        private MediaBrowserConnection(Context context) {
            super(context, MusicService.class);
        }
        @Override
        protected void onConnected(@NonNull MediaControllerCompat mediaController) {
            mediaController.addQueueItem(MusicLibrary.getMediaItemDescription(song), -2);
            Intent intent=new Intent(OpenFileActivity.this,MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("pageId",0);
            ImageView playButton=findViewById(R.id.play);
            ActivityOptions optionsCompat=ActivityOptions.makeSceneTransitionAnimation(OpenFileActivity.this,playButton,"playButton");
            startActivity(intent,optionsCompat.toBundle());
            new Handler()
                    .postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            finish();
                        }
                    },1000);
        }
    }
}