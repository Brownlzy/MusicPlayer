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
    private static String TAG = "UpdateUtils";
    private static Handler updateHandler;
    private static Handler newsHandler;
    private static UpdateInfo updateInfo;

    static class UpdateInfo {
        String crc;
        String lastVersionName;
        int lastVersionCode;
        String filename;
        String size;
        String changLog;
        String manual;
    }

    static class News {
        int id;
        String ct;
        int fun=0;
        String ag;
        String bn;
    }

    public static void checkUpdate(Context context, boolean isShowStateInfo) {
        if (isShowStateInfo)
            Toast.makeText(context, "正在检查更新，请稍候", Toast.LENGTH_SHORT).show();
        updateHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (msg.arg1 == 200) {
                    updateHandle(context, String.valueOf(msg.obj), isShowStateInfo);
                    SharedPrefs.putLastCheckUpdateTime(TimeUtils.getNowMills());
                } else if (isShowStateInfo)
                    Toast.makeText(context, (String) msg.obj, Toast.LENGTH_SHORT).show();
            }
        };

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("https://brownlzy.github.io/MyOtaInfo/MusicPlayer/updateinfo2.json")
                .get()//default
                .build();
        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.d(TAG, "onFailure: ");
                Message message = Message.obtain();
                message.arg1 = 0;
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
                    message.arg1 = 200;
                    message.obj = result;
                } else {
                    message.arg1 = 0;
                    message.obj = "获取更新信息失败（" + id + "）";
                }
                updateHandler.sendMessage(message);
            }
        });
    }

    public static void checkNews(Context context, boolean isManual) {
        if (isManual)
            Toast.makeText(context, "正在读取公告板", Toast.LENGTH_SHORT).show();
        newsHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (msg.arg1 == 200) {
                    newsHandle(context, String.valueOf(msg.obj), isManual);
                    SharedPrefs.putLastNewsUpdateTime(TimeUtils.getNowMills());
                } else if (isManual)
                    Toast.makeText(context, String.valueOf(msg.obj), Toast.LENGTH_SHORT).show();
            }
        };

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("https://brownlzy.github.io/MyOtaInfo/MusicPlayer/newsboard.json")
                .get()//default
                .build();
        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.d(TAG, "onFailure: 获取信息失败（无法连接服务器）");
                Message message = Message.obtain();
                message.arg1 = 0;
                message.obj = "获取失败（无法连接服务器）";
                newsHandler.sendMessage(message);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String result = response.body().string();
                int id = response.code();
                Log.d(TAG, "onResponse: " + result);
                Message message = Message.obtain();
                if (id == 200) {
                    message.arg1 = 200;
                    message.obj = result;
                } else {
                    message.arg1 = 0;
                    message.obj = "获取失败";
                    Log.d(TAG, "onResponse: " + id);
                }
                newsHandler.sendMessage(message);
            }
        });
    }


    private static void updateHandle(Context context, String result, boolean isShowStateInfo) {
        int versionCode = 0;
        String versionName = "";
        try {
            versionName = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
            versionCode = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        Gson gson = new Gson();
        updateInfo = gson.fromJson(result, UpdateInfo.class);

        if (updateInfo == null || versionCode == 0)
            Toast.makeText(context, "获取更新信息失败:" + versionCode, Toast.LENGTH_SHORT).show();
        else {
            AlertDialog.Builder alertInfoDialog = null;
            alertInfoDialog = new AlertDialog.Builder(context)
                    .setTitle(R.string.title_update)
                    .setMessage(context.getString(R.string.title_nowVersion) + versionName + "\n"
                            + context.getString(R.string.title_lastVersion) + updateInfo.lastVersionName + "\n"
                            + context.getString(R.string.title_size) + updateInfo.size + "\n"
                            + context.getString(R.string.title_changlog) + "\n"
                            + updateInfo.changLog.replace("\\n", "\n"))
                    .setIcon(R.mipmap.ic_launcher)
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });
            if (updateInfo.lastVersionCode > versionCode) {
                alertInfoDialog.setPositiveButton(R.string.update, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Toast.makeText(context, "安装包下载中", Toast.LENGTH_SHORT).show();
                                acquireDownload(context, "https://brownlzy.github.io/MyOtaInfo/MusicPlayer/apk/" + updateInfo.filename);
                            }
                        })
                        .setNeutralButton(R.string.manual_download, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent(Intent.ACTION_VIEW);
                                Uri uri = Uri.parse(updateInfo.manual);
                                intent.setData(uri);
                                context.startActivity(intent);
                            }
                        });
            }
            if (isShowStateInfo||updateInfo.lastVersionCode > versionCode) {
                if (updateInfo.lastVersionCode <= versionCode)
                    Toast.makeText(context, "当前已是最新版", Toast.LENGTH_SHORT).show();
                alertInfoDialog.create();
                alertInfoDialog.show();
            }
        }
    }

    private static void newsHandle(Context context, String result, boolean isManual) {
        Gson gson = new Gson();
        News news = gson.fromJson(result, News.class);
        if (isManual || news.id > SharedPrefs.getLastNewsId()) {
            AlertDialog.Builder alertInfoDialog = null;
            alertInfoDialog = new AlertDialog.Builder(context);
            alertInfoDialog.setTitle(R.string.newsBoard);
            alertInfoDialog.setMessage(news.ct);
            alertInfoDialog.setIcon(R.mipmap.ic_launcher);
            if(news.fun==1) {
                alertInfoDialog.setPositiveButton(news.bn, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        Uri uri = Uri.parse(news.ag);
                        intent.setData(uri);
                        context.startActivity(intent);
                        SharedPrefs.putLastNewsId(news.id);
                    }
                });
            }else if(news.fun==2){
                alertInfoDialog.setPositiveButton(R.string.title_checkUpdate, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        checkUpdate(context,true);
                        SharedPrefs.putLastNewsId(news.id);
                    }
                });
            }else {
                alertInfoDialog.setPositiveButton(R.string.readed, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SharedPrefs.putLastNewsId(news.id);
                    }
                });
            }
            alertInfoDialog.create();
            alertInfoDialog.show();
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
        String fileName = url.substring(url.lastIndexOf('/') + 1) + ".apk";
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


    private static boolean checkCRC(String apkPath) {
        //Long dexCrc = Long.parseLong(updateInfo.crc);
        Long dexCrc = Long.parseLong("3E1BC558", 16);
        //建议将dexCrc值放在服务器做校验
        try {
            ZipFile zipfile = new ZipFile(apkPath);
            ZipEntry dexentry = zipfile.getEntry("classes.dex");
            Log.i("verification", "classes.dexcrc=" + dexentry.getCrc());
            if (dexentry.getCrc() != dexCrc) {
                Log.i("verification", "Dexhas been modified!");
                return false;
            } else {
                Log.i("verification", "Dex hasn't been modified!");
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

}
