package com.liux.musicplayer.ui;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.StrictMode;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceManager;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.navigation.NavigationBarView;
import com.liux.musicplayer.R;
import com.liux.musicplayer.databinding.ActivityMainBinding;
import com.liux.musicplayer.interfaces.MusicServiceCallback;
import com.liux.musicplayer.service.MusicService;
import com.liux.musicplayer.ui.home.HomeFragment;
import com.liux.musicplayer.ui.playlist.PlaylistFragment;
import com.liux.musicplayer.ui.settings.SettingsFragment;
import com.liux.musicplayer.utils.MusicUtils;

public class MainActivity extends FragmentActivity {

    private ActivityMainBinding binding = null;
    private SeekBar playProgressBar;
    private TextView PlayBarTitle;
    private TextView TabTitle;
    private ImageView PlayBarLyric;
    private ImageView PlayBarPause;
    private ImageView PlayBarOrder;
    private ImageView PlayBarPrev;
    private ImageView PlayBarNext;
    //private MusicPlayer musicPlayer;
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
    private ShapeableImageView shapeableImageView;
    private ShapeableImageView backImageView;
    private int lastPageId = 0;
    //是否进入后台
    private int countActivity = 0;
    private boolean isBackground = false;

    private MusicService musicService;
    private MusicConnector serviceConnection;
    private MusicServiceCallback musicServiceCallback = new MusicServiceCallback() {
        @Override
        public void nowPlayingThis(int musicID) {
            nowPlaying(musicID);
        }

        @Override
        public void playingErrorThis(int musicID) {
            playingError(musicID);
        }

        @Override
        public void updatePlayStateThis() {
            updatePlayState();
        }

        @Override
        public void nowLoadingThis(int musicId) {
            nowLoading(musicId);
        }
    };

    private void nowLoading(int musicId) {
        prepareInfo(musicId);
        PlayBarPause.setImageDrawable(getDrawable(R.drawable.ic_round_arrow_circle_down_24));
    }

