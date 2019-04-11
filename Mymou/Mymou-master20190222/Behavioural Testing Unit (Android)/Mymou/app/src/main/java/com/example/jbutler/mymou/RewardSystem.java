package com.example.jbutler.mymou;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Created by jbutler on 06/12/2018.
 */

public class RewardSystem {

    public static boolean bluetoothConnection = false;
    private static Handler connectionLoopHandler;
    private static boolean bluetoothEnabled = false;
    private static String allChanOff, chanZeroOn, chanZeroOff, chanOneOn, chanOneOff, chanTwoOn,
            chanTwoOff, chanThreeOn, chanThreeOff;
    private static Context context;
    private static final int REQUEST_ENABLE_BT = 1;
    private static BluetoothAdapter btAdapter = null;
    private static BluetoothSocket btSocket = null;
    private static OutputStream outStream = null;
    //这里用https://wenku.baidu.com/view/5f1c944155270722192ef775.html 3.0蓝牙Android编程原理；
    // Replace with your devices UUID and address
    //这个UUID和address是平板设备的还是将要连接的Arduino Uno board的？地址是指蓝牙地址吗？
    //UUID 是 通用唯一识别码（Universally Unique Identifier）
    //标准的UUID格式为：xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx (8-4-4-4-12),其中x为十六进制数字；
    //这个UUID是对应每个服务类别的不同，而不是每个设备拥有一个UUID
    //网址中描述了不同服务对应的UUID,https://blog.csdn.net/zf_c_cqupt/article/details/52177723
    //本例中运用的是#蓝牙串口服务SerialPortServiceClass_UUID？我们买的同样的设备，与原文UUID一样？
    //以下是Android的BluetoothDevice.java文件中--如果您要连接蓝牙串行板，请尝试使用众所周知的SPP UUID
   /* <p>Hint: If you are connecting to a Bluetooth serial board then try
            * using the well-known SPP UUID 00001101-0000-1000-8000-00805F9B34FB.
     * However if you are connecting to an Android peer then please generate
     * your own unique UUID.*/
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");//原文UUID
    //试试蓝牙串口服务的另一个：LANAccessUsingPPPServiceClass_UUID = '{00001102-0000-1000-8000-00805F9B34FB}'
//    private static final UUID MY_UUID = UUID.fromString("00001102-0000-1000-8000-00805F9B34FB");
//    三星平板地址：蓝牙地址FC:A6:21:D8:11:E4，WLAN MAC地址:FC:A6:21:D8:11:E5，在BluetoothDevice中使用，应该是蓝牙地址；
//    private static String address = "FC:A6:21:D8:11:E4";//三星平板设备蓝牙地址
//    private static String address = "FC:A6:21:D8:11:E5";//三星平板设备MAC地址
    //蓝牙地址必须6个字节，必须大写；
    private static String address = "98:D3:81:FD:44:85";//Arduino Uno 设备连接的蓝牙接收发射的地址；
//    private static String address = "20:16:06:08:64:22";//原文蓝牙地址
//    private static String address = "48:2c:a0:be:7c:7c";//试一试小米note7
//    对于HC-05蓝牙模块，安卓中用00001101-0000-1000-8000-00805F9B34FB这个uuid来进行串口蓝牙通讯SerialPortService


    //蓝牙模块能正确连通，看看通道模块，现在的奖励交付系统连接是二通道，修改下代码，

    //Context应用程序环境中全局信息的接口，它整合了许多系统级的服务，可以用来获取应用中的类、资源，以及可以进行应用程序级的调起操作
    //由于Context本身是abstract的，所以它只负责定义需要的操作，具体的实现它并不关心
    //我们所能看到的对Context做第一层实现的，应该是位于android.app包下的ContextImpl
    public RewardSystem(Context context_in) {

        context = context_in;
        //初始化奖励通道，1代表chanZeroOn
        initialiseRewardChannelStrings();
        if (MainMenu.useBluetooth) {
            loopUntilConnected();
        }

    }
    //注册蓝牙接收器
    private static void registerBluetoothReceivers() {
            //IntentFilter翻译成中文就是“意图过滤器”，主要用来过滤隐式意图。当用户进行一项操作的时候，
            //Android系统会根据配置的 “意图过滤器” 来寻找可以响应该操作的组件，服务。
            //Android系统会进行“动作测试”，“数据测试”，“类别测试”，来寻找可以响应隐式意图的组件或服务
            IntentFilter bluetoothIntent = new IntentFilter();
            bluetoothIntent.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
            bluetoothIntent.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
            context.registerReceiver(bluetoothReceiver, bluetoothIntent);
    }

    private static void connectToBluetooth() {
        checkBluetoothEnabled();//该方法返回bluetoothEnabled=true;
        if (bluetoothEnabled) {
            establishConnection();
        }
    }

