package com.liux.musicplayer.adapters;

import android.content.Context;
import android.util.SparseBooleanArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.liux.musicplayer.R;
import com.liux.musicplayer.media.MusicLibrary;
import com.liux.musicplayer.models.Song;
import com.liux.musicplayer.utils.MusicUtils;

import java.util.List;

public class SonglistAdapter extends BaseAdapter {

    List<MusicLibrary.SongList> data;
    private final Context mContext;
    ViewHolder holder;
    private boolean isShowCheckBox = false;//表示当前是否是多选状态。
    private final SparseBooleanArray stateCheckedMap;//用来存放CheckBox的选中状态，true为选中,false为没有选中
    private final PopUpMenuListener mPopUpMenuListener;

    public interface PopUpMenuListener{
        void PopUpMenu(int position,View v);
    }

    public SonglistAdapter(Context context, List<MusicLibrary.SongList> data, SparseBooleanArray stateCheckedMap, PopUpMenuListener playlistFragment) {
        this.data = data;
        mContext = context;
        this.stateCheckedMap = stateCheckedMap;
        mPopUpMenuListener=playlistFragment;
    }

    @Override
    public int getCount() {
        if (data==null)
            return 0;
        return data.size();
    }

    @Override
    public Object getItem(int i) {
        return data.get(i).n;
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup viewGroup) {
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = View.inflate(mContext, R.layout.item_songlist_list, null);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        //绑定对象
        holder.checkBox = convertView.findViewById(R.id.chb_select_way_point);
        holder.btnMore = convertView.findViewById(R.id.btn_more_vert);
        holder.mItemTitle = convertView.findViewById(R.id.item_title);
        holder.mItemId = convertView.findViewById(R.id.item_id);
        holder.mItemSummary = convertView.findViewById(R.id.item_summary);
        showAndHideCheckBox();//控制CheckBox的那个的框显示与隐藏
        //设置数据
        if(data.get(position).n.equals("allSongList"))
            holder.mItemTitle.setText(R.string.allSongList);
        else if(data.get(position).n.equals("webAllSongList"))
            holder.mItemTitle.setText(R.string.webAllSongList);
        else
            holder.mItemTitle.setText(data.get(position).n);
        holder.mItemId.setText(String.valueOf(position + 1));
        holder.mItemSummary.setText(
                mContext.getString(R.string.songlist_summary)
                        .replace("%d",
                        String.valueOf(data.get(position).c)
                ));
        holder.checkBox.setChecked(stateCheckedMap.get(position));//设置CheckBox是否选中
        holder.btnMore.setOnClickListener(new View.OnClickListener() {  //设置单击监听器
            @Override
            public void onClick(View v) {
                mPopUpMenuListener.PopUpMenu(position,v);
            }
        });
        return convertView;
    }

    public class ViewHolder {
        public TextView mItemTitle;
        public TextView mItemId;
        public TextView mItemSummary;
        public CheckBox checkBox;
        public ImageView btnMore;
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
}
