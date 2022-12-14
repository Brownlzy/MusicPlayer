package com.liux.musicplayer.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.media.MediaMetadataCompat;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.MenuItem;
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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.blankj.utilcode.util.ConvertUtils;
import com.blankj.utilcode.util.FileUtils;
import com.google.android.material.imageview.ShapeableImageView;
import com.liux.musicplayer.R;
import com.liux.musicplayer.activities.MainActivity;
import com.liux.musicplayer.adapters.LyricAdapter;
import com.liux.musicplayer.media.MusicLibrary;
import com.liux.musicplayer.models.Song;
import com.liux.musicplayer.utils.CustomDialogUtils;
import com.liux.musicplayer.utils.LyricUtils;
import com.liux.musicplayer.utils.MusicUtils;
import com.liux.musicplayer.utils.SharedPrefs;
import com.liux.musicplayer.viewmodels.MyViewModel;

import java.util.List;

public class HomeFragment extends Fragment {

    public LyricUtils lyric;
    public ListView lyricList;
    private TextView songTitle;
    private TextView songArtist;
    private TextView songInfo;
    private TextView titleFileInfo;
    private ImageView playLyric;
    private ImageView btnMore;
    private ShapeableImageView albumImageView;
    private RelativeLayout songLyricLayout;
    private View mView;
    private LyricAdapter adapter;
    private LyricThread lyricThread;
    private boolean isSetLyricPosition = true;
    private final SparseBooleanArray nowLyricMap = new SparseBooleanArray();//??????????????????????????????????????????true?????????,false???????????????
    private int lastLyricId;
    private boolean lastLyricEnabled = false;
    private MyViewModel myViewModel;
    private Song mSong;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        mView = view;
        myViewModel = new ViewModelProvider(MainActivity.mainActivity).get(MyViewModel.class);
        initViewCompat();
        initObserver();
        return view;
    }

    @SuppressLint("SetTextI18n")
    public void initMusicInfo(MediaMetadataCompat mediaMetadata) {
        if (mediaMetadata == null) {
            return;
        }
        String TITLE = mediaMetadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE);
        String ARTIST = mediaMetadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST);
        String ALBUM = mediaMetadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM);
        String MEDIA_ID = mediaMetadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID);
        String MEDIA_URI = mediaMetadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI);
        String LYRIC_URI =mediaMetadata.getBundle().getString("LYRIC_URI","null");
        int DURATION = (int) mediaMetadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION);
        Song song = new Song(TITLE, DURATION, ARTIST,ALBUM, MEDIA_ID,MEDIA_URI,LYRIC_URI);

        MusicUtils.Metadata metadata = MusicUtils.getMetadata(song.getSongPath());
        if (metadata.isValid) { //??????metadata???????????????????????????metadata?????????
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
        } else {    //?????????????????????????????????
            songTitle.setText(song.getSongTitle());
            songArtist.setText(song.getArtistName());
            songInfo.setText(getString(R.string.title_album) + song.getAlbumName() + "\n" +
                    getString(R.string.title_path) + song.getSongPath() + "\n" +
                    getString(R.string.title_lyric) + song.getLyricPath());
        }
        Bitmap bitmap=MusicUtils.getAlbumImage(song.getSongPath());
        if (bitmap == null) {   //???????????????????????????????????????
            albumImageView.setImageResource(R.drawable.ic_baseline_music_note_24);
        } else {    //??????
            albumImageView.setImageBitmap(bitmap);
        }
    }

    private void initObserver() {
        myViewModel.getNowPlaying().observe(getViewLifecycleOwner(),new Observer<Song>() {
            @Override
            public void onChanged(Song song) {
                setMusicInfo(song);
            }
        });
        myViewModel.getIsPlaying().observe(getViewLifecycleOwner(),new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean isPlaying) {
                if(isPlaying)
                    startLyric();
                else
                    stopLyric();
            }
        });
        Log.e("HomeFragment","intedObserved");
    }

    private void initViewCompat() {
        songTitle = mView.findViewById(R.id.home_song_title);
        songArtist = mView.findViewById(R.id.home_song_artist);
        songInfo = mView.findViewById(R.id.home_song_info);
        albumImageView = mView.findViewById(R.id.albumImageView);
        songLyricLayout = mView.findViewById(R.id.songLyricLayout);
        lyricList = mView.findViewById(R.id.lyricList);
        //??????????????????
        mView.findViewById(R.id.albumImageView).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                setIsLyric();
                return false;
            }
        });
        playLyric = mView.findViewById(R.id.playLyric);
        btnMore = mView.findViewById(R.id.btnMore);
        playLyric.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setIsLyric();
            }
        });
        btnMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popMore();
            }
        });
        lyricList = mView.findViewById(R.id.lyricList);
        lyricList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        lyricList.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                // OnScrollListener.SCROLL_STATE_FLING; //????????????????????????
                // OnScrollListener.SCROLL_STATE_IDLE; //??????????????????
                // OnScrollListener.SCROLL_STATE_TOUCH_SCROLL;// ??????????????????
                if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
                    //?????????????????????????????????????????????
                    isSetLyricPosition = false;
                } else if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
                    //???????????????????????????????????????????????????
                    isSetLyricPosition = true;
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            }
        });
        //??????????????????Item???????????????
        lyricList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //Toast.makeText(getContext(), String.valueOf(position), Toast.LENGTH_SHORT).show();
                //???????????????????????????????????????
                //myViewModel.getMusicService().setProgress(lyric.startMillionTime.get(position).intValue());
                myViewModel.getmMediaController().getTransportControls().seekTo(lyric.startMillionTime.get(position));
            }
        });
    }

    private void popMore() {
        PopupMenu popup = new PopupMenu(requireContext(), btnMore);
        popup.getMenuInflater().inflate(R.menu.home_info_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.item_menu_edit:
                        CustomDialogUtils.showSongInfoEditDialog(MainActivity.mainActivity, mSong, false,
                                new CustomDialogUtils.AlertDialogBtnClickListener() {
                                    @Override
                                    public void clickPositive(Song song) {
                                        MusicLibrary.editSongInfo(song);
                                        myViewModel.getmMediaController().addQueueItem(MusicLibrary.getMediaItemDescription(song),-2);
                                        setMusicInfo(song);
                                    }

                                    @Override
                                    public void clickNegative() {

                                    }
                                });
                        break;
                    case R.id.item_menu_add_to_list:
                        DialogInterface.OnClickListener pos=new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                MusicLibrary.addMusicToList(mSong.getSongPath(),CustomDialogUtils.chosenOne);
                                Toast.makeText(getContext(), "?????????\""+mSong.getSongTitle()+"\"???\""+CustomDialogUtils.chosenOne+"\"", Toast.LENGTH_SHORT).show();
                            }
                        };
                        DialogInterface.OnClickListener neg=new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        };
                        List<String> songListName=MusicLibrary.getAllSongListName();
                        songListName.removeIf(s -> s.equals("allSongList")||s.equals("webAllSongList"));
                        String[] sln=songListName.toArray(new String[songListName.size()]);
                        CustomDialogUtils.chooseDialog(
                                getContext(),
                                getString(R.string.item_menu_add_to_list),
                                sln,
                                getString(R.string.confirm),
                                getString(R.string.cancel),
                                pos,
                                neg);
                        break;
                }
                return true;
            }
        });
        popup.show();
    }

    public void setIsLyric() {
        setIsLyricLayoutShow(!lastLyricEnabled);
        //myViewModel.setFragmentLyricState(!myViewModel.getFragmentLyricState());
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @SuppressLint("SetTextI18n")
    public void setMusicInfo(Song song) {
        if (mView != null) {
            //?????????????????????????????????
            songTitle = mView.findViewById(R.id.home_song_title);
            songArtist = mView.findViewById(R.id.home_song_artist);
            songInfo = mView.findViewById(R.id.home_song_info);
            albumImageView = mView.findViewById(R.id.albumImageView);
            songLyricLayout = mView.findViewById(R.id.songLyricLayout);
            lyricList = mView.findViewById(R.id.lyricList);
            titleFileInfo=mView.findViewById(R.id.home_song_info_title);
            lyricList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
            //??????????????????metadata??????
            MusicUtils.Metadata metadata = MusicUtils.getMetadata(song.getSongPath());
            if (SharedPrefs.isUseMetaData()&&metadata.isValid) { //??????metadata???????????????????????????metadata?????????
                titleFileInfo.setText(R.string.fileMetaDataInfo);
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
            } else {    //?????????????????????????????????
                titleFileInfo.setText(R.string.fileInfo);
                songTitle.setText(song.getSongTitle());
                songArtist.setText(song.getArtistName());
                songInfo.setText(getString(R.string.title_album) + song.getAlbumName() + "\n" +
                        getString(R.string.title_duration) + ConvertUtils.millis2FitTimeSpan(song.getSongDuration(), 4) + "\n" +
                        getString(R.string.title_bitrate) + (long)(((double)song.getSize()/((double)song.getSongDuration()/8000)) / 1024) + "Kbps\n" +
                        getString(R.string.title_mimetype) + FileUtils.getFileExtension(song.getSongPath()) + "\n" +
                        getString(R.string.file_size) + ConvertUtils.byte2FitMemorySize(song.getSize())+ "\n" +
                        getString(R.string.title_path) + song.getSongPath() + "\n" +
                        getString(R.string.title_lyric) + song.getLyricPath());
            }
            //??????????????????
            //Bitmap bitmap = myViewModel.getMusicService().getAlbumImage();
            //if (bitmap == null) {   //???????????????????????????????????????
            albumImageView.setImageBitmap(myViewModel.getNowAlbumArt());
            //} else {    //??????
            //    albumImageView.setImageBitmap(bitmap);
            //}
            initLyric(song);
        }
        MainActivity.mainActivity.HideSplash(2);
        mSong=song;
    }

    //?????????????????????????????????
    public void setIsLyricLayoutShow(boolean isLyric) {
        if (songLyricLayout != null) {
            if (!isLyric&& lastLyricEnabled) {
                Animation animation = AnimationUtils.loadAnimation(getContext(), R.anim.gradually_hide);
                songLyricLayout.startAnimation(animation);
                playLyric.setImageDrawable(requireContext().getDrawable(R.drawable.ic_baseline_subtitles_24));
                songLyricLayout.setVisibility(View.INVISIBLE);
            } else if(!lastLyricEnabled){
                Animation animation = AnimationUtils.loadAnimation(getContext(), R.anim.gradually_show);
                songLyricLayout.setVisibility(View.VISIBLE);
                playLyric.setImageDrawable(requireContext().getDrawable(R.drawable.ic_baseline_subtitles_green_24));
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
        //setIsLyricLayoutShow(lastLyricEnabled);
    }

    //????????????????????????????????????????????????
    public void setLyricPosition(int lyricPosition) {
        if (lyricPosition != lastLyricId) { //????????????????????????????????????
            nowLyricMap.clear();    //??????????????????
            nowLyricMap.put(lyricPosition, true);   //???????????????????????????
            if (adapter != null)
                adapter.notifyDataSetChanged(); //??????adapter??????????????????
            if (isSetLyricPosition) {   //???????????????????????????
                try {
                    //??????????????????
                    lyricList.smoothScrollToPositionFromTop(lyricPosition, lyricList.getWidth() / 2 - 40);
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
            }
            lastLyricId = lyricPosition;
        }
    }

    public void initLyric(Song song) {
        //????????????
        lastLyricId = -1;
        //lyric = myViewModel.getMusicService().getLyric();    //??????????????????????????????
        lyric=myViewModel.getLyric();
        adapter = new LyricAdapter(getContext(), lyric, nowLyricMap); //??????LyricAdapter??????
        lyricList.setAdapter(adapter);  //???adapter???ListView??????
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

        //????????????????????????????????????
        void pauseThread() {
            pause = true;
        }

        boolean isPaused() {
            return pause;
        }

        //?????????????????????????????????????????????
        void resumeThread() {
            pause = false;
            synchronized (lock) {
                lock.notifyAll();
            }
        }

        //??????????????????????????????run??????????????????????????????????????????????????????????????????
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
                // ?????????????????????????????????
                while (pause) {
                    onPause();
                }
                try {
                    if (myViewModel.getLyric().isCompleted) {
                        int currentLyricId = lyric.getNowLyric((myViewModel.getmMediaController().getPlaybackState().getPosition()));
                        if (currentLyricId >= 0) {
                            Message msg = new Message();
                            msg.what = 100;  //?????????????????????
                            msg.obj = currentLyricId; //???????????????????????????  Object String ??? int
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
