package com.liux.musicplayer.services;

import static androidx.annotation.Dimension.SP;

import android.app.Activity;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Chronometer;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.liux.musicplayer.R;
import com.liux.musicplayer.interfaces.DeskLyricCallback;
import com.liux.musicplayer.activities.MainActivity;
import com.liux.musicplayer.utils.LyricUtils;
import com.liux.musicplayer.views.StrokeTextView;


public class FloatLyricService extends Service {


    private WindowManager winManager;
    private WindowManager.LayoutParams wmParams;
    private LayoutInflater inflater;
    private LyricThread lyricThread;
    private StrokeTextView firstLyric;
    private StrokeTextView secondLyric;
    private HorizontalScrollView firstScroll;
    private HorizontalScrollView secondScroll;

    //浮动布局
    private View mFloatingLayout;
    private Chronometer chronometer;
    private long rangeTime;
    private boolean isAllShow;
    private boolean isSettingsBar;
    //private MusicService musicService;
    private MusicConnector serviceConnection;
    private LyricUtils lyric;
    private int nowLyricId;
    private int nowTextSize;
    private int nowColorId;
    private String nowTitle = "";
    private String nowArtist = "";
    private SharedPreferences sp;
    private DeskLyricCallback deskLyricCallback = new DeskLyricCallback() {
        @Override
        public void updatePlayState(int musicId) {
            //nowTitle = musicService.getPlayingList().get(musicId).title;
            //nowArtist = musicService.getPlayingList().get(musicId).artist;
            updatePlayInfo(nowTitle
                    + ((nowArtist.equals("null")) ? "" : " - " + nowArtist));
            if (lyric.isCompleted)
                updateLyric();
            updatePlayState();
        }

        @Override
        public void updatePlayState() {
            //if (musicService.isPlaying()) {
                ((ImageView) mFloatingLayout.findViewById(R.id.playPause)).setImageDrawable(getDrawable(R.drawable.ic_round_pause_circle_float_24));
                //startLyricThread();
            //} else if (!musicService.isPrepared()) {
                ((ImageView) mFloatingLayout.findViewById(R.id.playPause)).setImageDrawable(getDrawable(R.drawable.ic_round_arrow_circle_down_float_24));
                //stopLyricThread();
            //} else {
                ((ImageView) mFloatingLayout.findViewById(R.id.playPause)).setImageDrawable(getDrawable(R.drawable.ic_round_play_circle_float_24));
                //stopLyricThread();
            }
        //}

        @Override
        public void newLyricSettings() {
            initLyricSettings();
        }
    };

    public void LyricSettings() {
        int color;
        switch (nowColorId) {
            case 0:
                color = 0xFFF44336;
                break;
            default:
            case 1:
                color = 0xFF03A9F4;
                break;
            case 2:
                color = 0xFF4CAF50;
                break;
            case 3:
                color = 0xFFFFC107;
                break;
            case 4:
                color = 0xFF673AB7;
                break;
        }
        ((StrokeTextView) (mFloatingLayout.findViewById(R.id.firstLyric))).setTextColor(color);
        ((StrokeTextView) (mFloatingLayout.findViewById(R.id.firstLyric))).setTextSize(SP, nowTextSize);
        ((StrokeTextView) (mFloatingLayout.findViewById(R.id.secondLyric))).setTextSize(SP, nowTextSize);
        setColorCheck(nowColorId);
    }

    public void LyricSettings(int textSize, int colorID) {
        nowColorId = colorID;
        nowTextSize = textSize;
        LyricSettings();
        saveLyricSettings();
    }

