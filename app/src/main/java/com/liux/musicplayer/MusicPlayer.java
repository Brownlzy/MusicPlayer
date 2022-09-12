package com.liux.musicplayer;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.Uri;

import androidx.appcompat.app.AlertDialog;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class MusicPlayer {
    private final MediaPlayer mp;//主要的播放类，搜MediaPlayer生命周期
    private List<Song> songList;
    private int nowId;
    private MainActivity mainActivity;
    //0=顺序播放 1=列表循环 2=单曲循环 3=随机播放
    private int playOrder;
    public static final int LIST_PLAY = 0;
    public static final int REPEAT_LIST = 1;
    public static final int REPEAT_ONE = 2;
    public static final int SHUFFLE_PLAY = 3;
    private final Context mContext;
    private SharedPreferences sp;
    private List<Integer> shuffleOrder;
    private int shuffleId;//随机播放列表的正在播放id


    public class Song {//歌曲信息类
        public int id;
        public String title;
        public String artist;
        public String album;
        public String filename;
        public String source_uri;
        public String lyric_uri;
    }

    public MusicPlayer(MainActivity mMainActivity, Context context) {
        songList = new ArrayList<>();
        mp = new MediaPlayer();
        nowId = 0;
        playOrder = 0;
        mContext = context;
        mainActivity = mMainActivity;
        sp = mContext.getSharedPreferences("com.liux.musicplayer_preferences", Activity.MODE_PRIVATE);
        setPlayList();//读取播放列表
        setMediaPlayerListener();//设置mediaplayer的监听器
    }

    public void playPrevOrNext(boolean isNext) {//根据播放顺序信息确定前后曲目
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
        playThisNow(nowId);//播放
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
                mainActivity.resetPlayProgress();
                playPrevOrNext(true);
            }
        });
    }

    public void refreshPlayList() {
        setPlayList();
    }

    private void setPlayList() {
        //读取nowId/Playlist/这些key值在/res/xml/root_。。。
        nowId = Integer.parseInt(sp.getString("nowId", "0"));
        String playListJson = sp.getString("playList",
                "[{\"id\":-1,\"title\":\"这是音乐标题\",\"artist\":\"这是歌手\",\"album\":\"这是专辑名\",\"filename\":\"此为测试数据，添加音乐文件后自动删除\"," +
                        "\"source_uri\":\"file:///storage/emulated/0/Android/data/com.liux.musicplayer/Music/这是歌手 - 这是音乐标题.mp3\"," +
                        "\"lyric_uri\":\"file:///storage/emulated/0/Android/data/com.liux.musicplayer/Music/这是歌手 - 这是音乐标题.lrc\"}]");
        Gson gson = new Gson();
        java.lang.reflect.Type playListType = new TypeToken<ArrayList<Song>>() {
        }.getType();
        songList = gson.fromJson(playListJson, playListType);
        if (songList == null) {
            playListJson = "[{\"id\":-1,\"title\":\"这是音乐标题\",\"artist\":\"这是歌手\",\"album\":\"这是专辑名\",\"filename\":\"此为测试数据，添加音乐文件后自动删除\"," +
                    "\"source_uri\":\"file:///storage/emulated/0/Android/data/com.liux.musicplayer/Music/这是歌手 - 这是音乐标题.mp3\"," +
                    "\"lyric_uri\":\"file:///storage/emulated/0/Android/data/com.liux.musicplayer/Music/这是歌手 - 这是音乐标题.lrc\"}]";
            songList = gson.fromJson(playListJson, playListType);
        }
        if (nowId >= songList.size()) nowId = 0;
        setPlayOrder(Integer.parseInt(sp.getString("playOrder", "0")));
        mainActivity.setPlayOrder(playOrder);
    }

    private void savePlayList() {
        Gson gson = new Gson();
        java.lang.reflect.Type playListType = new TypeToken<ArrayList<Song>>() {
        }.getType();
        String playListJson = gson.toJson(songList, playListType);
        SharedPreferences sp = mContext.getSharedPreferences("com.liux.musicplayer_preferences", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("playList", playListJson);
        editor.apply();
    }

    //遍历文件夹
    private List<String> ergodicFolder(String path) {

        return new ArrayList<>();
    }

    //添加音乐
    public int addMusic(String path) {

        return 0;
    }

    //删除音乐
    public int deleteMusic(int[] array) {

        return 0;
    }

    public List<Song> getPlayList() {
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
        //设置播放顺序
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
        } else if (playOrder == REPEAT_ONE) {
            mp.setLooping(true);
        } else {
            mp.setLooping(false);
        }
    }

    private void savePlayOrder() {
        //保存播放顺序
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
                mainActivity.setPlayBarTitle(musicId);
                mainActivity.setHomeFragment();
                //初始化进度条
                mainActivity.resetPlayProgress();
                //开启进度条跟踪线程
                mainActivity.startProgressBar();
                mainActivity.setPlayOrPause(true);
                break;
            default:
            case 1:
                mainActivity.setPlayBarTitle(musicId);
                mainActivity.setHomeFragment();
                AlertDialog alertInfoDialog = new AlertDialog.Builder(mContext)
                        .setTitle(R.string.play_error)
                        .setMessage(R.string.play_err_Info)
                        .setIcon(R.mipmap.ic_launcher)
                        .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mainActivity.setPlayOrPause(false);
                            }
                        })
                        .create();
                alertInfoDialog.show();
                break;
        }
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
            mp.start();
        }
        return reId;
    }

    public int playThis(Uri musicPath) {
        try {
            mp.reset();
            mp.setDataSource(mContext, musicPath);
            mp.prepare();
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
        return 0;
    }

    public void pause() {
        mp.pause();
    }

    public void start() {
        mp.start();
    }

    public void setProgress(int second) {
    }

    public boolean isPlaying() {
        return mp.isPlaying();
    }
}

