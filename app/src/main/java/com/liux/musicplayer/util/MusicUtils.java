package com.liux.musicplayer.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

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

public class MusicUtils {
    public static class Metadata {
        public boolean isValid = false;
        public String title = "null";
        public String album = "null";
        public String artist = "null";
        public String duration = "null";
        public String mimetype = "null";
        public String bitrate = "null";
    }

    public static class Song {
        public int id;
        public Long songId;
        public int albumId;
        public String title;
        public String artist;
        public String album;
        public String filename;
        public String source_uri;
        public String lyric_uri;
        public String duration;
        public Long size;
    }

    public static class Lyric {
        public List<String> lyricList;
        public List<String> startTime;
        public List<Long> delayMillionSeconds;

        public Lyric(Uri lyricUri) {
            lyricList = new ArrayList<>();
            startTime = new ArrayList<>();
            delayMillionSeconds = new ArrayList<>();
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
                }
                inStream.close();
                countDelayTime();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Log.e("MusicUtils", "歌词文件不存在");
                lyricList.add("歌词文件不存在");
                startTime.add("00:00.00");
                delayMillionSeconds.add((long) -1);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void splitLyricFromLine(String line) {
            String lineLyric;
            try {
                lineLyric = line.split("\\[[0-9][0-9]:[0-9][0-9]\\.[0-9][0-9]\\]")[1];
            } catch (ArrayIndexOutOfBoundsException e) {
                return;
            }
            if (lineLyric != null) {
                lyricList.add(lineLyric);
                startTime.add(line.substring(1, 9));
            }
        }

        private void countDelayTime() {
            if (lyricList.size() > 1) {
                String time1 = startTime.get(0);
                String time2 = startTime.get(1);
                for (int i = 0; i < startTime.size() - 1; i++, time1 = time2, time2 = startTime.get(i)) {
                    delayMillionSeconds.add(formatTime(time2) - formatTime(time1));
                }
            }
            delayMillionSeconds.add((long) -1);
        }

        private long formatTime(String stringTime) {
            //00:00.00
            long mSeconds;
            mSeconds = Long.parseLong(stringTime.substring(0, 2)) * 60000 +
                    Long.parseLong(stringTime.substring(3, 5)) * 1000 +
                    Long.parseLong(stringTime.substring(6, 8)) * 10;
            return mSeconds;
        }

        public int size() {
            return lyricList.size();
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
            Toast.makeText(context, "专辑图片读取失败", Toast.LENGTH_SHORT).show();
            return null;
        } catch (NullPointerException e) {
            //文件本身无专辑图片
            Toast.makeText(context, "该文件无专辑图片", Toast.LENGTH_SHORT).show();
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "专辑图片读取发生未知错误", Toast.LENGTH_SHORT).show();
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
}