    private View.OnClickListener lyricPlayBarListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.playPause:
                   // musicService.setPlayOrPause(!musicService.isPlaying());
                    break;
                case R.id.playNext:
                    //musicService.playPrevOrNext(true);
                    break;
                case R.id.playPrevious:
                   // musicService.playPrevOrNext(false);
                    break;
                case R.id.lyricSettings:
                    if (isSettingsBar)
                        mFloatingLayout.findViewById(R.id.settingsBar).setVisibility(View.GONE);
                    else
                        mFloatingLayout.findViewById(R.id.settingsBar).setVisibility(View.VISIBLE);
                    isSettingsBar = !isSettingsBar;
                    break;
                case R.id.lockLyric:
                    LockLyric();
                    break;
                case R.id.close:
                    //if (musicService != null)
                    //    musicService.showDesktopLyric();
                    break;
                case R.id.appIcon:
                    Intent intent = new Intent(FloatLyricService.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    break;
                case R.id.lyricCover:
                    setAllShow();
                    break;
            }
        }
    };

    private void LockLyric() {
        setAllShow();
        wmParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR;
        winManager.updateViewLayout(mFloatingLayout, wmParams);
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean("deskLyricLock", true);
        editor.apply();
    }

    private View.OnClickListener lyricSettingListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.color_red:
                    nowColorId = 0;
                    break;
                case R.id.color_blue:
                    nowColorId = 1;
                    break;
                case R.id.color_green:
                    nowColorId = 2;
                    break;
                case R.id.color_yellow:
                    nowColorId = 3;
                    break;
                case R.id.color_purple:
                    nowColorId = 4;
                    break;
                case R.id.largerText:
                    if (nowTextSize < 24)
                        nowTextSize += 2;
                    break;
                case R.id.smallerText:
                    if (nowTextSize > 14)
                        nowTextSize -= 2;
                    break;
            }
            setColorCheck(nowColorId);
            LyricSettings(nowTextSize, nowColorId);
        }
    };

    private void setColorCheck(int nowColorID) {
        ((ImageView) (mFloatingLayout.findViewById(R.id.color_red))).setImageDrawable(null);
        ((ImageView) (mFloatingLayout.findViewById(R.id.color_blue))).setImageDrawable(null);
        ((ImageView) (mFloatingLayout.findViewById(R.id.color_green))).setImageDrawable(null);
        ((ImageView) (mFloatingLayout.findViewById(R.id.color_yellow))).setImageDrawable(null);
        ((ImageView) (mFloatingLayout.findViewById(R.id.color_purple))).setImageDrawable(null);
        int viewID = R.id.color_blue;
        switch (nowColorID) {
            case 0:
                viewID = R.id.color_red;
                break;
            case 1:
                viewID = R.id.color_blue;
                break;
            case 2:
                viewID = R.id.color_green;
                break;
            case 3:
                viewID = R.id.color_yellow;
                break;
            case 4:
                viewID = R.id.color_purple;
                break;
        }
        ((ImageView) (mFloatingLayout.findViewById(viewID))).setImageDrawable(getDrawable(R.drawable.ic_round_check_24));
    }

    private void updateLyric() {
        if (nowLyricId >= lyric.lyricList.size()) return;
        if (lyric.lyricList.size() > 1) {
            String s = lyric.lyricList.get(nowLyricId);
            if (s.contains("\n")) {
                firstLyric.setText(s.split("\n")[0]);
                secondLyric.setText(s.split("\n")[1]);
            } else {
                firstLyric.setText(s);
                if (lyric.size() > nowLyricId + 1) {
                    secondLyric.setText(lyric.lyricList.get(nowLyricId + 1));
                } else {
                    secondLyric.setText("The End");
                }
            }
        } else if (lyric.lyricList.size() == 1) {
            firstLyric.setText(getString(R.string.nowPlaying) + nowTitle);
            secondLyric.setText(lyric.lyricList.get(0));
        } else {
            firstLyric.setText(getString(R.string.nowPlaying) + nowTitle);
            secondLyric.setText(getString(R.string.lyricFileIsEmpty));
        }
        firstScroll.scrollTo(0, 0);
        secondScroll.scrollTo(0, 0);
        firstScroll.post(new Runnable() {
            @Override
            public void run() {
                int offset = firstLyric.getMeasuredWidth() - firstScroll.getMeasuredWidth();
                if (offset > 0) {
                    try {
                        Thread.sleep(700);
                        firstScroll.smoothScrollTo(offset, 0);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    firstScroll.scrollTo(0, 0);
                }
            }
        });
        secondScroll.post(new Runnable() {
            @Override
            public void run() {
                int offset = secondLyric.getMeasuredWidth() - secondScroll.getMeasuredWidth();
                if (offset > 0) {
                    try {
                        Thread.sleep(700);
                        secondScroll.smoothScrollTo(offset, 0);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    secondScroll.scrollTo(0, 0);
                }
            }
        });
    }

    private class LyricThread extends Thread {
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
                    /*if (musicService.isPlaying() && lyric.isCompleted) {
                        int currentLyricId = lyric.getNowLyric(musicService.getCurrentPosition());
                        if (currentLyricId >= 0) {
                            Message msg = new Message();
                            msg.what = 200;  //消息发送的标志
                            msg.obj = currentLyricId; //消息发送的内容如：  Object String 类 int
                            LyricHandler.sendMessage(msg);
                        }
                    }*/
                    Thread.sleep(10);
                } catch (InterruptedException | NullPointerException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    private final Handler LyricHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 200) {
                if ((int) msg.obj != nowLyricId) {
                    nowLyricId = (int) msg.obj;
                    updateLyric();
                }
            }
        }
    };

    private class MusicConnector implements ServiceConnection {
        //成功绑定时调用 即bindService（）执行成功同时返回非空Ibinder对象
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            //musicService = ((MusicService.MyMusicBinder) iBinder).getService();
            //Log.e("MusicConnector", "musicService" + musicService);
            //musicService.setDeskLyricCallback(deskLyricCallback);
            initWindow();
            //悬浮框点击事件的处理
            initFloating();
            initLyricSettings();
            //lyric = musicService.getLyric();    //从音乐服务中歌词
            lyric.setOnLyricLoadCallback(new LyricUtils.OnLyricLoadCallback() {
                @Override
                public void LyricLoadCompleted() {
                    //nowLyricId = lyric.getNowLyric(musicService.getCurrentPosition());
                    updateLyric();
                }
            });
            //musicService.updateDeskLyricPlayInfo();
        }

        //不成功绑定时调用
        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            //musicService = null;
            Log.i("binding is fail", "binding is fail");
        }
    }

    private void initLyricSettings() {
        nowTextSize = sp.getInt("deskLyricTextSize", 18);
        nowColorId = sp.getInt("deskLyricColorId", 1);
        LyricSettings();
    }

    private void saveLyricSettings() {
        SharedPreferences.Editor editor = sp.edit();
        editor.putInt("deskLyricTextSize", nowTextSize);
        editor.putInt("deskLyricColorId", nowColorId);
        editor.putInt("deskLyricY", wmParams.y);
        editor.apply();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sp = this.getSharedPreferences(getPackageName() + "_preferences", Activity.MODE_PRIVATE);
        // Bind to LocalService
        serviceConnection = new MusicConnector();
        Intent intent = new Intent();
        //intent.setClass(FloatLyricService.this, MusicService.class);
        //startService(intent);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    /**
     * 悬浮窗点击事件
     */
    private void initFloating() {
        isAllShow = true;
        isSettingsBar = false;
        setAllShow();
        firstLyric = mFloatingLayout.findViewById(R.id.firstLyric);
        secondLyric = mFloatingLayout.findViewById(R.id.secondLyric);
        firstScroll = mFloatingLayout.findViewById(R.id.firstScroll);
        secondScroll = mFloatingLayout.findViewById(R.id.secondScroll);
        //悬浮框触摸事件，设置悬浮框可拖动
        mFloatingLayout.findViewById(R.id.lyricCover).setOnTouchListener(new FloatingListener());

        mFloatingLayout.findViewById(R.id.lyricCover).setOnClickListener(lyricPlayBarListener);
        mFloatingLayout.findViewById(R.id.playPause).setOnClickListener(lyricPlayBarListener);
        mFloatingLayout.findViewById(R.id.playPrevious).setOnClickListener(lyricPlayBarListener);
        mFloatingLayout.findViewById(R.id.playNext).setOnClickListener(lyricPlayBarListener);
        mFloatingLayout.findViewById(R.id.lyricSettings).setOnClickListener(lyricPlayBarListener);
        mFloatingLayout.findViewById(R.id.lockLyric).setOnClickListener(lyricPlayBarListener);
        mFloatingLayout.findViewById(R.id.close).setOnClickListener(lyricPlayBarListener);
        mFloatingLayout.findViewById(R.id.appIcon).setOnClickListener(lyricPlayBarListener);

        mFloatingLayout.findViewById(R.id.color_red).setOnClickListener(lyricSettingListener);
        mFloatingLayout.findViewById(R.id.color_blue).setOnClickListener(lyricSettingListener);
        mFloatingLayout.findViewById(R.id.color_green).setOnClickListener(lyricSettingListener);
        mFloatingLayout.findViewById(R.id.color_yellow).setOnClickListener(lyricSettingListener);
        mFloatingLayout.findViewById(R.id.color_purple).setOnClickListener(lyricSettingListener);
        mFloatingLayout.findViewById(R.id.largerText).setOnClickListener(lyricSettingListener);
        mFloatingLayout.findViewById(R.id.smallerText).setOnClickListener(lyricSettingListener);
    }

    public void setAllShow() {
        if (isAllShow) {
            mFloatingLayout.findViewById(R.id.background).setVisibility(View.INVISIBLE);
            mFloatingLayout.findViewById(R.id.lyricCtrlBar).setVisibility(View.GONE);
            mFloatingLayout.findViewById(R.id.lyricHeader).setVisibility(View.INVISIBLE);
        } else {
            mFloatingLayout.findViewById(R.id.background).setVisibility(View.VISIBLE);
            mFloatingLayout.findViewById(R.id.lyricHeader).setVisibility(View.VISIBLE);
            mFloatingLayout.findViewById(R.id.lyricCtrlBar).setVisibility(View.VISIBLE);
        }
        isAllShow = !isAllShow;
    }

    //开始触控的坐标，移动时的坐标（相对于屏幕左上角的坐标）
    private int mTouchStartX, mTouchStartY, mTouchCurrentX, mTouchCurrentY;
    //开始时的坐标和结束时的坐标（相对于自身控件的坐标）
    private int mStartX, mStartY, mStopX, mStopY;
    //判断悬浮窗口是否移动，这里做个标记，防止移动后松手触发了点击事件
    private boolean isMove;

    private class FloatingListener implements View.OnTouchListener {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int action = event.getAction();
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    isMove = false;
                    mTouchStartX = (int) event.getRawX();
                    mTouchStartY = (int) event.getRawY();
                    mStartX = (int) event.getX();
                    mStartY = (int) event.getY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    mTouchCurrentX = (int) event.getRawX();
                    mTouchCurrentY = (int) event.getRawY();
                    wmParams.x += mTouchCurrentX - mTouchStartX;
                    wmParams.y += mTouchCurrentY - mTouchStartY;
                    winManager.updateViewLayout(mFloatingLayout, wmParams);
                    mTouchStartX = mTouchCurrentX;
                    mTouchStartY = mTouchCurrentY;
                    break;
                case MotionEvent.ACTION_UP:
                    mStopX = (int) event.getX();
                    mStopY = (int) event.getY();
                    if (Math.abs(mStartX - mStopX) >= 1 || Math.abs(mStartY - mStopY) >= 1) {
                        isMove = true;
                    }
                    break;
                default:
                    break;
            }

            //如果是移动事件不触发OnClick事件，防止移动的时候一放手形成点击事件
            return isMove;
        }
    }

    /**
     * 初始化窗口
     */
    private void initWindow() {
        int y = sp.getInt("deskLyricY", 210);
        boolean lyricLock = sp.getBoolean("deskLyricLock", false);
        winManager = (WindowManager) getApplication().getSystemService(Context.WINDOW_SERVICE);
        //设置好悬浮窗的参数
        wmParams = getMyParams();
        // 透明背景
        wmParams.format = PixelFormat.RGBA_8888;
        // 悬浮窗默认显示以左上角为起始坐标
        wmParams.gravity = Gravity.CENTER_HORIZONTAL | Gravity.TOP;
        //悬浮窗的开始位置，因为设置的是从左上角开始，所以屏幕左上角是x=0;y=0
        //wmParams.x = winManager.getDefaultDisplay().getWidth();
        wmParams.x = 0;
        wmParams.y = y;
        wmParams.width = winManager.getDefaultDisplay().getWidth();
        //得到容器，通过这个inflater来获得悬浮窗控件
        inflater = LayoutInflater.from(getApplicationContext());
        // 获取浮动窗口视图所在布局
        mFloatingLayout = inflater.inflate(R.layout.desktop_lyric, null);
        // 添加悬浮窗的视图
        if (lyricLock) {
            wmParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR;
        }
        winManager.addView(mFloatingLayout, wmParams);
    }

    private WindowManager.LayoutParams getMyParams() {
        wmParams = new WindowManager.LayoutParams();
        //设置window type 下面变量2002是在屏幕区域显示，2003则可以显示在状态栏之上
        //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        wmParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        //} else {
        //    wmParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        //}
        //设置可以显示在状态栏上
        wmParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR |
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;

        //设置悬浮窗口长宽数据
        wmParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        wmParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        return wmParams;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        if (winManager != null) {
            winManager.removeView(mFloatingLayout);
            SharedPreferences.Editor editor = sp.edit();
            editor.putInt("deskLyricY", wmParams.y);
            editor.apply();
        }
        stopLyricThread();
        unbindService(serviceConnection);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startLyricThread() {
        if (lyricThread == null) {
            lyricThread = new LyricThread();
            lyricThread.start();
        } else if (lyricThread.isPaused()) {
            lyricThread.resumeThread();
        }
    }

    private void stopLyricThread() {
        if (lyricThread != null && !lyricThread.isPaused())
            lyricThread.pauseThread();
    }

    public void updatePlayInfo(String nowPlaying) {
        ((TextView) (mFloatingLayout.findViewById(R.id.nowPlaying))).setText(getText(R.string.nowPlaying) + nowPlaying);
    }
}