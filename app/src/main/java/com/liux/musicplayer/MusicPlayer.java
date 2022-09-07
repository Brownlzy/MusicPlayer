package com.liux.musicplayer;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.Uri;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MusicPlayer {
    public class Song {
        public int id;
        public String title;
        public String artist;
        public String album;
        public String filename;
        public String source_uri;
        public String lyric_uri;
    }

    private final MediaPlayer mp;
    private List<Song> songList;
    private int nowID;
    //0=顺序循环 1=单曲循环 2=随机播放
    private int playOrder;
    private final Context mContext;

    public MusicPlayer(Context context, String playListJson) {
        songList = new ArrayList<>();
        mp = new MediaPlayer();
        nowID = 0;
        playOrder = 0;
        mContext = context;
        setPlayList(playListJson);
    }

    private void setPlayList(String playListJson) {
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

    public int getNowID() {
        return nowID;
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

    ;

    public void setPlayOrder(int order) {
        playOrder = order;
    }

    public void setNowID(int id) {
        nowID = id;
    }

    public int playThis(int id) {
        int reId = 0;
        nowID = id;
        reId = playThis(Uri.parse(songList.get(nowID).source_uri));
        mp.start();
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

    public void playNext() {

    }

    public void setProgress(int second) {

    }

    public boolean isPlaying() {
        return mp.isPlaying();
    }
}

