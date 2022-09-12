package com.liux.musicplayer.ui.playlist;

import android.content.Context;
import android.util.SparseBooleanArray;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.PopupMenu;

import com.liux.musicplayer.MainActivity;
import com.liux.musicplayer.MusicPlayer;
import com.liux.musicplayer.R;

import java.util.List;

/**
 * Created by yangms on 2018/7/16.
 */

public class PlaylistAdapter extends BaseAdapter {

    List<MusicPlayer.Song> data;
    private final Context mContext;
    ViewHolder holder;
    private boolean isShowCheckBox = false;//表示当前是否是多选状态。
    private final SparseBooleanArray stateCheckedMap;//用来存放CheckBox的选中状态，true为选中,false为没有选中
    private PlaylistFragment mPlaylistFragment;

    public PlaylistAdapter(PlaylistFragment playlistFragment, Context context, List<MusicPlayer.Song> data, SparseBooleanArray stateCheckedMap) {
        this.data = data;//保存传入的数据
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
        return null;
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    //listView会通过这个方法找adaptar要每一个元素的View（布局），我们重写这个函数把带数据的元素返回给它
    @Override
    public View getView(final int position, View convertView, ViewGroup viewGroup) {
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = View.inflate(mContext, R.layout.item_playlist, null);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.checkBox = convertView.findViewById(R.id.chb_select_way_point);
        holder.btnMore = convertView.findViewById(R.id.btn_more_vert);
        holder.mItemTitle = convertView.findViewById(R.id.item_title);
        holder.mItemId = convertView.findViewById(R.id.item_id);
        holder.mItemSinger = convertView.findViewById(R.id.item_singer);
        showAndHideCheckBox();//控制CheckBox的那个的框显示与隐藏

        holder.mItemTitle.setText(data.get(position).title);
        holder.mItemId.setText(String.valueOf(data.get(position).id + 1));
        holder.mItemSinger.setText(data.get(position).artist +
                (data.get(position).album.equals("") ? "" : (" - " + data.get(position).album)));
        holder.checkBox.setChecked(stateCheckedMap.get(position));//设置CheckBox是否选中
        //设置右边三点的监听器
        holder.btnMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //弹出焦点在这个元素的菜单
                mPlaylistFragment.popMenu(position, v);
            }
        });
        return convertView;//返回给listview
    }

    public class ViewHolder {   //便于管理
        public TextView mItemTitle;
        public TextView mItemId;
        public TextView mItemSinger;
        public CheckBox checkBox;
        public ImageView btnMore;
    }

    //设置勾选框的开关
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

}
