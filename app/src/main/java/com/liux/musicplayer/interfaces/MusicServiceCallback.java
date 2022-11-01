package com.liux.musicplayer.interfaces;

/**
 * 用于{@link com.liux.musicplayer.service.MusicService}通知{@link com.liux.musicplayer.ui.MainActivity}更新播放状态的回调接口
 *
 * @author Brownlzy
 * @CreateDate: 2022/9/28
 * @UpdateDate: 2022/10/22
 * @UpdateRemark: 更新内容
 * @Version: 1.0
 */
public interface MusicServiceCallback {
    /**
     * 通知注册的接口当前正在播放的音乐id
     *
     * @param musicID 当前正在播放的音乐id
     * @return void
     */
    void nowPlayingThis(int musicID);

    /**
     * 通知注册的接口当前正在播放的音乐id
     *
     * @param musicID 当前正在播放的音乐id
     * @return void
     */
    void playingErrorThis(int musicID);

    /**
     * 通知需要更新当前播放状态
     *
     * @return void
     */
    void updatePlayStateThis();

    /**
     * 通知当初正在加载的音乐id
     *
     * @param musicId 当前正在加载的音乐id
     * @return void
     */
    void nowLoadingThis(int musicId);
}
