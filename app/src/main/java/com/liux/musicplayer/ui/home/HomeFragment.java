package com.liux.musicplayer.ui.home;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.blankj.utilcode.util.ConvertUtils;
import com.google.android.material.imageview.ShapeableImageView;
import com.liux.musicplayer.MainActivity;
import com.liux.musicplayer.MusicPlayer;
import com.liux.musicplayer.R;
import com.liux.musicplayer.databinding.FragmentHomeBinding;
import com.liux.musicplayer.util.MusicUtils;

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
            MusicUtils.Metadata metadata = MusicUtils.getMetadata(getContext(), song);
            songTitle.setText(metadata.title);
            songArtist.setText(metadata.artist);
            //songArtist.setText(metadata.artist + (metadata.album.equals("null") ? "" : (" - " + song.album)));
            songInfo.setText(getString(R.string.title_album) + metadata.album + "\n" +
                    getString(R.string.title_duration) + ConvertUtils.millis2FitTimeSpan(Long.parseLong(metadata.duration), 4) + "\n" +
                    getString(R.string.title_bitrate) + Long.parseLong(metadata.bitrate) / 1024 + "Kbps\n" +
                    getString(R.string.title_filename) + song.filename + "\n" +
                    getString(R.string.title_mimetype) + metadata.mimetype + "\n" +
                    getString(R.string.title_path) + song.source_uri + "\n" +
                    getString(R.string.title_lyric) + song.lyric_uri);
            /*songTitle.setText(song.title);
            songArtist.setText(song.artist + (song.album.equals("") ? "" : (" - " + song.album)));
            songInfo.setText(getString(R.string.title_album) + song.album + "\n" +
                    getString(R.string.title_filename) + song.filename + "\n" +
                    getString(R.string.title_path) + song.source_uri + "\n" +
                    getString(R.string.title_lyric) + song.lyric_uri);*/
            Bitmap bitmap = MusicUtils.getAlbumImage(getContext(), song);
            albumImageView.setImageBitmap(bitmap);
            playBarPic.setImageBitmap(bitmap);
        }
    }

    public void setIsLyric(boolean isLyric) {
        if (songLyricLayout != null) {
            if (!isLyric) {
                songLyricLayout.setVisibility(View.INVISIBLE);
            } else {
                songLyricLayout.setVisibility(View.VISIBLE);
            }
        }
    }
}
