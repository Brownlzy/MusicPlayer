package com.liux.musicplayer.utils;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import com.blankj.utilcode.util.ConvertUtils;
import com.blankj.utilcode.util.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MusicUtils {
    public static class Metadata {
        public boolean isValid = false;
        public String title = "null";
        public String album = "null";
        public String artist = "null";
        public String duration = "0";
        public String mimetype = "null";
        public String bitrate = "0";
        public String sizeByte = "0";
        public Long sizeLong = 0L;
    }

    public static class Song {
        public String title;
        public String artist;
        public String album;
        public String source_uri;
        public String lyric_uri;
        public String duration;
        public Long size;
    }

    public static class Lyric {
        public List<String> lyricList;
        public List<String> startTime;
        public List<Long> delayMillionSeconds;
        public List<Long> startMillionTime;

        public Lyric(Uri lyricUri) {
            lyricList = new ArrayList<>();
            startTime = new ArrayList<>();
            delayMillionSeconds = new ArrayList<>();
            startMillionTime = new ArrayList<>();
            try {
                File lyricFile = FileUtils.getFileByPath(lyricUri.toString());
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
                e.printStackTrace();
                Log.e("MusicUtils", "歌词文件不存在");
                lyricList.add("歌词文件不存在");
                startTime.add("00:00.00");
                delayMillionSeconds.add((long) -1);
                startMillionTime.add((long) 0);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void splitLyricFromLine(String line) {
            String lineLyric;
            try {
                lineLyric = line.split("\\[[0-9][0-9]:[0-9][0-9]\\.[0-9][0-9]\\]")[1];
            } catch (ArrayIndexOutOfBoundsException e) {
                try {
                    lineLyric = line.split("\\[[0-9][0-9]:[0-9][0-9]\\.[0-9][0-9][0-9]\\]")[1];
                } catch (ArrayIndexOutOfBoundsException e2) {
                    try {
                        lineLyric = line.split("\\[[0-9][0-9]:[0-9][0-9]\\]")[1];
                    } catch (ArrayIndexOutOfBoundsException e3) {
                        try {
                            lineLyric = line.split("\\[[0-9][0-9]:[0-9][0-9]:[0-9][0-9]\\]")[1];
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

        @Deprecated
        private void countDelayTime() {
            if (lyricList.size() > 1) {
                String time1 = startTime.get(0);
                String time2 = startTime.get(1);
                startMillionTime.add(formatTime(time1));
                for (int i = 0; i < startTime.size() - 1; i++) {
                    time1 = time2;
                    time2 = startTime.get(i + 1);
                    delayMillionSeconds.add(formatTime(time2) - formatTime(time1));
                    startMillionTime.add(formatTime(time2));
                }
            } else if (lyricList.size() > 0) {
                startMillionTime.add(formatTime(startTime.get(0)));
            }
            delayMillionSeconds.add((long) -1);
        }

        private void getStartMillionTime() {
            for (int i = 0; i < startTime.size(); i++) {
                startMillionTime.add(formatTime(startTime.get(i)));
            }
        }

        private long formatTime(String stringTime) {
            //[00:00.00]
            long mSeconds = 0;
            if (stringTime.matches("\\[[0-9][0-9]:[0-9][0-9]\\.[0-9][0-9]\\]"))
                mSeconds = Long.parseLong(stringTime.substring(1, 3)) * 60000 +
                        Long.parseLong(stringTime.substring(4, 6)) * 1000 +
                        Long.parseLong(stringTime.substring(7, 9)) * 10;
            if (stringTime.matches("\\[[0-9][0-9]:[0-9][0-9]\\.[0-9][0-9][0-9]\\]"))
                mSeconds = Long.parseLong(stringTime.substring(1, 3)) * 60000 +
                        Long.parseLong(stringTime.substring(4, 6)) * 1000 +
                        Long.parseLong(stringTime.substring(7, 10));
            if (stringTime.matches("\\[[0-9][0-9]:[0-9][0-9]\\]"))
                mSeconds = Long.parseLong(stringTime.substring(1, 3)) * 60000 +
                        Long.parseLong(stringTime.substring(4, 6)) * 1000;
            if (stringTime.matches("\\[[0-9][0-9]:[0-9][0-9]:[0-9][0-9]\\]"))
                mSeconds = Long.parseLong(stringTime.substring(1, 3)) * 60000 +
                        Long.parseLong(stringTime.substring(4, 6)) * 1000 +
                        Long.parseLong(stringTime.substring(7, 9)) * 10;
            return mSeconds;
        }

        public int size() {
            return lyricList.size();
        }

        public int getNowLyric(int currentPosition) {
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

    public static Bitmap getAlbumImage(Context context, Song song) {
        try {
            MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
            mediaMetadataRetriever.setDataSource(song.source_uri.replace("file:///storage/emulated/0", "/sdcard"));
            //获取专辑图片
            byte[] cover = mediaMetadataRetriever.getEmbeddedPicture();
            Bitmap bitmap = BitmapFactory.decodeByteArray(cover, 0, cover.length);
            mediaMetadataRetriever.release();
            return bitmap;
        } catch (IllegalArgumentException e) {
            //文件路径错误或无权限
            //Toast.makeText(context, "专辑图片读取失败", Toast.LENGTH_SHORT).show();
            return null;
        } catch (NullPointerException e) {
            //文件本身无专辑图片
            //Toast.makeText(context, "该文件无专辑图片", Toast.LENGTH_SHORT).show();
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            //Toast.makeText(context, "专辑图片读取发生未知错误", Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    public static Metadata getMetadata(Context context, MusicUtils.Song song) {
        try {
            MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
            mediaMetadataRetriever.setDataSource(song.source_uri.replace("file:///storage/emulated/0", "/sdcard"));
            Metadata md = new Metadata();
            md.title = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
            md.album = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
            md.artist = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
            md.duration = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            md.mimetype = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE);
            md.bitrate = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE);
            md.sizeLong = song.size;
            md.sizeByte = ConvertUtils.byte2FitMemorySize(md.sizeLong);
            mediaMetadataRetriever.release();
            md.isValid = true;
            return md;
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            //文件路径错误或无权限
            //Toast.makeText(context, "专辑图片读取失败", Toast.LENGTH_SHORT).show();
            //return BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_baseline_music_note_24);
        } catch (NullPointerException e) {
            e.printStackTrace();
            //文件本身无专辑图片
            //return BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_baseline_music_note_24);
        } catch (Exception e) {
            e.printStackTrace();
            //Toast.makeText(context, "专辑图片读取发生未知错误", Toast.LENGTH_SHORT).show();
            //return BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_baseline_music_note_24);
        }
        return new Metadata();
    }

    public static String millis2FitTimeSpan(long t) {
        if (t < 60000) {
            return "0:" + (t % 60000) / 1000;
        } else if (t < 3600000) {
            return (t % 3600000) / 60000 + ":" + String.format(Locale.getDefault(), "%02d", ((t % 60000) / 1000));
        } else {
            return (t / 3600000) + ":" + String.format(Locale.getDefault(), "%02d", (t % 3600000) / 60000) + ":"
                    + String.format(Locale.getDefault(), "%02d", (t % 60000) / 1000);
        }
    }

    public static String millis2FitTimeSpan(long millis, int precision) {
        if (precision <= 0) return null;
        precision = Math.min(precision, 5);
        String[] units = {"天", ":", ":", ".", ""};
        if (millis == 0) return 0 + units[precision - 1];
        StringBuilder sb = new StringBuilder();
        if (millis < 0) {
            sb.append("-");
            millis = -millis;
        }
        int[] unitLen = {86400000, 3600000, 60000, 1000, 1};
        for (int i = 0; i < precision; i++) {
            if (millis >= unitLen[i]) {
                long mode = millis / unitLen[i];
                millis -= mode * unitLen[i];
                sb.append(mode).append(units[i]);
            }
        }
        return sb.toString();
    }

    /**
     * 扫描系统里面的音频文件，返回一个list集合
     */
    public static List<Song> getMusicData(Context context) {
        List<Song> list = new ArrayList<Song>();
        // 媒体库查询语句（写一个工具类MusicUtils）
        Cursor cursor = context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null,
                null, MediaStore.Audio.Media.IS_MUSIC);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                Song song = new Song();
                //歌曲名称
                song.title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
                //歌手
                song.artist = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
                //专辑ID
                //song.albumId = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID));
                //专辑名
                song.album = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM));
                //歌曲路径
                song.source_uri = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
                //歌曲时长
                song.duration = String.valueOf(cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)));
                //歌曲大小
                song.size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE));

                if (song.size > 1000 * 800) {
                    // 注释部分是切割标题，分离出歌曲名和歌手 （本地媒体库读取的歌曲信息不规范）
                    if (song.title.contains(" - ")) {
                        String[] str = song.title.split(" - ");
                        song.artist = str[0];
                        song.title = str[1];
                    } else if (song.title.contains("-")) {
                        String[] str = song.title.split("-");
                        song.artist = str[0];
                        song.title = str[1];
                    }
                    list.add(song);
                }
            }
            // 释放资源
            cursor.close();
        }
        return list;
    }
}