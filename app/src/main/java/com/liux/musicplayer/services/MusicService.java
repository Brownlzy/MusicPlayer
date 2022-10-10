package com.liux.musicplayer.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
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
import android.support.v4.media.session.MediaSessionCompat;
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
import com.liux.musicplayer.receiver.MediaButton2Receiver;
import com.liux.musicplayer.activities.MainActivity;
import com.liux.musicplayer.utils.LyricUtils;
import com.liux.musicplayer.utils.MusicUtils;
import com.liux.musicplayer.utils.UploadDownloadUtils;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class MusicService extends Service implements MediaButton2Receiver.IKeyDownListener {
    public static final String PLAY = "play";
    public static final String PAUSE = "pause";
    public static final String PREV = "prev";
    public static final String NEXT = "next";
    public static final String CLOSE = "close";
    public static final String LYRIC = "lyric";
    public static final String TAG = "MusicService";
    public static final int NOTIFICATION_ID = 1;
    public static final int LIST_PLAY = 0;
    public static final int REPEAT_LIST = 1;
    public static final int REPEAT_ONE = 2;
    public static final int SHUFFLE_PLAY = 3;

    private SharedPreferences prefs;
    private static RemoteViews remoteViewsSmall;
    private static RemoteViews remoteViewsLarge;
    private MusicReceiver musicReceiver;
    private Notification notification;

    private static MediaPlayer mediaPlayer;
    private boolean prepared = false;
    private boolean enabled = false;
    private List<MusicUtils.PlayList> allPlayList;
    private List<String> allPlayListName;
    private List<MusicUtils.Song> allSongList;
    private List<MusicUtils.Song> playingList;
    private int nowId;
    //0=顺序播放 1=列表循环 2=单曲循环 3=随机播放
    private int playOrder;
    private List<Integer> shuffleOrder;
    private int shuffleId;
    private boolean isAppLyric = false;

    private boolean isDesktopLyric = false;
    public int nowPageId = 0;
    private MediaSessionCompat mMediaSession;
    private boolean isActivityForeground = false;
    private MusicServiceCallback musicServiceCallback;
    private DeskLyricCallback deskLyricCallback;
    private TimingThread timingThread;

    private LyricUtils lyric = null;
    private MusicUtils.Metadata metadata = null;
    private Bitmap albumImage = null;
    private MediaButton2Receiver mediaButtonReceiver;
    private int[] keyTimes = new int[1];
    private KeyTimeThread keyTimeThread;
    private BluetoothStateReceiver mBluetoothStateReceiver;
    private boolean waitForDevice = false;
    private HeadsetPlugReceiver mHeadsetPlugReceiver;

    public boolean isWebPlayMode() {
        return webPlayMode;
    }

    private boolean webPlayMode;

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
            if (playingList.get(nowId).duration != null)
                return Integer.parseInt(playingList.get(nowId).duration);
            else return 0;
        }

    }

    public List<MusicUtils.Song> getPlayingList() {
        return playingList;
    }

    public int getNowId() {
        return nowId;
    }

    public int getMaxID() {
        return playingList.size() - 1;
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

    public void setProgress(int mSec) {
        mediaPlayer.seekTo(mSec);
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

    private void pause() {
        mediaPlayer.pause();
        updateNotificationShow(getNowId());
    }

    private void start() {
        if (prepared)
            mediaPlayer.start();
        updateNotificationShow(getNowId());
    }

    @Override
    public void onCreate() {
        //lyric = new LyricUtils();
        metadata = new MusicUtils.Metadata();
        initializePlayer();
        initMemberData();
        initRemoteViews();
        initNotification();
        registerMusicReceiver();
        //registerRemoteControlReceiver();
        registerBluetoothReceiver();
        registerHeadsetPlugReceiver();
        updateNotificationShow(nowId);
    }

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
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        intentFilter.addAction("android.bluetooth.BluetoothAdapter.STATE_OFF");
        intentFilter.addAction("android.bluetooth.BluetoothAdapter.STATE_ON");
        registerReceiver(mBluetoothStateReceiver, intentFilter);
    }

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

    private void initMemberData() {
        keyTimes[0] = 0;
        keyTimeThread = new KeyTimeThread();
        allSongList = new ArrayList<>();
        allPlayList = new ArrayList<>();
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        isDesktopLyric = prefs.getBoolean("isShowLyric", false);
        nowId = Integer.parseInt(prefs.getString("nowId", "0"));
        playOrder = Integer.parseInt(prefs.getString("playOrder", "0"));
        webPlayMode = prefs.getBoolean("isUseWebPlayList", false);
        readAllSongList();
        readAllPlayList();
        if (!webPlayMode) {
            //lyric.LoadLyric(playingList.get(nowId));
            albumImage = MusicUtils.getAlbumImage(this, playingList.get(nowId).source_uri);
            metadata = MusicUtils.getMetadata(this, playingList.get(nowId).source_uri);
        }
        setPlayOrder(playOrder);
    }

    @Override
    public void onDestroy() {
        if (musicReceiver != null) {
            //解除动态注册的广播
            unregisterReceiver(musicReceiver);
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

    private void registerRemoteControlReceiver() {
        ComponentName mbr = new ComponentName(getPackageName(), MusicService.class.getName());
        mMediaSession = new MediaSessionCompat(this, "mbr", mbr, null);
        mMediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public boolean onMediaButtonEvent(Intent intent) {
                //在这里就可以接收到（线控、蓝牙耳机的按键事件了）

                //通过intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);获取按下的按键实现自己对应功能
                Log.e(TAG, String.valueOf(intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT)));
                if (intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT).toString().contains("ACTION_UP")) {
                    setPlayOrPause(!isPlaying());
                    updateNotificationShow(getNowId());
                }
                //返回true表示不让别的程序继续处理这个广播
                return true;
            }
        });
        if (!mMediaSession.isActive()) {
            mMediaSession.setActive(true);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        //Toast.makeText(this, "绑定音乐服务成功", Toast.LENGTH_SHORT).show();
        return new MyMusicBinder();
    }

    @Override
    public void onRebind(Intent intent) {
        //Toast.makeText(this, "重新绑定音乐服务成功", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        //松绑Service，会触发onDestroy()
        return true;
    }

    private void initializePlayer() {
        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.reset();
            AudioAttributes.Builder audioAttributesBuilder = new AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC);
            mediaPlayer.setAudioAttributes(audioAttributesBuilder.build());
            mediaButtonReceiver = new MediaButton2Receiver(getApplicationContext(), this);
        }
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
                this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        //初始化通知
        notification = new NotificationCompat.Builder(this, "play_control")
                .setContentIntent(contentIntent)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.drawable.ic_baseline_music_note_24)
                //.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_baseline_music_note_24))
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
        if (bitmap == null) {
            remoteViewsSmall.setImageViewResource(R.id.iv_album_cover, R.drawable.ic_baseline_music_note_24);
            remoteViewsLarge.setImageViewResource(R.id.iv_album_cover, R.drawable.ic_baseline_music_note_24);
        } else {
            remoteViewsSmall.setImageViewBitmap(R.id.iv_album_cover, bitmap);
            remoteViewsLarge.setImageViewBitmap(R.id.iv_album_cover, bitmap);
        }
        //歌曲名
        remoteViewsSmall.setTextViewText(R.id.tv_notification_song_name, playingList.get(position).title);
        remoteViewsLarge.setTextViewText(R.id.tv_notification_song_name, playingList.get(position).title);
        //歌手名
        remoteViewsSmall.setTextViewText(R.id.tv_notification_singer, playingList.get(position).artist +
                (playingList.get(position).album.equals("null") ? "" : (" - " + playingList.get(position).album)));
        remoteViewsLarge.setTextViewText(R.id.tv_notification_singer, playingList.get(position).artist +
                (playingList.get(position).album.equals("null") ? "" : (" - " + playingList.get(position).album)));
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
        //manager.cancel(NOTIFICATION_ID);
        stopForeground(true);
    }

    private void initRemoteViews() {
        remoteViewsSmall = new RemoteViews(this.getPackageName(), R.layout.notification_small);
        remoteViewsLarge = new RemoteViews(this.getPackageName(), R.layout.notification_large);
        //通知栏控制器上一首按钮广播操作
        Intent intentPrev = new Intent(PREV);
        PendingIntent prevPendingIntent = PendingIntent.getBroadcast(this, 0, intentPrev, PendingIntent.FLAG_IMMUTABLE);
        //为prev控件注册事件
        remoteViewsSmall.setOnClickPendingIntent(R.id.btn_notification_previous, prevPendingIntent);
        remoteViewsLarge.setOnClickPendingIntent(R.id.btn_notification_previous, prevPendingIntent);

        //通知栏控制器播放暂停按钮广播操作  //用于接收广播时过滤意图信息
        Intent intentPlay = new Intent(PLAY);
        PendingIntent playPendingIntent = PendingIntent.getBroadcast(this, 0, intentPlay, PendingIntent.FLAG_IMMUTABLE);
        //为play控件注册事件
        remoteViewsSmall.setOnClickPendingIntent(R.id.btn_notification_play, playPendingIntent);
        remoteViewsLarge.setOnClickPendingIntent(R.id.btn_notification_play, playPendingIntent);

        //通知栏控制器下一首按钮广播操作
        Intent intentNext = new Intent(NEXT);
        PendingIntent nextPendingIntent = PendingIntent.getBroadcast(this, 0, intentNext, PendingIntent.FLAG_IMMUTABLE);
        //为next控件注册事件
        remoteViewsSmall.setOnClickPendingIntent(R.id.btn_notification_next, nextPendingIntent);
        remoteViewsLarge.setOnClickPendingIntent(R.id.btn_notification_next, nextPendingIntent);

        //通知栏控制器关闭按钮广播操作
        Intent intentClose = new Intent(CLOSE);
        PendingIntent closePendingIntent = PendingIntent.getBroadcast(this, 0, intentClose, PendingIntent.FLAG_IMMUTABLE);
        //为close控件注册事件
        remoteViewsSmall.setOnClickPendingIntent(R.id.btn_notification_close, closePendingIntent);
        remoteViewsLarge.setOnClickPendingIntent(R.id.btn_notification_close, closePendingIntent);

        //通知栏控制器切换歌词开关操作
        Intent intentLyric = new Intent(LYRIC);
        PendingIntent lyricPendingIntent = PendingIntent.getBroadcast(this, 0, intentLyric, PendingIntent.FLAG_IMMUTABLE);
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

    public void hideDesktopLyric(boolean isHide) {
        Intent intent = new Intent(MusicService.this, FloatLyricService.class);
        if (isHide) {
            stopService(intent);
        } else {
            startService(intent);
        }
    }

    public void updateDeskLyricPlayInfo() {
        if (deskLyricCallback != null)
            deskLyricCallback.updatePlayState(getNowId());
    }

    public void setPlayOrPause(boolean isPlay) {
        if (enabled && !prepared)
            return;
        if (isPlay) {
            waitForDevice = false;
            if (!isEnabled()) {
                setEnabled(true);
                playThisNow(getNowId());
            } else if (isPrepared()) {
                start();
                if (musicServiceCallback != null)
                    musicServiceCallback.updatePlayStateThis();
                if (deskLyricCallback != null)
                    deskLyricCallback.updatePlayState();
            } else {
                playThisNow(getNowId());
            }
        } else {
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
    private void registerMusicReceiver() {
        musicReceiver = new MusicReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(PLAY);
        intentFilter.addAction(PREV);
        intentFilter.addAction(NEXT);
        intentFilter.addAction(CLOSE);
        intentFilter.addAction(LYRIC);
        registerReceiver(musicReceiver, intentFilter);
    }

    public void playPrevOrNext(boolean isNext) {
        Log.e(TAG, "enabled:" + enabled + " prepared:" + prepared);
        if (enabled && !prepared) return;
        int order = getPlayOrder();
        switch (order) {
            case SHUFFLE_PLAY:
                if (isNext) {
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
            case REPEAT_ONE:
            case REPEAT_LIST:
                if (isNext) {
                    if (nowId < getMaxID()) nowId += 1;
                    else nowId = 0;
                } else {
                    if (nowId > 0) nowId -= 1;
                    else nowId = getMaxID();
                }
                break;
            default:
            case LIST_PLAY:
                if (isNext) {
                    if (nowId < getMaxID())
                        nowId += 1;
                    else {
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
        playThisNow(nowId);
    }

    private void setMediaPlayerListener() {
        //MediaPlayer准备资源的监听器
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                prepared = true;
                mediaPlayer.start();
                if (musicServiceCallback != null)
                    musicServiceCallback.nowPlayingThis(getNowId());
                if (musicServiceCallback != null)
                    musicServiceCallback.updatePlayStateThis();
                if (deskLyricCallback != null)
                    deskLyricCallback.updatePlayState();
                updateNotificationShow(nowId);
                SharedPreferences.Editor spEditor = prefs.edit();
                spEditor.putString("nowId", String.valueOf(nowId));
                spEditor.apply();
            }
        });
        //音频播放完成的监听器
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                //把所有的都回归到0
                if (prepared) {
                    if (enabled)
                        playPrevOrNext(true);
                    else
                        prepared = false;
                }
            }
        });
    }

    private void readAllPlayList() {
        String defaultAllPlayList = "[]";
        String allPlayListJson;
        if (!webPlayMode)
            allPlayListJson = prefs.getString("allPlayList", defaultAllPlayList);
        else
            allPlayListJson = prefs.getString("webAllPlayList", defaultAllPlayList);
        Gson gson = new Gson();
        Type allPlayListType = new TypeToken<ArrayList<MusicUtils.Song>>() {
        }.getType();
        allPlayList = gson.fromJson(allPlayListJson, allPlayListType);
        //错误修正，防止播放列表空指针
        if (allPlayList == null || allPlayList.size() == 0) {
            allPlayListJson = defaultAllPlayList;
            allPlayList = gson.fromJson(allPlayListJson, allPlayListType);
        }
    }
    private void readAllSongList() {
        String defaultPlayList = "[{\"id\":-1,\"title\":\"这是音乐标题\",\"artist\":\"这是歌手\",\"album\":\"这是专辑名\",\"memory\":\"此为测试数据，添加音乐文件后自动删除\"," +
                "\"source_uri\":\"file:///storage/emulated/0/Android/data/" + getPackageName() + "/Music/这是歌手 - 这是音乐标题.mp3\"," +
                "\"lyric_uri\":\"file:///storage/emulated/0/Android/data/" + getPackageName() + "/Music/这是歌手 - 这是音乐标题.lrc\",\"duration\":\"0\"}]";
        String songListJson;
        String playingListJson;
        if (!webPlayMode) {
            playingListJson = prefs.getString("playingList", defaultPlayList);
            songListJson = prefs.getString("allSongList", defaultPlayList);
        }else {
            playingListJson = prefs.getString("webPlayingList", defaultPlayList);
            songListJson = prefs.getString("webAllSongList", defaultPlayList);
        }
        Gson gson = new Gson();
        Type songListType = new TypeToken<ArrayList<MusicUtils.Song>>() {
        }.getType();
        allSongList = gson.fromJson(songListJson, songListType);
        playingList=gson.fromJson(playingListJson, songListType);
        //错误修正，防止播放列表空指针
        if (allSongList == null || allSongList.size() == 0) {
            songListJson = defaultPlayList;
            allSongList = gson.fromJson(songListJson, songListType);
        }
        if (playingList == null || playingList.size() == 0) {
            songListJson = defaultPlayList;
            playingList = gson.fromJson(songListJson, songListType);
        }
        if (nowId >= playingList.size()) nowId = 0;
    }

    public void setAllPlayList(List<MusicUtils.PlayList> newAllPlayList) {
        allPlayList=newAllPlayList;
        saveAllPlayList();
    }
    public void setPlayingList(List<MusicUtils.Song> newPlayingList, int startId) {
        //深复制
        playingList.clear();
        Collections.addAll(playingList,new MusicUtils.Song[newPlayingList.size()]);
        Collections.copy(playingList,newPlayingList);
        //playingList=newPlayingList;//浅复制
        nowId=startId;
        playThisNow(nowId);
        saveAllSongList();
    }

    public void saveAllSongList() {
        if (!webPlayMode) {
            Gson gson = new Gson();
            Type songListType = new TypeToken<ArrayList<MusicUtils.Song>>() {
            }.getType();
            String songListJson = gson.toJson(allSongList, songListType);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("allSongList", songListJson);
            songListJson=gson.toJson(playingList,songListType);
            editor.putString("playingList", songListJson);
            editor.apply();
        }
    }

    private void saveAllPlayList() {
        if (!webPlayMode) {
            Gson gson = new Gson();
            Type allPlayListType = new TypeToken<ArrayList<MusicUtils.PlayList>>() {
            }.getType();
            String playListJson = gson.toJson(allPlayList, allPlayListType);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("allPlayList", playListJson);
            editor.apply();
        }
    }

    //添加音乐
    public void addMusic(List<MusicUtils.Song> songList) {
        for (MusicUtils.Song song : songList) {
            addMusic(song.source_uri.replace("/storage/emulated/0", "/sdcard"));
        }
    }

    //添加音乐
    public void addMusic(String path) {
        MusicUtils.Song newSong = new MusicUtils.Song();
        newSong.source_uri = path;
        MusicUtils.Metadata newMetadata = MusicUtils.getMetadata(this, newSong.source_uri);
        if (newMetadata.isValid) {
            newSong.title = newMetadata.title;
            newSong.artist = newMetadata.artist;
            newSong.album = newMetadata.album;
            newSong.duration = newMetadata.duration;
        }
        newSong.size = newMetadata.sizeLong;
        if (newSong.album == null) newSong.album = "null";
        if (FileUtils.getFileNameNoExtension(path).matches(".* - .*")) {
            if (newSong.title == null)
                newSong.title = FileUtils.getFileNameNoExtension(path).split(" - ")[1];
            if (newSong.artist == null)
                newSong.artist = FileUtils.getFileNameNoExtension(path).split(" - ")[0];
        } else if (FileUtils.getFileNameNoExtension(path).matches(".*-.*")) {
            if (newSong.title == null)
                newSong.title = FileUtils.getFileNameNoExtension(path).split("-")[1];
            if (newSong.artist == null)
                newSong.artist = FileUtils.getFileNameNoExtension(path).split("-")[0];
        } else {
            if (newSong.title == null) newSong.title = FileUtils.getFileNameNoExtension(path);
            if (newSong.artist == null) newSong.artist = "null";
        }
        //判断是否存在歌词
        if (FileUtils.isFileExists(path.replace(FileUtils.getFileExtension(path), "lrc")))
            newSong.lyric_uri = path.replace(FileUtils.getFileExtension(path), "lrc");
        else
            newSong.lyric_uri = "null";

        if (allSongList.stream().map(t -> t.source_uri).distinct().collect(Collectors.toList()).contains(path)) {  //如果播放列表已有同路径的音乐，就更新其内容
            allSongList.set(allSongList.stream().map(t -> t.source_uri).distinct().collect(Collectors.toList()).indexOf(path), newSong);
        } else {
            if (allSongList.size() == 1 && !FileUtils.isFileExists(allSongList.get(0).source_uri)) {  //播放列表第一位如果是示例数据则将其替换
                allSongList.set(0, newSong);
            } else {
                allSongList.add(newSong);
            }
        }
        saveAllSongList();
    }

    public void deleteMusic(int allSongId){
        for (MusicUtils.PlayList p:allPlayList) {
            p.list.removeIf(song -> {return song.source_uri.equals(allSongList.get(allSongId).source_uri);});
        }
        int myNowId=playingList.stream().map(t -> t.source_uri).distinct().collect(Collectors.toList()).indexOf(allSongList.get(allSongId).source_uri);
        allSongList.remove(allSongId);
        if(myNowId>=0)
            deleteMusicFromPlayingList(myNowId);
        if(musicServiceCallback!=null)
            musicServiceCallback.onPlayingListChanged();
        saveAllPlayList();
    }


    public void setPlayOrder(int order) {
        playOrder = order;
        savePlayOrder();
        if (playOrder == SHUFFLE_PLAY) {
            mediaPlayer.setLooping(false);
            shuffleOrder = new ArrayList<>();
            for (int i = 0; i < playingList.size(); i++) {
                if (i != nowId)
                    shuffleOrder.add(i);
            }
            Collections.shuffle(shuffleOrder);
            shuffleOrder.add(nowId);
            shuffleId = shuffleOrder.size() - 1;
        } else mediaPlayer.setLooping(playOrder == REPEAT_ONE);
    }

    private void savePlayOrder() {
        SharedPreferences.Editor spEditor = prefs.edit();
        spEditor.putString("playOrder", String.valueOf(playOrder));
        spEditor.apply();
    }

    public void playThisFromList(int musicId) {
        Log.e(TAG, "enabled:" + enabled + " prepared:" + prepared);
        if (enabled && !prepared) return;
        else {
            playThisNow(musicId);
        }
    }

    private void playThisNow(int musicId) {
        nowId=musicId;
        switch (playNow()) {
            case 0:
                if (playOrder == REPEAT_ONE)
                    mediaPlayer.setLooping(true);
                if (musicServiceCallback != null)
                    musicServiceCallback.nowLoadingThis(nowId);
                if (deskLyricCallback != null)
                    deskLyricCallback.updatePlayState(nowId);
                break;
            default:
            case -1:
                if (musicServiceCallback != null)
                    musicServiceCallback.playingErrorThis(nowId);
                if (deskLyricCallback != null)
                    deskLyricCallback.updatePlayState(nowId);
                setEnabled(false);
                break;
        }
        updateNotificationShow(nowId);
    }

    private int playNow() {
        prepared = false;
        mediaPlayer.reset();
        int reId;
            //lyric.LoadLyric(playingList.get(nowId));
            if (FileUtils.isFileExists(playingList.get(nowId).source_uri)) {
                albumImage = MusicUtils.getAlbumImage(this, playingList.get(nowId).source_uri);
                metadata = MusicUtils.getMetadata(this, playingList.get(nowId).source_uri);
                prepare(playingList.get(nowId).source_uri);
                reId = 0;
            } else if (playingList.get(nowId).source_uri.matches("^(https?)://[-A-Za-z0-9+&@#/%?=~_|!:,.;]+[-A-Za-z0-9+&@#/%=~_|]+[\\S\\s]*")) {
                albumImage = null;
                metadata = MusicUtils.getMetadataFromSong(playingList.get(nowId));
                UploadDownloadUtils uploadDownloadUtils = new UploadDownloadUtils(this);
                uploadDownloadUtils.set0nImageLoadListener(new UploadDownloadUtils.OnImageLoadListener() {
                    @Override
                    public void onFileDownloadCompleted(ArrayList<String> array) {
                        if (!array.get(0).equals(playingList.get(nowId).source_uri))
                            return;
                        prepare(array.get(1));
                    }

                    @Override
                    public void onFileDownloading(ArrayList<String> array) {
                        if (!array.get(0).equals(playingList.get(nowId).source_uri))
                            return;
                        //Log.e("Downloading",array.get(1));
                    }

                    @Override
                    public void onFileDownloadError(ArrayList<String> array) {
                        if (!array.get(0).equals(playingList.get(nowId).source_uri))
                            return;
                        if (musicServiceCallback != null)
                            musicServiceCallback.playingErrorThis(nowId);
                        if (deskLyricCallback != null)
                            deskLyricCallback.updatePlayState(nowId);
                        setEnabled(false);
                    }
                });
                uploadDownloadUtils.downloadFile(PathUtils.getExternalAppCachePath(), TimeUtils.getNowMills() + ".tmp", playingList.get(nowId).source_uri);
                reId = 0;
            } else {
                albumImage = MusicUtils.getAlbumImage(this, "null");
                metadata = MusicUtils.getMetadata(this, "null");
                reId = -1;
            }
        return reId;
    }

    private void prepare(String path) {
        try {
            Log.e(TAG, path);
            albumImage = MusicUtils.getAlbumImage(this, path);
            metadata = MusicUtils.getMetadata(this, path);
            mediaPlayer.reset();
            mediaPlayer.setDataSource(this, Uri.parse(path));
            mediaPlayer.prepare();
        } catch (IOException e) {
            e.printStackTrace();
            mediaPlayer.reset();
            if (musicServiceCallback != null)
                musicServiceCallback.playingErrorThis(nowId);
            if (deskLyricCallback != null)
                deskLyricCallback.updatePlayState(nowId);
            setEnabled(false);
        }
    }

    private int prepareToPlayThis(Uri musicPath) {
        //if (FileUtils.isFileExists(musicPath.getPath())) {

        //} else {
        //Toast.makeText(this, "文件不存在", Toast.LENGTH_SHORT).show();
        //return -1;
        //}
        return 0;
    }

    public LyricUtils getLyric() {
        return lyric;
    }

    public void setWebPlayMode(boolean webPlayMode) {
        this.webPlayMode = webPlayMode;
        readAllSongList();
        readAllPlayList();
    }

    public MusicUtils.Metadata getMetadata() {
        return metadata;
    }

    public Bitmap getAlbumImage() {
        return albumImage;
    }

    @Override
    public void onKeyDown(int keyAction) {
        switch (keyAction) {
            case MediaButton2Receiver.KeyActions.PLAY_ACTION:
                Log.d(TAG, "播放...");
                setPlayOrPause(true);
                break;
            case MediaButton2Receiver.KeyActions.PAUSE_ACTION:
                Log.d(TAG, "暂停...");
                setPlayOrPause(false);
                break;
            case MediaButton2Receiver.KeyActions.PREV_ACTION:
                Log.d(TAG, "上一首...");
                playPrevOrNext(false);
                break;
            case MediaButton2Receiver.KeyActions.NEXT_ACTION:
                Log.d(TAG, "下一首...");
                playPrevOrNext(true);
                break;
            case MediaButton2Receiver.KeyActions.KEYCODE_HEADSETHOOK:
                //Log.e(TAG, "keytimes:" + keyTimes[0]);
                if (keyTimes[0] != 0)
                    keyTimeThread.interrupt();
                keyTimeThread = new KeyTimeThread();
                keyTimeThread.start();
                if (keyTimes[0] >= 6)
                    keyTimes[0] = 0;
                else
                    keyTimes[0]++;
                break;
        }
    }

    public List<MusicUtils.Song> getAllSongList() {
        return allSongList;
    }

    public void setAllSongListAfterDelete(List<MusicUtils.Song> mSongList) {
        for (MusicUtils.Song deletedSong:allSongList) {
            if(!mSongList.contains(deletedSong)){
                for (MusicUtils.PlayList p:allPlayList) {
                    p.list.remove(deletedSong);
                }
            }
        }
        saveAllSongList();
    }

    public void setAllSongListAfterAdd(List<MusicUtils.Song> mSongList) {
        for (MusicUtils.Song editedSong:mSongList) {
            if (allSongList.stream().map(t -> t.source_uri).distinct().collect(Collectors.toList()).contains(editedSong.source_uri)&&!allSongList.contains(editedSong)){
                for (MusicUtils.PlayList p:allPlayList) {
                    int id=p.list.stream().map(t -> t.source_uri).distinct().collect(Collectors.toList()).indexOf(editedSong.source_uri);
                    if(id!=-1){
                        p.list.set(id,editedSong);
                    }
                }
            }
        }
        saveAllSongList();
    }

    public void deleteMusicFromPlayingList(int position) {
        if(position<=getMaxID()) {
            playingList.remove(position);
            if (position < nowId)
                nowId--;
            else if(position==nowId)
                playThisFromList(nowId);
        }
        if(musicServiceCallback!=null)
            musicServiceCallback.onPlayingListChanged();
        saveAllSongList();
    }

    public void setPlayingListNextSong(MusicUtils.Song nextSong) {
        if(nextSong.source_uri.equals(playingList.get(nowId).source_uri))
            return;
        if(playingList.size()==1&&playingList.get(0).memory.equals("此为测试数据，添加音乐文件后自动删除"))
            playingList.remove(0);
        playingList.removeIf(song -> {
                return song.source_uri.equals(nextSong.source_uri);
            });
        playingList.add(nowId+1,nextSong);
        if(musicServiceCallback!=null)
            musicServiceCallback.onPlayingListChanged();
        saveAllSongList();
    }

    public void deletePlayingList() {
        nowId=0;
        playingList.clear();
        String defaultPlayList = "[{\"id\":-1,\"title\":\"这是音乐标题\",\"artist\":\"这是歌手\",\"album\":\"这是专辑名\",\"memory\":\"此为测试数据，添加音乐文件后自动删除\"," +
                "\"source_uri\":\"file:///storage/emulated/0/Android/data/" + getPackageName() + "/Music/这是歌手 - 这是音乐标题.mp3\"," +
                "\"lyric_uri\":\"file:///storage/emulated/0/Android/data/" + getPackageName() + "/Music/这是歌手 - 这是音乐标题.lrc\",\"duration\":\"0\"}]";
        Gson gson = new Gson();
        Type songListType = new TypeToken<ArrayList<MusicUtils.Song>>() {
        }.getType();
        List<MusicUtils.Song> tmp=gson.fromJson(defaultPlayList, songListType);
        playingList.add(tmp.get(0));
        //playThisFromList(0);
        setPlayOrPause(false);
        if(musicServiceCallback!=null)
            musicServiceCallback.onPlayingListChanged();
        saveAllSongList();
    }

    private class KeyTimeThread extends Thread {
        @Override
        public void run() {
            try {
                Thread.sleep(400);
                //Log.e(TAG, "keytimes:" + keyTimes[0]);
                if (keyTimes[0] == 2)
                    keyHandler.sendEmptyMessage(1);
                else if (keyTimes[0] == 4)
                    keyHandler.sendEmptyMessage(2);
                else if (keyTimes[0] == 6)
                    keyHandler.sendEmptyMessage(3);
                keyTimes[0] = 0;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private Handler keyHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                setPlayOrPause(!isPlaying());
            } else if (msg.what == 2) {
                playPrevOrNext(true);
            } else if (msg.what == 3) {
                playPrevOrNext(false);
            }
        }
    };

    public class MusicReceiver extends BroadcastReceiver {
        public static final String TAG = "MusicReceiver";

        @Override
        public void onReceive(Context context, Intent intent) {
            //UI控制
            UIControl(intent.getAction());
        }

        /**
         * 页面的UI 控制 ，通过服务来控制页面和通知栏的UI
         *
         * @param state 状态码
         */
        private void UIControl(String state) {
            switch (state) {
                case PLAY:
                    Log.d(MusicReceiver.TAG, PLAY + " or " + PAUSE);
                    setPlayOrPause(!isPlaying());
                    break;
                case PREV:
                    Log.d(MusicReceiver.TAG, PREV);
                    playPrevOrNext(false);
                    break;
                case NEXT:
                    Log.d(MusicReceiver.TAG, NEXT);
                    playPrevOrNext(true);
                    break;
                case CLOSE:
                    Log.d(MusicReceiver.TAG, CLOSE);
                    break;
                case LYRIC:
                    Log.d(MusicReceiver.TAG, LYRIC);
                    showDesktopLyric();
                    break;
                default:
                    break;
            }
            updateNotificationShow(getNowId());
        }
    }

    public class MyMusicBinder extends Binder {
        //返回Service对象
        public MusicService getService() {
            return MusicService.this;
        }
    }

    private class TimingThread extends Thread {
        public boolean isTiming = false;

        @Override
        public void run() {
            super.run();
            int timing = 0;
            do {
                try {
                    timing = prefs.getInt("timing", 0);
                    Log.e(TAG, "timing" + timing);
                    if (timing > 0) {
                        isTiming = true;
                        Thread.sleep(60000);
                        timing--;
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putInt("timing", timing);
                        editor.apply();
                    }
                } catch (InterruptedException | NullPointerException e) {
                    return;
                }
            } while (timing > 0);
            setPlayOrPause(false);
            isTiming = false;
        }
    }

    public void startTiming() {
        if (timingThread == null) {
            timingThread = new TimingThread();
            timingThread.start();
        } else {
            timingThread.interrupt();
            timingThread = new TimingThread();
            timingThread.start();
        }
    }

    public void stopTiming() {
        if (timingThread != null)
            timingThread.interrupt();
    }

    public boolean isTiming() {
        if (timingThread != null)
            return timingThread.isTiming;
        else return false;
    }
}