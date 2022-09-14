package com.liux.musicplayer;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.Uri;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.blankj.utilcode.util.FileUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.liux.musicplayer.util.MusicUtils;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class MusicPlayer {
    private final static MediaPlayer mp = new MediaPlayer();
    private List<MusicUtils.Song> songList;
    private int nowId;
    private MainActivity mainActivity;
    //0=顺序播放 1=列表循环 2=单曲循环 3=随机播放
    private int playOrder;
    public static final int LIST_PLAY = 0;
    public static final int REPEAT_LIST = 1;
    public static final int REPEAT_ONE = 2;
    public static final int SHUFFLE_PLAY = 3;
    private final Context mContext;
    private SharedPreferences sp;
    private List<Integer> shuffleOrder;
    private int shuffleId;
    private boolean isLyric = false;
    private boolean prepared = false;

    public MusicPlayer(MainActivity mMainActivity, Context context) {
        songList = new ArrayList<>();
        nowId = 0;
        playOrder = 0;
        mContext = context;
        mainActivity = mMainActivity;
        sp = mContext.getSharedPreferences("com.liux.musicplayer_preferences", Activity.MODE_PRIVATE);
        readPlayList();
        setMediaPlayerListener();
    }

    public boolean isLyric() {
        return isLyric;
    }

    public void setLyric(boolean lyric) {
        isLyric = lyric;
    }

    public void playPrevOrNext(boolean isNext) {
        int maxId = getMaxID();
        int nowId = getNowId();
        int order = getPlayOrder();
        switch (order) {
            case SHUFFLE_PLAY:
                if (isNext) {
                    if (shuffleId < shuffleOrder.size() - 1) {
                        shuffleId += 1;
                    } else {
                        shuffleId = 0;
                    }
                } else {
                    if (shuffleId > 0) {
                        shuffleId -= 1;
                    } else {
                        shuffleId = shuffleOrder.size() - 1;
                    }
                }
                nowId = shuffleOrder.get(shuffleId);
                break;
            case REPEAT_ONE:
            case REPEAT_LIST:
                if (isNext) {
                    if (nowId < maxId) nowId += 1;
                    else nowId = 0;
                } else {
                    if (nowId > 0) nowId -= 1;
                    else nowId = maxId;
                }
                break;
            default:
            case LIST_PLAY:
                if (isNext) {
                    if (nowId < maxId)
                        nowId += 1;
                    else
                        return;
                } else {
                    if (nowId > 0)
                        nowId -= 1;
                    else
                        nowId = 0;
                }
                break;
        }
        playThisNow(nowId);
    }

    private void setMediaPlayerListener() {
        //MediaPlayer准备资源的监听器
        mp.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
            }
        });
        //音频播放完成的监听器
        mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                //把所有的都回归到0
                prepared = false;
                mainActivity.resetPlayProgress();
                playPrevOrNext(true);
            }
        });
    }

    public void refreshPlayList() {
        readPlayList();
    }

    private void readPlayList() {
        nowId = Integer.parseInt(sp.getString("nowId", "0"));
        String playListJson = sp.getString("playList",
                "[{\"id\":-1,\"title\":\"这是音乐标题\",\"artist\":\"这是歌手\",\"album\":\"这是专辑名\",\"filename\":\"此为测试数据，添加音乐文件后自动删除\"," +
                        "\"source_uri\":\"file:///storage/emulated/0/Android/data/com.liux.musicplayer/Music/这是歌手 - 这是音乐标题.mp3\"," +
                        "\"lyric_uri\":\"file:///storage/emulated/0/Android/data/com.liux.musicplayer/Music/这是歌手 - 这是音乐标题.lrc\"}]");
        Gson gson = new Gson();
        Type playListType = new TypeToken<ArrayList<MusicUtils.Song>>() {
        }.getType();
        songList = gson.fromJson(playListJson, playListType);
        if (songList == null || songList.size() == 0) {
            playListJson = "[{\"id\":-1,\"title\":\"这是音乐标题\",\"artist\":\"这是歌手\",\"album\":\"这是专辑名\",\"filename\":\"此为测试数据，添加音乐文件后自动删除\"," +
                    "\"source_uri\":\"file:///storage/emulated/0/Android/data/com.liux.musicplayer/Music/这是歌手 - 这是音乐标题.mp3\"," +
                    "\"lyric_uri\":\"file:///storage/emulated/0/Android/data/com.liux.musicplayer/Music/这是歌手 - 这是音乐标题.lrc\"}]";
            songList = gson.fromJson(playListJson, playListType);
        }
        if (nowId >= songList.size()) nowId = 0;
        setPlayOrder(Integer.parseInt(sp.getString("playOrder", "0")));
        mainActivity.setPlayOrder(playOrder);
    }

    public void setPlayList(List<MusicUtils.Song> newSongList) {
        songList = newSongList;
        savePlayList();
    }

    private void savePlayList() {
        Gson gson = new Gson();
        Type playListType = new TypeToken<ArrayList<MusicUtils.Song>>() {
        }.getType();
        String playListJson = gson.toJson(songList, playListType);
        SharedPreferences sp = mContext.getSharedPreferences("com.liux.musicplayer_preferences", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("playList", playListJson);
        editor.apply();
    }

    //遍历文件夹
    private List<String> ergodicFolder(String path) {

        return new ArrayList<>();
    }

    //添加音乐
    public int addMusic(String path) {
        if (songList.stream().map(t -> t.source_uri).distinct().collect(Collectors.toList()).contains(path)) {
            return 0;
        } else {
            MusicUtils.Song newSong = new MusicUtils.Song();
            newSong.source_uri = path;
            MusicUtils.Metadata newMetadata = MusicUtils.getMetadata(mContext, newSong);
            if (newMetadata.isValid) {
                newSong.title = newMetadata.title;
                newSong.artist = newMetadata.artist;
                newSong.album = newMetadata.album;
            }
            if (newSong.album == null) newSong.album = "null";
            if (FileUtils.getFileNameNoExtension(path).matches(".* - .*")) {
                if (newSong.title == null)
                    newSong.title = FileUtils.getFileNameNoExtension(path).split(" - ")[1];
                if (newSong.artist == null)
                    newSong.artist = FileUtils.getFileNameNoExtension(path).split(" - ")[0];
            } else if (FileUtils.getFileNameNoExtension(path).matches(".*-.*")) {
                if (newSong.title == null)
                    newSong.title = FileUtils.getFileNameNoExtension(path).split("-")[1];
                if (newSong.artist == null)
                    newSong.artist = FileUtils.getFileNameNoExtension(path).split("-")[0];
            } else {
                if (newSong.title == null) newSong.title = FileUtils.getFileNameNoExtension(path);
                if (newSong.artist == null) newSong.artist = "null";
            }
            //判断是否存在歌词
            if (FileUtils.isFileExists(path.replace(FileUtils.getFileExtension(path), "lrc")))
                newSong.lyric_uri = path.replace(FileUtils.getFileExtension(path), "lrc");
            else
                newSong.lyric_uri = "null";

            if (songList.size() == 1 && !FileUtils.isFileExists(songList.get(0).source_uri)) {
                songList.set(0, newSong);
            } else {
                songList.add(newSong);
            }
            savePlayList();
        }
        return 0;
    }

    //删除音乐
    public int deleteMusic(int[] array) {

        return 0;
    }

    public List<MusicUtils.Song> getPlayList() {
        return songList;
    }

    public int getNowId() {
        return nowId;
    }

    public int getMaxID() {
        return songList.size() - 1;
    }

    public int getPlayOrder() {
        return playOrder;
    }

    public MediaPlayer getMediaPlayer() {
        return mp;
    }

    public void setPlayOrder(int order) {
        playOrder = order;
        savePlayOrder();
        if (playOrder == SHUFFLE_PLAY) {
            mp.setLooping(false);
            shuffleOrder = new ArrayList<>();
            for (int i = 0; i < songList.size(); i++) {
                if (i != nowId)
                    shuffleOrder.add(i);
            }
            Collections.shuffle(shuffleOrder);
            shuffleOrder.add(nowId);
            shuffleId = shuffleOrder.size() - 1;
        } else if (playOrder == REPEAT_ONE) {
            mp.setLooping(true);
        } else {
            mp.setLooping(false);
        }
    }

    private void savePlayOrder() {
        SharedPreferences.Editor spEditor = sp.edit();
        spEditor.putString("playOrder", String.valueOf(playOrder));
        spEditor.apply();
    }

    public void setNowId(int id) {
        nowId = id;
    }

    public void playThisNow(int musicId) {
        switch (playThis(musicId)) {
            case 0:
                if (playOrder == REPEAT_ONE)
                    mp.setLooping(true);
                mainActivity.setPlayBarTitle(musicId);
                mainActivity.setHomeFragment();
                //初始化进度条
                mainActivity.resetPlayProgress();
                //开启进度条跟踪线程
                mainActivity.startProgressBar();
                mainActivity.startLyric();
                ;
                mainActivity.setPlayOrPause(true);
                break;
            default:
            case -1:
                mainActivity.setPlayBarTitle(musicId);
                mainActivity.setHomeFragment();
                AlertDialog alertInfoDialog = new AlertDialog.Builder(mContext)
                        .setTitle(R.string.play_error)
                        .setMessage(R.string.play_err_Info)
                        .setIcon(R.mipmap.ic_launcher)
                        .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mainActivity.setPlayOrPause(false);
                            }
                        })
                        .create();
                alertInfoDialog.show();
                break;
        }
    }

    private int playThis(int id) {
        int reId;
        nowId = id;
        SharedPreferences.Editor spEditor = sp.edit();
        spEditor.putString("nowId", String.valueOf(nowId));
        spEditor.apply();
        if (nowId > getMaxID()) {
            reId = -1;
        } else {
            reId = playThis(Uri.parse(songList.get(nowId).source_uri));
            if (reId == 0 && prepared)
                mp.start();
        }
        return reId;
    }

    public int playThis(Uri musicPath) {
        if (FileUtils.isFileExists(musicPath.getPath())) {
            try {
                mp.reset();
                mp.setDataSource(mContext, musicPath);
                mp.prepare();
                prepared = true;
            } catch (IOException e) {
                e.printStackTrace();
                mp.reset();
                return -1;
            }
        } else {
            Toast.makeText(mainActivity, "文件不存在", Toast.LENGTH_SHORT).show();
            return -1;
        }
        return 0;
    }

    public void pause() {
        mp.pause();
    }

    public void start() {
        if (prepared)
            mp.start();
    }

    public void setProgress(int second) {
    }

    public boolean isPlaying() {
        return mp.isPlaying();
    }

    public boolean isPrepared() {
        return prepared;
    }
}

