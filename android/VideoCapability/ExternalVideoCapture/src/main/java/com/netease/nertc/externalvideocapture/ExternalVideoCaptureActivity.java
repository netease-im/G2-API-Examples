package com.netease.nertc.externalvideocapture;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
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
import com.netease.lava.nertc.sdk.NERtcOption;
import com.netease.lava.nertc.sdk.NERtcParameters;
import com.netease.lava.nertc.sdk.video.NERtcRemoteVideoStreamType;
import com.netease.lava.nertc.sdk.video.NERtcVideoView;
import com.netease.nertc.config.DemoDeploy;
import com.netease.nertc.externalvideocapture.externalvideo.ExternalTextureVideoSource;
import com.netease.nertc.externalvideocapture.externalvideo.ExternalVideoSource;

import java.util.Random;

public class ExternalVideoCaptureActivity extends AppCompatActivity implements NERtcCallback, View.OnClickListener {
    private static final String TAG = "ExternalVideoActivity";
    private static final int REQUEST_CODE_REQUEST_CONFIG = 10003;

    private NERtcVideoView mRemoteUserVv;
    private Button mStartJoinBtn;
    private EditText mRoomIdEt;
    private EditText mUserIdEt;
    private NERtcVideoView mLocalUserVv;
    private ImageView mBackIv;
    private RelativeLayout mContainer;
    private boolean mJoinChannel = false;
    private boolean mEnableLocalAudio = true;
    private boolean mEnableLocalVideo = true;
    private String mRoomId;
    private long mUserId;
    private Button mExternFileConfigBtn;
    private String mVideoPath;
    private Button mStartPlayBtn;
    private boolean started = false;
    private ExternalVideoSource mExternalVideoSource;
    private int mVideoWidth;
    private int mVideoHeight;
    private int mVideoFrameRate;
    private int mVideoAngle;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_extermal_video_capture);
        initView();
    }

    @Override
    protected void onDestroy() {
        NERtcEx.getInstance().release();
        super.onDestroy();
    }

    private void initView() {
        mContainer = findViewById(R.id.rl_container);
        mBackIv = findViewById(R.id.iv_back);
        mStartJoinBtn = findViewById(R.id.btn_join_channel);
        mExternFileConfigBtn = findViewById(R.id.btn_set_config);
        mRoomIdEt = findViewById(R.id.et_room_id);
        mUserIdEt = findViewById(R.id.et_user_id);
        mStartPlayBtn = findViewById(R.id.btn_start_play);
        mLocalUserVv = findViewById(R.id.vv_local_user);

        mRemoteUserVv = findViewById(R.id.vv_remote_user_1);
        mUserId = new Random().nextInt(100000);
        mUserIdEt.setText(String.valueOf(mUserId));

        mExternFileConfigBtn.setOnClickListener(this);
        mStartJoinBtn.setOnClickListener(this);
        mBackIv.setOnClickListener(this);
        mStartPlayBtn.setOnClickListener(this);

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

    private void joinChannel() {
        if (!mJoinChannel) {
            userInfo();
            setupNERtc();
            setuplocalVideo();
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
        } else if (id == R.id.btn_set_config) {
            Intent intent = new Intent(ExternalVideoCaptureActivity.this, SetVideoConfigActivity.class);
            intent.putExtra("videoPath", mVideoPath);
            intent.putExtra("videoHeight",mVideoHeight);
            intent.putExtra("videoWidth",mVideoWidth);
            intent.putExtra("videoFrameRate",mVideoFrameRate);
            intent.putExtra("videoAngle",mVideoAngle);
            startActivityForResult(intent, REQUEST_CODE_REQUEST_CONFIG);
        } else if (id == R.id.btn_start_play) {
            toggleExternalVideo();
        }
    }

    //fixme 这时应该是切换到外部输入 或从外部输入切换到相机， 明确下需求及方案描述
    private void toggleExternalVideo() {
        //如果不需要切换，可以在join 时就 setExternalVideoSource true
        if (!mJoinChannel) {
            Toast.makeText(this, R.string.please_join_channel, Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(mVideoPath)) {
            Toast.makeText(this, R.string.please_config_video, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!started) {
            // 创建外部视频源，输入文件路径和视频帧处理回调
            mExternalVideoSource = new ExternalTextureVideoSource(mVideoPath,mVideoWidth,mVideoHeight,mVideoFrameRate,mVideoAngle);
            if (mExternalVideoSource == null) {
                return;
            }
            // 设置为外部视频源
            setExternalVideoSource(true);
            // 开始发送视频数据
            if (!mExternalVideoSource.start()) {
                return;
            }
        } else {
            // 停止发送视频数据
            if (mExternalVideoSource != null) {
                mExternalVideoSource.stop();
                mExternalVideoSource = null;
            }
            // 取消外部视频源
            setExternalVideoSource(false);
        }
        started = !started;
        mStartPlayBtn.setText(started ? R.string.stop_external_video : R.string.start_external_play);
    }

    private void setExternalVideoSource(boolean enable) {
        // 关闭本地视频采集以及发送
        NERtcEx.getInstance().enableLocalVideo(false);
        // 使用外部视频源
        NERtcEx.getInstance().setExternalVideoSource(enable);
        // 开启本地视频采集以及发送
        NERtcEx.getInstance().enableLocalVideo(true);
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
            setupRemoteVideo(userId);
            mRemoteUserVv.setTag(userId);
        }
    }

    @Override
    public void onUserLeave(long userId, int i) {
        Log.i(TAG, "onUserLeave uid: " + userId);
        NERtcVideoView userView = mContainer.findViewWithTag(userId);
        if (userView != null) {
            //设置TAG为null，代表当前没有订阅
            userView.setTag(null);
            NERtcEx.getInstance().subscribeRemoteVideoStream(userId, NERtcRemoteVideoStreamType.kNERtcRemoteVideoStreamTypeHigh, false);
            //不展示远端
            userView.setVisibility(View.INVISIBLE);
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
        NERtcEx.getInstance().subscribeRemoteVideoStream(userId, NERtcRemoteVideoStreamType.kNERtcRemoteVideoStreamTypeHigh, true);
        userView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onUserVideoStop(long userId) {
        Log.i(TAG, "onUserVideoStop, uid=" + userId);
        NERtcVideoView userView = mContainer.findViewWithTag(userId);
        if (userView != null) {
            userView.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_CODE_REQUEST_CONFIG){
            mVideoPath = data.getStringExtra("videoPath");
            String videoWidth = data.getStringExtra("videoWidth");
            String videoHeight = data.getStringExtra("videoHeight");
            String videoFrameRate = data.getStringExtra("videoFrameRate");
            String videoAngle = data.getStringExtra("videoAngle");
            mVideoWidth = Integer.parseInt(videoWidth.equals("") ? "0" :  videoWidth);
            mVideoHeight = Integer.parseInt(videoHeight.equals("") ? "0" :  videoHeight);
            mVideoFrameRate = Integer.parseInt(videoFrameRate.equals("") ? "0" :  videoFrameRate);
            mVideoAngle = Integer.parseInt(videoAngle.equals("") ? "0" :  videoAngle);
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