package com.liux.musicplayer.ui.home;

import static androidx.annotation.Dimension.SP;

import android.content.Context;
import android.graphics.Color;
import android.util.SparseBooleanArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.liux.musicplayer.R;
import com.liux.musicplayer.utils.LyricUtils;
import com.liux.musicplayer.utils.MusicUtils;

public class LyricAdapter extends BaseAdapter {

    LyricUtils lyricData;
    private final Context mContext;
    ViewHolder holder;
    private final HomeFragment mHomeFragment;
    private final SparseBooleanArray mNowLyricMap;

    public LyricAdapter(HomeFragment homeFragment, Context context, LyricUtils lyric, SparseBooleanArray nowLyricMap) {
        lyricData = lyric;
        mContext = context;
        mHomeFragment = homeFragment;
        mNowLyricMap = nowLyricMap;
    }

    @Override
    public int getCount() {
        return lyricData.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = View.inflate(mContext, R.layout.item_lyric, null);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        //绑定对象
        holder.lyricText = convertView.findViewById(R.id.item_lyric_text);
        holder.lyricText.setText(lyricData.lyricList.get(position));
        if (mContext.getApplicationContext().getResources().getConfiguration().uiMode == 0x21) {    //深色模式
            if (mNowLyricMap.get(position, false)) {  //被选中则设置高亮颜色
                holder.lyricText.setTextColor(Color.CYAN);
                holder.lyricText.setTextSize(SP, 16);
            } else {
                holder.lyricText.setTextColor(Color.WHITE);
                holder.lyricText.setTextSize(SP, 14);
            }
        } else {
            if (mNowLyricMap.get(position, false)) {  //被选中则设置高亮颜色
                holder.lyricText.setTextColor(Color.GREEN);
                holder.lyricText.setTextSize(SP, 16);
            } else {
                holder.lyricText.setTextColor(Color.WHITE);
                holder.lyricText.setTextSize(SP, 14);
            }
        }
        return convertView;
    }

    public class ViewHolder {
        public TextView lyricText;
    }
}
