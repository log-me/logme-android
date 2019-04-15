package com.gtu.logme.Core;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import android.os.Handler;

public class BluetoothService extends Thread {
    public final int BT_RECEIVED_MESSAGE = 0;

    private final UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private final InputStream mmInStream;
    private final OutputStream mmOutStream;

    private Handler mHandler;
    private final BluetoothDevice mDevice;

    public BluetoothService(BluetoothDevice device, Handler messageHandler) throws IOException {
        mDevice = device;
        mHandler = messageHandler;

        BluetoothSocket socket = mDevice.createRfcommSocketToServiceRecord(uuid);
        mmOutStream = socket.getOutputStream();
        mmInStream = socket.getInputStream();

    }

    @Override
    public void run() {
        byte[] buffer = new byte[1024];
        int bytes;

        while (true) {
            try {
                bytes = mmInStream.read(buffer);
                String readMessage = new String(buffer, 0, bytes);

                mHandler.obtainMessage(BT_RECEIVED_MESSAGE, bytes, -1, readMessage).sendToTarget();
            } catch (IOException e) {
                break;
            }
        }
    }

    public void write(String input) {
        byte[] msgBuffer = input.getBytes();
        try {
            mmOutStream.write(msgBuffer);
        } catch (IOException e) {
            // TODO: Log exception
        }
    }
}

