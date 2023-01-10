package com.liux.musicplayer.models;

import static com.liux.musicplayer.services.MusicService.LIST_PLAY;
import static com.liux.musicplayer.services.MusicService.REPEAT_LIST;
import static com.liux.musicplayer.services.MusicService.REPEAT_ONE;
import static com.liux.musicplayer.services.MusicService.SHUFFLE_PLAY;

import com.blankj.utilcode.util.FileUtils;
import com.liux.musicplayer.BuildConfig;
import com.liux.musicplayer.media.MusicLibrary;
import com.liux.musicplayer.services.HttpServer;
import com.liux.musicplayer.utils.SharedPrefs;
import com.liux.musicplayer.utils.UriTransform;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
            theme = "#4CAF50";
            if (song.getSongPath().matches("^(https?)://[-A-Za-z0-9+&@#/%?=~_|!:,.;]+[-A-Za-z0-9+&@#/%=~_|]+[\\S\\s]*")) {
                url = song.getSongPath();
                cover = "/api/cover?path=earth";
                lrc = song.getLyricPath();
            } else {
                url = "/api/file?path=" + UriTransform.toURLEncoded(song.getSongPath());
                cover = "/api/cover?path=" + UriTransform.toURLEncoded(song.getSongPath());
                if (!song.getLyricPath().equals("null"))
                    lrc = "/api/file?path=" + UriTransform.toURLEncoded(song.getLyricPath());
                else
                    lrc = "no-lrc.lrc";
            }
        }
    }

    public static class SongListList {
        int total;
        List<String> playlists;

        public SongListList(List<MusicLibrary.SongList> allSongListList) {
            playlists = allSongListList.stream().map(songList -> songList.n).distinct().collect(Collectors.toList());
            playlists.remove("allSongList");
            playlists.remove("webAllSongList");
            playlists.add(0, "正在播放");
            playlists.add(1, "所有音乐");
            playlists.add(2, "在线列表");
            this.total = playlists.size();
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
                if (FileUtils.isFileExists(s.getSongPath()))
                    playlist.add(new Song(
                            s.getSongTitle(),
                            s.getSongDuration(),
                            s.getArtistName(),
                            s.getAlbumName(),
                            s.getmId(),
                            s.getSize(),
                            HttpServer.Config.HTTP_URL
                                    .replace("IP", HttpServer.Config.HTTP_IP)
                                    .replace("PORT", String.valueOf(HttpServer.Config.HTTP_PORT)) + "api/file?path="
                                    + UriTransform.toURLEncoded(s.getSongPath()),
                            (s.getLyricPath().equals("null")) ? "null" : (HttpServer.Config.HTTP_URL
                                    .replace("IP", HttpServer.Config.HTTP_IP)
                                    .replace("PORT", String.valueOf(HttpServer.Config.HTTP_PORT)) + "api/file?path="
                                    + UriTransform.toURLEncoded(s.getLyricPath()))
                    ));
            }
        }
    }

    public static class Info {
        boolean mini = false;
        boolean fixed = false;
        boolean autoplay = false;
        String theme = "#4CAF50";
        String loop;
        String order;
        String preload = "auto";
        float volume = 0.5f;
        boolean mutex = true;
        boolean listFolded = false;
        String listMaxHeight = "500px";
        int lrcType = 3;

        public Info() {
            switch (SharedPrefs.getPlayOrder()) {
                case LIST_PLAY:
                    loop = "none";
                    order = "list";
                    break;
                case REPEAT_LIST:
                    loop = "all";
                    order = "list";
                    break;
                case REPEAT_ONE:
                    loop = "one";
                    order = "list";
                    break;
                case SHUFFLE_PLAY:
                    loop = "all";
                    order = "random";
                    break;
            }
        }
    }

    public static class VERSION {
        String packageName;
        String versionName;
        int versionCode;
        boolean debug = BuildConfig.DEBUG;

        public VERSION(String packageName, String versionName, int versionCode) {
            this.packageName = packageName;
            this.versionName = versionName;
            this.versionCode = versionCode;
        }
    }
}
