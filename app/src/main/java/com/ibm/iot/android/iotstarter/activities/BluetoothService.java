package com.ibm.iot.android.iotstarter.activities;

import android.annotation.TargetApi;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
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

    private static Timer timer = new Timer();

    private ConnectThread mConnectThread;
    private Handler bluetoothIn;
    private StringBuilder recDataString;
    private Intent intent;

    @Override
    public void onCreate() {
        super.onCreate();
        recDataString = new StringBuilder();
        intent = new Intent("BluetoothService");
        init();

    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void init() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter != null) {

            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
            if (pairedDevices.size() > 0) {
                for (BluetoothDevice device : pairedDevices) {
                    mDevice = device;
                }
            }


                mConnectThread = new ConnectThread(mDevice);
                mConnectThread.start();


                //connect();

        }
        startService();

        }

    private void startService()
    {
        timer.scheduleAtFixedRate(new mainTask(), 0, 2000);
    }

    private class mainTask extends TimerTask
    {
        private int i = 0;
        public void run()
        {
            i++;
            sendMessageToActivity("{ \"d\": {" +
                    "\"movement\":\"" + String.valueOf(i + "\" " +
                    "} }"));
        }
    }


    public void sendMessageToActivity(String message){
        intent.putExtra("bluetoothMessage", message);
        sendBroadcast(intent);
    }

    private void connect() {
        bluetoothIn = new Handler() {

            public void handleMessage(android.os.Message msg) {
                if (msg.what == Constants.handlerState) {
                    //if message is what we want
                    String readMessage = (String) msg.obj;                              // msg.arg1 = bytes from connect thread

                    recDataString.append(readMessage);                                  //keep appending to string until ~
                    int endOfLineIndex = recDataString.indexOf("~");                    // determine the end-of-line

                    if (endOfLineIndex > 0) {                                           // make sure there data before ~
                       // String dataInPrint = recDataString.substring(0, endOfLineIndex);    // extract string
                        //  System.out.print("Data Received = " + dataInPrint);

                        if (recDataString.charAt(0) == '#')                             //if it starts with # we know it is what we are looking for
                        {

                            String[] data = recDataString.toString().split(",");       //Split data in array

                            try {
                              //  sendMessageToActivity(data);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        recDataString.delete(0, recDataString.length());                    //clear all string data
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

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                System.out.print(e);
            }
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
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                System.out.print(e);
            }
        }
    }
}