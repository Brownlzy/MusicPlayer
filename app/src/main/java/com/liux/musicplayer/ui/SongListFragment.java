package com.liux.musicplayer.ui;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.DocumentsContract;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import com.blankj.utilcode.util.FileUtils;
import com.google.android.material.card.MaterialCardView;
import com.liux.musicplayer.R;
import com.liux.musicplayer.activities.MainActivity;
import com.liux.musicplayer.adapters.SongAdapter;
import com.liux.musicplayer.adapters.SonglistAdapter;
import com.liux.musicplayer.media.MusicLibrary;
import com.liux.musicplayer.models.Song;
import com.liux.musicplayer.utils.CustomDialogUtils;
import com.liux.musicplayer.utils.DisplayUtils;
import com.liux.musicplayer.utils.MusicUtils;
import com.liux.musicplayer.utils.SharedPrefs;
import com.liux.musicplayer.utils.UriTransform;
import com.liux.musicplayer.utils.User;
import com.liux.musicplayer.viewmodels.MyViewModel;
import com.xiasuhuei321.loadingdialog.view.LoadingDialog;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SongListFragment extends Fragment {

    private ListView songListView;
    private ListView songlistListView;
    private SongAdapter songAdapter;
    private SonglistAdapter songlistAdapter;
    private int listPosition = -1;
    private int listPositionY = 0;
    private List<Song> mSongList = null;//????????????
    private List<MusicLibrary.SongList> mSonglistList = null;//????????????
    private final List<String> mCheckedData = new ArrayList<>();//???????????????????????????
    private final SparseBooleanArray stateCheckedMap = new SparseBooleanArray();//????????????CheckBox??????????????????true?????????,false???????????????
    private boolean isSelectedAll = true;//?????????????????????????????????????????????????????????

    public boolean multipleChooseFlag = false;
    public boolean searchFlag = false;
    private boolean headerShowFlag = true;
    private boolean scrollFlag = false;// ??????????????????

    private int lastVisibleItemPosition = 0;// ?????????????????????????????????????????????0
    private MaterialCardView playlistHeader;
    private MaterialCardView playlistListHeader;
    private EditText searchEditText;
    private ImageView sortWay;
    private ImageView sortWayList;

    private LinearLayout addSongLayout;
    private LinearLayout editSongLayout;
    private LinearLayout sortSongLayout;
    private LinearLayout searchSongLayout;
    private List<Song> searchList;

    private MyViewModel myViewModel;
    public String nowSongListName;
    public boolean songlistFlag;

    //??????????????????????????????????????????????????????
    ActivityResultLauncher<Intent> getFolderIntent = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        int resultCode = result.getResultCode();
        if (resultCode == -1) {
            Intent data = result.getData();
            assert data != null;
            Uri uri = data.getData();
            addFolder(uri);
        }
    });
    private boolean isWebPlaylist;
    private SongAdapter.PopUpMenuListener popUpMenuListener = new SongAdapter.PopUpMenuListener() {
        @Override
        public void PopUpMenu(int position, View v) {
            PopupMenu popup = new PopupMenu(requireContext(), v);
            popup.getMenuInflater().inflate(R.menu.playlist_item_menu, popup.getMenu());
            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    switch (item.getItemId()) {
                        case R.id.item_menu_play:
                            myViewModel.getmMediaController().addQueueItem(MusicLibrary.getMediaItemDescription(mSongList.get(positionToMusicId(position))), -2);
                            break;
                        case R.id.item_menu_next_play:
                            myViewModel.getmMediaController().addQueueItem(MusicLibrary.getMediaItemDescription(mSongList.get(positionToMusicId(position))), -1);
                            break;
                        case R.id.item_menu_moreInfo:
                            showMusicDetails(positionToMusicId(position));
                            break;
                        case R.id.item_menu_edit:
                            CustomDialogUtils.showSongInfoEditDialog(MainActivity.mainActivity, mSongList.get(positionToMusicId(position)), false,
                                    new CustomDialogUtils.AlertDialogBtnClickListener() {
                                        @Override
                                        public void clickPositive(Song song) {
//                                            mSongList.set(positionToMusicId(position), song);
//                                            SharedPrefs.saveSongListByName(mSongList, nowSongListName);
                                            MusicLibrary.editSongInfo(song);
                                        }

                                        @Override
                                        public void clickNegative() {

                                        }
                                    });
                            break;
                        case R.id.item_menu_delete:
                            AlertDialog dialog = new AlertDialog.Builder(requireContext())
                                    .setTitle(R.string.confirmDelete)
                                    .setMessage(R.string.deleteInfo)
                                    .setIcon(R.mipmap.ic_launcher)
                                    .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            MusicLibrary.deleteMusicFromList(((Song) songAdapter.getItem(position)).getSongPath(), nowSongListName);
                                            initSongData(nowSongListName);
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
                            break;
                        case R.id.item_menu_add_to_list:
                            DialogInterface.OnClickListener pos=new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    MusicLibrary.addMusicToList(mSongList.get(positionToMusicId(position)).getSongPath(),CustomDialogUtils.chosenOne);
                                    Toast.makeText(getContext(), "?????????\""+mSongList.get(positionToMusicId(position)).getSongTitle()+"\"???\""+CustomDialogUtils.chosenOne+"\"", Toast.LENGTH_SHORT).show();
                                }
                            };
                            DialogInterface.OnClickListener neg=new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            };
                            List<String> songListName=MusicLibrary.getAllSongListName();
                            songListName.removeIf(s -> s.equals("allSongList")||s.equals("webAllSongList"));
                            String[] sln=songListName.toArray(new String[songListName.size()]);
                            CustomDialogUtils.chooseDialog(
                                    getContext(),
                                    getString(R.string.item_menu_add_to_list),
                                    sln,
                                    getString(R.string.confirm),
                                    getString(R.string.cancel),
                                    pos,
                                    neg);
                            break;
                    }
                    return true;
                }
            });
            popup.show();
        }
    };
    private SonglistAdapter.PopUpMenuListener popUpMenuListListener = new SonglistAdapter.PopUpMenuListener() {
        @Override
        public void PopUpMenu(int position, View v) {
            PopupMenu popup = new PopupMenu(requireContext(), v);
            popup.getMenuInflater().inflate(R.menu.playlist_list_item_menu, popup.getMenu());
            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    switch (item.getItemId()) {
                        case R.id.item_menu_play:
                            Log.e("SongPlaylistFragment", "newPlaylist");
//                            if (User.isLogin) {
                                if (!MusicLibrary.getSongListByName((String) songlistAdapter.getItem(position)).isEmpty()) {
                                    Bundle bundle = new Bundle();
                                    bundle.putString("QueueTitle", (String) songlistAdapter.getItem(position));
                                    //bundle.putString("Path", mSongList.get(0).getSongPath());
                                    myViewModel.getmMediaController().getTransportControls().sendCustomAction("NEW_PLAYLIST", bundle);
                                }
//                            } else
//                                Toast.makeText(getContext(), "????????????????????????????????????????????????", Toast.LENGTH_SHORT).show();
                            break;
                        case R.id.item_menu_moreInfo:
                            //showMusicDetails(positionToMusicId(position));
                            break;
                        case R.id.item_menu_edit:
                            if ((songlistAdapter.getItem(position)).equals("allSongList") || (songlistAdapter.getItem(position)).equals("webAllSongList"))
                                Toast.makeText(getContext(), "????????????????????????", Toast.LENGTH_SHORT).show();
                            else {
                                DialogInterface.OnClickListener pos = new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        if (CustomDialogUtils.editText.getText().toString().trim().length() >= 1) {
                                            MusicLibrary.renameSongList((String) songlistAdapter.getItem(position), CustomDialogUtils.editText.getText().toString().trim());
                                            initData();
                                        } else
                                            Toast.makeText(getContext(), "???????????????????????????1", Toast.LENGTH_SHORT).show();
                                    }
                                };
                                CustomDialogUtils.editTextDialog(getContext(),
                                        getString(R.string.inputListName),
                                        R.drawable.ic_outline_create_24,
                                        getString(R.string.confirm),
                                        null,
                                        getString(R.string.cancel),
                                        pos,
                                        null,
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {

                                            }
                                        }
                                );
                            }
                            break;
                        case R.id.item_menu_delete:
                            if ((songlistAdapter.getItem(position)).equals("allSongList") || (songlistAdapter.getItem(position)).equals("webAllSongList"))
                                Toast.makeText(getContext(), "????????????????????????", Toast.LENGTH_SHORT).show();
                            else {
                                AlertDialog dialog = new AlertDialog.Builder(requireContext())
                                        .setTitle(R.string.confirmDelete)
                                        .setMessage(R.string.deleteInfo)
                                        .setIcon(R.mipmap.ic_launcher)
                                        .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                MusicLibrary.deleteSongList((String) songlistAdapter.getItem(position));
                                                initData();
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
                            break;
                    }
                    return true;
                }
            });
            popup.show();
        }
    };

    private void addAllMusic() {
        LoadingDialog ld = new LoadingDialog(getContext());
        ld.setLoadingText("?????????")
                .setSuccessText("????????????")//??????????????????????????????
                .setFailedText("????????????")
                .closeSuccessAnim()
                .closeFailedAnim()
                .show();
        try {
            Handler addHandler = new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    super.handleMessage(msg);
                    if (msg.arg1 == 0) {
                        refreshList();
                        ld.loadSuccess();
                    } else {
                        ld.loadFailed();
                    }
                }
            };
            new Thread(new Runnable() {
                @Override
                public void run() {
                    List<Song> songList = MusicUtils.getMusicData(requireContext());
                    List<String> pathList = songList.stream().map(Song::getSongPath).distinct().collect(Collectors.toList());
                    MusicLibrary.addMusicListToList(pathList, nowSongListName);
                    Message message = Message.obtain();
                    message.arg1 = 0;
                    addHandler.sendMessage(message);
                }
            }).start();
            refreshList();
        } catch (Exception e) {
            ld.loadFailed();
        }
    }

    private void addFolder(Uri uri) {
        LoadingDialog ld = new LoadingDialog(getContext());
        ld.setLoadingText("?????????")
                .setSuccessText("????????????")//??????????????????????????????
                .setFailedText("????????????")
                .closeSuccessAnim()
                .closeFailedAnim()
                .show();
        Log.e("AddFolder", uri.toString());
        try {
            Uri docUri = DocumentsContract.buildDocumentUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri));
            List<String> pathList = new ArrayList<>();
            Handler addHandler = new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    super.handleMessage(msg);
                    if (msg.arg1 == 0) {
                        refreshList();
                        ld.loadSuccess();
                    } else {
                        ld.loadFailed();
                    }
                }
            };
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        searchFile(pathList, UriTransform.getPath(requireContext(), docUri).replace("/storage/emulated/0", "/sdcard"));
                        MusicLibrary.addMusicListToList(pathList, nowSongListName);
                        Message message = Message.obtain();
                        message.arg1 = 0;
                        addHandler.sendMessage(message);
                    } catch (Exception e) {
                        Message message = Message.obtain();
                        message.arg1 = 1;
                        addHandler.sendMessage(message);
                    }
                }
            }).start();
            refreshList();
        } catch (Exception e) {
            ld.loadFailed();
        }
    }

    private void searchFile(List<String> pathList, String filePath) {
        File file = new File(filePath);
        List<File> folderList = new ArrayList<File>();
        if (file.isDirectory()) {
            if (file.listFiles() != null) {
                for (File childFile : Objects.requireNonNull(file.listFiles())) {
                    if (childFile.isDirectory()) {
                        folderList.add(childFile);
                    } else {
                        if (checkChild(childFile))
                            pathList.add(childFile.getAbsolutePath());//??????????????????
                    }
                }
            }
        } else {
            if (checkChild(file))
                pathList.add(file.getAbsolutePath());
        }
        for (File folder : folderList) {
            searchFile(pathList, folder.getPath());
        }
    }

    private boolean checkChild(File childFile) {
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
            //??????????????????
            //if (childFile.length() / 1024 > 1024) {
            //?????????????????????????????????????????????????????????????????????????????????
            //Log.e("ScannedFiles", childFile.getAbsolutePath());
            //myViewModel.getMusicService().addMusic(childFile.getAbsolutePath());
            return true;
            //}
        }
        return false;
    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_playlist, container, false);
        myViewModel = new ViewModelProvider(MainActivity.mainActivity).get(MyViewModel.class);
        initView(view);
        View footView = LayoutInflater.from(requireContext()).inflate(R.layout.playlist_footview, container, false);
        songListView.addFooterView(footView);
        setOnListViewItemClickListener();
        setOnListViewItemLongClickListener();
        initData();
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    private View.OnClickListener songToolsListener = new View.OnClickListener() {
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
                    searchState();
                    break;
                case R.id.refresh_list:
                    refreshList();
                    break;
                case R.id.addSongs:
                    if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                        AlertDialog alertInfoDialog = new AlertDialog.Builder(requireContext())
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
                        showNoPermissionDialog();
                    }
                    break;
                case R.id.addFolder:
                    if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
                        getFolderIntent.launch(new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE));
                    else {
                        showNoPermissionDialog();
                    }
                    break;
                case R.id.sortWay:
                    sortSongPopMenu();
                    break;
                case R.id.playThisList:
