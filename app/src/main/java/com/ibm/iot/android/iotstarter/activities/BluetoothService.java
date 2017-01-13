package com.ibm.iot.android.iotstarter.activities;

import android.annotation.TargetApi;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;


import com.ibm.iot.android.iotstarter.utils.Constants;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import static java.lang.Thread.sleep;

public class BluetoothService extends Service {
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mDevice;
    private ConnectThread mConnectThread;
    private Handler bluetoothIn;
    private StringBuilder recDataString;
    private Intent intent;
    private int count = 0;
    private String newline;

    private String line;

    private static final String deviceID = "20:16:09:08:22:16";

    @Override
    public void onCreate() {
        super.onCreate();
        recDataString = new StringBuilder();
        intent = new Intent("BluetoothService");
        getApplicationContext().registerReceiver(mMessageReceiver, new IntentFilter("QuitService"));
        newline = System.getProperty("line.separator");
        init();
    }

    public BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public synchronized void onReceive(Context context, Intent intent) {
            String data = intent.getStringExtra("quitMessage");
            if(data.contains("quit")){
                stopSelf();
            }
        }
    };

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void init() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter != null) {

            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
            if (pairedDevices.size() > 0) {
                for (BluetoothDevice device : pairedDevices) {
                    if(device.toString().equals(deviceID)){
                        mDevice = device;
                    }
                }
            }
                mConnectThread = new ConnectThread(mDevice);
                mConnectThread.start();
                connect();
        }
        }

    public void sendMessageToActivity(String[] message){
        intent.putExtra("bluetoothMessage", message);
        sendBroadcast(intent);
    }

    private void connect() {
        bluetoothIn = new Handler() {

            public void handleMessage(android.os.Message msg) {
                if (msg.what == Constants.handlerState) {
                    recDataString.append((String) msg.obj);

                            if (recDataString.toString().contains(newline)) {
                                line += recDataString.toString().substring(2,recDataString.length());
                                count++;

                                if(count == 30) {
                                    line = line.substring(0,line.length()-1);
                                    String[] data = line.split(",");
                                    try {
                                        sendMessageToActivity(data);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                    count=0;
                                    line ="";
                                }
                                recDataString.setLength(0);
                        }

                }
            }
        };
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    private class ConnectThread extends Thread {
        private ConnectedThread mConnectedThread;
        private BluetoothDevice mmDevice;
        private BluetoothSocket mmSocket;

        public ConnectThread(BluetoothDevice device) {
            BluetoothSocket tmp = null;
            mmDevice = device;
            try {
                tmp = device.createRfcommSocketToServiceRecord(Constants.MY_UUID);
            } catch (IOException e) {
                System.out.print(e);
            }
            mmSocket = tmp;
        }

        public void run() {
            mBluetoothAdapter.cancelDiscovery();
            try {
                mmSocket.connect();

            } catch (IOException connectException) {
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                }
                return;
            }
            mConnectedThread = new ConnectedThread(mmSocket);
            mConnectedThread.start();
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
            }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }
        public void run() {
            byte[] buffer = new byte[1024];
            int bytes = 0;
            while (true) {
                try {
                    mConnectThread.mConnectedThread.write("*".getBytes());

                    bytes = mmInStream.read(buffer);
                    String readMessage = new String(buffer, 0, bytes);
                    bluetoothIn.obtainMessage(Constants.handlerState, bytes, -1, readMessage).sendToTarget();

                } catch (IOException e) {
                    break;
                }
            }
        }
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
            }
        }
    }
}