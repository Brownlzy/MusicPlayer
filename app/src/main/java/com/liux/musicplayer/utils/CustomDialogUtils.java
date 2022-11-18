package com.liux.musicplayer.utils;

import android.content.Context;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.appcompat.app.AlertDialog;

import com.liux.musicplayer.R;
import com.liux.musicplayer.activities.MainActivity;
import com.liux.musicplayer.models.Song;

public class CustomDialogUtils {

    private static AlertDialog dialog;
    private static Song content;

    /**
     * @param activity                    Context
     * @param cancelableTouchOut          点击外部是否隐藏提示框
     * @param alertDialogBtnClickListener 点击监听
     */

    public static void showSongInfoEditDialog(MainActivity activity, Song song, boolean cancelableTouchOut, final AlertDialogBtnClickListener alertDialogBtnClickListener) {
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
        frameEditTextTitle.setText(song.getSongTitle());
        frameEditTextArtist.setText(song.getArtistName());
        frameEditTextAlbum.setText(song.getAlbumName());
        frameEditTextPath.setText(song.getSongPath());
        frameEditTextLyric.setText(song.getLyricPath());

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
                content.setmTitle( s.toString().trim());
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
                content.setmArtistName( s.toString().trim());
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
                content.setmAlbumName(s.toString().trim());
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
                content.setLyricPath(s.toString().trim());
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
        void clickPositive(Song song);

        void clickNegative();
    }
    public static EditText editText;
    public static void editTextDialog(Context context,
                                      String title,
                                      @DrawableRes int iconId,
                                      String strPos,
                                      String strNeg,
                                      String strNormal,
                                      DialogInterface.OnClickListener pos,
                                      DialogInterface.OnClickListener neg,
                                      DialogInterface.OnClickListener normal){
        View view = LayoutInflater.from(context).inflate(R.layout.oneline_edittext,null,false);
        editText = view.findViewById(R.id.editText);
        AlertDialog.Builder inputDialog = new AlertDialog.Builder(context);
        inputDialog.setTitle(title).setView(view);
        inputDialog.setCancelable(false);
        inputDialog.setIcon(iconId);
        if(pos!=null)
        inputDialog.setPositiveButton(strPos,pos);
        if(normal!=null)
        inputDialog.setNeutralButton(strNormal,normal);
        if(neg!=null)
        inputDialog.setNegativeButton(strNeg,neg);
        inputDialog.show();
    }

}