    private final Handler progressHandler = new Handler(Looper.getMainLooper()) {
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
                    if (musicService.isPlaying()) {
                        int nowMillionSeconds = musicService.getCurrentPosition();
                        Message msg = new Message();
                        msg.what = 100;  //消息发送的标志
                        msg.obj = nowMillionSeconds; //消息发送的内容如：  Object String 类 int
                        progressHandler.sendMessage(msg);
                    }
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    private class MusicConnector implements ServiceConnection {
        //成功绑定时调用 即bindService（）执行成功同时返回非空Ibinder对象
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            musicService = ((MusicService.MyMusicBinder) iBinder).getService();
            Log.e("MusicConnector", "musicService" + musicService);
            musicService.setMusicServiceCallback(musicServiceCallback);
            initMainActivity();
        }

        //不成功绑定时调用
        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            musicService = null;
            Log.i("binding is fail", "binding is fail");
        }
    }

    private void initMainActivity() {
        musicService.setActivityForeground(true);
        initVariable();
        initViewCompat();
        initViewPager2();
        initBackgroundCallBack();
        setMainActivityData();
    }

    private void setMainActivityData() {
        setPlayOrder(musicService.getPlayOrder());
        setPlayBarTitle(musicService.getNowId());
        if (musicService.isPlaying()) {
            prepareInfo(musicService.getNowId());
            updatePlayState();
        } else if (musicService.isEnabled()) {
            //setHomeFragment();
            resetPlayProgress();
            int nowMillionSeconds = musicService.getCurrentPosition();
            playProgressBar.setProgress(nowMillionSeconds); //实时获取播放音乐的位置并且设置进度条的位置
            playProgressNowText.setText(nowMillionSeconds / 60000 + ((nowMillionSeconds / 1000 % 60 < 10) ? ":0" : ":") + nowMillionSeconds / 1000 % 60);
        }
    }

    private void initVariable() {
        lastPageId = 0;
        countActivity = 0;
        isBackground = false;
    }

    private void updatePlayState() {
        if (musicService.isPlaying()) {
            if (viewPager.getCurrentItem() == 0)
                homeFragment.startLyric();
            startProgressBar();
            PlayBarPause.setImageDrawable(getDrawable(R.drawable.ic_round_pause_circle_outline_24));
        } else {
            homeFragment.stopLyric();
            stopProgressBar();
            PlayBarPause.setImageDrawable(getDrawable(R.drawable.ic_round_play_circle_outline_24));
        }
    }

    private void nowPlaying(int musicId) {
        prepareInfo(musicId);
        //开启进度条跟踪线程
        updatePlayState();
    }

    private void prepareInfo(int musicId) {
        setPlayBarTitle(musicId);
        setChildFragment();
        playlistFragment.setNowPlaying(musicService.getNowId());
        //初始化进度条
        resetPlayProgress();
    }

    private void playingError(int musicId) {
        prepareInfo(musicId);
        AlertDialog alertInfoDialog = new AlertDialog.Builder(MainActivity.this)
                .setTitle(R.string.play_error)
                .setMessage(getString(R.string.play_err_Info))
                .setIcon(R.mipmap.ic_launcher)
                .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        updatePlayState();
                    }
                })
                .create();
        alertInfoDialog.show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        //允许在主线程连接网络
        //StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        //StrictMode.setThreadPolicy(policy);

        // Bind to LocalService
        serviceConnection = new MusicConnector();
        Intent intent = new Intent();
        intent.setClass(MainActivity.this, MusicService.class);
        startService(intent);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void initBackgroundCallBack() {
        registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle bundle) {

            }

            @Override
            public void onActivityStarted(Activity activity) {
                countActivity++;
                Log.e("MyApplication", "countActivity:" + countActivity + " isBack:" + String.valueOf(isBackground));
                if (countActivity == 0 && isBackground) {
                    Log.e("MyApplication", "onActivityStarted: 应用进入前台");
                    isBackground = false;
                    //说明应用重新进入了前台
                    //Toast.makeText(MainActivity.this, "应用进入前台", Toast.LENGTH_SHORT).show();
                    if (viewPager.getCurrentItem() == 0) {
                        startProgressBar();
                        homeFragment.startLyric();
                    }
                    musicService.setActivityForeground(true);
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
                Log.e("MyApplication", "countActivity:" + countActivity + " isBack:" + String.valueOf(isBackground));
                if (countActivity < 0 && !isBackground) {
                    Log.e("MyApplication", "onActivityStarted: 应用进入后台");
                    isBackground = true;
                    //说明应用进入了后台
                    //Toast.makeText(MainActivity.this, "应用进入后台", Toast.LENGTH_SHORT).show();
                    if (viewPager.getCurrentItem() == 0) {
                        stopProgressBar();
                        //homeFragment.stopLyric();
                    }
                    musicService.setActivityForeground(false);
                }
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {

            }

            @Override
            public void onActivityDestroyed(Activity activity) {

            }
        });
    }

    @Override
    public void onDestroy() {
        musicService.unregisterMusicServiceCallback(musicServiceCallback);
        unbindService(serviceConnection);
        homeFragment.onDestroy();
        //homeFragment.stopLyric();
        stopProgressBar();
        super.onDestroy();
    }

    public void setPlayOrPause() {
        //setPlayOrPause(!musicService.isPlaying());
        musicService.setPlayOrPause(!musicService.isPlaying());
    }

    public MusicService getMusicService() {
        if (musicService != null)
            return musicService;
        else
            throw new NullPointerException();
    }

    public void setPlayBarTitle(int musicId) {
        PlayBarTitle.setText(musicService.getPlayList().get(musicId).title + " - " + musicService.getPlayList().get(musicId).artist);
        Bitmap bitmap = musicService.getAlbumImage();
        if (bitmap == null) {   //获取图片失败，使用默认图片
            shapeableImageView.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_baseline_music_note_24));
            backImageView.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_baseline_music_note_24));
        } else {    //成功
            shapeableImageView.setImageBitmap(bitmap);
            backImageView.setImageBitmap(bitmap);
        }
    }

    public void playPrevOrNext(boolean isNext) {
        musicService.playPrevOrNext(isNext);
    }

    @Override
    public void onBackPressed() {
        if (viewPager.getCurrentItem() == 0) {
            // If the user is currently looking at the first step, allow the system to handle the
            // Back button. This calls finish() on this activity and pops the back stack.
            super.onBackPressed();
        } else if (viewPager.getCurrentItem() == 1 && playlistFragment.multipleChooseFlag) {
            playlistFragment.onBackPressed();
        } else {
            viewPager.setCurrentItem(viewPager.getCurrentItem() - 1);
        }
    }

    public void setChildFragment() {
        if (musicService != null) {
            homeFragment.setMusicInfo(musicService.getPlayList().get(musicService.getNowId()));
            setIsLyric(musicService.isAppLyric());
        }
    }

    private void initViewCompat() {
        playProgressBar = findViewById(R.id.seekBar);
        playProgressLayout = findViewById(R.id.playProgress);
        musicPlayingLayout = findViewById(R.id.musicPlayingLayout);
        playProgressNowText = findViewById(R.id.nowProgress);
        playProgressAllText = findViewById(R.id.allProgress);
        PlayBarTitle = findViewById(R.id.musicPlaying);
        TabTitle = findViewById(R.id.tabText);
        PlayBarLyric = findViewById(R.id.playLyric);
        PlayBarPause = findViewById(R.id.playPause);
        PlayBarOrder = findViewById(R.id.playOrder);
        PlayBarPrev = findViewById(R.id.playPrevious);
        PlayBarNext = findViewById(R.id.playNext);
        shapeableImageView = findViewById(R.id.playBarAlbumImage);
        backImageView = findViewById(R.id.backImageView);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        backImageView.setVisibility(prefs.getBoolean("isNewAppearance", false)
                ? View.VISIBLE
                : View.GONE);
        PlayBarLyric.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setIsLyric();
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
        findViewById(R.id.musicPlayingLayout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playlistFragment.setListViewPosition(musicService.getNowId());
            }
        });
        //根据音乐的时长设置进度条的最大进度
        playProgressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                stopProgressBar();
                //homeFragment.stopLyric();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                musicService.setProgress(seekBar.getProgress());
                //松开之后音乐跳转到相应位置
                playProgressNowText.setText(seekBar.getProgress() / 60000 + ((seekBar.getProgress() / 1000 % 60 < 10) ? ":0" : ":") + seekBar.getProgress() / 1000 % 60);
                startProgressBar();
                homeFragment.startLyric();
            }
        });
        resetPlayProgress();
    }

    public void setIsLyric() {
        setIsLyric(!musicService.isAppLyric());
    }

    public void setIsLyric(boolean isLyric) {
        if (!isLyric) {
            homeFragment.setIsLyricLayoutShow(false);
            PlayBarLyric.setImageDrawable(getDrawable(R.drawable.ic_baseline_subtitles_24));
            musicService.setAppLyric(false);
        } else {
            homeFragment.setIsLyricLayoutShow(true);
            PlayBarLyric.setImageDrawable(getDrawable(R.drawable.ic_baseline_subtitles_green_24));
            musicService.setAppLyric(true);
        }
    }

    private void setPlayOrder() {
        switch (musicService.getPlayOrder()) {
            case MusicService.LIST_PLAY:
                musicService.setPlayOrder(MusicService.REPEAT_LIST);
                PlayBarOrder.setImageDrawable(getDrawable(R.drawable.ic_round_repeat_24));
                Toast.makeText(this, R.string.repeat_list, Toast.LENGTH_SHORT).show();
                break;
            case MusicService.REPEAT_LIST:
                musicService.setPlayOrder(MusicService.REPEAT_ONE);
                PlayBarOrder.setImageDrawable(getDrawable(R.drawable.ic_round_repeat_one_24));
                Toast.makeText(this, R.string.repeat_one, Toast.LENGTH_SHORT).show();
                break;
            case MusicService.REPEAT_ONE:
                musicService.setPlayOrder(MusicService.SHUFFLE_PLAY);
                PlayBarOrder.setImageDrawable(getDrawable(R.drawable.ic_round_shuffle_24));
                Toast.makeText(this, R.string.shuffle_play, Toast.LENGTH_SHORT).show();
                break;
            default:
            case MusicService.SHUFFLE_PLAY:
                musicService.setPlayOrder(MusicService.LIST_PLAY);
                PlayBarOrder.setImageDrawable(getDrawable(R.drawable.ic_baseline_low_priority_24));
                Toast.makeText(this, R.string.list_play, Toast.LENGTH_SHORT).show();
                break;
        }
    }

    public void setPlayOrder(int playOrder) {
        switch (playOrder) {
            default:
            case MusicService.LIST_PLAY:
                PlayBarOrder.setImageDrawable(getDrawable(R.drawable.ic_baseline_low_priority_24));
                break;
            case MusicService.REPEAT_LIST:
                PlayBarOrder.setImageDrawable(getDrawable(R.drawable.ic_round_repeat_24));
                break;
            case MusicService.REPEAT_ONE:
                PlayBarOrder.setImageDrawable(getDrawable(R.drawable.ic_round_repeat_one_24));
                break;
            case MusicService.SHUFFLE_PLAY:
                PlayBarOrder.setImageDrawable(getDrawable(R.drawable.ic_round_shuffle_24));
                break;
        }
    }

    private void initViewPager2() {
        // Instantiate a ViewPager2 and a PagerAdapter.
        viewPager = findViewById(R.id.pager);
        bottomNavigationView = findViewById(R.id.nav_view);
        homeFragment = new HomeFragment();
        playlistFragment = new PlaylistFragment();
        settingsFragment = new SettingsFragment();
        pagerAdapter = new ScreenSlidePagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);
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
                Animation animation;
                switch (bottomNavigationView.getMenu().getItem(position).getItemId()) {
                    case R.id.navigation_home:
                    default:
                        TabTitle.setText(R.string.app_name);
                        animation = AnimationUtils.loadAnimation(MainActivity.this, R.anim.gradually_movedown_hide);
                        musicPlayingLayout.startAnimation(animation);
                        musicPlayingLayout.setVisibility(View.GONE);
                        animation = AnimationUtils.loadAnimation(MainActivity.this, R.anim.gradually_movedown_show);
                        playProgressLayout.setVisibility(View.VISIBLE);
                        playProgressLayout.startAnimation(animation);
                        startProgressBar();
                        homeFragment.startLyric();
                        musicService.setNowPageId(0);
                        break;
                    case R.id.navigation_playlist:
                        TabTitle.setText(R.string.title_playlist);
                        if (lastPageId == 0) {
                            animation = AnimationUtils.loadAnimation(MainActivity.this, R.anim.gradually_movedown_show);
                            musicPlayingLayout.setVisibility(View.VISIBLE);
                            musicPlayingLayout.startAnimation(animation);
                            animation = AnimationUtils.loadAnimation(MainActivity.this, R.anim.gradually_movedown_hide);
                            playProgressLayout.startAnimation(animation);
                            playProgressLayout.setVisibility(View.GONE);
                        }
                        stopProgressBar();
                        //homeFragment.stopLyric();
                        musicService.setNowPageId(1);
                        break;
                    case R.id.navigation_settings:
                        TabTitle.setText(R.string.title_settings);
                        if (lastPageId == 0) {
                            animation = AnimationUtils.loadAnimation(MainActivity.this, R.anim.gradually_movedown_show);
                            musicPlayingLayout.setVisibility(View.VISIBLE);
                            musicPlayingLayout.startAnimation(animation);
                            animation = AnimationUtils.loadAnimation(MainActivity.this, R.anim.gradually_movedown_hide);
                            playProgressLayout.startAnimation(animation);
                            playProgressLayout.setVisibility(View.GONE);
                        }
                        stopProgressBar();
                        //homeFragment.stopLyric();
                        musicService.setNowPageId(2);
                        break;
                }
                lastPageId = position;
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);
            }
        });
        viewPager.setCurrentItem(musicService.getNowPageId(), false);
        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.navigation_home:
                    default:
                        TabTitle.setText(R.string.app_name);
                        musicPlayingLayout.setVisibility(View.GONE);
                        playProgressLayout.setVisibility(View.VISIBLE);
                        setViewPagerToId(0);
                        startProgressBar();
                        homeFragment.startLyric();
                        musicService.setNowPageId(0);
                        break;
                    case R.id.navigation_playlist:
                        TabTitle.setText(R.string.title_playlist);
                        musicPlayingLayout.setVisibility(View.VISIBLE);
                        playProgressLayout.setVisibility(View.GONE);
                        setViewPagerToId(1);
                        stopProgressBar();
                        //homeFragment.stopLyric();
                        musicService.setNowPageId(1);
                        break;
                    case R.id.navigation_settings:
                        TabTitle.setText(R.string.title_settings);
                        musicPlayingLayout.setVisibility(View.VISIBLE);
                        playProgressLayout.setVisibility(View.GONE);
                        setViewPagerToId(2);
                        stopProgressBar();
                        //homeFragment.stopLyric();
                        musicService.setNowPageId(2);
                        break;
                }
                return true;
            }
        });
    }

    public void setViewPagerToId(int pageId) {
        viewPager.setCurrentItem(pageId, true);
    }

    private class ScreenSlidePagerAdapter extends FragmentStateAdapter {
        public ScreenSlidePagerAdapter(FragmentActivity fa) {
            super(fa);
        }

        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                default:
                    return homeFragment;
                case 1:
                    return playlistFragment;
                case 2:
                    settingsFragment = new SettingsFragment();
                    return settingsFragment;
            }
        }

        @Override
        public int getItemCount() {
            return NUM_PAGES;
        }
    }

    public void startProgressBar() {
        if (viewPager.getCurrentItem() == 0 && musicService.isPlaying()) {
            if (progressThread == null) {
                progressThread = new ProgressThread();
                progressThread.start();
            } else if (progressThread.isPaused()) {
                progressThread.resumeThread();
            }
        }
    }
    public void stopProgressBar() {
        if (progressThread != null && !progressThread.isPaused())
            progressThread.pauseThread();
    }

    public void resetPlayProgress() {
        int maxMillionSeconds = musicService.getDuration();
        playProgressBar.setMax(maxMillionSeconds);
        playProgressAllText.setText(maxMillionSeconds / 60000 + (((int) maxMillionSeconds / 1000 % 60 < 10) ? ":0" : ":") + maxMillionSeconds / 1000 % 60);
        playProgressNowText.setText("0:00");
        playProgressBar.setProgress(0);
        //setPlayOrPause(false);
    }


    public void setNewAppearance(boolean isTrue) {
        if (isTrue) {
            backImageView.setVisibility(View.VISIBLE);
        } else {
            backImageView.setVisibility(View.GONE);
        }
    }
}
