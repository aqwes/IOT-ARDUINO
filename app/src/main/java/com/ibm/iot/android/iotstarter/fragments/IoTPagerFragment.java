/*******************************************************************************
 * Copyright (c) 2014-2016 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 *
 * The Eclipse Public License is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *   http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *    Mike Robertson - initial contribution
 *    Aldo Eisma - fix occasional stale reference to drawingView
 *******************************************************************************/
package com.ibm.iot.android.iotstarter.fragments;

import android.app.AlertDialog;
import android.content.*;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.ibm.iot.android.iotstarter.IoTStarterApplication;
import com.ibm.iot.android.iotstarter.R;
import com.ibm.iot.android.iotstarter.iot.IoTClient;
import com.ibm.iot.android.iotstarter.utils.Constants;
import com.ibm.iot.android.iotstarter.utils.MessageFactory;
import com.ibm.iot.android.iotstarter.utils.MyIoTActionListener;
import com.ibm.iot.android.iotstarter.views.DrawingView;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import weka.classifiers.Classifier;
import weka.classifiers.meta.FilteredClassifier;
import weka.classifiers.trees.J48;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SparseInstance;
import weka.filters.unsupervised.attribute.Remove;

/**
 * The IoT Fragment is the main fragment of the application that will be displayed while the device is connected
 * to IoT. From this fragment, users can send text event messages. Users can also see the number
 * of messages the device has published and received while connected.
 */
public class IoTPagerFragment extends IoTStarterPagerFragment {
    private final static String TAG = IoTPagerFragment.class.getName();

    Classifier classifier;
    private View view;
    private TextView text1;
    double[] testData = new double[181];
    Instance single_window;



    private Context mainContext;
    private IoTStarterApplication app;

    /**************************************************************************
     * Fragment functions for establishing the fragment
     **************************************************************************/

    public static IoTPagerFragment newInstance() {
        IoTPagerFragment i = new IoTPagerFragment();
        return i;
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.iot, container, false);
        text1 = (TextView) view.findViewById(R.id.textView);

