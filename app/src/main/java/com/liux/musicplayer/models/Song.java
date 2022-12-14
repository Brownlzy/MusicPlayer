package com.liux.musicplayer.models;

import androidx.annotation.NonNull;

import com.blankj.utilcode.util.FileUtils;
import com.liux.musicplayer.utils.MusicUtils;
import com.liux.musicplayer.utils.SHA256Util;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class Song implements Cloneable {

    static final Song EMPTY_SONG = new Song("", -1, -1, -1, null, null, "null", -1, "null");

    private String mTitle;
    private int mTrackNumber;
    private int mDuration;
    private String mPath;
    private String mLyricPath;
    private String mAlbumName;
    private int mArtistId;
    private String mArtistName;
    private int mYear;
    private long mSize;
    private String mId;

    public Song() {
        setmTitle("null");
        setmArtistName("null");
    }

//    public Song(String mTitle, int mDuration, String mArtistName, String mId) {
//        this.mTitle = mTitle;
//        this.mDuration = mDuration;
//        this.mArtistName = mArtistName;
//        this.mId = mId;
//    }

    public Song(String mTitle, int mDuration, String mArtistName, String mAlbumName, String mId, String mPath, String mLyricPath) {
        this.mTitle = mTitle;
        this.mDuration = mDuration;
        this.mArtistName = mArtistName;
        this.mAlbumName = mAlbumName;
        this.mId = mId;
        this.mPath = mPath;
        this.mLyricPath = mLyricPath;
    }

    public Song(String mTitle, int mDuration, String mArtistName, String mAlbumName, String mId, Long mSize, String mPath, String mLyricPath) {
        this.mTitle = mTitle;
        this.mDuration = mDuration;
        this.mArtistName = mArtistName;
        this.mAlbumName = mAlbumName;
        this.mSize = mSize;
        this.mId = SHA256Util.md5(mPath);
        this.mPath = mPath;
        this.mLyricPath = mLyricPath;
    }

    public Song(String mSongPath) {
        this.mPath = mSongPath;
        this.mId = SHA256Util.md5(mSongPath);
        MusicUtils.Metadata metadata = getMetadata(mSongPath);
        initSong(mSongPath, metadata.title, metadata.artist, metadata.album, metadata.duration, metadata.sizeLong, mId);
    }

    public Song(String path, String title, String artist, String album, String duration, Long sizeLong, String id) {
        initSong(path, title, artist, album, duration, sizeLong, id);
    }

    public Song(String id, String path, String title, String artist, String album, String lyricPath) {
        MusicUtils.Metadata metadata = getMetadata(path);
        this.mPath = path;
        this.mId = id;
        this.mTitle = title;
        this.mArtistName = artist;
        this.mAlbumName = album;
        this.mLyricPath = lyricPath;
        this.mDuration = Integer.parseInt(metadata.duration);
        this.mSize = metadata.sizeLong;
    }

    public Song(@NonNull final String title, final int trackNumber, final int year, final int duration, final String path, final String lyricPath, final String albumName, final int artistId, final String artistName) {
        mTitle = title;
        mTrackNumber = trackNumber;
        mYear = year;
        mDuration = duration;
        mPath = path;
        mId = SHA256Util.md5(path);
        mLyricPath = lyricPath;
        mAlbumName = albumName;
        mArtistId = artistId;
        mArtistName = artistName;
    }

    public static MusicUtils.Metadata getMetadata(String path) {
        return MusicUtils.getMetadata(path);
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

    public Song(@NonNull final String title, final int trackNumber, final int year, final int duration, final String path) {
        mTitle = title;
        mTrackNumber = trackNumber;
        mYear = year;
        mDuration = duration;
        mPath = path;
        mId = SHA256Util.md5(path);
    }

    public void initSong(String path, String title, String artist, String album, String duration, Long sizeLong, String id) {
        this.mPath = path;
        if (id == null)
            this.mId = SHA256Util.md5(path);
        else
            this.mId = id;
        this.mTitle = title;
        this.mArtistName = artist;
        this.mAlbumName = album;
        this.mDuration = Integer.parseInt(duration);
        this.mSize = sizeLong;
        if (FileUtils.getFileNameNoExtension(path).matches(".* - .*")) {
            if (mTitle == null || mTitle.equals("null"))
                mTitle = FileUtils.getFileNameNoExtension(path).split(" - ")[1];
            if (mArtistName == null)
                mArtistName = FileUtils.getFileNameNoExtension(path).split(" - ")[0];
        } else if (FileUtils.getFileNameNoExtension(path).matches(".*-.*")) {
            if (mTitle == null || mTitle.equals("null"))
                mTitle = FileUtils.getFileNameNoExtension(path).split("-")[1];
            if (mArtistName == null)
                mArtistName = FileUtils.getFileNameNoExtension(path).split("-")[0];
        } else {
            if (mTitle == null || mTitle.equals("null"))
                mTitle = FileUtils.getFileNameNoExtension(path);
            if (mArtistName == null) mArtistName = "null";
        }
        if (mAlbumName == null) mAlbumName = "null";
        //????????????????????????
        if (FileUtils.isFileExists(path.replace(FileUtils.getFileExtension(path), "lrc")))
            mLyricPath = path.replace(FileUtils.getFileExtension(path), "lrc");
        else
            mLyricPath = "null";
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
        if(mAlbumName!=null)
        this.mAlbumName = mAlbumName;
    }

    public void setmArtistName(String mArtistName) {
        if(mArtistName!=null)
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
        if(!FileUtils.isFileExists(mLyricPath)) {
            if (FileUtils.isFileExists(mPath.replace(FileUtils.getFileExtension(mPath), "lrc")))
                mLyricPath = mPath.replace(FileUtils.getFileExtension(mPath), "lrc");
        }
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
        if (mId == null)
            return SHA256Util.md5(mPath);
        else
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

    public void setPath(String path) {
        this.mPath=path;
    }

    public void setDuration(long duration) {
    }

    public void setSize(Long sizeLong) {
        this.mSize=sizeLong;
    }

    public Long getSize() {
        return mSize;
    }
}

