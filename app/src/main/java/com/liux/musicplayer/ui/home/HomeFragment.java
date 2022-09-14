package com.liux.musicplayer.ui.home;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.blankj.utilcode.util.ConvertUtils;
import com.google.android.material.imageview.ShapeableImageView;
import com.liux.musicplayer.MainActivity;
import com.liux.musicplayer.R;
import com.liux.musicplayer.databinding.FragmentHomeBinding;
import com.liux.musicplayer.util.MusicUtils;

public class HomeFragment extends Fragment {

    public MusicUtils.Lyric lyric;
    public ListView lyricList;
    private FragmentHomeBinding binding;
    private TextView songTitle;
    private TextView songArtist;
    private TextView songInfo;
    private ShapeableImageView albumImageView;
    private RelativeLayout songLyricLayout;
    private View mView;
    private LyricAdapter adapter;
    private int listPosition = -1;
    private int listPositionY = 0;
    private boolean isSetLyricPosition = true;
    private final SparseBooleanArray nowLyricMap = new SparseBooleanArray();//用来存放高亮歌词的选中状态，true为选中,false为没有选中
    private int lastLyricId;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        mView = view;
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
                ((MainActivity) getActivity()).getMusicPlayer().getMediaPlayer().seekTo(lyric.startMillionTime.get(position).intValue());
            }
        });
        //创建窗体完成，通知MainActivity来设置显示信息
        callMainActivityForInfo();
        return view;
    }

    private void callMainActivityForInfo() {
        ((MainActivity) getActivity()).initHomeFragment();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @SuppressLint("SetTextI18n")
    public void setMusicInfo(MusicUtils.Song song, ShapeableImageView playBarPic) {
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
                songInfo.setText(getString(R.string.title_album) + metadata.album + "\n" +
                        getString(R.string.title_duration) + ConvertUtils.millis2FitTimeSpan(Long.parseLong(metadata.duration), 4) + "\n" +
                        getString(R.string.title_bitrate) + Long.parseLong(metadata.bitrate) / 1024 + "Kbps\n" +
                        getString(R.string.title_mimetype) + metadata.mimetype + "\n" +
                        getString(R.string.title_path) + song.source_uri + "\n" +
                        getString(R.string.title_lyric) + song.lyric_uri);
            } else {    //否则使用播放列表的数据
                songTitle.setText(song.title);
                songArtist.setText(song.artist);
                songInfo.setText(getString(R.string.title_album) + song.album + "\n" +
                        getString(R.string.title_path) + song.source_uri + "\n" +
                        getString(R.string.title_lyric) + song.lyric_uri);
            }
            //读取专辑图片
            Bitmap bitmap = MusicUtils.getAlbumImage(getContext(), song);
            if (bitmap == null) {   //获取图片失败，使用默认图片
                albumImageView.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.ic_baseline_music_note_24));
                playBarPic.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.ic_baseline_music_note_24));
            } else {    //成功
                albumImageView.setImageBitmap(bitmap);
                playBarPic.setImageBitmap(bitmap);
            }
            //刷新歌词
            lyric = new MusicUtils.Lyric(Uri.parse(song.lyric_uri));    //从歌词文件中读取歌词
            adapter = new LyricAdapter(this, getContext(), lyric, nowLyricMap); //构造LyricAdapter对象
            lyricList.setAdapter(adapter);  //将adapter与ListView绑定
        }
    }

    //显示歌词列表及模糊背景
    public void setIsLyricLayoutShow(boolean isLyric) {
        if (songLyricLayout != null) {
            if (!isLyric) {
                songLyricLayout.setVisibility(View.INVISIBLE);
            } else {
                songLyricLayout.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        //记住onPause时歌词列表位置以便恢复
        if (lyricList != null) {
            listPosition = lyricList.getFirstVisiblePosition();
            if (lyricList.getChildAt(0) != null)
                listPositionY = lyricList.getChildAt(0).getTop();
            Log.e("lyricList", String.valueOf(listPosition));
            Log.e("lyricList", String.valueOf(listPositionY));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (listPosition != -1)
            lyricList.setSelectionFromTop(listPosition, listPositionY);
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
                    lyricList.setSelectionFromTop(lyricPosition, lyricList.getWidth() / 2 - 20);
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
            }
            lastLyricId = lyricPosition;
        }
    }

    public void initLyric() {

    }
}
