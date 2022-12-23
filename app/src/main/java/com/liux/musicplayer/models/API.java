package com.liux.musicplayer.models;

import com.liux.musicplayer.media.MusicLibrary;

import java.util.List;

public class API {
    public static class SongDetail {
        String name;
        String artist;
        String url;
        String cover;
        String lrc;
        String theme;

        public SongDetail(Song song) {
            name = song.getSongTitle();
            artist = song.getArtistName();
            theme = "rgb(12 ,12 ,12 )";
            url = "/api/file?path=" + song.getSongPath();
            cover = "/api/cover?path=" + song.getSongPath();
            lrc = "/api/file?path=" + song.getLyricPath();
        }
    }

    public static class SongListList {
        int total;
        List<MusicLibrary.SongList> playlists;

        public SongListList(List<MusicLibrary.SongList> allSongListList) {
            this.playlists = allSongListList;
            this.total = allSongListList.size();
        }
    }
}
