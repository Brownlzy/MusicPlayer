package com.liux.musicplayer.utils;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.liux.musicplayer.R;
import com.liux.musicplayer.activities.MainActivity;

public class CustomDialogUtils {

    private static AlertDialog dialog;
    private static MusicUtils.Song content;

    /**
     * @param activity                    Context
     * @param cancelableTouchOut          点击外部是否隐藏提示框
     * @param alertDialogBtnClickListener 点击监听
     */

    public static void showSongInfoEditDialog(MainActivity activity, MusicUtils.Song song, boolean cancelableTouchOut, final AlertDialogBtnClickListener alertDialogBtnClickListener) {
        content = song;
        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_song_info_edit, null);
        LinearLayout frame = (LinearLayout) view.findViewById(R.id.edit_info);
        EditText frameEditTextTitle = (EditText) view.findViewById(R.id.newTitle);
        EditText frameEditTextArtist = (EditText) view.findViewById(R.id.newArtist);
        EditText frameEditTextAlbum = (EditText) view.findViewById(R.id.newAlbum);
        EditText frameEditTextPath = (EditText) view.findViewById(R.id.oldPath);
        EditText frameEditTextLyric = (EditText) view.findViewById(R.id.newLyric);
        TextView frameConfirm = (TextView) view.findViewById(R.id.dialog_button_confirm);
        TextView frameCancel = (TextView) view.findViewById(R.id.dialog_button_cancel);
        frameEditTextTitle.setText(song.title);
        frameEditTextArtist.setText(song.artist);
        frameEditTextAlbum.setText(song.album);
        frameEditTextPath.setText(song.source_uri);
        frameEditTextLyric.setText(song.lyric_uri);

        if (activity.getApplicationContext().getResources().getConfiguration().uiMode == 0x21) {
            frame.setBackgroundResource(R.drawable.popup_full_dark);
        } else {
            frame.setBackgroundResource(R.drawable.popup_full_bright);
        }

        frameEditTextTitle.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                content.title = s.toString().trim();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        frameEditTextArtist.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                content.artist = s.toString().trim();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        frameEditTextAlbum.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                content.album = s.toString().trim();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        frameEditTextLyric.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                content.lyric_uri = s.toString().trim();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        frameConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialogBtnClickListener.clickPositive(content);
                dialog.dismiss();
            }
        });

        frameCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialogBtnClickListener.clickNegative();
                dialog.dismiss();
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setView(view);
        dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);//去掉圆角背景背后的棱角
        dialog.setCanceledOnTouchOutside(cancelableTouchOut);   //失去焦点dismiss
        dialog.show();

    }

    public interface AlertDialogBtnClickListener {
        void clickPositive(MusicUtils.Song song);

        void clickNegative();
    }

}
