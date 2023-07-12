package com.netease.nertc.screenshare;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Rect;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;
import com.netease.lava.api.IVideoRender;
import com.netease.lava.nertc.sdk.LastmileProbeResult;
import com.netease.lava.nertc.sdk.NERtc;
import com.netease.lava.nertc.sdk.NERtcCallbackEx;
import com.netease.lava.nertc.sdk.NERtcConstants;
import com.netease.lava.nertc.sdk.NERtcEx;
import com.netease.lava.nertc.sdk.NERtcOption;
import com.netease.lava.nertc.sdk.NERtcParameters;
import com.netease.lava.nertc.sdk.NERtcUserJoinExtraInfo;
import com.netease.lava.nertc.sdk.NERtcUserLeaveExtraInfo;
import com.netease.lava.nertc.sdk.stats.NERtcAudioVolumeInfo;
import com.netease.lava.nertc.sdk.video.NERtcEncodeConfig;
import com.netease.lava.nertc.sdk.video.NERtcRemoteVideoStreamType;
import com.netease.lava.nertc.sdk.video.NERtcScreenConfig;
import com.netease.lava.nertc.sdk.video.NERtcVideoStreamType;
import com.netease.lava.nertc.sdk.video.NERtcVideoView;
import com.netease.nertc.config.DemoDeploy;

import java.nio.ByteBuffer;
import java.util.Random;

public class ScreenShareActivity extends AppCompatActivity implements NERtcCallbackEx,View.OnClickListener{
    private static final String TAG = "ScreenShareActivity";

    private static final int REQUEST_CODE_SCREEN_CAPTURE = 10000;

    private NERtcVideoView mRemoteUserVv;
    private Button mStartJoinBtn;
    private EditText mRoomIdView;
    private EditText mUserIdView;
    private NERtcVideoView mLocalUserVv;
    private NERtcVideoView localScreenRender;
    private NERtcVideoView mRemoteUserRender;

