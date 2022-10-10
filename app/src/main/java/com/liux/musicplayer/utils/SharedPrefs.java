package com.liux.musicplayer.utils;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.liux.musicplayer.models.Song;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class SharedPrefs {
    @SuppressLint("StaticFieldLeak")
    private static Context context;
    private static SharedPreferences sharedPreferences;
    private static SharedPreferences.Editor sharedPreferencesEditor;

    public static void init(Application application) {
        context = application.getApplicationContext();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        sharedPreferencesEditor = sharedPreferences.edit();
    }

    public static void saveSleepTimerToggle(boolean isSleepTimerEnabled) {
        sharedPreferencesEditor.putBoolean("KEY_IS_SLEEP_TIMER_ENABLED", isSleepTimerEnabled)
                .apply();
    }

    public static boolean isSleepTimerOn() {
        return sharedPreferences.getBoolean("KEY_IS_SLEEP_TIMER_ENABLED", false);
    }

    public static void saveSleepTime(long timeInMillis) {
        sharedPreferencesEditor.putLong("KEY_SLEEP_TIME", timeInMillis)
                .apply();
    }

    public static long getSleepTime() {
        return sharedPreferences.getLong("KEY_SLEEP_TIME",
                Calendar.getInstance().getTimeInMillis());
    }


    public static List<Song> getPlayingListFromSharedPrefer(String defaultPlayList) {
        String songListJson;
        songListJson = sharedPreferences.getString("playingList",defaultPlayList);
        Gson gson = new Gson();
        Type songListType = new TypeToken<ArrayList<Song>>() {
        }.getType();
        return gson.fromJson(songListJson, songListType);
    }

    public static void savePlayingList(List<Song> songs) {
        Gson gson = new Gson();
        Type songListType = new TypeToken<ArrayList<Song>>() {
        }.getType();
        gson.toJson(songs,songListType);
        sharedPreferencesEditor.putString("playingList",gson.toJson(songs,songListType)).apply();
    }

    public static List<Song> getSongListFromSharedPrefer(String defaultPlayList) {
        String songListJson;
        songListJson = sharedPreferences.getString("allSongList",defaultPlayList);
        Gson gson = new Gson();
        Type songListType = new TypeToken<ArrayList<Song>>() {
        }.getType();
        return gson.fromJson(songListJson, songListType);
    }

    public static void saveSongList(List<Song> songs) {
        Gson gson = new Gson();
        Type songListType = new TypeToken<ArrayList<Song>>() {
        }.getType();
        gson.toJson(songs,songListType);
        sharedPreferencesEditor.putString("allSongList",gson.toJson(songs,songListType)).apply();
    }

    public static void putIsDeskLyric(boolean b) {
        sharedPreferencesEditor.putBoolean("isShowLyric",b).apply();
    }
    public static boolean getIsDeskLyric() {
       return sharedPreferences.getBoolean("isShowLyric",false);
    }
    public static boolean getIsDeskLyricLock() {
       return sharedPreferences.getBoolean("deskLyricLock",false);
    }

    public static void putIsDeskLyricLock(boolean b) {
        sharedPreferencesEditor.putBoolean("deskLyricLock",b).apply();
    }

    public static int getPlayOrder() {
       return Integer.parseInt(sharedPreferences.getString("playOrder","0"));
    }

    public static String getCacheList(String s) {
        return sharedPreferences.getString("cacheList",s);
    }

    public static void putCacheList(String strCacheListJson) {
        sharedPreferencesEditor.putString("cacheList",strCacheListJson);
    }

    public static int getNowPlayId() {
        return Integer.parseInt(sharedPreferences.getString("nowId","0"));
    }
}