    //建立蓝牙连接；（这是在检查蓝牙连接成功之后，也就是bluetoothEnabled=true）
    private static void establishConnection() {
        Log.d("RewardSystem","Connecting to bluetooth..");//日志
        // Set up a pointer to the remote node using it's address.
        //用它的地址设立一个指向远程节点的指针；
        BluetoothDevice device = btAdapter.getRemoteDevice(address);
        try {
            btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
        } catch (IOException e) {
            Log.d("RewardSystem","Error: Could not create socket");
            return;
        }

        btAdapter.cancelDiscovery();
        //建立连接。 这将阻塞直到它连接
        //Establish the connection.  This will block until it connects.
        try {
            btSocket.connect();
            Log.d("RewardSystem", "Connected to Bluetooth");
            bluetoothConnection = true;
            registerBluetoothReceivers();
        } catch (IOException e) {
            Log.d("RewardSystem","Error: Failed to establish connection");
            try {
                btSocket.close();
            } catch (IOException e2) {
                Log.d("RewardSystem","Error: Failed to close socket");
            }
        }

        // Create data stream to talk to server.
        try {
            outStream = btSocket.getOutputStream();
        } catch (IOException e) {
            Log.d("RewardSystem","Error: Failed to create output stream");
        }
    }

    //检查蓝牙是否连接成功；
    private static void checkBluetoothEnabled() {

        btAdapter = BluetoothAdapter.getDefaultAdapter();

        if (btAdapter == null) {
            Log.d("RewardSystem","Error: No Bluetooth support found");
        } else if (!btAdapter.isEnabled()) {
            //提示用户打开蓝牙
            //Prompt user to turn on Bluetooth
            Log.d("RewardSystem", "Error: Bluetooth not enabled");
            Toast.makeText(context, "Bluetooth is disabled, please enable and restart", Toast.LENGTH_LONG).show();
            Intent enableBtIntent = new Intent(btAdapter.ACTION_REQUEST_ENABLE);
            Activity activity = (Activity) context;
            activity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            return;
        }

        bluetoothEnabled = true;
    }

    public static void stopAllChannels() {
        sendData(allChanOff);
    }

    private static String getStopString(int Ch) {
        String offString="";
        if(Ch == 0) {
            offString = chanZeroOff;
        } else if (Ch == 1) {
            offString = chanOneOff;
        } else if (Ch == 2) {
            offString = chanTwoOff;
        } else if (Ch == 3) {
            offString = chanThreeOff;
        } else {
            Log.d("RewardSystem", "Error: No valid Ch specified");
        }
        return offString;
    }

    private static String getStartString(int Ch) {
        String onString="";
        if(Ch == 0) {
            onString = chanZeroOn;
        } else if (Ch == 1) {
            onString = chanOneOn;
        } else if (Ch == 2) {
            onString = chanTwoOn;
        } else if (Ch == 3) {
            onString = chanThreeOn;
        } else {
            Log.d("RewardSystem", "Error: Invalid ch specified");
        }
        return onString;
    }

    public static void stopChannel(int Ch) {
        Log.d("RewardSystem", "Stopping channel "+Ch);
        String stopString = getStopString(Ch);
        sendData(stopString);
    }

    public static void startChannel(int Ch) {
        Log.d("RewardSystem", "Starting channel "+Ch);
        String startString = getStartString(Ch);
        sendData(startString);
    }

    public static void activateChannel(final int Ch, int amount) {
        Log.d("RewardSystem","Giving reward "+amount+" ms on channel "+Ch);
        startChannel(Ch);

        new CountDownTimer(amount, 100) {
            public void onTick(long ms) {}
            public void onFinish() { stopChannel(Ch); }
        }.start();
    }

    public static void sendData(String message) {
        if(bluetoothConnection) {
            byte[] msgBuffer = message.getBytes();
            try {
                outStream.write(msgBuffer);
            } catch (IOException e) {
                Log.d("RewardSystem", "Error: No socket");
            }
        } else {
            Log.d("RewardSystem", "Error: No connection");
        }
    }

    public static void quitBt() {
        Log.d("RewardSystem", "Quitting bluetooth");
        if (connectionLoopHandler != null) {
            connectionLoopHandler.removeCallbacksAndMessages(null);
        }
        try {
            context.unregisterReceiver(bluetoothReceiver);
        } catch (IllegalArgumentException e) {
            // No receiver registered
        }
        if (bluetoothConnection) {
            stopAllChannels();
            try {
                outStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                btSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private static final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action){
                case BluetoothDevice.ACTION_ACL_CONNECTED:
                    //Bluetooth connected
                    Log.d("RewardSystem","Bluetooth reconnected");
                    TaskManager.enableApp(true);
                    break;
                case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                    //Bluetooth disconnected
                    Log.d("RewardSystem","Lost bluetooth connection..");
                    bluetoothConnection = false;
                    TaskManager.enableApp(false);
                    loopUntilConnected();
                    break;
            }
        }
    };
    //一直循环直到连接，连接手机可否，更改蓝牙地址？
    public static void loopUntilConnected() {

        connectToBluetooth();

        if(!bluetoothConnection) {
            connectionLoopHandler = new Handler();
            connectionLoopHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    loopUntilConnected();
                }
            }, 15000);
        } else {
            stopAllChannels();
        }

    }
    //0代表所有通道关闭，1代表通道0打开，2代表通道0关闭？
    private static void initialiseRewardChannelStrings() {
        allChanOff = context.getString(R.string.allChanOff);
        chanZeroOn = context.getString(R.string.chanZeroOn);
        chanZeroOff = context.getString(R.string.chanZeroOff);
        chanOneOn = context.getString(R.string.chanOneOn);
        chanOneOff = context.getString(R.string.chanOneOff);
        chanTwoOn = context.getString(R.string.chanTwoOn);
        chanTwoOff = context.getString(R.string.chanTwoOff);
        chanThreeOn = context.getString(R.string.chanThreeOn);
        chanThreeOff = context.getString(R.string.chanThreeOff);
    }


}
