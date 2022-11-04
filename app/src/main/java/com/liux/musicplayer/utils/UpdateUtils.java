package com.liux.musicplayer.utils;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;

import com.blankj.utilcode.util.TimeUtils;
import com.google.gson.Gson;
import com.liux.musicplayer.R;

import java.io.File;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class UpdateUtils {
    private static String TAG="UpdateUtils";
    private static Handler updateHandler;
    private static UpdateInfo updateInfo;

    static class UpdateInfo {
        String lastVersionName;
        int lastVersionCode;
        String filename;
        String size;
        String changLog;
    }
    public static void checkUpdate(Context context,boolean isShowStateInfo) {
        if(isShowStateInfo)
            Toast.makeText(context, "正在检查更新，请稍候", Toast.LENGTH_SHORT).show();
        updateHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if(msg.arg1==200){
                    updateHandle(context, String.valueOf(msg.obj),isShowStateInfo);
                }else if(isShowStateInfo)
                    Toast.makeText(context, (String)msg.obj, Toast.LENGTH_SHORT).show();
            }
        };

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("https://brownlzy.github.io/MyOtaInfo/MusicPlayer/forteachcher.json")
                .get()//default
                .build();
        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.d(TAG, "onFailure: ");
                Message message = Message.obtain();
                message.arg1=0;
                message.obj = "获取更新信息失败（无法连接服务器）";
                updateHandler.sendMessage(message);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String result = response.body().string();
                int id = response.code();
                Log.d(TAG, "onResponse: " + result);
                Message message = Message.obtain();
                if (id == 200) {
                    message.arg1=200;
                    message.obj = result;
                } else {
                    message.arg1=0;
                    message.obj = "获取更新信息失败（" + id +"）";
                }
                updateHandler.sendMessage(message);
            }
        });
    }

    private static void updateHandle(Context context, String result,boolean isShowStateInfo) {
        int versionCode = 0;
        try {
            versionCode = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        Gson gson = new Gson();
        updateInfo = gson.fromJson(result, UpdateInfo.class);

        if (updateInfo == null || versionCode == 0)
            Toast.makeText(context, "获取更新信息失败:" + versionCode, Toast.LENGTH_SHORT).show();
        else if (updateInfo.lastVersionCode > versionCode) {
            AlertDialog alertInfoDialog = null;
            alertInfoDialog = new AlertDialog.Builder(context)
                    .setTitle(R.string.title_update)
                    .setMessage(context.getString(R.string.title_lastVersion) + updateInfo.lastVersionName + "\n"
                            + context.getString(R.string.title_size) + updateInfo.size + "\n"
                            + context.getString(R.string.title_changlog) + "\n"
                            + updateInfo.changLog.replace("\\n", "\n"))
                    .setIcon(R.mipmap.ic_launcher)
                    .setPositiveButton(R.string.update, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Toast.makeText(context, "安装包下载中", Toast.LENGTH_SHORT).show();
                            acquireDownload(context, "https://brownlzy.github.io/MyOtaInfo/MusicPlayer/apk/" + updateInfo.filename);
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
            if(isShowStateInfo)
                Toast.makeText(context, "当前已是最新版", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 调用系统下载器下载文件
     *
     * @param context ()
     * @return boolean 是否成功调用系统下载
     */
    private static boolean acquireDownload(Context context, String url) {
        Log.i(TAG + "//acquireDownload()", "Download requested");
        String fileName = url.substring(url.lastIndexOf('/') + 1)+".apk";
        File localFile = new File(context.getExternalCacheDir() + "/apk", fileName);
        //if (localFile.exists()&&checkCRC(localFile.getAbsolutePath())) {
        if (localFile.exists()) {
            Toast.makeText(context, "如果解析安装包失败，请前往应用详情页清理缓存", Toast.LENGTH_LONG).show();
            Uri uri = FileProvider.getUriForFile(context, context.getPackageName(), localFile);
            Log.d(TAG + "//acquireDownload()", "File exists");
            Log.d(TAG + "//acquireDownload()", uri.toString());
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
            context.startActivity(intent);
            return true;
        } else {
            Log.d(TAG + "//acquireDownload()", "Download file");
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setTitle(context.getResources().getString(R.string.app_name));
            request.setDescription(context.getResources().getString(R.string.notification_downloading_latest_version));
            request.setVisibleInDownloadsUi(true);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
            File cloudFile = new File(context.getExternalCacheDir() + "/apk", fileName);
            request.setDestinationUri(Uri.fromFile(cloudFile));
            DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            if (downloadManager != null) {
                Long requestID = downloadManager.enqueue(request);
                context.registerReceiver(new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        DownloadManager.Query query = new DownloadManager.Query();
                        query.setFilterById(requestID);
                        Cursor cursor = downloadManager.query(query);
                        if (cursor.moveToFirst()) {
                            int columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                            if (DownloadManager.STATUS_SUCCESSFUL == cursor.getInt(columnIndex)) {
                                int columnIndexUri = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI);
                                String downloadFileName = cursor.getString(columnIndexUri);
                                if (downloadFileName != null) {
                                    downloadFileName = downloadFileName.substring(downloadFileName.lastIndexOf('/') + 1);
                                    File downloadFile = new File(context.getExternalCacheDir() + "/apk", downloadFileName);
                                    Uri uri = FileProvider.getUriForFile(context, context.getPackageName(), downloadFile);
                                    Log.i(getClass().toString() + "//acquireDownload()", uri.toString());
                                    Intent installIntent = new Intent(Intent.ACTION_VIEW);
                                    installIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    installIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                    installIntent.setDataAndType(uri, "application/vnd.android.package-archive");
                                    context.startActivity(installIntent);
                                }
                            }
                        }
                    }
                }, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
                return true;
            }
        }
        return false;
    }
}