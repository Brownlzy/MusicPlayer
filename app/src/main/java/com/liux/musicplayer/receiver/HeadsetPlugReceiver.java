package com.liux.musicplayer.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * 监听有线耳机连接状态
 */
public class HeadsetPlugReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.hasExtra("state")) {
            if (intent.getIntExtra("state", 0) == 0) {
                iHeadsetPlugListener.onHeadsetUnplugged();
            } else if (intent.getIntExtra("state", 0) == 1) {
                iHeadsetPlugListener.onHeadsetPlugged();
            }
        }
    }

    public interface IHeadsetPlugListener {
        void onHeadsetPlugged();

        void onHeadsetUnplugged();
    }

    private IHeadsetPlugListener iHeadsetPlugListener;

    public void setIHeadsetPlugListener(IHeadsetPlugListener iHeadsetPlugListener) {
        this.iHeadsetPlugListener = iHeadsetPlugListener;
    }

}