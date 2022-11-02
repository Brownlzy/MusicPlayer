package com.liux.musicplayer.service;

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
import com.liux.musicplayer.ui.MainActivity;
import com.liux.musicplayer.utils.LyricUtils;
import com.liux.musicplayer.views.StrokeTextView;

 /**
  * 用于显示桌面悬浮歌词的后台服务，需要由{@link MusicService}启动，启动后会立即绑定{@link MusicService}。展示所需的歌词数据和歌曲信息均由{@link MusicService}提供
  * @author         Brownlzy
  * @CreateDate:     2022/10/6
  * @UpdateDate:     2022/9/28
  * @Version:        1.0
  */
public class FloatLyricService extends Service {

    private WindowManager winManager;
    private WindowManager.LayoutParams wmParams;
    private LayoutInflater inflater;

    //用于歌词的线程更新
    private LyricThread lyricThread;

    //用于显示歌词的控件
    private StrokeTextView firstLyric;
    private StrokeTextView secondLyric;
    private HorizontalScrollView firstScroll;
    private HorizontalScrollView secondScroll;

    //浮动布局
    private View mFloatingLayout;
    private boolean isAllShow;
    private boolean isSettingsBar;
    private MusicService musicService;
    private MusicConnector serviceConnection;
    private LyricUtils lyric;
    private int nowLyricId;
    private int nowTextSize;
    private int nowColorId;
    private String nowTitle = "";
    private String nowArtist = "";
    private SharedPreferences sp;

     //开始触控的坐标，移动时的坐标（相对于屏幕左上角的坐标）
     private int mTouchStartX, mTouchStartY, mTouchCurrentX, mTouchCurrentY;
     //开始时的坐标和结束时的坐标（相对于自身控件的坐标）
     private int mStartX, mStartY, mStopX, mStopY;
     //判断悬浮窗口是否移动，这里做个标记，防止移动后松手触发了点击事件
     private boolean isMove;

