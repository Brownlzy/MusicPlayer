package com.liux.musicplayer.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.session.MediaSession;
import android.view.KeyEvent;

import androidx.annotation.NonNull;

/**
 * 监听musicButton事件（线控播放）
 */
public class MediaButtonReceiver extends BroadcastReceiver {
    private MediaSession mMediaSession;

    public class KeyActions {
        //所有keyCode参考:https://www.apiref.com/android-zh/android/view/KeyEvent.html
        public static final int PLAY_ACTION = 126;
        public static final int PAUSE_ACTION = 127;
        public static final int PREV_ACTION = 88;
        public static final int NEXT_ACTION = 87;
        public static final int KEYCODE_HEADSETHOOK = 79;
    }

    public interface IKeyDownListener {
        void onKeyDown(int keyAction);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // TO-DO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public MediaButtonReceiver(Context context, IKeyDownListener mKeyDownListener) {
        mMediaSession = new MediaSession(context, "MediaButtonReceiver");
        mMediaSession.setCallback(new MediaSession.Callback() {
            @Override
            public boolean onMediaButtonEvent(@NonNull Intent mediaButtonIntent) {
                KeyEvent keyEvent = mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                if (keyEvent != null) {
                    mKeyDownListener.onKeyDown(keyEvent.getKeyCode());//把请求传给注册的监听器
                    return true;
                } else return false;
            }
        });
        mMediaSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS | MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mMediaSession.setActive(true);
    }
}