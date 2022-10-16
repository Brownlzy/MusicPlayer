package com.liux.musicplayer.activities;

import static com.liux.musicplayer.services.MusicService.LIST_PLAY;
import static com.liux.musicplayer.services.MusicService.REPEAT_LIST;
import static com.liux.musicplayer.services.MusicService.REPEAT_ONE;
import static com.liux.musicplayer.services.MusicService.SHUFFLE_PLAY;

import android.app.Activity;
import android.app.Application;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.navigation.NavigationBarView;
import com.liux.musicplayer.R;
import com.liux.musicplayer.adapters.PlayingListAdapter;
import com.liux.musicplayer.databinding.ActivityMainBinding;
import com.liux.musicplayer.media.MusicLibrary;
import com.liux.musicplayer.media.SimpleMusicService;
import com.liux.musicplayer.models.Song;
import com.liux.musicplayer.ui.HomeFragment;
import com.liux.musicplayer.ui.SongListFragment;
import com.liux.musicplayer.ui.SettingsFragment;
import com.liux.musicplayer.utils.CrashHandlers;
import com.liux.musicplayer.utils.SharedPrefs;
import com.liux.musicplayer.viewmodels.MyViewModel;

import java.util.List;

public class MainActivity extends FragmentActivity {

    public static MainActivity mainActivity;
    private MyViewModel myViewModel;
    private ActivityMainBinding binding = null;
    private SeekBar playProgressBar;
    private TextView playBarTitle;
    private TextView TabTitle;
    private TextView TotalCount;
    private ImageView playButton;
    private ImageView PlayBarOrder;
    private ImageView playBarPrev;
    private ImageView playBarNext;
    private ImageView playBarPlayingList;
    //private MusicPlayer musicPlayer;
    private static final int NUM_PAGES = 3;
    private HomeFragment homeFragment;
    private SongListFragment songListFragment;
    private SettingsFragment settingsFragment;
    private ViewPager2 viewPager;
    private BottomNavigationView bottomNavigationView;
    private FragmentStateAdapter pagerAdapter;
    private LinearLayout playProgressLayout;
    private LinearLayout musicPlayingLayout;
    private RelativeLayout playingListLayout;
    private ListView playingList;
    private TextView playProgressNowText;
    private TextView playProgressAllText;
    private ProgressThread progressThread;
    private ShapeableImageView shapeableImageView;
    private ShapeableImageView backImageView;
    private boolean isAlreadyShowPlayBarTitle = false;
    private boolean isSplash = true;
    //是否进入后台
    private int countActivity = 0;
    private boolean isBackground = false;

    //private MusicService musicService;
    private boolean isPlayingListShowing = false;
    private PlayingListAdapter adapter;
    private MaterialCardView splashCard;
    private boolean isPlayList = false;
    private boolean isHome = false;

    private void nowLoading(int musicId) {
        prepareInfo(musicId);
        playButton.setImageDrawable(getDrawable(R.drawable.ic_round_arrow_circle_down_24));
    }

    public void initMainActivity() {
        MyViewModel.setActivityForeground(true);
        initVariable();
        initViewCompat();
        initViewPager2();
        initBackgroundCallBack();
        initObserver();
        //setMainActivityData();
    }

    private PlayingListAdapter.RefreshListener refreshListener = new PlayingListAdapter.RefreshListener() {
        @Override
        public void deleteThis(MediaDescriptionCompat description) {
            myViewModel.getmMediaController().removeQueueItem(description);
        }

        @Override
        public void skipToThis(long id) {
            myViewModel.getmMediaController().getTransportControls().skipToQueueItem(id);
        }
    };

