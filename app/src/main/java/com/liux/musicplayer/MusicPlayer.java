package com.liux.musicplayer;

import android.media.MediaPlayer;

import java.util.ArrayList;
import java.util.List;

public class MusicPlayer {
    private MediaPlayer mp;
    private List<String> playList;
    private int nowID;

    public MusicPlayer() {
        playList = new ArrayList<>();
        mp = new MediaPlayer();
        nowID = 0;
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

    public List<String> getPlayList() {
        return playList;
    }

    public int getNowID() {
        return nowID;
    }

    public void setNowID(int id) {
        nowID = id;
    }

    public int playThis(int id) {
        nowID = id;

        return 0;
    }

    public void pause() {

    }

    public void playNext() {

    }

    public void setProgress(int second) {

    }

}
