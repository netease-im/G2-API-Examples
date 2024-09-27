package com.netease.nertc.externalvideorender;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;
import com.netease.lava.api.IVideoRender;
import com.netease.lava.nertc.sdk.NERtc;
import com.netease.lava.nertc.sdk.NERtcCallback;
import com.netease.lava.nertc.sdk.NERtcConstants;
import com.netease.lava.nertc.sdk.NERtcEx;
import com.netease.lava.nertc.sdk.NERtcExternalVideoRenderer;
import com.netease.lava.nertc.sdk.NERtcOption;
import com.netease.lava.nertc.sdk.NERtcParameters;
import com.netease.lava.nertc.sdk.NERtcUserJoinExtraInfo;
import com.netease.lava.nertc.sdk.NERtcUserLeaveExtraInfo;
import com.netease.lava.nertc.sdk.video.NERtcRemoteVideoStreamType;
import com.netease.lava.nertc.sdk.video.NERtcVideoStreamType;
import com.netease.lava.webrtc.EglBase;
import com.netease.lite.BuildConfig;
import com.netease.nertc.config.DemoDeploy;

import java.util.Random;

public class ExternalVideoRenderActivity extends AppCompatActivity implements NERtcCallback, View.OnClickListener {
    private static final String TAG = "ExternalRenderActivity";
    private static final int REQUEST_CODE_REQUEST_CONFIG = 10003;