    private void initObserver() {
        //监视正在播放列表
        myViewModel.getQueueItemsMutableLiveData().observe(this, new Observer<List<MediaSessionCompat.QueueItem>>() {
            @Override
            public void onChanged(List<MediaSessionCompat.QueueItem> queueItems) {
                int listPosition;
                int listPositionY;
                try {
                    listPosition = playingList.getFirstVisiblePosition();
                    listPositionY = playingList.getChildAt(0).getTop();
                } catch (NullPointerException e) {
                    e.printStackTrace();
                    listPosition = -1;
                    listPositionY = 0;
                }
                TotalCount.setText(getString(R.string.totalCount).replace("%d", String.valueOf(queueItems.size())));
                adapter = new PlayingListAdapter(MainActivity.this, queueItems, refreshListener);
                playingList.setAdapter(adapter);
                if (myViewModel.getmMediaController().getMetadata() != null)
                    adapter.setNowPlay(myViewModel.getmMediaController().getMetadata().getDescription().getMediaUri().getPath());
                if (listPosition != -1)
                    playingList.setSelectionFromTop(listPosition, listPositionY);

            }
        });
        //监视正在播放的歌曲
        myViewModel.getNowPlaying().observe(this, new Observer<Song>() {
            @Override
            public void onChanged(Song song) {
                setPlayBarTitle(song);
                Log.e("Main", "duration" + song.getSongDuration());
                setSeekBarMax(song.getSongDuration());
                if (myViewModel.getmMediaController().getPlaybackState() != null)
                    setSeekBarDuration(myViewModel.getmMediaController().getPlaybackState().getPosition());
                else
                    setSeekBarDuration(0L);
                setPlayingListView(song);
                startProgressBar();
            }
        });
        //监视播放进度
        myViewModel.getCurrentPlayingDuration().observe(this, new Observer<Long>() {
            @Override
            public void onChanged(Long aLong) {
                //setSeekBarDuration(Math.toIntExact(aLong));
            }
        });
        //监视播放状态
        myViewModel.getIsPlaying().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean isPlaying) {
                setPlayOrPause(isPlaying);
                if (isPlaying)
                    startProgressBar();
                else
                    stopProgressBar();
            }
        });
    }

    public void HideSplash(int where) {
        if (where == 3) {
            initMainActivity();
            myViewModel.getPlayOrder();
            TotalCount.setText(getString(R.string.totalCount).replace("%d", String.valueOf(myViewModel.getmMediaController().getQueue().size())));
            adapter = new PlayingListAdapter(this, myViewModel.getmMediaController().getQueue(), refreshListener);
            playingList.setAdapter(adapter);
            adapter.setNowPlay(myViewModel.getmMediaController().getMetadata().getDescription().getMediaUri().getPath());
            viewPager.setCurrentItem(myViewModel.getViewPagerId());
            //myViewModel.getQueueItemsMutableLiveData().getValue();
            //myViewModel.getNowPlaying().getValue();
            splashCard = findViewById(R.id.splash_view);
            Animation animation = AnimationUtils.loadAnimation(MainActivity.this, R.anim.gradually_movedown_hide);
            splashCard.startAnimation(animation);
            splashCard.setVisibility(View.GONE);
        }
        if (where == 1) isPlayList = true;
        if (where == 2) isHome = true;
        if (myViewModel.isSplash && isHome && isPlayList) {
            isSplash = false;
            myViewModel.isSplash = false;
            TotalCount.setText(getString(R.string.totalCount).replace("%d", String.valueOf(myViewModel.getmMediaController().getQueue().size())));
            adapter = new PlayingListAdapter(this, myViewModel.getmMediaController().getQueue(), refreshListener);
            playingList.setAdapter(adapter);
            if (myViewModel.getmMediaController().getMetadata() != null)
                adapter.setNowPlay(myViewModel.getmMediaController().getMetadata().getDescription().getMediaUri().getPath());
            myViewModel.getPlayOrder();
            splashCard = findViewById(R.id.splash_view);
            Animation animation = AnimationUtils.loadAnimation(MainActivity.this, R.anim.gradually_movedown_hide);
            splashCard.startAnimation(animation);
            splashCard.setVisibility(View.GONE);
        }
    }

    private void setPlayingListView(Song song) {
        adapter.setNowPlay(song.getSongPath());
        adapter.notifyDataSetChanged();
    }

    private void setPlayOrPause(Boolean isPlaying) {
        if (isPlaying) {
            playButton.setImageDrawable(getDrawable(R.drawable.ic_round_pause_circle_outline_24));
        } else {
            playButton.setImageDrawable(getDrawable(R.drawable.ic_round_play_circle_outline_24));
        }
    }

    private void setSeekBarDuration(long i) {
        playProgressBar.setProgress(Math.toIntExact(i), false);
        playProgressNowText.setText(i / 60000 + ((i / 1000 % 60 < 10) ? ":0" : ":") + i / 1000 % 60);
    }

    private void setSeekBarMax(int songDuration) {
        playProgressBar.setMax(songDuration);
        playProgressAllText.setText(songDuration / 60000 + ((songDuration / 1000 % 60 < 10) ? ":0" : ":") + songDuration / 1000 % 60);
    }

    private void initVariable() {
        isAlreadyShowPlayBarTitle = false;
        countActivity = 0;
        isBackground = false;
    }


    private void prepareInfo(int musicId) {
        //setPlayBarTitle(musicId);
        //setChildFragment();
        //songListFragment.setNowPlaying(musicService.getNowId());
        //初始化进度条
        //resetPlayProgress();
    }

    public void playingError(String errMsg) {
        AlertDialog alertInfoDialog = new AlertDialog.Builder(MainActivity.this)
                .setTitle(R.string.play_error)
                .setMessage(getString(R.string.play_err_Info) + "\n" + errMsg)
                .setIcon(R.mipmap.ic_launcher)
                .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

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
        mainActivity = this;
        CrashHandlers crashHandlers = CrashHandlers.getInstance();
        crashHandlers.init(MainActivity.this);
        //SharedPrefs.init(getApplication());
        myViewModel = new ViewModelProvider(MainActivity.mainActivity).get(MyViewModel.class);
        CrashHandlers.checkIfExistsLastCrash(MainActivity.this);

        //允许在主线程连接网络
        //StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        //StrictMode.setThreadPolicy(policy);
//initMainActivity();
        // Bind to LocalService
        /*serviceConnection = new MusicConnector();
        Intent intent = new Intent();
        intent.setClass(MainActivity.this, MusicService.class);
        //startService(intent);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);*/
    }

    @Override
    protected void onStart() {
        super.onStart();
        new Handler().post(new Runnable() {
            @Override
            public void run() {
            }
        });
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
                if (countActivity >= 0 && isBackground) {
                    Log.e("MyApplication", "onActivityStarted: 应用进入前台");
                    isBackground = false;
                    //说明应用重新进入了前台
                    //Toast.makeText(MainActivity.this, "应用进入前台", Toast.LENGTH_SHORT).show();
                    //musicService.setActivityForeground(true);
                    MyViewModel.setActivityForeground(true);
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
                if (countActivity <= 0 && !isBackground) {
                    Log.e("MyApplication", "onActivityStarted: 应用进入后台");
                    isBackground = true;
                    //说明应用进入了后台
                    //Toast.makeText(MainActivity.this, "应用进入后台", Toast.LENGTH_SHORT).show();
                    //musicService.setActivityForeground(false);
                    MyViewModel.setActivityForeground(false);
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
        //musicService.unregisterMusicServiceCallback(musicServiceCallback);
        //unbindService(serviceConnection);
        //homeFragment.onDestroy();
        //homeFragment.stopLyric();
        //stopProgressBar();
        removeObserver();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //两个状态不同，说明Activity被重绘了
        if (!myViewModel.isSplash && isSplash) HideSplash(3);
    }

    private void removeObserver() {
        //myViewModel.getNowPlaying().removeObserver();
    }

    public void clickedPlayOrPause() {
        if (myViewModel == null) return;
        if (Boolean.TRUE.equals(myViewModel.getIsPlaying().getValue()))
            myViewModel.getmMediaController().getTransportControls().pause();
        else
            myViewModel.getmMediaController().getTransportControls().play();
    }

    //public MusicService getMusicService() {
    //    if (musicService != null)
    //        return musicService;
    //    else
    //        return null;
    //}

    public void setPlayBarTitle(Song song) {
        playBarTitle.setText(song.getSongTitle() + " - " + song.getArtistName());
        Bitmap bitmap = myViewModel.getNowAlbumArt();

        shapeableImageView.setImageBitmap(bitmap);
        backImageView.setImageBitmap(bitmap);
    }

    public void playPrevOrNext(boolean isNext) {
        if (isNext)
            myViewModel.getmMediaController().getTransportControls().skipToNext();
        else
            myViewModel.getmMediaController().getTransportControls().skipToPrevious();
        stopProgressBar();
    }

    @Override
    public void onBackPressed() {
        if (viewPager.getCurrentItem() == 0) {
            // If the user is currently looking at the first step, allow the system to handle the
            // Back button. This calls finish() on this activity and pops the back stack.
            super.onBackPressed();
        } else if (viewPager.getCurrentItem() == 1 && songListFragment.multipleChooseFlag) {
            songListFragment.onBackPressed();
        } else {
            viewPager.setCurrentItem(viewPager.getCurrentItem() - 1);
        }
    }

    public void setChildFragment() {
        //if (musicService != null) {
        //homeFragment.setMusicInfo(musicService.getPlayingList().get(musicService.getNowId()));
        //setIsLyric(musicService.isAppLyric());
        //}
    }

    private void initViewCompat() {
        splashCard = findViewById(R.id.splash_view);
        playProgressBar = findViewById(R.id.seekBar);
        playProgressLayout = findViewById(R.id.playProgress);
        musicPlayingLayout = findViewById(R.id.musicPlayingLayout);
        playingListLayout = findViewById(R.id.main_layout_playing_list);
        playProgressNowText = findViewById(R.id.nowProgress);
        playProgressAllText = findViewById(R.id.allProgress);
        playBarTitle = findViewById(R.id.musicPlaying);
        TabTitle = findViewById(R.id.tabText);
        TotalCount = findViewById(R.id.totalCount);
        playButton = findViewById(R.id.playPause);
        PlayBarOrder = findViewById(R.id.playOrder);
        playBarPrev = findViewById(R.id.playPrevious);
        playBarNext = findViewById(R.id.playNext);
        playBarPlayingList = findViewById(R.id.playList);
        shapeableImageView = findViewById(R.id.playBarAlbumImage);
        backImageView = findViewById(R.id.backImageView);
        playingList = findViewById(R.id.main_list_playing);
        playingList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        View footView = LayoutInflater.from(this).inflate(R.layout.playlist_footview, null);
        playingList.addFooterView(footView);
        splashCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });
        adapter = new PlayingListAdapter(this, null, refreshListener);
        playingList.setAdapter(adapter);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        setOnListViewItemClickListener();
        setNewAppearance(prefs.getBoolean("isNewAppearance", false));
        playBarPlayingList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPlayingList();
            }
        });
        playingListLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPlayingList();
            }
        });
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickedPlayOrPause();
            }
        });
        PlayBarOrder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myViewModel.setPlayOrder();
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
                playingList.smoothScrollToPositionFromTop(adapter.getNowPlay(), 0);
            }
        });
        findViewById(R.id.delete_all_playing_list).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog dialog = new AlertDialog.Builder(MainActivity.this)
                        .setTitle(R.string.confirmDelete)
                        .setMessage(R.string.deleteInfo)
                        .setIcon(R.mipmap.ic_launcher)
                        .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                myViewModel.getmMediaController().getTransportControls().sendCustomAction("CLEAR_PLAYLIST", null);
                                dialog.dismiss();
                            }
                        })
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .create();
                dialog.show();
            }
        });
        //根据音乐的时长设置进度条的最大进度
        playProgressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                //homeFragment.stopLyric();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                //musicService.setProgress(seekBar.getProgress());
                //松开之后音乐跳转到相应位置
                playProgressNowText.setText(seekBar.getProgress() / 60000 + ((seekBar.getProgress() / 1000 % 60 < 10) ? ":0" : ":") + seekBar.getProgress() / 1000 % 60);
                //homeFragment.startLyric();
                myViewModel.getmMediaController().getTransportControls().seekTo(seekBar.getProgress());
            }
        });
        //resetPlayProgress();
    }


    private void setOnListViewItemClickListener() {
        playingList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //musicService.playThisFromList(position);
            }
        });
    }


    private void showPlayingList() {
        if (isPlayingListShowing) {
            Animation animation = AnimationUtils.loadAnimation(MainActivity.this, R.anim.gradually_movedown_hide);
            playingListLayout.startAnimation(animation);
            playingListLayout.setVisibility(View.GONE);
            if (viewPager.getCurrentItem() == 0)
                setPlayBarTitle(false);
        } else {
            Animation animation = AnimationUtils.loadAnimation(MainActivity.this, R.anim.gradually_movedown_show);
            playingListLayout.startAnimation(animation);
            playingListLayout.setVisibility(View.VISIBLE);
            if (viewPager.getCurrentItem() == 0)
                setPlayBarTitle(true);
        }
        isPlayingListShowing = !isPlayingListShowing;
    }