//                    if (User.isLogin) {
                        playThisList();
//                    } else {
//                        Toast.makeText(getContext(), "????????????????????????????????????????????????", Toast.LENGTH_SHORT).show();
//                    }
                    break;
            }
        }
    };
    private View.OnClickListener songlistToolsListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.refresh_list_list:
                    initData();
                    break;
                case R.id.addNewList:
                    addNewList();
                    break;
                case R.id.addFolderList:
                    if (User.isLogin) {
                        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
                            addNewListFromFolder();
                        else {
                            showNoPermissionDialog();
                        }
                    } else
                        Toast.makeText(getContext(), "????????????????????????????????????????????????", Toast.LENGTH_SHORT).show();
                    break;
                case R.id.sortWayList:
                    sortSonglistPopMenu();
                    break;
            }
        }
    };

    private void addNewList() {
        DialogInterface.OnClickListener pos = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (CustomDialogUtils.editText.getText().toString().trim().length() >= 1) {
                    MusicLibrary.addNewSongList(CustomDialogUtils.editText.getText().toString().trim(), "");
                    initData();
                } else
                    Toast.makeText(getContext(), "???????????????????????????1", Toast.LENGTH_SHORT).show();
            }
        };
        CustomDialogUtils.editTextDialog(getContext(),
                getString(R.string.inputListName),
                R.drawable.ic_round_add_to_new_list_24,
                getString(R.string.confirm),
                null,
                getString(R.string.cancel),
                pos,
                null,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                }
        );
    }

    private void addNewListFromFolder() {
        DialogInterface.OnClickListener pos = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (CustomDialogUtils.editText.getText().toString().trim().length() >= 1) {
                    if (MusicLibrary.addNewSongList(CustomDialogUtils.editText.getText().toString().trim(), "")) {
                        initData();
                        initSongData(CustomDialogUtils.editText.getText().toString().trim());
                        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
                            getFolderIntent.launch(new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE));
                        else {
                            showNoPermissionDialog();
                        }
                    } else {
                        Toast.makeText(getContext(), "????????????????????????????????????????????????", Toast.LENGTH_SHORT).show();
                    }
                } else
                    Toast.makeText(getContext(), "???????????????????????????1", Toast.LENGTH_SHORT).show();
            }
        };
        CustomDialogUtils.editTextDialog(getContext(),
                getString(R.string.inputListName),
                R.drawable.ic_baseline_create_new_folder_24,
                getString(R.string.confirm),
                null,
                getString(R.string.cancel),
                pos,
                null,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                }
        );
    }

    private void sortSongPopMenu() {
        PopupMenu popup = new PopupMenu(requireContext(), sortWay);
        popup.getMenuInflater().inflate(R.menu.sort_way_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.sort_title_up:
                        songAdapter.setSortWay(SongAdapter.TITLEUP);
                        break;
                    case R.id.sort_title_down:
                        songAdapter.setSortWay(SongAdapter.TITLEDW);
                        break;
                    case R.id.sort_artist_up:
                        songAdapter.setSortWay(SongAdapter.ARTISTUP);
                        break;
                    case R.id.sort_artist_down:
                        songAdapter.setSortWay(SongAdapter.ARTISTDW);
                        break;
                    case R.id.sort_album_up:
                        songAdapter.setSortWay(SongAdapter.ALBUMUP);
                        break;
                    case R.id.sort_album_down:
                        songAdapter.setSortWay(SongAdapter.ALBUMDW);
                        break;
//                    case R.id.sort_time_up:
//                        songAdapter.setSortWay(SongAdapter.TIMEUP);
//                        break;
//                    case R.id.sort_time_down:
//                        songAdapter.setSortWay(SongAdapter.TIMEDW);
//                        break;
                }
                songAdapter.notifyDataSetChanged();
                return true;
            }
        });
        popup.show();
    }

    private void sortSonglistPopMenu() {
        PopupMenu popup = new PopupMenu(requireContext(), sortWayList);
        popup.getMenuInflater().inflate(R.menu.sort_list_way_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.sort_name_up:
                        songlistAdapter.setSortWay(SonglistAdapter.NAMEUP);
                        break;
                    case R.id.sort_name_down:
                        songlistAdapter.setSortWay(SonglistAdapter.NAMEDW);
                        break;
                    case R.id.sort_amount_up:
                        songlistAdapter.setSortWay(SonglistAdapter.AMOUNTUP);
                        break;
                    case R.id.sort_amount_down:
                        songlistAdapter.setSortWay(SonglistAdapter.AMOUNTDW);
                        break;
//                    case R.id.sort_time_up:
//                        songAdapter.setSortWay(SongAdapter.TIMEUP);
//                        break;
//                    case R.id.sort_time_down:
//                        songAdapter.setSortWay(SongAdapter.TIMEDW);
//                        break;
                }
                songlistAdapter.notifyDataSetChanged();
                return true;
            }
        });
        popup.show();
    }

    private void playThisList() {
                            /*Log.e("SongPlaylistFragment", (String) myViewModel.getmMediaController().getQueueTitle());
                    if(myViewModel.getmMediaController().getQueueTitle().equals(nowSongListName)){
                        Log.e("SongPlaylistFragment","equals(nowSongListName)");
                        //myViewModel.getmMediaController().addQueueItem(MusicLibrary.getMediaItemDescription(mSongList.get(positionToMusicId(position))),-2);
                        int qid=myViewModel.getmMediaController().getQueue().stream()
                                .map(t -> t.getDescription().getMediaUri().getPath()).distinct().collect(Collectors.toList())
                                .indexOf(mSongList.get(positionToMusicId(position)).getSongPath());
                        myViewModel.getmMediaController().getTransportControls().skipToQueueItem(myViewModel.getmMediaController().getQueue().get(qid).getQueueId());
                    }else{*/
        Log.e("SongPlaylistFragment", "newPlaylist");
        if (!mSongList.isEmpty()) {
            Bundle bundle = new Bundle();
            bundle.putString("QueueTitle", nowSongListName);
            //bundle.putString("Path", mSongList.get(0).getSongPath());
            myViewModel.getmMediaController().getTransportControls().sendCustomAction("NEW_PLAYLIST", bundle);
        }
        /* }*/
    }

    private void showNoPermissionDialog() {
        AlertDialog alertInfoDialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.no_permission)
                .setMessage(R.string.no_permission_info)
                .setIcon(R.mipmap.ic_launcher)
                .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .setNeutralButton(R.string.gotoSettings, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        MainActivity.mainActivity.setViewPagerToId(2);
                    }
                })
                .create();
        alertInfoDialog.show();
    }

    private void editList() {
        if (multipleChooseFlag) {
            editSongLayout.setVisibility(View.GONE);
            addSongLayout.setVisibility(View.VISIBLE);
            cancel();
        } else {
            multipleChooseFlag = true;
            songAdapter.setShowCheckBox(true);//CheckBox?????????????????????
            songAdapter.notifyDataSetChanged();//??????ListView
            addSongLayout.setVisibility(View.GONE);
            editSongLayout.setVisibility(View.VISIBLE);
        }
    }

    private void searchState() {
        if (searchFlag) {
            searchSongLayout.setVisibility(View.GONE);
            sortSongLayout.setVisibility(View.VISIBLE);
            searchEditText.setText("");
            songAdapter = new SongAdapter(requireContext(), mSongList, stateCheckedMap, popUpMenuListener);
            songListView.setAdapter(songAdapter);
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
//            songAdapter.setNowPlay(Integer.parseInt(prefs.getString("nowId", "0")));
        } else {
            sortSongLayout.setVisibility(View.GONE);
            searchSongLayout.setVisibility(View.VISIBLE);
            searchList = new ArrayList<>();
            songAdapter = new SongAdapter(requireContext(), searchList, stateCheckedMap, popUpMenuListener);
            songListView.setAdapter(songAdapter);
//            songAdapter.setNowPlay(-1);
        }
        searchFlag = !searchFlag;
    }

    @Override
    public void onPause() {
        super.onPause();  // Always call the superclass method first
        try {
            myViewModel.setListPosition(songListView.getFirstVisiblePosition());
            myViewModel.setListPositionY(songListView.getChildAt(0).getTop());
        } catch (NullPointerException e) {
            myViewModel.setListPosition(-1);
        }
    }

    @Override
    public void onResume() {
        super.onResume();  // Always call the superclass method first
        if (getActivity() != null)
            //&& myViewModel.getMusicService() != null
            //&& isWebPlaylist != myViewModel.getMusicService().isWebPlayMode())
            //initData();
            if (myViewModel.getListPosition() != -1)
                songListView.setSelectionFromTop(myViewModel.getListPosition(), myViewModel.getListPositionY() - DisplayUtils.dip2px(requireContext(), 44));
        if (!SharedPrefs.getIsUseWebPlayList() && mSonglistList.stream().map(t -> t.n).distinct().collect(Collectors.toList()).contains("webAllSongList")
                || SharedPrefs.getIsUseWebPlayList() && !mSonglistList.stream().map(t -> t.n).distinct().collect(Collectors.toList()).contains("webAllSongList")) {
            unInitSongData();
            initData();
            MainActivity.mainActivity.setSongListFragmentTitle();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    public void cancel() {
        setStateCheckedMap(false);//???CheckBox????????????????????????????????????
        songAdapter.setShowCheckBox(false);//???CheckBox??????????????????
        songAdapter.notifyDataSetChanged();//??????ListView
        multipleChooseFlag = false;
    }

    private void delete() {
        if (mCheckedData.size() == 0) {
            Toast.makeText(requireContext(), "?????????????????????????????????", Toast.LENGTH_SHORT).show();
            return;
        }
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
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
        //mSongList.removeAll(mCheckedData);//??????????????????
        //mSongList.removeIf(song -> mCheckedData.contains(song.getSongPath()));
        MusicLibrary.deleteMusicListFromList(mCheckedData, nowSongListName);
        setStateCheckedMap(false);//???CheckBox????????????????????????????????????
        mCheckedData.clear();//??????????????????
        songAdapter.notifyDataSetChanged();
        //myViewModel.getMusicService().setAllSongListAfterDelete(mSongList);
        //SharedPrefs.saveSongListByName(mSongList,nowSongListName);
        Toast.makeText(requireContext(), "????????????", Toast.LENGTH_SHORT).show();
        refreshList();
    }

    /**
     * ????????????stateCheckedMap?????????true?????????false,false?????????true
     */
    private void inverse() {
        mCheckedData.clear();
        for (int i = 0; i < mSongList.size(); i++) {
            if (stateCheckedMap.get(i)) {
                stateCheckedMap.put(i, false);
            } else {
                stateCheckedMap.put(i, true);
                mCheckedData.add(mSongList.get(i).getSongPath());
            }
            songListView.setItemChecked(i, stateCheckedMap.get(i));//????????????????????????ListView?????????????????????????????????????????????????????????????????????
        }
        songAdapter.notifyDataSetChanged();
    }

    private void selectAll() {
        mCheckedData.clear();//????????????????????????
        if (isSelectedAll) {
            setStateCheckedMap(true);//???CheckBox?????????????????????????????????
            isSelectedAll = false;
            //??????source??????uri
            List<String> uriList = mSongList.stream().map(t -> t.getSongPath()).distinct().collect(Collectors.toList());
            mCheckedData.addAll(uriList);//??????????????????????????????????????????
        } else {
            setStateCheckedMap(false);//???CheckBox????????????????????????????????????
            isSelectedAll = true;
        }
        songAdapter.notifyDataSetChanged();
    }

    private void setOnListViewItemClickListener() {
        songListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position >= mSongList.size()) return;
                if (multipleChooseFlag) {
                    updateCheckBoxStatus(view, position);
                } else {
                    /*Log.e("SongPlaylistFragment", (String) myViewModel.getmMediaController().getQueueTitle());
                    if(myViewModel.getmMediaController().getQueueTitle().equals(nowSongListName)){
                        Log.e("SongPlaylistFragment","equals(nowSongListName)");
                        //myViewModel.getmMediaController().addQueueItem(MusicLibrary.getMediaItemDescription(mSongList.get(positionToMusicId(position))),-2);
                        int qid=myViewModel.getmMediaController().getQueue().stream()
                                .map(t -> t.getDescription().getMediaUri().getPath()).distinct().collect(Collectors.toList())
                                .indexOf(mSongList.get(positionToMusicId(position)).getSongPath());
                        myViewModel.getmMediaController().getTransportControls().skipToQueueItem(myViewModel.getmMediaController().getQueue().get(qid).getQueueId());
                    }else{
                        Log.e("SongPlaylistFragment","newPlaylist");
                        Bundle bundle = new Bundle();
                        //bundle.putLong("QueueId",MusicLibrary.getPlayingList().get(positionToMusicId(position)).getDescription().hashCode());
                        bundle.putString("QueueTitle", nowSongListName);
                        bundle.putString("Path", mSongList.get(positionToMusicId(position)).getSongPath());
                        myViewModel.getmMediaController().getTransportControls().sendCustomAction("NEW_PLAYLIST", bundle);
                    }*/

                    myViewModel.getmMediaController().addQueueItem(MusicLibrary.getMediaItemDescription(mSongList.get(positionToMusicId(position))), -2);
                }
            }
        });
        songlistListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                initSongData((String) songlistAdapter.getItem(position));
            }
        });
    }

    /**
     * ????????????false??????click??????????????????,,?????????Long click???????????????click???
     * ????????????true??????click??????????????????click????????????????????????
     * ?????????click???setOnItemClickListener
     */
    private void setOnListViewItemLongClickListener() {
        songListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                editList();
                multipleChooseFlag = true;
                songAdapter.setShowCheckBox(true);//CheckBox?????????????????????
                updateCheckBoxStatus(view, position);
                showPlaylistHeaderBar(true);
                return true;
            }
        });
    }

    private void updateCheckBoxStatus(View view, int position) {
        SongAdapter.ViewHolder holder = (SongAdapter.ViewHolder) view.getTag();
        holder.checkBox.toggle();//??????CheckBox???????????????
        songListView.setItemChecked(position, holder.checkBox.isChecked());//??????ListView????????????????????????
        stateCheckedMap.put(position, holder.checkBox.isChecked());//??????CheckBox???????????????
        if (holder.checkBox.isChecked()) {
            mCheckedData.add(mSongList.get(positionToMusicId(position)).getSongPath());//CheckBox?????????????????????????????????????????????????????????
        } else {
            mCheckedData.remove(mSongList.get(positionToMusicId(position)).getSongPath());//CheckBox???????????????????????????????????????????????????????????????
        }
        songAdapter.notifyDataSetChanged();
    }

    private void initView(View view) {
        view.findViewById(R.id.addNewList).setOnClickListener(songlistToolsListener);
        view.findViewById(R.id.refresh_list_list).setOnClickListener(songlistToolsListener);
        sortWayList = view.findViewById(R.id.sortWayList);
        sortWayList.setOnClickListener(songlistToolsListener);
        view.findViewById(R.id.addFolderList).setOnClickListener(songlistToolsListener);

        playlistHeader = view.findViewById(R.id.playlist_header);
        playlistListHeader = view.findViewById(R.id.playlist_list_header);
        view.findViewById(R.id.ll_cancel).setOnClickListener(songToolsListener);
        view.findViewById(R.id.ll_delete).setOnClickListener(songToolsListener);
        view.findViewById(R.id.ll_inverse).setOnClickListener(songToolsListener);
        view.findViewById(R.id.ll_select_all).setOnClickListener(songToolsListener);
        view.findViewById(R.id.addSongs).setOnClickListener(songToolsListener);
        view.findViewById(R.id.addFolder).setOnClickListener(songToolsListener);
        view.findViewById(R.id.edit_list).setOnClickListener(songToolsListener);
        view.findViewById(R.id.refresh_list).setOnClickListener(songToolsListener);
        view.findViewById(R.id.search_list).setOnClickListener(songToolsListener);
        view.findViewById(R.id.playThisList).setOnClickListener(songToolsListener);
        sortWay = view.findViewById(R.id.sortWay);
        sortWay.setOnClickListener(songToolsListener);
        addSongLayout = view.findViewById(R.id.ll_addBar);
        editSongLayout = view.findViewById(R.id.ll_editBar);
        sortSongLayout = view.findViewById(R.id.ll_sort);
        searchSongLayout = view.findViewById(R.id.ll_search);
        searchEditText = view.findViewById(R.id.searchEditText);
        searchEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                searchFromList();
                return false;
            }
        });
        songListView = view.findViewById(R.id.lv);
        songlistListView = view.findViewById(R.id.list_lv);
        songListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        songlistListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        songListView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                //????????????
                switch (scrollState) {
                    // ???????????????
                    case AbsListView.OnScrollListener.SCROLL_STATE_IDLE:// ???????????????????????????
                        scrollFlag = false;
                        // ????????????????????? ???position??????0???????????????
                        if (songListView.getLastVisiblePosition() == (songListView.getCount() - 1)) {
                            //Toast.makeText(requireContext(), "?????????", Toast.LENGTH_SHORT).show();
                            showPlaylistHeaderBar(true);
                        }
                        // ?????????????????????
                        if (songListView.getFirstVisiblePosition() == 0) {
                            showPlaylistHeaderBar(true);
                        }
                        break;
                    case AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL:// ?????????
                        scrollFlag = true;
                        break;
                    case AbsListView.OnScrollListener.SCROLL_STATE_FLING:
                        // ?????????????????????????????????????????????????????????????????????????????????????????????
                        scrollFlag = true;
                        break;
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                //????????????
                if (scrollFlag) {
                    if (firstVisibleItem < lastVisibleItemPosition) {
                        // ??????
                        showPlaylistHeaderBar(true);
                    } else if (firstVisibleItem > lastVisibleItemPosition) {
                        // ??????
                        showPlaylistHeaderBar(false);
                    } else {
                        return;
                    }
                    lastVisibleItemPosition = firstVisibleItem;//????????????
                }
            }
        });
        myViewModel.getSongsMutableLiveData().observeForever(new Observer<List<Song>>() {
            @Override
            public void onChanged(List<Song> songs) {
                songAdapter = new SongAdapter(requireContext(), songs, stateCheckedMap, popUpMenuListener);
                songListView.setAdapter(songAdapter);
            }
        });
    }

    private void searchFromList() {
        searchList.clear();
        Pattern pattern = Pattern.compile(String.valueOf(searchEditText.getText()),Pattern.CASE_INSENSITIVE);   //??????????????????
        Matcher matcher;
        for (Song song : mSongList) {
            matcher = pattern.matcher(song.getSongTitle()+" "+song.getArtistName()+" "+song.getAlbumName());
            if(matcher.find()){
                searchList.add(song);
            }
        }
        songAdapter.notifyDataSetChanged();
    }

    private void showPlaylistHeaderBar(boolean isShow) {
        if (isShow) {
            if (!headerShowFlag) {
                headerShowFlag = true;
                Animation animation = AnimationUtils.loadAnimation(requireContext(), R.anim.gradually_moveup_show);
                playlistHeader.setVisibility(View.VISIBLE);
                playlistHeader.startAnimation(animation);
            }
        } else {
            if (headerShowFlag) {
                headerShowFlag = false;
                Animation animation = AnimationUtils.loadAnimation(requireContext(), R.anim.gradually_moveup_hide);
                playlistHeader.startAnimation(animation);
                playlistHeader.setVisibility(View.GONE);
            }
        }
    }

    private void refreshList() {
        if (multipleChooseFlag) editList();
        if (searchFlag) searchState();
        try {
            listPosition = songListView.getFirstVisiblePosition();
            listPositionY = songListView.getChildAt(0).getTop();
        } catch (NullPointerException e) {
            e.printStackTrace();
            listPosition = -1;
        }
        Log.e("playList", String.valueOf(listPosition));
        Log.e("playList", String.valueOf(listPositionY));
        initSongData(nowSongListName);
        if (listPosition != -1)
            songListView.setSelectionFromTop(listPosition, listPositionY - DisplayUtils.dip2px(requireContext(), 44));

        //myViewModel.getMusicService().setAllSongListAfterAdd(mSongList);
        //setNowPlaying(myViewModel.getMusicService().getNowId());
    }

    public void initData() {
        //if (myViewModel!=null && myViewModel.getMusicService() != null) {
        //isWebPlaylist = myViewModel.getMusicService().isWebPlayMode();
        mSonglistList = MusicLibrary.getAllSongListList();
        songlistAdapter = new SonglistAdapter(requireContext(), mSonglistList, stateCheckedMap, popUpMenuListListener);
        songlistListView.setAdapter(songlistAdapter);
        songlistAdapter.notifyDataSetChanged();
        showPlaylistHeaderBar(false);
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        //}
    }

    public void initSongData(String name) {
        showSonglistList(false);
        showPlaylistHeaderBar(true);
        songlistFlag = true;
        songListView.setVisibility(View.VISIBLE);
        mSongList = new ArrayList<>();
        for (Song s : MusicLibrary.getSongListByName(name)) {
            mSongList.add(MusicLibrary.querySong(s.getSongPath()));
        }
        nowSongListName = name;
        setStateCheckedMap(false);
        songAdapter = new SongAdapter(requireContext(), mSongList, stateCheckedMap, popUpMenuListener);
        songListView.setAdapter(songAdapter);
        songAdapter.notifyDataSetChanged();
        MainActivity.mainActivity.setSongListFragmentTitle();
    }

    public void unInitSongData() {
        showSonglistList(true);
        showPlaylistHeaderBar(false);
        songListView.setVisibility(View.INVISIBLE);
        songlistFlag = false;
    }

    private void showSonglistList(boolean isShow) {
        if (isShow) {
            songlistListView.setVisibility(View.VISIBLE);
            Animation animation = AnimationUtils.loadAnimation(requireContext(), R.anim.gradually_moveup_show);
            playlistListHeader.startAnimation(animation);
            playlistListHeader.setVisibility(View.VISIBLE);
        } else {
            songlistListView.setVisibility(View.GONE);
            if(!songlistFlag) {
                Animation animation = AnimationUtils.loadAnimation(requireContext(), R.anim.gradually_moveup_hide);
                playlistListHeader.startAnimation(animation);
            }
                playlistListHeader.setVisibility(View.GONE);
        }
    }

    /**
     * ????????????CheckBox???????????????
     */
    private void setStateCheckedMap(boolean isSelectedAll) {
        for (int i = 0; i < mSongList.size(); i++) {
            stateCheckedMap.put(i, isSelectedAll);
            songListView.setItemChecked(i, isSelectedAll);
        }
    }

    private int positionToMusicId(int position) {
        return mSongList.indexOf(((Song) songAdapter.getItem(position)));
    }

    private void showMusicDetails(int musicId) {
        CustomDialogUtils.showMusicDetails(getContext(),
                mSongList.get(musicId).getSongPath());

//        MusicUtils.Metadata metadata = null;
//        //if (myViewModel.getMusicService().isWebPlayMode()) {
//        ///    metadata = MusicUtils.getMetadataFromSong(mSongList.get(musicId));
//        //} else {
//        metadata = MusicUtils.getMetadata(mSongList.get(musicId).getSongPath());
//        //}
//        Bitmap bitmap = MusicUtils.getAlbumImage(mSongList.get(musicId).getSongPath());
//        AlertDialog.Builder dialog = new AlertDialog.Builder(requireContext())
//                .setTitle(mSongList.get(musicId).getSongTitle())
//                .setMessage(
//                        getString(R.string.title_artist) + metadata.artist + "\n" +
//                                getString(R.string.title_album) + metadata.album + "\n" +
//                                getString(R.string.title_duration) + ConvertUtils.millis2FitTimeSpan(Long.parseLong(metadata.duration), 4) + "\n" +
//                                getString(R.string.title_bitrate) + Long.parseLong(metadata.bitrate) / 1024 + "Kbps\n" +
//                                getString(R.string.title_mimetype) + metadata.mimetype + "\n" +
//                                getString(R.string.file_size) + metadata.sizeByte + "\n" +
//                                getString(R.string.title_path) + mSongList.get(musicId).getSongPath() + "\n" +
//                                getString(R.string.title_lyric) + mSongList.get(musicId).getLyricPath()
//                )
//                .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                    }
//                });
//        if (bitmap == null) {   //???????????????????????????????????????
//            dialog.setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.ic_baseline_music_note_24));
//        } else {    //??????
//            dialog.setIcon(new BitmapDrawable(requireContext().getResources(), bitmap));
//        }
//        dialog.create().show();
    }

    public int onBackPressed() {
        if (multipleChooseFlag || searchFlag) {
            if (multipleChooseFlag)
                editList();
            if (searchFlag)
                searchState();
        } else if (songlistFlag) {
            unInitSongData();
            initData();
            MainActivity.mainActivity.setSongListFragmentTitle();
        }
        return 0;
    }

    public void setListViewPosition(int listViewPosition) {
        songListView.setSelectionFromTop(listViewPosition, 0);
        showPlaylistHeaderBar(true);
        lastVisibleItemPosition = listViewPosition;
    }

    public void setNowPlaying(int musicId) {
        if (songAdapter != null && !searchFlag) {
//            songAdapter.setNowPlay(musicId);
            songAdapter.notifyDataSetChanged();
        }
    }
}


