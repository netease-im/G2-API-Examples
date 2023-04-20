package com.netease.nertc.externalbeauty;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class ExternalBeautyActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_external_beauty);
        initview();

    }

    private void initview() {
        findViewById(R.id.btn_bytedance_beauty).setOnClickListener(this);
        findViewById(R.id.btn_sensetime_beauty).setOnClickListener(this);
        findViewById(R.id.btn_faceunity_beauty).setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if(id == R.id.btn_faceunity_beauty){
            Intent intent = new Intent(ExternalBeautyActivity.this, FaceUnityBeautyActivity.class);
            startActivity(intent);
        }else if(id == R.id.btn_sensetime_beauty){
            Intent intent = new Intent(ExternalBeautyActivity.this,SenseTimeBeautyActivity.class);
            startActivity(intent);
        }else if(id == R.id.btn_bytedance_beauty){
            Intent intent = new Intent(ExternalBeautyActivity.this, ByteDanceBeautyActivity.class);
            startActivity(intent);
        }
    }
}