/*
    public void setIsLyric(boolean isLyric) {
        if (!isLyric) {
            homeFragment.setIsLyricLayoutShow(false);
            musicService.setAppLyric(false);
        } else {
            homeFragment.setIsLyricLayoutShow(true);
            musicService.setAppLyric(true);
        }
    }*/
/*
    private void setPlayOrder() {
        switch (myViewModel.getPlayOrder()) {
            case MusicService.LIST_PLAY:
                //musicService.setPlayOrder(MusicService.REPEAT_LIST);
                PlayBarOrder.setImageDrawable(getDrawable(R.drawable.ic_round_repeat_24));
                Toast.makeText(this, R.string.repeat_list, Toast.LENGTH_SHORT).show();
                break;
            case MusicService.REPEAT_LIST:
                //musicService.setPlayOrder(MusicService.REPEAT_ONE);
                PlayBarOrder.setImageDrawable(getDrawable(R.drawable.ic_round_repeat_one_24));
                Toast.makeText(this, R.string.repeat_one, Toast.LENGTH_SHORT).show();
                break;
            case MusicService.REPEAT_ONE:
                //musicService.setPlayOrder(MusicService.SHUFFLE_PLAY);
                PlayBarOrder.setImageDrawable(getDrawable(R.drawable.ic_round_shuffle_24));
                Toast.makeText(this, R.string.shuffle_play, Toast.LENGTH_SHORT).show();
                break;
            default:
            case MusicService.SHUFFLE_PLAY:
                //musicService.setPlayOrder(MusicService.LIST_PLAY);
                PlayBarOrder.setImageDrawable(getDrawable(R.drawable.ic_baseline_low_priority_24));
                Toast.makeText(this, R.string.list_play, Toast.LENGTH_SHORT).show();
                break;
        }
    }*/
