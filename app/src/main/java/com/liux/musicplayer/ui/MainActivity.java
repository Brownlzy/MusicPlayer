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
import com.liux.musicplayer.utils.CrashHandlers;

public class MainActivity extends FragmentActivity {

    private SeekBar playProgressBar;    //播放进度条
    private TextView PlayBarTitle;  //正在播放信息
    private TextView TabTitle;  //当前页信息
    private ImageView PlayBarLyric; //歌词开关
    private ImageView PlayBarPause; //播放暂停键
    private ImageView PlayBarOrder; //播放顺序键
    private static final int NUM_PAGES = 3; //总页数
    private HomeFragment homeFragment;  //主页
    private PlaylistFragment playlistFragment;  //播放列表页
    private SettingsFragment settingsFragment;  //设置页
    private ViewPager2 viewPager;   //用于fragment滑动切换
    private BottomNavigationView bottomNavigationView;  //底部导航栏
    private ProgressThread progressThread;  //用于更新进度条的子线程
    private LinearLayout playProgressLayout;    //进度条所在布局
    private LinearLayout musicPlayingLayout;    //正在播放信息所在布局
    private TextView playProgressNowText;   //播放进度显示文本
    private TextView playProgressAllText;   //总时长显示文本
    private ShapeableImageView shapeableImageView;  //专辑图片
    private ShapeableImageView backImageView;   //背景图片
    private int lastPageId = 0; //上页id
    //用于判断程序是否进入后台
    private int countActivity = 0;
    private boolean isBackground = false;

