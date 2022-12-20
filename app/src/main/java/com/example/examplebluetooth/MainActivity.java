package com.example.examplebluetooth;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.Dialog;
import android.app.Notification;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    // static constants
    private static final String TAG = "MainActivity";
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // variables needed for bluetooth connectivity
    BluetoothAdapter mBluetoothAdapter;
    DeviceListAdapter listAdapter = new DeviceListAdapter();
    BluetoothSocket mBluetoothSocket;





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // setting up the onscreen buttons functionality
        Button btnOnOff = (Button) findViewById(R.id.btnOnOff);
        Button btnEnableDisable_Discoverable = (Button) findViewById(R.id.btnEnableDisable_Discoverable);
        Button btnDiscoverDevices = (Button) findViewById(R.id.btnDiscoverDevices);

        btnOnOff.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.S)
            @Override
            public void onClick(View view) {
                enableDisableBT();
            }
        });
        btnEnableDisable_Discoverable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                enableDisableDiscoverable();
            }
        });
        btnDiscoverDevices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!mBluetoothAdapter.isEnabled()){
                    enableDisableBT();
                }

                discoverDevices();
            }
        });


        // setting up the recyclerview for viewing devices
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        recyclerView.setAdapter(listAdapter);
        listAdapter.setOnAdapterItemClick(new DeviceListAdapter.IAdapterItemClick() {
            @Override
            public void onItemClicked(BluetoothDevice selectedDevice) {
                try {
                    // pairing with the selected device
                    mBluetoothAdapter.cancelDiscovery();
                    listAdapter.devices.clear();
                    listAdapter.notifyDataSetChanged();
                    Log.d(TAG, "onItemClicked: " + selectedDevice.getName() + " " + selectedDevice.getAddress());

                    startTTSActivity(selectedDevice);
//                    connectHandler.post(new Runnable() {
//                        @Override
//                        public void run() {
//                            connectDevice(selectedDevice);
//                        }
//                    });


                } catch (SecurityException se) {
                    Log.d(TAG, "onItemClicked: Cannot bond to selected device due to permission errors.");
                }

            }
        });


        // default bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();


        // intent filters for different actions
        IntentFilter btStateChangeFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        IntentFilter btDiscoverableFilter = new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        IntentFilter btDiscoverDevicesFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        IntentFilter btDeviceUUIDFilter = new IntentFilter(BluetoothDevice.ACTION_UUID);


        // registering the receivers
        registerReceiver(btStateChangeReceiver, btStateChangeFilter);
        registerReceiver(btDiscoverableReceiver, btDiscoverableFilter);
        registerReceiver(btDiscoverDevicesReceiver, btDiscoverDevicesFilter);


        // setting up the handler threads


    }
    private void startTTSActivity(BluetoothDevice device){
        Intent textToSpeechActivityIntent = new Intent(MainActivity.this, TTS.class);
        textToSpeechActivityIntent.putExtra("btDevice", device);
        startActivity(textToSpeechActivityIntent);
    }




    BroadcastReceiver btStateChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        Log.d(TAG, "onReceive: Bluetooth Turned Off");
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.d(TAG, "onReceive: Bluetooth Turning Off");
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d(TAG, "onReceive: Bluetooth turning On");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.d(TAG, "onReceive: Bluetooth turned On");
                        break;
                }
            }
        }
    };


    BroadcastReceiver btDiscoverableReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                        Log.d(TAG, "DiscoverableReceive: Discoverability enabled.");
                        break;
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                        Log.d(TAG, "DiscoverableReceive: Discoverability Disabled. Able to receive connections");
                        break;
                    case BluetoothAdapter.SCAN_MODE_NONE:
                        Log.d(TAG, "DiscoverableReceive: No discoverability");
                        break;
                    case BluetoothAdapter.STATE_CONNECTED:
                        Log.d(TAG, "DiscoverableReceive: Connected");
                        break;
                    case BluetoothAdapter.STATE_CONNECTING:
                        Log.d(TAG, "DiscoverableReceive: Connecting...");
                        break;
                }
            }
        }
    };


    BroadcastReceiver btDiscoverDevicesReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
