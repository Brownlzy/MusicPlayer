package com.liux.musicplayer.media;

import static com.liux.musicplayer.services.MusicService.LIST_PLAY;
import static com.liux.musicplayer.services.MusicService.REPEAT_LIST;
import static com.liux.musicplayer.services.MusicService.REPEAT_ONE;
import static com.liux.musicplayer.services.MusicService.SHUFFLE_PLAY;

import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
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
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;
import androidx.media.MediaBrowserServiceCompat;
import androidx.media.session.MediaButtonReceiver;

import com.liux.musicplayer.services.FloatLyricService;
import com.liux.musicplayer.utils.LyricUtils;
import com.liux.musicplayer.utils.MediaNotificationManager;
import com.liux.musicplayer.utils.SharedPrefs;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class SimpleMusicService extends MediaBrowserServiceCompat implements LifecycleOwner {

    Handler sleepHandler;
    public MediaSessionCallback mCallback;
    private MediaSessionCompat mSession;
    private MediaNotificationManager mMediaNotificationManager;
    private MediaPlayerAdapter mPlayback;
    private boolean mServiceInStartedState;
    private List<MediaBrowserCompat.MediaItem> mPlaylist = new ArrayList<>();
    private static final String TAG ="SimpleMusicService";
    private boolean mainActivityState=true;
    public LyricReceiver lyricReceiver;
    private LifecycleRegistry mLifecycleRegistry = new LifecycleRegistry(this);
    private LyricUtils lyric=new LyricUtils();

    @NonNull
    @Override
    public Lifecycle getLifecycle() {
        return mLifecycleRegistry;
    }

    public class LyricReceiver extends BroadcastReceiver {
        public static final String TAG = "MusicReceiver";
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.e(TAG,"LyricReceiver:"+intent.getAction());
            Intent deskLyricIntent = new Intent(SimpleMusicService.this, FloatLyricService.class);
            switch (intent.getAction()){
                case "com.liux.musicplayer.OPEN_LYRIC":
                    SharedPrefs.putIsDeskLyric(true);
                    if(!mainActivityState) {
                        intent.putExtra("isLock", SharedPrefs.getIsDeskLyricLock());
                        startService(deskLyricIntent);
                    }
                    break;
                case "com.liux.musicplayer.CLOSE_LYRIC":
                    SharedPrefs.putIsDeskLyric(false);
                    stopService(deskLyricIntent);
                    break;
                case "com.liux.musicplayer.FOREGROUND":
                    mainActivityState=true;
                    stopService(deskLyricIntent);
                    break;
                case "com.liux.musicplayer.BACKGROUND":
                    mainActivityState=false;
                    if(SharedPrefs.getIsDeskLyric()) {
                        intent.putExtra("isLock", SharedPrefs.getIsDeskLyricLock());
                        startService(deskLyricIntent);
                    }
                    break;
                case "com.liux.musicplayer.LOCK_LYRIC":
                    SharedPrefs.putIsDeskLyricLock(true);
                        /*if(SharedPrefs.getIsDeskLyric()&&!mainActivityState){
                            intent.putExtra("isLock",true);
                            startService(deskLyricIntent);
                        }*/
                    break;
                case "com.liux.musicplayer.UNLOCK_LYRIC":
                    SharedPrefs.putIsDeskLyricLock(false);
                    /*if(SharedPrefs.getIsDeskLyric()&&!mainActivityState){
                        intent.putExtra("isLock",false);
                        startService(deskLyricIntent);
                    }*/
                    break;
            }
            if(mSession.getController().getPlaybackState()!=null)
                new MediaPlayerListener().mServiceManager.updateNotificationForLyric(
                        mSession.getController().getPlaybackState()
                );
        }
    }
    private void registerLyricReceiver() {
        lyricReceiver = new LyricReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.liux.musicplayer.CLOSE_LYRIC");
        intentFilter.addAction("com.liux.musicplayer.OPEN_LYRIC");
        intentFilter.addAction("com.liux.musicplayer.LOCK_LYRIC");
        intentFilter.addAction("com.liux.musicplayer.UNLOCK_LYRIC");
        intentFilter.addAction("com.liux.musicplayer.FOREGROUND");
        intentFilter.addAction("com.liux.musicplayer.BACKGROUND");
        registerReceiver(lyricReceiver, intentFilter);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        registerLyricReceiver();
        mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);
        sleepHandler = new Handler();

        mSession = new MediaSessionCompat(getApplicationContext(), SimpleMusicService.class.getSimpleName());
        setSessionToken(mSession.getSessionToken());
        mSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS |
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        mCallback = new MediaSessionCallback();
        mSession.setCallback(mCallback);

        mMediaNotificationManager = new MediaNotificationManager(this);

        mPlayback = new MediaPlayerAdapter(this, new MediaPlayerListener());
        mPlaylist=MusicLibrary.getPlayingList();
        mCallback.onCustomAction("REFRESH_PLAYLIST",null);
    }

    @Override
    public IBinder onBind(Intent intent) {
        mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);
        return super.onBind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP);
        return super.onUnbind(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //TODO 继承MediaButton
        MediaButtonReceiver.handleIntent(mSession, intent);
        mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        mMediaNotificationManager.onDestroy();
        mSession.release();
        //关闭桌面歌词
        mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY);
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

        result.sendResult(mPlaylist);
    }

    @Override
    public void onCustomAction(@NonNull String action, Bundle extras, @NonNull Result<Bundle> result) {
        super.onCustomAction(action, extras, result);
        Log.e(TAG,action);
        switch (action){
            case "GET_LYRIC":
                Bundle bundle=new Bundle();
                bundle.putSerializable("lyric",lyric);
                break;
        }
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
        private int mRepeatMode;
        private int mShuffleMode;

        @Override
        public void onAddQueueItem(MediaDescriptionCompat description) {
            Log.d(TAG, "onAddQueueItem: Called SimpleMusicService");
            Log.d(TAG, String.format("onAddQueueItem: %s. Index: %s", description.getTitle(), description.hashCode()));
            MediaSessionCompat.QueueItem queueItem = new MediaSessionCompat.QueueItem(description, description.hashCode());
            mPlaylist.add(queueItem);
            queueItemHashMap.put(description.getMediaUri().getPath(), queueItem);
            mQueueIndex = (mQueueIndex == -1) ? 0 : mQueueIndex;
            mSession.setQueue(mPlaylist);
        }

        @Override
        public void onAddQueueItem(MediaDescriptionCompat description, int index) {
            super.onAddQueueItem(description, index);
        }

        @Override
        public void onRemoveQueueItem(MediaDescriptionCompat description) {
            Log.d(TAG, "onRemoveQueueItem: Called SimpleMusicService");
            Log.d(TAG, String.format("onRemoveQueueItem: %s. Index: %s", description.getTitle(), description.hashCode()));
            MediaSessionCompat.QueueItem queueItem = new MediaSessionCompat.QueueItem(description, description.hashCode());
            mPlaylist.remove(queueItem);
            queueItemHashMap.remove(description.getMediaUri().getPath());
//            mPlaylist.remove(new MediaSessionCompat.QueueItem(description, description.hashCode()));
            mQueueIndex = (mPlaylist.isEmpty()) ? -1 : mQueueIndex;
            mSession.setQueue(mPlaylist);
        }


        /*@Override
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
        }*/

        @Override
        public void onPrepareFromSearch(String query, Bundle extras) {
            super.onPrepareFromSearch(query, extras);
        }

        @Override
        public void onPrepareFromUri(Uri uri, Bundle extras) {
            super.onPrepareFromUri(uri, extras);
            Log.d(TAG, "onPrepare: Called SimpleMusicService");
            if (mQueueIndex < 0 && mPlaylist.isEmpty()) {
                // Nothing to play.
                return;
            }

            mPreparedMedia = MusicLibrary.getMetadata(uri);
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

            final Uri mediaUri = mPlaylist.get(mQueueIndex).getDescription().getMediaUri();
            mPreparedMedia = MusicLibrary.getMetadata(mediaUri);
            lyric.LoadLyric(mPlaylist.get(mQueueIndex).getDescription().getExtras().getString("LYRIC_URI","null"));
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
                Log.e(TAG, String.valueOf(mPreparedMedia));
            }
            mPlayback.playFromMedia(mPreparedMedia);
            if(mShuffleMode==PlaybackStateCompat.SHUFFLE_MODE_ALL){
                SharedPrefs.saveNowPlayId(mPlaylistOriginal.indexOf(mPlaylist.get(mQueueIndex)));
            }else {
                SharedPrefs.saveNowPlayId(mQueueIndex);
            }
        }

        /*@Override
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
        }*/

        @Override
        public void onPlayFromSearch(String query, Bundle extras) {
            super.onPlayFromSearch(query, extras);
        }

        @Override
        public void onPlayFromUri(Uri uri, Bundle extras) {
            super.onPlayFromUri(uri, extras);
            Log.d(TAG, "onPlayFromMediaId: Called SimpleMusicService" + uri);
            if (!isReadyToPlay()) {
                // Nothing to play.
                Log.d(TAG, "not ready to play: ");
                return;
            }

//            if (mPreparedMedia == null) {
            onPrepareFromUri(uri, null);
//            }

            mQueueIndex = mPlaylist.indexOf(queueItemHashMap.get(uri.getPath()));
            mPlayback.playFromMedia(mPreparedMedia);

        }

        @Override
        public void onSkipToQueueItem(long id) {
            mQueueIndex = mPlaylist.stream().map(MediaSessionCompat.QueueItem::getQueueId).distinct().collect(Collectors.toList()).indexOf(id);
            Log.d(TAG, "onSkipToNext: QueueIndex: " + mQueueIndex);
            mPreparedMedia = null;
            onPlay();
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
            Intent deskLyricIntent = new Intent(SimpleMusicService.this, FloatLyricService.class);
            stopService(deskLyricIntent);
        }

        @Override
        public void onSkipToNext() {
            mQueueIndex = (++mQueueIndex % mPlaylist.size());
            Log.d(TAG, "onSkipToNext: QueueIndex: " + mQueueIndex);
            mPreparedMedia = null;
            onPlay();
        }

        public void onSkipToThis() {
            Log.d(TAG, "onSkipToNext: QueueIndex: " + mQueueIndex);
            mPreparedMedia = null;
            onPlay();
        }


        @Override
        public void onSetRepeatMode(int repeatMode) {
            mRepeatMode=repeatMode;
            mSession.setRepeatMode(repeatMode);
            mPlayback.setRepeating(repeatMode == PlaybackStateCompat.REPEAT_MODE_ONE);
            super.onSetRepeatMode(repeatMode);
        }

        @Override
        public void onSetShuffleMode(int shuffleMode) {
            Log.d(TAG, "onSetShuffleMode: Called inside SimpleMusicService");
            mShuffleMode=shuffleMode;
            isRandom = shuffleMode == PlaybackStateCompat.SHUFFLE_MODE_ALL;
            if (isRandom) {
                long seed = System.nanoTime();
                MediaSessionCompat.QueueItem nowItem=mPlaylist.get(mQueueIndex);
                mPlaylistOriginal.clear();
                mPlaylistOriginal.addAll(mPlaylist);
                Collections.shuffle(mPlaylist, new Random(seed));
                mQueueIndex=mPlaylist.indexOf(nowItem);
            } else {
                if(mPlaylistOriginal.size()!=0) {
                    MediaSessionCompat.QueueItem nowItem=mPlaylist.get(mQueueIndex);
                    mPlaylist.clear();
                    mPlaylist.addAll(mPlaylistOriginal);
                    mQueueIndex=mPlaylist.indexOf(nowItem);
                }
            }
            mSession.setShuffleMode(shuffleMode);
            super.onSetShuffleMode(shuffleMode);
        }

        @Override
        public void onCustomAction(String action, Bundle extras) {
            Log.e(TAG,"session+"+action);
            switch (action){
                case "REFRESH_PLAYLIST":
                    Log.e(TAG,"Reconized+"+action);
                    List<MediaBrowserCompat.MediaItem> newPlayList=MusicLibrary.getPlayingList();
                    mQueueIndex=SharedPrefs.getNowPlayId();
                    switch(SharedPrefs.getPlayOrder()){
                        case LIST_PLAY:
                            mRepeatMode=PlaybackStateCompat.REPEAT_MODE_NONE;
                            mShuffleMode=PlaybackStateCompat.SHUFFLE_MODE_NONE;
                            break;
                        case REPEAT_LIST:
                            mRepeatMode=PlaybackStateCompat.REPEAT_MODE_ALL;
                            mShuffleMode=PlaybackStateCompat.SHUFFLE_MODE_NONE;
                            break;
                        case REPEAT_ONE:
                            mRepeatMode=PlaybackStateCompat.REPEAT_MODE_ONE;
                            mShuffleMode=PlaybackStateCompat.SHUFFLE_MODE_NONE;
                            break;
                        case SHUFFLE_PLAY:
                            mRepeatMode=PlaybackStateCompat.REPEAT_MODE_ALL;
                            mShuffleMode=PlaybackStateCompat.SHUFFLE_MODE_ALL;
                            break;
                    }
                    mPlaylist.clear();
                    queueItemHashMap.clear();
                    for(MediaBrowserCompat.MediaItem mediaItem:newPlayList) {
                        MediaSessionCompat.QueueItem queueItem = new MediaSessionCompat.QueueItem(mediaItem.getDescription(), mediaItem.getDescription().hashCode());
                        mPlaylist.add(queueItem);
                        queueItemHashMap.put(mediaItem.getDescription().getMediaUri().getPath(), queueItem);
                    }
                    mSession.setQueue(mPlaylist);
                    onSetRepeatMode(mRepeatMode);
                    onSetShuffleMode(mShuffleMode);
            }
            super.onCustomAction(action, extras);
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
            private void updateNotificationForLyric(PlaybackStateCompat state) {
                if(state.getState()==PlaybackStateCompat.STATE_PLAYING) {
                    moveServiceToStartedState(state);
                }else if(state.getState()==PlaybackStateCompat.STATE_PAUSED){
                    updateNotificationForPause(state);
                }else {
                    moveServiceOutOfStartedState(state);
                }
            }

            private void moveServiceOutOfStartedState(PlaybackStateCompat state) {
                stopForeground(true);
                mServiceInStartedState = false;
                stopSelf();
            }
        }

    }
}
