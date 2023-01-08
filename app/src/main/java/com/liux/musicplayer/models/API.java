package com.liux.musicplayer.models;

import com.liux.musicplayer.media.MusicLibrary;
import com.liux.musicplayer.services.HttpServer;

import java.util.ArrayList;
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
            theme = "#FADFA3";
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

    public static class SongList {
        int count = 0;
        List<SongDetail> audio;

        public SongList(List<Song> songList) {
            count = songList.size();
            audio = new ArrayList<>();
            for (Song s : songList) {
                audio.add(new SongDetail(s));
            }
        }
    }

    public static class PlayList {
        public List<Song> playlist;

        public PlayList(List<Song> songList) {
            playlist = new ArrayList<>();
            for (Song s : songList) {
                playlist.add(new Song(
                        s.getSongTitle(),
                        s.getSongDuration(),
                        s.getArtistName(),
                        s.getAlbumName(),
                        s.getmId(),
                        HttpServer.Config.HTTP_URL
                                .replace("IP", HttpServer.Config.HTTP_IP)
                                .replace("PORT", String.valueOf(HttpServer.Config.HTTP_PORT)) + "api/file?path=" + s.getSongPath(),
                        HttpServer.Config.HTTP_URL
                                .replace("IP", HttpServer.Config.HTTP_IP)
                                .replace("PORT", String.valueOf(HttpServer.Config.HTTP_PORT) + "api/file?path=" + s.getLyricPath()
                                )));
            }
        }
    }

    public static class Info {
        boolean mini = false;
        boolean autoplay = false;
        String theme = "#FADFA3";
        String loop = "all";
        String order = "random";
        String preload = "auto";
        float volume = 0.7f;
        boolean mutex = true;
        boolean listFolded = false;
        int listMaxHeight = 90;
        int lrcType = 3;
    }
}
