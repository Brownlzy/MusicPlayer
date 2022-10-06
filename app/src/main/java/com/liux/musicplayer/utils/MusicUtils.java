package com.liux.musicplayer.utils;

import static com.blankj.utilcode.util.ThreadUtils.runOnUiThread;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import com.blankj.utilcode.util.ConvertUtils;
import com.blankj.utilcode.util.FileUtils;
import com.blankj.utilcode.util.PathUtils;
import com.blankj.utilcode.util.TimeUtils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

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
        public String memory = "null";
        public String source_uri = "null";
        public String lyric_uri = "null";
        public String duration = "0";
        public Long size = 0L;
    }

    public static Bitmap getAlbumImage(Context context, String path) {
        try {
            MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
            mediaMetadataRetriever.setDataSource(path);
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

    public static Metadata getMetadata(Context context, String path) {
        try {
            MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
            mediaMetadataRetriever.setDataSource(path);
            Metadata md = new Metadata();
            md.sizeLong = FileUtils.getLength(path);
            md.sizeByte = ConvertUtils.byte2FitMemorySize(md.sizeLong);
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

    public static Metadata getMetadataFromSong(Song song) {
        Metadata md = new Metadata();
        md.title = song.title;
        md.album = song.album;
        md.artist = song.artist;
        md.duration = song.duration;
        md.mimetype = "null";
        md.bitrate = "0";
        md.sizeLong = 0L;
        md.sizeByte = ConvertUtils.byte2FitMemorySize(md.sizeLong);
        md.isValid = true;
        return md;
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