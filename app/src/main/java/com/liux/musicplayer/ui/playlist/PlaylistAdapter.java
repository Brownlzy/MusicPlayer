package com.liux.musicplayer.ui.playlist;

import android.content.Context;
import android.util.SparseBooleanArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.liux.musicplayer.R;
import com.liux.musicplayer.utils.MusicUtils;

import java.util.List;

public class PlaylistAdapter extends BaseAdapter {

    List<MusicUtils.Song> data;
    private final Context mContext;
    ViewHolder holder;
    private boolean isShowCheckBox = false;//表示当前是否是多选状态。
    private final SparseBooleanArray stateCheckedMap;//用来存放CheckBox的选中状态，true为选中,false为没有选中
    private final PlaylistFragment mPlaylistFragment;
    private int nowPlay;

    public PlaylistAdapter(PlaylistFragment playlistFragment, Context context, List<MusicUtils.Song> data, SparseBooleanArray stateCheckedMap) {
        this.data = data;
        mContext = context;
        this.stateCheckedMap = stateCheckedMap;
        mPlaylistFragment = playlistFragment;
    }

    @Override
    public int getCount() {
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
            convertView = View.inflate(mContext, R.layout.item_playlist, null);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        //绑定对象
        holder.checkBox = convertView.findViewById(R.id.chb_select_way_point);
        holder.btnMore = convertView.findViewById(R.id.btn_more_vert);
        holder.mItemTitle = convertView.findViewById(R.id.item_title);
        holder.mItemId = convertView.findViewById(R.id.item_id);
        holder.mItemSinger = convertView.findViewById(R.id.item_singer);
        holder.mItemDuration = convertView.findViewById(R.id.item_duration);
        holder.hasLyric = convertView.findViewById(R.id.hasLyric);
        holder.playArrow = convertView.findViewById(R.id.playArrow);
        showAndHideCheckBox();//控制CheckBox的那个的框显示与隐藏
        //设置数据
        holder.mItemTitle.setText(data.get(position).title);
        holder.mItemId.setText(String.valueOf(position + 1));
        holder.mItemSinger.setText(data.get(position).artist +
                (data.get(position).album.equals("null") ? "" : (" - " + data.get(position).album)));
        if (data.get(position).duration != null)
            holder.mItemDuration.setText(MusicUtils.millis2FitTimeSpan(Long.parseLong(data.get(position).duration)));
        else
            holder.mItemDuration.setText("null");
        if (data.get(position).lyric_uri.equals("null"))
            holder.hasLyric.setVisibility(View.GONE);
        else
            holder.hasLyric.setVisibility(View.VISIBLE);
        if (position == nowPlay)
            holder.playArrow.setVisibility(View.VISIBLE);
        else
            holder.playArrow.setVisibility(View.GONE);
        holder.checkBox.setChecked(stateCheckedMap.get(position));//设置CheckBox是否选中
        holder.btnMore.setOnClickListener(new View.OnClickListener() {  //设置单击监听器
            @Override
            public void onClick(View v) {
                mPlaylistFragment.popMenu(position, v); //弹出popup菜单
            }
        });
        return convertView;
    }

    public class ViewHolder {
        public TextView mItemTitle;
        public TextView mItemId;
        public TextView mItemSinger;
        public TextView mItemDuration;
        public CheckBox checkBox;
        public ImageView btnMore;
        public ImageView hasLyric;
        public ImageView playArrow;
    }

    private void showAndHideCheckBox() {
        if (isShowCheckBox) {
            holder.checkBox.setVisibility(View.VISIBLE);
            holder.btnMore.setVisibility(View.GONE);
        } else {
            holder.checkBox.setVisibility(View.GONE);
            holder.btnMore.setVisibility(View.VISIBLE);
        }
    }


    public boolean isShowCheckBox() {
        return isShowCheckBox;
    }

    public void setShowCheckBox(boolean showCheckBox) {
        isShowCheckBox = showCheckBox;
    }

    public void setNowPlay(int musicId) {
        nowPlay = musicId;
    }

}
