package com.liux.musicplayer.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.liux.musicplayer.BuildConfig;
import com.liux.musicplayer.R;
import com.liux.musicplayer.ui.AboutFragment;
import com.liux.musicplayer.ui.UserFragment;
import com.liux.musicplayer.utils.SharedPrefs;
import com.liux.musicplayer.utils.User;

import java.io.File;

public class UserActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, new UserFragment());
        fragmentTransaction.commit();
        if(BuildConfig.DEBUG)
            findViewById(R.id.tabDebugText).setVisibility(View.VISIBLE);
        TextView userName=findViewById(R.id.userName);
        if(User.isLogin){
            userName.setText(User.userData.userName);
        }else {
            userName.setText(R.string.user_unlogin);
        }
    }

    public void setSplashPic() {
        int type= SharedPrefs.getSplashType();
        ((ImageView)findViewById(R.id.backgroundPic)).setImageURI(Uri.fromFile(new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                User.userData.userName+(type==0?"":"_custom"))));
    }
}