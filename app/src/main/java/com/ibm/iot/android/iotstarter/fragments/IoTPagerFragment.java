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
import android.view.*;
import android.widget.TextView;
import com.ibm.iot.android.iotstarter.IoTStarterApplication;
import com.ibm.iot.android.iotstarter.R;
import com.ibm.iot.android.iotstarter.iot.IoTClient;
import com.ibm.iot.android.iotstarter.utils.*;
import com.ibm.iot.android.iotstarter.views.DrawingView;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import weka.classifiers.Classifier;
import weka.classifiers.meta.FilteredClassifier;
import weka.classifiers.trees.J48;
import weka.core.*;
import weka.filters.unsupervised.attribute.Remove;

import static com.ibm.iot.android.iotstarter.utils.DefaultInstanceAttribute.getFormatDefaultInstanceAttribute;

/**
 * The IoT Fragment is the main fragment of the application that will be displayed while the device is connected
 * to IoT. From this fragment, users can send text event messages. Users can also see the number
 * of messages the device has published and received while connected.
 */
public class IoTPagerFragment extends IoTStarterPagerFragment {
    private final static String TAG = IoTPagerFragment.class.getName();
    private View view;
    private TextView text1;
    double[] testData = new double[181];
    private Context mainContext;
    private IoTStarterApplication app;


    //Weka
    private FilteredClassifier fc;
    private FastVector instanceAttributes;
    private Instances dataSet;

    Classifier classifier;
    Instance single_window;

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

        initializeIoTActivity();
        try {
            createClassifier();
        } catch (Exception e) {
            e.printStackTrace();
        }
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

    public BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public synchronized void onReceive(Context context, Intent intent) {
            String [] data = intent.getStringArrayExtra("bluetoothMessage");
            try {
                text1.setText(createArray(data));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    //--------Train data-------------------------------------------------------------------------------
    public void createClassifier() throws Exception {
        Resources res = this.getResources();
        BufferedReader reader2 = new BufferedReader(new InputStreamReader(res.openRawResource(R.raw.traindatatest)));
        Instances traindata = new Instances(reader2);
        reader2.close();
        traindata.setClassIndex(traindata.numAttributes() - 1);
        Instances train = traindata;

        Remove rm = new Remove();
        classifier = new J48();
        fc = new FilteredClassifier();
        fc.setFilter(rm);
        fc.setClassifier(classifier);
        fc.buildClassifier(train);

        instanceAttributes = getFormatDefaultInstanceAttribute();
        dataSet = new Instances("Relation: trainData", instanceAttributes, 0);
        dataSet.setClassIndex(instanceAttributes.size() - 1);
    }

    public String createArray(String[] data) throws Exception {

        for (int i = 1; i <= 180; i++) {
            testData[i] = Double.valueOf(data[i]);
        }

        single_window = new SparseInstance(dataSet.numAttributes());
        for (int i = 0; i < testData.length; i++) {
            single_window.setValue((Attribute) instanceAttributes.elementAt(i), testData[i]);
        }
        single_window.setMissing(181);

        dataSet.add(single_window);
        single_window.setDataset(dataSet);

        double pred2 = fc.classifyInstance(single_window);
        String movement = "";

        if (pred2 == 0.0) {
            movement = "UP";
        } else if (pred2 == 1.0) {
            movement = "DOWN";
        } else if (pred2 == 2.0) {
            movement = "LEFT";
        } else if (pred2 == 3.0) {
            movement = "RIGHT";
        } else if (pred2 == 5.0) {
            movement = "TILT LEFT";
        } else if (pred2 == 4.0) {
            movement = "TILT RIGHT";
        }

        String messageData = "{ \"d\": {" +
                "\"movement\":\"" + movement + "\" " +
                "} }";
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

        return movement;
    }
}