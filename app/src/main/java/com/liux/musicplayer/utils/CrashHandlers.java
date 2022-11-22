package com.liux.musicplayer.utils;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.preference.PreferenceManager;

import com.blankj.utilcode.util.TimeUtils;
import com.liux.musicplayer.BuildConfig;
import com.liux.musicplayer.R;
import com.liux.musicplayer.activities.MainActivity;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.Field;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Forrest.
 * User: Administrator
 * Date: 2021/9/30
 * Description:
 */
public class CrashHandlers implements UncaughtExceptionHandler {

    public static final String TAG = "CrashHandler";

    // 系统默认的UncaughtException处理类
    private UncaughtExceptionHandler mDefaultHandler;
    // CrashHandler实例
    private static CrashHandlers INSTANCE = new CrashHandlers();
    // 程序的Context对象
    private Context mContext;
    // 用来存储设备信息和异常信息
    private Map<String, String> infos = new HashMap<String, String>();

    /**
     * 保证只有一个CrashHandler实例
     */
    private CrashHandlers() {
    }

    /**
     * 获取CrashHandler实例 ,单例模式
     */
    public static CrashHandlers getInstance() {
        return INSTANCE;
    }

    public static void checkIfExistsLastCrash(Context context,boolean isManual) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean isCrashed = prefs.getBoolean("lastCrash", false);
        String fileName = prefs.getString("lastErrorLog", "null");
        if (isCrashed||isManual) {
            SharedPreferences.Editor editor = prefs.edit();
            AlertDialog alertInfoDialog = new AlertDialog.Builder(context)
                    .setTitle(R.string.title_crashed)
                    .setMessage(context.getString(R.string.crashed_info))
                    .setIcon(R.mipmap.ic_launcher)
                    .setPositiveButton(R.string.send_error_log, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            shareErrorLog(fileName, false,context);
                            editor.putBoolean("lastCrash", false);
                            editor.apply();
                        }
                    })
                    .setNegativeButton(R.string.send_error_log_by_mail, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            shareErrorLog(fileName,true, context);
                            editor.putBoolean("lastCrash", false);
                            editor.apply();
                        }
                    })
                    .setNeutralButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            editor.putBoolean("lastCrash", false);
                            editor.apply();
                        }
                    })
                    .create();
            alertInfoDialog.show();
            editor.apply();
        }
    }

    /**
     * 初始化
     *
     * @param context
     */
    public void init(Context context) {
        Log.e("CrashHandler", "...........................2");
        mContext = context;
        // 获取系统默认的UncaughtException处理器
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        // 设置该CrashHandler为程序的默认处理器
        Thread.setDefaultUncaughtExceptionHandler(this);
    }


    /**
     * 当UncaughtException发生时会转入该函数来处理
     */
    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        if (!handleException(ex) && mDefaultHandler != null) {
            // 如果用户没有处理则让系统默认的异常处理器来处理
            mDefaultHandler.uncaughtException(thread, ex);
        } else {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Log.e(TAG, "error : ", e);
            }
            // 退出程序
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(1);
        }
    }

    /**
     * 自定义错误处理,收集错误信息 发送错误报告等操作均在此完成.
     *
     * @param ex
     * @return true:如果处理了该异常信息;否则返回false.
     */
    private boolean handleException(Throwable ex) {
        if (ex == null) {
            return false;
        }
        // 收集设备参数信息
        collectDeviceInfo(mContext);
        // 保存日志文件
        String fileName = "MPErrorLog-" + TimeUtils.getNowMills() + ".log";
        savePreference(fileName);
        saveCrashInfo2File(ex, fileName);
        //  自动分享错误日志
        //shareErrorLog(fileName);
        return true;
    }

    private void savePreference(String fileName) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("lastCrash", true);
        editor.putString("lastErrorLog", fileName);
        editor.apply();
    }

    public static void shareErrorLog(String fileName,boolean isMail, Context mContext) {
        Uri uri = FileProvider.getUriForFile(mContext, mContext.getPackageName(), new File(mContext.getExternalCacheDir().getPath() + "/log/" + fileName));
        Log.e("CrashHandler", uri.getPath());
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        if(isMail){
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "[BrownMusic]"+ BuildConfig.VERSION_NAME+" BugReport");
            shareIntent.putExtra(Intent.EXTRA_TEXT, "收件人请填:\n\nbrownmusicplayer@outlook.com\n\n错误报告已附上，烦请您在下方描述出现问题时的情形,这将有助于帮助我解决问题：\n=======================");
            shareIntent.putExtra(Intent.EXTRA_EMAIL, "brownmusicplayer@outlook.com");
            shareIntent.setType("text/plain");
            shareIntent.setType("message/rfc882");
        }else {
            shareIntent.setType("text/plain");
        }
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        mContext.startActivity(Intent.createChooser(shareIntent, "分享错误日志"));
    }

    /**
     * 收集设备参数信息
     *
     * @param ctx
     */
    public void collectDeviceInfo(Context ctx) {
        try {
            PackageManager pm = ctx.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(ctx.getPackageName(), PackageManager.GET_ACTIVITIES);
            if (pi != null) {
                String versionName = pi.versionName == null ? "null" : pi.versionName;
                String versionCode = pi.versionCode + "";
                infos.put("versionName", versionName);
                infos.put("versionCode", versionCode);
            }
        } catch (NameNotFoundException e) {
            Log.e(TAG, "an error occured when collect package info", e);
        }
        Field[] fields = Build.class.getDeclaredFields();
        for (Field field : fields) {
            try {
                field.setAccessible(true);
                infos.put(field.getName(), field.get(null).toString());
                Log.d(TAG, field.getName() + " : " + field.get(null));
            } catch (Exception e) {
                Log.e(TAG, "an error occured when collect crash info", e);
            }
        }
    }

    /**
     * 保存错误信息到文件中
     *
     * @param ex
     * @return 返回文件名称, 便于将文件传送到服务器
     */
    private String saveCrashInfo2File(Throwable ex, String fileName) {
        Log.e(TAG, "------1" + ex.getMessage());
        StringBuffer sb = new StringBuffer();
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);
        String time = year + "-" + month + "-" + day + " " + hour + ":" + minute + ":" + second;
        sb.append("\r\n");
        sb.append("\r\n");
        sb.append("\r\n");
        sb.append("************************************************" + time + "****************************************" + "\r\n");
        sb.append("\r\n");
        sb.append("User:"+(User.isLogin?User.getUserHash(User.userData.userName):"UnLogin"));
        sb.append("\r\n");
        for (Map.Entry<String, String> entry : infos.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            sb.append(key + "=" + value + "\n");
        }

        Writer writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        ex.printStackTrace(printWriter);
        Throwable cause = ex.getCause();
        while (cause != null) {
            cause.printStackTrace(printWriter);
            cause = cause.getCause();
        }
        printWriter.close();
        String result = writer.toString();
        Log.e("CrashHandler", result);
        sb.append(result);
        try {
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                String path;//= Environment.getExternalStorageDirectory() + "/errorLog";
                path = mContext.getExternalCacheDir().getPath() + "/log";
                Log.e(TAG, "-----2" + path);
                File dir = new File(path);
                if (!dir.exists()) {
                    dir.mkdir();
                }
                File file = new File(path + File.separator + fileName);
                if (!file.exists()) {
                    file.createNewFile();
                }
                FileWriter fileWriter = new FileWriter(file, true);
                fileWriter.write(sb.toString());
                fileWriter.close();
            }
            return fileName;
        } catch (Exception e) {
            return null;
        }
    }
}