    private ImageView mBackIv;
    private RelativeLayout mContainer;
    private boolean mJoinChannel = false;
    private boolean mEnableLocalAudio = true;
    private boolean mEnableLocalVideo = true;
    private boolean screenStarted;
    private boolean videoStarted = true;
    private String mRoomId;
    private long mUserId;
    private Button mLocalVideoBtn;
    private Button mShareScreenBtn;
    private ScreenShareServiceConnection mServiceConnection;
    private SimpleScreenShareService mScreenService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screen_share);
        initView();
    }

    private void initView() {
        mContainer = findViewById(R.id.container);
        mBackIv = findViewById(R.id.iv_back);
        mStartJoinBtn = findViewById(R.id.btn_join_channel);
        mLocalVideoBtn = findViewById(R.id.btn_local_video);
        mRoomIdView = findViewById(R.id.et_room_id);
        mUserIdView = findViewById(R.id.et_user_id);
        mShareScreenBtn = findViewById(R.id.btn_share_start);
        mLocalUserVv = findViewById(R.id.vv_local_user1);
        localScreenRender = findViewById(R.id.vv_local_user2);
        mRemoteUserVv = findViewById(R.id.vv_remote_user_1);
        mRemoteUserRender = findViewById(R.id.vv_remote_user_2);
        mUserId = new Random().nextInt(100000);
        mUserIdView.setText(String.valueOf(mUserId));

        mLocalVideoBtn.setOnClickListener(this);
        mStartJoinBtn.setOnClickListener(this);
        mBackIv.setOnClickListener(this);
        mShareScreenBtn.setOnClickListener(this);

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
        unbindScreenService();
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
    }
    private void setuplocalVideo() {
        mLocalUserVv.setZOrderMediaOverlay(false);
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
            bindScreenService();
        }
    }

    private void bindScreenService() {
        Intent intent = new Intent();
        intent.setClass(this, SimpleScreenShareService.class);
        mServiceConnection = new ScreenShareServiceConnection();
        bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }
    private void unbindScreenService() {
        if (mServiceConnection != null) {
            unbindService(mServiceConnection);
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if(id == R.id.iv_back){
            exit();
        }else if(id == R.id.btn_join_channel){
            startJoinHome();
        }else if(id == R.id.btn_local_video){
            if(mJoinChannel)
                toggleLocalVideo();
        }else if(id == R.id.btn_share_start){
            if(mJoinChannel)
                toggleScreenCapture();
        }
    }

    private void toggleLocalVideo() {
        videoStarted = !videoStarted;
        NERtcEx.getInstance().setupLocalVideoCanvas(videoStarted ? mLocalUserVv : null);
        NERtcEx.getInstance().enableLocalVideo(videoStarted);
        updateBtnUI();
    }

    private void toggleScreenCapture() {
        if (!screenStarted) {
            requestScreenCapture();
        } else {
            stopScreenCapture();
            screenStarted = false;
            NERtcEx.getInstance().setupLocalSubStreamVideoCanvas(null);
            updateBtnUI();
        }
    }

    private void updateBtnUI() {
        mLocalVideoBtn.setText(videoStarted ? R.string.stop_video: R.string.start_video );
        mShareScreenBtn.setText(screenStarted ? R.string.stop_screen_share : R.string.start_screen_share);
    }

    private void stopScreenCapture() {
        if (mScreenService == null) {
            Toast.makeText(this, R.string.screen_capture_server_is_null, Toast.LENGTH_SHORT).show();
            return;
        }
        // 停止屏幕共享
        mScreenService.stopScreenCapture();
    }

    private void requestScreenCapture() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            startActivityForResult(createScreenCaptureIntent(this), REQUEST_CODE_SCREEN_CAPTURE);
        } else {
            Toast.makeText(this, R.string.screen_capture_min_sdk_version, Toast.LENGTH_SHORT).show();
        }
    }
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static Intent createScreenCaptureIntent(Context context) {
        MediaProjectionManager manager =
                (MediaProjectionManager) context.getSystemService(
                        Context.MEDIA_PROJECTION_SERVICE);
        return manager.createScreenCaptureIntent();
    }
    @Override
    public void onJoinChannel(int result, long channelId, long elapsed, long l2) {
        Log.i(TAG, "onJoinChannel result: " + result + " channelId: " + channelId + " elapsed: " + elapsed);
        if(result == NERtcConstants.ErrorCode.OK){
            mJoinChannel = true;
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
        Log.i(TAG, "onUserJoined");
        if(mRemoteUserVv.getTag() == null){
            setupRemoteVideo(userId);
            mRemoteUserVv.setTag(userId);
        }
    }

    @Override
    public void onUserJoined(long uid, NERtcUserJoinExtraInfo joinExtraInfo) {

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
        NERtcVideoView userView = mContainer.findViewWithTag(userId);
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
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_SCREEN_CAPTURE) {
            if (resultCode == RESULT_OK) {
                startScreenCapture(data);
            } else {
                Toast.makeText(this, R.string.screen_capture_request_denied, Toast.LENGTH_SHORT).show();
            }
        }
    }
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void startScreenCapture(Intent mediaProjectionPermissionResultData) {

        if (mScreenService == null) {
            Toast.makeText(this, R.string.screen_capture_server_start_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        //todo 画布尺寸比例与实际屏幕共享分辨率比例不一致，可能存在截断，实际开发中调整好画布尺寸比例即可
        NERtcEx.getInstance().setupLocalSubStreamVideoCanvas(localScreenRender);

        // 屏幕录制回调
        final MediaProjection.Callback mediaProjectionCallback = new MediaProjection.Callback() {
            @Override
            public void onStop() {
                super.onStop();
            }
        };

        NERtcScreenConfig screenProfile = new NERtcScreenConfig();
        screenProfile.videoProfile = NERtcConstants.VideoProfile.HD1080p;
        screenProfile.frameRate = NERtcEncodeConfig.NERtcVideoFrameRate.FRAME_RATE_FPS_15;
        // 开启屏幕共享
        int result = mScreenService.startScreenCapture(screenProfile,
                mediaProjectionPermissionResultData, // 屏幕录制请求返回的Intent
                mediaProjectionCallback);

        if (result == NERtcConstants.ErrorCode.OK) {
            screenStarted = true;
            updateBtnUI();
        }
    }
    protected void setupRemoteScreenRenderer(NERtcVideoView videoView, long userId) {
        if (videoView != null) {
            videoView.setTag("screen#" + userId);
            videoView.setScalingType(IVideoRender.ScalingType.SCALE_ASPECT_FIT);

        }
        NERtcEx.getInstance().setupRemoteSubStreamVideoCanvas(videoView, userId);
    }
    @Override
    public void onUserSubStreamVideoStart(long userId, int maxProfile) {
        if (mRemoteUserRender.getTag() == null) {
            setupRemoteScreenRenderer(mRemoteUserRender, userId);
            NERtcEx.getInstance().subscribeRemoteSubStreamVideo(userId, true);
            return;
        }
    }

    @Override
    public void onUserSubStreamVideoStop(long userId) {
        String screenTag = "screen#" + userId;
        if (screenTag.equals(mRemoteUserRender.getTag())) {
            mRemoteUserRender.setTag(null);
            setupRemoteScreenRenderer(null, userId);
            NERtcEx.getInstance().subscribeRemoteSubStreamVideo(userId, false);
            return;
        }
    }

    @Override
    public void onUserAudioMute(long l, boolean b) {

    }

    @Override
    public void onUserVideoMute(long l, boolean b) {

    }

    @Override
    public void onUserVideoMute(NERtcVideoStreamType neRtcVideoStreamType, long l, boolean b) {

    }

    @Override
    public void onFirstAudioDataReceived(long l) {

    }

    @Override
    public void onFirstVideoDataReceived(long l) {

    }

    @Override
    public void onFirstVideoDataReceived(NERtcVideoStreamType neRtcVideoStreamType, long l) {

    }

    @Override
    public void onFirstAudioFrameDecoded(long l) {

    }

    @Override
    public void onFirstVideoFrameDecoded(long l, int i, int i1) {

    }

    @Override
    public void onFirstVideoFrameDecoded(NERtcVideoStreamType neRtcVideoStreamType, long l, int i, int i1) {

    }

    @Override
    public void onUserVideoProfileUpdate(long l, int i) {

    }

    @Override
    public void onAudioDeviceChanged(int i) {

    }

    @Override
    public void onAudioDeviceStateChange(int i, int i1) {

    }

    @Override
    public void onVideoDeviceStageChange(int i) {

    }

    @Override
    public void onConnectionTypeChanged(int i) {

    }

    @Override
    public void onReconnectingStart() {

    }

    @Override
    public void onReJoinChannel(int i, long l) {

    }

    @Override
    public void onAudioMixingStateChanged(int i) {

    }

    @Override
    public void onAudioMixingTimestampUpdate(long l) {

    }

    @Override
    public void onAudioEffectTimestampUpdate(long id, long timestampMs) {

    }

    @Override
    public void onAudioEffectFinished(int i) {

    }

    @Override
    public void onLocalAudioVolumeIndication(int i) {

    }

    @Override
    public void onLocalAudioVolumeIndication(int i, boolean b) {

    }

    @Override
    public void onRemoteAudioVolumeIndication(NERtcAudioVolumeInfo[] neRtcAudioVolumeInfos, int i) {

    }

    @Override
    public void onLiveStreamState(String s, String s1, int i) {

    }

    @Override
    public void onConnectionStateChanged(int i, int i1) {

    }

    @Override
    public void onCameraFocusChanged(Rect rect) {

    }

    @Override
    public void onCameraExposureChanged(Rect rect) {

    }

    @Override
    public void onRecvSEIMsg(long l, String s) {

    }

    @Override
    public void onAudioRecording(int i, String s) {

    }

    @Override
    public void onError(int i) {

    }

    @Override
    public void onWarning(int i) {

    }

    @Override
    public void onApiCallExecuted(String apiName, int result, String message) {

    }

    @Override
    public void onMediaRelayStatesChange(int i, String s) {

    }

    @Override
    public void onMediaRelayReceiveEvent(int i, int i1, String s) {

    }

    @Override
    public void onLocalPublishFallbackToAudioOnly(boolean b, NERtcVideoStreamType neRtcVideoStreamType) {

    }

    @Override
    public void onRemoteSubscribeFallbackToAudioOnly(long l, boolean b, NERtcVideoStreamType neRtcVideoStreamType) {

    }

    @Override
    public void onLastmileQuality(int i) {

    }

    @Override
    public void onLastmileProbeResult(LastmileProbeResult lastmileProbeResult) {

    }

    @Override
    public void onMediaRightChange(boolean b, boolean b1) {

    }

    @Override
    public void onVirtualBackgroundSourceEnabled(boolean b, int i) {

    }

    @Override
    public void onUserSubStreamAudioStart(long l) {

    }

    @Override
    public void onUserSubStreamAudioStop(long l) {

    }

    @Override
    public void onUserSubStreamAudioMute(long l, boolean b) {

    }

    @Override
    public void onPermissionKeyWillExpire() {

    }

    @Override
    public void onUpdatePermissionKey(String key, int error, int timeout) {

    }

    @Override
    public void onLocalVideoWatermarkState(NERtcVideoStreamType neRtcVideoStreamType, int i) {

    }

    @Override
    public void onUserDataStart(long uid) {

    }

    @Override
    public void onUserDataStop(long uid) {

    }

    @Override
    public void onUserDataReceiveMessage(long uid, ByteBuffer bufferData, long bufferSize) {

    }

    @Override
    public void onUserDataStateChanged(long uid) {

    }

    @Override
    public void onUserDataBufferedAmountChanged(long uid, long previousAmount) {

    }

    private class ScreenShareServiceConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {

            if (service instanceof SimpleScreenShareService.ScreenShareBinder) {
                mScreenService = ((SimpleScreenShareService.ScreenShareBinder) service).getService();
                Log.i(TAG, "onServiceConnect");
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mScreenService = null;
        }
    }
}