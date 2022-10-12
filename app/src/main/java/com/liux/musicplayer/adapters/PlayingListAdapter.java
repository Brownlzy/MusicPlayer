package com.liux.musicplayer.adapters;

import android.content.Context;
import android.graphics.Color;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.blankj.utilcode.util.FileUtils;
import com.liux.musicplayer.R;
import com.liux.musicplayer.activities.MainActivity;
import com.liux.musicplayer.media.MusicLibrary;
import com.liux.musicplayer.models.Song;
import com.liux.musicplayer.utils.MusicUtils;

import java.util.List;
import java.util.stream.Collectors;

public class PlayingListAdapter extends BaseAdapter {

    List<MediaSessionCompat.QueueItem> data;
    private final Context mContext;
    ViewHolder holder;
    private int nowPlay;
    private RefreshListener mRefreshListener;

    public void setNowPlay(String path) {
        if(data==null) return;
        nowPlay=data.stream().map(t -> t.getDescription().getMediaUri().getPath()).distinct().collect(Collectors.toList()).indexOf(path);
    }

    public int getNowPlay() {
        return nowPlay;
    }

    public interface RefreshListener{
        void deleteThis(MediaDescriptionCompat description);
        void skipToThis(long id);
    }

    public PlayingListAdapter(MainActivity context, List<MediaSessionCompat.QueueItem> data, RefreshListener refreshListener) {
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
        holder.mChoose=convertView.findViewById(R.id.choose_songs);
        holder.mItemTitle = convertView.findViewById(R.id.item_title);
        holder.mItemSinger = convertView.findViewById(R.id.item_singer);
        holder.mItemDuration = convertView.findViewById(R.id.item_duration);
        holder.hasLyric = convertView.findViewById(R.id.hasLyric);
        holder.playArrow = convertView.findViewById(R.id.playArrow);
        holder.btnDelete=convertView.findViewById(R.id.item_remove_this);
        //设置数据
        holder.mItemTitle.setText(data.get(position).getDescription().getTitle());
        if(position==nowPlay){
            holder.mItemTitle.setTextColor(Color.CYAN);
        }else {
            holder.mItemTitle.setTextColor(Color.GREEN);
        }
        holder.mItemSinger.setText(" - "+ String.valueOf(data.get(position).getDescription().getSubtitle()).split(" - ")[0]);
            //holder.mItemDuration.setText(MusicUtils.millis2FitTimeSpan(data.get(position).getDescription()));
        if (FileUtils.isFileExists(data.get(position).getDescription().getExtras().getString("LYRIC_URI")))
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
                mRefreshListener.deleteThis(data.get(position).getDescription());
            }
        });
        holder.mChoose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRefreshListener.skipToThis(data.get(position).getQueueId());
            }
        });
        return convertView;
    }

    public class ViewHolder {
        public RelativeLayout mChoose;
        public TextView mItemTitle;
        public TextView mItemSinger;
        public TextView mItemDuration;
        public ImageView btnDelete;
        public ImageView hasLyric;
        public ImageView playArrow;
    }
public int getNowId(){
        return nowPlay;
}

}
