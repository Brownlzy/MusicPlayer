package com.liux.musicplayer.services;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.blankj.utilcode.util.FileUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.liux.musicplayer.media.MusicLibrary;
import com.liux.musicplayer.models.API;
import com.liux.musicplayer.utils.MusicUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class HttpServer extends NanoHTTPD {
    private static final String TAG = "HttpServer";
    private final Gson mGson = new GsonBuilder().disableHtmlEscaping().create();
    AssetManager asset_mgr;
    private int count = 0;

    public HttpServer(String hostname, int port) {
        super(hostname, port);
    }

    @Override
    public Response serve(IHTTPSession session) {
        return dealWith(session);
    }

    public Response dealWith(IHTTPSession session) {
        Log.i(TAG, "dealWith: session.uri = " + session.getUri() + ", method = " + session.getMethod() + ", header = " + session.getHeaders() + ", " +
                "params = " + session.getParameters() + "");
        String url = session.getUri();

        //响应get请求
        if (Method.GET == session.getMethod()) {
            Log.i(TAG, "dealWith: " + url);
            if (url.startsWith("/api/")) {
                count++;
                Map<String, List<String>> param = session.getParameters();
                switch (url) {
                    case Config.HTTP_API_HELLO_WORLD:
                        return responseJsonString(
                                200,
                                "Hello World!",
                                "请求成功！"
                        );
                    case Config.HTTP_API_INFO:
                        return responseJsonString(
                                200,
                                new API.Info(),
                                "请求成功！"
                        );
                    case Config.HTTP_API_SONG_LIST_LIST:
                        API.SongListList data = new API.SongListList(MusicLibrary.getAllSongListList());
                        return responseJsonString(
                                200,
                                data,
                                "请求成功！"
                        );
                    case Config.HTTP_API_SONG_LIST:
                        return responseJsonString(
                                200,
                                new API.SongList(MusicLibrary.getSongListByName((param.get("name")).get(0))),
                                "请求成功！"
                        );
                    case Config.HTTP_API_PLAYLIST:
                        return responseJsonString(
                                new API.PlayList(MusicLibrary.getSongListByName((param.get("name")).get(0))).playlist
                        );
                    case Config.HTTP_API_SONG:
                        API.SongDetail songDetail = new API.SongDetail(MusicLibrary.querySong(param.get("path").get(0)));
                        return responseJsonString(
                                200,
                                songDetail,
                                "请求成功！"
                        );
                    case Config.HTTP_API_SONG_PIC:
                        String file1 = param.get("path").get(0);
                        if (FileUtils.isFileExists(file1)) {
                            InputStream fis = null;
                            try {
                                Bitmap cover = MusicUtils.getAlbumImage(file1);
                                if (cover != null)
                                    fis = Bitmap2InputStream(cover);
                                //fis.skip(Long.parseLong(session.getHeaders().get("range").split("bytes=")[1].split("-")[0]));
                                return newFixedLengthResponse(Response.Status.OK, "image/jpeg", fis, fis.available() - 1);
                            } catch (IOException e) {
                                e.printStackTrace();
                                return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/html", "文件不存在：" + file1);
                            }
                        } else {
                            return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/html", "文件不存在：" + file1);
                        }
                    case Config.HTTP_API_FILE:
                        String file = param.get("path").get(0);
                        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(file));
                        if (FileUtils.isFileExists(file)) {
                            FileInputStream fis = null;
                            try {
                                fis = new FileInputStream(file);
                                //fis.skip(Long.parseLong(session.getHeaders().get("range").split("bytes=")[1].split("-")[0]));
                                return newFixedLengthResponse(Response.Status.OK, mimeType, fis, fis.available() - 1);
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                                return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/html", "文件不存在：" + file);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else {
                            return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/html", "文件不存在：" + file);
                        }
                }
            } else {
                if (url.endsWith("/"))
                    url = url + "index.html";
                String file_name = "wap" + url;
                String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(url));
                InputStream in = null;
                try {
                    //通过AssetManager直接打开文件进行读取操作
                    in = asset_mgr.open(file_name, AssetManager.ACCESS_BUFFER);
                } catch (IOException e) {
                    //e.printStackTrace();
                    return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/html", "INTERNAL_ERROR:" + url);
                }
                return newChunkedResponse(Response.Status.OK, mimeType, in);
            }
        }
        return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/html", "文件不存在：" + url);
    }

    // 将Bitmap转换成InputStream
    public InputStream Bitmap2InputStream(Bitmap bm) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        InputStream is = new ByteArrayInputStream(baos.toByteArray());
        return is;
    }

    private <T> Response responseJsonString(int code, T data, String msg) {
        MyResponse response = new MyResponse(code, data, msg);
        String jsonString = mGson.toJson(response);
        Log.i(TAG, "responseJsonString: " + jsonString);
        return newFixedLengthResponse(Response.Status.OK, "application/json;charset=UTF-8", jsonString);//返回对应的响应体Response
    }

    private <T> Response responseJsonString(T data) {
        String jsonString = mGson.toJson(data);
        Log.i(TAG, "responseJsonString: " + jsonString);
        return newFixedLengthResponse(Response.Status.OK, "application/json;charset=UTF-8", jsonString);//返回对应的响应体Response
    }

    public static class Config {
        public static final String HTTP_URL = "http://IP:PORT/";
        public static int HTTP_PORT = 8068;
        public static final String HTTP_API_HELLO_WORLD = "/api/hello_world";
        public static final String HTTP_API_INFO = "/api/info";
        public static final String HTTP_API_SONG_LIST_LIST = "/api/allsonglist";
        public static final String HTTP_API_SONG_LIST = "/api/songlist";
        public static final String HTTP_API_SONG = "/api/song";
        public static final String HTTP_API_SONG_PIC = "/api/cover";
        public static final String HTTP_API_SONG_LYRIC = "/api/song_lyric";
        public static final String HTTP_API_FILE = "/api/file";
        public static final String HTTP_API_PLAYLIST = "/api/playlist";
        public static String HTTP_IP = "IP";//这是我当前手机流量下的IP地址
    }

    private static class MyResponse<T> {
        int code;
        T data;
        String msg;

        public MyResponse(int code, T data, String msg) {
            this.code = code;
            this.data = data;
            this.msg = msg;
        }
    }
}