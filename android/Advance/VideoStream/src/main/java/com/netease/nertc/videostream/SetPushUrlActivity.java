package com.netease.nertc.videostream;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;

public class SetPushUrlActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_CHOOSE_VIDEO_FILE = 10000;
    private static final int REQUEST_CODE_REQUEST_PERMISSION = 10001;
    private EditText mPushUrl;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_address_config);
        initView();
    }

    @Override
    public void onBackPressed() {
        String pushUrl = mPushUrl.getText().toString();

        Intent intent = new Intent();
        intent.putExtra("pushUrl",pushUrl);

        setResult(0, intent);
        super.onBackPressed();
    }

    private void initView() {
        mPushUrl = findViewById(R.id.Url);
        String pushUrl = getIntent().getStringExtra("pushUrl");
        mPushUrl.setText(pushUrl);
    }

}