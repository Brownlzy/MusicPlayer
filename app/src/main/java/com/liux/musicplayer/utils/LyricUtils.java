package com.liux.musicplayer.utils;

import android.content.Context;
import android.util.Log;

import com.blankj.utilcode.util.FileUtils;
import com.blankj.utilcode.util.PathUtils;
import com.blankj.utilcode.util.TimeUtils;
import com.liux.musicplayer.models.Song;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class LyricUtils implements Serializable {
    public boolean isCompleted = false;
    public List<String> lyricList;
    public List<String> startTime;
    public List<Long> startMillionTime;
    private Context mContext;
    private List<OnLyricLoadCallback> onLyricLoadCallbackList = new ArrayList<>();

    public interface OnLyricLoadCallback {
        void LyricLoadCompleted();
    }

    public void setOnLyricLoadCallback(OnLyricLoadCallback mOnLyricLoadCallback) {
        this.onLyricLoadCallbackList.add(mOnLyricLoadCallback);
    }

    public void sendCompleted() {
        for (OnLyricLoadCallback o : onLyricLoadCallbackList) {
            if (o != null)
                o.LyricLoadCompleted();
        }
    }
    public LyricUtils( Song song) {
        LoadLyric(song);
    }
    public LyricUtils( String path) {
        LoadLyric(path);
    }
    public LyricUtils( ) {
    }

    public void LoadLyric(Song song) {
        isCompleted = false;
        lyricList = new ArrayList<>();
        startTime = new ArrayList<>();
        startMillionTime = new ArrayList<>();
        if (FileUtils.isFileExists(song.getLyricPath()))
            LyricFromFile(song.getLyricPath());
        else if (song.getLyricPath()!=null&&song.getLyricPath().matches("^(https?)://[-A-Za-z0-9+&@#/%?=~_|!:,.;]+[-A-Za-z0-9+&@#/%=~_|]+[\\S\\s]*"))
            LyricFromUrl(song.getLyricPath());
        else {
            lyricList.add("歌词文件不存在");
            startTime.add("[00:00.00]");
            startMillionTime.add((long) 0);
            sendCompleted();
            isCompleted = true;
        }
    }
/*
    public void LoadLyric(String s) {
        isCompleted = false;
        lyricList = new ArrayList<>();
        startTime = new ArrayList<>();
        startMillionTime = new ArrayList<>();
        lyricList.add(s);
        startTime.add("[00:00.00]");
        startMillionTime.add((long) 0);
        sendCompleted();
        isCompleted = true;
    }*/
    public void LoadLyric(String s) {
        isCompleted = false;
        lyricList = new ArrayList<>();
        startTime = new ArrayList<>();
        startMillionTime = new ArrayList<>();
        if (FileUtils.isFileExists(s))
            LyricFromFile(s);
        else if (s!=null&&s.matches("^(https?)://[-A-Za-z0-9+&@#/%?=~_|!:,.;]+[-A-Za-z0-9+&@#/%=~_|]+[\\S\\s]*"))
            LyricFromUrl(s);
        else {
            lyricList.add("歌词文件不存在");
            startTime.add("[00:00.00]");
            startMillionTime.add((long) 0);
            sendCompleted();
            isCompleted = true;
        }
    }

    private void LyricFromFile(String path) {
        try {
            File lyricFile = FileUtils.getFileByPath(path);
            InputStream inStream = null;
            inStream = new FileInputStream(lyricFile);
            InputStreamReader inputReader = new InputStreamReader(inStream);
            BufferedReader buffReader = new BufferedReader(inputReader);
            String line;
            //分行读取
            while ((line = buffReader.readLine()) != null) {
                splitLyricFromLine(line);
                //Log.e("Lyric",line);
            }
            inStream.close();
            getStartMillionTime();
        } catch (FileNotFoundException e) {
            //e.printStackTrace();
            lyricList.add("歌词文件不存在");
            startTime.add("[00:00.00]");
            startMillionTime.add((long) 0);
        } catch (IOException e) {
            //e.printStackTrace();
            lyricList.add("歌词文件加载失败");
            startTime.add("[00:00.00]");
            startMillionTime.add((long) 0);
        }
        sendCompleted();
        isCompleted = true;
    }

    public void LyricFromUrl(String s) {
        UploadDownloadUtils uploadDownloadUtils = new UploadDownloadUtils(mContext);
        uploadDownloadUtils.set0nImageLoadListener(new UploadDownloadUtils.OnImageLoadListener() {
            @Override
            public void onFileDownloadCompleted(ArrayList<String> array) {
                Log.e("lyric", array.get(0));
                if (!array.get(0).equals(s))
                    return;
                handleUrlResult(array.get(1));
            }

            @Override
            public void onFileDownloadError(ArrayList<String> array) {
                if (!array.get(0).equals(s))
                    return;
                lyricList.add("歌词文件加载失败");
                startTime.add("[00:00.00]");
                startMillionTime.add((long) 0);

            }
        });
        uploadDownloadUtils.downloadFile(PathUtils.getExternalAppCachePath(), TimeUtils.getNowMills() + ".lrc", s);
    }

    private void handleUrlResult(String resultFilePath) {
        try {
            File lyricFile = FileUtils.getFileByPath(resultFilePath);
            InputStream inStream = null;
            inStream = new FileInputStream(lyricFile);
            InputStreamReader inputReader = new InputStreamReader(inStream);
            BufferedReader buffReader = new BufferedReader(inputReader);
            String line;
            //分行读取
            while ((line = buffReader.readLine()) != null) {
                splitLyricFromLine(line);
                //Log.e("Lyric",line);
            }
            inStream.close();
            getStartMillionTime();
        } catch (IOException e) {
            lyricList.add("歌词文件加载失败");
            startTime.add("[00:00.00]");
            startMillionTime.add((long) 0);
            //e.printStackTrace();
        }
        sendCompleted();
        isCompleted = true;
    }

    private void splitLyricFromLine(String line) {
        String lineLyric;
        try {
            lineLyric = line.split("\\[[0-9][0-9]:[0-9][0-9]\\.[0-9][0-9]]")[1];
        } catch (ArrayIndexOutOfBoundsException e) {
            try {
                lineLyric = line.split("\\[[0-9][0-9]:[0-9][0-9]\\.[0-9][0-9][0-9]]")[1];
            } catch (ArrayIndexOutOfBoundsException e2) {
                try {
                    lineLyric = line.split("\\[[0-9][0-9]:[0-9][0-9]]")[1];
                } catch (ArrayIndexOutOfBoundsException e3) {
                    try {
                        lineLyric = line.split("\\[[0-9][0-9]:[0-9][0-9]:[0-9][0-9]]")[1];
                    } catch (ArrayIndexOutOfBoundsException e4) {
                        return;
                    }
                }
            }
        }
        if (lineLyric != null) {
            int lyricTextStartIndex = line.indexOf(lineLyric);
            if (lyricTextStartIndex == -1) lyricTextStartIndex = 0;
            int sameTimeId = startTime.indexOf(line.substring(0, lyricTextStartIndex));
            if (sameTimeId == -1) {
                lyricList.add(lineLyric);
                startTime.add(line.substring(0, lyricTextStartIndex));
            } else {//有相同的时间
                lyricList.set(sameTimeId, lyricList.get(sameTimeId) + "\n" + lineLyric);
            }
        }
    }

    private void getStartMillionTime() {
        for (int i = 0; i < startTime.size(); i++) {
            startMillionTime.add(formatTime(startTime.get(i)));
        }
    }

    private long formatTime(String stringTime) {
        //[00:00.00]
        long mSeconds = 0;
        if (stringTime.matches("\\[[0-9][0-9]:[0-9][0-9]\\.[0-9][0-9]]"))
            mSeconds = Long.parseLong(stringTime.substring(1, 3)) * 60000 +
                    Long.parseLong(stringTime.substring(4, 6)) * 1000 +
                    Long.parseLong(stringTime.substring(7, 9)) * 10;
        if (stringTime.matches("\\[[0-9][0-9]:[0-9][0-9]\\.[0-9][0-9][0-9]]"))
            mSeconds = Long.parseLong(stringTime.substring(1, 3)) * 60000 +
                    Long.parseLong(stringTime.substring(4, 6)) * 1000 +
                    Long.parseLong(stringTime.substring(7, 10));
        if (stringTime.matches("\\[[0-9][0-9]:[0-9][0-9]]"))
            mSeconds = Long.parseLong(stringTime.substring(1, 3)) * 60000 +
                    Long.parseLong(stringTime.substring(4, 6)) * 1000;
        if (stringTime.matches("\\[[0-9][0-9]:[0-9][0-9]:[0-9][0-9]]"))
            mSeconds = Long.parseLong(stringTime.substring(1, 3)) * 60000 +
                    Long.parseLong(stringTime.substring(4, 6)) * 1000 +
                    Long.parseLong(stringTime.substring(7, 9)) * 10;
        return mSeconds;
    }

    public int size() {
        return lyricList.size();
    }

    public int getNowLyric(long currentPosition) {
        if (lyricList.size() > 0) {
            for (int i = 0; i < lyricList.size(); i++) {
                if (startMillionTime.get(i) > currentPosition)
                    return (i - 1 >= 0) ? i - 1 : i;
            }
            return lyricList.size() - 1;
        } else {
            return -1;
        }
    }
}

