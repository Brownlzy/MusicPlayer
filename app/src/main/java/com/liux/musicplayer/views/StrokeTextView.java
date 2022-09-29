package com.liux.musicplayer.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatTextView;

import com.liux.musicplayer.R;

/**
 * @Author : XiaoXred
 * @Time : On 2020/10/22 15:59
 * @Description : StrokeTextView  文字内容有描边的TextView
 */

public class StrokeTextView extends AppCompatTextView {
    private TextView backGroundText = null;//用于描边的TextView

    public StrokeTextView(Context context) {
        this(context, null);
    }

    public StrokeTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StrokeTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        backGroundText = new TextView(context, attrs, defStyle);
    }

    @Override
    public void setLayoutParams(ViewGroup.LayoutParams params) {
//同步布局参数
        backGroundText.setLayoutParams(params);
        super.setLayoutParams(params);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        CharSequence tt = backGroundText.getText();
        //两个TextView上的文字必须一致
        if (tt == null || !tt.equals(this.getText())) {
            backGroundText.setText(getText());
            this.postInvalidate();
        }
        backGroundText.measure(widthMeasureSpec, heightMeasureSpec);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        backGroundText.layout(left, top, right, bottom);
        super.onLayout(changed, left, top, right, bottom);
    }

    @Override
    public void setTextSize(int unit, float size) {
        backGroundText.setTextSize(unit, size);
        super.setTextSize(unit, size);
    }

    @Override
    protected void onDraw(Canvas canvas) {
//其他地方，backGroundText和super的先后顺序影响不会很大，但是此处必须要先绘制backGroundText，
        init();
        backGroundText.draw(canvas);
        super.onDraw(canvas);
    }

    public void init() {
        TextPaint tp1 = backGroundText.getPaint();
        //设置描边宽度
        tp1.setStrokeWidth(2);
        //背景描边并填充全部
        tp1.setStyle(Paint.Style.FILL_AND_STROKE);
        //设置描边颜色
        backGroundText.setTextColor(Color.DKGRAY);
        //将背景的文字对齐方式做同步
        backGroundText.setGravity(getGravity());
    }
}