package com.liux.musicplayer.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.liux.musicplayer.ui.MainActivity;

public class NotificationClickReceiver extends BroadcastReceiver {
    public static final String TAG = "NotificationClickReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "通知栏点击");
        //获取栈顶的Activity
        intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setClass(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        context.startActivity(intent);
    }
}