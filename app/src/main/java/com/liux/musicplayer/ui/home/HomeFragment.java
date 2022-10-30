package com.liux.musicplayer.ui.home;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
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
import com.liux.musicplayer.utils.LyricUtils;
import com.liux.musicplayer.utils.MusicUtils;

public class HomeFragment extends Fragment {

    public LyricUtils lyric;
    public ListView lyricList;
    private TextView songTitle;
    private TextView songArtist;
    private TextView songInfo;
    private ShapeableImageView albumImageView;
    private RelativeLayout songLyricLayout;
    private View mView;
    private LyricAdapter adapter;
    private LyricThread lyricThread;
    private boolean isSetLyricPosition = true;
    private final SparseBooleanArray nowLyricMap = new SparseBooleanArray();//用来存放高亮歌词的选中状态，true为选中,false为没有选中
    private int lastLyricId;
    private boolean lastLyricEnabled = false;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
    }

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

    /**
     * 显示歌曲信息
     * @param song 歌曲的结构体
     * @return void
     */
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
            MusicUtils.Metadata metadata = ((MainActivity) requireActivity()).getMusicService().getMetadata();
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
            Bitmap bitmap = ((MainActivity) requireActivity()).getMusicService().getAlbumImage();
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
//            当歌词区域不显示歌词时，将显示明显的专辑封面
            if (!isLyric) {
                Animation animation = AnimationUtils.loadAnimation(getContext(), R.anim.gradually_hide);
                songLyricLayout.startAnimation(animation);
                songLyricLayout.setVisibility(View.INVISIBLE);
//                当歌词区域显示歌词时，将专辑封面虚化处理
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
        stopLyric();
    }

    @Override
    public void onResume() {
        super.onResume();
        //lastLyricId = -1;
        startLyric();
    }

    //根据当前歌词位置设置歌词高亮居中
    public void setLyricPosition(int lyricPosition) {
        if (lyricPosition != lastLyricId) { //判断高亮歌词是否需要改变
            nowLyricMap.clear();    //清除之前数据
            nowLyricMap.put(lyricPosition, true);   //设置当前歌词为选中
            if (adapter != null)
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
        lastLyricId = -1;
        lyric = ((MainActivity) getActivity()).getMusicService().getLyric();    //从歌词文件中读取歌词
        HomeFragment homeFragment = this;
        adapter = new LyricAdapter(homeFragment, getContext(), lyric, nowLyricMap); //构造LyricAdapter对象
        lyricList.setAdapter(adapter);  //将adapter与ListView绑定
        adapter.notifyDataSetChanged();
        lyric.setOnLyricLoadCallback(new LyricUtils.OnLyricLoadCallback() {
            @Override
            public void LyricLoadCompleted() {
                adapter.notifyDataSetChanged();
            }
        });
    }

    private class LyricThread extends Thread {
        private final Object lock = new Object();
        private boolean pause = false;

        //调用这个方法实现暂停线程
        void pauseThread() {
            pause = true;
        }

        boolean isPaused() {
            return pause;
        }

        //调用这个方法实现恢复线程的运行
        void resumeThread() {
            pause = false;
            synchronized (lock) {
                lock.notifyAll();
            }
        }

        //注意：这个方法只能在run方法里调用，不然会阻塞主线程，导致页面无响应
        void onPause() {
            synchronized (lock) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void run() {
            super.run();
            int index = 0;
            while (true) {
                // 让线程处于暂停等待状态
                while (pause) {
                    onPause();
                }
                try {
                    if (((MainActivity) getActivity()).getMusicService().isPlaying() && ((MainActivity) getActivity()).getMusicService().getLyric().isCompleted) {
                        int currentLyricId = lyric.getNowLyric(((MainActivity) getActivity()).getMusicService().getCurrentPosition());
                        if (currentLyricId >= 0) {
                            Message msg = new Message();
                            msg.what = 100;  //消息发送的标志
                            msg.obj = currentLyricId; //消息发送的内容如：  Object String 类 int
                            LyricHandler.sendMessage(msg);
                        }
                    }
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (NullPointerException e) {
                    stopLyric();
                }
            }
        }
    }

    public void startLyric() {
        if (lyricThread == null) {
            lyricThread = new LyricThread();
            lyricThread.start();
        } else if (lyricThread.isPaused()) {
            lyricThread.resumeThread();
        }
    }

    public void stopLyric() {
        if (lyricThread != null && !lyricThread.isPaused())
            lyricThread.pauseThread();
    }

    private final Handler LyricHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 100) {
                setLyricPosition((int) msg.obj);
            }
        }
    };


}
