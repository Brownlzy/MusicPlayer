package com.liux.musicplayer.ui.home;

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

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        binding = FragmentHomeBinding.inflate(inflater, container, false);

        songTitle = view.findViewById(R.id.home_song_title);
        songArtist = view.findViewById(R.id.home_song_artist);

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

    public void setMusicInfo(MusicPlayer.Song song) {
        songTitle.setText(song.title);
        songArtist.setText(song.artist +
                (song.album.equals("") ? "" : (" - " + song.album)));
    }
}