package com.liux.musicplayer.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.media.MediaMetadataCompat;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.blankj.utilcode.util.ConvertUtils;
import com.google.android.material.imageview.ShapeableImageView;
import com.liux.musicplayer.R;
import com.liux.musicplayer.adapters.LyricAdapter;
import com.liux.musicplayer.models.Song;
import com.liux.musicplayer.utils.LyricUtils;
import com.liux.musicplayer.utils.MusicUtils;

public class InfoFragment extends Fragment {

    public LyricUtils lyric;
    public ListView lyricList;
    private TextView songTitle;
    private TextView songArtist;
    private TextView songInfo;
    private ImageView PlayBarLyric;
    private ShapeableImageView albumImageView;
    private RelativeLayout songLyricLayout;
    private View mView;
    private LyricAdapter adapter;
    private boolean isSetLyricPosition = true;
    private final SparseBooleanArray nowLyricMap = new SparseBooleanArray();//用来存放高亮歌词的选中状态，true为选中,false为没有选中
    private int lastLyricId;
    private boolean lastLyricEnabled = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        Bundle bundle=this.getArguments();
        mView=view;
        initViewCompat();
        if (bundle != null) {
            Log.e("InfoFragment",bundle.toString());
            Song song = new Song(bundle.getString("path"));
            setMusicInfo(song);
        }
        return view;
    }

    private void initViewCompat() {
        songTitle = mView.findViewById(R.id.home_song_title);
        songArtist = mView.findViewById(R.id.home_song_artist);
        songInfo = mView.findViewById(R.id.home_song_info);
        albumImageView = mView.findViewById(R.id.albumImageView);
        songLyricLayout = mView.findViewById(R.id.songLyricLayout);
        lyricList = mView.findViewById(R.id.lyricList);
        //绑定歌词列表
        mView.findViewById(R.id.albumImageView).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                setIsLyric();
                return false;
            }
        });
        PlayBarLyric = mView.findViewById(R.id.playLyric);
        PlayBarLyric.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setIsLyric();
            }
        });
        lyricList = mView.findViewById(R.id.lyricList);
        lyricList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        lyricList.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                // OnScrollListener.SCROLL_STATE_FLING; //屏幕处于甩动状态
                // OnScrollListener.SCROLL_STATE_IDLE; //停止滑动状态
                // OnScrollListener.SCROLL_STATE_TOUCH_SCROLL;// 手指接触状态
                if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
                    //手指接触时设置停止歌词居中标志
                    isSetLyricPosition = false;
                } else if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
                    //列表完全停下时设置启用歌词居中标志
                    isSetLyricPosition = true;
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            }
        });
    }

    public void setIsLyric() {
        setIsLyricLayoutShow(!lastLyricEnabled);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @SuppressLint("SetTextI18n")
    public void setMusicInfo(Song song) {
        if (mView != null) {
            //重新获取一遍防止空指针
            songTitle = mView.findViewById(R.id.home_song_title);
            songArtist = mView.findViewById(R.id.home_song_artist);
            songInfo = mView.findViewById(R.id.home_song_info);
            albumImageView = mView.findViewById(R.id.albumImageView);
            songLyricLayout = mView.findViewById(R.id.songLyricLayout);
            lyricList = mView.findViewById(R.id.lyricList);
            lyricList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
            //从文件中读取metadata数据
            MusicUtils.Metadata metadata = MusicUtils.getMetadata(song.getSongPath());
            if (metadata.isValid) { //如果metadata有效标志为真则使用metadata的数据
                songTitle.setText((metadata.title == null) ? song.getSongTitle() : metadata.title);
                songArtist.setText((metadata.artist == null) ? song.getArtistName() : metadata.artist);
                //songArtist.setText(metadata.artist + (metadata.album.equals("null") ? "" : (" - " + song.album)));
                songInfo.setText(
                        getString(R.string.title_album) + metadata.album + "\n" +
                                getString(R.string.title_duration) + ConvertUtils.millis2FitTimeSpan(Long.parseLong(metadata.duration), 4) + "\n" +
                                getString(R.string.title_bitrate) + Long.parseLong(metadata.bitrate) / 1024 + "Kbps\n" +
                                getString(R.string.title_mimetype) + metadata.mimetype + "\n" +
                                getString(R.string.file_size) + metadata.sizeByte + "\n" +
                                getString(R.string.title_path) + song.getSongPath() + "\n" +
                                getString(R.string.title_lyric) + song.getLyricPath()
                );
            } else {    //否则使用播放列表的数据
                songTitle.setText(song.getSongTitle());
                songArtist.setText(song.getArtistName());
                songInfo.setText(getString(R.string.title_album) + song.getAlbumName() + "\n" +
                        getString(R.string.title_path) + song.getSongPath() + "\n" +
                        getString(R.string.title_lyric) + song.getLyricPath());
            }
            //读取专辑图片
            Bitmap bitmap=MusicUtils.getAlbumImage(song.getSongPath());
            if(bitmap==null){
                albumImageView.setImageResource(R.drawable.ic_baseline_music_note_24);
            }else {
                albumImageView.setImageBitmap(bitmap);
            }
            initLyric(song);
        }
    }

    //显示歌词列表及模糊背景
    public void setIsLyricLayoutShow(boolean isLyric) {
        if (songLyricLayout != null) {
            if (!isLyric && lastLyricEnabled) {
                Animation animation = AnimationUtils.loadAnimation(getContext(), R.anim.gradually_hide);
                songLyricLayout.startAnimation(animation);
                PlayBarLyric.setImageDrawable(getContext().getDrawable(R.drawable.ic_baseline_subtitles_24));
                songLyricLayout.setVisibility(View.INVISIBLE);
            } else if (!lastLyricEnabled) {
                Animation animation = AnimationUtils.loadAnimation(getContext(), R.anim.gradually_show);
                songLyricLayout.setVisibility(View.VISIBLE);
                PlayBarLyric.setImageDrawable(getContext().getDrawable(R.drawable.ic_baseline_subtitles_green_24));
                songLyricLayout.startAnimation(animation);
            }
            lastLyricEnabled = !lastLyricEnabled;
        }
    }

    public void initLyric(Song song) {
        //刷新歌词
        lastLyricId = -1;
        lyric=new LyricUtils(song); //从歌词文件中读取歌词
        adapter = new LyricAdapter(getContext(), lyric, nowLyricMap); //构造LyricAdapter对象
        lyricList.setAdapter(adapter);  //将adapter与ListView绑定
        adapter.notifyDataSetChanged();
        lyric.setOnLyricLoadCallback(new LyricUtils.OnLyricLoadCallback() {
            @Override
            public void LyricLoadCompleted() {
                adapter.notifyDataSetChanged();
            }
        });
    }
}
