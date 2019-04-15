package com.gtu.logme;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.gtu.logme.Core.BluetoothService;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import butterknife.BindView;
import butterknife.ButterKnife;

import android.os.Handler;
import android.os.Message;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    public final static int BT_ENABLE_REQUEST = 1;


    @BindView(R.id.message)
    TextView mTextMessage;

    @BindView(R.id.message_to_send)
    EditText messageToSend;

    @BindView(R.id.send_button)
    Button sendButton;

    @BindView(R.id.navigation)
    BottomNavigationView nav;

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
    private String remoteDeviceMAC;

    @Override
    @SuppressLint("HandlerLeak")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        nav.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        sendButton.setOnClickListener(mOnSendButtonClickedListener);

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

            remoteDeviceMAC = "";
            mBtAdapter = BluetoothAdapter.getDefaultAdapter();
            promptBT();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (featureFlag) startBtService();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode){
                case BT_ENABLE_REQUEST:
                    startBtService();
                    break;
            }
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
    }

    private void startBtService(){
        mDevice = mBtAdapter.getRemoteDevice(remoteDeviceMAC);
        try {
            BluetoothService btService = new BluetoothService(mDevice, mHandler);
            btService.start();

            btService.write("Example input"); // Example data send to bt device.
        }
        catch (Exception ex) {
            Toast.makeText(this, ex.toString(), Toast.LENGTH_LONG).show();
        }
    }
}
