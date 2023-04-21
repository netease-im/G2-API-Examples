package com.netease.nertc.externalvideocapture;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.netease.nertc.externalvideocapture.externalvideo.FileUtil;

public class SetVideoConfigActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_CHOOSE_VIDEO_FILE = 10000;
    private static final int REQUEST_CODE_REQUEST_PERMISSION = 10001;

    private String mVideoPath;
    private EditText mVideoPathView;
    private Button mChooseFileBtn;
    private EditText mVideoWidth;
    private EditText mVideoHeight;
    private EditText mVideoFrameRate;
    private EditText mVideoAngle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_video_config);
        initView();
    }

    @Override
    public void onBackPressed() {
        String videoWidth = mVideoWidth.getText().toString();
        String videoHeight = mVideoHeight.getText().toString();
        String videoFrameRate = mVideoFrameRate.getText().toString();
        String videoAngle = mVideoAngle.getText().toString();
        String videoPath = mVideoPathView.getText().toString();

        Intent intent = new Intent();
        intent.putExtra("videoPath", videoPath);
        intent.putExtra("videoHeight",videoHeight);
        intent.putExtra("videoWidth",videoWidth);
        intent.putExtra("videoFrameRate",videoFrameRate);
        intent.putExtra("videoAngle",videoAngle);

        setResult(0, intent);
        super.onBackPressed();
    }

    private void initView() {
        mVideoPathView = findViewById(R.id.et_filepath);
        mChooseFileBtn = findViewById(R.id.btn_choose_file);
        mVideoWidth = findViewById(R.id.video_width);
        mVideoHeight = findViewById(R.id.video_height);
        mVideoFrameRate = findViewById(R.id.video_framerate);
        mVideoAngle = findViewById(R.id.video_angle);

        mVideoPathView.setText(getIntent().getStringExtra("videoPath"));
        int videoWidth = getIntent().getIntExtra("videoWidth", 0);
        int videoHeight = getIntent().getIntExtra("videoHeight", 0);
        int videoFrameRate = getIntent().getIntExtra("videoFrameRate", 0);
        int videoAngle = getIntent().getIntExtra("videoAngle", 0);

        mVideoWidth.setText(videoWidth == 0 ? "" : String.valueOf(videoWidth));
        mVideoHeight.setText(videoHeight == 0 ? "" : String.valueOf(videoHeight));
        mVideoFrameRate.setText(videoFrameRate == 0 ? "" : String.valueOf(videoFrameRate));
        mVideoAngle.setText(videoAngle == 0 ? "" : String.valueOf(videoAngle));

        mChooseFileBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseVideoFile();
            }
        });
    }
    private void chooseVideoFile() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            openFileChoose(REQUEST_CODE_CHOOSE_VIDEO_FILE);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE_REQUEST_PERMISSION);
        }
    }

    private void openFileChoose(int requestCode) {
        try {
            Intent intent = new Intent();
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                intent.setAction(Intent.ACTION_OPEN_DOCUMENT);
            } else {
                intent.setAction(Intent.ACTION_GET_CONTENT);
            }
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            startActivityForResult(intent, requestCode);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE_REQUEST_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                chooseVideoFile();
            }
        }
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_CHOOSE_VIDEO_FILE) {
            if (resultCode == RESULT_OK) {
                mVideoPath = FileUtil.getPath(getApplicationContext(), data.getData());
                mVideoPathView.setText(mVideoPath);
            }
        }
    }
}