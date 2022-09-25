package com.liux.musicplayer.ui;

import android.content.ComponentName;
import android.media.MediaMetadata;
import android.media.browse.MediaBrowser;
import android.media.session.MediaController;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.liux.musicplayer.service.MusicSession;

import java.util.List;

public class MusicActivity extends AppCompatActivity {
    private static final String TAG = "SimulateClientActivity";
    private MediaBrowser mBrowser = null;
    private MediaController mController = null;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBrowser = new MediaBrowser(this, new ComponentName(this, MusicSession.class),
                browserConnectionCallback, null);
    }

    /**
     * 连接状态的回调接口
     */
    MediaBrowser.ConnectionCallback browserConnectionCallback = new MediaBrowser.ConnectionCallback() {
        @Override
        public void onConnected() {
            super.onConnected();
            Log.e(TAG, "onConnected: ");
            //进行订阅操作
            if (!mBrowser.isConnected()) return;
            Log.e(TAG, "onConnected: 2");
            //运行连接会正常返回值,不允许连接会返回null
            String mediaId = mBrowser.getRoot();
            //注册回调
            mController = new MediaController(MusicActivity.this, mBrowser.getSessionToken());
            mController.registerCallback(controllerCallback);
            //browser通过订阅的方式向Service请求数据,发起订阅需要mediaId参数
            mBrowser.unsubscribe(mediaId);
            mBrowser.subscribe(mediaId, browserSubscriptionCallback);
            Log.e(TAG, "subscribe");
        }

        @Override
        public void onConnectionFailed() {
            super.onConnectionFailed();
            Log.i(TAG, "onConnectionFailed: 连接失败");
        }
    };

    //播放状态改变的回调
    private MediaController.Callback controllerCallback = new MediaController.Callback() {

    };
    /**
     * 向MediaBrowserService发起数据订阅请求后的回调接口
     */
    private MediaBrowser.SubscriptionCallback browserSubscriptionCallback = new MediaBrowser.SubscriptionCallback() {
        @Override
        public void onChildrenLoaded(@NonNull String parentId, @NonNull List<MediaBrowser.MediaItem> children) {
            Log.e(TAG, "onChildrenLoaded: ");
            //service发送回来的媒体数据集合
            for (MediaBrowser.MediaItem child : children) {
                Log.e(TAG, "onChildrenLoaded: " + child.getDescription().getTitle());
            }
            //执行ui刷新
            handlerPlayEvent();
        }
    };

    @Override
    public void onStart() {
        super.onStart();
        mBrowser.connect();
    }

    @Override
    public void onStop() {
        super.onStop();
        mBrowser.disconnect();
    }

    /**
     * 处理播放按钮事件
     */
    private void handlerPlayEvent() {
        Log.e(TAG, String.valueOf(mController.getPlaybackState().getState()));
        switch (mController.getPlaybackState().getState()) {
            case PlaybackState.STATE_PLAYING:
                mController.getTransportControls().pause();
                break;
            case PlaybackState.STATE_PAUSED:
                mController.getTransportControls().play();
                break;
            default:
                mController.getTransportControls().playFromSearch("Galway Girl", null);
                break;
        }
    }

    /**
     * 媒体控制器控制播放过程中的回调接口，可以用来根据播放状态更新UI
     */
    private final MediaController.Callback ControllerCallback =
            new MediaController.Callback() {
                /***
                 * 音乐播放状态改变的回调
                 * @param state
                 */
                @Override
                public void onPlaybackStateChanged(PlaybackState state) {
                    switch (state.getState()) {
                        case PlaybackState.STATE_NONE://无任何状态
                            Log.e(TAG, "STATE_NONE");
                            break;
                        case PlaybackState.STATE_PAUSED:
                            Log.e(TAG, "STATE_PAUSED");
                            break;
                        case PlaybackState.STATE_PLAYING:
                            Log.e(TAG, "STATE_PLAYING");
                            break;
                    }
                }

                /**
                 * 播放音乐改变的回调
                 * @param metadata
                 */
                @Override
                public void onMetadataChanged(MediaMetadata metadata) {
                    Log.e(TAG, (String) metadata.getDescription().getTitle());
                }
            };
}
