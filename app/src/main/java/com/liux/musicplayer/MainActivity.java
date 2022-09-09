package com.liux.musicplayer;

import android.app.Activity;
import android.app.Application;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

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
    private SeekBar playProgressBar;
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
    private LinearLayout playProgressLayout;
    private LinearLayout musicPlayingLayout;
    private TextView playProgressNowText;
    private TextView playProgressAllText;
    //是否进入后台
    private int countActivity = 0;
    private boolean isBackground = false;

    private Handler progressHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 100) {
                playProgressBar.setProgress((int) msg.obj); //实时获取播放音乐的位置并且设置进度条的位置
                playProgressNowText.setText((int) msg.obj / 60000 + (((int) msg.obj / 1000 % 60 < 10) ? ":0" : ":") + (int) msg.obj / 1000 % 60);
            }
        }
    };

    private class ProgressThread extends Thread {
        private final Object lock = new Object();
        private boolean pause = false;

        //调用这个方法实现暂停线程
        void pauseThread() {
            pause = true;
        }

        boolean isPaused() {
            return pause;
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
                try {
                    Thread.sleep(1000);
                    if (musicPlayer.getMediaPlayer().isPlaying()) {
                        int nowMillionSeconds = musicPlayer.getMediaPlayer().getCurrentPosition();
                        Message msg = new Message();
                        msg.what = 100;  //消息发送的标志
                        msg.obj = nowMillionSeconds; //消息发送的内容如：  Object String 类 int
                        progressHandler.sendMessage(msg);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
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
        initBackgroundCallBack();
    }

    private void initBackgroundCallBack() {
        registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle bundle) {

            }

            @Override
            public void onActivityStarted(Activity activity) {
                countActivity++;
                if (countActivity == 1 && isBackground) {
                    Log.e("MyApplication", "onActivityStarted: 应用进入前台");
                    isBackground = false;
                    //说明应用重新进入了前台
                    //Toast.makeText(MainActivity.this, "应用进入前台", Toast.LENGTH_SHORT).show();
                    if (viewPager.getCurrentItem() == 0)
                        startProgressBar();
                }

            }

            @Override
            public void onActivityResumed(Activity activity) {

            }

            @Override
            public void onActivityPaused(Activity activity) {

            }

            @Override
            public void onActivityStopped(Activity activity) {
                countActivity--;
                if (countActivity <= 0 && !isBackground) {
                    Log.e("MyApplication", "onActivityStarted: 应用进入后台");
                    isBackground = true;
                    //说明应用进入了后台
                    //Toast.makeText(MainActivity.this, "应用进入后台", Toast.LENGTH_SHORT).show();
                }
                if (viewPager.getCurrentItem() == 0)
                    startProgressBar();
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {

            }

            @Override
            public void onActivityDestroyed(Activity activity) {

            }
        });
    }

    public void setPlayOrPause() {
        if (musicPlayer.isPlaying()) {
            musicPlayer.pause();
            PlayBarPause.setImageDrawable(getDrawable(R.drawable.ic_round_play_circle_outline_24));
            stopProgressBar();
        } else {
            musicPlayer.start();
            PlayBarPause.setImageDrawable(getDrawable(R.drawable.ic_round_pause_circle_outline_24));
            startProgressBar();
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

    private void initProgress() {
        //根据音乐的时长设置进度条的最大进度
        playProgressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
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
                playProgressNowText.setText(seekBar.getProgress() / 60000 + ((seekBar.getProgress() / 1000 % 60 < 10) ? ":0" : ":") + seekBar.getProgress() / 1000 % 60);
                startProgressBar();
            }
        });
    }

    private void initViewCompat() {
        //BottomNavigationView navView = findViewById(R.id.nav_view);
        playProgressBar = findViewById(R.id.seekBar);
        playProgressLayout = findViewById(R.id.playProgress);
        musicPlayingLayout = findViewById(R.id.musicPlayingLayout);
        playProgressNowText = findViewById(R.id.nowProgress);
        playProgressAllText = findViewById(R.id.allProgress);
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
                setPlayOrder();
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

    private void setPlayOrder() {
        switch (musicPlayer.getPlayOrder()) {
            case MusicPlayer.LIST_PLAY:
                musicPlayer.setPlayOrder(MusicPlayer.REPEAT_LIST);
                PlayBarOrder.setImageDrawable(getDrawable(R.drawable.ic_round_repeat_24));
                Toast.makeText(this, R.string.repeat_list, Toast.LENGTH_SHORT).show();
                break;
            case MusicPlayer.REPEAT_LIST:
                musicPlayer.setPlayOrder(MusicPlayer.REPEAT_ONE);
                PlayBarOrder.setImageDrawable(getDrawable(R.drawable.ic_round_repeat_one_24));
                Toast.makeText(this, R.string.repeat_one, Toast.LENGTH_SHORT).show();
                break;
            case MusicPlayer.REPEAT_ONE:
                musicPlayer.setPlayOrder(MusicPlayer.SHUFFLE_PLAY);
                PlayBarOrder.setImageDrawable(getDrawable(R.drawable.ic_round_shuffle_24));
                Toast.makeText(this, R.string.shuffle_play, Toast.LENGTH_SHORT).show();
                break;
            default:
            case MusicPlayer.SHUFFLE_PLAY:
                musicPlayer.setPlayOrder(MusicPlayer.LIST_PLAY);
                PlayBarOrder.setImageDrawable(getDrawable(R.drawable.ic_baseline_low_priority_24));
                Toast.makeText(this, R.string.list_play, Toast.LENGTH_SHORT).show();
                break;
        }
    }

    public void setPlayOrder(int playOrder) {
        switch (playOrder) {
            default:
            case MusicPlayer.LIST_PLAY:
                PlayBarOrder.setImageDrawable(getDrawable(R.drawable.ic_baseline_low_priority_24));
                break;
            case MusicPlayer.REPEAT_LIST:
                PlayBarOrder.setImageDrawable(getDrawable(R.drawable.ic_round_repeat_24));
                break;
            case MusicPlayer.REPEAT_ONE:
                PlayBarOrder.setImageDrawable(getDrawable(R.drawable.ic_round_repeat_one_24));
                break;
            case MusicPlayer.SHUFFLE_PLAY:
                PlayBarOrder.setImageDrawable(getDrawable(R.drawable.ic_round_shuffle_24));
                break;
        }
    }

    private void initViewPager2() {
        // Instantiate a ViewPager2 and a PagerAdapter.
        viewPager = findViewById(R.id.pager);
        bottomNavigationView = findViewById(R.id.nav_view);
        pagerAdapter = new ScreenSlidePagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);
        // 重点是这一句
        viewPager.setSaveEnabled(false);
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
                        musicPlayingLayout.setVisibility(View.GONE);
                        playProgressLayout.setVisibility(View.VISIBLE);
                        startProgressBar();
                        break;
                    case R.id.navigation_playlist:
                        TabTitle.setText(R.string.title_playlist);
                        musicPlayingLayout.setVisibility(View.VISIBLE);
                        playProgressLayout.setVisibility(View.GONE);
                        stopProgressBar();
                        break;
                    case R.id.navigation_settings:
                        TabTitle.setText(R.string.title_settings);
                        musicPlayingLayout.setVisibility(View.VISIBLE);
                        playProgressLayout.setVisibility(View.GONE);
                        stopProgressBar();
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
                        musicPlayingLayout.setVisibility(View.GONE);
                        playProgressLayout.setVisibility(View.VISIBLE);
                        viewPager.setCurrentItem(0, false);
                        startProgressBar();
                        break;
                    case R.id.navigation_playlist:
                        TabTitle.setText(R.string.title_playlist);
                        musicPlayingLayout.setVisibility(View.VISIBLE);
                        playProgressLayout.setVisibility(View.GONE);
                        viewPager.setCurrentItem(1, false);
                        stopProgressBar();
                        break;
                    case R.id.navigation_settings:
                        TabTitle.setText(R.string.title_settings);
                        musicPlayingLayout.setVisibility(View.VISIBLE);
                        playProgressLayout.setVisibility(View.GONE);
                        viewPager.setCurrentItem(2, false);
                        stopProgressBar();
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
        } else if (progressThread.isPaused()) {
            progressThread.resumeThread();
        }
    }

    public void stopProgressBar() {
        if (progressThread != null && !progressThread.isPaused())
            progressThread.pauseThread();
    }

    public void resetPlayProgress() {
        int maxMillionSeconds = musicPlayer.getMediaPlayer().getDuration();
        playProgressBar.setMax(maxMillionSeconds);
        playProgressAllText.setText(maxMillionSeconds / 60000 + (((int) maxMillionSeconds / 1000 % 60 < 10) ? ":0" : ":") + maxMillionSeconds / 1000 % 60);
        playProgressNowText.setText("0:00");
        playProgressBar.setProgress(0);
        setPlayOrPause(false);
    }

}
