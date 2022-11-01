package com.liux.musicplayer.interfaces;
 /**
  * 用于控制桌面歌词状态的接口
  * @author         Brownlzy
  * @CreateDate:     2022/10/22
  * @UpdateUser:     updater
  * @UpdateDate:     2022/10/22
  * @UpdateRemark:   更新内容
  * @Version:        1.0
  */
public interface DeskLyricCallback {
     /**
      * 用于通知桌面歌词更新歌词和播放信息
      * @param musicId 新的播放id（来自MusicService中的播放列表）
      * @return void
      */
    void updateNowPlaying(int musicId);
     /**
      * 用于通知桌面歌词播放状态的更新
      * @return void
      */
    void updatePlayState();
}