     /** 实现的桌面歌词回调接口{@link DeskLyricCallback} */
    private final DeskLyricCallback deskLyricCallback = new DeskLyricCallback() {
        @Override
        public void updateNowPlaying(int musicId) {
            nowTitle = musicService.getPlayList().get(musicId).title;
            nowArtist = musicService.getPlayList().get(musicId).artist;
            updatePlayInfo(nowTitle
                    + ((nowArtist.equals("null")) ? "" : " - " + nowArtist));
            if (lyric.isCompleted)
                updateLyric();
            updatePlayState();
        }

        @Override
        public void updatePlayState() {
            //根据最新播放状态设置显示的播放按钮图标
            if (musicService.isPlaying()) {
                ((ImageView) mFloatingLayout.findViewById(R.id.playPause)).setImageDrawable(getDrawable(R.drawable.ic_round_pause_circle_float_24));
                startLyricThread();
            } else if (!musicService.isPrepared()) {
                ((ImageView) mFloatingLayout.findViewById(R.id.playPause)).setImageDrawable(getDrawable(R.drawable.ic_round_arrow_circle_down_float_24));
                stopLyricThread();
            } else {
                ((ImageView) mFloatingLayout.findViewById(R.id.playPause)).setImageDrawable(getDrawable(R.drawable.ic_round_play_circle_float_24));
                stopLyricThread();
            }
        }
    };
 /**
  * 根据已加载的界面自定义参数设置
  * @param textSize 桌面歌词的字号（单位sp）
  * @param colorID 歌词字体颜色id（0-4）
  * @return void
  */
    public void LyricSettings(int textSize, int colorID) {
        nowColorId = colorID;
        nowTextSize = textSize;
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
        saveLyricSettings();
    }
/** 桌面歌词播放栏的按钮监听器 */
    private View.OnClickListener lyricPlayBarListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.playPause:
                    //暂停
                    musicService.setPlayOrPause(!musicService.isPlaying());
                    break;
                case R.id.playNext:
                    //下一首
                    musicService.playPrevOrNext(true);
                    break;
                case R.id.playPrevious:
                    //上一首
                    musicService.playPrevOrNext(false);
                    break;
                case R.id.lyricSettings:
                    //点击了设置
                    if (isSettingsBar)
                        mFloatingLayout.findViewById(R.id.settingsBar).setVisibility(View.GONE);
                    else
                        mFloatingLayout.findViewById(R.id.settingsBar).setVisibility(View.VISIBLE);
                    isSettingsBar = !isSettingsBar;
                    break;
                case R.id.lockLyric:
                    //点击了锁定歌词
                    LockLyric();
                    break;
                case R.id.close:
                    //点击了关闭
                    if (musicService != null)
                        musicService.showDesktopLyric();
                    break;
                case R.id.appIcon:
                    //点击了app图标
                    Intent intent = new Intent(FloatLyricService.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    break;
                case R.id.lyricCover:
                    //点击了歌词区域
                    setAllShow();
                    break;
            }
        }
    };
 /**
  * 切换桌面歌词为锁定模式（因为取消锁定桌面歌词在{@link com.liux.musicplayer.ui.settings.SettingsFragment}中，进行{@link FloatLyricService}一定处于终止状态，故无需设置解锁方法）
  * @return void
  */
    private void LockLyric() {
        //点击锁定按钮时，桌面歌词一定为完整状态，调用此方法用于切换仅显示歌词模式
        setAllShow();
        //设置窗口flag
        wmParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL    //不接受点击事件
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE //不可聚焦
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE //不可触摸
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR;
        //更新参数
        winManager.updateViewLayout(mFloatingLayout, wmParams);
        //报存锁定设置
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean("deskLyricLock", true);
        editor.apply();
    }
    /** 点击设置栏时触发的监听器 */
    private View.OnClickListener lyricSettingListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            //根据点击到的控件id确定执行代码
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
                    //字体最大字号为24sp
                    if (nowTextSize < 24)
                        nowTextSize += 2;
                    break;
                case R.id.smallerText:
                    //字体最小字号为14sp
                    if (nowTextSize > 14)
                        nowTextSize -= 2;
                    break;
            }
            //更新颜色选中信息显示
            setColorCheck(nowColorId);
            //更新设置
            LyricSettings(nowTextSize, nowColorId);
        }
    };
     /**
      * 根据颜色id更新设置栏颜色选项的选中对勾位置
      * @param nowColorID 新选中的颜色id
      * @return void
      */
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
        //设置被选中的ImageView控件显示对勾
        ((ImageView) (mFloatingLayout.findViewById(viewID))).setImageDrawable(getDrawable(R.drawable.ic_round_check_24));
    }
 /**
  * 更新当前显示的歌词
  * @return void
  */
    private void updateLyric() {
        //如果当前歌词行id大于歌词列表的长度，说明歌词数据未初始化，暂不执行操作
        if (nowLyricId >= lyric.lyricList.size()) return;
        if (lyric.lyricList.size() > 1) {   //如果歌词总行数大于1，则按常规显示方法显示
            String s = lyric.lyricList.get(nowLyricId); //通过歌词行id获取当前行歌词
            if (s.contains("\n")) { //该行歌词包括换行，说明它为带翻译的歌词
                firstLyric.setText(s.split("\n")[0]);   //第一个TextView显示原文
                secondLyric.setText(s.split("\n")[1]);  //第二行TextView显示翻译
            } else {    //不包含换行
                firstLyric.setText(s);  //第一行显示获取到的歌词
                if (lyric.size() > nowLyricId + 1) {    //判断是否存在下一行歌词
                    secondLyric.setText(lyric.lyricList.get(nowLyricId + 1));  //第二个TextView显示下一行歌词
                } else {
                    secondLyric.setText("The End"); //第二个TextView显示结束标记
                }
            }
        } else if (lyric.lyricList.size() == 1) {   //歌词数量为1
            firstLyric.setText(getString(R.string.nowPlaying) + nowTitle);  //第一行显示正在播放的歌曲名
            secondLyric.setText(lyric.lyricList.get(0));    //第二行显示歌词
        } else {
            firstLyric.setText(getString(R.string.nowPlaying) + nowTitle);  //第一行显示正在播放的歌曲名
            secondLyric.setText(getString(R.string.lyricFileIsEmpty));  //第二行显示歌词文件为空
        }
        //实现超出屏幕长度的歌词滚动
        firstScroll.scrollTo(0, 0);
        secondScroll.scrollTo(0, 0);
        //滚动开始时间为700ms后
        firstScroll.postDelayed(new Runnable() {
            @Override
            public void run() {
                int offset = firstLyric.getMeasuredWidth() - firstScroll.getMeasuredWidth();
                if (offset > 0) {
                        firstScroll.smoothScrollTo(offset, 0);
                } else {
                    firstScroll.scrollTo(0, 0);
                }
            }
        },700);
        secondScroll.postDelayed(new Runnable() {
            @Override
            public void run() {
                int offset = secondLyric.getMeasuredWidth() - secondScroll.getMeasuredWidth();
                if (offset > 0) {
                        secondScroll.smoothScrollTo(offset, 0);
                } else {
                    secondScroll.scrollTo(0, 0);
                }
            }
        },700);
    }
 /**
  * 歌词更新线程
  * @author         Brownlzy
  * @CreateDate:     2022/9/29
  * @Version:        1.0
  */
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
                    if (musicService.isPlaying() && lyric.isCompleted) {    //歌曲正在播放且歌词加载完毕
                        int currentLyricId = lyric.getNowLyric(musicService.getCurrentPosition());
                        if (currentLyricId >= 0) {
                            Message msg = new Message();
                            msg.what = 200;  //消息发送的标志
                            msg.obj = currentLyricId; //消息发送的内容为当前歌词id
                            LyricHandler.sendMessage(msg);
                        }
                    }
                    //延迟10ms
                    Thread.sleep(10);
                } catch (InterruptedException | NullPointerException e) {
                    e.printStackTrace();
                }
            }
        }

    }
