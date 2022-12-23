package com.liux.musicplayer.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.RatingCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.KeyEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.media.MediaBrowserServiceCompat;
import androidx.media.session.MediaButtonReceiver;

import com.danikula.videocache.CacheListener;
import com.danikula.videocache.HttpProxyCacheServer;
import com.liux.musicplayer.R;
import com.liux.musicplayer.media.MediaPlayerAdapter;
import com.liux.musicplayer.media.MusicLibrary;
import com.liux.musicplayer.media.PlaybackInfoListener;
import com.liux.musicplayer.utils.LyricUtils;
import com.liux.musicplayer.utils.MediaNotificationManager;
import com.liux.musicplayer.utils.PermissionUtils;
import com.liux.musicplayer.utils.SharedPrefs;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class MusicService extends MediaBrowserServiceCompat {
    public static final int LIST_PLAY = 0;
    public static final int REPEAT_LIST = 1;
    public static final int REPEAT_ONE = 2;
    public static final int SHUFFLE_PLAY = 3;

    Handler sleepHandler;
    public MediaSessionCallback mCallback;
    private MediaSessionCompat mSession;
    private MediaNotificationManager mMediaNotificationManager;
    private MediaPlayerAdapter mPlayback;
    private boolean mServiceInStartedState;
    private List<MediaBrowserCompat.MediaItem> mPlaylist = new ArrayList<>();
    private List<MediaSessionCompat.QueueItem> queueItems=new ArrayList<>();
    private static final String TAG ="SimpleMusicService";
    private boolean mainActivityState=true;
    public LyricReceiver lyricReceiver;
    private LyricUtils lyric=new LyricUtils();
    private boolean hasPlayedOnce=false;
    private HttpProxyCacheServer proxy;

    public HttpProxyCacheServer getProxy() {
        if(proxy==null) {
            proxy = new HttpProxyCacheServer(this);
        }
        return proxy;
    }

    public CacheListener cacheListener = new CacheListener() {
        @Override
        public void onCacheAvailable(File cacheFile, String url, int percentsAvailable) {
            Log.e(TAG, percentsAvailable + "%,url=" + url);
        }
    };

    private void registerLyricReceiver() {
        lyricReceiver = new LyricReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(getPackageName() + ".CLOSE_LYRIC");
        intentFilter.addAction(getPackageName() + ".OPEN_LYRIC");
        intentFilter.addAction(getPackageName() + ".LOCK_LYRIC");
        intentFilter.addAction(getPackageName() + ".UNLOCK_LYRIC");
        intentFilter.addAction(getPackageName() + ".FOREGROUND");
        intentFilter.addAction(getPackageName() + ".BACKGROUND");
        intentFilter.addAction(getPackageName() + ".WEB_ON");
        intentFilter.addAction(getPackageName() + ".WEB_OFF");
        intentFilter.addAction(Intent.ACTION_SCREEN_ON);
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        intentFilter.addAction(Intent.ACTION_USER_PRESENT);
        registerReceiver(lyricReceiver, intentFilter);
    }

    public class LyricReceiver extends BroadcastReceiver {
        public static final String TAG = "MusicReceiver";

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.e(TAG, "LyricReceiver:" + intent.getAction());
            Intent deskLyricIntent = new Intent(MusicService.this, FloatLyricService.class);
            Intent webServerIntent = new Intent(MusicService.this, WebService.class);
            if (intent.getAction().contains(getPackageName())) {
                switch (intent.getAction().split(getPackageName())[1]) {
                    case ".OPEN_LYRIC":
                        if (!PermissionUtils.checkFloatPermission(MusicService.this)) {
                            //Toast.makeText(context, "请先在设置页授予悬浮窗权限", Toast.LENGTH_SHORT).show();
//                        Intent intent1 = new Intent(MusicService.this,MainActivity.class);
//                        intent1.putExtra("pageId",2);
//                        intent1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                        startActivity(intent1);
//                        PendingIntent contentIntent = PendingIntent.getActivity(
//                                MusicService.this, 0, intent1, PendingIntent.FLAG_UPDATE_CURRENT| PendingIntent.FLAG_IMMUTABLE);
                            String channelId = "权限通知"; // 通知渠道
                            Notification notification = new Notification.Builder(MusicService.this, channelId)
                                    .setContentTitle("综音无悬浮窗权限")
                                    .setContentText("请先在设置页授予悬浮窗权限后再开启桌面歌词")
                                    .setSmallIcon(R.mipmap.ic_launcher)
//                                .setContentIntent(contentIntent)
                                    .build();
                            NotificationManager mNotificationManager =
                                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                            mNotificationManager.notify(411, notification);
                        } else {
                            NotificationManager mNotificationManager =
                                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                            mNotificationManager.cancel(411);

                            SharedPrefs.putIsDeskLyric(true);
                            if ((!mainActivityState || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) && mSession.isActive()) {
                                deskLyricIntent.putExtra("isLock", SharedPrefs.getIsDeskLyricLock());
                                startService(deskLyricIntent);
                            }
                        }
                        break;
                    case ".CLOSE_LYRIC":
                        SharedPrefs.putIsDeskLyric(false);
                        stopService(deskLyricIntent);
                        break;
                    case ".FOREGROUND":
                        mainActivityState = true;
                        stopService(deskLyricIntent);
                        break;
                    case ".BACKGROUND":
                        mainActivityState = false;
                        if (SharedPrefs.getIsDeskLyric() && mSession.isActive() && hasPlayedOnce) {
                            deskLyricIntent.putExtra("isLock", SharedPrefs.getIsDeskLyricLock());
                            startService(deskLyricIntent);
                        }
                        break;
                    case ".LOCK_LYRIC":
                        SharedPrefs.putIsDeskLyricLock(true);
                        /*if(SharedPrefs.getIsDeskLyric()&&!mainActivityState){
                            intent.putExtra("isLock",true);
                            startService(deskLyricIntent);
                        }*/
                        break;
                    case ".UNLOCK_LYRIC":
                        SharedPrefs.putIsDeskLyricLock(false);
                    /*if(SharedPrefs.getIsDeskLyric()&&!mainActivityState){
                        intent.putExtra("isLock",false);
                        startService(deskLyricIntent);
                    }*/
                        break;
                    case ".WEB_ON":
                        startService(webServerIntent);
                        break;
                    case ".WEB_OFF":
                        stopService(webServerIntent);
                        break;
                    case Intent.ACTION_SCREEN_OFF:
                        stopService(deskLyricIntent);
                        break;
                    case Intent.ACTION_SCREEN_ON:
                        if (!mainActivityState && SharedPrefs.getIsDeskLyric() && mPlayback.isPlaying())
                            startService(deskLyricIntent);
                        break;
                }
                if (mSession.getController().getPlaybackState() != null)
                    new MediaPlayerListener().mServiceManager.updateNotificationForLyric(
                            mSession.getController().getPlaybackState()
                    );
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        registerLyricReceiver();
        sleepHandler = new Handler();

        mSession = new MediaSessionCompat(getApplicationContext(), MusicService.class.getSimpleName());
        setSessionToken(mSession.getSessionToken());
        mSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS |
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        mCallback = new MediaSessionCallback();
        mSession.setCallback(mCallback);
        mMediaNotificationManager = new MediaNotificationManager(this);
        mSession.setSessionActivity(mMediaNotificationManager.createContentIntent());

        mPlayback = new MediaPlayerAdapter(this, new MediaPlayerListener());
        mPlaylist=MusicLibrary.getPlayingMediaItemList();
        mCallback.onCustomAction("REFRESH_PLAYLIST",null);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //TODO 继承MediaButton
        MediaButtonReceiver.handleIntent(mSession, intent);
        //return super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        //MusicLibrary.savePlayingList(queueItems);
        mMediaNotificationManager.onDestroy();
        mSession.release();
        //关闭桌面歌词
        Intent deskLyricIntent = new Intent(MusicService.this, FloatLyricService.class);
        stopService(deskLyricIntent);
        unregisterReceiver(lyricReceiver);
        Log.d(TAG, "onDestroy: MediaPlayerAdapter stopped, and MediaSession released");
    }

    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {
        Log.e(TAG, "onGetRoot: "+clientPackageName);
        return new BrowserRoot(MusicService.class.getSimpleName(), null);
//        return null;
    }

    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
        Log.d(TAG, "SimpleMusicService onLoadChildren called: ");
        Log.d(TAG, "SimpleMusicService onLoadChildren parentId: " + parentId);
        Log.d(TAG, "SimpleMusicService onLoadChildren result: " + result);
        //if(mPlaylist==null||mPlaylist.size()==0)
        //mCallback.onCustomAction("REFRESH_PLAYLIST",null);

        result.sendResult(mPlaylist);
    }

    @Override
    public void onCustomAction(@NonNull String action, Bundle extras, @NonNull Result<Bundle> result) {
        super.onCustomAction(action, extras, result);
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
        private TimingThread timingThread=new TimingThread();
        private class TimingThread extends Thread {
            public boolean isTiming = false;

            @Override
            public void run() {
                super.run();
                int timing = 0;
                do {
                    try {
                        timing = SharedPrefs.getTiming();
                        Log.e(TAG, "timing" + timing);
                        if (timing > 0) {
                            isTiming = true;
                            Thread.sleep(60000);
                            timing--;
                            SharedPrefs.putTiming(timing);
                            Log.e(TAG,"Timing:"+timing);
                        }
                    } catch (InterruptedException | NullPointerException e) {
                        return;
                    }
                } while (timing > 0);
                onStop();
                isTiming = false;
            }
        }

        @Override
        public void onAddQueueItem(MediaDescriptionCompat description) {
            Log.d(TAG, "onAddQueueItem: Called SimpleMusicService");
            Log.d(TAG, String.format("onAddQueueItem: %s. Index: %s", description.getTitle(), description.hashCode()));
            onRemoveQueueItem(description);
            MediaSessionCompat.QueueItem queueItem = new MediaSessionCompat.QueueItem(description, description.hashCode());
            queueItemHashMap.remove(description.getMediaUri().getPath());
            mPlaylist.add(queueItem);
            if(mPlaylistOriginal.size()!=0)
                mPlaylistOriginal.add(queueItem);
            queueItemHashMap.put(description.getMediaUri().getPath(), queueItem);
            mQueueIndex = (mQueueIndex == -1) ? 0 : mQueueIndex;
            mSession.setQueue(mPlaylist);
            MusicLibrary.savePlayingList(mPlaylist);
        }

        @Override
        public void onAddQueueItem(MediaDescriptionCompat description, int index) {
            if(queueItemHashMap.containsKey(description.getMediaUri().getPath())){
                if(index==-2){
//                    if(!description.equals(queueItemHashMap.get(description.getMediaUri().getPath()).getDescription())) {//描述信息不同
                    if(!isSameDescription(description,queueItemHashMap.get(description.getMediaUri().getPath()).getDescription())) {//描述信息不同
                        MediaSessionCompat.QueueItem queueItem = new MediaSessionCompat.QueueItem(description, description.hashCode());
                        int sid=mPlaylist.indexOf(queueItemHashMap.get(description.getMediaUri().getPath()));
                        int id=mPlaylistOriginal.indexOf(queueItemHashMap.get(description.getMediaUri().getPath()));
                        if(sid>=0) mPlaylist.set(sid,queueItem);
                        if(id>=0) mPlaylistOriginal.set(id,queueItem);
                        queueItemHashMap.put(description.getMediaUri().getPath(), queueItem);
                        mSession.setQueue(mPlaylist);
                        MusicLibrary.savePlayingList(mPlaylist);
                    }
                    onSkipToQueueItem(mPlaylist.indexOf(queueItemHashMap.get(description.getMediaUri().getPath())));
                }else {
                    onRemoveQueueItem(description);
                    MediaSessionCompat.QueueItem queueItem = new MediaSessionCompat.QueueItem(description, description.hashCode());
                    if(index==-1){
                        mPlaylist.add(mQueueIndex + 1, queueItem);
                    }else {
                        mPlaylist.add(index, queueItem);
                    }
                    if (mPlaylistOriginal.size() != 0)
                        mPlaylistOriginal.add(queueItem);
                    mQueueIndex = (mQueueIndex == -1) ? 0 : mQueueIndex;
                    mSession.setQueue(mPlaylist);
                    MusicLibrary.savePlayingList(mPlaylist);
                }
            }else {
                MediaSessionCompat.QueueItem queueItem = new MediaSessionCompat.QueueItem(description, description.hashCode());
                queueItemHashMap.put(description.getMediaUri().getPath(), queueItem);
                if (index == -2) {
                    mPlaylist.add(++mQueueIndex, queueItem);
                    mPlaylistOriginal.add(queueItem);
                    mSession.setQueue(mPlaylist);
                    mPreparedMedia = null;
                    onPlay();
                    MusicLibrary.savePlayingList(mPlaylist);
                    return;
                } else if (index == -1) {
                    mPlaylist.add(mQueueIndex + 1, queueItem);
                } else {
                    mPlaylist.add(index, queueItem);
                }
                if (mPlaylistOriginal.size() != 0)
                    mPlaylistOriginal.add(queueItem);
                mQueueIndex = (mQueueIndex == -1) ? 0 : mQueueIndex;
                mSession.setQueue(mPlaylist);
                MusicLibrary.savePlayingList(mPlaylist);
            }
        }

        private boolean isSameDescription(MediaDescriptionCompat description, MediaDescriptionCompat description1) {
            return description.getMediaUri().equals(description1.getMediaUri())
                    &&description.getTitle().equals(description1.getTitle())
                    &&description.getSubtitle().equals(description1.getSubtitle());
        }

        @Override
        public void onRemoveQueueItem(MediaDescriptionCompat description) {
            Log.d(TAG, "onRemoveQueueItem: Called SimpleMusicService");
            Log.d(TAG, String.format("onRemoveQueueItem: %s. Index: %s", description.getTitle(), description.hashCode()));
            MediaSessionCompat.QueueItem toDelete=queueItemHashMap.get(description.getMediaUri().getPath());
            int toDeleteId= mPlaylist.indexOf(toDelete);
            if(toDelete!=null) {
                mPlaylist.remove(toDelete);
                mPlaylistOriginal.remove(toDelete);
                queueItemHashMap.remove(description.getMediaUri().getPath());
                mSession.setQueue(mPlaylist);
                if(mQueueIndex==toDeleteId){
                    //onStop();
                    mPreparedMedia=null;
                    if(mPlayback.isPlaying())
                        onPlay();
                    else {
                        onPrepare();
                        mSession.setMetadata(mPreparedMedia);
                    }
                }else if(mQueueIndex>toDeleteId){
                    mQueueIndex--;
                }
                if(mQueueIndex>=mPlaylist.size()){
                    mQueueIndex=mPlaylist.size()-1;
                }
                MusicLibrary.savePlayingList(mPlaylist);
            }
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
        public void onCommand(String command, Bundle extras, ResultReceiver cb) {
            Log.e(TAG,command);
            Bundle bundle=new Bundle();
            switch (command) {
                case "GET_LYRIC":
                    bundle.putSerializable("lyric", lyric);
                    break;
                case "IS_TIMING":
                    bundle.putBoolean("IS_TIMING", timingThread.isTiming);
                    break;
            }
            if(cb!=null)
                cb.send(0,bundle);
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
            if (mPlaylist.isEmpty()) {
                mSession.setMetadata(null);
                // Nothing to play.
                return;
            }else if(mQueueIndex>=mPlaylist.size()){
                mQueueIndex=mPlaylist.size()-1;
            }else if(mQueueIndex<0){
                mQueueIndex=0;
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
                mSession.setMetadata(null);
                onStop();
                return;
            }
            if (mPreparedMedia == null) {
                onPrepare();
                Log.e(TAG, String.valueOf(mPreparedMedia));
            }
            try {
                hasPlayedOnce=true;
                mPlayback.playFromMedia(mPreparedMedia);
            }catch (Exception e){
                //onPlayError(e);
                onStop();
            }
            if(mShuffleMode==PlaybackStateCompat.SHUFFLE_MODE_ALL){
                SharedPrefs.saveNowPlayId(mPlaylistOriginal.indexOf(mPlaylist.get(mQueueIndex)));
            }else {
                SharedPrefs.saveNowPlayId(mQueueIndex);
            }
        }

        private void onPlayError(Exception e) {
            Bundle bundle=new Bundle();
            bundle.putString("ERR_MSG",e.toString());
            mSession.sendSessionEvent("PLAY_ERROR",bundle);
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
            mQueueIndex= Math.toIntExact(id);
            //mQueueIndex = mPlaylist.stream().map(MediaSessionCompat.QueueItem::getQueueId).distinct().collect(Collectors.toList()).indexOf(id);
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
            //onPause();
            mPlayback.stop();
            mSession.setActive(false);
            mPreparedMedia=null;
        }

        @Override
        public void onSkipToNext() {
            if(mRepeatMode==PlaybackStateCompat.REPEAT_MODE_ALL) {
                mQueueIndex = (++mQueueIndex % mPlaylist.size());
                Log.d(TAG, "onSkipToNext: QueueIndex: " + mQueueIndex);
                mPreparedMedia = null;
                onPlay();
            }else if(mRepeatMode==PlaybackStateCompat.REPEAT_MODE_NONE){
                if(mQueueIndex==mPlaylist.size()-1)
                    onPause();
                else {
                    mQueueIndex++;
                    mPreparedMedia = null;
                    onPlay();
                }
            }
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
            MediaSessionCompat.QueueItem nowItem = null;
            if (isRandom) {
                long seed = System.nanoTime();
                if(mQueueIndex>=0&&mQueueIndex<mPlaylist.size()-1)
                    nowItem=mPlaylist.get(mQueueIndex);
                mPlaylistOriginal.clear();
                mPlaylistOriginal.addAll(mPlaylist);
                Collections.shuffle(mPlaylist, new Random(seed));
                if(mQueueIndex>=0&&mQueueIndex<mPlaylist.size()-1)
                    mQueueIndex=mPlaylist.indexOf(nowItem);
            } else {
                if(mPlaylistOriginal.size()!=0) {
                    if(mQueueIndex>=0&&mQueueIndex<mPlaylist.size()-1)
                        nowItem=mPlaylist.get(mQueueIndex);
                    mPlaylist.clear();
                    mPlaylist.addAll(mPlaylistOriginal);
                    if(mQueueIndex>=0&&mQueueIndex<mPlaylist.size()-1)
                        mQueueIndex=mPlaylist.indexOf(nowItem);
                }
            }
            mSession.setShuffleMode(shuffleMode);
            mSession.setQueue(mPlaylist);
            super.onSetShuffleMode(shuffleMode);
        }

        @Override
        public void onCustomAction(String action, Bundle extras) {
            Log.e(TAG,"session+"+action);
            switch (action){
                case "REFRESH_PLAYLIST":
                    Log.e(TAG,"Reconized+"+action);
                    List<MediaDescriptionCompat> newPlayList=MusicLibrary.getPlayingList();
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
                    mPlaylistOriginal.clear();
                    queueItemHashMap.clear();
                    for(MediaDescriptionCompat description:newPlayList) {
                        MediaSessionCompat.QueueItem queueItem = new MediaSessionCompat.QueueItem(description, description.hashCode());
                        mPlaylist.add(queueItem);
                        queueItemHashMap.put(queueItem.getDescription().getMediaUri().getPath(), queueItem);
                    }
                    if(mQueueIndex>=mPlaylist.size()){
                        mQueueIndex=mPlaylist.size()-1;
                    }
                    onSetRepeatMode(mRepeatMode);
                    onSetShuffleMode(mShuffleMode);
                    mSession.setQueue(mPlaylist);
                    mSession.setQueueTitle(SharedPrefs.getQueueTitle());
                    break;
                case "NEW_PLAYLIST":
                    List<MediaDescriptionCompat> PlayList=MusicLibrary.getNewPlayingList(extras.getString("QueueTitle","playingList"));
                    mPlaylist.clear();
                    mPlaylistOriginal.clear();
                    queueItemHashMap.clear();
                    for(MediaDescriptionCompat description:PlayList) {
                        MediaSessionCompat.QueueItem queueItem = new MediaSessionCompat.QueueItem(description, description.hashCode());
                        mPlaylist.add(queueItem);
                        queueItemHashMap.put(queueItem.getDescription().getMediaUri().getPath(), queueItem);
                    }
                    //mSession.setQueue(mPlaylist);
                    //mSession.setQueueTitle(extras.getString("QueueTitle","playingList"));
                    MusicLibrary.savePlayingList(mPlaylist);
                    SharedPrefs.saveQueueTitle(extras.getString("QueueTitle","playingList"));
                    onSetRepeatMode(mRepeatMode);
                    onSetShuffleMode(mShuffleMode);
                    //mQueueIndex=mPlaylist.stream().map(t -> t.getQueueId()).distinct().collect(Collectors.toList()).indexOf(extras.getLong("QueueId"));
                    if(extras.containsKey("Path")) {
                        //onSkipToQueueItem(
                        //        mPlaylist.get(
                        //                mPlaylist.stream().map(t -> t.getDescription().getMediaUri().getPath()).distinct().collect(Collectors.toList()).indexOf(extras.getString("Path"))
                        //        ).getQueueId());
                        onSkipToQueueItem(
                                mPlaylist.stream().map(t -> t.getDescription().getMediaUri().getPath()).distinct().collect(Collectors.toList()).indexOf(extras.getString("Path"))
                        );
                    }else {
                        if(!mPlaylist.isEmpty())
                            onSkipToQueueItem(0);
                    }
                    mSession.setQueue(mPlaylist);
                    mSession.setQueueTitle(extras.getString("QueueTitle","playingList"));
                    break;
                case "CLEAR_PLAYLIST":
                    onStop();
                    mQueueIndex=-1;
                    mPreparedMedia=null;
                    mPlaylist.clear();
                    mPlaylistOriginal.clear();
                    queueItemHashMap.clear();
                    mSession.setMetadata(null);
                    mSession.setQueue(mPlaylist);
                    mSession.setQueueTitle("playingList");
                    SharedPrefs.saveQueueTitle("playingList");
                    MusicLibrary.savePlayingList(mPlaylist);
                    break;
                case "TIMING":
                    if(extras.getBoolean("isStart",false))
                        startTiming();
                    else
                        stopTiming();
                    break;
            }
            super.onCustomAction(action, extras);
        }

        @Override
        public void onSkipToPrevious() {
            if(mRepeatMode==PlaybackStateCompat.REPEAT_MODE_ALL) {
                mQueueIndex = mQueueIndex > 0 ? mQueueIndex - 1 : mPlaylist.size() - 1;
                mPreparedMedia = null;
                onPlay();
            }else if(mRepeatMode==PlaybackStateCompat.REPEAT_MODE_NONE){
                if(mQueueIndex<=0)
                    onPause();
                else {
                    mQueueIndex--;
                    mPreparedMedia = null;
                    onPlay();
                }
            }
        }

        @Override
        public void onSeekTo(long pos) {
            mPlayback.seekTo(pos);
        }

        @Override
        public void onSetRating(RatingCompat rating) {
            mPreparedMedia=MusicLibrary.getMetadata(mPreparedMedia.getDescription().getMediaUri(),rating);
            mSession.setMetadata(mPreparedMedia);
        }

        @Override
        public void onSetCaptioningEnabled(boolean enabled) {
            Intent deskLyricIntent = new Intent(MusicService.this, FloatLyricService.class);
            if(enabled){
                SharedPrefs.putIsDeskLyric(true);
                if(!mainActivityState&&mSession.isActive()) {
                    deskLyricIntent.putExtra("isLock", SharedPrefs.getIsDeskLyricLock());
                    startService(deskLyricIntent);
                }
            }else {
                SharedPrefs.putIsDeskLyric(false);
                stopService(deskLyricIntent);
            }
        }

        private boolean isReadyToPlay() {
            return (!mPlaylist.isEmpty());
        }

        public void startTiming() {
            if (timingThread == null) {
                timingThread = new TimingThread();
                timingThread.start();
            } else {
                timingThread.interrupt();
                timingThread = new TimingThread();
                timingThread.start();
            }
        }

        public void stopTiming() {
            if (timingThread != null)
                timingThread.interrupt();
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
        public void onPlayingError(Exception e) {
            Bundle bundle=new Bundle();
            bundle.putString("ERR_MSG",e.toString());
            mSession.sendSessionEvent("PLAY_ERROR",bundle);
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
                    //关闭桌面歌词
                    Intent deskLyricIntent = new Intent(MusicService.this, FloatLyricService.class);
                    stopService(deskLyricIntent);
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
                            MusicService.this,
                            new Intent(MusicService.this, MusicService.class));
                    mServiceInStartedState = true;
                }
                try {
                    startForeground(MediaNotificationManager.NOTIFICATION_ID, notification);
                }catch (Exception e){
                    NotificationManager mNotificationManager =
                            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    mNotificationManager.notify(MediaNotificationManager.NOTIFICATION_ID,notification);
                }
            }

            private void updateNotificationForPause(PlaybackStateCompat state) {
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
                mMediaNotificationManager.getNotificationManager()
                        .cancel(MediaNotificationManager.NOTIFICATION_ID);
                mServiceInStartedState = false;
                stopSelf();
            }
        }

    }
}
