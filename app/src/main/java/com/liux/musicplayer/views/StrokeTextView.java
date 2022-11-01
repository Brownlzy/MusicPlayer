package com.liux.musicplayer.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

 /**
  * 带有描边的Textview，防止文字颜色与背景相近导致难以区分
  * @author         Brownlzy
  */
public class StrokeTextView extends AppCompatTextView {
    private AppCompatTextView borderText;///用于描边的TextView
    TextPaint tp1; //borderText的Paint

    private int mStrokeColor = Color.DKGRAY;
    private int strokeWidth = 2;
//三个构造函数，需要同时初始化得到一个参数完全相同的Textview（borderText）
    public StrokeTextView(Context context) {
        super(context);
        borderText = new AppCompatTextView(context);
        init();
    }

    public StrokeTextView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        borderText = new AppCompatTextView(context, attrs);
        init();
    }

    public StrokeTextView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        borderText = new AppCompatTextView(context, attrs, defStyleAttr);
        init();
    }
 /**
  * 初始化描边颜色和其他信息
  */
    public void init() {
        borderText.setTextColor(this.mStrokeColor);  //设置描边颜色
        borderText.setGravity(getGravity());
        tp1 = borderText.getPaint();
        tp1.setStrokeWidth(this.strokeWidth); //设置描边宽度
        tp1.setStyle(Paint.Style.FILL_AND_STROKE); //设置画笔样式为描边
        tp1.setStrokeJoin(Paint.Join.ROUND); //连接方式为圆角
    }

    //设置描边的颜色和宽度
    public void setStroke(int color, int strokeWidth) {
        tp1.setStrokeWidth(strokeWidth);
        borderText.setTextColor(color);  //设置描边颜色
    }
//以下几个重写方法保证了在对上层TextView修改的同时同步修改borderText
    @Override
    public void setLineSpacing(float add, float mult) {
        super.setLineSpacing(add, mult);
        borderText.setLineSpacing(add, mult);
    }

    @Override
    public void setMaxWidth(int maxPixels) {
        super.setMaxWidth(maxPixels);
        borderText.setMaxWidth(maxPixels);
    }

    public void setLayoutParams(ViewGroup.LayoutParams params) {
        super.setLayoutParams(params);
        borderText.setLayoutParams(params);
    }

    @Override
    public void setGravity(int gravity) {
        super.setGravity(gravity);
        borderText.setGravity(gravity);
    }

    @Override
    public void setPadding(int left, int top, int right, int bottom) {
        super.setPadding(left, top, right, bottom);
        borderText.setPadding(left, top, right, bottom);
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        borderText.measure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    public void setTextSize(int unit, float size) {
        super.setTextSize(unit, size);
        borderText.setTextSize(unit, size);
    }

    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        borderText.layout(left, top, right, bottom);
    }

    protected void onDraw(Canvas canvas) {
        borderText.draw(canvas);
        super.onDraw(canvas);
    }

    @Override
    protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter);
        if (borderText != null) {
            CharSequence tt = borderText.getText();
            if (tt == null || !tt.equals(this.getText())) {
                borderText.setText(getText());
                this.postInvalidate();
            }
        }
    }

}