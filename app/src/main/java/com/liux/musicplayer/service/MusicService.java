package com.liux.musicplayer.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;

import com.blankj.utilcode.util.FileUtils;
import com.blankj.utilcode.util.PathUtils;
import com.blankj.utilcode.util.TimeUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.liux.musicplayer.R;
import com.liux.musicplayer.interfaces.DeskLyricCallback;
import com.liux.musicplayer.interfaces.MusicServiceCallback;
import com.liux.musicplayer.receiver.BluetoothStateReceiver;
import com.liux.musicplayer.receiver.HeadsetPlugReceiver;
import com.liux.musicplayer.receiver.MediaButtonReceiver;
import com.liux.musicplayer.ui.MainActivity;
import com.liux.musicplayer.utils.LyricUtils;
import com.liux.musicplayer.utils.MusicUtils;
import com.liux.musicplayer.utils.UploadDownloadUtils;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
 /**
  * 音乐播放器后台服务，实现播放音乐的核心功能，为其他类提供完整生命周期的信息存储
  * @author         Brownlzy
  * @CreateDate:     2022/9/13
  * @version        2.0
  */
public class MusicService extends Service implements MediaButtonReceiver.IKeyDownListener {
    public static final String PLAY = "play";
    public static final String PREV = "prev";
    public static final String STOP = "stop";
    public static final String NEXT = "next";
    public static final String CLOSE = "close";
    public static final String LYRIC = "lyric";
    public static final String TAG = "MusicService";
    public static final int NOTIFICATION_ID = 1;
    //播放顺序id
    public static final int LIST_PLAY = 0;
    public static final int REPEAT_LIST = 1;
    public static final int REPEAT_ONE = 2;
    public static final int SHUFFLE_PLAY = 3;

    private SharedPreferences prefs;
    //通知栏RemoteView
    private static RemoteViews remoteViewsSmall;
    private static RemoteViews remoteViewsLarge;
    //通知栏点击监听器
    private remoteMusicReceiver remoteMusicReceiver;
    private Notification notification;
    //播放器及其状态信息
    private static MediaPlayer mediaPlayer;
    private boolean prepared = false;
    private boolean enabled = false;
    //播放列表
    private List<MusicUtils.Song> songList;
    private int nowId;
    //0=顺序播放 1=列表循环 2=单曲循环 3=随机播放
    private int playOrder;
    //随机播放有关变量
    private List<Integer> shuffleOrder;
    private int shuffleId;
    //app歌词和桌面歌词的开关状态
    private boolean isAppLyric = false;
    private boolean isDesktopLyric = false;

     //MainActivity是否位于前台
    private boolean isActivityForeground = false;
     public int nowPageId = 0;  //MainActivity当前页id用于重建Activity时恢复到退出前状态
    //用于MainActivity和桌面歌词的状态回调
    private MusicServiceCallback musicServiceCallback;
    private DeskLyricCallback deskLyricCallback;
    //定时播放倒计时线程
    private TimingThread timingThread;

    //保存当前正在播放歌曲的信息，用于其他类随时读取
    private LyricUtils lyric = null;
    private MusicUtils.Metadata metadata = null;
    private Bitmap albumImage = null;

    //用于存储线控耳机的按键次数
    private final int[] keyTimes = new int[1];
    private KeyTimeThread keyTimeThread;
    /** 蓝牙或有线耳机断开后是否处于等待设备重新连接状态 */
    private boolean waitForDevice = false;
    //蓝牙或有线耳机的状态监听器
     private BluetoothStateReceiver mBluetoothStateReceiver;
     private HeadsetPlugReceiver mHeadsetPlugReceiver;
     private MediaButtonReceiver mediaButtonReceiver;
     private boolean webPlayMode;
     private int lastPosition=-1;

