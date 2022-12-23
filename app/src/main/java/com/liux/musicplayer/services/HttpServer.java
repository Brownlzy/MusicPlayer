package com.liux.musicplayer.services;

import android.content.res.AssetManager;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.blankj.utilcode.util.FileUtils;
import com.google.gson.Gson;
import com.liux.musicplayer.media.MusicLibrary;
import com.liux.musicplayer.models.API;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class HttpServer extends NanoHTTPD {
    private static final String TAG = "HttpServer";
    private final Gson mGson = new Gson();
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
                                MusicLibrary.getSongListByName((param.get("name")).get(0)),
                                "请求成功！"
                        );
                    case Config.HTTP_API_SONG:
                        API.SongDetail songDetail = new API.SongDetail(MusicLibrary.querySong(param.get("path").get(0)));
                        return responseJsonString(
                                200,
                                songDetail,
                                "请求成功！"
                        );
                    case Config.HTTP_API_FILE:
                        String file = param.get("path").get(0);
                        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(file));
                        if (FileUtils.isFileExists(file)) {
                            FileInputStream fis = null;
                            try {
                                fis = new FileInputStream(file);
                                fis.skip(Long.parseLong(session.getHeaders().get("range").split("bytes=")[1].split("-")[0]));
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

    private <T> Response responseJsonString(int code, T data, String msg) {
        MyResponse response = new MyResponse(code, data, msg);
        String jsonString = mGson.toJson(response);
        Log.i(TAG, "responseJsonString: " + jsonString);
        return newFixedLengthResponse(Response.Status.OK, "application/json", jsonString);//返回对应的响应体Response
    }

    public static class Config {
        public static final int HTTP_PORT = 8066;
        public static final String HTTP_URL = "http://IP:" + HTTP_PORT + "/";
        public static final String HTTP_API_HELLO_WORLD = "/api/hello_world";
        public static final String HTTP_API_SONG_LIST_LIST = "/api/allsonglist";
        public static final String HTTP_API_SONG_LIST = "/api/songlist";
        public static final String HTTP_API_SONG = "/api/song";
        public static final String HTTP_API_SONG_PIC = "/api/song_pic";
        public static final String HTTP_API_SONG_LYRIC = "/api/song_lyric";
        public static final String HTTP_API_FILE = "/api/file";
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
