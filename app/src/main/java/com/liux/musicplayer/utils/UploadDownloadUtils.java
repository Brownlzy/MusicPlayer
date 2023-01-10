package com.liux.musicplayer.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import com.blankj.utilcode.util.FileUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Create by JiYaRuo on 2021/8/20.
 * Describe:
 */
public class UploadDownloadUtils {
    private static final String TAG = UploadDownloadUtils.class.getSimpleName();
    private static UploadDownloadUtils mInstance;
    public final static String DOCUMENT_PATH = Environment.getExternalStorageDirectory() + "/RecorderGuide/";
    private OnImageLoadListener mOnImageLoadListener;
    private static List<MyCache> cacheList;
    private static HashMap<String,String> cacheMap=new HashMap<>();

    public UploadDownloadUtils(Context context) {
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
                Log.e("Download", url+"-"+fileName);
                if (cacheMap.containsKey(url)) {
                    if (FileUtils.isFileExists(path + "/" + cacheMap.get(url))) {
                        Message message = mHandler.obtainMessage();
                        ArrayList<String> array = new ArrayList<String>();
                        array.add(url);
                        array.add(path + "/" + cacheMap.get(url));
                        message.obj = array;
                        message.what = 2;
                        mHandler.sendMessage(message);
                        return;
                    } else {
                        cacheMap.remove(url);
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
//                    LogUtils.i(TAG,"downloadDocument","文件大小=="+lengh);
                    // 文件保存到本地
                    File file1 = null;
                    file1 = new File(path);
                    if (!file1.exists())
                        file1.mkdirs();
//                    LogUtils.i(TAG,"downloadDocument","文件名称=="+fileName);
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
                        if (i == 1.0) {
                            //Log.i(TAG, "downloadDocument" + "下载完成" + fileName);
                        } else {
                            //LogUtils.i(TAG,"downloadDocument","下载进度=="+i);
                        }
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
                    Message message = mHandler.obtainMessage();
                    ArrayList<String> array = new ArrayList<String>();
                    array.add(url);
                    array.add(file.getAbsolutePath());
                    message.obj = array;
                    message.what = 2;
                    Log.i(TAG, "downloadDocument" + "下载完成" + fileName);
                    mHandler.sendMessage(message);
                    cacheMap.put(url, fileName);
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

    private void LoadCacheList() {
        if (cacheList == null) {
            String strCacheList = SharedPrefs.getCacheList("[]");
            Gson gson = new Gson();
            Type cacheListType = new TypeToken<ArrayList<MyCache>>() {
            }.getType();
            cacheList = gson.fromJson(strCacheList, cacheListType);
            if (cacheList == null)
                cacheList = new ArrayList<>();
            cacheMap.clear();
            for (MyCache cache:cacheList) {
                cacheMap.put(cache.url,cache.cache);
            }
        }
    }

    private void saveCacheList() {
        cacheList.clear();
        for (String url : cacheMap.keySet()) {
            cacheList.add(new MyCache(url, cacheMap.get(url)));
        }
        Gson gson = new Gson();
        try {
            String strCacheListJson = gson.toJson(cacheList);
            SharedPrefs.putCacheList(strCacheListJson);
        } catch (Exception ignored) {

        }
    }

    Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 2:
                    if (mOnImageLoadListener != null) {
                        mOnImageLoadListener.onFileDownloadCompleted((ArrayList<String>) msg.obj);
                    }
                    break;
                case 3:
                    if (mOnImageLoadListener != null) {
                        mOnImageLoadListener.onFileDownloadError((ArrayList<String>) msg.obj);
                    }
                    break;
                case 6:
                    if (mOnImageLoadListener != null) {
                        mOnImageLoadListener.onFileDownloading((ArrayList<String>) msg.obj);
                    }
                    break;
            }
        }
    };

    /**
     * 获取文件夹下的文件
     */
    public static List<String> getLocalPic(String path) {
        List<String> mList = new ArrayList<>();
        File dir = new File(path);
        if (dir == null || !dir.exists() || !dir.isDirectory())
            return null;
        for (File file : dir.listFiles()) {
            if (file.isFile())
                mList.add(file.getPath());
        }
        return mList;
    }

    /**
     * 删除文件
     */
    public void deleteFile(String path) {
        File file = new File(path);
        deleteDirWithFile(file);
    }

    /**
     * 删除文件
     *
     * @param dir
     */
    private void deleteDirWithFile(File dir) {
        if (dir == null || !dir.exists() || !dir.isDirectory())
            return;
        for (File file : dir.listFiles()) {
            if (file.isFile())
                file.delete(); // 删除所有文件
            else if (file.isDirectory())
                deleteDirWithFile(file); // 递规的方式删除文件夹
        }
    }

    /**
     * 销毁
     */
    public void onDestroy() {
        mOnImageLoadListener = null;
        mInstance = null;
    }

    private class MyCache {
        String url = "";
        String cache = "";

        public MyCache(String url, String cache) {
            this.url = url;
            this.cache = cache;
        }
    }
}
