package com.liux.musicplayer.viewmodels;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.liux.musicplayer.activities.MainActivity;
import com.liux.musicplayer.media.MediaBrowserHelper;
import com.liux.musicplayer.media.MusicLibrary;
import com.liux.musicplayer.media.SimpleMusicService;
import com.liux.musicplayer.media.SongProvider;
import com.liux.musicplayer.models.Song;
import com.liux.musicplayer.services.MusicService;
import com.liux.musicplayer.utils.SharedPrefs;

import java.util.List;

public class MyViewModel extends AndroidViewModel {
    private static final String TAG = "MyViewModel";
    private final MutableLiveData<Boolean> mIsPlaying = new MutableLiveData<Boolean>(false);
    private final MutableLiveData<Long> currentPlayingDuration = new MutableLiveData<Long>();
    private final MutableLiveData<Integer> shuffleMode = new MutableLiveData<Integer>();
    private final MutableLiveData<Integer> repeatMode = new MutableLiveData<Integer>();
    private static boolean activityForeground=false;
    private static boolean isDeskTopLyric=false;
    private static boolean isDeskTopLyricLocked=false;
    public static void setActivityForeground(boolean activityForeground) {
        MyViewModel.activityForeground = activityForeground;
        Intent intent=new Intent(MainActivity.mainActivity,SimpleMusicService.class);
        if(activityForeground){
            //intent.setAction();

        }
    }

    public MutableLiveData<List<Song>> getSongsMutableLiveData() {
        return songsMutableLiveData;
    }

    private final MutableLiveData<List<Song>> songsMutableLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<Song>> playingSongsMutableLiveData = new MutableLiveData<>();
    MutableLiveData<List<MediaBrowserCompat.MediaItem>> mediaItemsMutableLiveData = new MutableLiveData<>();
    MutableLiveData<List<MediaBrowserCompat.MediaItem>> searchResultsLiveData = new MutableLiveData<>();
    MutableLiveData<Song> nowPlaying = new MutableLiveData<Song>();
    MutableLiveData<PlaybackStateCompat> playingPlaybackState = new MutableLiveData<PlaybackStateCompat>();
    private MediaBrowserHelper mMediaBrowserHelper;

    public MusicService getMusicService() {
        return musicService;
    }

    private MusicService musicService;

    public MyViewModel(@NonNull Application application) {
        super(application);
        refreshSongsList();
        connectToMediaPlaybackService();
    }

    public void setMusicService(MusicService musicService) {
        this.musicService = musicService;
    }
    public MutableLiveData<List<MediaBrowserCompat.MediaItem>> getSearchResultsLiveData() {
        return searchResultsLiveData;
    }

    public LiveData<Long> getCurrentPlayingDuration() {
        return currentPlayingDuration;
    }

//    public LiveData<PlaybackStateCompat> getPlayingPlaybackState() {
//        return playingPlaybackState;
//    }

    public MediaControllerCompat getmMediaController() {
        return mMediaBrowserHelper.getmMediaController();
    }

    public LiveData<Integer> getShuffleMode() {
        return shuffleMode;
    }

    public LiveData<Integer> getRepeatMode() {
        return repeatMode;
    }

    private void connectToMediaPlaybackService() {
        if (mMediaBrowserHelper == null) {
            mMediaBrowserHelper = new MediaBrowserConnection(getApplication().getApplicationContext());

            mMediaBrowserHelper.registerCallback(new MediaBrowserListener());
            mMediaBrowserHelper.onStart();
        }
    }

