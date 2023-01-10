package com.liux.musicplayer.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.core.splashscreen.SplashScreen;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.liux.musicplayer.BuildConfig;
import com.liux.musicplayer.R;
import com.liux.musicplayer.ui.AboutFragment;
import com.liux.musicplayer.utils.CustomDialogUtils;

public class AboutActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, new AboutFragment());
        fragmentTransaction.commit();
        if(BuildConfig.DEBUG)
            findViewById(R.id.tabDebugText).setVisibility(View.VISIBLE);
        ((TextView)findViewById(R.id.versionName)).setText(
                "Version "
                +BuildConfig.VERSION_NAME
                +(BuildConfig.DEBUG?"_debug ":" " )
                +"("+BuildConfig.VERSION_CODE+")"
        );
        findViewById(R.id.changeLog).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CustomDialogUtils.openUrl(AboutActivity.this,"https://brownlzy.github.io/MyOtaInfo/MusicPlayer/changelog.txt");
            }
        });
        findViewById(R.id.openSource).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CustomDialogUtils.openUrl(AboutActivity.this,"https://brownlzy.github.io/MyOtaInfo/MusicPlayer/opensource.html");
            }
        });
        findViewById(R.id.contactMe).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent data=new Intent(Intent.ACTION_SENDTO);
                data.setData(Uri.parse("mailto:brownmusicplayer@outlook.com"));
                data.putExtra(Intent.EXTRA_SUBJECT, "[BrownMusic]Contact");
                data.putExtra(Intent.EXTRA_TEXT, "From BrownMusic v"+BuildConfig.VERSION_NAME+" ("+BuildConfig.VERSION_CODE+")\n==============================");
                startActivity(data);
            }
        });
    }
}