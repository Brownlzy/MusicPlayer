package com.liux.musicplayer.utils;

import static android.content.Context.ACTIVITY_SERVICE;

import android.app.ActivityManager;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.provider.MediaStore;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.blankj.utilcode.util.ConvertUtils;
import com.blankj.utilcode.util.FileUtils;
import com.liux.musicplayer.media.MusicLibrary;
import com.liux.musicplayer.models.Song;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
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

//    public static class Song {
//        public String title;
//        public String artist;
//        public String album;
//        public String memory = "null";
//        public String source_uri = "null";
//        public String lyric_uri = "null";
//        public String duration = "0";
//        public Long size = 0L;
//    }

    public static class PlayList{
        public String name;
        public List<Song> list=new ArrayList<>();
    }

    public static Bitmap getAlbumImage(String path) {
        try {
            if(path.matches("^(https?)://[-A-Za-z0-9+&@#/%?=~_|!:,.;]+[-A-Za-z0-9+&@#/%=~_|]+[\\S\\s]*"))
                return getWebAlbumImage(path);
            MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
            mediaMetadataRetriever.setDataSource(path);
            //获取专辑图片
            byte[] cover = mediaMetadataRetriever.getEmbeddedPicture();
            Bitmap bitmap = BitmapFactory.decodeByteArray(cover, 0, cover.length);
            mediaMetadataRetriever.release();
            return bitmap;
        } catch (Exception e) {
            //e.printStackTrace();
            //Toast.makeText(context, "专辑图片读取发生未知错误", Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    static Bitmap bitmap;

    public static Bitmap getBitmap(String netUrl) {
        URL url = null;
        try {
            url = new URL(netUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setRequestMethod("GET");
            if (conn.getResponseCode() == 200) {
                InputStream inputStream = conn.getInputStream();
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                return bitmap;
            } else {
                Bitmap bitmap = null;
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                return bitmap;
            }
        } catch (Exception e) {
            e.printStackTrace();
            Bitmap bitmap = null;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            return bitmap;
        }
    }

    private static Bitmap getWebAlbumImage(String path) {
        if (path.contains("/api/file?path=")) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    bitmap = getBitmap(path.replace("/api/file?path=", "/api/cover?path="));
                }
            }).start();

            return bitmap;
        } else
            return null;
    }

    public static Metadata getMetadata(String path) {
        try {
            if(path.matches("^(https?)://[-A-Za-z0-9+&@#/%?=~_|!:,.;]+[-A-Za-z0-9+&@#/%=~_|]+[\\S\\s]*"))
                return getWebMetadata(path);
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
        } catch (Exception e) {
            //e.printStackTrace();
            //Toast.makeText(context, "专辑图片读取发生未知错误", Toast.LENGTH_SHORT).show();
            //return BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_baseline_music_note_24);
        }
        return new Metadata();
    }

    private static Metadata getWebMetadata(String path) {
        Song song=MusicLibrary.querySong(path);
        Metadata md=new Metadata();
        md.sizeLong = song.getSize();
        md.sizeByte = ConvertUtils.byte2FitMemorySize(md.sizeLong);
        md.title = song.getSongTitle();
        md.album = song.getAlbumName();
        md.artist = song.getArtistName();
        md.duration = String.valueOf(song.getSongDuration());
        md.mimetype = MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(path.substring(path.lastIndexOf("."), path.length())));
        md.bitrate = String.valueOf((long) ((double) song.getSize() / ((double) song.getSongDuration() / 8000)));
        Log.e("getWebMetadata","size:"+md.sizeLong);
        Log.e("getWebMetadata","duration:"+song.getSongDuration());
        Log.e("getWebMetadata","bitrate:"+md.bitrate);
        md.isValid = true;
        return md;
    }

    public static Metadata getMetadataFromSong(Song song) {
        Metadata md = new Metadata();
        md.title = song.getSongTitle();
        md.album = song.getAlbumName();
        md.artist = song.getArtistName();
        md.duration = String.valueOf(song.getSongDuration());
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
                Song song = new Song(
                        //歌曲路径
                        cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)).replace("/storage/emulated/0", "/sdcard"),
                        //歌曲名称
                        cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)),
                        //歌手
                        cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)),
                        //专辑名
                        cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)),
                        //歌曲时长
                        String.valueOf(cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION))),
                        //歌曲大小
                        cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)),
                        //歌曲ID
                        cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
                );
//                if (song.size > 1000 * 800) {
//                    // 注释部分是切割标题，分离出歌曲名和歌手 （本地媒体库读取的歌曲信息不规范）
//                    if (song.title.contains(" - ")) {
//                        String[] str = song.title.split(" - ");
//                        song.artist = str[0];
//                        song.title = str[1];
//                    } else if (song.title.contains("-")) {
//                        String[] str = song.title.split("-");
//                        song.artist = str[0];
//                        song.title = str[1];
//                    }
//                    list.add(song);
//                }
                list.add(song);
            }
            // 释放资源
            cursor.close();
        }
        return list;
    }

    /**
     * 判断服务是否在运行
     * @param context
     * @param serviceName
     * @return
     * 服务名称为全路径 例如com.ghost.WidgetUpdateService
     */
    public static boolean isRunService(Context context,String serviceName) {
        ActivityManager manager = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceName.equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}