package com.netease.nertc.videocall;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import java.util.Random;

public class VideoCallEntryActivity extends AppCompatActivity {
    private Button mJoinBtn;
    private EditText mRoomIdEt;
    private EditText mUserIdEt;
    private ImageView mBackIv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_call_entry);
        initViews();

    }
    private void initViews(){
        String RoomId = "1382";
        mJoinBtn = findViewById(R.id.btn_join);
        mRoomIdEt = findViewById(R.id.room_id);
        mUserIdEt = findViewById(R.id.user_id);
        mBackIv = findViewById(R.id.iv_back);

        mRoomIdEt.setText(RoomId);
        mUserIdEt.setText(String.valueOf(new Random().nextInt(100000)));
        mBackIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        mJoinBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Editable roomIdEdit = mRoomIdEt.getText();
                if(roomIdEdit == null || roomIdEdit.length() <= 0){
                    return;
                }
                Editable userIdEdit = mUserIdEt.getText();
                if(userIdEdit == null || userIdEdit.length() <= 0){
                    return;
                }
                VideoCallActivity.startActivity(VideoCallEntryActivity.this, roomIdEdit.toString(), Long.parseLong(userIdEdit.toString()));
            }
        });
    }
}