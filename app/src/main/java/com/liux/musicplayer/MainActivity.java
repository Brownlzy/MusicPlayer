package com.liux.musicplayer;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.liux.musicplayer.databinding.ActivityMainBinding;
import com.liux.musicplayer.ui.home.HomeFragment;
import com.liux.musicplayer.ui.playlist.PlaylistFragment;
import com.liux.musicplayer.ui.settings.SettingsFragment;

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
    private static final int NUM_PAGES = 3;
    private HomeFragment homeFragment;
    private PlaylistFragment playlistFragment;
    private SettingsFragment settingsFragment;
    private ViewPager2 viewPager;
    private BottomNavigationView bottomNavigationView;
    private FragmentStateAdapter pagerAdapter;
    private ProgressThread progressThread;

    private class ProgressThread extends Thread {
        private final Object lock = new Object();
        private boolean pause = false;
        //调用这个方法实现暂停线程
        void pauseThread() {
            pause = true;
        }

        //调用这个方法实现恢复线程的运行
        void resumeThread() {
            pause = false;
            synchronized (lock) {
                lock.notifyAll();
            }
        }

        //注意：这个方法只能在run方法里调用，不然会阻塞主线程，导致页面无响应
        void onPause() {
            synchronized (lock) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        @Override
        public void run() {
            super.run();
            int index = 0;
            while (true) {
                // 让线程处于暂停等待状态
                while (pause) {
                    onPause();
                }
                if (musicPlayer.getMediaPlayer().isPlaying()) {
                    playProgress.setProgress(musicPlayer.getMediaPlayer().getCurrentPosition()); //实时获取播放音乐的位置并且设置进度条的位置
                }
            }
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initViewCompat();
    }

    public void setPlayOrPause() {
        if (musicPlayer.isPlaying()) {
            musicPlayer.pause();
            PlayBarPause.setImageDrawable(getDrawable(R.drawable.ic_round_play_circle_outline_24));
        } else {
            musicPlayer.start();
            PlayBarPause.setImageDrawable(getDrawable(R.drawable.ic_round_pause_circle_outline_24));
        }
    }

    public void setPlayOrPause(boolean isPlay) {
        if (isPlay) {
            musicPlayer.start();
            PlayBarPause.setImageDrawable(getDrawable(R.drawable.ic_round_pause_circle_outline_24));
        } else {
            musicPlayer.pause();
            PlayBarPause.setImageDrawable(getDrawable(R.drawable.ic_round_play_circle_outline_24));
        }
    }

    public MusicPlayer getMusicPlayer() {
        return musicPlayer;
    }

    public void setPlayBarTitle(int musicId) {
        PlayBarTitle.setText(musicPlayer.getPlayList().get(musicId).title + " - " + musicPlayer.getPlayList().get(musicId).artist);
    }

    public void playPrevOrNext(boolean isNext) {
        musicPlayer.playPrevOrNext(isNext);
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
        musicPlayer.playThisNow(musicPlayer.getNowId());
        setPlayOrPause();
    }

    public void setHomeFragment() {
        homeFragment.setMusicInfo(musicPlayer.getPlayList().get(musicPlayer.getNowId()));
    }

    public void initProgress() {
        //根据音乐的时长设置进度条的最大进度
        playProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                stopProgressBar();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                musicPlayer.getMediaPlayer().seekTo(seekBar.getProgress());
                //松开之后音乐跳转到相应位置
                startProgressBar();
            }
        });
    }

    public void initViewCompat() {
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
        musicPlayer = new MusicPlayer(MainActivity.this, MainActivity.this);

        initViewPager2();

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
        initProgress();
    }

    private void initViewPager2() {
        // Instantiate a ViewPager2 and a PagerAdapter.
        viewPager = findViewById(R.id.pager);
        bottomNavigationView = findViewById(R.id.nav_view);
        pagerAdapter = new ScreenSlidePagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels);
            }
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
            }
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
    }

    public void setViewPagerToId(int pageId) {
        viewPager.setCurrentItem(pageId, false);
    }

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

    public void startProgressBar() {
        if (progressThread == null) {
            progressThread = new MainActivity.ProgressThread();
            progressThread.start();
        } else {
            progressThread.resumeThread();
        }
    }

    public void stopProgressBar() {
        if (progressThread != null) progressThread.pauseThread();
    }

    public void resetPlayProgress() {
        playProgress.setMax(musicPlayer.getMediaPlayer().getDuration());
        playProgress.setProgress(0);
        setPlayOrPause(false);
    }

}
