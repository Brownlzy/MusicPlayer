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
import java.util.List;
import java.util.Random;

public class MusicPlayer {
    private final MediaPlayer mp;
    private List<Song> songList;
    private int nowId;
    private MainActivity mainActivity;
    //0=顺序循环 1=单曲循环 2=随机播放
    private int playOrder;
    private final Context mContext;
    private SharedPreferences sp;


    public class Song {
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
        setPlayList();
        setMediaPlayerListener();
    }

    public void playPrevOrNext(boolean isNext) {
        int maxId = getMaxID();
        int nowId = getNowId();
        int order = getPlayOrder();
        switch (order) {
            case 2:
                Random r = new Random();
                nowId = r.nextInt(maxId + 1);
                break;
            default:
            case 0:
                if (isNext) {
                    if (nowId < maxId) nowId += 1;
                    else nowId = 0;
                } else {
                    if (nowId > 0) nowId -= 1;
                    else nowId = maxId;
                }
                break;
            case 1:
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
                mainActivity.resetPlayProgress();
                playPrevOrNext(true);
            }
        });
    }

    public void refreshPlayList() {
        setPlayList();
    }

    private void setPlayList() {
        nowId = Integer.parseInt(sp.getString("nowId", "0"));
        playOrder = Integer.parseInt(sp.getString("playOrder", "0"));
        mainActivity.setPlayOrder(playOrder);
        String playListJson = sp.getString("playList",
                "[{\"id\":-1,\"title\":\"这是音乐标题\",\"artist\":\"这是歌手\",\"album\":\"这是专辑名\",\"filename\":\"此为测试数据，添加音乐文件后自动删除\"," +
                        "\"source_uri\":\"file:///storage/emulated/0/Android/data/com.liux.musicplayer/Music/eg\"," +
                        "\"lyric_uri\":\"file:///storage/emulated/0/Android/data/com.liux.musicplayer/Music/eg\"}]");
        Gson gson = new Gson();
        java.lang.reflect.Type playListType = new TypeToken<ArrayList<Song>>() {
        }.getType();
        songList = gson.fromJson(playListJson, playListType);
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
        playOrder = order;
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
                                mainActivity.setViewPagerToId(2);
                            }
                        })
                        .create();
                alertInfoDialog.show();
                break;
        }
    }

    private int playThis(int id) {
        int reId = 0;
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

