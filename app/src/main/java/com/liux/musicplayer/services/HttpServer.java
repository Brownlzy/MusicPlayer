package com.liux.musicplayer.services;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import com.blankj.utilcode.util.FileUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.liux.musicplayer.R;
import com.liux.musicplayer.media.MusicLibrary;
import com.liux.musicplayer.models.API;
import com.liux.musicplayer.utils.MusicUtils;
import com.liux.musicplayer.utils.SharedPrefs;
import com.liux.musicplayer.utils.User;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import fi.iki.elonen.NanoHTTPD;

public class HttpServer extends NanoHTTPD {
    private static final String TAG = "HttpServer";
    private final Gson mGson = new GsonBuilder().disableHtmlEscaping().create();
    AssetManager asset_mgr;
    Context mContext;

    public final static String ACCESS_CONTROL_ALLOW_HEADER_PROPERTY_NAME = "AccessControlAllowHeader";

    @Override
    public Response serve(IHTTPSession session) {
        return dealWith(session);
    }

    // explicitly relax visibility to package for tests purposes
    final static String DEFAULT_ALLOWED_HEADERS = "origin,accept,content-type";
    private final static String ALLOWED_METHODS = "GET, POST, PUT, DELETE, OPTIONS, HEAD";
    private final static int MAX_AGE = 42 * 60 * 60;

    public HttpServer(Context context, String hostname, int port) {
        super(hostname, port);
        this.mContext = context;
    }

