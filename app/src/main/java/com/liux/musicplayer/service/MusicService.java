package com.liux.musicplayer.service;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.blankj.utilcode.util.FileUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.liux.musicplayer.R;
import com.liux.musicplayer.interfaces.DeskLyricCallback;
import com.liux.musicplayer.interfaces.MusicServiceCallback;
import com.liux.musicplayer.receiver.RemoteControlReceiver;
import com.liux.musicplayer.ui.MainActivity;
import com.liux.musicplayer.utils.MusicUtils;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class MusicService extends Service {
    public static final String PLAY = "play";
    public static final String PAUSE = "pause";
    public static final String PREV = "prev";
    public static final String NEXT = "next";
    public static final String CLOSE = "close";
    public static final String LYRIC = "lyric";
    public static final String PROGRESS = "progress";
    public static final String TAG = "MusicService";
    public static final int NOTIFICATION_ID = 1;
    private static RemoteViews remoteViewsSmall;
    private static RemoteViews remoteViewsLarge;
    private MusicReceiver musicReceiver;
    private Notification notification;
    private static NotificationManager manager;
    private static MediaPlayer mp;
    private List<MusicUtils.Song> songList;
    private int nowId;
    //0=顺序播放 1=列表循环 2=单曲循环 3=随机播放
    private int playOrder;
    public static final int LIST_PLAY = 0;
    public static final int REPEAT_LIST = 1;
    public static final int REPEAT_ONE = 2;
    public static final int SHUFFLE_PLAY = 3;
    private List<Integer> shuffleOrder;
    private SharedPreferences sp;
    private int shuffleId;
    private boolean isAppLyric = false;

    public void setDesktopLyric(boolean desktopLyric) {
        isDesktopLyric = desktopLyric;
        updateNotificationShow(getNowId());
    }

    private boolean isDesktopLyric = false;
    private boolean prepared = false;
    private boolean isEnabled = false;
    public int nowPageId = 0;
    private String notificationId = "MusicService";
    private String notificationName = "常驻后台通知";
    private MediaSessionCompat mMediaSession;
    private DesktopLyricServiceConnector desktopLyricServiceConnector;
    private boolean isActivityForeground = false;

    public void setActivityForeground(boolean activityForeground) {
        isActivityForeground = activityForeground;
        if (isActivityForeground || !isDesktopLyric) hideDesktopLyric(true);
        else hideDesktopLyric(false);
    }

    private MusicServiceCallback musicServiceCallback;

    public void setMusicServiceCallback(MusicServiceCallback musicServiceCallback) {
        this.musicServiceCallback = musicServiceCallback;
    }

    private DeskLyricCallback deskLyricCallback;

    public void setDeskLyricCallback(DeskLyricCallback deskLyricCallback) {
        this.deskLyricCallback = deskLyricCallback;
    }

    @Override
    public void onCreate() {
        songList = new ArrayList<>();
        nowId = 0;
        playOrder = 0;
        sp = this.getSharedPreferences("com.liux.musicplayer_preferences", Activity.MODE_PRIVATE);
        initializePlayer();
        readPlayList();
        setMediaPlayerListener();
        initRemoteViews();
        initNotification();
        registerMusicReceiver();
        //showNotification();
        updateNotificationShow(nowId);
        registerRemoteControlReceiver();
        initDesktopLyric();
    }

    private void initDesktopLyric() {
        isDesktopLyric = sp.getBoolean("isShowLyric", false);
        updateNotificationShow(getNowId());
    }

    private void initializePlayer() {
        if (mp == null) {
            mp = new MediaPlayer();
        }
    }

    @Override
    public void onDestroy() {
        if (musicReceiver != null) {
            //解除动态注册的广播
            unregisterReceiver(musicReceiver);
            stopService(new Intent(MusicService.this, FloatLyricServices.class));
            closeNotification();
        }
        mMediaSession.release();
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    private void registerRemoteControlReceiver() {
        ComponentName mbr = new ComponentName(getPackageName(), RemoteControlReceiver.class.getName());
        mMediaSession = new MediaSessionCompat(this, "mbr", mbr, null);
        mMediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public boolean onMediaButtonEvent(Intent intent) {
                //在这里就可以接收到（线控、蓝牙耳机的按键事件了）

                //通过intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);获取按下的按键实现自己对应功能
                Log.e(TAG, String.valueOf(intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT)));
                if (intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT).toString().contains("ACTION_UP")) {
                    PlayOrPause(!isPlaying());
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

    @SuppressLint("NotificationTrampoline")
    private void initNotification() {
        String channelId = "play_control";
        String channelName = "播放控制";
        int importance = NotificationManager.IMPORTANCE_HIGH;
        NotificationChannel channel = new NotificationChannel(channelId, channelName, importance);
        manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.createNotificationChannel(channel);

        PendingIntent contentIntent = PendingIntent.getActivity(
                this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);

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

    public void updateNotificationShow(int position) {
        //封面专辑
        Bitmap bitmap = MusicUtils.getAlbumImage(this, songList.get(position));
        if (bitmap == null) {
            remoteViewsSmall.setImageViewResource(R.id.iv_album_cover, R.drawable.ic_baseline_music_note_24);
            remoteViewsLarge.setImageViewResource(R.id.iv_album_cover, R.drawable.ic_baseline_music_note_24);
        } else {
            remoteViewsSmall.setImageViewBitmap(R.id.iv_album_cover, bitmap);
            remoteViewsLarge.setImageViewBitmap(R.id.iv_album_cover, bitmap);
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
        if (mp.isPlaying()) {
            remoteViewsSmall.setImageViewResource(R.id.btn_notification_play, R.drawable.ic_round_pause_24);
            remoteViewsLarge.setImageViewResource(R.id.btn_notification_play, R.drawable.ic_round_pause_24);
            startForeground(NOTIFICATION_ID, notification);
        } else {
            remoteViewsSmall.setImageViewResource(R.id.btn_notification_play, R.drawable.ic_round_play_arrow_24);
            remoteViewsLarge.setImageViewResource(R.id.btn_notification_play, R.drawable.ic_round_play_arrow_24);
            stopForeground(false);
            manager.notify(NOTIFICATION_ID, notification);
        }
    }

    /**
     * 关闭音乐通知栏
     */
    public void closeNotification() {
        if (mp != null) {
            if (mp.isPlaying()) {
                mp.pause();
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
        PendingIntent prevPendingIntent = PendingIntent.getBroadcast(this, 0, intentPrev, 0);
        //为prev控件注册事件
        remoteViewsSmall.setOnClickPendingIntent(R.id.btn_notification_previous, prevPendingIntent);
        remoteViewsLarge.setOnClickPendingIntent(R.id.btn_notification_previous, prevPendingIntent);

        //通知栏控制器播放暂停按钮广播操作  //用于接收广播时过滤意图信息
        Intent intentPlay = new Intent(PLAY);
        PendingIntent playPendingIntent = PendingIntent.getBroadcast(this, 0, intentPlay, 0);
        //为play控件注册事件
        remoteViewsSmall.setOnClickPendingIntent(R.id.btn_notification_play, playPendingIntent);
        remoteViewsLarge.setOnClickPendingIntent(R.id.btn_notification_play, playPendingIntent);

        //通知栏控制器下一首按钮广播操作
        Intent intentNext = new Intent(NEXT);
        PendingIntent nextPendingIntent = PendingIntent.getBroadcast(this, 0, intentNext, 0);
        //为next控件注册事件
        remoteViewsSmall.setOnClickPendingIntent(R.id.btn_notification_next, nextPendingIntent);
        remoteViewsLarge.setOnClickPendingIntent(R.id.btn_notification_next, nextPendingIntent);

        //通知栏控制器关闭按钮广播操作
        Intent intentClose = new Intent(CLOSE);
        PendingIntent closePendingIntent = PendingIntent.getBroadcast(this, 0, intentClose, 0);
        //为close控件注册事件
        remoteViewsSmall.setOnClickPendingIntent(R.id.btn_notification_close, closePendingIntent);
        remoteViewsLarge.setOnClickPendingIntent(R.id.btn_notification_close, closePendingIntent);

        //通知栏控制器切换歌词开关操作
        Intent intentLyric = new Intent(LYRIC);
        PendingIntent lyricPendingIntent = PendingIntent.getBroadcast(this, 0, intentLyric, 0);
        //为lyric控件注册事件
        remoteViewsSmall.setOnClickPendingIntent(R.id.btn_notification_lyric, lyricPendingIntent);
        remoteViewsLarge.setOnClickPendingIntent(R.id.btn_notification_lyric, lyricPendingIntent);

    }

    public boolean isDesktopLyric() {
        return isDesktopLyric;
    }

    public class MusicReceiver extends BroadcastReceiver {

        public static final String TAG = "MusicReceiver";

        @Override
        public void onReceive(Context context, Intent intent) {
            //UI控制
            UIControl(intent.getAction(), TAG);
        }

        /**
         * 页面的UI 控制 ，通过服务来控制页面和通知栏的UI
         *
         * @param state 状态码
         * @param tag
         */
        private void UIControl(String state, String tag) {
            switch (state) {
                case PLAY:
                    Log.d(tag, PLAY + " or " + PAUSE);
                    PlayOrPause(!isPlaying());
                    break;
                case PREV:
                    Log.d(tag, PREV);
                    playPrevOrNext(false);
                    break;
                case NEXT:
                    Log.d(tag, NEXT);
                    playPrevOrNext(true);
                    break;
                case CLOSE:
                    Log.d(tag, CLOSE);
                    break;
                case LYRIC:
                    Log.d(tag, LYRIC);
                    showDesktopLyric();
                    break;
                default:
                    break;
            }
            updateNotificationShow(getNowId());
        }
    }

    public void showDesktopLyric() {
        Intent intent = new Intent(MusicService.this, FloatLyricServices.class);
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
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean("isShowLyric", isDesktopLyric);
        editor.apply();
    }

    public void hideDesktopLyric(boolean isHide) {
        Intent intent = new Intent(MusicService.this, FloatLyricServices.class);
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

    public void PlayOrPause(boolean isPlay) {
        if (isPlay) {
            if (!isEnabled()) {
                setEnabled(true);
                playThisNow(getNowId());
            } else if (isPrepared()) {
                start();
            } else {
                playThisNow(getNowId());
                pause();
            }
        } else {
            pause();
        }
        if (musicServiceCallback != null)
            musicServiceCallback.updatePlayStateThis();
        if (deskLyricCallback != null)
            deskLyricCallback.updatePlayState();
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

    @Override
    public boolean onUnbind(Intent intent) {
        //松绑Service，会触发onDestroy()
        //Toast.makeText(this, "解绑音乐服务成功", Toast.LENGTH_SHORT).show();
        //stopSelf();
        return true;
    }

    private class DesktopLyricServiceConnector implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // 获取服务的操作对象
            FloatLyricServices.MyBinder binder = (FloatLyricServices.MyBinder) service;
            binder.getService();
            Log.d(TAG, "Connected");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    }

    ;

    public class MyMusicBinder extends Binder {
        //返回Service对象
        public MusicService getService() {
            return MusicService.this;
        }
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    public void setNowPageId(int id) {
        nowPageId = id;
    }

    public int getNowPageId() {
        return nowPageId;
    }

    public boolean isAppLyric() {
        return isAppLyric;
    }

    public void setAppLyric(boolean appLyric) {
        isAppLyric = appLyric;
    }

    public void playPrevOrNext(boolean isNext) {
        int maxId = getMaxID();
        int nowId = getNowId();
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
                    if (nowId < maxId) nowId += 1;
                    else nowId = 0;
                } else {
                    if (nowId > 0) nowId -= 1;
                    else nowId = maxId;
                }
                break;
            default:
            case LIST_PLAY:
                if (isNext) {
                    if (nowId < maxId)
                        nowId += 1;
                    else
                        return;
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
        mp.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
            }
        });
        //音频播放完成的监听器
        mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                //把所有的都回归到0
                prepared = false;
                if (isEnabled)
                    playPrevOrNext(true);
            }
        });
    }

    public void refreshPlayList() {
        readPlayList();
    }

    private void readPlayList() {
        nowId = Integer.parseInt(sp.getString("nowId", "0"));
        String playListJson = sp.getString("playList",
                "[{\"id\":-1,\"title\":\"这是音乐标题\",\"artist\":\"这是歌手\",\"album\":\"这是专辑名\",\"filename\":\"此为测试数据，添加音乐文件后自动删除\"," +
                        "\"source_uri\":\"file:///storage/emulated/0/Android/data/com.liux.musicplayer/Music/这是歌手 - 这是音乐标题.mp3\"," +
                        "\"lyric_uri\":\"file:///storage/emulated/0/Android/data/com.liux.musicplayer/Music/这是歌手 - 这是音乐标题.lrc\",\"duration\":\"0\"}]");
        Gson gson = new Gson();
        Type playListType = new TypeToken<ArrayList<MusicUtils.Song>>() {
        }.getType();
        songList = gson.fromJson(playListJson, playListType);
        if (songList == null || songList.size() == 0) {
            playListJson = "[{\"id\":-1,\"title\":\"这是音乐标题\",\"artist\":\"这是歌手\",\"album\":\"这是专辑名\",\"filename\":\"此为测试数据，添加音乐文件后自动删除\"," +
                    "\"source_uri\":\"file:///storage/emulated/0/Android/data/com.liux.musicplayer/Music/这是歌手 - 这是音乐标题.mp3\"," +
                    "\"lyric_uri\":\"file:///storage/emulated/0/Android/data/com.liux.musicplayer/Music/这是歌手 - 这是音乐标题.lrc\",\"duration\":\"0\"}]";
            songList = gson.fromJson(playListJson, playListType);
        }
        if (nowId >= songList.size()) nowId = 0;
        setPlayOrder(Integer.parseInt(sp.getString("playOrder", "0")));
        //sendMyBroadcast("method","setPlayOrder",playOrder);
    }

    public void setPlayList(List<MusicUtils.Song> newSongList) {
        songList = newSongList;
        savePlayList();
    }

    private void savePlayList() {
        Gson gson = new Gson();
        Type playListType = new TypeToken<ArrayList<MusicUtils.Song>>() {
        }.getType();
        String playListJson = gson.toJson(songList, playListType);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("playList", playListJson);
        editor.apply();
    }

    //添加音乐
    public int addMusic(String path) {
        MusicUtils.Song newSong = new MusicUtils.Song();
        newSong.source_uri = path;
        MusicUtils.Metadata newMetadata = MusicUtils.getMetadata(this, newSong);
        if (newMetadata.isValid) {
            newSong.title = newMetadata.title;
            newSong.artist = newMetadata.artist;
            newSong.album = newMetadata.album;
            newSong.duration = newMetadata.duration;
        }
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

        if (songList.stream().map(t -> t.source_uri).distinct().collect(Collectors.toList()).contains(path)) {  //如果播放列表已有同路径的音乐，就更新其内容
            songList.set(songList.stream().map(t -> t.source_uri).distinct().collect(Collectors.toList()).indexOf(path), newSong);
        } else {
            if (songList.size() == 1 && !FileUtils.isFileExists(songList.get(0).source_uri)) {  //播放列表第一位如果是示例数据则将其替换
                songList.set(0, newSong);
            } else {
                songList.add(newSong);
            }
        }
        savePlayList();
        return 0;
    }

    //删除音乐
    public int deleteMusic(int[] array) {

        return 0;
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

    public MediaPlayer getMediaPlayer() {
        return mp;
    }

    public void setPlayOrder(int order) {
        playOrder = order;
        savePlayOrder();
        if (playOrder == SHUFFLE_PLAY) {
            mp.setLooping(false);
            shuffleOrder = new ArrayList<>();
            for (int i = 0; i < songList.size(); i++) {
                if (i != nowId)
                    shuffleOrder.add(i);
            }
            Collections.shuffle(shuffleOrder);
            shuffleOrder.add(nowId);
            shuffleId = shuffleOrder.size() - 1;
        } else mp.setLooping(playOrder == REPEAT_ONE);
    }

    private void savePlayOrder() {
        SharedPreferences.Editor spEditor = sp.edit();
        spEditor.putString("playOrder", String.valueOf(playOrder));
        spEditor.apply();
    }

    public void setNowId(int id) {
        nowId = id;
    }

    public void playThisNow(int musicId) {
        switch (playThis(musicId)) {
            case 0:
                if (playOrder == REPEAT_ONE)
                    mp.setLooping(true);
                if (musicServiceCallback != null)
                    musicServiceCallback.nowPlayingThis(musicId);
                if (deskLyricCallback != null)
                    deskLyricCallback.updatePlayState(musicId);
                break;
            default:
            case -1:
                if (musicServiceCallback != null)
                    musicServiceCallback.playingErrorThis(musicId);
                if (deskLyricCallback != null)
                    deskLyricCallback.updatePlayState(musicId);
                setEnabled(false);
                break;
        }
        updateNotificationShow(nowId);
    }

    private int playThis(int id) {
        int reId;
        nowId = id;
        SharedPreferences.Editor spEditor = sp.edit();
        spEditor.putString("nowId", String.valueOf(nowId));
        spEditor.apply();
        if (nowId > getMaxID()) {
            reId = -1;
        } else {
            reId = playThis(Uri.parse(songList.get(nowId).source_uri));
            if (reId == 0 && prepared)
                mp.start();
        }
        return reId;
    }

    public int playThis(Uri musicPath) {
        if (FileUtils.isFileExists(musicPath.getPath())) {
            try {
                mp.reset();
                mp.setDataSource(this, musicPath);
                mp.prepare();
                prepared = true;
            } catch (IOException e) {
                e.printStackTrace();
                mp.reset();
                return -1;
            }
        } else {
            Toast.makeText(this, "文件不存在", Toast.LENGTH_SHORT).show();
            return -1;
        }
        return 0;
    }

    public void pause() {
        mp.pause();
        updateNotificationShow(getNowId());
    }

    public void start() {
        if (prepared)
            mp.start();
        updateNotificationShow(getNowId());
    }

    public void setProgress(int second) {
    }

    public boolean isPlaying() {
        return mp.isPlaying();
    }

    public boolean isPrepared() {
        return prepared;
    }
}