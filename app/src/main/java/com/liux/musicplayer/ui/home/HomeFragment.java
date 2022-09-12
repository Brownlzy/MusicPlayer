package com.liux.musicplayer.ui.home;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.liux.musicplayer.MainActivity;
import com.liux.musicplayer.MusicPlayer;
import com.liux.musicplayer.R;
import com.liux.musicplayer.databinding.FragmentHomeBinding;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private TextView songTitle;
    private TextView songArtist;
    private TextView songInfo;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        binding = FragmentHomeBinding.inflate(inflater, container, false);

        songTitle = view.findViewById(R.id.home_song_title);
        songArtist = view.findViewById(R.id.home_song_artist);
        songInfo = view.findViewById(R.id.home_song_info);
        //告诉MainActivity准备好了
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
    public void setMusicInfo(MusicPlayer.Song song) {
        if (songTitle != null) {
            songTitle.setText(song.title);
            songArtist.setText(song.artist + (song.album.equals("") ? "" : (" - " + song.album)));
            songInfo.setText(getString(R.string.title_album) + song.album + "\n" +
                    getString(R.string.title_filename) + song.filename + "\n" +
                    getString(R.string.title_path) + song.source_uri + "\n" +
                    getString(R.string.title_lyric) + song.lyric_uri);
        }
    }
}