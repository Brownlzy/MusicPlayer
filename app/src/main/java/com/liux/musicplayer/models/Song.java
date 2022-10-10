package com.liux.musicplayer.models;

import android.content.Context;
import android.media.MediaMetadataRetriever;

import androidx.annotation.NonNull;

import com.blankj.utilcode.util.ConvertUtils;
import com.blankj.utilcode.util.FileUtils;
import com.liux.musicplayer.utils.MusicUtils;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class Song implements Cloneable {

    static final Song EMPTY_SONG = new Song("", -1, -1, -1, null, null, "", -1, "");

    private /*final*/ String mTitle;
    private /*final*/ int mTrackNumber;
    private /*final*/ int mDuration;
    private /*final*/ String mPath;
    private /*final*/ String mLyricPath;
    private /*final*/ String mAlbumName;
    private /*final*/ int mArtistId;
    private /*final*/ String mArtistName;
    private /*final*/ int mYear;
    private String mId;

    public Song() {
        setmTitle("");
        setmArtistName("");
    }

    public Song(String mTitle, int mDuration, String mArtistName, String mId) {
        this.mTitle = mTitle;
        this.mDuration = mDuration;
        this.mArtistName = mArtistName;
        this.mId = mId;
    }

    public Song(String mTitle, int mDuration, String mArtistName, String mId, String mPath, String mLyricPath) {
        this.mTitle = mTitle;
        this.mDuration = mDuration;
        this.mArtistName = mArtistName;
        this.mId = mId;
        this.mPath = mPath;
        this.mLyricPath = mLyricPath;
    }

    public Song(String mSongPath) {
        this.mPath = mSongPath;
        MusicUtils.Metadata metadata = getMetadata(mSongPath);
        this.mTitle = metadata.title;
    }

    public static MusicUtils.Metadata getMetadata(String path) {
        try {
            MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
            mediaMetadataRetriever.setDataSource(path);
            MusicUtils.Metadata md = new MusicUtils.Metadata();
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
            e.printStackTrace();
            //Toast.makeText(context, "专辑图片读取发生未知错误", Toast.LENGTH_SHORT).show();
            //return BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_baseline_music_note_24);
        }
        return new MusicUtils.Metadata();
    }

    public Song(@NonNull final String id, String title, final int trackNumber, final int year, final int duration, final String path, final String albumName, final int artistId, final String artistName) {
        mId = id;
        mTitle = title;
        mTrackNumber = trackNumber;
        mYear = year;
        mDuration = duration;
        mPath = path;
        mAlbumName = albumName;
        mArtistId = artistId;
        mArtistName = artistName;
    }

    public Song(@NonNull final String title, final int trackNumber, final int year, final int duration, final String path, final String lyricPath, final String albumName, final int artistId, final String artistName) {
        mTitle = title;
        mTrackNumber = trackNumber;
        mYear = year;
        mDuration = duration;
        mPath = path;
        mLyricPath = lyricPath;
        mAlbumName = albumName;
        mArtistId = artistId;
        mArtistName = artistName;
    }

    public Song(@NonNull final String title, final int trackNumber, final int year, final int duration, final String path) {
        mTitle = title;
        mTrackNumber = trackNumber;
        mYear = year;
        mDuration = duration;
        mPath = path;
    }

    @NonNull
    public static String formatDuration(final int duration) {
        return String.format(Locale.getDefault(), "%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(duration),
                TimeUnit.MILLISECONDS.toSeconds(duration) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration)));
    }

    public static int formatTrack(final int trackNumber) {
        int formatted = trackNumber;
        if (trackNumber >= 1000) {
            formatted = trackNumber % 1000;
        }
        return formatted;
    }

    @NonNull
    @Override
    public Song clone() {
        Song song = null;
        try {
            song = (Song) super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            song = new Song();
        }
        song.setmTitle(this.mTitle);
        song.setmArtistName(this.mArtistName);

        return song;
    }

    public void setmTitle(String mTitle) {
        this.mTitle = mTitle;
    }

    public void setmAlbumName(String mAlbumName) {
        this.mAlbumName = mAlbumName;
    }

    public void setmArtistName(String mArtistName) {
        this.mArtistName = mArtistName;
    }

    @NonNull
    public final String getSongTitle() {
        return mTitle;
    }

    public final int getTrackNumber() {
        return mTrackNumber;
    }

    public final int getSongDuration() {
        return mDuration;
    }

    @NonNull
    public final String getSongPath() {
        return mPath;
    }

    public final String getLyricPath() {
        return mLyricPath;
    }

    @NonNull
    public final String getAlbumName() {
        return mAlbumName;
    }

    public final int getArtistId() {
        return mArtistId;
    }


    public final String getArtistName() {
        return mArtistName;
    }

    public final int getYear() {
        return mYear;
    }

    public String getmId() {
        return mId;
    }

    public void setmId(String mId) {
        this.mId = mId;
    }

    @Override
    public String toString() {
        return "Song{" +
                "mID='" + mId + '\'' +
                "mTitle='" + mTitle + '\'' +
                ", mTrackNumber=" + mTrackNumber +
                ", mDuration=" + mDuration +
                ", mPath='" + mPath + '\'' +
                ", mAlbumName='" + mAlbumName + '\'' +
                ", mArtistId=" + mArtistId +
                ", mArtistName='" + mArtistName + '\'' +
                ", mYear=" + mYear +
                '}';
    }


    public void setLyricPath(String newLyricPath) {
        if (newLyricPath == null)
            this.mLyricPath = "null";
        else
            this.mLyricPath = newLyricPath;
    }
}

