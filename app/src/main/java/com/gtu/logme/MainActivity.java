package com.gtu.logme;

import android.os.Bundle;

import androidx.annotation.NonNull;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.AppCompatActivity;
import butterknife.BindView;
import butterknife.ButterKnife;

import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.message)
    private TextView mTextMessage;

    @BindView(R.id.message_to_send)
    private EditText messageToSend;

    @BindView(R.id.send_button)
    private Button sendButton;

    @BindView(R.id.navigation)
    private BottomNavigationView nav;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        nav.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        sendButton.setOnClickListener(mOnSendButtonClickedListener);

    }

}
