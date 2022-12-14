package com.liux.musicplayer.utils;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.liux.musicplayer.media.MusicLibrary;
import com.liux.musicplayer.models.Song;
import com.liux.musicplayer.models.UserData;

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

    public static List<MusicLibrary.SongList> getAllSonglistList(){
        Gson gson = new Gson();
        Type songListListType = new TypeToken<ArrayList<MusicLibrary.SongList>>() {
        }.getType();
        List<MusicLibrary.SongList> list;
        list=gson.fromJson(sharedPreferences.getString("allSonglistList","[]"),songListListType);
        if(list==null||list.size()==0) {
            list=new ArrayList<>();
            list.add(0,new MusicLibrary.SongList("allSongList","",0));
        }
        if(!getIsUseWebPlayList()){
            list.removeIf(songList -> songList.n.equals("webAllSongList"));
        }
        return list;
    }

    public static boolean getIsUseWebPlayList() {
        return sharedPreferences.getBoolean("isUseWebPlayList",false);
    }

    public static void putAllSonglistList(List<MusicLibrary.SongList> list){
        Gson gson = new Gson();
        Type songListListType = new TypeToken<ArrayList<MusicLibrary.SongList>>() {
        }.getType();
        String listJson=gson.toJson(list,songListListType);
        sharedPreferencesEditor.putString("allSonglistList",listJson).apply();
    }

    public static int getVersionCode(){
       return sharedPreferences.getInt("versionCode",-1);
    }

    public static void putVersionCode(){
        int code=-1;
        try {
            code = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        sharedPreferencesEditor.putInt("versionCode",code).apply();
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

/*
    public static List<Song> getPlayingListFromSharedPrefer(String defaultPlayList) {
        String songListJson;
        songListJson = sharedPreferences.getString("playingList",defaultPlayList);
        Gson gson = new Gson();
        Type songListType = new TypeToken<ArrayList<Song>>() {
        }.getType();
        return gson.fromJson(songListJson, songListType);
    }*/

    public static UserData getUserData() {
        String userDataJson;
        userDataJson = sharedPreferences.getString("user","");
        Gson gson = new Gson();
        return gson.fromJson(userDataJson, UserData.class);
    }

    public static void saveUserData(UserData userData) {
        Gson gson = new Gson();
        String userDataJson = gson.toJson(userData, UserData.class);
        sharedPreferencesEditor.putString("user",userDataJson).apply();
    }
/*
    public static void savePlayingList(List<Song> songs) {
        Gson gson = new Gson();
        Type songListType = new TypeToken<ArrayList<Song>>() {
        }.getType();
        gson.toJson(songs,songListType);
        sharedPreferencesEditor.putString("playingList",gson.toJson(songs,songListType)).apply();
    }*/

    /*public static List<Song> getSongListFromSharedPrefer(String defaultPlayList) {
        String songListJson;
        songListJson = sharedPreferences.getString("allSongList",defaultPlayList);
        Gson gson = new Gson();
        Type songListType = new TypeToken<ArrayList<Song>>() {
        }.getType();
        return gson.fromJson(songListJson, songListType);
    }*/

    /*public static void saveSongList(List<Song> songs) {
        Gson gson = new Gson();
        Type songListType = new TypeToken<ArrayList<Song>>() {
        }.getType();
        gson.toJson(songs,songListType);
        sharedPreferencesEditor.putString("allSongList",gson.toJson(songs,songListType)).apply();
    }*/

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
        sharedPreferencesEditor.putString("cacheList",strCacheListJson).apply();
    }

    public static int getNowPlayId() {
        return Integer.parseInt(sharedPreferences.getString("nowId","-1"));
    }

    public static void savePlayOrder(int playOrder) {
        sharedPreferencesEditor.putString("playOrder",String.valueOf(playOrder)).apply();
    }

    public static void saveNowPlayId(int mQueueIndex) {
        sharedPreferencesEditor.putString("nowId",String.valueOf(mQueueIndex)).apply();
    }

    public static List<Song> getSongListByName(String listName) {
        String songListJson;
        songListJson = sharedPreferences.getString("SONGLIST_"+listName,"");
        Gson gson = new Gson();
        Type songListType = new TypeToken<ArrayList<Song>>() {
        }.getType();
        List<Song> aList=gson.fromJson(songListJson, songListType);
        if(aList!=null)
            return aList;
        else
            return new ArrayList<>();
    }

    public static void saveSongListByName(List<Song> theList, String listName) {
        Gson gson = new Gson();
        Type songListType = new TypeToken<ArrayList<Song>>() {
        }.getType();
        sharedPreferencesEditor.putString("SONGLIST_"+listName,gson.toJson(theList,songListType)).apply();
    }

    public static CharSequence getQueueTitle() {
        return sharedPreferences.getString("QueueTitle","playingList");
    }
    public static void saveQueueTitle(String queueTitle) {
        sharedPreferencesEditor.putString("QueueTitle",queueTitle).apply();
    }

    public static int getTiming() {
        return sharedPreferences.getInt("timing",0);
    }
    public static void putTiming(int timing) {
        sharedPreferencesEditor.putInt("timing",timing).apply();
    }
    public static long getLastCheckUpdateTime() {
       return sharedPreferences.getLong("lastCheckUpdate",0L);
    }
    public static void putLastCheckUpdateTime(long date) {
        sharedPreferencesEditor.putLong("lastCheckUpdate",date).apply();
    }
    public static void putLastNewsUpdateTime(long date) {
       sharedPreferencesEditor.putLong("lastNewsUpdate",date).apply();
    }
    public static long getLastNewsUpdateTime() {
       return sharedPreferences.getLong("lastNewsUpdate",0L);
    }
    public static void putLastNewsId(int newsId) {
       sharedPreferencesEditor.putInt("lastNewsId",newsId).apply();
    }
    public static int getLastNewsId() {
        return sharedPreferences.getInt("lastNewsId",-1);
    }

    public static boolean getIsNeedFastStart() {
        return sharedPreferences.getBoolean("isNeedFastStart",false);
    }
    public static void putIsNeedFastStart(boolean isNeedFastStart) {
        sharedPreferencesEditor.putBoolean("isNeedFastStart",isNeedFastStart).apply();
    }

    public static void cleanOldData() {
        sharedPreferencesEditor.putString("playList","").apply();
    }

    public static void putIsUseWebPlayList(boolean b) {
        sharedPreferencesEditor.putBoolean("isUseWebPlayList",false);
    }

    public static boolean isUseMetaData() {
        return sharedPreferences.getBoolean("isUseMetaData",true);
    }

    public static void putLastVersion(int lastVersionCode,String lastVersionName) {
        sharedPreferencesEditor.putInt("lastVersionCode",lastVersionCode);
        sharedPreferencesEditor.putString("lastVersionName",lastVersionName);
        sharedPreferencesEditor.apply();
    }

    public static int getLastVersionCode(){
        return sharedPreferences.getInt("lastVersionCode",0);
    }
    public  static String getLastVersionName(){
        return sharedPreferences.getString("lastVersionName","null");
    }

    public static int getSplashType() {
        try {
            return Integer.parseInt(sharedPreferences.getString("splashType", "0"));
        } catch (Exception e) {
            return 0;
        }
    }

    public static void cleanSplashPath() {
        sharedPreferencesEditor.remove("splashPicPath").apply();
    }

    public static void setWebServerEnable(boolean b) {
        sharedPreferencesEditor.putBoolean("isEnableWebServer", b).apply();
    }

    public static boolean getIsWebServerEnable() {
        return sharedPreferences.getBoolean("isEnableWebServer", false);
    }

    public static void putExitFlag(boolean b) {
        sharedPreferencesEditor.putBoolean("ExitFlag", b).apply();
    }

    public static boolean getExitFlag() {
        return sharedPreferences.getBoolean("ExitFlag", false);
    }
}