/*
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
    }*/

    private void initViewPager2() {
        // Instantiate a ViewPager2 and a PagerAdapter.
        viewPager = findViewById(R.id.pager);
        bottomNavigationView = findViewById(R.id.nav_view);
        homeFragment = new HomeFragment();
        songListFragment = new SongListFragment();
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
                myViewModel.setViewPagerId(position);
                switch (bottomNavigationView.getMenu().getItem(position).getItemId()) {
                    case R.id.navigation_home:
                    default:
                        TabTitle.setText(R.string.app_name);
                        if (isPlayingListShowing)
                            showPlayingList();
                        else
                            setPlayBarTitle(false);
                        break;
                    case R.id.navigation_playlist:
                        TabTitle.setText(R.string.title_allSongList);
                        if (isPlayingListShowing)
                            showPlayingList();
                        else
                            setPlayBarTitle(true);
                        break;
                    case R.id.navigation_settings:
                        TabTitle.setText(R.string.title_settings);
                        if (isPlayingListShowing)
                            showPlayingList();
                        else
                            setPlayBarTitle(true);
                        break;
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);
            }
        });
        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.navigation_home:
                    default:
                        setViewPagerToId(0);
                        break;
                    case R.id.navigation_playlist:
                        setViewPagerToId(1);
                        break;
                    case R.id.navigation_settings:
                        setViewPagerToId(2);
                        break;
                }
                return true;
            }
        });
    }

    public void setPlayBarTitle(boolean isShow) {
        if (isShow) {
            if (!isAlreadyShowPlayBarTitle) {
                Animation animation = AnimationUtils.loadAnimation(MainActivity.this, R.anim.gradually_movedown_show);
                musicPlayingLayout.setVisibility(View.VISIBLE);
                musicPlayingLayout.startAnimation(animation);
                animation = AnimationUtils.loadAnimation(MainActivity.this, R.anim.gradually_movedown_hide);
                playProgressLayout.startAnimation(animation);
                playProgressLayout.setVisibility(View.GONE);
                isAlreadyShowPlayBarTitle = true;
            }
        } else {
            if (isAlreadyShowPlayBarTitle) {
                Animation animation = AnimationUtils.loadAnimation(MainActivity.this, R.anim.gradually_movedown_hide);
                musicPlayingLayout.startAnimation(animation);
                musicPlayingLayout.setVisibility(View.GONE);
                animation = AnimationUtils.loadAnimation(MainActivity.this, R.anim.gradually_movedown_show);
                playProgressLayout.setVisibility(View.VISIBLE);
                playProgressLayout.startAnimation(animation);
                isAlreadyShowPlayBarTitle = false;
            }
        }
    }

    public void setViewPagerToId(int pageId) {
        viewPager.setCurrentItem(pageId, true);
        myViewModel.setViewPagerId(pageId);
    }

    public void setPlayOrder(int playOrder) {
        PlayBarOrder = findViewById(R.id.playOrder);
        switch (playOrder) {
            case LIST_PLAY:
                PlayBarOrder.setImageResource(R.drawable.ic_baseline_low_priority_24);
                break;
            case REPEAT_LIST:
                PlayBarOrder.setImageResource(R.drawable.ic_round_repeat_24);
                break;
            case REPEAT_ONE:
                PlayBarOrder.setImageResource(R.drawable.ic_round_repeat_one_24);
                break;
            case SHUFFLE_PLAY:
                PlayBarOrder.setImageResource(R.drawable.ic_round_shuffle_24);
        }
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
                    songListFragment = new SongListFragment();
                    return songListFragment;
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

    public void setNewAppearance(boolean isTrue) {
        if (isTrue) {
            backImageView.setVisibility(View.VISIBLE);
            findViewById(R.id.realtimeBlurView).setVisibility(View.VISIBLE);
            findViewById(R.id.blur_playing_list).setVisibility(View.VISIBLE);
            findViewById(R.id.playingListBack).setVisibility(View.GONE);
        } else {
            backImageView.setVisibility(View.GONE);
            findViewById(R.id.realtimeBlurView).setVisibility(View.GONE);
            findViewById(R.id.blur_playing_list).setVisibility(View.GONE);
            findViewById(R.id.playingListBack).setVisibility(View.VISIBLE);
        }
    }

    public void startProgressBar() {
        if (progressThread == null) {
            progressThread = new ProgressThread();
            progressThread.start();
        } else if (progressThread.isPaused()) {
            progressThread.resumeThread();
        }
    }

    public void stopProgressBar() {
        if (progressThread != null && !progressThread.isPaused())
            progressThread.pauseThread();
    }

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
                    Long nowMillionSeconds = 0L;
                    if (myViewModel.getmMediaController().getPlaybackState() != null) {
                        nowMillionSeconds = myViewModel.getmMediaController().getPlaybackState().getPosition();
                        //Log.e("TAG", String.valueOf(nowMillionSeconds));
                        Message msg = new Message();
                        msg.what = 100;  //消息发送的标志
                        msg.obj = nowMillionSeconds.intValue(); //消息发送的内容如：  Object String 类 int
                        progressHandler.sendMessage(msg);
                    }
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
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

}
