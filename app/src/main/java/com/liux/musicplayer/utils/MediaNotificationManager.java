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

package com.liux.musicplayer.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.media.session.MediaButtonReceiver;

import com.liux.musicplayer.R;
import com.liux.musicplayer.activities.MainActivity;
import com.liux.musicplayer.services.MusicService;


/**
 * Keeps track of a notification and updates it automatically for a given MediaSession. This is
 * required so that the music service don't get killed during playback.
 */
public class MediaNotificationManager {

    public static final int NOTIFICATION_ID = 412;

    private static final String TAG = MediaNotificationManager.class.getSimpleName();
    private static final String CHANNEL_ID = "MediaController";
    private static final int REQUEST_CODE = 501;

//    private final MusicService mService;
    private final MusicService mService;

    private final NotificationCompat.Action mPlayAction;
    private final NotificationCompat.Action mPauseAction;
    private final NotificationCompat.Action mNextAction;
    private final NotificationCompat.Action mPrevAction;
    private final NotificationCompat.Action mStopAction;
    private final NotificationCompat.Action mSlyricAction;
    private final NotificationCompat.Action mHlyricAction;
    private final NotificationManager mNotificationManager;

    public MediaNotificationManager(MusicService service) {
        mService = service;

        mNotificationManager =
                (NotificationManager) mService.getSystemService(Context.NOTIFICATION_SERVICE);

        mPlayAction =
                new NotificationCompat.Action(
                        R.drawable.ic_round_play_arrow_24,
                        "Play",
                        MediaButtonReceiver.buildMediaButtonPendingIntent(
                                mService,
                                PlaybackStateCompat.ACTION_PLAY));
        mPauseAction =
                new NotificationCompat.Action(
                        R.drawable.ic_round_pause_24,
                        "Pause",
                        MediaButtonReceiver.buildMediaButtonPendingIntent(
                                mService,
                                PlaybackStateCompat.ACTION_PAUSE));
        mNextAction =
                new NotificationCompat.Action(
                        R.drawable.ic_round_skip_next_24,
                        "Next",
                        MediaButtonReceiver.buildMediaButtonPendingIntent(
                                mService,
                                PlaybackStateCompat.ACTION_SKIP_TO_NEXT));
        mPrevAction =
                new NotificationCompat.Action(
                        R.drawable.ic_round_skip_previous_24,
                        "Previous",
                        MediaButtonReceiver.buildMediaButtonPendingIntent(
                                mService,
                                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS));

        mStopAction =
                new NotificationCompat.Action(
                        R.drawable.ic_round_stop_24,
                        "Stop",
                        MediaButtonReceiver.buildMediaButtonPendingIntent(
                                mService,
                                PlaybackStateCompat.ACTION_STOP));

        Intent floatLyricIntent;
        PendingIntent floatLyricPendingIntent;

        floatLyricIntent = new Intent(mService.getPackageName() + ".OPEN_LYRIC");
        floatLyricPendingIntent = PendingIntent.getBroadcast(mService, 0, floatLyricIntent, PendingIntent.FLAG_IMMUTABLE);
        mSlyricAction =
                new NotificationCompat.Action(
                        R.drawable.ic_round_subtitles_off_18,
                        "openLyric",
                        floatLyricPendingIntent);

        floatLyricIntent = new Intent(mService.getPackageName() + ".CLOSE_LYRIC");
        floatLyricPendingIntent = PendingIntent.getBroadcast(mService, 0, floatLyricIntent, PendingIntent.FLAG_IMMUTABLE);
        mHlyricAction =
                new NotificationCompat.Action(
                        SharedPrefs.getIsDeskLyricLock()?
                                R.drawable.ic_round_lock_18:
                                R.drawable.ic_round_subtitles_18,
                        "closeLyric",
                        floatLyricPendingIntent);

        // Cancel all notifications to handle the case where the Service was killed and
        // restarted by the system.
        mNotificationManager.cancelAll();
    }

    public void onDestroy() {
        mNotificationManager.cancelAll();
        Log.d(TAG, "onDestroy: ");

    }

    public NotificationManager getNotificationManager() {
        return mNotificationManager;
    }

    public Notification getNotification(MediaMetadataCompat metadata,
                                        @NonNull PlaybackStateCompat state,
                                        MediaSessionCompat.Token token) {
        boolean isPlaying = state.getState() == PlaybackStateCompat.STATE_PLAYING;
        MediaDescriptionCompat description = metadata.getDescription();
        NotificationCompat.Builder builder =
                buildNotification(state, token, isPlaying, description);
        return builder.build();
    }
    private String getAlbumArt(int album_id)    {
        String mUriAlbums = "content://media/external/audio/albums";
        String[] projection = new String[] { "album_art" };
        Cursor cur = mService.getContentResolver().query(Uri.parse(mUriAlbums + "/" + Integer.toString(album_id)),  projection, null, null, null);
        String album_art = null;
        if (cur.getCount() > 0 && cur.getColumnCount() > 0)
        {  cur.moveToNext();
            album_art = cur.getString(0);
        }
        cur.close();
        cur = null;
        return album_art;
    }

