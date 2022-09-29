package com.liux.musicplayer.ui.playlist;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.blankj.utilcode.util.FileUtils;
import com.google.android.material.card.MaterialCardView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.liux.musicplayer.ui.MainActivity;
import com.liux.musicplayer.R;
import com.liux.musicplayer.databinding.FragmentPlaylistBinding;
import com.liux.musicplayer.utils.DisplayUtils;
import com.liux.musicplayer.utils.MusicUtils;
import com.liux.musicplayer.utils.UriTransform;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class PlaylistFragment extends Fragment implements View.OnClickListener {

    private FragmentPlaylistBinding binding;
    private ListView lvData;
    private PlaylistAdapter adapter;
    private int listPosition = -1;
    private int listPositionY = 0;
    private List<MusicUtils.Song> mSongList = new ArrayList<>();//所有数据
    private final List<String> mCheckedData = new ArrayList<>();//将选中数据放入里面
    private final SparseBooleanArray stateCheckedMap = new SparseBooleanArray();//用来存放CheckBox的选中状态，true为选中,false为没有选中
    private boolean isSelectedAll = true;//用来控制点击全选，全选和全不选相互切换

    public boolean multipleChooseFlag = false;
    private boolean searchFlag = false;
    private boolean headerShowFlag = true;

    private int lastVisibleItemPosition = 0;// 标记上次滑动位置，初始化默认为0
    private boolean scrollFlag = false;// 标记是否滑动
    private MaterialCardView playlistHeader;

    private LinearLayout addSongLayout;
    private LinearLayout editSongLayout;
    private LinearLayout sortSongLayout;
    private LinearLayout searchSongLayout;

    //用于接受系统文件管理器返回目录的回调
    ActivityResultLauncher<Intent> getFolderIntent = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            int resultCode = result.getResultCode();
            if (resultCode == -1) {
                Intent data = result.getData();
                assert data != null;
                Uri uri = data.getData();
                addFolder(uri);
            }
        }
    });

    private void addAllMusic() {
        List<MusicUtils.Song> songList = MusicUtils.getMusicData(getContext());
        ((MainActivity) getActivity()).getMusicPlayer().addMusic(songList);
    }

    private void addFolder(Uri uri) {
        Log.e("AddFolder", uri.toString());
        Uri docUri = DocumentsContract.buildDocumentUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri));
        searchFile(UriTransform.getPath(getContext(), docUri).replace("/storage/emulated/0", "/sdcard"));
        refreshList();
    }

    private void searchFile(String filePath) {
        File file = new File(filePath);
        List<File> folderList = new ArrayList<File>();
        if (file.isDirectory()) {
            if (file.listFiles() != null) {
                for (File childFile : Objects.requireNonNull(file.listFiles())) {
                    if (childFile.isDirectory()) {
                        folderList.add(childFile);
                    } else {
                        checkChild(childFile);//筛选结果返回
                    }
                }
            }
        } else {
            checkChild(file);
        }
        for (File folder : folderList) {
            searchFile(folder.getPath());
        }
    }

    private void checkChild(File childFile) {
        if (FileUtils.getFileExtension(childFile).equalsIgnoreCase("mp3")
                || FileUtils.getFileExtension(childFile).equalsIgnoreCase("flac")
                || FileUtils.getFileExtension(childFile).equalsIgnoreCase("amr")
                || FileUtils.getFileExtension(childFile).equalsIgnoreCase("aac")
                || FileUtils.getFileExtension(childFile).equalsIgnoreCase("ogg")
                || FileUtils.getFileExtension(childFile).equalsIgnoreCase("3gp")
                || FileUtils.getFileExtension(childFile).equalsIgnoreCase("m4a")
                || FileUtils.getFileExtension(childFile).equalsIgnoreCase("gsm")
                || FileUtils.getFileExtension(childFile).equalsIgnoreCase("wav")
        ) {
            //if (childFile.length() / 1024 > 1024) {
            //创建模型类存储值，并添加到集合中。通过集合可做任意操作
            Log.e("ScannedFiles", childFile.getAbsolutePath());
            ((MainActivity) getActivity()).getMusicPlayer().addMusic(childFile.getAbsolutePath());
            //}
        }
    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_playlist, container, false);
        binding = FragmentPlaylistBinding.inflate(inflater, container, false);

        initView(view);
        initData();
        adapter = new PlaylistAdapter(this, getContext(), mSongList, stateCheckedMap);
        lvData.setAdapter(adapter);
        setOnListViewItemClickListener();
        setOnListViewItemLongClickListener();

        return view;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.ll_cancel:
                cancel();
                break;
            case R.id.ll_delete:
                delete();
                break;
            case R.id.ll_inverse:
                inverse();
                break;
            case R.id.ll_select_all:
                selectAll();
                break;
            case R.id.edit_list:
                editList();
                break;
            case R.id.search_list:
                searchList();
                break;
            case R.id.refresh_list:
                refreshList();
                break;
            case R.id.addSongs:
                if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    AlertDialog alertInfoDialog = new AlertDialog.Builder(getContext())
                            .setTitle(R.string.addAllMusic)
                            .setMessage(R.string.addAllMusic_info)
                            .setIcon(R.mipmap.ic_launcher)
                            .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    addAllMusic();
                                    refreshList();
                                }
                            })
                            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            })
                            .create();
                    alertInfoDialog.show();
                } else {
                    AlertDialog alertInfoDialog = new AlertDialog.Builder(getContext())
                            .setTitle(R.string.no_permission)
                            .setMessage(R.string.no_permission_info)
                            .setIcon(R.mipmap.ic_launcher)
                            .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            })
                            .create();
                    alertInfoDialog.show();
                }
                break;
            case R.id.addFolder:
                if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
                    getFolderIntent.launch(new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE));
                else {
                    AlertDialog alertInfoDialog = new AlertDialog.Builder(getContext())
                            .setTitle(R.string.no_permission)
                            .setMessage(R.string.no_permission_info)
                            .setIcon(R.mipmap.ic_launcher)
                            .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            })
                            .create();
                    alertInfoDialog.show();
                }
                break;
        }
    }

    private void editList() {
        if (multipleChooseFlag) {
            editSongLayout.setVisibility(View.GONE);
            addSongLayout.setVisibility(View.VISIBLE);
            cancel();
        } else {
            multipleChooseFlag = true;
            adapter.setShowCheckBox(true);//CheckBox的那个方框显示
            adapter.notifyDataSetChanged();//更新ListView
            addSongLayout.setVisibility(View.GONE);
            editSongLayout.setVisibility(View.VISIBLE);
        }
    }

    private void searchList() {
        if (searchFlag) {
            searchSongLayout.setVisibility(View.GONE);
            sortSongLayout.setVisibility(View.VISIBLE);
        } else {
            sortSongLayout.setVisibility(View.GONE);
            searchSongLayout.setVisibility(View.VISIBLE);
        }
        searchFlag = !searchFlag;
    }

    @Override
    public void onPause() {
        super.onPause();  // Always call the superclass method first
        try {
            listPosition = lvData.getFirstVisiblePosition();
            listPositionY = lvData.getChildAt(0).getTop();
        } catch (NullPointerException e) {
            e.printStackTrace();
            listPosition = -1;
        }
        Log.e("playList", String.valueOf(listPosition));
        Log.e("playList", String.valueOf(listPositionY));
    }

    @Override
    public void onResume() {
        super.onResume();  // Always call the superclass method first
        if (listPosition != -1)
            lvData.setSelectionFromTop(listPosition, listPositionY - DisplayUtils.dip2px(getContext(), 44));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public void cancel() {
        setStateCheckedMap(false);//将CheckBox的所有选中状态变成未选中
        adapter.setShowCheckBox(false);//让CheckBox那个方框隐藏
        adapter.notifyDataSetChanged();//更新ListView
        multipleChooseFlag = false;
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
        setStateCheckedMap(false);//将CheckBox的所有选中状态变成未选中
        mCheckedData.clear();//清空选中数据
        adapter.notifyDataSetChanged();
        ((MainActivity) getActivity()).getMusicPlayer().setPlayList(mSongList);
        Toast.makeText(getContext(), "删除成功", Toast.LENGTH_SHORT).show();
        refreshList();
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
            //取出source——uri
            List<String> uriList = mSongList.stream().map(t -> t.source_uri).distinct().collect(Collectors.toList());
            mCheckedData.addAll(uriList);//把所有的数据添加到选中列表中
        } else {
            setStateCheckedMap(false);//将CheckBox的所有选中状态变成未选中
            isSelectedAll = true;
        }
        adapter.notifyDataSetChanged();
    }

    private void setOnListViewItemClickListener() {
        lvData.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (multipleChooseFlag)
                    updateCheckBoxStatus(view, position);
                else {
                    ((MainActivity) getActivity()).getMusicPlayer().playThisNow(position);
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
                editList();
                multipleChooseFlag = true;
                adapter.setShowCheckBox(true);//CheckBox的那个方框显示
                updateCheckBoxStatus(view, position);
                showPlaylistHeaderBar(true);
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
        playlistHeader = view.findViewById(R.id.playlist_header);
        view.findViewById(R.id.ll_cancel).setOnClickListener(this);
        view.findViewById(R.id.ll_delete).setOnClickListener(this);
        view.findViewById(R.id.ll_inverse).setOnClickListener(this);
        view.findViewById(R.id.ll_select_all).setOnClickListener(this);
        view.findViewById(R.id.addSongs).setOnClickListener(this);
        view.findViewById(R.id.addFolder).setOnClickListener(this);
        view.findViewById(R.id.edit_list).setOnClickListener(this);
        view.findViewById(R.id.refresh_list).setOnClickListener(this);
        view.findViewById(R.id.search_list).setOnClickListener(this);
        addSongLayout = view.findViewById(R.id.ll_addBar);
        editSongLayout = view.findViewById(R.id.ll_editBar);
        sortSongLayout = view.findViewById(R.id.ll_sort);
        searchSongLayout = view.findViewById(R.id.ll_search);
        lvData = view.findViewById(R.id.lv);
        lvData.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        lvData.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                //判断状态
                switch (scrollState) {
                    // 当不滚动时
                    case AbsListView.OnScrollListener.SCROLL_STATE_IDLE:// 是当屏幕停止滚动时
                        scrollFlag = false;
                        // 判断滚动到底部 、position是从0开始算起的
                        if (lvData.getLastVisiblePosition() == (lvData.getCount() - 1)) {
                            //TODO
                            //Toast.makeText(getContext(), "到底了", Toast.LENGTH_SHORT).show();
                            showPlaylistHeaderBar(true);
                        }
                        // 判断滚动到顶部
                        if (lvData.getFirstVisiblePosition() == 0) {
                            //TODO
                            showPlaylistHeaderBar(true);
                        }
                        break;
                    case AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL:// 滚动时
                        scrollFlag = true;
                        break;
                    case AbsListView.OnScrollListener.SCROLL_STATE_FLING:
                        // 当用户由于之前划动屏幕并抬起手指，屏幕产生惯性滑动时，即滚动时
                        scrollFlag = true;
                        break;
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                //当滑动时
                if (scrollFlag) {
                    if (firstVisibleItem < lastVisibleItemPosition) {
                        //TODO 上滑
                        showPlaylistHeaderBar(true);
                    } else if (firstVisibleItem > lastVisibleItemPosition) {
                        //TODO 下滑
                        showPlaylistHeaderBar(false);
                    } else {
                        return;
                    }
                    lastVisibleItemPosition = firstVisibleItem;//更新位置
                }
            }
        });
    }

    private void showPlaylistHeaderBar(boolean isShow) {
        if (isShow) {
            if (!headerShowFlag) {
                headerShowFlag = true;
                Animation animation = AnimationUtils.loadAnimation(getContext(), R.anim.gradually_moveup_show);
                playlistHeader.setVisibility(View.VISIBLE);
                playlistHeader.startAnimation(animation);
            }
        } else {
            if (headerShowFlag) {
                headerShowFlag = false;
                Animation animation = AnimationUtils.loadAnimation(getContext(), R.anim.gradually_moveup_hide);
                playlistHeader.startAnimation(animation);
                playlistHeader.setVisibility(View.GONE);
            }
        }
    }

    private void refreshList() {
        if (multipleChooseFlag) editList();
        if (searchFlag) searchList();
        try {
            listPosition = lvData.getFirstVisiblePosition();
            listPositionY = lvData.getChildAt(0).getTop();
        } catch (NullPointerException e) {
            e.printStackTrace();
            listPosition = -1;
        }
        Log.e("playList", String.valueOf(listPosition));
        Log.e("playList", String.valueOf(listPositionY));

        ((MainActivity) getActivity()).getMusicPlayer().refreshPlayList();
        initData();
        adapter = new PlaylistAdapter(this, getContext(), mSongList, stateCheckedMap);
        lvData.setAdapter(adapter);

        if (listPosition != -1)
            lvData.setSelectionFromTop(listPosition, listPositionY - DisplayUtils.dip2px(getContext(), 44));

        ((MainActivity) getActivity()).getMusicPlayer().setPlayList(mSongList);
    }

    private void initData() {
        SharedPreferences sp = getActivity().getSharedPreferences("com.liux.musicplayer_preferences", Activity.MODE_PRIVATE);
        String playListJson = sp.getString("playList",
                "[{\"id\":-1,\"title\":\"这是音乐标题\",\"artist\":\"这是歌手\",\"album\":\"这是专辑名\",\"filename\":\"此为测试数据，添加音乐文件后自动删除\"," +
                        "\"source_uri\":\"file:///storage/emulated/0/Android/data/com.liux.musicplayer/Music/这是歌手 - 这是音乐标题.mp3\"," +
                        "\"lyric_uri\":\"file:///storage/emulated/0/Android/data/com.liux.musicplayer/Music/这是歌手 - 这是音乐标题.lrc\",\"duration\":\"0\"}]");
        Gson gson = new Gson();
        Type playListType = new TypeToken<ArrayList<MusicUtils.Song>>() {
        }.getType();
        mSongList = gson.fromJson(playListJson, playListType);
        if (mSongList == null || mSongList.size() == 0) {
            playListJson = "[{\"id\":-1,\"title\":\"这是音乐标题\",\"artist\":\"这是歌手\",\"album\":\"这是专辑名\",\"filename\":\"此为测试数据，添加音乐文件后自动删除\"," +
                    "\"source_uri\":\"file:///storage/emulated/0/Android/data/com.liux.musicplayer/Music/这是歌手 - 这是音乐标题.mp3\"," +
                    "\"lyric_uri\":\"file:///storage/emulated/0/Android/data/com.liux.musicplayer/Music/这是歌手 - 这是音乐标题.lrc\",\"duration\":\"0\"}]";
            mSongList = gson.fromJson(playListJson, playListType);
        }
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

    public int onBackPressed() {
        editList();
        return 0;
    }

    public void setListViewPosition(int listViewPosition) {
        lvData.setSelectionFromTop(listViewPosition, 0);
        showPlaylistHeaderBar(true);
        lastVisibleItemPosition = listViewPosition;
    }
}