     //各种播放状态信息的读取与设置============================================
    public boolean isWebPlayMode() {
        return webPlayMode;
    }
    public boolean isPlaying() {
        return mediaPlayer.isPlaying();
    }
    public boolean isPrepared() {
        return prepared;
    }
    public boolean isEnabled() {
        return enabled;
    }
    public boolean isAppLyric() {
        return isAppLyric;
    }
    public int getCurrentPosition() {
        return mediaPlayer.getCurrentPosition();
    }
    public int getDuration() {
        if (isPrepared())
            return mediaPlayer.getDuration();
        else {
            if (songList.get(nowId).duration != null)
                return Integer.parseInt(songList.get(nowId).duration);
            else return 0;
        }

    }
    public List<MusicUtils.Song> getPlayList() {
        return songList;
    }
    public int getNowId() {
        return nowId;
    }
    public int getMaxID() {
        return songList.size() - 1;
    }
    public int getPlayOrder() {
        return playOrder;
    }
    public int getNowPageId() {
        return nowPageId;
    }
    public void setDesktopLyric(boolean desktopLyric) {
        isDesktopLyric = desktopLyric;
        updateNotificationShow(getNowId());
    }
    public void setActivityForeground(boolean activityForeground) {
        isActivityForeground = activityForeground;
        hideDesktopLyric(isActivityForeground || !isDesktopLyric);
    }
    public void setMusicServiceCallback(MusicServiceCallback musicServiceCallback) {
        this.musicServiceCallback = musicServiceCallback;
    }
    public void unregisterMusicServiceCallback(MusicServiceCallback musicServiceCallback) {
        if (this.musicServiceCallback == musicServiceCallback)
            this.musicServiceCallback = null;
    }
    public void setDeskLyricCallback(DeskLyricCallback deskLyricCallback) {
        this.deskLyricCallback = deskLyricCallback;
    }
    public void setEnabled(boolean isEnabled) {
        this.enabled = isEnabled;
    }
    public void setNowPageId(int id) {
        nowPageId = id;
    }
    public void setAppLyric(boolean appLyric) {
        isAppLyric = appLyric;
    }
     public LyricUtils getLyric() {
         return lyric;
     }
     public void setWebPlayMode(boolean webPlayMode) {
         this.webPlayMode = webPlayMode;
         readPlayList();
     }
     public MusicUtils.Metadata getMetadata() {
         return metadata;
     }
     public Bitmap getAlbumImage() {
         return albumImage;
     }
     public boolean isTiming() {
         if (timingThread != null)
             return timingThread.isTiming;
         else return false;
     }
//各种播放状态信息的读取与设置============================================

     @Override
     public void onCreate() {
         //初始化
         initializePlayer();
         initMemberData();
         initRemoteViews();
         initNotification();
         registerRemoteMusicReceiver();
         registerBluetoothReceiver();
         registerHeadsetPlugReceiver();
         mediaButtonReceiver = new MediaButtonReceiver(getApplicationContext(), this);
         updateNotificationShow(nowId);
     }


     /**
      * 设置MediaPlayer的播放进度
      * @param mSec 要定位到的毫秒数
      */
     public void setProgress(int mSec) {
         mediaPlayer.seekTo(mSec);
     }

    private void pause() {
        mediaPlayer.pause();
        updateNotificationShow(getNowId());
    }

    private void start() {
        if (prepared)
            mediaPlayer.start();
        updateNotificationShow(getNowId());
    }
 /**
  * 注册蓝牙监听器
  */
    private void registerBluetoothReceiver() {
        mBluetoothStateReceiver = new BluetoothStateReceiver();
        mBluetoothStateReceiver.setIBluetoothStateListener(new BluetoothStateReceiver.IBluetoothStateListener() {
            @Override
            public void onBluetoothDeviceConnected() {
                if (waitForDevice) {
                    setPlayOrPause(true);
                }
            }
            @Override
            public void onBluetoothDeviceDisconnected() {
                if (isPlaying()) {
                    waitForDevice = true;
                    setPlayOrPause(false);
                }
            }
        });
        //设置广播过滤器
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        intentFilter.addAction("android.bluetooth.BluetoothAdapter.STATE_OFF");
        intentFilter.addAction("android.bluetooth.BluetoothAdapter.STATE_ON");
        registerReceiver(mBluetoothStateReceiver, intentFilter);
    }
 /**
  * 注册有线耳机监听器
  */
    private void registerHeadsetPlugReceiver() {
        mHeadsetPlugReceiver = new HeadsetPlugReceiver();
        mHeadsetPlugReceiver.setIHeadsetPlugListener(new HeadsetPlugReceiver.IHeadsetPlugListener() {
            @Override
            public void onHeadsetPlugged() {
                if (waitForDevice) {
                    setPlayOrPause(true);
                }
            }

            @Override
            public void onHeadsetUnplugged() {
                if (isPlaying()) {
                    waitForDevice = true;
                    setPlayOrPause(false);
                }
            }
        });
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.HEADSET_PLUG");
        registerReceiver(mHeadsetPlugReceiver, intentFilter);
    }
 /**
  * 初始化成员变量
  */
    private void initMemberData() {
        lyric = new LyricUtils(this);
        metadata = new MusicUtils.Metadata();
        keyTimes[0] = 0;
        keyTimeThread = new KeyTimeThread();
        songList = new ArrayList<>();
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        isDesktopLyric = prefs.getBoolean("isShowLyric", false);
        nowId = Integer.parseInt(prefs.getString("nowId", "0"));
        playOrder = Integer.parseInt(prefs.getString("playOrder", "0"));
        webPlayMode = prefs.getBoolean("isUseWebPlayList", false);
        readPlayList();
        if (!webPlayMode) {
            lyric.LoadLyric(songList.get(nowId));
            albumImage = MusicUtils.getAlbumImage(songList.get(nowId).source_uri);
            metadata = MusicUtils.getMetadata(songList.get(nowId).source_uri);
        }
        setPlayOrder(playOrder);
    }