    public void refreshSongsList() {
        SharedPrefs.init(getApplication());
        //从媒体库中获取歌单
        final List<Song> songs = SongProvider.getSongs(SongProvider.makeSongCursor(
                getApplication(), SongProvider.getSongLoaderSortOrder())
        );
        SharedPrefs.saveSongList(songs);
        songsMutableLiveData.setValue(SharedPrefs.getSongListFromSharedPrefer("[{}]"));
        playingSongsMutableLiveData.setValue(SharedPrefs.getPlayingListFromSharedPrefer("[{}]"));
        //SharedPrefs.saveSongList(songs);
        //final List<Song> songs=SongProvider.getSongsFromSharedPrefer();
//        songsMutableLiveData.setValue(songs);
        MusicLibrary.buildMediaItems(songsMutableLiveData.getValue());
    }

    public void forceConnect() {
        mMediaBrowserHelper.forceConnect();
    }


    public LiveData<List<MediaBrowserCompat.MediaItem>> getMediaItemsMutableLiveData() {
        return mediaItemsMutableLiveData;
    }

    public LiveData<Song> getNowPlaying() {
        return nowPlaying;
    }


    public void nextSong() {
        mMediaBrowserHelper.getTransportControls().skipToNext();
    }

    public void prevSong() {
        mMediaBrowserHelper.getTransportControls().skipToPrevious();

    }

    public void playFromMediaId(String mediaId) {
        mMediaBrowserHelper.getTransportControls().playFromMediaId(mediaId, null);
    }


    public void PlayPauseResume() {
        if (mIsPlaying.getValue()) {
            mMediaBrowserHelper.getTransportControls().pause();
        } else {
            mMediaBrowserHelper.getTransportControls().play();
        }
    }


    public void shuffle() {
        if (shuffleMode.getValue() != null) {
            if (shuffleMode.getValue() == PlaybackStateCompat.SHUFFLE_MODE_ALL) {
//                mMediaBrowserHelper.shuffleCallback(PlaybackStateCompat.SHUFFLE_MODE_NONE);
                mMediaBrowserHelper.getTransportControls().setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_NONE);
            } else {
//                mMediaBrowserHelper.shuffleCallback(PlaybackStateCompat.SHUFFLE_MODE_ALL);
                mMediaBrowserHelper.getTransportControls().setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_ALL);
            }
        }
    }

    public void toggleRepeatMode() {
        if (repeatMode.getValue() != null) {
            if (repeatMode.getValue() == PlaybackStateCompat.REPEAT_MODE_ONE) {
//                mMediaBrowserHelper.repeatModeCallback(PlaybackStateCompat.REPEAT_MODE_NONE);
                mMediaBrowserHelper.getTransportControls().setRepeatMode(PlaybackStateCompat.REPEAT_MODE_NONE);
            } else {
//                mMediaBrowserHelper.repeatModeCallback(PlaybackStateCompat.REPEAT_MODE_ONE);
                mMediaBrowserHelper.getTransportControls().setRepeatMode(PlaybackStateCompat.REPEAT_MODE_ONE);
            }
        }
    }

    public LiveData<Boolean> getIsPlaying() {
        return mIsPlaying;
    }

    public void seekTo(int progress) {
        mMediaBrowserHelper.getTransportControls().seekTo(progress);
    }

    public void startSleepTimer() {

    }

    public void filterData(String query) {

    }

    public MutableLiveData<List<Song>> getPlayingSongsMutableLiveData() {
        return playingSongsMutableLiveData;
    }

    /**
     * and implement our app specific desires.
     */
    private class MediaBrowserConnection extends MediaBrowserHelper {
        private MediaBrowserConnection(Context context) {
            super(context, SimpleMusicService.class);
        }


        @Override
        protected void onConnected(@NonNull MediaControllerCompat mediaController) {
            Log.e(TAG, "onConnected: Called");
        }

        @Override
        protected void onChildrenLoaded(@NonNull String parentId,
                                        @NonNull List<MediaBrowserCompat.MediaItem> children) {
            super.onChildrenLoaded(parentId, children);

            Log.d(TAG, "onChildrenLoaded:  in songsViewModel: MediaItems size" + children.size());

            final MediaControllerCompat mediaController = getMediaController();
            mediaItemsMutableLiveData.setValue(children);
            searchResultsLiveData.setValue(children);
            // Queue up all media items for this simple sample.
            for (final MediaBrowserCompat.MediaItem mediaItem : children) {
                mediaController.addQueueItem(mediaItem.getDescription());
            }

            // Call prepare now so pressing play just works.
            mediaController.getTransportControls().prepare();
        }
    }

    /**
     * Implementation of the {@link MediaControllerCompat.Callback} methods we're interested in.
     * <p>
     * Here would also be where one could override
     * {@code onQueueChanged(List<MediaSessionCompat.QueueItem> queue)} to get informed when items
     * are added or removed from the queue. We don't do this here in order to keep the UI
     * simple.
     */
    private class MediaBrowserListener extends MediaControllerCompat.Callback {

        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat playbackState) {
            super.onPlaybackStateChanged(playbackState);
//            Log.d(Constants.TAG, "onPlaybackStateChanged: Called inside songsViewModel");
            playingPlaybackState.setValue(playbackState);

            if (playbackState != null &&
                    playbackState.getState() == PlaybackStateCompat.STATE_PLAYING) {
                mIsPlaying.setValue(true);
                currentPlayingDuration.setValue(playbackState.getPosition());
//                Log.d(Constants.TAG, "onPlaybackStateChanged: inside songsViewModel position: " + playbackState.getPosition());
            } else {
                mIsPlaying.setValue(false);
            }
//            mMediaControlsImage.setPressed(mIsPlaying);
        }

        @Override
        public void onRepeatModeChanged(int repeatMode) {
            Log.d(TAG, "onRepeatModeChanged: Called inside songs viewMoodel");
            MyViewModel.this.repeatMode.setValue(repeatMode);
            super.onRepeatModeChanged(repeatMode);
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat mediaMetadata) {
            if (mediaMetadata == null) {
                return;
            }
            Log.d(TAG, "onMetadataChanged: called inside songsViewModel");
            Log.d(TAG, "onMetadataChanged: Title " + mediaMetadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE));