    private MusicService musicService;//音乐服务
    private MusicConnector serviceConnection;//和音乐服务的连接器
    /** 音乐服务的状态回调，详见{@link MusicServiceCallback} */
    private final MusicServiceCallback musicServiceCallback = new MusicServiceCallback() {
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
     /**
      * 修改正在缓冲的音乐信息
      * @param musicId
      */
    private void nowLoading(int musicId) {
        prepareInfo(musicId);
        PlayBarPause.setImageDrawable(getDrawable(R.drawable.ic_round_arrow_circle_down_24));
    }
/** 用于接受来自子线程的信息，并在主线程更新进度条 */
    private final Handler progressHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 100) {
                playProgressBar.setProgress((int) msg.obj); //实时获取播放音乐的位置并且设置进度条的位置
                playProgressNowText.setText((int) msg.obj / 60000 + (((int) msg.obj / 1000 % 60 < 10) ? ":0" : ":") + (int) msg.obj / 1000 % 60);
            }
        }
    };
 /**
  * 用于更新播放进度条的线程
  * @author         Brownlzt
  */
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
                        msg.obj = nowMillionSeconds; //消息发送的内容
                        progressHandler.sendMessage(msg);
                    }
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

 /**
  * 用于绑定音乐服务的连接器
  * @author         Brownlzy
  */
    private class MusicConnector implements ServiceConnection {
        //成功绑定时调用 即bindService（）执行成功同时返回非空Ibinder对象
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            musicService = ((MusicService.MyMusicBinder) iBinder).getService();
            Log.e("MusicConnector", "musicService" + musicService);
            //注册MusicServiceCallback
            musicService.setMusicServiceCallback(musicServiceCallback);
            //连接服务成功，初始化mainActivity
            initMainActivity();
            if (viewPager.getCurrentItem() == 1)    //如果当前页为播放列表，初始化列表数据
                playlistFragment.initData();
        }

        //不成功绑定时调用
        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            musicService = null;
            Log.i("binding is fail", "binding is fail");
        }
    }
     /**
      * 初始化MainActivity
      */
    private void initMainActivity() {
        //声明当前位于前台（方便桌面歌词隐藏和显示）
        musicService.setActivityForeground(true);
        initViewCompat();
        initViewPager2();
        initBackgroundCallBack();
        initMainActivityData();
        SharedPreferences sp=PreferenceManager.getDefaultSharedPreferences(this);
        //首次启动显示教程
        if(sp.getBoolean("isFirstStart",true)){
            Intent intent=new Intent(MainActivity.this,UserGuide.class);
            startActivity(intent);
        }
    }
 /**
  * 初始化MainActivity数据
  */
    private void initMainActivityData() {
        setPlayOrder(musicService.getPlayOrder());
        setPlayBarTitle(musicService.getNowId());
        if (musicService.isPlaying()) {
            prepareInfo(musicService.getNowId());
            updatePlayState();
        } else if (musicService.isEnabled()) {
            resetPlayProgress();
            int nowMillionSeconds = musicService.getCurrentPosition();
            playProgressBar.setProgress(nowMillionSeconds); //实时获取播放音乐的位置并且设置进度条的位置
            playProgressNowText.setText(nowMillionSeconds / 60000 + ((nowMillionSeconds / 1000 % 60 < 10) ? ":0" : ":") + nowMillionSeconds / 1000 % 60);
        }
    }
 /**
  * 更新播放状态
  */
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
 /**
  * 修改正在播放的音乐信息
  * @param musicId 正在播放的id
  */
    private void nowPlaying(int musicId) {
        prepareInfo(musicId);
        //开启进度条跟踪线程
        updatePlayState();
    }
    /**
     * 根据id设置音乐信息
     * @param musicId 音乐的id
     */
    private void prepareInfo(int musicId) {
        setPlayBarTitle(musicId);
        setChildFragment();
        playlistFragment.setNowPlaying(musicService.getNowId());
        //初始化进度条
        resetPlayProgress();
    }
    /**
     * 修改播放出错的音乐信息
     * @param musicId 正在播放的id
     */
    private void playingError(int musicId) {
        prepareInfo(musicId);
        //弹窗告知
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
        //初始化错误日志生成
        CrashHandlers crashHandlers = CrashHandlers.getInstance();
        crashHandlers.init(this);
        com.liux.musicplayer.databinding.ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        // Bind to MusicService
        serviceConnection = new MusicConnector();
        Intent intent = new Intent();
        intent.setClass(MainActivity.this, MusicService.class);
        startService(intent);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStart() {
        super.onStart();
        CrashHandlers.checkIfExistsLastCrash(this);
    }
 /**
  * 初始化后台状态回调
  */
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
                    if (viewPager.getCurrentItem() == 0) {
                        stopProgressBar();
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
        stopProgressBar();
        super.onDestroy();
    }
 /**
  * 切换播放状态
  */
    public void setPlayOrPause() {
        musicService.setPlayOrPause(!musicService.isPlaying());
    }
 /**
  * 获取MainActivity绑定的{@link MusicService}
  * @return MusicService
  */
    public MusicService getMusicService() {
        if (musicService != null)
            return musicService;
        else
            return null;
    }
 /**
  * 设置正在播放信息栏
  * @param musicId 正在播放的音乐id
  */
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
 /**
  * 上一首或下一首
  * @param isNext true-下一首 false-上一首
  */
    public void playPrevOrNext(boolean isNext) {
        musicService.playPrevOrNext(isNext);
    }
 /**
  * 返回键
  */
    @Override
    public void onBackPressed() {
        if (viewPager.getCurrentItem() == 0) {//当前页为主页
            super.onBackPressed();//退出
        } else if (viewPager.getCurrentItem() == 1 && playlistFragment.multipleChooseFlag) {//当前页在播放列表且处于多选状态
            playlistFragment.onBackPressed();//让播放列表执行返回操作
        } else {
            viewPager.setCurrentItem(viewPager.getCurrentItem() - 1);//页面id-1
        }
    }
 /**
  * 设置子窗体信息
  */
    public void setChildFragment() {
        if (musicService != null) {
            homeFragment.setMusicInfo(musicService.getPlayList().get(musicService.getNowId()));
            setIsLyric(musicService.isAppLyric());
        }
    }

    private void initViewCompat() {
        //绑定控件
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
        ImageView playBarPrev = findViewById(R.id.playPrevious);
        ImageView playBarNext = findViewById(R.id.playNext);
        shapeableImageView = findViewById(R.id.playBarAlbumImage);
        backImageView = findViewById(R.id.backImageView);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //根据外观设置决定是否显示
        backImageView.setVisibility(prefs.getBoolean("isNewAppearance", false)
                ? View.VISIBLE
                : View.GONE);
        //设置点击监听器
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
        playBarPrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playPrevOrNext(false);
            }
        });
        playBarNext.setOnClickListener(new View.OnClickListener() {
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
        //设置进度条拖动监听器
        playProgressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                //手指在进度条上时暂停进度条更新
                stopProgressBar();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                //手指松开之后音乐跳转到相应位置
                musicService.setProgress(seekBar.getProgress());
                playProgressNowText.setText(seekBar.getProgress() / 60000 + ((seekBar.getProgress() / 1000 % 60 < 10) ? ":0" : ":") + seekBar.getProgress() / 1000 % 60);
                //恢复进度条更新
                startProgressBar();
                homeFragment.startLyric();
            }
        });
        //初始化进度条
        resetPlayProgress();
    }
 /**
  * 切换歌词显示
  */
    public void setIsLyric() {
        setIsLyric(!musicService.isAppLyric());
    }
 /**
  * 设置是否显示歌词
  * @param isLyric 是否显示歌词
  */
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
 /**
  * 切换播放顺序
  */
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
 /**
  * 设置播放顺序图标
  * @param playOrder 新的播放顺序
  */
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
        FragmentStateAdapter pagerAdapter = new ScreenSlidePagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);
        viewPager.setSaveEnabled(false);
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels);
            }
             /**
              * 页面被选中
              * @param position 被选中的id
              */
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                bottomNavigationView.getMenu().getItem(position).setChecked(true);
                Animation animation;
                switch (bottomNavigationView.getMenu().getItem(position).getItemId()) {
                    case R.id.navigation_home:
                    default:
                        TabTitle.setText(R.string.app_name);//修改页眉
                        //进度条和播放信息的切换动画
                        animation = AnimationUtils.loadAnimation(MainActivity.this, R.anim.gradually_movedown_hide);
                        musicPlayingLayout.startAnimation(animation);
                        musicPlayingLayout.setVisibility(View.GONE);
                        animation = AnimationUtils.loadAnimation(MainActivity.this, R.anim.gradually_movedown_show);
                        playProgressLayout.setVisibility(View.VISIBLE);
                        playProgressLayout.startAnimation(animation);
                        //启动进度条和歌词更新
                        startProgressBar();
                        homeFragment.startLyric();
                        musicService.setNowPageId(0);//保存
                        break;
                    case R.id.navigation_playlist:
                        TabTitle.setText(R.string.title_playlist);
                        if (lastPageId == 0) {
                            animation = AnimationUtils.loadAnimation(MainActivity.this, R.anim.gradually_movedown_show);
                            //进度条和播放信息的切换动画
                            musicPlayingLayout.setVisibility(View.VISIBLE);
                            musicPlayingLayout.startAnimation(animation);
                            animation = AnimationUtils.loadAnimation(MainActivity.this, R.anim.gradually_movedown_hide);
                            playProgressLayout.startAnimation(animation);
                            playProgressLayout.setVisibility(View.GONE);
                        }
                        //暂停进度条更新
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
                        //暂停进度条更新
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
        //从MusicServer读取上次页面id并恢复
        viewPager.setCurrentItem(musicService.getNowPageId(), false);
        //设置底部导航栏选择事件
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
 /**
  * 切换至页面
  * @param pageId 页面id
  */
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
                    homeFragment = new HomeFragment();
                    return homeFragment;
                case 1:
                    playlistFragment = new PlaylistFragment();
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
 /**
  * 开始进度条更新
  */
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
    /**
     * 暂停进度条更新
     */
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

 /**
  * 是否使用新外观
  * @param isTrue
  */
    public void setNewAppearance(boolean isTrue) {
        if (isTrue) {
            backImageView.setVisibility(View.VISIBLE);
        } else {
            backImageView.setVisibility(View.GONE);
        }
    }
}
