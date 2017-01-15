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
import android.content.res.Resources;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;


import com.ibm.iot.android.iotstarter.R;
import com.ibm.iot.android.iotstarter.iot.IoTClient;
import com.ibm.iot.android.iotstarter.utils.Constants;
import com.ibm.iot.android.iotstarter.utils.MyIoTActionListener;

import org.eclipse.paho.client.mqttv3.MqttException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import weka.classifiers.functions.MultilayerPerceptron;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SparseInstance;

//import static com.google.android.gms.internal.a.R;
import static com.google.android.gms.internal.a.C;
import static com.ibm.iot.android.iotstarter.R.layout.main;
import static com.ibm.iot.android.iotstarter.R.raw.trainset;
import static com.ibm.iot.android.iotstarter.R.raw.multilayerperceptron;
import static com.ibm.iot.android.iotstarter.utils.DefaultInstanceAttribute.getFormatDefaultInstanceAttribute;
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
    private Context mainContext;


    //Weka
    double[] testData = new double[180];
    private FastVector instanceAttributes;
    private Instances dataSet;

    MultilayerPerceptron classifier;
    Instance single_window;

    private String line;

    private static final String deviceID = "20:16:09:08:22:16";

    @Override
    public void onCreate() {
        super.onCreate();
        recDataString = new StringBuilder();
        intent = new Intent("BluetoothService");
        getApplicationContext().registerReceiver(mMessageReceiver, new IntentFilter("QuitService"));
        mainContext = this.getApplicationContext();

        try {
            createClassifier();
        } catch (Exception e) {
            e.printStackTrace();
        }
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

    public void sendMessageToActivity(String message){
        intent.putExtra("bluetoothMessage", message);
        sendBroadcast(intent);
    }

    private void connect() {
        bluetoothIn = new Handler() {

            public void handleMessage(android.os.Message msg) {
                if (msg.what == Constants.handlerState) {
                    newline = System.getProperty("line.separator");
                    recDataString.append((String) msg.obj);

                            if (recDataString.toString().contains(newline)) {
                                String temp = recDataString.toString();
                                if(temp.contains("null")){
                                   temp = temp.replace("null","");
                                }
                                if(temp.contains("h")){
                                    temp = temp.replace("h,","");
                                }

                                line += temp;
                                count++;

                                if(count == 30) {
                                    line = line.substring(0,line.length()-1);
                                    String[] data = line.split(",");
                                    try {
                                        sendMessageToActivity(createArray(data));
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


    //--------Train data-------------------------------------------------------------------------------
    public void createClassifier() throws Exception {
        Resources res = this.getResources();
        BufferedReader reader = new BufferedReader(new InputStreamReader(res.openRawResource(trainset)));
        Instances train = new Instances(reader);
        reader.close();
        train.setClassIndex(train.numAttributes() - 1);

        try {
            classifier = (MultilayerPerceptron) new ObjectInputStream(res.openRawResource(multilayerperceptron)).readObject();

        } catch (Error e) {
            System.out.println("PROBLEM ATT SKAPA CLASSIFIER: " + e);
        }

        instanceAttributes = getFormatDefaultInstanceAttribute();
        dataSet = new Instances("Relation: trainData", instanceAttributes, 0);
        dataSet.setClassIndex(instanceAttributes.size() - 1);
    }

    public String createArray(String[] data) throws Exception {
        single_window = new SparseInstance(dataSet.numAttributes());

        for (int i = 0; i < 180; i++) {

             if(data[i].equals("null")){
                 data[i] = data[i].replace("null", data[i+6]);
             }else if(data[i].contains("null")) {
                 data[i] = data[i].replace("null", "");
             }
            testData[i] = Double.valueOf(data[i]);
            single_window.setValue((Attribute) instanceAttributes.elementAt(i), testData[i]);
        }

        single_window.setMissing(180);

        dataSet.add(single_window);
        single_window.setDataset(dataSet);

        String movement = "";

        Instances labeled = new Instances(dataSet);


        for (int i = 0; i < dataSet.numInstances(); i++) {

            double pred = classifier.classifyInstance(dataSet.instance(i));
            labeled.instance(i).setClassValue(pred);
            movement = labeled.instance(i).classAttribute().value((int) pred);
        }
        return movement;
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