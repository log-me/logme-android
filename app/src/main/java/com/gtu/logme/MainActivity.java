package com.gtu.logme;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.gtu.logme.Adapters.BluetoothListAdapter;
import com.gtu.logme.Core.BluetoothService;
import com.polidea.rxandroidble2.RxBleDevice;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

import android.os.Handler;
import android.os.Message;
import android.os.Parcel;
import android.os.ParcelUuid;
import android.os.Parcelable;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static java.util.concurrent.TimeUnit.SECONDS;

public class MainActivity extends AppCompatActivity {

    public final static int BT_ENABLE_REQUEST = 1;
    public final static int BT_SCAN_PERM = 1001;


    @BindView(R.id.message)
    TextView mTextMessage;

    @BindView(R.id.message_to_send)
    EditText messageToSend;

    @BindView(R.id.send_button)
    Button sendButton;

    @BindView(R.id.navigation)
    BottomNavigationView nav;

    @BindView(R.id.bt_list)
    RecyclerView btList;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    //mTextMessage.setText(R.string.title_home);
                    return true;
                case R.id.navigation_dashboard:
                    //mTextMessage.setText(R.string.title_dashboard);
                    return true;
                case R.id.navigation_notifications:
                    //mTextMessage.setText(R.string.title_notifications);
                    return true;
            }
            return false;
        }
    };

    private View.OnClickListener mOnSendButtonClickedListener
            = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String message = messageToSend.getText().toString();
            Toast.makeText(tempThis, "BUTTON PRESSSED, SEND '" + message + "'", Toast.LENGTH_SHORT).show();
            // Send message
        }
    };

    private MainActivity tempThis = this;
    private boolean featureFlag = true;

    private Handler mHandler;
    private BluetoothDevice mDevice;
    private BluetoothAdapter mBtAdapter;
    private String remoteDeviceMAC = "B4:E6:2D:E9:53:B7";
    private List<BluetoothDevice> btDevices = new ArrayList<>();
    private List<BluetoothDevice> btDevicesWithUuid = new ArrayList<>();
    private boolean discoveryFinished = false;
    private BluetoothListAdapter mAdapter;

    Disposable connectionDisposable;

    @Override
    @SuppressLint("HandlerLeak")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        nav.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        sendButton.setOnClickListener(mOnSendButtonClickedListener);
        mTextMessage.setText("");
        btList.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new BluetoothListAdapter(this, btDevices);
        mAdapter.setClickListener((view, position) -> {
            mBtAdapter.cancelDiscovery();
            BluetoothDevice device = mAdapter.getItem(position);
            Toast.makeText(tempThis, device.getName(), Toast.LENGTH_LONG).show();

            boolean result = device.fetchUuidsWithSdp();
            ParcelUuid[] uuids = device.getUuids();

            startBtService(device);
        });
        btList.setAdapter(mAdapter);
        if (featureFlag) {

            mHandler = new Handler(){
                @Override
                public void handleMessage(Message msg) {
                    if (msg.what == BluetoothService.BT_RECEIVED_MESSAGE) {										//if message is what we want
                        String readMessage = (String) msg.obj;                                                                // msg.arg1 = bytes from connect thread
                        mTextMessage.setText(readMessage);
                    }
                }
            };


            mBtAdapter = BluetoothAdapter.getDefaultAdapter();
            promptBT();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

//        if (featureFlag)
//            startDiscover();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode){
                case BT_ENABLE_REQUEST:
                    //startDiscover();

                    BluetoothDevice device = mBtAdapter.getRemoteDevice(remoteDeviceMAC);
                    startBtService(device);
                    break;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case BT_SCAN_PERM:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    mBtAdapter.startDiscovery();
                }
                break;
        }
    }

    private void promptBT(){
        if (mBtAdapter == null) {
            Toast.makeText(this, "BtAdapter null, trying to get one", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!mBtAdapter.isEnabled()) {
            featureFlag = false;
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, BT_ENABLE_REQUEST);
        }
        else {
            //startDiscover();
            BluetoothDevice device = mBtAdapter.getRemoteDevice(remoteDeviceMAC);
            startBtService(device);
        }
    }

    private void startBtService(BluetoothDevice device){
        mBtAdapter.cancelDiscovery();
        try {
//            BluetoothService btService = new BluetoothService(this, device, mHandler);
//            btService.start();

            RxBleDevice bleDevice = MainApplication.getRxBleClient(this).getBleDevice(device.getAddress());
            connectionDisposable = bleDevice.establishConnection(false)
                    .doFinally(this::clearSubscription)
                    .flatMap(rxBleConnection -> rxBleConnection.setupNotification(UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8")))
                    .flatMap(notificationObservable -> notificationObservable)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::updateRssi, this::onConnectionFailure);

        }
        catch (Exception ex) {
            Toast.makeText(this, ex.toString(), Toast.LENGTH_LONG).show();
        }
    }

    public void updateRssi(byte[] bytes) {
        String x = "";
        for (byte b :
                bytes) {
            x += (char)b;
        }
        mTextMessage.setText(x);
        mTextMessage.setVisibility(View.VISIBLE);
    }

    private void onConnectionFailure(Throwable throwable) {
        //noinspection ConstantConditions
        Toast.makeText(this, "ERROR", Toast.LENGTH_LONG).show();

    }

    private void clearSubscription() {
        connectionDisposable = null;
    }

    private void startDiscover() {
        BroadcastReceiver mReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                //Finding devices
                if (BluetoothDevice.ACTION_FOUND.equals(action))
                {
                    // Get the BluetoothDevice object from the Intent
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    Parcelable[] uuidExtra = intent.getParcelableArrayExtra(BluetoothDevice.EXTRA_UUID);
                    // Add the name and address to an array adapter to show in a ListView
                    //mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                    if (!btDevices.contains(device)) {
                        btDevices.add(device);
                        mAdapter.notifyDataSetChanged();
                    }
                }
                else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                    for (BluetoothDevice dev :
                            btDevices) {
                        boolean result = dev.fetchUuidsWithSdp();
                    }
                    unregisterReceiver(this);
                    mBtAdapter.cancelDiscovery();
                    discoveryFinished = true;
                }
            }
        };

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothDevice.ACTION_UUID);
        registerReceiver(mReceiver, filter);
        if (Build.VERSION.SDK_INT >= 23){
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, BT_SCAN_PERM);
        }
        else{
            mBtAdapter.startDiscovery();
        }
    }
}
