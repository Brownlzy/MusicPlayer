/*
 * Copyright 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.liux.musicplayer.media;

import android.net.Uri;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;

import com.blankj.utilcode.util.FileUtils;
import com.liux.musicplayer.models.Song;
import com.liux.musicplayer.utils.MusicUtils;
import com.liux.musicplayer.utils.SharedPrefs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


public class MusicLibrary {

    private static final TreeMap<String, MediaBrowserCompat.MediaItem> music = new TreeMap<>();
    private static final TreeMap<String, MediaBrowserCompat.MediaItem> PlayingMediaItemList = new TreeMap<>();
    private static final List<Song> PlayingListOfSong =new ArrayList<>();
    private static final List<MediaDescriptionCompat> PlayingListOfDiscription=new ArrayList<>();
    //    private static final HashMap<String, Integer> albumRes = new HashMap<>();
    private static final TreeMap<String, Song> songs = new TreeMap<>();
    private static final TreeMap<String, Song> allListSongsTreeMap = new TreeMap<>();
    private static final String TAG = "MusicLibrary";
    private static final HashMap<String,List<Song>> SongLists=new HashMap<>();

    public static List<MediaBrowserCompat.MediaItem> getPlayingMediaItemList() {
        /*buildMediaItems(
                SharedPrefs.getPlayingListFromSharedPrefer("[{}]"),
                PlayingList
        );*/
        return new ArrayList<>(PlayingMediaItemList.values());
    }
    public static List<Song> getPlayingSongsList(){
        buildMediaItems(
                SharedPrefs.getPlayingListFromSharedPrefer("[{}]"),
                PlayingMediaItemList
        );
        return new ArrayList<>(allListSongsTreeMap.values());
    }

    private static void buildMediaItems(List<Song> playingListFromSharedPrefer, TreeMap<String, MediaBrowserCompat.MediaItem> playingList) {
        allListSongsTreeMap.clear();
        PlayingMediaItemList.clear();
        for (Song song : playingListFromSharedPrefer) {
            //Log.e("MusicLibrary",song.getSongPath());
            MusicLibrary.allListSongsTreeMap.put(song.getSongPath(), song);
            Log.d(TAG, "buildMediaItems: path" + song.getSongPath());
            String defaultLyricPath=song.getSongPath().substring(0,song.getSongPath().length()-(FileUtils.getFileExtension(song.getSongPath())).length())+"lrc";
            if(!FileUtils.isFileExists(song.getLyricPath())) {
                if (FileUtils.isFileExists(defaultLyricPath))
                    song.setLyricPath(defaultLyricPath);
                else
                    song.setLyricPath("null");
            }
                createMediaMetadataCompatToPlaylist(
                    song.getmId(),
                    song.getSongTitle(),
                    song.getArtistName(),
                    song.getAlbumName(),
                    song.getSongDuration(),
                    TimeUnit.MILLISECONDS,
                    song.getSongPath(),
                    song.getLyricPath());
        }

    }
    private static void createMediaMetadataCompatToPlaylist(
            String mediaId,
            String title,
            String artist,
            String album,
            long duration,
            TimeUnit durationUnit,
            String path,
            String lyric
    ) {
        Bundle extra=new Bundle();
        extra.putString("LYRIC_URI",lyric);
        PlayingMediaItemList.put(
                path,
                new MediaBrowserCompat.MediaItem(new MediaDescriptionCompat.Builder()
                        .setMediaId(mediaId)
                        //.setMediaUri(ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, Long.parseLong(mediaId)))
                        .setMediaUri(Uri.parse(path))
                        .setTitle(title)
                        .setExtras(extra)
                        .setSubtitle(artist+" - "+album)
                        .build(), MediaBrowserCompat.MediaItem.FLAG_PLAYABLE)
        );
    }
    public static MediaMetadataCompat getMetadata(Uri mediaUri) {
        Log.e("MusicPlayer",mediaUri.getPath());
        Song song = allListSongsTreeMap.get(mediaUri.getPath());
//        Log.d(Constants.TAG, "getMetadata: song " + song);
       MediaMetadataCompat.Builder  metaDataBuilder = new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, song.getmId())
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM,song.getAlbumName())
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST,song.getArtistName())
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE,song.getSongTitle())
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, song.getSongPath())
                .putString("LYRIC_URI",song.getLyricPath())
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, MusicUtils.getAlbumImage(song.getSongPath()))
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, song.getSongDuration());
        return metaDataBuilder.build();
    }

    public static void init() {
        /*buildMediaItems(
                SharedPrefs.getPlayingListFromSharedPrefer("[{}]"),
                PlayingMediaItemList
        );*/
    }
    //添加音乐
    public static void addMusicToList(String path,String listName) {
        MusicUtils.Metadata newMetadata = MusicUtils.getMetadata(path);
        Song newSong = new Song(path,newMetadata.title,newMetadata.artist,newMetadata.album,newMetadata.duration,newMetadata.sizeLong);
        List<Song> theList=SharedPrefs.getSongListByName(listName);
        if (theList.stream().map(t -> t.getSongPath()).distinct().collect(Collectors.toList()).contains(path)) {  //如果播放列表已有同路径的音乐，就更新其内容
            theList.set(theList.stream().map(t -> t.getSongPath()).distinct().collect(Collectors.toList()).indexOf(path), newSong);
        } else {
            if (theList.size() == 1 && !FileUtils.isFileExists(theList.get(0).getSongPath())) {  //播放列表第一位如果是示例数据则将其替换
                theList.set(0, newSong);
            } else {
                theList.add(newSong);
            }
        }
        SharedPrefs.saveSongListByName(theList,listName);
    }

    public static List<Song> getSongListByName(String name){
        if(SongLists.containsKey(name))
            return SongLists.get(name);
        else {
            List<Song> newSongList=SharedPrefs.getSongListByName(name);
            SongLists.put(name,newSongList);
            for (Song song:newSongList){
                allListSongsTreeMap.put(song.getSongPath(),song);
            }
            return newSongList;
        }
    }

    public static List<MediaDescriptionCompat>getNewPlayingList(String name){
        PlayingListOfSong.clear();
        PlayingListOfSong.addAll(SharedPrefs.getSongListByName(name));
            for (Song song:PlayingListOfSong) {
                PlayingListOfDiscription.add(getMediaItemDescription(song));
                allListSongsTreeMap.put(song.getSongPath(),song);
            }
            return PlayingListOfDiscription;
    }

    public static MediaDescriptionCompat getMediaItemDescription(Song song) {
        Bundle extra=new Bundle();
        extra.putString("LYRIC_URI",song.getLyricPath());
        return new MediaBrowserCompat.MediaItem(new MediaDescriptionCompat.Builder()
                .setMediaId(song.getSongPath())
                .setMediaUri(Uri.parse(song.getSongPath()))
                .setTitle(song.getSongTitle())
                .setExtras(extra)
                .setSubtitle(song.getArtistName()+" - "+song.getAlbumName())
                .build(), MediaBrowserCompat.MediaItem.FLAG_PLAYABLE).getDescription();
    }

    public static List<MediaDescriptionCompat>getPlayingList(){
        if(PlayingListOfSong.isEmpty()){
            PlayingListOfSong.addAll(SharedPrefs.getSongListByName("playingList"));
        }
        for (Song song:PlayingListOfSong) {
            PlayingListOfDiscription.add(getMediaItemDescription(song));
            allListSongsTreeMap.put(song.getSongPath(),song);
        }
        return PlayingListOfDiscription;
    }

    public static void savePlayingList(List<MediaSessionCompat.QueueItem> mPlaylist) {
        PlayingListOfSong.clear();
        for (MediaSessionCompat.QueueItem queueItem:mPlaylist) {
            Song song=new Song(queueItem.getDescription().getMediaUri().getPath(),
                    String.valueOf(queueItem.getDescription().getTitle()),
                    String.valueOf(queueItem.getDescription().getSubtitle()).split(" - ")[0],
                    String.valueOf(queueItem.getDescription().getSubtitle()).split(" - ")[1],
                    queueItem.getDescription().getExtras().getString("LYRIC_URI","null"));
            PlayingListOfSong.add(song);
        }
        SharedPrefs.saveSongListByName(PlayingListOfSong,"playingList");
    }
}