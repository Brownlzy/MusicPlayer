package com.liux.musicplayer.utils;

import android.app.AppOpsManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.core.content.ContextCompat;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 跳转自启动页面解决方案
 *
 * @author zhj
 */
public class PermissionUtils {

    private static HashMap<String, List<String>> hashMap = new HashMap<String, List<String>>() {
        {
            put("Xiaomi", Arrays.asList(
                    "com.miui.securitycenter/com.miui.permcenter.autostart.AutoStartManagementActivity",
                    "com.miui.securitycenter"
            ));
            put("samsung", Arrays.asList(
                    "com.samsung.android.sm_cn/com.samsung.android.sm.ui.ram.AutoRunActivity",
                    "com.samsung.android.sm_cn/com.samsung.android.sm.ui.appmanagement.AppManagementActivity",
                    "com.samsung.android.sm_cn/com.samsung.android.sm.ui.cstyleboard.SmartManagerDashBoardActivity",
                    "com.samsung.android.sm_cn/.ui.ram.RamActivity",
                    "com.samsung.android.sm_cn/.app.dashboard.SmartManagerDashBoardActivity",

                    "com.samsung.android.sm/com.samsung.android.sm.ui.ram.AutoRunActivity",
                    "com.samsung.android.sm/com.samsung.android.sm.ui.appmanagement.AppManagementActivity",
                    "com.samsung.android.sm/com.samsung.android.sm.ui.cstyleboard.SmartManagerDashBoardActivity",
                    "com.samsung.android.sm/.ui.ram.RamActivity",
                    "com.samsung.android.sm/.app.dashboard.SmartManagerDashBoardActivity",

                    "com.samsung.android.lool/com.samsung.android.sm.ui.battery.BatteryActivity",
                    "com.samsung.android.sm_cn",
                    "com.samsung.android.sm"
            ));
            put("HUAWEI", Arrays.asList(
                    "com.huawei.systemmanager/.appcontrol.activity.StartupAppControlActivity",
                    "com.huawei.systemmanager/.startupmgr.ui.StartupNormalAppListActivity",
                    "com.huawei.systemmanager/.optimize.process.ProtectActivity",
                    "com.huawei.systemmanager/.optimize.bootstart.BootStartActivity",
                    "com.huawei.systemmanager"
            ));
            put("vivo", Arrays.asList(
                    "com.iqoo.secure/.ui.phoneoptimize.BgStartUpManager",
                    "com.iqoo.secure/.safeguard.PurviewTabActivity",
                    "com.vivo.permissionmanager/.activity.BgStartUpManagerActivity",
//                    "com.iqoo.secure/.ui.phoneoptimize.AddWhiteListActivity", //这是白名单, 不是自启动
                    "com.iqoo.secure",
                    "com.vivo.permissionmanager"
            ));
            put("Meizu", Arrays.asList(
                    "com.meizu.safe/.permission.SmartBGActivity",
                    "com.meizu.safe/.permission.PermissionMainActivity",
                    "com.meizu.safe"
            ));
            put("OPPO", Arrays.asList(
                    "com.coloros.safecenter/.startupapp.StartupAppListActivity",
                    "com.coloros.safecenter/.permission.startup.StartupAppListActivity",
                    "com.oppo.safe/.permission.startup.StartupAppListActivity",
                    "com.coloros.oppoguardelf/com.coloros.powermanager.fuelgaue.PowerUsageModelActivity",
                    "com.coloros.safecenter/com.coloros.privacypermissionsentry.PermissionTopActivity",
                    "com.coloros.safecenter",
                    "com.oppo.safe",
                    "com.coloros.oppoguardelf"
            ));
            put("oneplus", Arrays.asList(
                    "com.oneplus.security/.chainlaunch.view.ChainLaunchAppListActivity",
                    "com.oneplus.security"
            ));
            put("letv", Arrays.asList(
                    "com.letv.android.letvsafe/.AutobootManageActivity",
                    "com.letv.android.letvsafe/.BackgroundAppManageActivity",
                    "com.letv.android.letvsafe"
            ));
            put("zte", Arrays.asList(
                    "com.zte.heartyservice/.autorun.AppAutoRunManager",
                    "com.zte.heartyservice"
            ));
            //金立
            put("F", Arrays.asList(
                    "com.gionee.softmanager/.MainActivity",
                    "com.gionee.softmanager"
            ));
            //以下为未确定(厂商名也不确定)
            put("smartisanos", Arrays.asList(
                    "com.smartisanos.security/.invokeHistory.InvokeHistoryActivity",
                    "com.smartisanos.security"
            ));
            //360
            put("360", Arrays.asList(
                    "com.yulong.android.coolsafe/.ui.activity.autorun.AutoRunListActivity",
                    "com.yulong.android.coolsafe"
            ));
            //360
            put("ulong", Arrays.asList(
                    "com.yulong.android.coolsafe/.ui.activity.autorun.AutoRunListActivity",
                    "com.yulong.android.coolsafe"
            ));
            //酷派
            put("coolpad"/*厂商名称不确定是否正确*/, Arrays.asList(
                    "com.yulong.android.security/com.yulong.android.seccenter.tabbarmain",
                    "com.yulong.android.security"
            ));
            //联想
            put("lenovo"/*厂商名称不确定是否正确*/, Arrays.asList(
                    "com.lenovo.security/.purebackground.PureBackgroundActivity",
                    "com.lenovo.security"
            ));
            put("htc"/*厂商名称不确定是否正确*/, Arrays.asList(
                    "com.htc.pitroad/.landingpage.activity.LandingPageActivity",
                    "com.htc.pitroad"
            ));
            //华硕
            put("asus"/*厂商名称不确定是否正确*/, Arrays.asList(
                    "com.asus.mobilemanager/.MainActivity",
                    "com.asus.mobilemanager"
            ));
            //酷派
            put("YuLong", Arrays.asList(
                    "com.yulong.android.softmanager/.SpeedupActivity",
                    "com.yulong.android.security/com.yulong.android.seccenter.tabbarmain",
                    "com.yulong.android.security"
            ));
            // 荣耀 实测机型（HONOR 50 SE）
            put("HONOR", Arrays.asList(
                    "com.hihonor.systemmanager/com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity",
                    "com.hihonor.systemmanager"
            ));
        }
    };