    private DemoRtcVideoView mRemoteUserVv;
    private Button mStartJoinBtn;
    private EditText mRoomIdEt;
    private EditText mUserIdEt;
    private DemoRtcVideoView mLocalUserVv;
    private ImageView mBackIv;
    private RelativeLayout mContainer;
    private boolean mJoinChannel = false;
    private boolean mEnableLocalAudio = true;
    private boolean mEnableLocalVideo = true;
    private String mRoomId;
    private long mUserId;
    private String mVideoPath;
    private boolean started = false;
    protected EglBase eglBase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_extermal_video_render);
        eglBase = EglBase.create();
        initView();
    }

    @Override
    protected void onDestroy() {
        NERtcEx.getInstance().release();
        eglBase.release();
        super.onDestroy();
    }

    private void initView() {
        mContainer = findViewById(R.id.rl_container);
        mBackIv = findViewById(R.id.iv_back);
        mStartJoinBtn = findViewById(R.id.btn_join_channel);
        mRoomIdEt = findViewById(R.id.et_room_id);
        mUserIdEt = findViewById(R.id.et_user_id);
        mLocalUserVv = findViewById(R.id.vv_local_user);
        mRemoteUserVv = findViewById(R.id.vv_remote_user_1);
        mUserId = new Random().nextInt(100000);
        mUserIdEt.setText(String.valueOf(mUserId));

        mStartJoinBtn.setOnClickListener(this);
        mBackIv.setOnClickListener(this);

    }

    /**
     * 设置本地音频可用性
     *
     * @param enable
     */
    private void setLocalAudioEnable(boolean enable) {
        mEnableLocalAudio = enable;
        NERtcEx.getInstance().enableLocalAudio(mEnableLocalAudio);
    }

    /**
     * 设置本地视频的可用性
     */
    private void setLocalVideoEnable(boolean enable) {
        mEnableLocalVideo = enable;
        NERtcEx.getInstance().enableLocalVideo(mEnableLocalVideo);
        mLocalUserVv.setVisibility(enable ? View.VISIBLE : View.INVISIBLE);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        exit();
    }

    /**
     * 退出房间并关闭页面
     */
    private void exit() {
        if (mJoinChannel) {
            leaveChannel();
        } else {
            finish();
        }
    }

    private boolean leaveChannel() {
        mJoinChannel = false;
        setLocalAudioEnable(false);
        setLocalVideoEnable(false);
        int ret = NERtcEx.getInstance().leaveChannel();
        return ret == NERtcConstants.ErrorCode.OK;
    }

    private void userInfo() {
        Editable roomIdEdit = mRoomIdEt.getText();
        if (roomIdEdit == null || roomIdEdit.length() <= 0) {
            return;
        }
        Editable userIdEdit = mUserIdEt.getText();
        if (userIdEdit == null || userIdEdit.length() <= 0) {
            return;
        }
        mRoomId = roomIdEdit.toString();
        mUserId = Long.parseLong(userIdEdit.toString());
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
            NERtcEx.getInstance().init(getApplicationContext(), DemoDeploy.APP_KEY, this, options);
        } catch (Exception e) {
            // 可能由于没有release导致初始化失败，release后再试一次
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
        setLocalVideoEnable(true);
    }

    private void setupLocalExternalVideoCanvas(){
        mLocalUserVv.init(this, eglBase.getEglBaseContext(), true, NERtcConstants.NERtcExternalVideoRendererBufferType.I420);
        mLocalUserVv.setMirror(true);
        mLocalUserVv.setScalingType(IVideoRender.ScalingType.SCALE_ASPECT_FIT);
        NERtcEx.getInstance().setLocalExternalVideoRenderer(NERtcVideoStreamType.kNERtcVideoStreamTypeMain,(NERtcExternalVideoRenderer)  mLocalUserVv.getRenderer());
    }

    private void setupRemoteExternalVideoCanvas(long userId){
        mRemoteUserVv.init(this, eglBase.getEglBaseContext(), true, NERtcConstants.NERtcExternalVideoRendererBufferType.I420);
        mRemoteUserVv.setScalingType(IVideoRender.ScalingType.SCALE_ASPECT_FIT);
        NERtcEx.getInstance().setRemoteExternalVideoRenderer(NERtcVideoStreamType.kNERtcVideoStreamTypeMain,userId ,(NERtcExternalVideoRenderer) mRemoteUserVv.getRenderer());
    }

    private void joinChannel(String roomId, long userId) {
        NERtcEx.getInstance().joinChannel("", roomId, userId);
    }

    private void joinChannel() {
        if (!mJoinChannel) {
            userInfo();
            setupNERtc();
            setupLocalExternalVideoCanvas();
            joinChannel(mRoomId, mUserId);
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.iv_back) {
            exit();
        } else if (id == R.id.btn_join_channel) {
            joinChannel();
        }
//        } else if (id == R.id.btn_set_config) {
//            Intent intent = new Intent(ExternalVideoRenderActivity.this, SetVideoConfigActivity.class);
//            intent.putExtra("videoPath", mVideoPath);
//            intent.putExtra("videoHeight",mVideoHeight);
//            intent.putExtra("videoWidth",mVideoWidth);
//            intent.putExtra("videoFrameRate",mVideoFrameRate);
//            intent.putExtra("videoAngle",mVideoAngle);
//            startActivityForResult(intent, REQUEST_CODE_REQUEST_CONFIG);
//        } else if (id == R.id.btn_start_play) {
//            toggleExternalVideo();
//        }
    }

    @Override
    public void onJoinChannel(int result, long channelId, long elapsed, long l2) {
        Log.i(TAG, "onJoinChannel result: " + result + " channelId: " + channelId + " elapsed: " + elapsed);
        if (result == NERtcConstants.ErrorCode.OK) {
            mJoinChannel = true;
        }
    }

    @Override
    public void onLeaveChannel(int result) {
        Log.i(TAG, "onLeaveChannel result: " + result);
        finish();
    }

    @Override
    public void onUserJoined(long userId) {
        Log.i(TAG, "onUserJoined userId: " + userId);
        Log.i(TAG, "onUserJoined");
        if (mRemoteUserVv.getTag() == null) {
            setupRemoteExternalVideoCanvas(userId);
            mRemoteUserVv.setTag(userId);
        }
    }

    @Override
    public void onUserJoined(long uid, NERtcUserJoinExtraInfo joinExtraInfo) {

    }

    @Override
    public void onUserLeave(long userId, int i) {
        Log.i(TAG, "onUserLeave uid: " + userId);
        DemoRtcVideoView userView = mContainer.findViewWithTag(userId);
        if (userView != null) {
            //设置TAG为null，代表当前没有订阅
            userView.setTag(null);
            NERtcEx.getInstance().subscribeRemoteVideoStream(userId, NERtcRemoteVideoStreamType.kNERtcRemoteVideoStreamTypeHigh, false);
            //不展示远端
            userView.setVisibility(View.INVISIBLE);
        }
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
    public void onUserVideoStart(long userId, int profile) {
        Log.i(TAG, "onUserVideoStart uid: " + userId + " profile: " + profile);
        DemoRtcVideoView userView = mContainer.findViewWithTag(userId);
        NERtcEx.getInstance().subscribeRemoteVideoStream(userId, NERtcRemoteVideoStreamType.kNERtcRemoteVideoStreamTypeHigh, true);
        userView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onUserVideoStop(long userId) {
        Log.i(TAG, "onUserVideoStop, uid=" + userId);
        DemoRtcVideoView userView = mContainer.findViewWithTag(userId);
        if (userView != null) {
            userView.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_CODE_REQUEST_CONFIG){
            mVideoPath = data.getStringExtra("videoPath");
        }
    }

    @Override
    public void onDisconnect(int i) {
        finish();
    }

    @Override
    public void onClientRoleChange(int i, int i1) {

    }
}