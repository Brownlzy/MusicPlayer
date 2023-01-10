package com.liux.musicplayer.utils;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.widget.Toast;

import com.liux.musicplayer.R;

public class ClipboardUtils {
    public static void copyText(Context context,String text) {
        ClipboardManager clipboardManager =
                (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        // When setting the clip board text.
        clipboardManager.setPrimaryClip(ClipData.newPlainText("", text));
        // Only show a toast for Android 12 and lower.
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2)
            Toast.makeText(context, context.getString(R.string.Copied) + text, Toast.LENGTH_SHORT).show();
    }

}
