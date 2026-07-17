package com.netease.nertc.beauty;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.faceunity.nama.FURenderer;
import com.netease.lava.api.IVideoRender;
import com.netease.lava.nertc.sdk.NERtcCallback;
import com.netease.lava.nertc.sdk.NERtcConstants;
import com.netease.lava.nertc.sdk.NERtcEx;
import com.netease.lava.nertc.sdk.NERtcOption;
import com.netease.lava.nertc.sdk.NERtcParameters;
import com.netease.lava.nertc.sdk.NERtcUserJoinExtraInfo;
import com.netease.lava.nertc.sdk.NERtcUserLeaveExtraInfo;
import com.netease.lava.nertc.sdk.video.NERtcVideoConfig;
import com.netease.lava.nertc.sdk.video.NERtcVideoView;
import com.netease.lite.BuildConfig;
import com.netease.nertc.config.DemoDeploy;

import java.util.Random;

public class FaceUnityBeautyActivity extends AppCompatActivity implements NERtcCallback {
    private static final String TAG = "FaceUnityBeautyActivity";
    private static final String ROOM_ID = "1383";
    private static final long USER_ID = new Random().nextInt(100000);

    private NERtcVideoView localVideoView;
    private FaceUnityBeautyProcessor beautyProcessor;
    private boolean joined;
    private boolean localAudioEnabled = true;
    private boolean localVideoEnabled = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faceunity_beauty);
        initView();
        beautyProcessor = new FaceUnityBeautyProcessor();
        beautyProcessor.init(getApplicationContext(), FURenderer.CAMERA_FACING_FRONT);
        setupNERtc();
        setupLocalVideo();
        beautyProcessor.start();
        joinChannel(ROOM_ID, USER_ID);
    }

    private void initView() {
        localVideoView = findViewById(R.id.vv_local_user);
        ImageView backView = findViewById(R.id.iv_back);
        backView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exit();
            }
        });
    }

    private void setupNERtc() {
        NERtcEx.getInstance().setParameters(new NERtcParameters());
        NERtcOption options = new NERtcOption();
        options.logLevel = BuildConfig.DEBUG
                ? NERtcConstants.LogLevel.INFO
                : NERtcConstants.LogLevel.WARNING;
        try {
            NERtcEx.getInstance().init(getApplicationContext(), DemoDeploy.APP_KEY, this, options);
        } catch (Exception e) {
            NERtcEx.getInstance().release();
            try {
                NERtcEx.getInstance().init(getApplicationContext(), DemoDeploy.APP_KEY, this, options);
            } catch (Exception ex) {
                Toast.makeText(this, "SDK初始化失败", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
        }
        setLocalAudioEnable(true);
        NERtcVideoConfig config = new NERtcVideoConfig();
        config.videoProfile = 4;
        NERtcEx.getInstance().setLocalVideoConfig(config);
        NERtcEx.getInstance().startVideoPreview();
        setLocalVideoEnable(true);
    }

    private void setupLocalVideo() {
        localVideoView.setZOrderMediaOverlay(true);
        localVideoView.setScalingType(IVideoRender.ScalingType.SCALE_ASPECT_FIT);
        NERtcEx.getInstance().setupLocalVideoCanvas(localVideoView);
    }

    private void joinChannel(String roomId, long userId) {
        NERtcEx.getInstance().joinChannel("", roomId, userId);
    }

    private void setLocalAudioEnable(boolean enable) {
        localAudioEnabled = enable;
        NERtcEx.getInstance().enableLocalAudio(localAudioEnabled);
    }

    private void setLocalVideoEnable(boolean enable) {
        localVideoEnabled = enable;
        NERtcEx.getInstance().enableLocalVideo(true);
        localVideoView.setVisibility(enable ? View.VISIBLE : View.INVISIBLE);
    }

    private void exit() {
        if (joined) {
            leaveChannel();
        }
        finish();
    }

    private boolean leaveChannel() {
        joined = false;
        if (beautyProcessor != null) {
            beautyProcessor.stop();
        }
        setLocalAudioEnable(false);
        setLocalVideoEnable(false);
        int ret = NERtcEx.getInstance().leaveChannel();
        return ret == NERtcConstants.ErrorCode.OK;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        exit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (beautyProcessor != null) {
            beautyProcessor.destroy();
            beautyProcessor = null;
        }
    }

    @Override
    public void onJoinChannel(int result, long channelId, long elapsed, long l2) {
        Log.i(TAG, "onJoinChannel result: " + result + " channelId: " + channelId + " elapsed: " + elapsed);
        joined = result == NERtcConstants.ErrorCode.OK;
    }

    @Override
    public void onLeaveChannel(int i) {
    }

    @Override
    public void onUserJoined(long l) {
    }

    @Override
    public void onUserJoined(long uid, NERtcUserJoinExtraInfo joinExtraInfo) {
    }

    @Override
    public void onUserLeave(long l, int i) {
    }

    @Override
    public void onUserLeave(long uid, int reason, NERtcUserLeaveExtraInfo leaveExtraInfo) {
    }

    @Override
    public void onUserAudioStart(long l) {
    }

    @Override
    public void onUserAudioStop(long l) {
    }

    @Override
    public void onUserVideoStart(long l, int i) {
    }

    @Override
    public void onUserVideoStop(long l) {
    }

    @Override
    public void onDisconnect(int i) {
    }

    @Override
    public void onClientRoleChange(int i, int i1) {
    }
}
