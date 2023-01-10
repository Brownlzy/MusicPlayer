/*
 * Copyright 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.liux.musicplayer.media;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import com.blankj.utilcode.util.FileUtils;
import com.liux.musicplayer.activities.MainActivity;
import com.liux.musicplayer.services.MusicService;
import com.liux.musicplayer.utils.SharedPrefs;

import java.io.IOException;


/**
 * Exposes the functionality of the {@link MediaPlayer} and implements the {@link PlayerAdapter}
 * so that {@link MainActivity} can control music playback.
 */
public final class MediaPlayerAdapter extends PlayerAdapter {

    private static final long DURATION_DELAY  = 500;
    private final Context mContext;
    private final PlaybackInfoListener mPlaybackInfoListener;
    MusicService musicService;
    Handler handler = new Handler();
    Handler sleepHandler = new Handler();
    private MediaPlayer mMediaPlayer;
    private String mFilename;
    private MediaMetadataCompat mCurrentMedia;
    private int mState;
    private boolean mCurrentMediaPlayedToCompletion;
    private boolean isRepeating = false;
    // Work-around for a MediaPlayer bug related to the behavior of MediaPlayer.seekTo()
    // while not playing.

    private int mSeekWhileNotPlaying = -1;
    private String TAG = "MediaPlayerAdaptar";

    public MediaPlayerAdapter(MusicService musicService, PlaybackInfoListener listener) {
        super(musicService.getApplicationContext());
        this.musicService = musicService;
        mContext = musicService.getApplicationContext();
        mPlaybackInfoListener = listener;
    }

    private int lastProgress = 0;

    // Implements PlaybackControl.
    @Override
    public void playFromMedia(MediaMetadataCompat metadata) {
        mCurrentMedia = metadata;
        playFile(metadata.getDescription().getMediaUri().toString());
    }

    @Override
    public MediaMetadataCompat getCurrentMedia() {
        return mCurrentMedia;
    }

