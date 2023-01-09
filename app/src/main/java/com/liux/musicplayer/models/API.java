package com.liux.musicplayer.models;

import static com.liux.musicplayer.services.MusicService.LIST_PLAY;
import static com.liux.musicplayer.services.MusicService.REPEAT_LIST;
import static com.liux.musicplayer.services.MusicService.REPEAT_ONE;
import static com.liux.musicplayer.services.MusicService.SHUFFLE_PLAY;

import com.blankj.utilcode.util.FileUtils;
import com.liux.musicplayer.media.MusicLibrary;
import com.liux.musicplayer.services.HttpServer;
import com.liux.musicplayer.utils.SharedPrefs;

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
            theme = "#FADFA3";
            url = "/api/file?path=" + song.getSongPath();
            cover = "/api/cover?path=" + song.getSongPath();
            if (!song.getLyricPath().equals("null"))
                lrc = "/api/file?path=" + song.getLyricPath();
            else
                lrc = "no-lrc.lrc";
        }
    }

    public static class SongListList {
        int total;
        List<String> playlists;

        public SongListList(List<MusicLibrary.SongList> allSongListList) {
            this.playlists = allSongListList.stream().map(songList -> songList.n).distinct().collect(Collectors.toList());
            ;
            //this.playlists.add(0,"playingList");
            this.playlists.add(0, "正在播放");
            playlists.remove("allSongList");
            playlists.remove("webAllSongList");
            playlists.add("所有歌曲");
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
                if (FileUtils.isFileExists(s.getSongPath()))
                    playlist.add(new Song(
                            s.getSongTitle(),
                            s.getSongDuration(),
                            s.getArtistName(),
                            s.getAlbumName(),
                            s.getmId(),
                            HttpServer.Config.HTTP_URL
                                    .replace("IP", HttpServer.Config.HTTP_IP)
                                    .replace("PORT", String.valueOf(HttpServer.Config.HTTP_PORT)) + "api/file?path="
                                    + s.getSongPath(),
                            HttpServer.Config.HTTP_URL
                                    .replace("IP", HttpServer.Config.HTTP_IP)
                                    .replace("PORT", String.valueOf(HttpServer.Config.HTTP_PORT)) + "api/file?path="
                                    + s.getLyricPath()
                    ));
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
        float volume = 0.5f;
        boolean mutex = true;
        boolean listFolded = false;
        int listMaxHeight = 10;
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
}
