package com.gtu.logme.Core;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.UUID;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.gtu.logme.MainApplication;
import com.polidea.rxandroidble2.RxBleDevice;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

import static java.util.concurrent.TimeUnit.SECONDS;

public class BluetoothService extends Thread {
    public final static int BT_RECEIVED_MESSAGE = 0;

    private InputStream mmInStream;
    private BluetoothSocket mSocket;

    private Handler mHandler;
    private final BluetoothDevice mDevice;

    private RxBleDevice bleDevice;

    private Context mContext;

    public BluetoothService(Context ctx, BluetoothDevice device, Handler messageHandler) throws IOException {
        mDevice = device;
        mHandler = messageHandler;
        mContext = ctx;

        mSocket = mDevice.createInsecureRfcommSocketToServiceRecord(UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8"));

        bleDevice = MainApplication.getRxBleClient(mContext).getBleDevice(device.getAddress());
        Disposable connectionDisposable = bleDevice.establishConnection(false)
                .doFinally(this::cleanUp)
                .flatMap(rxBleConnection -> // Set desired interval.
                        Observable.interval(2, SECONDS).flatMapSingle(sequence -> rxBleConnection.readRssi()))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::updateRssi, this::onConnectionFailure);
        try {
//            Method m = mDevice.getClass().getMethod("createRfcommSocket", new Class[] {int.class});
//            mSocket = (BluetoothSocket) m.invoke(mDevice, 1);
//            mSocket.connect();


            mmInStream = mSocket.getInputStream();

        } catch (Exception e) {
            Log.d("LOGME", e.toString());
        }
    }

    public void updateRssi(int rss) {
        Toast.makeText(mContext, rss, Toast.LENGTH_LONG).show();
    }

    private void onConnectionFailure(Throwable throwable) {
        //noinspection ConstantConditions
        Toast.makeText(mContext, "ERROR", Toast.LENGTH_LONG).show();

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

    public void cleanUp() {
        try {
            mSocket.close();
        } catch (Exception ex) {
            // TODO: Log error. Probably not couldn't be opened.
        }
    }
}