//            Log.d(TAG, "onReceive: On Device Found Receiver.");
            String action = intent.getAction();
            if(action.equals(BluetoothDevice.ACTION_FOUND)){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String name = intent.getStringExtra(BluetoothDevice.EXTRA_NAME);
//                Log.d(TAG, "UUID: " + intent.getStringExtra(BluetoothDevice.EXTRA_UUID));
                String address = device.getAddress();
//                Log.d(TAG, "Device : " +  name + "," + address);
                if(!listAdapter.devices.contains(device)){
                    listAdapter.devices.add(device);
                    listAdapter.notifyDataSetChanged();
                }
            }
        }
    };


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(btStateChangeReceiver);
        unregisterReceiver(btDiscoverableReceiver);
        unregisterReceiver(btDiscoverDevicesReceiver);


    }


    private void enableDisableBT() {
        if (mBluetoothAdapter == null) {
            Log.d(TAG, "enableDisableBT: No bluetooth adapters found");
            return;
        }
        if (mBluetoothAdapter.isEnabled()) {
            Log.d(TAG, "enableDisableBT: Disabling Bluetooth");
            try {
                mBluetoothAdapter.disable();
            } catch (SecurityException se) {
                Log.d(TAG, "enableDisableBT: User denied Permission for disabling Bluetooth");
            }

        } else {
            Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            try {
                startActivity(enableBTIntent);
            } catch (SecurityException se) {
                Log.d(TAG, "enableDisableBT: User denied permission for enabling Bluetooth");
            }

        }
    }


    private void enableDisableDiscoverable() {
        Log.d(TAG, "enableDisableDiscoverable: Making Device Discoverable for 300 seconds...");

        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 30);
        try {
            startActivity(intent);
        } catch (SecurityException se) {
            Log.d(TAG, "enableDisableDiscoverable: Cannot Enable Discoverability, user denied permission");
        }
    }


    private void discoverDevices() {
        checkPermissions();
        try{
            if (mBluetoothAdapter.isDiscovering()) {
                mBluetoothAdapter.cancelDiscovery();
                listAdapter.devices.clear();
                listAdapter.notifyDataSetChanged();
            }
            mBluetoothAdapter.startDiscovery();

        }catch (SecurityException se){
            Log.d(TAG, "discoverDevices: User denied discovery permissions");
        }
       

    }


    private void checkPermissions(){
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            int permissionCheck = 0;
            permissionCheck = this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);
            permissionCheck += this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION);
            if(permissionCheck != 0){
                this.requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},  1);

            }
        }else{
            Log.d(TAG, "checkPermissions: No need to take Permissions.");
        }
        
    }
    

//    // accept thread to accept bluetooth connections
//
//    private class AcceptThread extends Thread {
//        private final BluetoothServerSocket mmServerSocket;
//
//        public AcceptThread() {
//            // Use a temporary object that is later assigned to mmServerSocket
//            // because mmServerSocket is final.
//            BluetoothServerSocket tmp = null;
//            try {
//                // MY_UUID is the app's UUID string, also used by the client code.
//                tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
//            } catch (IOException e) {
//                Log.e(TAG, "Socket's listen() method failed", e);
//            }catch(SecurityException se){
//                Log.d(TAG, "AcceptThread: Cannot open socket fa87c0d0-afac-11de-8a39-0800200c9a66");
//            }
//            mmServerSocket = tmp;
//        }
//
//        public void run() {
//            BluetoothSocket socket = null;
//            // Keep listening until exception occurs or a socket is returned.
//            Log.d(TAG, "run: Running accept thread");
//            while (true) {
//                try {
//                    socket = mmServerSocket.accept();
//                } catch (IOException e) {
//                    Log.e(TAG, "Socket's accept() method failed", e);
//                    break;
//                }
//
//                if (socket != null) {
//                    // A connection was accepted. Perform work associated with
//                    // the connection in a separate thread.
////                    manageMyConnectedSocket(socket);
//                    try{
//                        mmServerSocket.close();
//                    }catch(IOException se){
//                        Log.e(TAG, "Could not close the connect socket", se);
//                    }
//                    break;
//                }
//            }
//        }
//
//        // Closes the connect socket and causes the thread to finish.
//        public void cancel() {
//            try {
//                mmServerSocket.close();
//            } catch (IOException e) {
//                Log.e(TAG, "Could not close the connect socket", e);
//            }
//        }
//    }
//
//    // connect thread
//
//    private class ConnectThread extends Thread {
//        private final BluetoothSocket mmSocket;
//        private final BluetoothDevice mmDevice;
//
//        public ConnectThread(BluetoothDevice device) {
//            // Use a temporary object that is later assigned to mmSocket
//            // because mmSocket is final.
//            BluetoothSocket tmp = null;
//            mmDevice = device;
//
//            try {
//                // Get a BluetoothSocket to connect with the given BluetoothDevice.
//                // MY_UUID is the app's UUID string, also used in the server code.
//                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
//            } catch (IOException e) {
//                Log.e(TAG, "Socket's create() method failed", e);
//            }catch(SecurityException se){
//                Log.d(TAG, "ConnectThread: Cannot connect to device");
//            }
//            mmSocket = tmp;
//        }
//
//        public void run() {
//            Log.d(TAG, "connect_run: Running connection thread");
//            // Cancel discovery because it otherwise slows down the connection.
//            try{
//                mBluetoothAdapter.cancelDiscovery();
//            }catch (SecurityException se){
//
//            }
//
//            try {
//                // Connect to the remote device through the socket. This call blocks
//                // until it succeeds or throws an exception.
//                mmSocket.connect();
//
//                Log.d(TAG, "run: Connection Succesfull");
//            } catch (IOException connectException) {
//                // Unable to connect; close the socket and return.
//                try {
//                    mmSocket.close();
//                } catch (IOException closeException) {
//                    Log.e(TAG, "Could not close the client socket", closeException);
//                }
//                return;
//            }catch(SecurityException se){
//                Log.d(TAG, "run: Cannot connect");
//            }
//
//            // The connection attempt succeeded. Perform work associated with
//            // the connection in a separate thread.
////            manageMyConnectedSocket(mmSocket);
//        }
//
//        // Closes the client socket and causes the thread to finish.
//        public void cancel() {
//            try {
//                mmSocket.close();
//            } catch (IOException e) {
//                Log.e(TAG, "Could not close the client socket", e);
//            }
//        }
//    }


}