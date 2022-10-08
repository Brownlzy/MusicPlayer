package com.liux.musicplayer.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.liux.musicplayer.R;
import com.liux.musicplayer.activities.MainActivity;
import com.liux.musicplayer.models.Song;
import com.liux.musicplayer.utils.MusicUtils;

import java.util.List;

public class PlayingListAdapter extends BaseAdapter {

    List<Song> data;
    private final Context mContext;
    ViewHolder holder;
    private int nowPlay;
    private RefreshListener mRefreshListener;
    public interface RefreshListener{
        void deleteThis(int position);
    }

    public PlayingListAdapter(MainActivity context, List<Song> data, RefreshListener refreshListener) {
        this.data = data;
        mContext = context;
        mRefreshListener=refreshListener;
    }

    @Override
    public int getCount() {
        if(data==null)
            return 0;
        else
        return data.size();
    }

    @Override
    public Object getItem(int i) {
        return data.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup viewGroup) {
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = View.inflate(mContext, R.layout.item_playing_list, null);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        //绑定对象
        holder.mItemTitle = convertView.findViewById(R.id.item_title);
        holder.mItemSinger = convertView.findViewById(R.id.item_singer);
        holder.mItemDuration = convertView.findViewById(R.id.item_duration);
        holder.hasLyric = convertView.findViewById(R.id.hasLyric);
        holder.playArrow = convertView.findViewById(R.id.playArrow);
        holder.btnDelete=convertView.findViewById(R.id.item_remove_this);
        //设置数据
        holder.mItemTitle.setText(data.get(position).getSongTitle());
        if(position==nowPlay){
            holder.mItemTitle.setTextColor(Color.CYAN);
        }else {
            holder.mItemTitle.setTextColor(Color.GREEN);
        }
        holder.mItemSinger.setText(" - "+data.get(position).getArtistName());
            holder.mItemDuration.setText(MusicUtils.millis2FitTimeSpan(data.get(position).getSongDuration()));
        if (data.get(position).getLyricPath()==null)
            holder.hasLyric.setVisibility(View.GONE);
        else
            holder.hasLyric.setVisibility(View.VISIBLE);
        if (position == nowPlay)
            holder.playArrow.setVisibility(View.VISIBLE);
        else
            holder.playArrow.setVisibility(View.GONE);
        holder.btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRefreshListener.deleteThis(position);
            }
        });
        return convertView;
    }

    public class ViewHolder {
        public TextView mItemTitle;
        public TextView mItemSinger;
        public TextView mItemDuration;
        public ImageView btnDelete;
        public ImageView hasLyric;
        public ImageView playArrow;
    }

    public void setNowPlay(int musicId) {
        nowPlay = musicId;
    }

}
