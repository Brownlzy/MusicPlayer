package com.liux.musicplayer.receiver;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;

/**
 * 监听蓝牙设备连接状态
 */
public class BluetoothStateReceiver extends BroadcastReceiver {
    private final int PROFILE_HEADSET = 0;
    private final int PROFILE_A2DP = 1;
    private final int PROFILE_OPP = 2;
    private final int PROFILE_HID = 3;
    private final int PROFILE_PANU = 4;
    private final int PROFILE_NAP = 5;
    private final int PROFILE_A2DP_SINK = 6;

    @Override
    public void onReceive(Context context, Intent intent) {
        switch (intent.getAction()) {
            case BluetoothDevice.ACTION_ACL_CONNECTED:
                //蓝牙已连接,如果不需要判断是否为音频设备,下面代码块的判断可以不要
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device == null) return;
                //如果在Android32 SDK还需要声明 android.permission.BLUETOOTH_CONNECT权限
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    // TO-DO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                if (isHeadPhone(device.getBluetoothClass())) {
                    Log.d("BluetoothStateReceiver", "蓝牙音频设备已连接");
                    iBluetoothStateListener.onBluetoothDeviceConnected();
                }
                break;

            case BluetoothAdapter.ACTION_STATE_CHANGED:
                //蓝牙状态发生改变,断开,正在扫描,正在连接
                int blueState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
                if (blueState == BluetoothAdapter.STATE_OFF) {
                    Log.d("BluetoothStateReceiver", "蓝牙设备状态改变");
                    iBluetoothStateListener.onBluetoothDeviceDisconnected();
                }
                break;
            case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                Log.d("BluetoothStateReceiver", "蓝牙设备已断开");
                iBluetoothStateListener.onBluetoothDeviceDisconnected();
                break;
        }
    }

    private boolean isHeadPhone(BluetoothClass bluetoothClass) {
        if (bluetoothClass == null) {
            return false;
        }
        if (doesClassMatch(bluetoothClass, PROFILE_HEADSET))
            return true;
        else
            return doesClassMatch(bluetoothClass, PROFILE_A2DP);
    }

    /**
     * Check class bits for possible bluetooth profile support.
     * This is a simple heuristic that tries to guess if a device with the
     * given class bits might support specified profile. It is not accurate for all
     * devices. It tries to err on the side of false positives.
     *
     * @param profile The profile to be checked
     * @return True if this device might support specified profile.
     * @hide
     */
    public boolean doesClassMatch(BluetoothClass bluetoothClass, int profile) {
        if (profile == PROFILE_A2DP) {
            if (bluetoothClass.hasService(BluetoothClass.Service.RENDER)) {
                return true;
            }
            // By the A2DP spec, sinks must indicate the RENDER service.
            // However we found some that do not (Chordette). So lets also
            // match on some other class bits.
            switch (bluetoothClass.getDeviceClass()) {
                case BluetoothClass.Device.AUDIO_VIDEO_HIFI_AUDIO:
                case BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES:
                case BluetoothClass.Device.AUDIO_VIDEO_LOUDSPEAKER:
                case BluetoothClass.Device.AUDIO_VIDEO_CAR_AUDIO:
                    return true;
                default:
                    return false;
            }
        } else if (profile == PROFILE_A2DP_SINK) {
            if (bluetoothClass.hasService(BluetoothClass.Service.CAPTURE)) {
                return true;
            }
            // By the A2DP spec, srcs must indicate the CAPTURE service.
            // However if some device that do not, we try to
            // match on some other class bits.
            switch (bluetoothClass.getDeviceClass()) {
                case BluetoothClass.Device.AUDIO_VIDEO_HIFI_AUDIO:
                case BluetoothClass.Device.AUDIO_VIDEO_SET_TOP_BOX:
                case BluetoothClass.Device.AUDIO_VIDEO_VCR:
                    return true;
                default:
                    return false;
            }
        } else if (profile == PROFILE_HEADSET) {
            // The render service class is required by the spec for HFP, so is a
            // pretty good signal
            if (bluetoothClass.hasService(BluetoothClass.Service.RENDER)) {
                return true;
            }
            // Just in case they forgot the render service class
            switch (bluetoothClass.getDeviceClass()) {
                case BluetoothClass.Device.AUDIO_VIDEO_HANDSFREE:
                case BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET:
                case BluetoothClass.Device.AUDIO_VIDEO_CAR_AUDIO:
                    return true;
                default:
                    return false;
            }
        } else if (profile == PROFILE_OPP) {
            if (bluetoothClass.hasService(BluetoothClass.Service.OBJECT_TRANSFER)) {
                return true;
            }

            switch (bluetoothClass.getDeviceClass()) {
                case BluetoothClass.Device.COMPUTER_UNCATEGORIZED:
                case BluetoothClass.Device.COMPUTER_DESKTOP:
                case BluetoothClass.Device.COMPUTER_SERVER:
                case BluetoothClass.Device.COMPUTER_LAPTOP:
                case BluetoothClass.Device.COMPUTER_HANDHELD_PC_PDA:
                case BluetoothClass.Device.COMPUTER_PALM_SIZE_PC_PDA:
                case BluetoothClass.Device.COMPUTER_WEARABLE:
                case BluetoothClass.Device.PHONE_UNCATEGORIZED:
                case BluetoothClass.Device.PHONE_CELLULAR:
                case BluetoothClass.Device.PHONE_CORDLESS:
                case BluetoothClass.Device.PHONE_SMART:
                case BluetoothClass.Device.PHONE_MODEM_OR_GATEWAY:
                case BluetoothClass.Device.PHONE_ISDN:
                    return true;
                default:
                    return false;
            }
        } else if (profile == PROFILE_HID) {
            return (bluetoothClass.getDeviceClass() & BluetoothClass.Device.Major.PERIPHERAL) == BluetoothClass.Device.Major.PERIPHERAL;
        } else if (profile == PROFILE_PANU || profile == PROFILE_NAP) {
            // No good way to distinguish between the two, based on class bits.
            if (bluetoothClass.hasService(BluetoothClass.Service.NETWORKING)) {
                return true;
            }
            return (bluetoothClass.getDeviceClass() & BluetoothClass.Device.Major.NETWORKING) == BluetoothClass.Device.Major.NETWORKING;
        } else {
            return false;
        }
    }

    public interface IBluetoothStateListener {
        void onBluetoothDeviceConnected();

        void onBluetoothDeviceDisconnected();
    }

    private IBluetoothStateListener iBluetoothStateListener;

    public void setIBluetoothStateListener(IBluetoothStateListener iBluetoothStateListener) {
        this.iBluetoothStateListener = iBluetoothStateListener;
    }
}