/** 由于非主线程不允许更新UI，lyricThread需要此Handler更新歌词 */
    private final Handler LyricHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 200) {
                if ((int) msg.obj != nowLyricId) {
                    nowLyricId = (int) msg.obj; //获取Message的内容
                    updateLyric();
                }
            }
        }
    };
 /**
  * 连接MusicServer，执行连接状态回调
  * @author         Brownlzy
  * @CreateDate:     2022/9/29
  * @Version:        1.0
  */
    private class MusicConnector implements ServiceConnection {
        //成功绑定时调用 即bindService（）执行成功同时返回非空Ibinder对象
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            musicService = ((MusicService.MyMusicBinder) iBinder).getService();
            Log.e("MusicConnector", "musicService" + musicService);
            musicService.setDeskLyricCallback(deskLyricCallback);
            //初始化悬浮窗
            initWindow();
            //悬浮框点击事件的处理
            initFloating();
            //初始化歌词设置
            initLyricSettings();
            lyric = musicService.getLyric();    //从音乐服务中歌词
            lyric.setOnLyricLoadCallback(new LyricUtils.OnLyricLoadCallback() {
                @Override
                public void LyricLoadCompleted() {
                    //保存当前歌词id
                    nowLyricId = lyric.getNowLyric(musicService.getCurrentPosition());
                    updateLyric();
                }
            });
            //通知MusicService更新歌曲id
            musicService.updateDeskLyricPlayInfo();
        }

        //不成功绑定时调用
        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            musicService = null;
            Log.i("binding is fail", "binding is fail");
            stopSelf();
        }
    }
 /**
  * 读取歌词显示设置
  */
    private void initLyricSettings() {
        //读取设置
        nowTextSize = sp.getInt("deskLyricTextSize", 18);
        nowColorId = sp.getInt("deskLyricColorId", 1);
        LyricSettings(nowTextSize,nowColorId);
    }
 /**
  * 保存歌词显示设置
  */
    private void saveLyricSettings() {
        //保存设置
        SharedPreferences.Editor editor = sp.edit();
        editor.putInt("deskLyricTextSize", nowTextSize);
        editor.putInt("deskLyricColorId", nowColorId);
        editor.putInt("deskLyricY", wmParams.y);    //保存当前位置
        editor.apply();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sp = this.getSharedPreferences(getPackageName() + "_preferences", Activity.MODE_PRIVATE);
        // 绑定MusicService
        serviceConnection = new MusicConnector();
        Intent intent = new Intent();
        intent.setClass(FloatLyricService.this, MusicService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    /**
     * 注册悬浮窗点击事件
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
 /**
  * 在桌面歌词简洁与完整状态间切换
  */
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
 /**
  * 实现悬浮窗的拖动和点击事件的筛选
  */
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
         boolean lyricLock = sp.getBoolean("deskLyricLock", false);
         winManager = (WindowManager) getApplication().getSystemService(Context.WINDOW_SERVICE);
         //设置好悬浮窗的参数
         wmParams = getMyParams(lyricLock);
         //得到容器，通过这个inflater来获得悬浮窗控件
         inflater = LayoutInflater.from(getApplicationContext());
         // 获取浮动窗口视图所在布局
         mFloatingLayout = inflater.inflate(R.layout.desktop_lyric, null);
         // 添加悬浮窗的视图
         winManager.addView(mFloatingLayout, wmParams);
     }

     private WindowManager.LayoutParams getMyParams(boolean isLock) {
         wmParams = new WindowManager.LayoutParams();
         wmParams.alpha=0.8f;
         wmParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
         if(!isLock){
             //设置可以显示在状态栏上
             wmParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                     WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR |
                     WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;
         }else {
             wmParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                     | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                     | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                     | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                     | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR;
         }
         //设置悬浮窗口长宽数据
         wmParams.width = winManager.getDefaultDisplay().getWidth();
         wmParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
         // 透明背景
         wmParams.format = PixelFormat.RGBA_8888;
         // 悬浮窗默认显示以左上角为起始坐标
         wmParams.gravity = Gravity.CENTER_HORIZONTAL | Gravity.TOP;
         //悬浮窗的开始位置，因为设置的是从左上角开始，所以屏幕左上角是x=0;y=0
         int y = sp.getInt("deskLyricY", 210);
         wmParams.x = 0;
         wmParams.y = y;
         return wmParams;
     }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        //销毁窗体前先保存位置
        if (winManager != null) {
            winManager.removeView(mFloatingLayout);
            SharedPreferences.Editor editor = sp.edit();
            editor.putInt("deskLyricY", wmParams.y);
            editor.apply();
        }
        //停止更新
        stopLyricThread();
        //解绑服务
        unbindService(serviceConnection);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
 /**
  * 启动歌词更新线程
  */
    private void startLyricThread() {
        if (lyricThread == null) {
            lyricThread = new LyricThread();
            lyricThread.start();
        } else if (lyricThread.isPaused()) {
            lyricThread.resumeThread();
        }
    }
     /**
      * 暂停歌词更新线程
      */
    private void stopLyricThread() {
        if (lyricThread != null && !lyricThread.isPaused())
            lyricThread.pauseThread();
    }
 /**
  * 更新正在播放信息
  * @param nowPlaying 正在播放的歌曲信息
  */
    public void updatePlayInfo(String nowPlaying) {
        ((TextView) (mFloatingLayout.findViewById(R.id.nowPlaying))).setText(getText(R.string.nowPlaying) + nowPlaying);
    }
}