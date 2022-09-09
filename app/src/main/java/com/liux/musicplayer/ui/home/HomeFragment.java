package com.liux.musicplayer.ui.home;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.imageview.ShapeableImageView;
import com.liux.musicplayer.MainActivity;
import com.liux.musicplayer.MusicPlayer;
import com.liux.musicplayer.R;
import com.liux.musicplayer.databinding.FragmentHomeBinding;

import java.io.IOException;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private TextView songTitle;
    private TextView songArtist;
    private TextView songInfo;
    private ShapeableImageView albumImageView;
    private RelativeLayout songLyricLayout;
    private View mView;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        mView = view;
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
    public void setMusicInfo(MusicPlayer.Song song, ShapeableImageView playBarPic) {
        if (mView != null) {
            songTitle = mView.findViewById(R.id.home_song_title);
            songArtist = mView.findViewById(R.id.home_song_artist);
            songInfo = mView.findViewById(R.id.home_song_info);
            albumImageView = mView.findViewById(R.id.albumImageView);
            songLyricLayout = mView.findViewById(R.id.songLyricLayout);
            songTitle.setText(song.title);
            songArtist.setText(song.artist + (song.album.equals("") ? "" : (" - " + song.album)));
            songInfo.setText(getString(R.string.title_album) + song.album + "\n" +
                    getString(R.string.title_filename) + song.filename + "\n" +
                    getString(R.string.title_path) + song.source_uri + "\n" +
                    getString(R.string.title_lyric) + song.lyric_uri);
            try {
                MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
                mediaMetadataRetriever.setDataSource(song.source_uri.replace("file:///storage/emulated/0", "/sdcard"));
                //获取专辑图片
                byte[] cover = mediaMetadataRetriever.getEmbeddedPicture();
                Bitmap bitmap = BitmapFactory.decodeByteArray(cover, 0, cover.length);
                albumImageView.setImageBitmap(bitmap);
                playBarPic.setImageBitmap(bitmap);
            } catch (IllegalArgumentException e) {
                //文件路径错误或无权限
                albumImageView.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.ic_baseline_music_note_24));
                playBarPic.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.ic_baseline_music_note_24));
                Toast.makeText(getContext(), "专辑图片读取失败", Toast.LENGTH_SHORT).show();
            } catch (NullPointerException e) {
                //文件本身无专辑图片
                albumImageView.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.ic_baseline_music_note_24));
                playBarPic.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.ic_baseline_music_note_24));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void setIsLyric(boolean isLyric) {
        if (songLyricLayout != null) {
            if (!isLyric) {
                songLyricLayout.setVisibility(View.GONE);
            } else {
                songLyricLayout.setVisibility(View.VISIBLE);
            }
        }
    }
}
