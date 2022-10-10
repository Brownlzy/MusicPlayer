package com.liux.musicplayer.media;

import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.KeyEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.media.MediaBrowserServiceCompat;
import androidx.media.session.MediaButtonReceiver;

import com.liux.musicplayer.services.FloatLyricService;
import com.liux.musicplayer.utils.MediaNotificationManager;
import com.liux.musicplayer.utils.SharedPrefs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class SimpleMusicService extends MediaBrowserServiceCompat {

    Handler sleepHandler;
    public MediaSessionCallback mCallback;
    private MediaSessionCompat mSession;
    private MediaNotificationManager mMediaNotificationManager;
    private MediaPlayerAdapter mPlayback;
    private boolean mServiceInStartedState;
    private static final String TAG ="SimpleMusicService";
    private boolean mainActivityState;
    public LyricReceiver lyricReceiver;
    public class LyricReceiver extends BroadcastReceiver {
        public static final String TAG = "MusicReceiver";
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.e(TAG,intent.getAction());
            Intent deskLyricIntent = new Intent(SimpleMusicService.this, FloatLyricService.class);
            switch (intent.getAction()){
                case "ACTION_DESKTOP_OPEN_LYRIC":
                    SharedPrefs.putIsDeskLyric(true);
                    if(mainActivityState) {
                        intent.putExtra("isLock", SharedPrefs.getIsDeskLyricLock());
                        startService(deskLyricIntent);
                    }
                    break;
                case "ACTION_DESKTOP_CLOSE_LYRIC":
                    SharedPrefs.putIsDeskLyric(false);
                    stopService(deskLyricIntent);
                    break;
                case "ACTION_ACTIVITY_FOREGROUND":
                    mainActivityState=true;
                    stopService(deskLyricIntent);
                    break;
                case "ACTION_ACTIVITY_BACKGROUND":
                    mainActivityState=false;
                    if(SharedPrefs.getIsDeskLyric()) {
                        intent.putExtra("isLock", SharedPrefs.getIsDeskLyricLock());
                        startService(deskLyricIntent);
                    }
                    break;
                case "ACTION_DESKTOP_LOCK_LYRIC":
                    SharedPrefs.putIsDeskLyricLock(true);
                        if(SharedPrefs.getIsDeskLyric()&&!mainActivityState){
                            intent.putExtra("isLock",true);
                            startService(deskLyricIntent);
                        }
                    break;
                case "ACTION_DESKTOP_UNLOCK_LYRIC":
                    SharedPrefs.putIsDeskLyricLock(false);
                    if(SharedPrefs.getIsDeskLyric()&&!mainActivityState){
                        intent.putExtra("isLock",false);
                        startService(deskLyricIntent);
                    }
                    break;
            }
        }
    }
    private void registerLyricReceiver() {
        lyricReceiver = new LyricReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("ACTION_DESKTOP_CLOSE_LYRIC");
        intentFilter.addAction("ACTION_DESKTOP_OPEN_LYRIC");
        registerReceiver(lyricReceiver, intentFilter);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sleepHandler = new Handler();
        registerLyricReceiver();
//        countDownTimer = new CountDownTimer(6000,1000) {
//            @Override
//            public void onTick(long millisUntilFinished) {
//                Log.d(Constants.TAG, "onTick: " + millisUntilFinished);
//            }
//
//            @Override
//            public void onFinish() {
//                Toast.makeText(SimpleMusicService.this, "Closing service", Toast.LENGTH_SHORT).show();
//                mPlayback.onStop();
//                onDestroy();
//            }
//        };
//
//        countDownTimer.start();

        mSession = new MediaSessionCompat(getApplicationContext(), SimpleMusicService.class.getSimpleName());
        setSessionToken(mSession.getSessionToken());
        mSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS |
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        mCallback = new MediaSessionCallback();
        mSession.setCallback(mCallback);

        mMediaNotificationManager = new MediaNotificationManager(this);

        mPlayback = new MediaPlayerAdapter(this, new MediaPlayerListener());


    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        MediaButtonReceiver.handleIntent(mSession, intent);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        mMediaNotificationManager.onDestroy();
        mSession.release();
        //关闭桌面歌词
        Intent deskLyricIntent = new Intent(SimpleMusicService.this, FloatLyricService.class);
        stopService(deskLyricIntent);
        unregisterReceiver(lyricReceiver);
        Log.d(TAG, "onDestroy: MediaPlayerAdapter stopped, and MediaSession released");
    }

    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {
        Log.e(TAG, "onGetRoot: "+clientPackageName);
        return new BrowserRoot(SimpleMusicService.class.getSimpleName(), null);
//        return null;
    }

    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
        Log.d(TAG, "SimpleMusicService onLoadChildren called: ");
        Log.d(TAG, "SimpleMusicService onLoadChildren parentId: " + parentId);
        Log.d(TAG, "SimpleMusicService onLoadChildren result: " + result);

        result.sendResult(MusicLibrary.getPlayingList());
    }

    @Override
    public void onCustomAction(@NonNull String action, Bundle extras, @NonNull Result<Bundle> result) {
        super.onCustomAction(action, extras, result);
        Log.e(TAG,action);
    }

    // MediaSession Callback: Transport Controls -> MediaPlayerAdapter
    public class MediaSessionCallback extends MediaSessionCompat.Callback {
        private final List<MediaSessionCompat.QueueItem> mPlaylist = new ArrayList<>();
        private final HashMap<String, MediaSessionCompat.QueueItem> queueItemHashMap = new HashMap<>();
        private final List<MediaSessionCompat.QueueItem> mPlaylistOriginal = new ArrayList<>();
        private int mQueueIndex = -1;
        private MediaMetadataCompat mPreparedMedia;
        private boolean isRandom = false;
        private final boolean isRepeatModeOn = false;

        @Override
        public void onAddQueueItem(MediaDescriptionCompat description) {
            Log.d(TAG, "onAddQueueItem: Called SimpleMusicService");
            Log.d(TAG, String.format("onAddQueueItem: %s. Index: %s", description.getTitle(), description.hashCode()));
            MediaSessionCompat.QueueItem queueItem = new MediaSessionCompat.QueueItem(description, description.hashCode());
            mPlaylist.add(queueItem);
            queueItemHashMap.put(description.getMediaId(), queueItem);
            mQueueIndex = (mQueueIndex == -1) ? 0 : mQueueIndex;
            mSession.setQueue(mPlaylist);
        }

        @Override
        public void onRemoveQueueItem(MediaDescriptionCompat description) {
            Log.d(TAG, "onRemoveQueueItem: Called SimpleMusicService");
            MediaSessionCompat.QueueItem queueItem = new MediaSessionCompat.QueueItem(description, description.hashCode());
            mPlaylist.remove(queueItem);
            queueItemHashMap.remove(description.getMediaId());
//            mPlaylist.remove(new MediaSessionCompat.QueueItem(description, description.hashCode()));
            mQueueIndex = (mPlaylist.isEmpty()) ? -1 : mQueueIndex;
            mSession.setQueue(mPlaylist);
        }


        @Override
        public void onPrepareFromMediaId(String mediaId, Bundle extras) {
            Log.d(TAG, "onPrepare: Called SimpleMusicService");
            if (mQueueIndex < 0 && mPlaylist.isEmpty()) {
                // Nothing to play.
                return;
            }

            mPreparedMedia = MusicLibrary.getMetadata(mediaId);
            mSession.setMetadata(mPreparedMedia);

            if (!mSession.isActive()) {
                mSession.setActive(true);
            }
        }

        @Override
        public boolean onMediaButtonEvent(Intent mediaButtonEvent) {
            KeyEvent keyEvent = mediaButtonEvent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            Log.e(TAG, String.valueOf(mediaButtonEvent));
            Log.e(TAG, String.valueOf(keyEvent.getKeyCode()));
            return super.onMediaButtonEvent(mediaButtonEvent);
        }

        @Override
        public void onPrepare() {
            Log.d(TAG, "onPrepare: Called SimpleMusicService");
            if (mQueueIndex < 0 && mPlaylist.isEmpty()) {
                // Nothing to play.
                return;
            }

            final String mediaId = mPlaylist.get(mQueueIndex).getDescription().getMediaId();
            mPreparedMedia = MusicLibrary.getMetadata(mediaId);
            mSession.setMetadata(mPreparedMedia);

            if (!mSession.isActive()) {
                mSession.setActive(true);
            }
        }

        @Override
        public void onPlay() {
            Log.d(TAG, "onPlay: Called SimpleMusicService");
            if (!isReadyToPlay()) {
                // Nothing to play.
                Log.d(TAG, "not ready to play: ");
                return;
            }

            if (mPreparedMedia == null) {
                onPrepare();
            }

            mPlayback.playFromMedia(mPreparedMedia);
        }

        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
            Log.d(TAG, "onPlayFromMediaId: Called SimpleMusicService" + mediaId);
            if (!isReadyToPlay()) {
                // Nothing to play.
                Log.d(TAG, "not ready to play: ");
                return;
            }

//            if (mPreparedMedia == null) {
            onPrepareFromMediaId(mediaId, null);
//            }

            mQueueIndex = mPlaylist.indexOf(queueItemHashMap.get(mediaId));
            mPlayback.playFromMedia(mPreparedMedia);
        }

        @Override
        public void onPause() {
            Log.d(TAG, "onPause: Called");
            mPlayback.pause();
        }

        @Override
        public void onStop() {
            onPause();
            mPlayback.stop();
            mSession.setActive(false);
        }

        @Override
        public void onSkipToNext() {
            mQueueIndex = (++mQueueIndex % mPlaylist.size());
            Log.d(TAG, "onSkipToNext: QueueIndex: " + mQueueIndex);
            mPreparedMedia = null;
            onPlay();
        }


        @Override
        public void onSetRepeatMode(int repeatMode) {
            mSession.setRepeatMode(repeatMode);
            mPlayback.setRepeating(repeatMode == PlaybackStateCompat.REPEAT_MODE_ONE);
            super.onSetRepeatMode(repeatMode);
        }

        @Override
        public void onSetShuffleMode(int shuffleMode) {
            Log.d(TAG, "onSetShuffleMode: Called inside SimpleMusicService");
            isRandom = shuffleMode == PlaybackStateCompat.SHUFFLE_MODE_ALL;
            if (isRandom) {
                long seed = System.nanoTime();
                mPlaylistOriginal.clear();
                mPlaylistOriginal.addAll(mPlaylist);
                Collections.shuffle(mPlaylist, new Random(seed));
            } else {
                mPlaylist.clear();
                mPlaylist.addAll(mPlaylistOriginal);
            }
            mSession.setShuffleMode(shuffleMode);
            super.onSetShuffleMode(shuffleMode);
        }

        @Override
        public void onCustomAction(String action, Bundle extras) {
            super.onCustomAction(action, extras);
            Log.e(TAG,"session+"+action);
        }

        @Override
        public void onSkipToPrevious() {
            mQueueIndex = mQueueIndex > 0 ? mQueueIndex - 1 : mPlaylist.size() - 1;
            mPreparedMedia = null;
            onPlay();
        }

        @Override
        public void onSeekTo(long pos) {
            mPlayback.seekTo(pos);
        }

        private boolean isReadyToPlay() {
            return (!mPlaylist.isEmpty());
        }

    }

    // MediaPlayerAdapter Callback: MediaPlayerAdapter state -> MusicService.
    public class MediaPlayerListener extends PlaybackInfoListener {

        private final ServiceManager mServiceManager;
        private final Handler handler;

        MediaPlayerListener() {
            mServiceManager = new ServiceManager();
            handler = new Handler();
        }


        @Override
        public void onPlaybackCompleted() {
        }

        @Override
        public void onPlaybackStateChange(PlaybackStateCompat state) {
            // Report the state to the MediaSession.
//            Log.d(Constants.TAG, "onPlaybackStateChange: Called");
            // Manage the started state of this service.
            switch (state.getState()) {
                case PlaybackStateCompat.STATE_PLAYING:
                    mServiceManager.moveServiceToStartedState(state);
                    break;
                case PlaybackStateCompat.STATE_PAUSED:
                    mServiceManager.updateNotificationForPause(state);
                    break;
                case PlaybackStateCompat.STATE_STOPPED:
                    mServiceManager.moveServiceOutOfStartedState(state);
                    break;
            }
            mSession.setPlaybackState(state);
        }

        class ServiceManager {

            private void moveServiceToStartedState(PlaybackStateCompat state) {
                Notification notification =
                        mMediaNotificationManager.getNotification(
                                mPlayback.getCurrentMedia(), state, getSessionToken());

//                Log.d(Constants.TAG, "moveServiceToStartedState: called");
                if (!mServiceInStartedState) {
                    ContextCompat.startForegroundService(
                            SimpleMusicService.this,
                            new Intent(SimpleMusicService.this, SimpleMusicService.class));
                    mServiceInStartedState = true;
                }

                startForeground(MediaNotificationManager.NOTIFICATION_ID, notification);
            }

            private void updateNotificationForPause(PlaybackStateCompat state) {
                stopForeground(false);
                Notification notification =
                        mMediaNotificationManager.getNotification(
                                mPlayback.getCurrentMedia(), state, getSessionToken());
                mMediaNotificationManager.getNotificationManager()
                        .notify(MediaNotificationManager.NOTIFICATION_ID, notification);
                stopForeground(false);
            }

            private void moveServiceOutOfStartedState(PlaybackStateCompat state) {
                stopForeground(true);
                mServiceInStartedState = false;
                stopSelf();
            }
        }

    }
}