        return view;
    }

    public void setText1(String text) {text1.setText(text);}

    /**
     * Called when the fragment is resumed.
     */
    @Override
    public void onResume() {
        Log.d(TAG, ".onResume() entered");

        super.onResume();
        app = (IoTStarterApplication) getActivity().getApplication();
        app.setCurrentRunningActivity(TAG);

        if (broadcastReceiver == null) {
            Log.d(TAG, ".onResume() - Registering iotBroadcastReceiver");
            broadcastReceiver = new BroadcastReceiver() {

                @Override
                public void onReceive(Context context, Intent intent) {
                    Log.d(TAG, ".onReceive() - Received intent for iotBroadcastReceiver");
                    processIntent(intent);
                }
            };
        }
        mainContext = this.getContext();
        getActivity().getApplicationContext().registerReceiver(mMessageReceiver, new IntentFilter("BluetoothService"));
        getActivity().getApplicationContext().registerReceiver(broadcastReceiver,
                new IntentFilter(Constants.APP_ID + Constants.INTENT_IOT));

        // initialise
        initializeIoTActivity();
    }

    /**
     * Called when the fragment is destroyed.
     */
    @Override
    public void onDestroy() {
        Log.d(TAG, ".onDestroy() entered");

        try {
            getActivity().getApplicationContext().unregisterReceiver(broadcastReceiver);
            getActivity().getApplicationContext().unregisterReceiver(mMessageReceiver);

        } catch (IllegalArgumentException iae) {
            // Do nothing
        }
        super.onDestroy();
    }

    /**
     * Initializing onscreen elements and shared properties
     */
    private void initializeIoTActivity() {
        Log.d(TAG, ".initializeIoTFragment() entered");

        context = getActivity().getApplicationContext();

        updateViewStrings();

        DrawingView drawingView = (DrawingView) getActivity().findViewById(R.id.drawing);
        drawingView.setContext(context);
    }

    /**
     * Update strings in the fragment based on IoTStarterApplication values.
     */
    @Override
    void updateViewStrings() {
        Log.d(TAG, ".updateViewStrings() entered");
        // DeviceId should never be null at this point.
        if (app.getDeviceId() != null) {
            ((TextView) getActivity().findViewById(R.id.deviceIDIoT)).setText(app.getDeviceId());
        } else {
            ((TextView) getActivity().findViewById(R.id.deviceIDIoT)).setText("-");
        }
    }

    /**************************************************************************
     * Functions to process intent broadcasts from other classes
     **************************************************************************/

    /**
     * Process the incoming intent broadcast.
     * @param intent The intent which was received by the fragment.
     */
    private void processIntent(Intent intent) {
        Log.d(TAG, ".processIntent() entered");

        // No matter the intent, update log button based on app.unreadCount.
        updateViewStrings();

        String data = intent.getStringExtra(Constants.INTENT_DATA);
        assert data != null;
     if (data.equals(Constants.ALERT_EVENT)) {
            String message = intent.getStringExtra(Constants.INTENT_DATA_MESSAGE);
            new AlertDialog.Builder(getActivity())
                    .setTitle(getResources().getString(R.string.alert_dialog_title))
                    .setMessage(message)
                    .setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                        }
                    }).show();
        }
    }

    private FastVector getFormatDefaultInstanceAttribute() {
        List<Attribute> attributes1 = new ArrayList<>();

        attributes1.add(new Attribute("AccX1"));
        attributes1.add(new Attribute("AccY1"));
        attributes1.add(new Attribute("AccZ1"));
        attributes1.add(new Attribute("GyrX1"));
        attributes1.add(new Attribute("GyrY1"));
        attributes1.add(new Attribute("GyrZ1"));
        attributes1.add(new Attribute("AccX2"));
        attributes1.add(new Attribute("AccY2"));
        attributes1.add(new Attribute("AccZ2"));
        attributes1.add(new Attribute("GyrX2"));
        attributes1.add(new Attribute("GyrY2"));
        attributes1.add(new Attribute("GyrZ2"));
        attributes1.add(new Attribute("AccX3"));
        attributes1.add(new Attribute("AccY3"));
        attributes1.add(new Attribute("AccZ3"));
        attributes1.add(new Attribute("GyrX3"));
        attributes1.add(new Attribute("GyrY3"));
        attributes1.add(new Attribute("GyrZ3"));
        attributes1.add(new Attribute("AccX4"));
        attributes1.add(new Attribute("AccY4"));
        attributes1.add(new Attribute("AccZ4"));
        attributes1.add(new Attribute("GyrX4"));
        attributes1.add(new Attribute("GyrY4"));
        attributes1.add(new Attribute("GyrZ4"));
        attributes1.add(new Attribute("AccX5"));
        attributes1.add(new Attribute("AccY5"));
        attributes1.add(new Attribute("AccZ5"));
        attributes1.add(new Attribute("GyrX5"));
        attributes1.add(new Attribute("GyrY5"));
        attributes1.add(new Attribute("GyrZ5"));
        attributes1.add(new Attribute("AccX6"));
        attributes1.add(new Attribute("AccY6"));
        attributes1.add(new Attribute("AccZ6"));
        attributes1.add(new Attribute("GyrX6"));
        attributes1.add(new Attribute("GyrY6"));
        attributes1.add(new Attribute("GyrZ6"));
        attributes1.add(new Attribute("AccX7"));
        attributes1.add(new Attribute("AccY7"));
        attributes1.add(new Attribute("AccZ7"));
        attributes1.add(new Attribute("GyrX7"));
        attributes1.add(new Attribute("GyrY7"));
        attributes1.add(new Attribute("GyrZ7"));
        attributes1.add(new Attribute("AccX8"));
        attributes1.add(new Attribute("AccY8"));
        attributes1.add(new Attribute("AccZ8"));
        attributes1.add(new Attribute("GyrX8"));
        attributes1.add(new Attribute("GyrY8"));
        attributes1.add(new Attribute("GyrZ8"));
        attributes1.add(new Attribute("AccX9"));
        attributes1.add(new Attribute("AccY9"));
        attributes1.add(new Attribute("AccZ9"));
        attributes1.add(new Attribute("GyrX9"));
        attributes1.add(new Attribute("GyrY9"));
        attributes1.add(new Attribute("GyrZ9"));
        attributes1.add(new Attribute("AccX10"));
        attributes1.add(new Attribute("AccY10"));
        attributes1.add(new Attribute("AccZ10"));
        attributes1.add(new Attribute("GyrX10"));
        attributes1.add(new Attribute("GyrY10"));
        attributes1.add(new Attribute("GyrZ10"));
        attributes1.add(new Attribute("AccX11"));
        attributes1.add(new Attribute("AccY11"));
        attributes1.add(new Attribute("AccZ11"));
        attributes1.add(new Attribute("GyrX11"));
        attributes1.add(new Attribute("GyrY11"));
        attributes1.add(new Attribute("GyrZ11"));
        attributes1.add(new Attribute("AccX12"));
        attributes1.add(new Attribute("AccY12"));
        attributes1.add(new Attribute("AccZ12"));
        attributes1.add(new Attribute("GyrX12"));
        attributes1.add(new Attribute("GyrY12"));
        attributes1.add(new Attribute("GyrZ12"));
        attributes1.add(new Attribute("AccX13"));
        attributes1.add(new Attribute("AccY13"));
        attributes1.add(new Attribute("AccZ13"));
        attributes1.add(new Attribute("GyrX13"));
        attributes1.add(new Attribute("GyrY13"));
        attributes1.add(new Attribute("GyrZ13"));
        attributes1.add(new Attribute("AccX14"));
        attributes1.add(new Attribute("AccY14"));
        attributes1.add(new Attribute("AccZ14"));
        attributes1.add(new Attribute("GyrX14"));
        attributes1.add(new Attribute("GyrY14"));
        attributes1.add(new Attribute("GyrZ14"));
        attributes1.add(new Attribute("AccX15"));
        attributes1.add(new Attribute("AccY15"));
        attributes1.add(new Attribute("AccZ15"));
        attributes1.add(new Attribute("GyrX15"));
        attributes1.add(new Attribute("GyrY15"));
        attributes1.add(new Attribute("GyrZ15"));
        attributes1.add(new Attribute("AccX16"));
        attributes1.add(new Attribute("AccY16"));
        attributes1.add(new Attribute("AccZ16"));
        attributes1.add(new Attribute("GyrX16"));
        attributes1.add(new Attribute("GyrY16"));
        attributes1.add(new Attribute("GyrZ16"));
        attributes1.add(new Attribute("AccX17"));
        attributes1.add(new Attribute("AccY17"));
        attributes1.add(new Attribute("AccZ17"));
        attributes1.add(new Attribute("GyrX17"));
        attributes1.add(new Attribute("GyrY17"));
        attributes1.add(new Attribute("GyrZ17"));
        attributes1.add(new Attribute("AccX18"));
        attributes1.add(new Attribute("AccY18"));
        attributes1.add(new Attribute("AccZ18"));
        attributes1.add(new Attribute("GyrX18"));
        attributes1.add(new Attribute("GyrY18"));
        attributes1.add(new Attribute("GyrZ18"));
        attributes1.add(new Attribute("AccX19"));
        attributes1.add(new Attribute("AccY19"));
        attributes1.add(new Attribute("AccZ19"));
        attributes1.add(new Attribute("GyrX19"));
        attributes1.add(new Attribute("GyrY19"));
        attributes1.add(new Attribute("GyrZ19"));
        attributes1.add(new Attribute("AccX20"));
        attributes1.add(new Attribute("AccY20"));
        attributes1.add(new Attribute("AccZ20"));
        attributes1.add(new Attribute("GyrX20"));
        attributes1.add(new Attribute("GyrY20"));
        attributes1.add(new Attribute("GyrZ20"));
        attributes1.add(new Attribute("AccX21"));
        attributes1.add(new Attribute("AccY21"));
        attributes1.add(new Attribute("AccZ21"));
        attributes1.add(new Attribute("GyrX21"));
        attributes1.add(new Attribute("GyrY21"));
        attributes1.add(new Attribute("GyrZ21"));
        attributes1.add(new Attribute("AccX22"));
        attributes1.add(new Attribute("AccY22"));
        attributes1.add(new Attribute("AccZ22"));
        attributes1.add(new Attribute("GyrX22"));
        attributes1.add(new Attribute("GyrY22"));
        attributes1.add(new Attribute("GyrZ22"));
        attributes1.add(new Attribute("AccX23"));
        attributes1.add(new Attribute("AccY23"));
        attributes1.add(new Attribute("AccZ23"));
        attributes1.add(new Attribute("GyrX23"));
        attributes1.add(new Attribute("GyrY23"));
        attributes1.add(new Attribute("GyrZ23"));
        attributes1.add(new Attribute("AccX24"));
        attributes1.add(new Attribute("AccY24"));
        attributes1.add(new Attribute("AccZ24"));
        attributes1.add(new Attribute("GyrX24"));
        attributes1.add(new Attribute("GyrY24"));
        attributes1.add(new Attribute("GyrZ24"));
        attributes1.add(new Attribute("AccX25"));
        attributes1.add(new Attribute("AccY25"));
        attributes1.add(new Attribute("AccZ25"));
        attributes1.add(new Attribute("GyrX25"));
        attributes1.add(new Attribute("GyrY25"));
        attributes1.add(new Attribute("GyrZ25"));
        attributes1.add(new Attribute("AccX26"));
        attributes1.add(new Attribute("AccY26"));
        attributes1.add(new Attribute("AccZ26"));
        attributes1.add(new Attribute("GyrX26"));
        attributes1.add(new Attribute("GyrY26"));
        attributes1.add(new Attribute("GyrZ26"));
        attributes1.add(new Attribute("AccX27"));
        attributes1.add(new Attribute("AccY27"));
        attributes1.add(new Attribute("AccZ27"));
        attributes1.add(new Attribute("GyrX27"));
        attributes1.add(new Attribute("GyrY27"));
        attributes1.add(new Attribute("GyrZ27"));
        attributes1.add(new Attribute("AccX28"));
        attributes1.add(new Attribute("AccY28"));
        attributes1.add(new Attribute("AccZ28"));
        attributes1.add(new Attribute("GyrX28"));
        attributes1.add(new Attribute("GyrY28"));
        attributes1.add(new Attribute("GyrZ28"));
        attributes1.add(new Attribute("AccX29"));
        attributes1.add(new Attribute("AccY29"));
        attributes1.add(new Attribute("AccZ29"));
        attributes1.add(new Attribute("GyrX29"));
        attributes1.add(new Attribute("GyrY29"));
        attributes1.add(new Attribute("GyrZ29"));
        attributes1.add(new Attribute("AccX30"));
        attributes1.add(new Attribute("AccY30"));
        attributes1.add(new Attribute("AccZ30"));
        attributes1.add(new Attribute("GyrX30"));
        attributes1.add(new Attribute("GyrY30"));
        attributes1.add(new Attribute("GyrZ30"));

        FastVector fvClassVal = new FastVector(6);             // Sista label
        fvClassVal.addElement("up");
        fvClassVal.addElement("down");
        fvClassVal.addElement("left");
        fvClassVal.addElement("right");
        fvClassVal.addElement("tiltleft");
        fvClassVal.addElement("tiltright");
        attributes1.add(new Attribute("test", fvClassVal));   // Sista label

        //-----------Klistra in allt---------------------------------------------------
        FastVector fvWekaAttributes = new FastVector(attributes1.size() + 1);
        for (Attribute attribute : attributes1) {
            fvWekaAttributes.addElement(attribute);
        }
        return fvWekaAttributes;



    }

    public BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public synchronized void onReceive(Context context, Intent intent) {
            String [] data = intent.getStringArrayExtra("bluetoothMessage");
            try {
                text1.setText(createArray(data));
            } catch (Exception e) {
                e.printStackTrace();
            }
 /*           String  data2 = intent.getStringExtra("bluetoothMessage");
            text1.setText(data2);

            try {
                test(data2);
            } catch (Exception e) {
                e.printStackTrace();
            }*/

        }
    };
    public String createArray(String[] data) throws Exception {

        for (int i = 1; i <= 180; i++) {
            testData[i] = Double.valueOf(data[i]);
        }
        //testData[181] = "?";

        FastVector instanceAttributes = getFormatDefaultInstanceAttribute();
        Instances dataSet = new Instances("Relation: trainData", instanceAttributes, 0);
        dataSet.setClassIndex(instanceAttributes.size() - 1);

        single_window = new SparseInstance(dataSet.numAttributes());
        for (int i = 0; i < testData.length; i++) {
            single_window.setValue((Attribute) instanceAttributes.elementAt(i), testData[i]);
        }
        single_window.setMissing(181);


        dataSet.add(single_window);

        single_window.setDataset(dataSet); // Completed instance

        //--------Train data-------------------------------------------------------------------------------
        Resources res = this.getResources();

        BufferedReader reader2 = new BufferedReader(
                new InputStreamReader(res.openRawResource(R.raw.traindatatest2)));
        Instances traindata = new Instances(reader2);
        reader2.close();
        traindata.setClassIndex(traindata.numAttributes() - 1);
        Instances train = traindata;
        Remove rm = new Remove();
        classifier = new J48();
        FilteredClassifier fc = new FilteredClassifier();
        fc.setFilter(rm);
        fc.setClassifier(classifier);
        fc.buildClassifier(train);

        //double pred = fc.classifyInstance(test.instance(i));
        double pred2 = fc.classifyInstance(single_window);
        //System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX predicted: " + test.classAttribute().value((int) pred)+"XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
        System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX predicted: " + single_window.classAttribute().value((int) pred2) + "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");

        String movement = "";

        if (pred2 == 0.0) {
            movement = "UP";
        }
        if (pred2 == 1.0) {
            movement = "DOWN";
        }
        if (pred2 == 2.0) {
            movement = "LEFT";
        }
        if (pred2 == 3.0) {
            movement = "RIGHT";
        }
        if (pred2 == 5.0) {
            movement = "TILT LEFT";
        }
        if (pred2 == 4.0) {
            movement = "TILT RIGHT";
        }

        String messageData = MessageFactory.getAccelMessage(movement);

        try {
            MyIoTActionListener listener = new MyIoTActionListener(mainContext, Constants.ActionStateStatus.PUBLISH);
            IoTClient iotClient = IoTClient.getInstance(mainContext);
            if (app.getConnectionType() == Constants.ConnectionType.QUICKSTART) {
                iotClient.publishEvent(Constants.STATUS_EVENT, "json", messageData, 0, false, listener);
            } else {
                iotClient.publishEvent(Constants.ACCEL_EVENT, "json", messageData, 0, false, listener);
            }

            Intent actionIntent = new Intent(Constants.APP_ID + Constants.INTENT_IOT);
            actionIntent.putExtra(Constants.INTENT_DATA, Constants.INTENT_DATA_PUBLISHED);
            mainContext.sendBroadcast(actionIntent);
        } catch (MqttException e) {
        }

        Intent actionIntent = new Intent(Constants.APP_ID + Constants.INTENT_IOT);
        actionIntent.putExtra(Constants.INTENT_DATA, Constants.ACCEL_EVENT);
        mainContext.sendBroadcast(actionIntent);

        return messageData;
    }



    //Dennis test
    public void test(String data) throws Exception {
        try {
            MyIoTActionListener listener = new MyIoTActionListener(mainContext, Constants.ActionStateStatus.PUBLISH);
            IoTClient iotClient = IoTClient.getInstance(mainContext);
            if (app.getConnectionType() == Constants.ConnectionType.QUICKSTART) {
                iotClient.publishEvent(Constants.STATUS_EVENT, "json", data, 0, false, listener);
            } else {
                iotClient.publishEvent(Constants.TEXT_EVENT, "json", data, 0, false, listener);
            }

            Intent actionIntent = new Intent(Constants.APP_ID + Constants.INTENT_IOT);
            actionIntent.putExtra(Constants.INTENT_DATA, Constants.INTENT_DATA_PUBLISHED);
            mainContext.sendBroadcast(actionIntent);
        } catch (MqttException e) {
        }

        Intent actionIntent = new Intent(Constants.APP_ID + Constants.INTENT_IOT);
        actionIntent.putExtra(Constants.INTENT_DATA, Constants.ACCEL_EVENT);
        mainContext.sendBroadcast(actionIntent);

    }
}
