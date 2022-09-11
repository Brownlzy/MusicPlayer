package com.liux.musicplayer.ui.home;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.liux.musicplayer.R;
import com.liux.musicplayer.util.MusicUtils;

public class LyricAdapter extends BaseAdapter {

    MusicUtils.Lyric lyricData;
    private final Context mContext;
    ViewHolder holder;
    private HomeFragment mHomeFragment;

    public LyricAdapter(HomeFragment homeFragment, Context context, MusicUtils.Lyric lyric) {
        lyricData = lyric;
        mContext = context;
        mHomeFragment = homeFragment;
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
        holder.lyricText = convertView.findViewById(R.id.item_lyric_text);
        holder.lyricText.setText(lyricData.lyricList.get(position));
        return convertView;
    }

    public class ViewHolder {
        public TextView lyricText;
    }
}
