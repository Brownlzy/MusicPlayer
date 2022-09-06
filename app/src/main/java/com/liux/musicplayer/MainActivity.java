package com.liux.musicplayer;

import android.Manifest;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.liux.musicplayer.databinding.ActivityMainBinding;

import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private SeekBar playProgress;
    private TextView PlayBarTitle;
    private ImageView PlayBarPause;
    private ImageView PlayBarOrder;
    private ImageView PlayBarPrev;
    private ImageView PlayBarNext;
    private MusicPlayer musicPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        BottomNavigationView navView = findViewById(R.id.nav_view);
        playProgress = findViewById(R.id.seekBar);
        PlayBarTitle = findViewById(R.id.musicPlaying);
        PlayBarPause = findViewById(R.id.playPause);
        PlayBarOrder = findViewById(R.id.playOrder);
        PlayBarPrev = findViewById(R.id.playPrevious);
        PlayBarNext = findViewById(R.id.playNext);
        SharedPreferences sp = getSharedPreferences("com.liux.musicplayer_preferences", Activity.MODE_PRIVATE);
        musicPlayer = new MusicPlayer(MainActivity.this, sp.getString("playList", ""));
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_playlist, R.id.navigation_settings)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);
        navView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                if (item.getItemId() == R.id.navigation_home) {
                    PlayBarTitle.setVisibility(View.GONE);
                    playProgress.setVisibility(View.VISIBLE);
                } else {
                    PlayBarTitle.setVisibility(View.VISIBLE);
                    playProgress.setVisibility(View.GONE);
                }
                return NavigationUI.onNavDestinationSelected(item, navController);
            }
        });

        PlayBarPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setPlayOrPause();
            }
        });
        PlayBarOrder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (musicPlayer.getPlayOrder()) {
                    case 0:
                        musicPlayer.setPlayOrder(1);
                        PlayBarOrder.setImageDrawable(getDrawable(R.drawable.ic_round_repeat_one_24));
                        break;
                    case 1:
                        musicPlayer.setPlayOrder(2);
                        PlayBarOrder.setImageDrawable(getDrawable(R.drawable.ic_round_shuffle_24));
                        break;
                    case 2:
                    default:
                        musicPlayer.setPlayOrder(0);
                        PlayBarOrder.setImageDrawable(getDrawable(R.drawable.ic_round_repeat_24));
                        break;
                }
            }
        });
        PlayBarPrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playPrevOrNext(false);
            }
        });
        PlayBarNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playPrevOrNext(true);
            }
        });
        /*if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            switch (musicPlayer.playThis(Uri.parse("file:///storage/emulated/0/Music/OneRepublic - Counting Stars.mp3"))) {
                case -1:
                    Toast.makeText(MainActivity.this, "ERROR", Toast.LENGTH_SHORT).show();
                    break;
                default:
                    Toast.makeText(MainActivity.this, "READY", Toast.LENGTH_SHORT).show();
                    break;
            }
        //} else {
            Toast.makeText(MainActivity.this, "noPermission", Toast.LENGTH_SHORT).show();
        //}*/
        setNowPlayThis(musicPlayer.getNowID());
        setPlayOrPause();
    }

    private void setPlayOrPause() {
        if (musicPlayer.isPlaying()) {
            musicPlayer.pause();
            PlayBarPause.setImageDrawable(getDrawable(R.drawable.ic_round_play_circle_outline_24));
        } else {
            musicPlayer.start();
            PlayBarPause.setImageDrawable(getDrawable(R.drawable.ic_round_pause_circle_outline_24));
        }
    }

    public MusicPlayer getMusicPlayer() {
        return musicPlayer;
    }

    public void setPlayBarTitle(int musicId) {
        PlayBarTitle.setText(musicPlayer.getPlayList().get(musicId).title + " - " + musicPlayer.getPlayList().get(musicId).artist);
    }

    public void setNowPlayThis(int musicId) {
        musicPlayer.playThis(musicId);
        setPlayBarTitle(musicId);
        PlayBarPause.setImageDrawable(getDrawable(R.drawable.ic_round_pause_circle_outline_24));
    }

    public void playPrevOrNext(boolean isNext) {
        int maxId = musicPlayer.getMaxID();
        int nowId = musicPlayer.getNowID();
        int order = musicPlayer.getPlayOrder();
        if (order == 2) {
            Random r = new Random();
            nowId = r.nextInt(maxId + 1);
        } else {
            if (isNext) {
                if (nowId < maxId) nowId += 1;
                else nowId = 0;
            } else {
                if (nowId > 0) nowId = 0;
                else nowId = maxId;
            }
        }
        setNowPlayThis(nowId);
    }
}