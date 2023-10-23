package com.netease.nertc.networktest;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.netease.lava.nertc.sdk.AbsNERtcCallbackEx;
import com.netease.lava.nertc.sdk.LastmileProbeConfig;
import com.netease.lava.nertc.sdk.LastmileProbeResult;
import com.netease.lava.nertc.sdk.NERtc;
import com.netease.lava.nertc.sdk.NERtcConstants;
import com.netease.lava.nertc.sdk.NERtcEx;
import com.netease.lava.nertc.sdk.NERtcOption;
import com.netease.lava.nertc.sdk.NERtcParameters;
import com.netease.lava.nertc.sdk.NERtcUserJoinExtraInfo;
import com.netease.lava.nertc.sdk.NERtcUserLeaveExtraInfo;

import com.netease.lava.nertc.sdk.video.NERtcVideoStreamType;
import com.netease.nertc.config.DemoDeploy;

public class NetworkTestActivity extends AppCompatActivity {

    private static final String TAG = "NetworkTestActivity";

    private Button mSpeedTestBtn;
    private TextView mTextTestResult;
    boolean mButtonState = false;
    private ImageView mBackIv;
    private AbsNERtcCallbackEx callback = new AbsNERtcCallbackEx() {


        @Override
        public void onJoinChannel(int result, long channelId, long elapsed, long uid) {

        }

        @Override
        public void onLeaveChannel(int result) {

        }

        @Override
        public void onUserJoined(long uid) {

        }

        @Override
        public void onUserJoined(long uid, NERtcUserJoinExtraInfo joinExtraInfo) {

        }

        @Override
        public void onUserLeave(long uid, int reason) {

        }

        @Override
        public void onUserLeave(long uid, int reason, NERtcUserLeaveExtraInfo leaveExtraInfo) {

        }

        @Override
        public void onUserAudioStart(long uid) {

        }

        @Override
        public void onUserAudioStop(long uid) {

        }

        @Override
        public void onUserVideoStart(long uid, int maxProfile) {

        }

        @Override
        public void onUserVideoStop(long uid) {

        }

        @Override
        public void onDisconnect(int reason) {

        }

        @Override
        public void onAudioEffectTimestampUpdate(long id, long timestampMs) {

        }

        @Override
        public void onError(int code) {

        }

        @Override
        public void onPermissionKeyWillExpire() {

        }

        @Override
        public void onUpdatePermissionKey(String key, int error, int timeout) {

        }

        @Override
        public void onLocalVideoWatermarkState(NERtcVideoStreamType videoStreamType, int state) {

        }
        @Override
        public void onLastmileProbeResult(LastmileProbeResult lastmileProbeResult) {
            LastmileProbeResult.LastmileProbeOneWayResult uplinkReport = lastmileProbeResult.uplinkReport;
            LastmileProbeResult.LastmileProbeOneWayResult downlinkReport = lastmileProbeResult.downlinkReport;
            String networkQuality = new String();
            networkQuality += "网络质量：\n";
            networkQuality += "rtt:" + lastmileProbeResult.rtt + "ms\n";
            networkQuality += "上行质量：\n";
            networkQuality += "Bandwidth：" + uplinkReport.availableBandwidth + "\n";
            networkQuality += "jitter：" + uplinkReport.jitter + "\n";
            networkQuality += "packetLossRate：" + uplinkReport.packetLossRate + "%\n";
            networkQuality += "下行质量：\n";
            networkQuality += "Bandwidth:" + downlinkReport.availableBandwidth + "\n";
            networkQuality += "jitter：" + downlinkReport.jitter + "\n";
            networkQuality += "packetLossRate：" + downlinkReport.packetLossRate + "%\n";
            mTextTestResult.setText(networkQuality);
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_network_test);
        initView();
        setupNERtc();


    }
    private void setupNERtc() {
        NERtcParameters parameters = new NERtcParameters();
        NERtcEx.getInstance().setParameters(parameters); //先设置参数，后初始化

        NERtcOption options = new NERtcOption();

        if (BuildConfig.DEBUG) {
            options.logLevel = NERtcConstants.LogLevel.INFO;
        } else {
            options.logLevel = NERtcConstants.LogLevel.WARNING;
        }

        try {
            NERtcEx.getInstance().init(getApplicationContext(), DemoDeploy.APP_KEY, callback, options);
        } catch (Exception e) {
            // 可能由于没有release导致初始化失败，release后再试一次
            NERtcEx.getInstance().release();
            try {
                NERtcEx.getInstance().init(getApplicationContext(), DemoDeploy.APP_KEY, callback, options);
            } catch (Exception ex) {
                Toast.makeText(this, "SDK初始化失败", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
        }
    }

    private void initView() {
        mSpeedTestBtn = findViewById(R.id.btn_speed_test_start);
        mSpeedTestBtn.setOnClickListener(mSpeedTestClickListener);
        mTextTestResult = findViewById(R.id.tv_speed_test_result);
        mBackIv = findViewById(R.id.iv_back);
        mBackIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }
    private void exit(){
        NERtc.getInstance().release();
    }
    private final View.OnClickListener mSpeedTestClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(mButtonState == false) {
                mButtonState = true;
                LastmileProbeConfig lastmileProbeConfig = new LastmileProbeConfig();
                lastmileProbeConfig.probeDownlink = true;
                lastmileProbeConfig.probeUplink = true;
                NERtcEx.getInstance().startLastmileProbeTest(lastmileProbeConfig);
                mTextTestResult.setText("测速中，请稍等......");
            }
        }
    };

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        exit();
    }
}