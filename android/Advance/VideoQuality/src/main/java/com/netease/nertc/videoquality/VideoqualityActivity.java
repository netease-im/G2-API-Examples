package com.netease.nertc.videoquality;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.netease.lava.api.IVideoRender;
import com.netease.lava.nertc.sdk.NERtc;
import com.netease.lava.nertc.sdk.NERtcCallback;
import com.netease.lava.nertc.sdk.NERtcConstants;
import com.netease.lava.nertc.sdk.NERtcEx;
import com.netease.lava.nertc.sdk.NERtcOption;
import com.netease.lava.nertc.sdk.NERtcParameters;
import com.netease.lava.nertc.sdk.NERtcUserJoinExtraInfo;
import com.netease.lava.nertc.sdk.NERtcUserLeaveExtraInfo;
import com.netease.lava.nertc.sdk.stats.NERtcAudioRecvStats;
import com.netease.lava.nertc.sdk.stats.NERtcAudioSendStats;
import com.netease.lava.nertc.sdk.stats.NERtcNetworkQualityInfo;
import com.netease.lava.nertc.sdk.stats.NERtcStats;
import com.netease.lava.nertc.sdk.stats.NERtcStatsObserver;
import com.netease.lava.nertc.sdk.stats.NERtcVideoRecvStats;
import com.netease.lava.nertc.sdk.stats.NERtcVideoSendStats;
import com.netease.lava.nertc.sdk.video.NERtcRemoteVideoStreamType;
import com.netease.lava.nertc.sdk.video.NERtcVideoConfig;
import com.netease.lava.nertc.sdk.video.NERtcVideoView;
import com.netease.nertc.config.DemoDeploy;

import java.util.ArrayList;
import java.util.Random;

public class VideoqualityActivity extends AppCompatActivity implements NERtcCallback,View.OnClickListener, NERtcStatsObserver {
    private static final String TAG = "VideoqualityActivity";