    /**
     * Once the {@link MediaPlayer} is released, it can't be used again, and another one has to be
     * created. In the onStop() method of the {@link MainActivity} the {@link MediaPlayer} is
     * released. Then in the onStart() of the {@link MainActivity} a new {@link MediaPlayer}
     * object has to be created. That's why this method is private, and called by load(int) and
     * not the constructor.
     */
    private void initializeMediaPlayer() {
        if (mMediaPlayer == null) {
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    {
                        mPlaybackInfoListener.onPlaybackCompleted();
                        // Set the state to "paused" because it most closely matches the state
                        // in MediaPlayer with regards to available state transitions compared
                        // to "stop".
                        // Paused allows: seekTo(), start(), pause(), stop()
                        // Stop allows: stop()
                        // 避免切歌时后台启动前台服务
                        setNewState(PlaybackStateCompat.STATE_BUFFERING);
                        if (isRepeating) {
                            //musicService.mCallback.onSkipToThis();
                            //mMediaPlayer.seekTo(0);
                            //setNewState(PlaybackStateCompat.STATE_PLAYING);
                            mFilename = null;
                            playFromMedia(getCurrentMedia());
                            //musicService.mCallback.onSkipToThis();
                        } else {

                            musicService.mCallback.onSkipToNext();

                        }
                    }
                }
            });
            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    play();
                }
            });
            mMediaPlayer.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
                @Override
                public void onBufferingUpdate(MediaPlayer mp, int percent) {
//                    Log.e("mMediaPlayer/onBufferingUpdat", String.valueOf(percent));
//                    Intent progressIntent = new Intent(mContext.getPackageName() + ".CACHE_PROGRESS");
//                    progressIntent.putExtra("p", percent);
//                    mContext.sendBroadcast(progressIntent);
                }
            });
        } else {
            mMediaPlayer.reset();
        }
    }

    private void startSleepTimer() {
        if (SharedPrefs.isSleepTimerOn() /*&& shouldRunSleepTimer()*/) {
            //handler.post(sleepTimerRunnable);
        } else {
            //handler.removeCallbacks(sleepTimerRunnable);
        }
    }

    private boolean shouldRunSleepTimer() {
        long sleepTimeInMillis = SharedPrefs.getSleepTime();
        long currentTimeInMillis = System.currentTimeMillis();

        Log.d(TAG, String.format("%s < %s", sleepTimeInMillis, currentTimeInMillis));
        return sleepTimeInMillis > currentTimeInMillis;

    }

    @Override
    public void onStop() {
        // Regardless of whether or not the MediaPlayer has been created / started, the state must
        // be updated, so that MediaNotificationManager can take down the notification.
        setNewState(PlaybackStateCompat.STATE_STOPPED);
        release();
    }

    private void release() {
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    @Override
    public boolean isPlaying() {
        return mMediaPlayer != null && mMediaPlayer.isPlaying();
    }

    @Override
    protected void onPlay() {
        if (mMediaPlayer != null && !mMediaPlayer.isPlaying()) {
            mMediaPlayer.start();
            setNewState(PlaybackStateCompat.STATE_PLAYING);
            //TODO 这样会导致卡顿
            //handler.postDelayed(runnable, DURATION_DELAY);
            //startSleepTimer();
        }
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause: Called ");
        if (mMediaPlayer != null
                && mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();
            setNewState(PlaybackStateCompat.STATE_PAUSED);
        }
    }

    // This is the main reducer for the player state machine.
    private void setNewState(@PlaybackStateCompat.State int newPlayerState) {
        mState = newPlayerState;
        Log.e(TAG, String.valueOf(newPlayerState));

        // Whether playback goes to completion, or whether it is stopped, the
        // mCurrentMediaPlayedToCompletion is set to true.
        if (mState == PlaybackStateCompat.STATE_STOPPED) {
            mCurrentMediaPlayedToCompletion = true;
        }

        // Work around for MediaPlayer.getCurrentPosition() when it changes while not playing.
        final long reportPosition;
        if (mSeekWhileNotPlaying >= 0) {
            reportPosition = mSeekWhileNotPlaying;

            if (mState == PlaybackStateCompat.STATE_PLAYING) {
                mSeekWhileNotPlaying = -1;
            }
        } else {
            reportPosition = mMediaPlayer == null ? 0 : mMediaPlayer.getCurrentPosition();
        }
        final PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder();
        stateBuilder.setActions(getAvailableActions());
        stateBuilder.setState(mState,
                reportPosition,
                1.0f,
                SystemClock.elapsedRealtime());
        mPlaybackInfoListener.onPlaybackStateChange(stateBuilder.build());
    }

    /**
     * Set the current capabilities available on this session. Note: If a capability is not
     * listed in the bitmask of capabilities then the MediaSession will not handle it. For
     * example, if you don't want ACTION_STOP to be handled by the MediaSession, then don't
     * included it in the bitmask that's returned.
     */
    @PlaybackStateCompat.Actions
    private long getAvailableActions() {
        long actions = PlaybackStateCompat.ACTION_PLAY_FROM_URI
                | PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH
                | PlaybackStateCompat.ACTION_SET_RATING
                | PlaybackStateCompat.ACTION_SET_CAPTIONING_ENABLED
                | PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS;
        switch (mState) {
            case PlaybackStateCompat.STATE_STOPPED:
                actions |= PlaybackStateCompat.ACTION_PLAY
                        | PlaybackStateCompat.ACTION_PAUSE;
                break;
            case PlaybackStateCompat.STATE_PLAYING:
                actions |= PlaybackStateCompat.ACTION_STOP
                        | PlaybackStateCompat.ACTION_PAUSE
                        | PlaybackStateCompat.ACTION_SEEK_TO
                        | PlaybackStateCompat.ACTION_SET_REPEAT_MODE;
                break;
            case PlaybackStateCompat.STATE_PAUSED:
                actions |= PlaybackStateCompat.ACTION_PLAY
                        | PlaybackStateCompat.ACTION_SEEK_TO
                        | PlaybackStateCompat.ACTION_STOP;
                break;
            default:
                actions |= PlaybackStateCompat.ACTION_PLAY
                        | PlaybackStateCompat.ACTION_PLAY_PAUSE
                        | PlaybackStateCompat.ACTION_STOP
                        | PlaybackStateCompat.ACTION_PAUSE;
        }
        return actions;
    }

    @Override
    public void seekTo(long position) {
        if (mMediaPlayer != null) {
            if (!mMediaPlayer.isPlaying()) {
                mSeekWhileNotPlaying = (int) position;
            }
            mMediaPlayer.seekTo((int) position);

            // Set the state (to the current state) because the position changed and should
            // be reported to clients.
            setNewState(mState);
        }
    }

    @Override
    public void setVolume(float volume) {
        if (mMediaPlayer != null) {
            mMediaPlayer.setVolume(volume, volume);
        }
    }

    private void playFile(String filename) {
        boolean mediaChanged = (mFilename == null || !filename.equals(mFilename));
        if (mCurrentMediaPlayedToCompletion) {
            // Last audio file was played to completion, the resourceId hasn't changed, but the
            // player was released, so force a reload of the media file for playback.
            mediaChanged = true;
            mCurrentMediaPlayedToCompletion = false;
        }
        if (!mediaChanged) {
            if (!isPlaying()) {
                play();
            }
            return;
        } else {
            release();
        }

        mFilename = filename;

        if (FileUtils.isFileExists(mFilename)) {
            try {
                initializeMediaPlayer();
                mMediaPlayer.setDataSource(mContext.getApplicationContext(), Uri.parse(filename));
                mMediaPlayer.prepare();
            } catch (Exception e) {
                onStop();
                mPlaybackInfoListener.onPlayingError(new RuntimeException("Failed to open file: " + mFilename, e));
            }
        } else if (mFilename.matches("^(https?)://[-A-Za-z0-9+&@#/%?=~_|!:,.;]+[-A-Za-z0-9+&@#/%=~_|]+[\\S\\s]*")) {
            try {
                String proxyUrl = musicService.getProxy().getProxyUrl(mFilename);
                musicService.getProxy().registerCacheListener(musicService.cacheListener, mFilename);
                initializeMediaPlayer();
                mMediaPlayer.setDataSource(proxyUrl);
                mMediaPlayer.prepareAsync();
            } catch (IOException e) {
                e.printStackTrace();
            }
            setNewState(PlaybackStateCompat.STATE_BUFFERING);
        } else {
            throw new RuntimeException("Failed to open file: " + mFilename);
        }
    }

    public int getNowPercentage() {
        if (mMediaPlayer.isPlaying()
                && mMediaPlayer.getDuration() != 0) {
            lastProgress = 100 * mMediaPlayer.getCurrentPosition() / mMediaPlayer.getDuration();
        }
        return lastProgress;
    }

    public void setRepeating(boolean b) {
        this.isRepeating = b;
    }
}
