package com.liux.musicplayer.utils;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
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

import com.blankj.utilcode.util.FileUtils;
import com.blankj.utilcode.util.TimeUtils;
import com.google.gson.Gson;
import com.liux.musicplayer.BuildConfig;
import com.liux.musicplayer.R;
import com.liux.musicplayer.activities.MainActivity;
import com.liux.musicplayer.activities.SplashActivity;
import com.liux.musicplayer.models.UserData;
import com.liux.musicplayer.models.UserDataJson;
import com.xiasuhuei321.loadingdialog.view.LoadingDialog;

import java.io.File;
import java.io.IOException;

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
    private static LoadingDialog ld;

    public static void init(Context context) {
        userData = SharedPrefs.getUserData();
        if (userData != null && userData.isValid) {
            userHash = SHA256Util.getSHA256StrJava(userData.userName + HASH_SALT);
            try {
                if (Math.abs(TimeUtils.getNowMills() - Long.parseLong(userData.loginTime)) > 1209600000L
                        || Long.parseLong(userData.expired) < TimeUtils.getNowMills()
                        || (!BuildConfig.DEBUG && !userHash.equals(RSAUtils.publicKeyDecrypt(userData.userHashRSA, userData.publicKey)))
                ) {
                    String userName=userData.userName;
                    userData = new UserData();
                    SharedPrefs.saveUserData(userData);
                    login(context,userName,true);
                    Toast.makeText(context, "?????????????????????????????????", Toast.LENGTH_SHORT).show();
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
        ld = new LoadingDialog(context);
        ld.setLoadingText("????????????")
                .setSuccessText("????????????")//??????????????????????????????
                .setFailedText("????????????")
                .closeSuccessAnim()
                .closeFailedAnim()
                .show();
        userData.userName = userName + (BuildConfig.DEBUG ? "_DEBUG" : "");
        Log.e(TAG, userName + "  DEBUG");
        if (BuildConfig.DEBUG) {
            Gson gson = new Gson();
            String result = "{\"publicKey\":\"\",\"userHashRSA\":\"\",\"join\":\"" + TimeUtils.getNowMills() + "\",\"expired\":\"" + String.valueOf(TimeUtils.getNowMills() + 86400000) + "\",\"level\":0,\"userSplash\":\"0.jpg\"}";
            userDataJson = gson.fromJson(result, UserDataJson.class);
            if (checkResult(context, isReLogin)) {
                ld.loadSuccess();
                new Handler()
                        .postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                Intent restartIntent = new Intent(context, SplashActivity.class);
                                MainActivity.mainActivity.finish();
                                context.startActivity(restartIntent);
                                //System.exit(0);
                            }
                        }, 1000);
            } else {
                ld.loadFailed();
            }
        } else {
            userHash = SHA256Util.getSHA256StrJava(userData.userName + HASH_SALT);
            handler = new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    super.handleMessage(msg);
                    if (msg.arg1 == 0) {
                        ld.loadSuccess();
                    } else {
                        ld.loadFailed();
                    }
                    Toast.makeText(context, (String) msg.obj, Toast.LENGTH_SHORT).show();
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
                    Log.d(TAG, "??????");
                    Message message = Message.obtain();
                    message.arg1 = 1;
                    message.obj = "???????????????????????????????????????";
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
                        if (checkResult(context, isReLogin)) {
                            Message message = Message.obtain();
                            if (isReLogin) {
                                message.arg1 = 0;
                                message.obj = "?????????????????????????????????...";
                                new Handler()
                                        .postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                Intent restartIntent = new Intent(context, SplashActivity.class);
                                                MainActivity.mainActivity.finish();
                                                context.startActivity(restartIntent);
                                                //System.exit(0);
                                            }
                                        }, 1000);
                            } else {
                                message.arg1 = 0;
                                message.obj = "??????????????????????????????????????????...";
                            }
                            handler.sendMessage(message);
                        } else {
                            Message message = Message.obtain();
                            message.arg1 = 1;
                            message.obj = "??????????????????????????????";
                            handler.sendMessage(message);
                        }
                    } else {
                        Log.e(TAG, "response.code??????");
                        Message message = Message.obtain();
                        message.arg1 = 1;
                        message.obj = "?????????????????????????????????" + id + "???";
                        handler.sendMessage(message);
                    }
                }

            });
        }
    }

    private static boolean checkResult(Context context,boolean isReLogin) {
        try {
            if ((BuildConfig.DEBUG || userHash.equals(RSAUtils.publicKeyDecrypt(userDataJson.userHashRSA, userDataJson.publicKey)))
                    && Long.parseLong(userDataJson.expired) > TimeUtils.getNowMills()) {
                userData.isValid = true;
                userData.publicKey = userDataJson.publicKey;
                userData.userHashRSA = userDataJson.userHashRSA;
                userData.level = userDataJson.level;
                userData.join = userDataJson.join;
                userData.expired = userDataJson.expired;
                userData.loginTime = String.valueOf(TimeUtils.getNowMills());
                SharedPrefs.saveUserData(userData);
                User.isLogin = true;
                if (!isReLogin && !BuildConfig.DEBUG)
                    acquireDownload(context, "https://brownlzy.github.io/MyOtaInfo/MusicPlayer/pic/" + userDataJson.userSplash);
                Log.e(TAG, String.valueOf(userData));
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

//    public static void logout() {
    public static void logout(Context context) {
        User.isLogin=false;
        FileUtils.delete(new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), userData.userName));
        FileUtils.delete(new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), userData.userName+"_custom"));
        SharedPrefs.cleanSplashPath();
        userData=new UserData();
        SharedPrefs.saveUserData(userData);
        new Handler()
                .postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Intent restartIntent = new Intent(context,SplashActivity.class);
                        MainActivity.mainActivity.finish();
                        context.startActivity(restartIntent);
                        //System.exit(0);
                    }
                },1000);
    }

    public static String getUserHash(String trim) {
        return SHA256Util.getSHA256StrJava(trim + HASH_SALT);
    }

    /**
     * ?????????????????????????????????
     *
     * @param context ()
     * @return boolean ??????????????????????????????
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
                                    Toast.makeText(context, "???????????????????????????????????????????????????", Toast.LENGTH_SHORT).show();
                                    new Handler()
                                            .postDelayed(new Runnable() {
                                                @Override
                                                public void run() {
                                                    Intent restartIntent = new Intent(context,SplashActivity.class);
                                                    MainActivity.mainActivity.finish();
                                                    context.startActivity(restartIntent);
                                                    //System.exit(0);
                                                }
                                            },1000);
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