    private ArrayList<NERtcVideoView> mRemoteVideoList;
    private Button mStartJoinBtn;
    private EditText mRoomIdView;
    private EditText mUserIdView;
    private NERtcVideoView mLocalUserVv;
    private Button mBtnQuality1;
    private Button mBtnQuality2;
    private Button mBtnQuality3;
    private Button mBtnQuality4;
    private ImageView mBackIv;
    private RelativeLayout mContainer;
    private boolean mJoinChannel = false;
    private boolean mEnableLocalAudio = true;
    private boolean mEnableLocalVideo = true;
    private String mRoomId;
    private long mUserId;
    private int mVideoProfile;
    private TextView channelDuration;
    private TextView receiveRate;
    private TextView sendRate;
    private TextView allReceiveBytes;
    private TextView allSendBytes;
    private TextView averageDelay;
    private TextView channelUsers;
    private int mUserNumbers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_videoquality);
        initView();
    }

    private void initView() {
        mRemoteVideoList = new ArrayList<>();
        mContainer = findViewById(R.id.container);
        mBackIv = findViewById(R.id.iv_back);
        mBtnQuality1 = findViewById(R.id.btn_quality_normal);
        mBtnQuality2 = findViewById(R.id.btn_quality_standard);
        mBtnQuality3 = findViewById(R.id.btn_quality_Ultraclear);
        mBtnQuality4 = findViewById(R.id.btn_quality_1080);
        channelDuration = findViewById(R.id.channel_duration);
        receiveRate = findViewById(R.id.receive_rate);
        sendRate = findViewById(R.id.send_rate);
        allReceiveBytes = findViewById(R.id.all_receive_bytes);
        allSendBytes = findViewById(R.id.all_send_bytes);
        averageDelay = findViewById(R.id.average_delay);
        channelUsers = findViewById(R.id.channel_users_number);

        mStartJoinBtn = findViewById(R.id.btn_join_channel);
        mRoomIdView = findViewById(R.id.et_room_id);
        mUserIdView = findViewById(R.id.et_user_id);

        mLocalUserVv = findViewById(R.id.vv_local_user);

        mRemoteVideoList.add((NERtcVideoView) findViewById(R.id.vv_remote_user_1));
        mRemoteVideoList.add((NERtcVideoView) findViewById(R.id.vv_remote_user_2));
        mRemoteVideoList.add((NERtcVideoView) findViewById(R.id.vv_remote_user_3));
        mRemoteVideoList.add((NERtcVideoView) findViewById(R.id.vv_remote_user_4));
        mUserId = new Random().nextInt(100000);
        mUserIdView.setText(String.valueOf(mUserId));


        mStartJoinBtn.setOnClickListener(this);
        mBackIv.setOnClickListener(this);
        mBtnQuality1.setOnClickListener(this);
        mBtnQuality2.setOnClickListener(this);
        mBtnQuality3.setOnClickListener(this);
        mBtnQuality4.setOnClickListener(this);

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
        NERtcEx.getInstance().joinChannel("", roomId, userId);
    }
    private void startJoinRoom() {
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
        }else if(id == R.id.btn_join_channel){
            startJoinRoom();
        }else if(id == R.id.btn_quality_normal){
            if(mJoinChannel) {
                mVideoProfile = NERtcConstants.VideoProfile.Lowest;
                onVideoConfigChange();
            }
            mBtnQuality1.setBackgroundColor(getResources().getColor(R.color.base_blue));
            mBtnQuality2.setBackgroundColor(getResources().getColor(R.color.button_select_off));
            mBtnQuality3.setBackgroundColor(getResources().getColor(R.color.button_select_off));
            mBtnQuality4.setBackgroundColor(getResources().getColor(R.color.button_select_off));
        }else if(id == R.id.btn_quality_standard){
            if(mJoinChannel) {
                mVideoProfile = NERtcConstants.VideoProfile.LOW;
                onVideoConfigChange();
            }
            mBtnQuality1.setBackgroundColor(getResources().getColor(R.color.button_select_off));
            mBtnQuality2.setBackgroundColor(getResources().getColor(R.color.base_blue));
            mBtnQuality3.setBackgroundColor(getResources().getColor(R.color.button_select_off));
            mBtnQuality4.setBackgroundColor(getResources().getColor(R.color.button_select_off));
        }else if(id == R.id.btn_quality_Ultraclear){
            if(mJoinChannel) {
                mVideoProfile = NERtcConstants.VideoProfile.HD720P;
                onVideoConfigChange();
            }
            mBtnQuality1.setBackgroundColor(getResources().getColor(R.color.button_select_off));
            mBtnQuality2.setBackgroundColor(getResources().getColor(R.color.button_select_off));
            mBtnQuality3.setBackgroundColor(getResources().getColor(R.color.base_blue));
            mBtnQuality4.setBackgroundColor(getResources().getColor(R.color.button_select_off));
        }else if(id == R.id.btn_quality_1080){
            if(mJoinChannel) {
                mVideoProfile = NERtcConstants.VideoProfile.HD1080p;
                onVideoConfigChange();
            }
            mBtnQuality1.setBackgroundColor(getResources().getColor(R.color.button_select_off));
            mBtnQuality2.setBackgroundColor(getResources().getColor(R.color.button_select_off));
            mBtnQuality3.setBackgroundColor(getResources().getColor(R.color.button_select_off));
            mBtnQuality4.setBackgroundColor(getResources().getColor(R.color.base_blue));
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
        // 设置视频质量
        config.videoProfile = mVideoProfile;
        NERtc.getInstance().setLocalVideoConfig(config);
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
        for(int i = 0;i < mRemoteVideoList.size();i++){
            Log.i(TAG, "onUserJoined i: " + i);
            if(mRemoteVideoList.get(i).getTag() == null){
                setupRemoteVideo(userId, i);
                mRemoteVideoList.get(i).setTag(userId);
                mUserNumbers++;
                channelUsers.setText("频道中的用户数："+ mUserNumbers);
                break;
            }
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
            mUserNumbers--;
            channelUsers.setText("频道中的用户数："+ mUserNumbers);
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