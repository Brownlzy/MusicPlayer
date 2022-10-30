package com.liux.musicplayer.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import com.blankj.utilcode.util.FileUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * 修改自 JiYaRuo on 2021/8/20.
 * 修改内容：支持缓存列表，缓存列表中存在的优先返回缓存地址
 */
public class UploadDownloadUtils {
    private static final String TAG = UploadDownloadUtils.class.getSimpleName();
    private static UploadDownloadUtils mInstance;
    public final static String DOCUMENT_PATH = Environment.getExternalStorageDirectory() + "/RecorderGuide/";
    private final Context mContext;
    public SharedPreferences prefs = null;
    private OnImageLoadListener mOnImageLoadListener;
    private static List<MyCache> cacheList;

    public UploadDownloadUtils(Context context) {
        mContext = context;
        prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        LoadCacheList();
    }

    public void set0nImageLoadListener(OnImageLoadListener mOnImageLoadListener) {
        this.mOnImageLoadListener = mOnImageLoadListener;
    }

    public interface OnImageLoadListener {
        void onFileDownloadCompleted(ArrayList<String> array);

        default void onFileDownloading(ArrayList<String> array) {
        }

        void onFileDownloadError(ArrayList<String> array);
    }

    public static UploadDownloadUtils getInstance(Context mContext) {
        if (mInstance == null) {
            synchronized (UploadDownloadUtils.class) {
                if (mInstance == null) {
                    mInstance = new UploadDownloadUtils(mContext);
                }
            }
        }
        return mInstance;
    }

    /**
     * 下载文件
     *
     * @param path     下载路径
     * @param fileName 文件命名（加后缀名）
     * @param url      文件的url
     */
    public void downloadFile(String path, String fileName, String url) {
        new Thread(new Runnable() {
            @SuppressLint("NewApi")
            @Override
            public void run() {
                Log.e("Download", url);
                int cacheId = -1;
                cacheId = cacheList.stream().map(t -> t.url).distinct().collect(Collectors.toList()).indexOf(url);
                if (cacheId >= 0) {
                    if (FileUtils.isFileExists(path + "/" + cacheList.get(cacheId).cache)) {
                        Message message = mHandler.obtainMessage();
                        ArrayList<String> array = new ArrayList<String>();
                        array.add(url);
                        array.add(path + "/" + cacheList.get(cacheId).cache);
                        message.obj = array;
                        message.what = 2;
                        mHandler.sendMessage(message);
                        return;
                    } else {
                        cacheList.remove(cacheId);
                        saveCacheList();
                    }
                }
                try {
                    OkHttpClient client = new OkHttpClient();
                    Request request = new Request
                            .Builder()
                            .url(url)//要访问的链接
                            .get()
                            .build();
                    Call call = client.newCall(request);
                    Response execute = call.execute();
                    //获取下载的内容输入流
                    ResponseBody body = execute.body();
                    InputStream inputStream = body.byteStream();
                    final long lengh = body.contentLength();
                    // 文件保存到本地
                    File file1 = null;
                    file1 = new File(path);
                    if (!file1.exists())
                        file1.mkdirs();
                    File file = new File(file1, fileName);
                    if (!file.exists()) {
                        file.createNewFile();
                    }
                    FileOutputStream outputStream = new FileOutputStream(file);
                    int lien = 0;
                    int losing = 0;
                    byte[] bytes = new byte[1024];
                    while ((lien = inputStream.read(bytes)) != -1) {
                        outputStream.write(bytes, 0, lien);
                        losing += lien;
                        final float i = losing * 1.0f / lengh;
//                        if (i == 1.0) {
//                            //Log.i(TAG, "downloadDocument" + "下载完成" + fileName);
//                        } else {
//                            //LogUtils.i(TAG,"downloadDocument","下载进度=="+i);
//                        }
                        Message message = mHandler.obtainMessage();
                        ArrayList<String> array = new ArrayList<String>();
                        array.add(url);
                        array.add(String.valueOf(i));
                        message.obj = array;
                        message.what = 6;
                        mHandler.sendMessage(message);
                    }
                    outputStream.flush();
                    inputStream.close();
                    outputStream.close();
                    //发送信息
                    Message message = mHandler.obtainMessage();
                    ArrayList<String> array = new ArrayList<String>();
                    array.add(url);
                    array.add(file.getAbsolutePath());
                    message.obj = array;
                    message.what = 2;
                    Log.i(TAG, "downloadDocument" + "下载完成" + fileName);
                    mHandler.sendMessage(message);
                    //存入缓存列表
                    cacheList.add(new MyCache(url, fileName));
                    saveCacheList();
                } catch (IOException e) {
                    e.printStackTrace();
                    ArrayList<String> array = new ArrayList<String>();
                    array.add(url);
                    Message message = mHandler.obtainMessage();
                    message.what = 3;
                    message.obj = array;
                    mHandler.sendMessage(message);
                }
            }
        }).start();
    }
 /**
  * 加载缓存列表
  */
    private void LoadCacheList() {
        if (cacheList == null) {
            String strCacheList = prefs.getString("cacheList", "[]");
            Gson gson = new Gson();
            Type cacheListType = new TypeToken<ArrayList<MyCache>>() {
            }.getType();
            cacheList = gson.fromJson(strCacheList, cacheListType);
            if (cacheList == null)
                cacheList = new ArrayList<>();
        }
    }
 /**
  * 保存缓存列表
  */
    private void saveCacheList() {
        Gson gson = new Gson();
        Type cacheListType = new TypeToken<ArrayList<MyCache>>() {
        }.getType();
        String strCacheListJson = gson.toJson(cacheList, cacheListType);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("cacheList", strCacheListJson);
        editor.apply();
    }
/** 处理OkHttp线程的下载信息 */
    Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 2: //下载完成
                    if (mOnImageLoadListener != null) {
                        mOnImageLoadListener.onFileDownloadCompleted((ArrayList<String>) msg.obj);
                    }
                    break;
                case 3: //下载失败
                    if (mOnImageLoadListener != null) {
                        mOnImageLoadListener.onFileDownloadError((ArrayList<String>) msg.obj);
                    }
                    break;
                case 6: //正在下载
                    if (mOnImageLoadListener != null) {
                        mOnImageLoadListener.onFileDownloading((ArrayList<String>) msg.obj);
                    }
                    break;
            }
        }
    };

    /**
     * 销毁
     */
    public void onDestroy() {
        mOnImageLoadListener = null;
        mInstance = null;
    }
 /**
  * 缓存类
  * @author         Brownlzy
  */
    private class MyCache {
        String url = "";
        String cache = "";

        public MyCache(String url, String cache) {
            this.url = url;
            this.cache = cache;
        }
    }
}
