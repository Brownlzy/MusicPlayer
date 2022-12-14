package com.liux.musicplayer.adapters;

import android.content.Context;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.liux.musicplayer.R;
import com.liux.musicplayer.activities.MainActivity;

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
        if(nowPlay==-1)
            nowPlay=data.stream().map(t -> t.getDescription().getMediaUri().toString()).distinct().collect(Collectors.toList()).indexOf(path);
    }

    public int getNowPlay() {
        return nowPlay;
    }

    public interface RefreshListener{
        void deleteThis(MediaDescriptionCompat description);
        void skipToThis(long id);
        void popMenu(int position, View v);
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
        //????????????
        holder.mChoose=convertView.findViewById(R.id.choose_songs);
        holder.mItemTitle = convertView.findViewById(R.id.item_title);
        holder.mItemSinger = convertView.findViewById(R.id.item_singer);
        holder.mItemDuration = convertView.findViewById(R.id.item_duration);
        holder.hasLyric = convertView.findViewById(R.id.hasLyric);
        holder.playArrow = convertView.findViewById(R.id.playArrow);
        holder.btnDelete=convertView.findViewById(R.id.item_remove_this);
        //????????????
        holder.mItemTitle.setText(data.get(position).getDescription().getTitle());
        if(position==nowPlay){
            holder.mItemTitle.setTextColor(mContext.getColor(R.color.teal_200));
        }else {
            holder.mItemTitle.setTextColor(mContext.getColor(R.color.green_700));
        }
        holder.mItemSinger.setText(" - "+ String.valueOf(data.get(position).getDescription().getSubtitle()).split(" - ")[0]);
            //holder.mItemDuration.setText(MusicUtils.millis2FitTimeSpan(data.get(position).getDescription()));
        if (data.get(position).getDescription().getExtras().getString("LYRIC_URI")==null||
                data.get(position).getDescription().getExtras().getString("LYRIC_URI").equals("null"))
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
                mRefreshListener.skipToThis(position);
            }
        });
        holder.mChoose.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                mRefreshListener.popMenu(position,v);
                return false;
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
