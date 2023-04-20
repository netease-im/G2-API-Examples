package com.netease.nertc.rawaudiocallback;

import androidx.appcompat.app.AppCompatActivity;

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
import com.netease.lava.nertc.sdk.NERtcOption;
import com.netease.lava.nertc.sdk.NERtcParameters;
import com.netease.lava.nertc.sdk.audio.NERtcAudioFrame;
import com.netease.lava.nertc.sdk.audio.NERtcAudioFrameObserver;
import com.netease.lava.nertc.sdk.audio.NERtcAudioFrameOpMode;
import com.netease.lava.nertc.sdk.audio.NERtcAudioFrameRequestFormat;
import com.netease.lava.nertc.sdk.video.NERtcRemoteVideoStreamType;
import com.netease.lava.nertc.sdk.video.NERtcVideoConfig;
import com.netease.lava.nertc.sdk.video.NERtcVideoView;
import com.netease.nertc.config.DemoDeploy;

import java.util.ArrayList;
import java.util.Random;

public class RawAudioCallbackActivity extends AppCompatActivity implements NERtcCallback,View.OnClickListener {
    private static final String TAG = "RawAudioActivity";

    private ArrayList<NERtcVideoView> mRemoteVideoList;
    private EditText mUserIdView;
    private Button mStartJoinBtn;
    private boolean mOpen;
    private EditText mRoomIdView;
    private NERtcVideoView mLocalUserVv;
    private ImageView mBackIv;
    private RelativeLayout mContainer;
    private boolean mJoinChannel = false;
    private boolean mEnableLocalAudio = true;
    private boolean mEnableLocalVideo = true;
    private String mRoomId;
    private long mUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_raw_audio_callback);
        initView();
    }

    @Override
    protected void onDestroy() {
        NERtcEx.getInstance().release();
        super.onDestroy();
    }

    private void openCallback() {
        mOpen = true;
        setRecordAudioParameters();
        setPlaybackAudioParameters();
        NERtcEx.getInstance().setAudioFrameObserver(new NERtcAudioFrameObserver() {
            @Override
            public void onRecordFrame(NERtcAudioFrame neRtcAudioFrame) {
                Log.d(TAG,"onRecordFrame");
            }

            @Override
            public void onRecordSubStreamAudioFrame(NERtcAudioFrame neRtcAudioFrame) {
                Log.d(TAG,"onRecordSubStreamAudioFrame");
            }

            @Override
            public void onPlaybackFrame(NERtcAudioFrame neRtcAudioFrame) {
                Log.d(TAG,"onPlaybackFrame");
            }

            @Override
            public void onPlaybackAudioFrameBeforeMixingWithUserID(long l, NERtcAudioFrame neRtcAudioFrame) {
                Log.d(TAG,"onPlaybackAudioFrameBeforeMixingWithUserID");
            }

            @Override
            public void onPlaybackAudioFrameBeforeMixingWithUserID(long l, NERtcAudioFrame neRtcAudioFrame, long l1) {
                Log.d(TAG,"onPlaybackAudioFrameBeforeMixingWithUserID");
            }

            @Override
            public void onMixedAudioFrame(NERtcAudioFrame neRtcAudioFrame) {
                Log.d(TAG,"onMixedAudioFrame");
            }

            @Override
            public void onPlaybackSubStreamAudioFrameBeforeMixingWithUserID(long l, NERtcAudioFrame neRtcAudioFrame, long l1) {
                Log.d(TAG,"onPlaybackSubStreamAudioFrameBeforeMixingWithUserID");
            }
        });
    }

    private void setPlaybackAudioParameters() {
        NERtcAudioFrameRequestFormat formatMix = new NERtcAudioFrameRequestFormat();
        //单声道、双声道
        formatMix.setChannels(1);
        //采样率
        formatMix.setSampleRate(32000);
        //读写权限
        formatMix.setOpMode(NERtcAudioFrameOpMode.kNERtcAudioFrameOpModeReadWrite);
        NERtcEx.getInstance().setPlaybackAudioFrameParameters(formatMix);
    }

    private void setRecordAudioParameters() {
        NERtcAudioFrameRequestFormat formatMix = new NERtcAudioFrameRequestFormat();
        //单声道、双声道
        formatMix.setChannels(1);
        //采样率
        formatMix.setSampleRate(32000);
        //读写权限
        formatMix.setOpMode(NERtcAudioFrameOpMode.kNERtcAudioFrameOpModeReadWrite);
        NERtcEx.getInstance().setRecordingAudioFrameParameters(formatMix);
    }

    private void initView() {
        mRemoteVideoList = new ArrayList<>();
        mContainer = findViewById(R.id.rl_container);
        mBackIv = findViewById(R.id.iv_back);
        mStartJoinBtn = findViewById(R.id.btn_join_channel);
        mRoomIdView = findViewById(R.id.et_room_id);
        mLocalUserVv = findViewById(R.id.vv_local_user);
        mUserIdView = findViewById(R.id.et_user_id);
        mUserId = new Random().nextInt(100000);
        mUserIdView.setText(String.valueOf(mUserId));
        mStartJoinBtn.setOnClickListener(this);
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
        } else {
            finish();
        }
    }
    private boolean leaveChannel(){

        mJoinChannel = false;
        mOpen = false;
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
    private void setupRemoteVideo(long userId, int index) {
        mRemoteVideoList.get(index).setZOrderMediaOverlay(true);
        mRemoteVideoList.get(index).setScalingType(IVideoRender.ScalingType.SCALE_ASPECT_FIT);
        NERtc.getInstance().setupRemoteVideoCanvas(mRemoteVideoList.get(index), userId);
    }
    private void joinChannel(String roomId, long userId) {
        NERtcEx.getInstance().setChannelProfile(NERtcConstants.RTCChannelProfile.LIVE_BROADCASTING);
        NERtcEx.getInstance().joinChannel("", roomId, userId);
        int ret = NERtcEx.getInstance().enableSuperResolution(true);
        Log.d(TAG,"ret " + ret);
        openCallback();

    }
    private void setVideoProfile() {
        NERtcVideoConfig config = new NERtcVideoConfig();
        // 设置视频质量
        config.videoProfile = NERtcConstants.VideoProfile.STANDARD;
        NERtc.getInstance().setLocalVideoConfig(config);
    }
    private void startJoinRoom() {
        if(!mJoinChannel) {
            userInfo();
            setupNERtc();
            setuplocalVideo();
            setVideoProfile();
            joinChannel(mRoomId, mUserId);
        }
    }
    @Override
    public void onClick(View view) {
        int id = view.getId();
        if(id == R.id.iv_back){
            exit();
        }else if(id == R.id.btn_join_channel){
            startJoinRoom();
        }
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
        if(result != NERtcConstants.ErrorCode.LEAVE_CHANNEL_FOR_SWITCH) {
            finish();
        }
    }

    @Override
    public void onUserJoined(long userId) {
        Log.i(TAG, "onUserJoined userId: " + userId);
        for(int i = 0;i < mRemoteVideoList.size();i++){
            Log.i(TAG, "onUserJoined i: " + i);
            if(mRemoteVideoList.get(i).getTag() == null){
                setupRemoteVideo(userId, i);
                mRemoteVideoList.get(i).setTag(userId);
                break;
            }
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
        if(userView != null){
            userView.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onDisconnect(int i) {
        finish();
    }

    @Override
    public void onClientRoleChange(int oldRole, int newRole) {
        Log.d(TAG, "ClientRoleChange oldRole:" + oldRole + " newRole:" + newRole);
    }
}