package com.liux.musicplayer.models;

import android.support.v4.media.session.MediaSessionCompat;

import com.liux.musicplayer.utils.LyricUtils;

import java.util.List;

public class Data {
    public List<MediaSessionCompat.QueueItem> currentPlayList;
    public int playOrder=0;
    public int currentPlayIndex=0;
    public LyricUtils currentLyric;
}