    public Response dealWith(IHTTPSession session) {
        Log.i(TAG, "dealWith: session.uri = " + session.getUri() + ", method = " + session.getMethod() + ", header = " + session.getHeaders() + ", " +
                "params = " + session.getParameters() + "");
        String url = session.getUri();

        //响应get请求
        if (Method.GET == session.getMethod()) {
            Log.i(TAG, "dealWith: " + url);
            if (url.startsWith("/api/")) {
                try {
                    Map<String, List<String>> param = session.getParameters();
                    switch (url) {
                        case Config.HTTP_API_VERSION:
                            int versionCode = 0;
                            String versionName = "";
                            try {
                                versionName = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0).versionName;
                                versionCode = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0).versionCode;
                            } catch (PackageManager.NameNotFoundException e) {
                                e.printStackTrace();
                            }
                            return responseJsonString(
                                    200,
                                    new API.VERSION(mContext.getPackageName(), versionName, versionCode),
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
                            String listName = (Objects.requireNonNull(param.get("name"))).get(0);
                            switch (listName) {
                                case "正在播放":
                                    listName = "playingList";
                                    break;
                                case "在线列表":
                                    listName = "webAllSongList";
                                    break;
                                case "所有音乐":
                                    listName = "allSongList";
                                    break;
                            }
                            return responseJsonString(
                                    200,
                                    new API.SongList(MusicLibrary.getSongListByName(listName)),
                                    "请求成功！"
                            );
                        case Config.HTTP_API_PLAYLIST:
                            String listName2 = (Objects.requireNonNull(param.get("name"))).get(0);
                            switch (listName2) {
                                case "正在播放":
                                    listName2 = "playingList";
                                    break;
                                case "在线列表":
                                    listName2 = "webAllSongList";
                                    break;
                                case "所有音乐":
                                    listName2 = "allSongList";
                                    break;
                            }
                            return responseJsonString(
                                    new API.PlayList(MusicLibrary.getSongListByName(listName2)).playlist
                            );
                        case Config.HTTP_API_SONG:
                            API.SongDetail songDetail = new API.SongDetail(MusicLibrary.querySong(Objects.requireNonNull(param.get("path")).get(0)));
                            return responseJsonString(
                                    200,
                                    songDetail,
                                    "请求成功！"
                            );
                        case Config.HTTP_API_SONG_PIC:
                            String file1 = Objects.requireNonNull(param.get("path")).get(0);
                            InputStream is = null;
                            if (FileUtils.isFileExists(file1)) {
                                try {
                                    Bitmap cover = MusicUtils.getAlbumImage(file1);
                                    if (cover == null) {
                                        cover = drawable2Bitmap(mContext.getDrawable(R.drawable.ic_launcher_foreground));
                                    }
                                    is = Bitmap2InputStream(cover);
                                    return newFixedLengthResponse(Response.Status.OK, "image/jpeg", is, is.available());
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    return getInternalErrorResponse(file1);
                                }
                            } else {
                                try {
                                    Bitmap cover = drawable2Bitmap(mContext.getDrawable(R.drawable.ic_round_earth_24));
                                    is = Bitmap2InputStream(cover);
                                    return newFixedLengthResponse(Response.Status.OK, "image/jpeg", is, is.available());
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    return getInternalErrorResponse(file1);
                                }
                            }
                        case Config.HTTP_API_BACK_PIC:
                            int type = SharedPrefs.getSplashType();
                            Uri bguri = Uri.fromFile(new File(mContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                                    User.userData.userName + (type == 0 ? "" : "_custom")));
                            if (FileUtils.isFileExists(bguri.getPath())) {
                                FileInputStream fis = null;
                                try {
                                    fis = new FileInputStream(bguri.getPath());
                                    return newFixedLengthResponse(Response.Status.OK, "image/jpeg", fis, fis.available());
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    return getInternalErrorResponse(bguri.getPath());
                                }
                            } else {
                                return getNotFoundResponse(bguri.getPath());
                            }
                        case Config.HTTP_API_FILE:
                            String file = Objects.requireNonNull(param.get("path")).get(0);
                            String mimeTypeForFile = getMimeTypeForFile(file);
                            return serveFile(url, session.getHeaders(), new File(file), mimeTypeForFile);
                    }
                } catch (NullPointerException e) {
                    return getInternalErrorResponse("NullPointer");
                }
            } else {
                if (url.endsWith("/"))
                    url = url + "index.html";
                String file_name = "wap" + url;
                String mimeType = getMimeTypeForFile(file_name);
                InputStream in;
                try {
                    //通过AssetManager直接打开文件进行读取操作
                    in = asset_mgr.open(file_name, AssetManager.ACCESS_BUFFER);
                } catch (IOException e) {
                    return getInternalErrorResponse(url);
                }
                return newChunkedResponse(Response.Status.OK, mimeType, in);
            }
        }
        return getNotFoundResponse(url);
    }

    Response serveFile(String uri, Map<String, String> header, File file, String mime) {
        Response res;
        try {
            // Calculate etag
            String etag = Integer.toHexString((file.getAbsolutePath() + file.lastModified() + "" + file.length()).hashCode());

            // Support (simple) skipping:
            long startFrom = 0;
            long endAt = -1;
            String range = header.get("range");
            if (range != null) {
                if (range.startsWith("bytes=")) {
                    range = range.substring("bytes=".length());
                    int minus = range.indexOf('-');
                    try {
                        if (minus > 0) {
                            startFrom = Long.parseLong(range.substring(0, minus));
                            endAt = Long.parseLong(range.substring(minus + 1));
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            // get if-range header. If present, it must match etag or else we
            // should ignore the range request
            String ifRange = header.get("if-range");
            boolean headerIfRangeMissingOrMatching = (ifRange == null || etag.equals(ifRange));

            String ifNoneMatch = header.get("if-none-match");
            boolean headerIfNoneMatchPresentAndMatching = ifNoneMatch != null && ("*".equals(ifNoneMatch) || ifNoneMatch.equals(etag));

            // Change return code and add Content-Range header when skipping is
            // requested
            long fileLen = file.length();

            if (headerIfRangeMissingOrMatching && range != null && startFrom >= 0 && startFrom < fileLen) {
                // range request that matches current etag
                // and the startFrom of the range is satisfiable
                if (headerIfNoneMatchPresentAndMatching) {
                    // range request that matches current etag
                    // and the startFrom of the range is satisfiable
                    // would return range from file
                    // respond with not-modified
                    res = newFixedLengthResponse(Response.Status.NOT_MODIFIED, mime, "");
                    res.addHeader("ETag", etag);
                } else {
                    if (endAt < 0) {
                        endAt = fileLen - 1;
                    }
                    long newLen = endAt - startFrom + 1;
                    if (newLen < 0) {
                        newLen = 0;
                    }

                    FileInputStream fis = new FileInputStream(file);
                    fis.skip(startFrom);

                    res = newFixedLengthResponse(Response.Status.PARTIAL_CONTENT, mime, fis, newLen);
                    res.addHeader("Accept-Ranges", "bytes");
                    res.addHeader("Content-Length", "" + newLen);
                    res.addHeader("Content-Range", "bytes " + startFrom + "-" + endAt + "/" + fileLen);
                    res.addHeader("ETag", etag);
                }
            } else {

                if (headerIfRangeMissingOrMatching && range != null && startFrom >= fileLen) {
                    // return the size of the file
                    // 4xx responses are not trumped by if-none-match
                    res = newFixedLengthResponse(Response.Status.RANGE_NOT_SATISFIABLE, NanoHTTPD.MIME_PLAINTEXT, "");
                    res.addHeader("Content-Range", "bytes */" + fileLen);
                    res.addHeader("ETag", etag);
                } else if (range == null && headerIfNoneMatchPresentAndMatching) {
                    // full-file-fetch request
                    // would return entire file
                    // respond with not-modified
                    res = newFixedLengthResponse(Response.Status.NOT_MODIFIED, mime, "");
                    res.addHeader("ETag", etag);
                } else if (!headerIfRangeMissingOrMatching && headerIfNoneMatchPresentAndMatching) {
                    // range request that doesn't match current etag
                    // would return entire (different) file
                    // respond with not-modified

                    res = newFixedLengthResponse(Response.Status.NOT_MODIFIED, mime, "");
                    res.addHeader("ETag", etag);
                } else {
                    // supply the file
                    res = newFixedFileResponse(file, mime);
                    res.addHeader("Content-Length", "" + fileLen);
                    res.addHeader("ETag", etag);
                }
            }
        } catch (IOException ioe) {
            res = getForbiddenResponse("Reading file failed.");
        }

        return addCORSHeaders(header, res, "*");
    }

    private Response newFixedFileResponse(File file, String mime) throws FileNotFoundException {
        Response res;
        res = newFixedLengthResponse(Response.Status.OK, mime, new FileInputStream(file), (int) file.length());
        res.addHeader("Accept-Ranges", "bytes");
        return res;
    }

    protected Response getForbiddenResponse(String s) {
        return newFixedLengthResponse(Response.Status.FORBIDDEN, NanoHTTPD.MIME_PLAINTEXT, "FORBIDDEN: " + s);
    }

    protected Response getInternalErrorResponse(String s) {
        return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, "INTERNAL ERROR: " + s);
    }

    protected Response getNotFoundResponse(String s) {
        return newFixedLengthResponse(Response.Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT, "Error 404, file not found:" + s);
    }

    protected Response addCORSHeaders(Map<String, String> queryHeaders, Response resp, String cors) {
        resp.addHeader("Access-Control-Allow-Origin", cors);
        resp.addHeader("Access-Control-Allow-Headers", calculateAllowHeaders(queryHeaders));
        resp.addHeader("Access-Control-Allow-Credentials", "true");
        resp.addHeader("Access-Control-Allow-Methods", ALLOWED_METHODS);
        resp.addHeader("Access-Control-Max-Age", "" + MAX_AGE);

        return resp;
    }

    private String calculateAllowHeaders(Map<String, String> queryHeaders) {
        // here we should use the given asked headers
        // but NanoHttpd uses a Map whereas it is possible for requester to send
        // several time the same header
        // let's just use default values for this version
        return System.getProperty(ACCESS_CONTROL_ALLOW_HEADER_PROPERTY_NAME, DEFAULT_ALLOWED_HEADERS);
    }

    // 将Bitmap转换成InputStream
    public InputStream Bitmap2InputStream(Bitmap bm) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        InputStream is = new ByteArrayInputStream(baos.toByteArray());
        return is;
    }

    public Bitmap drawable2Bitmap(Drawable drawable) {
        Bitmap bitmap = Bitmap
                .createBitmap(
                        drawable.getIntrinsicWidth(),
                        drawable.getIntrinsicHeight(),
                        drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888
                                : Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight());
        drawable.draw(canvas);
        return bitmap;
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
        public static final String HTTP_API_VERSION = "/api/version";
        public static final String HTTP_API_INFO = "/api/info";
        public static final String HTTP_API_SONG_LIST_LIST = "/api/allsonglist";
        public static final String HTTP_API_SONG_LIST = "/api/songlist";
        public static final String HTTP_API_SONG = "/api/song";
        public static final String HTTP_API_SONG_PIC = "/api/cover";
        public static final String HTTP_API_BACK_PIC = "/api/bg";
        public static final String HTTP_API_SONG_LYRIC = "/api/songlyric";
        public static final String HTTP_API_FILE = "/api/file";
        public static final String HTTP_API_PLAYLIST = "/api/playlist";
        public static String HTTP_IP = "IP";
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
