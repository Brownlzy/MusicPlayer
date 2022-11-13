package com.liux.musicplayer.models;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;

import com.blankj.utilcode.util.FileUtils;
import com.blankj.utilcode.util.TimeUtils;
import com.google.gson.Gson;
import com.liux.musicplayer.R;
import com.liux.musicplayer.utils.RSAUtils;
import com.liux.musicplayer.utils.SHA256Util;
import com.liux.musicplayer.utils.SharedPrefs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class User {
    private static final String HASH_SALT = "musicPlayerSalt";
    public static UserData userData;
    private static UserDataJson userDataJson;
    private static String TAG = "User";
    private static String userHash;
    public static boolean isLogin = false;

    public static void init(Context context) {
        userData = SharedPrefs.getUserData();
        if (userData != null && userData.isValid) {
            userHash = SHA256Util.getSHA256StrJava(userData.userName + HASH_SALT);
            try {
                if (Math.abs(TimeUtils.getNowMills()-Long.parseLong(userData.loginTime))>1209600000L
                        ||Long.parseLong(userData.expired)<TimeUtils.getNowMills()
                        ||!userHash.equals(RSAUtils.publicKeyDecrypt(userData.userHashRSA, userData.publicKey))
                ) {
                    String userName=userData.userName;
                    userData = new UserData();
                    SharedPrefs.saveUserData(userData);
                    login(context,userName,true);
                    Toast.makeText(context, "登录过期，正在重新登录", Toast.LENGTH_SHORT).show();
                }else {
                    isLogin=true;
                }
            } catch (Exception e) {
                //userData = new UserData();
                //SharedPrefs.saveUserData(userData);
            }
        } else {
            userData = new UserData();
        }
    }

    public static void login(Context context, String userName,boolean isReLogin) {
        userData.userName = userName;
        userHash = SHA256Util.getSHA256StrJava(userName + HASH_SALT);
        Log.e(TAG, userName + "  " + userHash);

        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                Toast.makeText(context, (String)msg.obj, Toast.LENGTH_SHORT).show();
            }
        };

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("https://brownlzy.github.io/MyOtaInfo/MusicPlayer/usr/" + userHash)
                .get()//default
                .build();
        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.d(TAG, "失败");
                Message message = Message.obtain();
                message.obj = "登录失败（无法连接服务器）";
                handler.sendMessage(message);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String result = response.body().string();
                int id = response.code();
                Log.d(TAG, "onResponse: " + result);
                if (id == 200) {
                    Gson gson = new Gson();
                    userDataJson = gson.fromJson(result, UserDataJson.class);
                    if(checkResult(context,isReLogin)){
                        Message message = Message.obtain();
                        if(isReLogin)
                            message.obj = "重新登录成功！请重启本应用";
                        else
                            message.obj = "登录成功！请等待通知栏下载进度完成后重启本应用";
                        handler.sendMessage(message);
                    }else {
                        Message message = Message.obtain();
                        message.obj = "登录失败（鉴权失败）";
                        handler.sendMessage(message);
                    }
                } else {
                    Log.e(TAG,"response.code失败");
                    Message message = Message.obtain();
                    message.obj = "登录失败（无当前用户："+id+"）";
                    handler.sendMessage(message);
                }
            }

        });

    }

    private static boolean checkResult(Context context,boolean isReLogin) {
        try {
            if (userHash.equals(RSAUtils.publicKeyDecrypt(userDataJson.userHashRSA, userDataJson.publicKey))
                &&Long.parseLong(userDataJson.expired)>TimeUtils.getNowMills()) {
                userData.isValid = true;
                userData.publicKey = userDataJson.publicKey;
                userData.userHashRSA = userDataJson.userHashRSA;
                userData.level = userDataJson.level;
                userData.expired = userDataJson.expired;
                userData.loginTime = String.valueOf(TimeUtils.getNowMills());
                if(!isReLogin)
                    acquireDownload(context, "https://brownlzy.github.io/MyOtaInfo/MusicPlayer/pic/" + userDataJson.userSplash);
                SharedPrefs.saveUserData(userData);
                Log.e(TAG, String.valueOf(userData));
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void logout(Context context) {
        AlertDialog alertInfoDialog = null;
        alertInfoDialog = new AlertDialog.Builder(context)
                .setTitle(R.string.sure)
                .setMessage(R.string.sure_to_logout)
                .setIcon(R.drawable.ic_round_account_circle_24)
                .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        User.isLogin=false;
                        FileUtils.delete(new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), userData.userName));
                        userData=new UserData();
                        SharedPrefs.saveUserData(userData);
                        new Handler()
                                .postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        System.exit(0);
                                    }
                                },1000);
                    }
                })
                .setNeutralButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .create();
        alertInfoDialog.show();
    }

    public static String getUserHash(String trim) {
        return SHA256Util.getSHA256StrJava(trim + HASH_SALT);
    }

    public static class UserData {
        public boolean isValid = false;
        public String userName = "";
        public String publicKey = "";
        public String userHashRSA = "";
        public int level = 100;
        public String loginTime = "";
        public String expired = "";
    }

    public static class UserDataJson {
        public String publicKey = "";
        public String userHashRSA;
        public String expired;
        public int level;
        public String userSplash;
    }

    /**
     * 调用系统下载器下载文件
     *
     * @param context ()
     * @return boolean 是否成功调用系统下载
     */
    private static boolean acquireDownload(Context context, String url) {
        Log.i(TAG + "//acquireDownload()", "Download requested");
        String fileName = userData.userName;
        File localFile = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), fileName);
        if (localFile.exists()) {
            return true;
        } else {
            Log.d(TAG + "//acquireDownload()", "Download file");
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setTitle(context.getResources().getString(R.string.app_name));
            request.setDescription(context.getResources().getString(R.string.notification_downloading_splash_pic));
            request.setVisibleInDownloadsUi(true);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
            File cloudFile = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), fileName);
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
                                    //Toast.makeText(context, downloadFileName, Toast.LENGTH_SHORT).show();
                                    Toast.makeText(context, "专属开屏页图片下载完成，现在可以重启！", Toast.LENGTH_SHORT).show();

                                    //android.os.Process.killProcess(android.os.Process.myPid());
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


    private static Handler handler;
}
