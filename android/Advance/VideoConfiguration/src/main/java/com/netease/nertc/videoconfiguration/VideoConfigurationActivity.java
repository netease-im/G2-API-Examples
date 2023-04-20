package com.netease.nertc.videoconfiguration;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatSpinner;

import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import com.netease.lava.api.IVideoRender;
import com.netease.lava.nertc.sdk.NERtc;
import com.netease.lava.nertc.sdk.NERtcCallback;
import com.netease.lava.nertc.sdk.NERtcConstants;
import com.netease.lava.nertc.sdk.NERtcEx;
import com.netease.lava.nertc.sdk.NERtcOption;
import com.netease.lava.nertc.sdk.NERtcParameters;
import com.netease.lava.nertc.sdk.stats.NERtcAudioRecvStats;
import com.netease.lava.nertc.sdk.stats.NERtcAudioSendStats;
import com.netease.lava.nertc.sdk.stats.NERtcNetworkQualityInfo;
import com.netease.lava.nertc.sdk.stats.NERtcStats;
import com.netease.lava.nertc.sdk.stats.NERtcStatsObserver;
import com.netease.lava.nertc.sdk.stats.NERtcVideoRecvStats;
import com.netease.lava.nertc.sdk.stats.NERtcVideoSendStats;
import com.netease.lava.nertc.sdk.video.NERtcCameraCaptureConfig;
import com.netease.lava.nertc.sdk.video.NERtcEncodeConfig;
import com.netease.lava.nertc.sdk.video.NERtcRemoteVideoStreamType;
import com.netease.lava.nertc.sdk.video.NERtcVideoConfig;
import com.netease.lava.nertc.sdk.video.NERtcVideoView;
import com.netease.nertc.config.DemoDeploy;

import java.util.Random;

public class VideoConfigurationActivity extends AppCompatActivity implements NERtcCallback,View.OnClickListener, NERtcStatsObserver {
    private static final String TAG = "VideoConfigActivity";