    public static void startToAutoStartSetting(Context context) {
        Log.e("Util", "******************当前手机型号为：" + Build.MANUFACTURER);
        Set<Map.Entry<String, List<String>>> entries = hashMap.entrySet();
        for (Map.Entry<String, List<String>> entry : entries) {
            String manufacturer = entry.getKey();
            List<String> actCompatList = entry.getValue();
            if (Build.MANUFACTURER.equalsIgnoreCase(manufacturer)) {
                for (String act : actCompatList) {
                    try {
                        Intent intent;
                        if (act.contains("/")) {
                            intent = new Intent();
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            ComponentName componentName = ComponentName.unflattenFromString(act);
                            intent.setComponent(componentName);
                        } else {
                            //所以我是直接跳转到对应的安全管家/安全中心
                            intent = context.getPackageManager().getLaunchIntentForPackage(act);
                        }
                        context.startActivity(intent);
                        break;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
    //检查权限是否获取成功
    public static boolean checkPermission(Context context,String permission) {
        //拒绝修改开关
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }

    //判断是否开启悬浮窗权限   context可以用你的Activity.或者this
    public static boolean checkFloatPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AppOpsManager appOpsMgr = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
            if (appOpsMgr == null)
                return false;
            int mode = appOpsMgr.checkOpNoThrow("android:system_alert_window", android.os.Process.myUid(), context
                    .getPackageName());
            return mode == AppOpsManager.MODE_ALLOWED;
        } else {
            return Settings.canDrawOverlays(context);
        }
    }

    //请求权限
    public static boolean askPermission(Context context, String permission, ActivityResultLauncher<String> requestPermissionLauncher) {
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            //Toast.makeText(getActivity(), R.string.permission_granted, Toast.LENGTH_LONG).show();
            return true;
        } else {
            requestPermissionLauncher.launch(permission);
            //Toast.makeText(getActivity(), R.string.asking_permission, Toast.LENGTH_LONG).show();
            return false;
        }
    }

    //权限打开
    public static boolean requestSettingCanDrawOverlays(Context context,ActivityResultLauncher<Intent> requestOverlayPermissionLauncher) {
        if (checkFloatPermission(context))
            return true;
        else {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
            requestOverlayPermissionLauncher.launch(intent);
            return false;
        }
    }

}