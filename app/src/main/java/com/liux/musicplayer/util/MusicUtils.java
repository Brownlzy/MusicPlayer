package com.liux.musicplayer.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.widget.Toast;

import com.liux.musicplayer.MusicPlayer;
import com.liux.musicplayer.R;

public class MusicUtils {
    public static class Metadata {
        public String title = "null";
        public String album = "null";
        public String artist = "null";
        public String duration = "null";
        public String mimetype = "null";
        public String bitrate = "null";
    }

    public static Bitmap getAlbumImage(Context context, MusicPlayer.Song song) {
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
            return BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_baseline_music_note_24);
        } catch (NullPointerException e) {
            //文件本身无专辑图片
            return BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_baseline_music_note_24);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "专辑图片读取发生未知错误", Toast.LENGTH_SHORT).show();
            return BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_baseline_music_note_24);
        }
    }

    public static Metadata getMetadata(Context context, MusicPlayer.Song song) {
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
            return md;
            //获取专辑图片
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