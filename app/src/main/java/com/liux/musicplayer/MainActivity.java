package com.liux.musicplayer;

import android.app.Activity;
import android.app.Application;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.method.ScrollingMovementMethod;
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
    //用于更新进度条和时间的Handler，用于progressThread调用（非主线程不能更新UI，要通过这种方式让其他线程上的程序发送消息给主线程，再由主线程更新）
    private Handler progressHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 100) {
                playProgressBar.setProgress((int) msg.obj); //实时获取播放音乐的位置并且设置进度条的位置
                playProgressNowText.setText((int) msg.obj / 60000 + (((int) msg.obj / 1000 % 60 < 10) ? ":0" : ":") + (int) msg.obj / 1000 % 60);
            }
        }
    };

    //新线程，用于读取（1Hz）播放进度并通知Handler
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
                    //判断播放器类MediaPlayer是否在播放，没在播放就不用发消息了，理论上没在播放这个Thread就不应该还在运行
                    if (musicPlayer.getMediaPlayer().isPlaying()) {
                        int nowMillionSeconds = musicPlayer.getMediaPlayer().getCurrentPosition();
                        Message msg = new Message();//构造消息
                        msg.what = 100;  //消息发送的标志
                        msg.obj = nowMillionSeconds; //消息发送的内容如：  Object String 类 int
                        progressHandler.sendMessage(msg);//交给Handler
                    }
                } catch (InterruptedException e) {//有sleep的必须要catch这个
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
        //具体见Activity的生命周期，一般在此用findViewById绑定控件和类内成员变量
        //初始化控件（绑定+设置）
        initViewCompat();
        //这个是判断应用是否在前台的，不在前台就可以关闭很多循环线程比如进度条更新
        initBackgroundCallBack();
    }

    private void initBackgroundCallBack() {
        registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            //这里有各种生命周期的状态
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
        //无参的播放或暂停，用于播放栏的按钮，根据MediaPlayer状态确定要播放还是暂停
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
        //有参的
        if (isPlay) {
            musicPlayer.start();
            PlayBarPause.setImageDrawable(getDrawable(R.drawable.ic_round_pause_circle_outline_24));
        } else {
            musicPlayer.pause();
            PlayBarPause.setImageDrawable(getDrawable(R.drawable.ic_round_play_circle_outline_24));
        }
    }

    //获取成员变量
    public MusicPlayer getMusicPlayer() {
        return musicPlayer;
    }

    //设置播放栏的正在播放
    public void setPlayBarTitle(int musicId) {
        PlayBarTitle.setText(musicPlayer.getPlayList().get(musicId).title + " - " + musicPlayer.getPlayList().get(musicId).artist);
    }

    //给播放栏的上一首或下一首
    public void playPrevOrNext(boolean isNext) {
        musicPlayer.playPrevOrNext(isNext);
    }

    @Override
    public void onBackPressed() {
        //监听返回键
        //viewPager内有3个fragment（home0，playlist1，setting2）
        if (viewPager.getCurrentItem() == 0) {
            // If the user is currently looking at the first step, allow the system to handle the
            // Back button. This calls finish() on this activity and pops the back stack.
            super.onBackPressed();
        } else {//因为播放列表页有个编辑，当编辑栏存在时要把返回事件传递给它，让它把编辑状态取消
            if (viewPager.getCurrentItem() == 1 && playlistFragment.onBackPressed() != 1
                    || viewPager.getCurrentItem() == 2) {
                // Otherwise, select the previous step.
                //不在home页时viewpage向前翻页，即当前id-1
                viewPager.setCurrentItem(viewPager.getCurrentItem() - 1);
            }
        }
    }

    //这是用来让home页告诉mainActivity它准备好接受数据显示了，初始用，后续切歌换信息不通过这个方法
    public void initHomeFragment() {
        musicPlayer.playThisNow(musicPlayer.getNowId());
        setPlayOrPause();
    }

    //设置主页信息，供MusicPlayer调用
    public void setHomeFragment() {
        homeFragment.setMusicInfo(musicPlayer.getPlayList().get(musicPlayer.getNowId()));
    }

    //初始化进度条
    private void initProgress() {
        //根据音乐的时长设置进度条的最大进度，并开始间隔一秒刷新的线程
        playProgressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            }

            //当手碰到进度条，暂停刷新进度条
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                stopProgressBar();
            }

            //当手离开进度条
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                //松开之后音乐跳转到相应位置
                musicPlayer.getMediaPlayer().seekTo(seekBar.getProgress());
                playProgressNowText.setText(seekBar.getProgress() / 60000 + ((seekBar.getProgress() / 1000 % 60 < 10) ? ":0" : ":") + seekBar.getProgress() / 1000 % 60);
                //重新开始刷新
                startProgressBar();
            }
        });
    }

    private void initViewCompat() {
        //初始化，绑定控件+设置初始值
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
        //读取设置
        SharedPreferences sp = getSharedPreferences("com.liux.musicplayer_preferences", Activity.MODE_PRIVATE);
        musicPlayer = new MusicPlayer(MainActivity.this, MainActivity.this);
        //初始化viewpager
        initViewPager2();
        //各种按钮的监听器
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
        //初始化进度条
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
        viewPager.setSaveEnabled(true);
        //设置滑动的监听器
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels);
            }

            //根据划到的page设置顶栏和播放栏
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
        //设置点击监听，点哪个到哪一页，已弃用
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

        //创建新页面，前面初始化已经创建好了，这里直接返回给它
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

        //返回页面数，这里的常量是3
        @Override
        public int getItemCount() {
            return NUM_PAGES;
        }
    }

    public void startProgressBar() {
        //如果线程还没有构造就构造一个，有了就继续线程
        if (progressThread == null) {
            progressThread = new MainActivity.ProgressThread();
            progressThread.start();
        } else if (progressThread.isPaused()) {
            progressThread.resumeThread();
        }
    }

    public void stopProgressBar() {
        //暂停已有线程
        if (progressThread != null && !progressThread.isPaused())
            progressThread.pauseThread();
    }

    public void resetPlayProgress() {
        //初始化进度条
        int maxMillionSeconds = musicPlayer.getMediaPlayer().getDuration();
        playProgressBar.setMax(maxMillionSeconds);
        playProgressAllText.setText(maxMillionSeconds / 60000 + (((int) maxMillionSeconds / 1000 % 60 < 10) ? ":0" : ":") + maxMillionSeconds / 1000 % 60);
        playProgressNowText.setText("0:00");
        playProgressBar.setProgress(0);
        setPlayOrPause(false);
    }

}