    @Override
    public void onDestroy() {
        if (remoteMusicReceiver != null) {
            //解除动态注册的广播
            unregisterReceiver(remoteMusicReceiver);
            closeNotification();
        }
        stopService(new Intent(MusicService.this, FloatLyricService.class));
        //mMediaSession.release();
        unregisterReceiver(mBluetoothStateReceiver);
        unregisterReceiver(mHeadsetPlugReceiver);
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        //Toast.makeText(this, "绑定音乐服务成功", Toast.LENGTH_SHORT).show();
        return new MyMusicBinder();
    }

    private void initializePlayer() {
        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.reset();
            AudioAttributes.Builder audioAttributesBuilder = new AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC);
            mediaPlayer.setAudioAttributes(audioAttributesBuilder.build());
        }
        //设置播放器状态监听器
        setMediaPlayerListener();
    }

    private void initNotification() {
        String channelId = "play_control";
        String channelName = "播放控制";
        int importance = NotificationManager.IMPORTANCE_HIGH;
        NotificationChannel channel = new NotificationChannel(channelId, channelName, importance);
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.createNotificationChannel(channel);

        PendingIntent contentIntent = PendingIntent.getActivity(
                this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);

        //初始化通知
        notification = new NotificationCompat.Builder(this, "play_control")
                .setContentIntent(contentIntent)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.drawable.ic_baseline_music_note_24)
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .setCustomContentView(remoteViewsSmall)
                .setCustomBigContentView(remoteViewsLarge)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(false)
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .build();
    }

    private void updateNotificationShow(int position) {
        //封面专辑
        Bitmap bitmap = albumImage;
        if (bitmap == null&&position!=lastPosition) {
            remoteViewsSmall.setImageViewResource(R.id.iv_album_cover, R.drawable.ic_baseline_music_note_24);
            remoteViewsLarge.setImageViewResource(R.id.iv_album_cover, R.drawable.ic_baseline_music_note_24);
        } else {
            //remoteViewsSmall.setImageViewBitmap(R.id.iv_album_cover, bitmap);
            //remoteViewsLarge.setImageViewBitmap(R.id.iv_album_cover, bitmap);
            lastPosition=position;
        }
        //歌曲名
        remoteViewsSmall.setTextViewText(R.id.tv_notification_song_name, songList.get(position).title);
        remoteViewsLarge.setTextViewText(R.id.tv_notification_song_name, songList.get(position).title);
        //歌手名
        remoteViewsSmall.setTextViewText(R.id.tv_notification_singer, songList.get(position).artist +
                (songList.get(position).album.equals("null") ? "" : (" - " + songList.get(position).album)));
        remoteViewsLarge.setTextViewText(R.id.tv_notification_singer, songList.get(position).artist +
                (songList.get(position).album.equals("null") ? "" : (" - " + songList.get(position).album)));
        //桌面歌词
        if (isDesktopLyric) {
            remoteViewsSmall.setImageViewResource(R.id.btn_notification_lyric, R.drawable.ic_baseline_subtitles_green_24);
            remoteViewsLarge.setImageViewResource(R.id.btn_notification_lyric, R.drawable.ic_baseline_subtitles_green_24);
        } else {
            remoteViewsSmall.setImageViewResource(R.id.btn_notification_lyric, R.drawable.ic_baseline_subtitles_24);
            remoteViewsLarge.setImageViewResource(R.id.btn_notification_lyric, R.drawable.ic_baseline_subtitles_24);
        }
        //发送通知
        //播放状态判断
        if (isPlaying()) {
            remoteViewsSmall.setImageViewResource(R.id.btn_notification_play, R.drawable.ic_round_pause_24);
            remoteViewsLarge.setImageViewResource(R.id.btn_notification_play, R.drawable.ic_round_pause_24);
            startForeground(NOTIFICATION_ID, notification);
        } else if (isPrepared() || !isEnabled()) {
            remoteViewsSmall.setImageViewResource(R.id.btn_notification_play, R.drawable.ic_round_play_arrow_24);
            remoteViewsLarge.setImageViewResource(R.id.btn_notification_play, R.drawable.ic_round_play_arrow_24);
            startForeground(NOTIFICATION_ID, notification);
            stopForeground(false);
        } else {
            remoteViewsSmall.setImageViewResource(R.id.btn_notification_play, R.drawable.ic_round_arrow_downward_24);
            remoteViewsLarge.setImageViewResource(R.id.btn_notification_play, R.drawable.ic_round_arrow_downward_24);
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    /**
     * 关闭音乐通知栏
     */
    private void closeNotification() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
            }
        }
        //停止前台服务并移除通知
        stopForeground(true);
    }

    private void initRemoteViews() {
        remoteViewsSmall = new RemoteViews(this.getPackageName(), R.layout.notification_small);
        remoteViewsLarge = new RemoteViews(this.getPackageName(), R.layout.notification_large);
        //通知栏控制器上一首按钮广播操作
        Intent intentStop = new Intent(STOP);
        PendingIntent stopPendingIntent = PendingIntent.getBroadcast(this, 0, intentStop, PendingIntent.FLAG_MUTABLE);
        //为prev控件注册事件
        remoteViewsSmall.setOnClickPendingIntent(R.id.btn_notification_stop, stopPendingIntent);
        remoteViewsLarge.setOnClickPendingIntent(R.id.btn_notification_stop, stopPendingIntent);

        //通知栏控制器上一首按钮广播操作
        Intent intentPrev = new Intent(PREV);
        PendingIntent prevPendingIntent = PendingIntent.getBroadcast(this, 0, intentPrev, PendingIntent.FLAG_MUTABLE);
        //为prev控件注册事件
        remoteViewsSmall.setOnClickPendingIntent(R.id.btn_notification_previous, prevPendingIntent);
        remoteViewsLarge.setOnClickPendingIntent(R.id.btn_notification_previous, prevPendingIntent);

        //通知栏控制器播放暂停按钮广播操作  //用于接收广播时过滤意图信息
        Intent intentPlay = new Intent(PLAY);
        PendingIntent playPendingIntent = PendingIntent.getBroadcast(this, 0, intentPlay, PendingIntent.FLAG_MUTABLE);
        //为play控件注册事件
        remoteViewsSmall.setOnClickPendingIntent(R.id.btn_notification_play, playPendingIntent);
        remoteViewsLarge.setOnClickPendingIntent(R.id.btn_notification_play, playPendingIntent);

        //通知栏控制器下一首按钮广播操作
        Intent intentNext = new Intent(NEXT);
        PendingIntent nextPendingIntent = PendingIntent.getBroadcast(this, 0, intentNext, PendingIntent.FLAG_MUTABLE);
        //为next控件注册事件
        remoteViewsSmall.setOnClickPendingIntent(R.id.btn_notification_next, nextPendingIntent);
        remoteViewsLarge.setOnClickPendingIntent(R.id.btn_notification_next, nextPendingIntent);

        //通知栏控制器关闭按钮广播操作
        Intent intentClose = new Intent(CLOSE);
        PendingIntent closePendingIntent = PendingIntent.getBroadcast(this, 0, intentClose, PendingIntent.FLAG_MUTABLE);
        //为close控件注册事件
        remoteViewsSmall.setOnClickPendingIntent(R.id.btn_notification_close, closePendingIntent);
        remoteViewsLarge.setOnClickPendingIntent(R.id.btn_notification_close, closePendingIntent);

        //通知栏控制器切换歌词开关操作
        Intent intentLyric = new Intent(LYRIC);
        PendingIntent lyricPendingIntent = PendingIntent.getBroadcast(this, 0, intentLyric, PendingIntent.FLAG_MUTABLE);
        //为lyric控件注册事件
        remoteViewsSmall.setOnClickPendingIntent(R.id.btn_notification_lyric, lyricPendingIntent);
        remoteViewsLarge.setOnClickPendingIntent(R.id.btn_notification_lyric, lyricPendingIntent);

    }

    public void showDesktopLyric() {
        Intent intent = new Intent(MusicService.this, FloatLyricService.class);
        if (isDesktopLyric) {
            stopService(intent);
            isDesktopLyric = false;
        } else {
            if (!isActivityForeground) {
                startService(intent);
            }
            isDesktopLyric = true;
        }
        Log.d(TAG, String.valueOf(isDesktopLyric));
        updateNotificationShow(getNowId());
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("isShowLyric", isDesktopLyric);
        editor.apply();
    }

     /**
      * 显示或因此桌面歌词
      * @param isHide 是否隐藏桌面歌词
      */
    public void hideDesktopLyric(boolean isHide) {
        Intent intent = new Intent(MusicService.this, FloatLyricService.class);
        if (isHide) {
            stopService(intent);
        } else {
            startService(intent);
        }
    }

     /**
      * 更新桌面歌词的播放信息
      */
    public void updateDeskLyricPlayInfo() {
        if (deskLyricCallback != null)
            deskLyricCallback.updateNowPlaying(getNowId());
    }

     /**
      * 设置暂停或播放
      * @param isPlay 是否播放
      */
    public void setPlayOrPause(boolean isPlay) {
        if (enabled && !prepared)//播放器还在准备
            return;
        if (isPlay) {//要切换成播放
            waitForDevice = false;
            if (!isEnabled()) {//处于停止状态
                setEnabled(true);
                startToPrepare(getNowId());
            } else if (isPrepared()) {//处于准备完毕状态
                start();
                if (musicServiceCallback != null)
                    musicServiceCallback.updatePlayStateThis();
                if (deskLyricCallback != null)
                    deskLyricCallback.updatePlayState();
            } else {//处于空闲
                startToPrepare(getNowId());
            }
        } else {//要切换为暂停
            pause();
            if (musicServiceCallback != null)
                musicServiceCallback.updatePlayStateThis();
            if (deskLyricCallback != null)
                deskLyricCallback.updatePlayState();
        }
    }

    /**
     * 注册动态广播
     */
    private void registerRemoteMusicReceiver() {
        remoteMusicReceiver = new remoteMusicReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(PLAY);
        intentFilter.addAction(PREV);
        intentFilter.addAction(STOP);
        intentFilter.addAction(NEXT);
        intentFilter.addAction(CLOSE);
        intentFilter.addAction(LYRIC);
        registerReceiver(remoteMusicReceiver, intentFilter);
    }
     /**
      * 根据当前播放顺序计算下一首应该播放的歌曲id
      * @param isNext 是否是下一首（false表示上一首）
      */
    public void playPrevOrNext(boolean isNext) {
        Log.e(TAG, "enabled:" + enabled + " prepared:" + prepared);
        if (enabled && !prepared) return;
        int maxId = getMaxID();
        int nowId = getNowId();
        int order = getPlayOrder();
        switch (order) {
            case SHUFFLE_PLAY:  //随机播放
                if (isNext) {//列表内循环
                    if (shuffleId < shuffleOrder.size() - 1) {
                        shuffleId += 1;
                    } else {
                        shuffleId = 0;
                    }
                } else {
                    if (shuffleId > 0) {
                        shuffleId -= 1;
                    } else {
                        shuffleId = shuffleOrder.size() - 1;
                    }
                }
                nowId = shuffleOrder.get(shuffleId);
                break;
            case REPEAT_ONE:    //单曲循环，播放id不需要改动
                break;
            case REPEAT_LIST:   //列表循环
                if (isNext) {
                    if (nowId < maxId) nowId += 1;
                    else nowId = 0;
                } else {
                    if (nowId > 0) nowId -= 1;
                    else nowId = maxId;
                }
                break;
            default:
            case LIST_PLAY: //按列表播放，最后一首放完后暂停
                if (isNext) {
                    if (nowId < maxId)
                        nowId += 1;
                    else {  //停止播放
                        setPlayOrPause(false);
                        setEnabled(false);
                        return;
                    }
                } else {
                    if (nowId > 0)
                        nowId -= 1;
                    else
                        nowId = 0;
                }
                break;
        }
        //根据计算得出的id播放
        startToPrepare(nowId);
    }
 /**
  * 设置播放器状态监听器
  */
    private void setMediaPlayerListener() {
        //MediaPlayer准备资源的监听器
        mediaPlayer.setOnPreparedListener(mediaPlayer -> {
            prepared = true;
            mediaPlayer.start();
            //通知注册的监听器初始化状态
            if (musicServiceCallback != null)
                musicServiceCallback.nowPlayingThis(getNowId());
            if (musicServiceCallback != null)
                musicServiceCallback.updatePlayStateThis();
            if (deskLyricCallback != null)
                deskLyricCallback.updatePlayState();
            updateNotificationShow(nowId);  //更新通知
            SharedPreferences.Editor spEditor = prefs.edit();
            spEditor.putString("nowId", String.valueOf(nowId));
            spEditor.apply();
        });
        //音频播放完成的监听器
        mediaPlayer.setOnCompletionListener(mediaPlayer -> {
            //把所有的都回归到0
            if (prepared) {
                if (enabled)
                    //下一首
                    playPrevOrNext(true);
                else
                    prepared = false;
            }
        });
    }
 /**
  * 重新读取播放列表
  */
    public void refreshPlayList() {
        readPlayList();
    }
 /**
  * 从存储中读取播放列表
  */
    private void readPlayList() {
        String defaultPlayList = "[{\"id\":-1,\"title\":\"这是音乐标题\",\"artist\":\"这是歌手\",\"album\":\"这是专辑名\",\"memory\":\"此为测试数据，添加音乐文件后自动删除\"," +
                "\"source_uri\":\"file:///storage/emulated/0/Android/data/" + getPackageName() + "/Music/这是歌手 - 这是音乐标题.mp3\"," +
                "\"lyric_uri\":\"file:///storage/emulated/0/Android/data/" + getPackageName() + "/Music/这是歌手 - 这是音乐标题.lrc\",\"duration\":\"0\"}]";
        String playListJson;
        if (!webPlayMode)   //根据webmode选择读取的列表
            playListJson = prefs.getString("playList", defaultPlayList);
        else
            playListJson = prefs.getString("webPlayList", defaultPlayList);
        Gson gson = new Gson();
        Type playListType = new TypeToken<ArrayList<MusicUtils.Song>>() {
        }.getType();
        songList = gson.fromJson(playListJson, playListType);
        //列表为空时修正，防止播放列表空指针
        if (songList == null || songList.size() == 0) {
            playListJson = defaultPlayList;
            songList = gson.fromJson(playListJson, playListType);
        }
        if (nowId >= songList.size()) nowId = 0;
    }
 /**
  * 设置新的播放列表
  * @param newSongList 新的播放列表
  */
    public void setPlayList(List<MusicUtils.Song> newSongList) {
        songList = newSongList;
        savePlayList();
    }
 /**
  * 保存当前播放列表
  */
    private void savePlayList() {
        if (!webPlayMode) {
            MusicUtils.savePlayList(this,this.songList);
        }
    }

    //添加音乐列表
    public void addMusicList(List<MusicUtils.Song> songList) {
        MusicUtils.addMusicToList(this,songList,this.songList);
    }

    //添加音乐
    public void addMusic(String path) {
        MusicUtils.addMusic(this,path,this.songList);
        savePlayList();
    }
 /**
  * 设置播放顺序
  * @param order 播放顺序id
  */
    public void setPlayOrder(int order) {
        playOrder = order;
        savePlayOrder();
        if (playOrder == SHUFFLE_PLAY) {    //新播放顺序为随机播放时生成伪随机播放列表
            mediaPlayer.setLooping(false);
            shuffleOrder = new ArrayList<>();
            for (int i = 0; i < songList.size(); i++) {
                if (i != nowId)
                    shuffleOrder.add(i);
            }
            Collections.shuffle(shuffleOrder);
            shuffleOrder.add(nowId);
            shuffleId = shuffleOrder.size() - 1;
        } else
            //当播放顺序为单曲循环时，设置mediaPlayer循环标志
            mediaPlayer.setLooping(playOrder == REPEAT_ONE);
    }

    private void savePlayOrder() {
        SharedPreferences.Editor spEditor = prefs.edit();
        spEditor.putString("playOrder", String.valueOf(playOrder));
        spEditor.apply();
    }
 /**
  * 播放指定id的音乐
  * @param musicId 要播放的音乐在播放列表中的id
  */
    public void playThisFromList(int musicId) {
        Log.e(TAG, "enabled:" + enabled + " prepared:" + prepared);
        if (!enabled ||prepared) startToPrepare(musicId);
    }
 /**
  * 播放准备工作，向注册的监听器反馈播放准备状态
  * @param musicId 要播放的音乐在播放列表中的id
  */
    private void startToPrepare(int musicId) {
        switch (prepareThis(musicId)) {
            case 0:
                if (playOrder == REPEAT_ONE)
                    mediaPlayer.setLooping(true);
                if (musicServiceCallback != null)
                    musicServiceCallback.nowLoadingThis(musicId);
                if (deskLyricCallback != null)
                    deskLyricCallback.updateNowPlaying(musicId);
                break;
            default:
            case -1:
                if (musicServiceCallback != null)
                    musicServiceCallback.playingErrorThis(musicId);
                if (deskLyricCallback != null)
                    deskLyricCallback.updateNowPlaying(musicId);
                setEnabled(false);
                break;
        }
        updateNotificationShow(nowId);
    }
 /**
  * 准备播放包括在线歌曲的缓冲
  * @param id 要播放的音乐在播放列表中的id
  * @return int 0：成功 -1：失败
  */
    private int prepareThis(int id) {
        prepared = false;
        mediaPlayer.reset();
        int reId;
        nowId = id;
        if (nowId > getMaxID()) {
            reId = -1;
        } else {
            lyric.LoadLyric(songList.get(id));
            if (FileUtils.isFileExists(songList.get(id).source_uri)) {  //如果path是本地文件且文件存在
                //加载资源信息
                metadata = MusicUtils.getMetadata(songList.get(id).source_uri);
                startPlay(songList.get(id).source_uri);
                reId = 0;
            } else if (songList.get(id).source_uri.matches("^(https?)://[-A-Za-z0-9+&@#/%?=~_|!:,.;]+[-A-Za-z0-9+&@#/%=~_|]+[\\S\\s]*")) {
                //如果path的格式符合HTTP URL
                metadata = MusicUtils.getMetadataFromSong(songList.get(id));
                //开始下载音乐文件
                UploadDownloadUtils uploadDownloadUtils = new UploadDownloadUtils(this);
                //设置下载状态监听器
                uploadDownloadUtils.set0nDownloadListener(new UploadDownloadUtils.OnDownloadListener() {
                    @Override
                    public void onFileDownloadCompleted(ArrayList<String> array) {
                        if (!array.get(0).equals(songList.get(id).source_uri))
                            return;
                        //下载完成
                        startPlay(array.get(1));
                    }

                    @Override
                    public void onFileDownloading(ArrayList<String> array) {
                    }

                    @Override
                    public void onFileDownloadError(ArrayList<String> array) {
                        if (!array.get(0).equals(songList.get(id).source_uri))
                            return;
                        //下载失败，通知监听器失败
                        if (musicServiceCallback != null)
                            musicServiceCallback.playingErrorThis(nowId);
                        if (deskLyricCallback != null)
                            deskLyricCallback.updateNowPlaying(nowId);
                        setEnabled(false);
                    }
                });
                //开始下载
                uploadDownloadUtils.downloadFile(PathUtils.getExternalAppCachePath(), TimeUtils.getNowMills() + ".tmp", songList.get(id).source_uri);
                reId = 0;
            } else {
                reId = -1;
            }
        }
        return reId;
    }
 /**
  * 开始播放
  * @param path 音乐文件路径
  */
    private void startPlay(String path) {
        try {
            Log.e(TAG, path);
            albumImage = MusicUtils.getAlbumImage(path);
            metadata = MusicUtils.getMetadata(path);
            mediaPlayer.reset();
            mediaPlayer.setDataSource(this, Uri.parse(path));
            mediaPlayer.prepare();
        } catch (IOException e) {
            e.printStackTrace();
            mediaPlayer.reset();
            if (musicServiceCallback != null)
                musicServiceCallback.playingErrorThis(nowId);
            if (deskLyricCallback != null)
                deskLyricCallback.updateNowPlaying(nowId);
            setEnabled(false);
        }
    }

     /**
      * 处理线控点击事件
      * @param keyAction
      */
    @Override
    public void onKeyDown(int keyAction) {
        switch (keyAction) {
            case MediaButtonReceiver.KeyActions.PLAY_ACTION:
                Log.d(TAG, "播放...");
                setPlayOrPause(true);
                break;
            case MediaButtonReceiver.KeyActions.PAUSE_ACTION:
                Log.d(TAG, "暂停...");
                setPlayOrPause(false);
                break;
            case MediaButtonReceiver.KeyActions.PREV_ACTION:
                Log.d(TAG, "上一首...");
                playPrevOrNext(false);
                break;
            case MediaButtonReceiver.KeyActions.NEXT_ACTION:
                Log.d(TAG, "下一首...");
                playPrevOrNext(true);
                break;
            case MediaButtonReceiver.KeyActions.KEYCODE_HEADSETHOOK:    //耳机线控
                if (keyTimes[0] != 0)
                    keyTimeThread.interrupt();
                keyTimeThread = new KeyTimeThread();
                keyTimeThread.start();
                if (keyTimes[0] >= 6)   //超过6次归0
                    keyTimes[0] = 0;
                else
                    keyTimes[0]++;//按下抬起有两次触发，后面实际按下次数为1/2
                break;
        }
    }
 /**
  * 用于根据用户按键后延迟400ms统计按下总次数
  * @author         Brownlzy
  */
    private class KeyTimeThread extends Thread {
        @Override
        public void run() {
            try {
                Thread.sleep(400);  //延迟400ms等待下一个输入
                if (keyTimes[0] == 2)
                    keyHandler.sendEmptyMessage(1);//单机
                else if (keyTimes[0] == 4)
                    keyHandler.sendEmptyMessage(2);//双击
                else if (keyTimes[0] == 6)
                    keyHandler.sendEmptyMessage(3);//三击
                keyTimes[0] = 0;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
/** 用于根据反馈的次数实现功能响应 */
    private final Handler keyHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                setPlayOrPause(!isPlaying());   //单击暂停
            } else if (msg.what == 2) {
                playPrevOrNext(true);   //双击下一首
            } else if (msg.what == 3) {
                playPrevOrNext(false);  //三击上一首
            }
        }
    };
 /**
  * 根据通知栏按钮动作执行功能
  * @author         Brownlzy
  */
    public class remoteMusicReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            //UI控制
            UIControl(intent.getAction());
        }

        /**
         * 页面的UI 控制 ，通过服务来控制页面和通知栏的UI
         * @param state 状态码
         */
        private void UIControl(String state) {
            switch (state) {
                case PLAY:
                    setPlayOrPause(!isPlaying());
                    break;
                case PREV:
                    playPrevOrNext(false);
                    break;
                case NEXT:
                    playPrevOrNext(true);
                    break;
                case LYRIC:
                    showDesktopLyric();
                    break;
                case STOP:
                    stopForeground(true);
                    System.exit(0);
                    break;
                default:
                    break;
            }
            //updateNotificationShow(getNowId());
        }
    }

    public class MyMusicBinder extends Binder {
        //返回Service对象
        public MusicService getService() {
            return MusicService.this;
        }
    }
 /**
  * 用于实现定时播放的倒计时线程
  * @author         Brownlzy
  */
    private class TimingThread extends Thread {
        public boolean isTiming = false;

        @Override
        public void run() {
            super.run();
            int timing; //倒计时分钟数
            do {
                try {
                    //读取定时信息
                    timing = prefs.getInt("timing", 0);
                    Log.e(TAG, "timing" + timing);
                    if (timing > 0) {
                        isTiming = true;
                        Thread.sleep(60000);    //计时1分钟
                        timing--;   //倒计时分钟数-1
                        SharedPreferences.Editor editor = prefs.edit(); //保存新的时间
                        editor.putInt("timing", timing);
                        editor.apply();
                    }
                } catch (InterruptedException | NullPointerException e) {
                    return;
                }
            } while (timing > 0);   //倒计时未结束循环
            setPlayOrPause(false);  //结束后暂停播放
            isTiming = false;   //倒计时标志
        }
    }
 /**
  * 启动倒计时
  */
    public void startTiming() {
        if (timingThread != null) {
            timingThread.interrupt();
        }
        timingThread = new TimingThread();
        timingThread.start();
    }

     /**
      * 停止倒计时
      */
    public void stopTiming() {
        if (timingThread != null)
            timingThread.interrupt();
    }
}