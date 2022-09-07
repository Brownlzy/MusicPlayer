package com.liux.musicplayer;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.liux.musicplayer.databinding.ActivityMainBinding;
import com.liux.musicplayer.ui.home.HomeFragment;
import com.liux.musicplayer.ui.playlist.PlaylistFragment;
import com.liux.musicplayer.ui.settings.SettingsFragment;

import java.util.Random;

public class MainActivity extends FragmentActivity {

    private ActivityMainBinding binding;
    private SeekBar playProgress;
    private TextView PlayBarTitle;
    private TextView TabTitle;
    private ImageView PlayBarPause;
    private ImageView PlayBarOrder;
    private ImageView PlayBarPrev;
    private ImageView PlayBarNext;
    private MusicPlayer musicPlayer;
    /**
     * The number of pages (wizard steps) to show in this demo.
     */
    private static final int NUM_PAGES = 3;
    private HomeFragment homeFragment;
    private PlaylistFragment playlistFragment;
    private SettingsFragment settingsFragment;
    /**
     * The pager widget, which handles animation and allows swiping horizontally to access previous
     * and next wizard steps.
     */
    private ViewPager2 viewPager;
    private BottomNavigationView bottomNavigationView;
    /**
     * The pager adapter, which provides the pages to the view pager widget.
     */
    private FragmentStateAdapter pagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //BottomNavigationView navView = findViewById(R.id.nav_view);
        playProgress = findViewById(R.id.seekBar);
        PlayBarTitle = findViewById(R.id.musicPlaying);
        TabTitle = findViewById(R.id.tabText);
        PlayBarPause = findViewById(R.id.playPause);
        PlayBarOrder = findViewById(R.id.playOrder);
        PlayBarPrev = findViewById(R.id.playPrevious);
        PlayBarNext = findViewById(R.id.playNext);
        homeFragment = new HomeFragment();
        playlistFragment = new PlaylistFragment();
        settingsFragment = new SettingsFragment();

        SharedPreferences sp = getSharedPreferences("com.liux.musicplayer_preferences", Activity.MODE_PRIVATE);
        musicPlayer = new MusicPlayer(MainActivity.this, sp.getString("playList",
                "[{\"id\":-1,\"title\":\"这是音乐标题\",\"artist\":\"这是歌手\",\"album\":\"这是专辑名\",\"filename\":\"此为测试数据，添加音乐文件后自动删除\"," +
                        "\"source_uri\":\"file:///storage/emulated/0/Android/data/com.liux.musicplayer/Music/eg\"," +
                        "\"lyric_uri\":\"file:///storage/emulated/0/Android/data/com.liux.musicplayer/Music/eg\"}]"));

        // Instantiate a ViewPager2 and a PagerAdapter.
        viewPager = findViewById(R.id.nav_host_fragment_activity_main);
        bottomNavigationView = findViewById(R.id.nav_view);
        pagerAdapter = new ScreenSlidePagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            /**
             * This method will be invoked when the current page is scrolled, either as part
             * of a programmatically initiated smooth scroll or a user initiated touch scroll.
             *
             * @param position             Position index of the first page currently being displayed.
             *                             Page position+1 will be visible if positionOffset is nonzero.
             * @param positionOffset       Value from [0, 1) indicating the offset from the page at position.
             * @param positionOffsetPixels Value in pixels indicating the offset from position.
             */
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels);
            }

            /**
             * This method will be invoked when a new page becomes selected. Animation is not
             * necessarily complete.
             *
             * @param position Position index of the new selected page.
             */
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                bottomNavigationView.getMenu().getItem(position).setChecked(true);
                switch (bottomNavigationView.getMenu().getItem(position).getItemId()) {
                    case R.id.navigation_home:
                    default:
                        TabTitle.setText(R.string.app_name);
                        PlayBarTitle.setVisibility(View.GONE);
                        playProgress.setVisibility(View.VISIBLE);
                        break;
                    case R.id.navigation_playlist:
                        TabTitle.setText(R.string.title_playlist);
                        PlayBarTitle.setVisibility(View.VISIBLE);
                        playProgress.setVisibility(View.GONE);
                        break;
                    case R.id.navigation_settings:
                        TabTitle.setText(R.string.title_settings);
                        PlayBarTitle.setVisibility(View.VISIBLE);
                        playProgress.setVisibility(View.GONE);
                        break;
                }
                // TODO:更新ToolBar标题栏
                //setToolBarTitle(position);
            }

            /**
             * Called when the scroll state changes. Useful for discovering when the user begins
             * dragging, when a fake drag is started, when the pager is automatically settling to the
             * current page, or when it is fully stopped/idle. .
             *
             * @param state
             */
            @Override
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);
            }
        });
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.navigation_home:
                    default:
                        TabTitle.setText(R.string.app_name);
                        PlayBarTitle.setVisibility(View.GONE);
                        playProgress.setVisibility(View.VISIBLE);
                        viewPager.setCurrentItem(0, false);
                        break;
                    case R.id.navigation_playlist:
                        TabTitle.setText(R.string.title_playlist);
                        PlayBarTitle.setVisibility(View.VISIBLE);
                        playProgress.setVisibility(View.GONE);
                        viewPager.setCurrentItem(1, false);
                        break;
                    case R.id.navigation_settings:
                        TabTitle.setText(R.string.title_settings);
                        PlayBarTitle.setVisibility(View.VISIBLE);
                        playProgress.setVisibility(View.GONE);
                        viewPager.setCurrentItem(2, false);
                        break;
                }
                return true;
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
        switch (musicPlayer.playThis(musicId)) {
            case 0:
                setPlayBarTitle(musicId);
                homeFragment.setMusicInfo(musicPlayer.getPlayList().get(musicId));
                PlayBarPause.setImageDrawable(getDrawable(R.drawable.ic_round_pause_circle_outline_24));
                break;
            default:
            case 1:
                AlertDialog alertInfoDialog = new AlertDialog.Builder(MainActivity.this)
                        .setTitle(R.string.play_error)
                        .setMessage(R.string.play_err_Info)
                        .setIcon(R.mipmap.ic_launcher)
                        .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        })
                        .create();
                alertInfoDialog.show();
                break;
        }
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

    @Override
    public void onBackPressed() {
        if (viewPager.getCurrentItem() == 0) {
            // If the user is currently looking at the first step, allow the system to handle the
            // Back button. This calls finish() on this activity and pops the back stack.
            super.onBackPressed();
        } else {
            // Otherwise, select the previous step.
            viewPager.setCurrentItem(viewPager.getCurrentItem() - 1);
        }
    }

    public void initHomeFragment() {
        setNowPlayThis(musicPlayer.getNowID());
        setPlayOrPause();
    }

    /**
     * A simple pager adapter that represents 5 ScreenSlidePageFragment objects, in
     * sequence.
     */
    private class ScreenSlidePagerAdapter extends FragmentStateAdapter {
        public ScreenSlidePagerAdapter(FragmentActivity fa) {
            super(fa);
        }

        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    return homeFragment;
                case 1:
                    return playlistFragment;
                case 2:
                default:
                    return settingsFragment;
            }
        }

        @Override
        public int getItemCount() {
            return NUM_PAGES;
        }
    }
}