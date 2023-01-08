package com.liux.musicplayer.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.liux.musicplayer.R;
import com.liux.musicplayer.activities.MainActivity;
import com.liux.musicplayer.services.HttpServer.Config;
import com.liux.musicplayer.utils.SharedPrefs;

import java.io.IOException;

import fi.iki.elonen.NanoHTTPD;

public class WebService extends Service {
    public static final int NOTIFICATION_ID = 418;
    private static final String CHANNEL_ID = "WebServer通知";
    private HttpServer mHttpServer = null;
    private NotificationManager mNotificationManager;
    private Notification mNotification;

    @Override
    public void onCreate() {
        super.onCreate();
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        createChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startHttpServer();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        mHttpServer.stop();
        stopForeground(true);
        SharedPrefs.setWebServerEnable(false);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        startHttpServer();
        return null;
    }

    private void startHttpServer() {
        WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ipAddress = wifiInfo.getIpAddress();
        String ip = (ipAddress & 0xff) + "." + (ipAddress >> 8 & 0xff) + "." + (ipAddress >> 16 & 0xff) + "." + (ipAddress >> 24 & 0xff);
        Config.HTTP_IP = ip;
        mHttpServer = new HttpServer(ip, Config.HTTP_PORT);
        try {
            mHttpServer.asset_mgr = this.getAssets();
            mHttpServer.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
        } catch (IOException e) {
            //e.printStackTrace();
            Log.e("WebServer", Config.HTTP_IP + ":" + String.valueOf(Config.HTTP_PORT));
            SharedPrefs.setWebServerEnable(false);
            stopSelf();
            return;
        }
        SharedPrefs.setWebServerEnable(true);
        createNotification(Config.HTTP_URL.replace("IP", ip).replace("PORT", String.valueOf(Config.HTTP_PORT)));
        startForeground(NOTIFICATION_ID, mNotification);
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private void createChannel() {
        if (mNotificationManager.getNotificationChannel(CHANNEL_ID) == null) {
            // The user-visible name of the channel.
            CharSequence name = "WebServer通知";
            // The user-visible description of the channel.
            String description = "WebServer Info";
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
    }

    private Notification createNotification(String url) {
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE);
        mNotification = new Notification.Builder(WebService.this, CHANNEL_ID)
                .setContentTitle("WebServer Running...")
                .setContentText("URL: " + url)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(contentIntent)
                .build();
        return mNotification;
    }
}
