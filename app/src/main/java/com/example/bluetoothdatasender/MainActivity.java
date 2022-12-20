package com.example.bluetoothdatasender;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Random;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    BluetoothAdapter bluetoothAdapter;
    Button acceptBtn;
    private AcceptThread acceptThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Log.e(TAG, "onCreate: device doesn't support bluetooth");
        }

        IntentFilter btStateChangeFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(btStateChangeReceiver, btStateChangeFilter);

        acceptBtn = findViewById(R.id.accept);
        acceptBtn.setOnClickListener(view -> {
            enableDisableDiscoverable();
            if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
                if(acceptThread != null){
                    acceptThread.cancel();
                    acceptThread = null;

                }

                acceptThread = new AcceptThread();
                acceptThread.start();
            }else{
                bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                enableDisableBT();
            }


        });



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

    private void enableDisableBT() {
        if (bluetoothAdapter == null) {
            Log.d(TAG, "enableDisableBT: No bluetooth adapters found");
            return;
        }
        if (bluetoothAdapter.isEnabled()) {
            Log.d(TAG, "enableDisableBT: Disabling Bluetooth");
            try {
                bluetoothAdapter.disable();
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
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(btStateChangeReceiver);
    }

    private class AcceptThread extends Thread {
        private  BluetoothServerSocket mmServerSocket;
        private boolean close = false;

        public AcceptThread() {
            // Use a temporary object that is later assigned to mmServerSocket
            // because mmServerSocket is final.
            BluetoothServerSocket tmp = null;
            try {
                // MY_UUID is the app's UUID string, also used by the client code.
                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord("AcceptSocket", MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket's listen() method failed", e);
            }catch (SecurityException se){
                Log.e(TAG, "AcceptThread: no permission to open socket");
            }
            mmServerSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket = null;
            // Keep listening until exception occurs or a socket is returned.
            while (!close) {
                Log.d(TAG, "run: hehe");
                try {
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "Socket's accept() method failed", e);
                }


                if (socket != null) {
                    // A connection was accepted. Perform work associated with
                    // the connection in a separate thread.
                    try{
                       writeData(socket);
                    }catch(Exception e){
                        Log.d(TAG, "run: write ended");
                    }
                }
            }
        }

        // Closes the connect socket and causes the thread to finish.
        public void cancel() {
            try {
                mmServerSocket.close();
                close = true;
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }
    }

    private void writeData(BluetoothSocket socket)throws Exception {
        Random random = new Random(123456789);

        String []inputString = {"$090120150180090120150180090120", "$090120150180090120150180090150",
                "$090120150180090120150180090180", "$090120120180090120150180090180","$090120150180090120150090090180", "$180120150180090120150090090180",
                "$150120150180090120150090090180"};

        while(true){

            int i = (int)(Math.random()*(inputString.length));
            byte[] byteArrray = inputString[i].getBytes();
            Log.d(TAG, "writeData: writing...");
            OutputStream opStream = socket.getOutputStream();
            opStream.write(byteArrray);
        }
    }

}