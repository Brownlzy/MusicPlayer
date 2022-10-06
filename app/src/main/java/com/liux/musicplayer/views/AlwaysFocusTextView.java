package com.liux.musicplayer.views;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

public class AlwaysFocusTextView extends AppCompatTextView {

    public AlwaysFocusTextView(Context context) {
        super(context);
    }

    public AlwaysFocusTextView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public AlwaysFocusTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean isFocused() {
        return true;
    }
}