//            for (String s : mediaMetadata.getBundle().keySet()) {
//                Log.d(Constants.TAG, String.format("%s: %s", s, mediaMetadata.getString(s)));
//            }

            String TITLE = mediaMetadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE);
            String ARTIST = mediaMetadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST);
            String MEDIA_ID = mediaMetadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID);
            int DURATION = (int) mediaMetadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION);
            Song song = new Song(TITLE, DURATION, ARTIST, MEDIA_ID);
            nowPlaying.setValue(song);


        }

        @Override
        public void onSessionDestroyed() {
            super.onSessionDestroyed();
        }

        @Override
        public void onShuffleModeChanged(int shuffleMode) {
            Log.d(TAG, "onShuffleModeChanged: inside songsViewModel");
            switch (shuffleMode) {

                case PlaybackStateCompat.SHUFFLE_MODE_ALL:
                    Log.d(TAG, "onShuffleModeChanged: SHUFFLE_MODE_ALL");
                    break;
                case PlaybackStateCompat.SHUFFLE_MODE_GROUP:
                    Log.d(TAG, "onShuffleModeChanged: SHUFFLE_MODE_GROUP");
                    break;
                case PlaybackStateCompat.SHUFFLE_MODE_INVALID:
                    Log.d(TAG, "onShuffleModeChanged: SHUFFLE_MODE_INVALID");
                    break;
                case PlaybackStateCompat.SHUFFLE_MODE_NONE:
                    Log.d(TAG, "onShuffleModeChanged: SHUFFLE_MODE_NONE");
                    break;
            }
            MyViewModel.this.shuffleMode.setValue(shuffleMode);
            super.onShuffleModeChanged(shuffleMode);
        }

        @Override
        public void onQueueChanged(List<MediaSessionCompat.QueueItem> queue) {
            Log.d(TAG, "onQueueChanged: Called inside SongsViewModel");
            super.onQueueChanged(queue);
        }
    }
}