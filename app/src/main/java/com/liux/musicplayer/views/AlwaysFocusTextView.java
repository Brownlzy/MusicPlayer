package com.liux.musicplayer.views;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;
 /**
  * 始终有焦点的Textview，实现即使当前没有焦点始终跑马灯效果
  * @author        Brownlzy
  */
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
//关键点：永远处于焦点状态
    @Override
    public boolean isFocused() {
        return true;
    }
}
