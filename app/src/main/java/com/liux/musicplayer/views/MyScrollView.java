package com.liux.musicplayer.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ScrollView;

import com.liux.musicplayer.utils.DisplayUtils;

public class MyScrollView extends ScrollView {

    public MyScrollView(Context context) {
        super(context);
    }

    public MyScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MyScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public MyScrollView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        float x = ev.getX();
        float y = ev.getY() + getScrollY();
        if (y < getWidth() - DisplayUtils.dip2px(getContext(), 20)
                && x > DisplayUtils.dip2px(getContext(), 25)
                && x < getWidth() - DisplayUtils.dip2px(getContext(), 25)) {//y是你透明区域的高度
            return false;//穿透点击
        }
        return super.onTouchEvent(ev);//ScrollView 的正常滚动
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return super.dispatchTouchEvent(ev);
    }
}