    NERtcEncodeConfig.NERtcVideoFrameRate mFramerate;
    NERtcVideoConfig.NERtcVideoOutputOrientationMode mOrientationMode ;
    private Button mStartJoinBtn;
    private EditText mRoomIdView;
    private EditText mUserIdView;
    private NERtcVideoView mLocalUserVv;
    private NERtcVideoView mRemoteUserVv;
    private ImageView mBackIv;
    private RelativeLayout mContainer;
    private boolean mJoinChannel = false;
    private boolean mEnableLocalAudio = true;
    private boolean mEnableLocalVideo = true;
    private String mRoomId;
    private long mUserId;
    private EditText mResHeight;
    private EditText mResWidth;
    private EditText mCapResHeight;
    private EditText mCapResWidth;
    private EditText mBitRate;
    private Switch mMirrorMode;
    private AppCompatSpinner mFrameRateAcs;
    private AppCompatSpinner mOrientationAcs;
    private AppCompatSpinner mVideoCropAcs;
    private AppCompatSpinner mCameraRotationAcs;
    private AppCompatSpinner mLocalScalingTypeAcs;
    private AppCompatSpinner mRemoteScalingTypeAcs;
    private Button mUpdateVideo;
    private int mVideoCropMode;
    private TextView channelDuration;
    private TextView receiveRate;
    private TextView sendRate;
    private TextView allReceiveBytes;
    private TextView allSendBytes;
    private TextView averageDelay;
    private TextView channelUsers;
    private NERtcCameraCaptureConfig.NERtcCaptureExtraRotation mCameraRotation;
    private int mUserNumbers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_configuration);
        initView();
    }

    private void initView() {
        mContainer = findViewById(R.id.rl_container);
        mBackIv = findViewById(R.id.iv_back);
        mResHeight = findViewById(R.id.et_res_height);
        mResWidth = findViewById(R.id.et_res_width);
        mCapResHeight = findViewById(R.id.et_cap_res_height);
        mCapResWidth = findViewById(R.id.et_cap_res_wight);
        mBitRate = findViewById(R.id.et_bit_rate);
        mMirrorMode = findViewById(R.id.st_mirror_mode);

        mLocalScalingTypeAcs = findViewById(R.id.acs_local_scaling_type);
        mFrameRateAcs = findViewById(R.id.acs_frame_rate);
        mOrientationAcs = findViewById(R.id.orientation);
        mVideoCropAcs = findViewById(R.id.acs_video_cut);
        mCameraRotationAcs = findViewById(R.id.acs_camera_rotation);
        mRemoteScalingTypeAcs = findViewById(R.id.acs_remote_scaling_type);
        mStartJoinBtn = findViewById(R.id.btn_join_channel);
        mUpdateVideo = findViewById(R.id.btn_update_video);
        mRoomIdView = findViewById(R.id.et_room_id);
        mUserIdView = findViewById(R.id.et_user_id);
        channelDuration = findViewById(R.id.channel_duration);
        receiveRate = findViewById(R.id.receive_rate);
        sendRate = findViewById(R.id.send_rate);
        allReceiveBytes = findViewById(R.id.all_receive_bytes);
        allSendBytes = findViewById(R.id.all_send_bytes);
        averageDelay = findViewById(R.id.average_delay);
        channelUsers = findViewById(R.id.channel_users_number);
        mLocalUserVv = findViewById(R.id.vv_local_user);
        mRemoteUserVv = findViewById(R.id.vv_remote_user);

        mUserId = new Random().nextInt(100000);
        mUserIdView.setText(String.valueOf(mUserId));
        initFrameRateAcs();
        initOrientationAcs();
        initVideoCropAcs();
        initCameraRotationAcs();
        localScalingType();
        remoteScalingType();

        mStartJoinBtn.setOnClickListener(this);
        mUpdateVideo.setOnClickListener(this);
        mBackIv.setOnClickListener(this);

    }
    /**
     * 设置本地音频可用性
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
    private void exit(){
        if(mJoinChannel){
            leaveChannel();
        }
        finish();
    }
    private boolean leaveChannel(){

        mJoinChannel = false;
        setLocalAudioEnable(false);
        setLocalVideoEnable(false);
        int ret = NERtcEx.getInstance().leaveChannel();
        return ret == NERtcConstants.ErrorCode.OK;
    }
    private void userInfo() {
        Editable roomIdEdit = mRoomIdView.getText();
        if(roomIdEdit == null || roomIdEdit.length() <= 0){
            return;
        }
        Editable userIdEdit = mUserIdView.getText();
        if(userIdEdit == null || userIdEdit.length() <= 0){
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
        NERtcEx.getInstance().setStatsObserver(this);
    }
    private void setuplocalVideo() {
        mLocalUserVv.setZOrderMediaOverlay(true);
        mLocalUserVv.setScalingType(IVideoRender.ScalingType.SCALE_ASPECT_FIT);
        NERtcEx.getInstance().setupLocalVideoCanvas(mLocalUserVv);
    }
    private void setupRemoteVideo(long userId) {
        mRemoteUserVv.setZOrderMediaOverlay(true);
        mRemoteUserVv.setScalingType(IVideoRender.ScalingType.SCALE_ASPECT_FIT);
        NERtc.getInstance().setupRemoteVideoCanvas(mRemoteUserVv, userId);
    }
    private void joinChannel(String roomId, long userId) {
        NERtcEx.getInstance().joinChannel("", roomId, userId);
    }
    private void startJoinHome() {

        if(!mJoinChannel) {
            userInfo();
            setupNERtc();
            setuplocalVideo();
            joinChannel(mRoomId, mUserId);
        }

    }
    @Override
    public void onClick(View view) {
        int id = view.getId();
        if(id == R.id.iv_back){
            exit();
        }else if(id == R.id.btn_join_channel) {
            startJoinHome();
        }else if(id == R.id.btn_update_video){
            if(mJoinChannel)
                onVideoConfigChange();
            else
                Toast.makeText(this,"请先加入房间", Toast.LENGTH_SHORT).show();
        }
    }

    private void onVideoConfigChange() {
        // 关闭本地视频采集以及发送
//        NERtc.getInstance().enableLocalVideo(false);
        setVideoProfile(); //视频质量支持动态设置，不需要开关视频
        // 开启本地视频采集以及发送
//        NERtc.getInstance().enableLocalVideo(true);
    }

    private void setVideoProfile() {
        NERtcVideoConfig config = new NERtcVideoConfig();
        NERtcCameraCaptureConfig cameraConfig = new NERtcCameraCaptureConfig();

        Editable resHeight = mResHeight.getText();
        if(resHeight == null || resHeight.length() <= 0) {
            return;
        }
        Editable resWidth = mResWidth.getText();
        if(resWidth == null || resWidth.length() <= 0){
            return;
        }
        config.height = Integer.parseInt(resHeight.toString());
        config.width = Integer.parseInt(resWidth.toString());

        Editable capResHeight = mCapResHeight.getText();
        if(capResHeight == null || capResHeight.length() <= 0) {
            return;
        }
        Editable capResWidth = mCapResWidth.getText();
        if(capResWidth == null || capResWidth.length() <= 0){
            return;
        }

        cameraConfig.captureHeight = Integer.parseInt(capResHeight.toString());
        cameraConfig.captureWidth = Integer.parseInt(capResWidth.toString());


        config.frameRate = mFramerate;
        Editable bitRate = mBitRate.getText();
        if(bitRate == null || bitRate.length() <= 0){
            return;
        }
        config.bitrate = Integer.parseInt(bitRate.toString());
        config.orientationMode = mOrientationMode;
        config.mirrorMode = mMirrorMode.isChecked() ? NERtcVideoConfig.NERtcVideoMirrorMode.VIDEO_MIRROR_MODE_ENABLED
                : NERtcVideoConfig.NERtcVideoMirrorMode.VIDEO_MIRROR_MODE_DISABLED ;
        config.videoCropMode = mVideoCropMode;

        cameraConfig.extraRotation = mCameraRotation;
        // 设置视频质量
        NERtc.getInstance().setLocalVideoConfig(config);
        //设置相机参数
        NERtc.getInstance().setCameraCaptureConfig(cameraConfig);

    }
    public void initFrameRateAcs(){
        mFrameRateAcs.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String[] options = getResources().getStringArray(R.array.frameRate);
                switch (options[position]){
                    case "7 fps":
                        mFramerate = NERtcEncodeConfig.NERtcVideoFrameRate.FRAME_RATE_FPS_7;
                        break;
                    case "10 fps":
                        mFramerate = NERtcEncodeConfig.NERtcVideoFrameRate.FRAME_RATE_FPS_10;
                        break;
                    case "15 fps":
                        mFramerate = NERtcEncodeConfig.NERtcVideoFrameRate.FRAME_RATE_FPS_15;
                        break;
                    case "24 fps":
                        mFramerate = NERtcEncodeConfig.NERtcVideoFrameRate.FRAME_RATE_FPS_24;
                        break;
                    case "30 fps":
                        mFramerate = NERtcEncodeConfig.NERtcVideoFrameRate.FRAME_RATE_FPS_30;
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }


    public void initVideoCropAcs(){
        mVideoCropAcs.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String[] options = getResources().getStringArray(R.array.videoCropMode);
                switch (options[position]){
                    case "DEFAULT":
                        mVideoCropMode = NERtcConstants.VideoCropMode.DEFAULT;
                        break;
                    case "CROP_16x9":
                        mVideoCropMode = NERtcConstants.VideoCropMode.CROP_16x9;
                        break;
                    case "CROP_4x3":
                        mVideoCropMode = NERtcConstants.VideoCropMode.CROP_4x3;
                        break;
                    case "CROP_1x1":
                        mVideoCropMode = NERtcConstants.VideoCropMode.CROP_1x1;
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }
    public void initCameraRotationAcs(){
        mCameraRotationAcs.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String[] options = getResources().getStringArray(R.array.cameraRotation);
                switch (options[position]){
                    case "DEFAULT":
                        mCameraRotation = NERtcCameraCaptureConfig.NERtcCaptureExtraRotation.CAPTURE_EXTRA_ROTATION_DEFAULT;
                        break;
                    case "ROTATION_180":
                        mCameraRotation = NERtcCameraCaptureConfig.NERtcCaptureExtraRotation.CAPTURE_EXTRA_ROTATION_180;
                        break;
                    case "ANTICLOCKWISE_90":
                        mCameraRotation = NERtcCameraCaptureConfig.NERtcCaptureExtraRotation.CAPTURE_EXTRA_ROTATION_ANTICLOCKWISE_90;
                        break;
                    case "CLOCKWISE_90":
                        mCameraRotation = NERtcCameraCaptureConfig.NERtcCaptureExtraRotation.CAPTURE_EXTRA_ROTATION_CLOCKWISE_90;
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }
    public void initOrientationAcs(){
        mOrientationAcs.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String[] options = getResources().getStringArray(R.array.orientation);
                switch (options[position]){
                    case "Fixed Portrait":
                        mOrientationMode = NERtcVideoConfig.NERtcVideoOutputOrientationMode.VIDEO_OUTPUT_ORIENTATION_MODE_FIXED_PORTRAIT;
                        break;
                    case "Fixed Landscape":
                        mOrientationMode = NERtcVideoConfig.NERtcVideoOutputOrientationMode.VIDEO_OUTPUT_ORIENTATION_MODE_FIXED_LANDSCAPE;
                        break;
                    case "Auto":
                        mOrientationMode = NERtcVideoConfig.NERtcVideoOutputOrientationMode.VIDEO_OUTPUT_ORIENTATION_MODE_ADAPTATIVE;
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }
    public void localScalingType(){
        mLocalScalingTypeAcs.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    String[] options = getResources().getStringArray(R.array.scalingType);
                    switch (options[position]) {
                        case "SCALE_FIT":
                            mLocalUserVv.setScalingType(IVideoRender.ScalingType.SCALE_ASPECT_FIT);
                            break;
                        case "SCALE_FILL":
                            mLocalUserVv.setScalingType(IVideoRender.ScalingType.SCALE_ASPECT_FILL);
                            break;
                        case "SCALE_BALANCED":
                            mLocalUserVv.setScalingType(IVideoRender.ScalingType.SCALE_ASPECT_BALANCED);
                            break;
                    }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }
    public void remoteScalingType(){
        mRemoteScalingTypeAcs.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    String[] options = getResources().getStringArray(R.array.scalingType);
                    switch (options[position]) {
                        case "SCALE_FIT":
                            mRemoteUserVv.setScalingType(IVideoRender.ScalingType.SCALE_ASPECT_FIT);
                            break;
                        case "SCALE_FILL":
                            mRemoteUserVv.setScalingType(IVideoRender.ScalingType.SCALE_ASPECT_FILL);
                            break;
                        case "SCALE_BALANCED":
                            mRemoteUserVv.setScalingType(IVideoRender.ScalingType.SCALE_ASPECT_BALANCED);
                            break;
                    }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }
    @Override
    public void onJoinChannel(int result, long channelId, long elapsed, long l2) {
        Log.i(TAG, "onJoinChannel result: " + result + " channelId: " + channelId + " elapsed: " + elapsed);
        if(result == NERtcConstants.ErrorCode.OK){
            mJoinChannel = true;
            mUserNumbers++;
            channelUsers.setText("频道中的用户数："+ mUserNumbers);
        }
    }

    @Override
    public void onLeaveChannel(int result) {
        Log.i(TAG, "onLeaveChannel result: " + result);
        NERtc.getInstance().release();
        finish();
    }

    @Override
    public void onUserJoined(long userId) {
        Log.i(TAG, "onUserJoined userId: " + userId);
        if(mRemoteUserVv.getTag() == null){
            setupRemoteVideo(userId);
            mRemoteUserVv.setTag(userId);
            mUserNumbers++;
            channelUsers.setText("频道中的用户数："+ mUserNumbers);
        }

    }

    @Override
    public void onUserLeave(long userId, int i) {
        Log.i(TAG, "onUserLeave uid: " + userId);
        NERtcVideoView userView = mContainer.findViewWithTag(userId);
        if(userView != null){
            //设置TAG为null，代表当前没有订阅
            userView.setTag(null);
            NERtcEx.getInstance().subscribeRemoteVideoStream(userId, NERtcRemoteVideoStreamType.kNERtcRemoteVideoStreamTypeHigh, false);
            //不展示远端
            userView.setVisibility(View.INVISIBLE);
            mUserNumbers--;
            channelUsers.setText("频道中的用户数："+ mUserNumbers);
        }
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
        NERtcVideoView userView = mContainer.findViewWithTag(userId);
        Log.d(TAG, userView.toString());
        NERtcEx.getInstance().subscribeRemoteVideoStream(userId, NERtcRemoteVideoStreamType.kNERtcRemoteVideoStreamTypeHigh, true);
        userView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onUserVideoStop(long userId) {
        Log.i(TAG, "onUserVideoStop, uid=" + userId);
        NERtcVideoView userView = mContainer.findViewWithTag(userId);
        if(userView != null){
            userView.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onDisconnect(int i) {

    }

    @Override
    public void onClientRoleChange(int i, int i1) {

    }

    @Override
    public void onRtcStats(NERtcStats neRtcStats) {
        channelDuration.setText("频道时长：" + neRtcStats.totalDuration);
        receiveRate.setText("接收码率："+(neRtcStats.rxAudioKBitRate + neRtcStats.rxVideoKBitRate));
        sendRate.setText("发送码率："+(neRtcStats.txAudioKBitRate + neRtcStats.txVideoKBitRate));
        allReceiveBytes.setText("接收总字节数："+(neRtcStats.rxAudioBytes + neRtcStats.rxVideoBytes));
        allSendBytes.setText("发送总字节数："+(neRtcStats.txAudioBytes + neRtcStats.txVideoBytes));
        averageDelay.setText("上行平均往返时延："+neRtcStats.upRtt);
    }

    @Override
    public void onLocalAudioStats(NERtcAudioSendStats neRtcAudioSendStats) {

    }

    @Override
    public void onRemoteAudioStats(NERtcAudioRecvStats[] neRtcAudioRecvStats) {

    }

    @Override
    public void onLocalVideoStats(NERtcVideoSendStats neRtcVideoSendStats) {

    }

    @Override
    public void onRemoteVideoStats(NERtcVideoRecvStats[] neRtcVideoRecvStats) {

    }

    @Override
    public void onNetworkQuality(NERtcNetworkQualityInfo[] neRtcNetworkQualityInfos) {

    }
}