    private NotificationCompat.Builder buildNotification(@NonNull PlaybackStateCompat state,
                                                         MediaSessionCompat.Token token,
                                                         boolean isPlaying,
                                                         MediaDescriptionCompat description) {

        // Create the (mandatory) notification channel when running on Android Oreo.
        if (isAndroidOOrHigher()) {
            createChannel();
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(mService, CHANNEL_ID);
        builder.setStyle(
                new androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(token)
                        .setShowActionsInCompactView(2, 3, 4)
                        // For backwards compatibility with Android L and earlier.
                        .setShowCancelButton(true)
                        .setCancelButtonIntent(
                                MediaButtonReceiver.buildMediaButtonPendingIntent(
                                        mService,
                                        PlaybackStateCompat.ACTION_STOP)))
                //.setColor(ContextCompat.getColor(mService, R.color.notification_bg))
                .setSmallIcon(R.drawable.ic_baseline_music_note_white_24)
                .setLargeIcon(description.getIconBitmap())
                // Pending intent that is fired when user clicks on notification.
                .setContentIntent(createContentIntent())
                // Title - Usually Song name.
                .setContentTitle(description.getTitle())
                // Subtitle - Usually Artist name.
                .setContentText(description.getSubtitle())
//                .setLargeIcon(MusicLibrary.getAlbumBitmap(mService, description.getMediaId()))
                // When notification is deleted (when playback is paused and notification can be
                // deleted) fire MediaButtonPendingIntent with ACTION_STOP.
                .setDeleteIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(
                        mService, PlaybackStateCompat.ACTION_STOP))
                // Show controls on lock screen even when user hides sensitive content.
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        builder.addAction(mStopAction);

        // If skip to next action is enabled.
        if ((state.getActions() & PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS) != 0) {
            builder.addAction(mPrevAction);
        }

        builder.addAction(isPlaying ? mPauseAction : mPlayAction);

        // If skip to prev action is enabled.
        if ((state.getActions() & PlaybackStateCompat.ACTION_SKIP_TO_NEXT) != 0) {
            builder.addAction(mNextAction);
        }

        Intent floatLyricIntent = new Intent(mService.getPackageName() + ".CLOSE_LYRIC");
        PendingIntent floatLyricPendingIntent = PendingIntent.getBroadcast(mService, 0, floatLyricIntent, PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Action mHlyricAction =
                new NotificationCompat.Action(
                        SharedPrefs.getIsDeskLyricLock()?
                                R.drawable.ic_round_lock_18:
                                R.drawable.ic_round_subtitles_18,
                        "closeLyric",
                        floatLyricPendingIntent);

        builder.addAction(SharedPrefs.getIsDeskLyric()? mHlyricAction :mSlyricAction);

        return builder;
    }

    // Does nothing on versions of Android earlier than O.
    @RequiresApi(Build.VERSION_CODES.O)
    private void createChannel() {
        if (mNotificationManager.getNotificationChannel(CHANNEL_ID) == null) {
            // The user-visible name of the channel.
            CharSequence name = "MediaSession";
            // The user-visible description of the channel.
            String description = "MediaSession and MediaPlayer";
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name, importance);
            // Configure the notification channel.
            mChannel.setDescription(description);
            mChannel.enableLights(true);
            // Sets the notification light color for notifications posted to this
            // channel, if the device supports this feature.
            mChannel.setLightColor(Color.RED);
            mChannel.enableVibration(true);
            mChannel.setVibrationPattern(
                    new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
            mNotificationManager.createNotificationChannel(mChannel);
           // Log.d(TAG, "createChannel: New channel created");
        } else {
            //Log.d(TAG, "createChannel: Existing channel reused");
        }
        String channelId = "????????????"; // ????????????
        NotificationManager mNotificationManager =
                (NotificationManager) mService.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel channel = new NotificationChannel(
                channelId,
                "????????????",
                NotificationManager.IMPORTANCE_DEFAULT);
        mNotificationManager.createNotificationChannel(channel);
    }

    private boolean isAndroidOOrHigher() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
    }

    public PendingIntent createContentIntent() {
        Intent openUI = new Intent(mService, MainActivity.class);
        openUI.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(
                mService, REQUEST_CODE, openUI, PendingIntent.FLAG_CANCEL_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }

}