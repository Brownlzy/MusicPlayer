package com.liux.musicplayer.ui.home;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.blankj.utilcode.util.ConvertUtils;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.imageview.ShapeableImageView;
import com.liux.musicplayer.ui.MainActivity;
import com.liux.musicplayer.R;
import com.liux.musicplayer.utils.MusicUtils;

public class HomeFragment extends Fragment {

    public MusicUtils.Lyric lyric;
    public ListView lyricList;
    private TextView songTitle;
    private TextView songArtist;
    private TextView songInfo;
    private ShapeableImageView albumImageView;
    private RelativeLayout songLyricLayout;
    private View mView;
    private LyricAdapter adapter;
    private boolean isSetLyricPosition = true;
    private final SparseBooleanArray nowLyricMap = new SparseBooleanArray();//用来存放高亮歌词的选中状态，true为选中,false为没有选中
    private int lastLyricId;
    private boolean lastLyricEnabled = false;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        mView = view;
        initViewCompat();
        //创建窗体完成，通知MainActivity来设置显示信息
        callMainActivityForInfo();
        return view;
    }

    private void initViewCompat() {
        //绑定歌词列表
        mView.findViewById(R.id.albumImageView).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                ((MainActivity) getActivity()).setIsLyric();
                return false;
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
        //设置歌词列表Item单击监听器
        lyricList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //Toast.makeText(getContext(), String.valueOf(position), Toast.LENGTH_SHORT).show();
                //定位播放进度至点击的歌词处
                ((MainActivity) getActivity()).getMusicService().setProgress(lyric.startMillionTime.get(position).intValue());
            }
        });
    }

    private void callMainActivityForInfo() {
        ((MainActivity) getActivity()).setChildFragment();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @SuppressLint("SetTextI18n")
    public void setMusicInfo(MusicUtils.Song song) {
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
            MusicUtils.Metadata metadata = MusicUtils.getMetadata(getContext(), song);
            if (metadata.isValid) { //如果metadata有效标志为真则使用metadata的数据
                songTitle.setText((metadata.title == null) ? song.title : metadata.title);
                songArtist.setText((metadata.artist == null) ? song.artist : metadata.artist);
                //songArtist.setText(metadata.artist + (metadata.album.equals("null") ? "" : (" - " + song.album)));
                songInfo.setText(
                        getString(R.string.title_album) + metadata.album + "\n" +
                                getString(R.string.title_duration) + ConvertUtils.millis2FitTimeSpan(Long.parseLong(metadata.duration), 4) + "\n" +
                                getString(R.string.title_bitrate) + Long.parseLong(metadata.bitrate) / 1024 + "Kbps\n" +
                                getString(R.string.title_mimetype) + metadata.mimetype + "\n" +
                                getString(R.string.file_size) + metadata.sizeByte + "\n" +
                                getString(R.string.title_path) + song.source_uri + "\n" +
                                getString(R.string.title_lyric) + song.lyric_uri
                );
            } else {    //否则使用播放列表的数据
                songTitle.setText(song.title);
                songArtist.setText(song.artist);
                songInfo.setText(getString(R.string.title_album) + song.album + "\n" +
                        getString(R.string.title_path) + song.source_uri + "\n" +
                        getString(R.string.title_lyric) + song.lyric_uri);
            }
            //读取专辑图片
            Bitmap bitmap = MusicUtils.getAlbumImage(requireContext(), song);
            if (bitmap == null) {   //获取图片失败，使用默认图片
                albumImageView.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_baseline_music_note_24));
            } else {    //成功
                albumImageView.setImageBitmap(bitmap);
            }
            initLyric(song);
        }
    }

    //显示歌词列表及模糊背景
    public void setIsLyricLayoutShow(boolean isLyric) {
        if (songLyricLayout != null && isLyric != lastLyricEnabled) {
            if (!isLyric) {
                Animation animation = AnimationUtils.loadAnimation(getContext(), R.anim.gradually_hide);
                songLyricLayout.startAnimation(animation);
                songLyricLayout.setVisibility(View.INVISIBLE);
            } else {
                Animation animation = AnimationUtils.loadAnimation(getContext(), R.anim.gradually_show);
                songLyricLayout.setVisibility(View.VISIBLE);
                songLyricLayout.startAnimation(animation);
            }
            lastLyricEnabled = !lastLyricEnabled;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        lastLyricId = -1;
    }

    //根据当前歌词位置设置歌词高亮居中
    public void setLyricPosition(int lyricPosition) {
        if (lyricPosition != lastLyricId) { //判断高亮歌词是否需要改变
            nowLyricMap.clear();    //清除之前数据
            nowLyricMap.put(lyricPosition, true);   //设置当前歌词为选中
            adapter.notifyDataSetChanged(); //通知adapter刷新列表数据
            if (isSetLyricPosition) {   //检查歌词居中标志位
                try {
                    //设置歌词居中
                    lyricList.smoothScrollToPositionFromTop(lyricPosition, lyricList.getWidth() / 2 - 40);
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
            }
            lastLyricId = lyricPosition;
        }
    }

    public void initLyric(MusicUtils.Song song) {
        //刷新歌词
        lyric = new MusicUtils.Lyric(Uri.parse(song.lyric_uri));    //从歌词文件中读取歌词
        adapter = new LyricAdapter(this, getContext(), lyric, nowLyricMap); //构造LyricAdapter对象
        lyricList.setAdapter(adapter);  //将adapter与ListView绑定
    }
}
