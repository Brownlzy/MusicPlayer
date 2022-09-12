package com.liux.musicplayer.ui.playlist;

import android.content.DialogInterface;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.card.MaterialCardView;
import com.liux.musicplayer.MainActivity;
import com.liux.musicplayer.MusicPlayer;
import com.liux.musicplayer.R;
import com.liux.musicplayer.databinding.FragmentPlaylistBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PlaylistFragment extends Fragment implements View.OnClickListener {//声明了点击接口

    private FragmentPlaylistBinding binding;

    private ListView lvData;
    private MaterialCardView mLlEditBar;//控制下方那一行的显示与隐藏
    private PlaylistAdapter adapter;
    private ImageView editPen;
    private ImageView Refresh;
    private List<MusicPlayer.Song> mSongList = new ArrayList<>();//所有数据
    private final List<String> mCheckedData = new ArrayList<>();//将选中数据放入里面
    private final SparseBooleanArray stateCheckedMap = new SparseBooleanArray();//用来存放CheckBox的选中状态，true为选中,false为没有选中
    private boolean isSelectedAll = true;//用来控制点击全选，全选和全不选相互切换
    private boolean mutilChooseFlag = false;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_playlist, container, false);
        binding = FragmentPlaylistBinding.inflate(inflater, container, false);

        initView(view);//初始化控件
        initData();//初始化播放列表的数据
        adapter = new PlaylistAdapter(this, getContext(), mSongList, stateCheckedMap);
        lvData.setAdapter(adapter);//将listView和adapter绑定
        //设置列表的单击和长按监听器
        setOnListViewItemClickListener();
        setOnListViewItemLongClickListener();

        return view;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {//通过获得点击事件的控件id区分执行任务
            case R.id.ll_cancel:
                cancel();
                break;
            case R.id.ll_delete:
                delete();
                break;
            case R.id.ll_inverse:
                inverse();//反选
                break;
            case R.id.ll_select_all:
                selectAll();
                break;
        }
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void cancel() {
        setStateCheckedMap(false);//将CheckBox的所有选中状态变成未选中
        mLlEditBar.setVisibility(View.GONE);//隐藏下方布局
        adapter.setShowCheckBox(false);//让CheckBox那个方框隐藏
        adapter.notifyDataSetChanged();//更新ListView
        mutilChooseFlag = false;
    }

    private void delete() {
        if (mCheckedData.size() == 0) {
            Toast.makeText(getContext(), "您还没有选中任何数据！", Toast.LENGTH_SHORT).show();
            return;
        }
        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setTitle(R.string.confirmDelete)
                .setMessage(R.string.deleteInfo)
                .setIcon(R.mipmap.ic_launcher)
                .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        beSureDelete();
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create();
        dialog.show();
    }

    private void beSureDelete() {
        //mSongList.removeAll(mCheckedData);//删除选中数据
        mSongList.removeIf(song -> mCheckedData.contains(song.source_uri));
        for (int i = 0; i < mSongList.size(); i++) {
            mSongList.get(i).id = i;
        }
        setStateCheckedMap(false);//将CheckBox的所有选中状态变成未选中
        mCheckedData.clear();//清空选中数据
        adapter.notifyDataSetChanged();
        Toast.makeText(getContext(), "删除成功", Toast.LENGTH_SHORT).show();
    }

    /**
     * 反选就是stateCheckedMap的值为true时变为false,false时变成true
     */
    private void inverse() {
        mCheckedData.clear();
        for (int i = 0; i < mSongList.size(); i++) {
            if (stateCheckedMap.get(i)) {
                stateCheckedMap.put(i, false);
            } else {
                stateCheckedMap.put(i, true);
                mCheckedData.add(mSongList.get(i).source_uri);
            }
            lvData.setItemChecked(i, stateCheckedMap.get(i));//这个好行可以控制ListView复用的问题，不设置这个会出现点击一个会选中多个
        }
        adapter.notifyDataSetChanged();
    }

    private void selectAll() {
        mCheckedData.clear();//清空之前选中数据
        if (isSelectedAll) {
            setStateCheckedMap(true);//将CheckBox的所有选中状态变成选中
            isSelectedAll = false;
            //取出source_uri
            List<String> uriList = mSongList.stream().map(t -> t.source_uri).distinct().collect(Collectors.toList());
            mCheckedData.addAll(uriList);//把所有的数据添加到选中列表中
        } else {
            setStateCheckedMap(false);//将CheckBox的所有选中状态变成未选中
            isSelectedAll = true;
        }
        adapter.notifyDataSetChanged();//通知adapter更新
    }

    private void setOnListViewItemClickListener() {
        lvData.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (mutilChooseFlag)//判断是否正在多选
                    updateCheckBoxStatus(view, position);//增加刚单击的选项为勾选
                else {
                    ((MainActivity) getActivity()).getMusicPlayer().playThisNow(position);//播放刚才点击的音乐
                }
            }
        });
    }

    /**
     * 如果返回false那么click仍然会被调用,,先调用Long click，然后调用click。
     * 如果返回true那么click就会被吃掉，click就不会再被调用了
     * 在这里click即setOnItemClickListener
     */
    private void setOnListViewItemLongClickListener() {
        lvData.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                mutilChooseFlag = true;
                mLlEditBar.setVisibility(View.VISIBLE);//显示下方布局
                adapter.setShowCheckBox(true);//CheckBox的那个方框显示
                updateCheckBoxStatus(view, position);//更新刚刚长按的为勾选
                return true;
            }
        });
    }

    private void updateCheckBoxStatus(View view, int position) {
        PlaylistAdapter.ViewHolder holder = (PlaylistAdapter.ViewHolder) view.getTag();
        holder.checkBox.toggle();//反转CheckBox的选中状态
        lvData.setItemChecked(position, holder.checkBox.isChecked());//长按ListView时选中按的那一项
        stateCheckedMap.put(position, holder.checkBox.isChecked());//存放CheckBox的选中状态
        if (holder.checkBox.isChecked()) {
            mCheckedData.add(mSongList.get(position).source_uri);//CheckBox选中时，把这一项的数据加到选中数据列表
        } else {
            mCheckedData.remove(mSongList.get(position).source_uri);//CheckBox未选中时，把这一项的数据从选中数据列表移除
        }
        adapter.notifyDataSetChanged();
    }

    private void initView(View view) {
        lvData = view.findViewById(R.id.lv);
        mLlEditBar = view.findViewById(R.id.ll_edit_bar);

        view.findViewById(R.id.ll_cancel).setOnClickListener(this);
        view.findViewById(R.id.ll_delete).setOnClickListener(this);
        view.findViewById(R.id.ll_inverse).setOnClickListener(this);
        view.findViewById(R.id.ll_select_all).setOnClickListener(this);
        editPen = view.findViewById(R.id.edit_list);
        editPen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mutilChooseFlag) {
                    cancel();
                } else {
                    mutilChooseFlag = true;
                    mLlEditBar.setVisibility(View.VISIBLE);//显示下方布局
                    adapter.setShowCheckBox(true);//CheckBox的那个方框显示
                    adapter.notifyDataSetChanged();//更新ListView
                }
            }
        });
        Refresh = view.findViewById(R.id.refresh_list);
        Refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refreshList();
            }
        });
        lvData.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
    }

    private void refreshList() {
        //刷新播放列表
        ((MainActivity) getActivity()).getMusicPlayer().refreshPlayList();
        initData();
        adapter = new PlaylistAdapter(this, getContext(), mSongList, stateCheckedMap);
        lvData.setAdapter(adapter);
    }

    private void initData() {
        //初始化数据，我们的播放列表是MainActivity实例化的MusicPlayer从SharedPreferences里读取的。
        mSongList = ((MainActivity) getActivity()).getMusicPlayer().getPlayList();
        setStateCheckedMap(false);
    }

    /**
     * 设置所有CheckBox的选中状态
     */
    private void setStateCheckedMap(boolean isSelectedAll) {
        for (int i = 0; i < mSongList.size(); i++) {
            stateCheckedMap.put(i, isSelectedAll);
            lvData.setItemChecked(i, isSelectedAll);
        }
    }

    public void popMenu(int position, View view) {
        PopupMenu popup = new PopupMenu(getContext(), view);
        //绑定布局在/res/menu/playlist_item_menu.xml
        popup.getMenuInflater().inflate(R.menu.playlist_item_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.item_menu_play:
                        ((MainActivity) getActivity()).getMusicPlayer().playThisNow(position);
                        break;
                    case R.id.item_menu_moreInfo:
                        Toast.makeText(getContext(), "你点了详情",
                                Toast.LENGTH_SHORT).show();
                        break;
                }
                return true;
            }
        });
        popup.show();
    }

    //TODO:Fragment下的返回监听 参考https://blog.csdn.net/itheima_mxh/article/details/45774689
    public int onBackPressed() {
        if (mLlEditBar.getVisibility() == View.VISIBLE) {
            cancel();
            return 1;
        } else {
            return 0;
        }
    }
}


