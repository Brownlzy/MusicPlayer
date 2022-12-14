package com.liux.musicplayer.interfaces;

public interface MusicServiceCallback {
    void nowPlayingThis(int musicID);

    void playingErrorThis(int musicID);

    void updatePlayStateThis();

    void nowLoadingThis(int musicId);

    void onPlayingListChanged();
}
