package com.netease.nertc.videocall;
import androidx.appcompat.app.AppCompatActivity;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
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
import com.netease.lava.nertc.sdk.video.NERtcRemoteVideoStreamType;
import com.netease.lava.nertc.sdk.video.NERtcVideoView;
import com.netease.nertc.config.DemoDeploy;

import java.util.ArrayList;

public class VideoCallActivity extends AppCompatActivity implements NERtcCallback ,View.OnClickListener{
    private static final String EXTRA_ROOM_ID = "extra_room_id";
    private static final String EXTRA_USER_ID = "extra_user_id";
    private static final String TAG = "VideoCallActivity";
    private String mRoomId;

    private boolean mEnableLocalAudio = true;
    private boolean mEnableLocalVideo = true;
    private boolean mJoinChannel = false;
    private boolean mIsFrontCamera = true;
    private boolean mIsSpeakerPhone = true;
    private long mUserId;

    private RelativeLayout mContainer;
    private TextView mRoomTittleTv;
    private ImageView mBackIv;
    private ArrayList<NERtcVideoView> mRemoteVvList;
    private NERtcVideoView mLocalUserVv;
    private Button mSwitchCameraBtn;
    private Button mMuteCameraBtn;
    private Button mMuteMkBtn;
    private Button mAudioRouteBtn;

    public static void startActivity(Activity from, String roomId, long userId){
        Intent intent = new Intent(from, VideoCallActivity.class);
        intent.putExtra(EXTRA_ROOM_ID, roomId);
        intent.putExtra(EXTRA_USER_ID, userId);
        from.startActivity(intent);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_call);
        mRoomId = getIntent().getStringExtra(EXTRA_ROOM_ID);
        mUserId = getIntent().getLongExtra(EXTRA_USER_ID,-1);
        initView();
        setupNERtc();
        setupLocalVideo();
        joinChannel(mUserId, mRoomId);

    }

    @Override
    protected void onDestroy() {
        NERtcEx.getInstance().release();
        super.onDestroy();
    }

    /**
     * 初始化本地视频
     */
    private void setupLocalVideo() {
        // 如果小画布置于大画布上面，小画布需要 setZOrderMediaOverlay(true)
        // mLocalUserVv.setZOrderMediaOverlay(true);
        mLocalUserVv.setScalingType(IVideoRender.ScalingType.SCALE_ASPECT_FIT);
        NERtcEx.getInstance().setupLocalVideoCanvas(mLocalUserVv);
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

    /**
     * 离开房间
     * @return
     */
    private boolean leaveChannel(){
        mJoinChannel = false;
        setLocalAudioEnable(false);
        setLocalVideoEnable(false);
        int ret = NERtcEx.getInstance().leaveChannel();
        return ret == NERtcConstants.ErrorCode.OK;
    }

    private void initView(){
        mContainer = findViewById(R.id.container);
        mRoomTittleTv = findViewById(R.id.tv_room_id);
        mBackIv = findViewById(R.id.iv_back);

        mRemoteVvList = new ArrayList<>();
        mRemoteVvList.add((NERtcVideoView)findViewById(R.id.vv_remote_user_1));
        mRemoteVvList.add((NERtcVideoView)findViewById(R.id.vv_remote_user_2));
        mRemoteVvList.add((NERtcVideoView)findViewById(R.id.vv_remote_user_3));
        mRemoteVvList.add((NERtcVideoView)findViewById(R.id.vv_remote_user_4));

        mLocalUserVv = findViewById(R.id.vv_local_user);
        mSwitchCameraBtn = findViewById(R.id.btn_switch_camera);
        mMuteCameraBtn = findViewById(R.id.btn_mute_video);
        mMuteMkBtn = findViewById(R.id.btn_change_mk);
        mAudioRouteBtn = findViewById(R.id.btn_audio_route);

        mBackIv.setOnClickListener(this);
        mSwitchCameraBtn.setOnClickListener(this);
        mMuteCameraBtn.setOnClickListener(this);
        mMuteMkBtn.setOnClickListener(this);
        mAudioRouteBtn.setOnClickListener(this);

        if(!TextUtils.isEmpty(mRoomId)){
            mRoomTittleTv.setText(getString(R.string.room_id) + mRoomId);
        }
    }

    /**
     * 初始化NERtc
     */
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

    /**
     *  加入房间
     * @param userId 用户ID
     * @param roomId 房间ID
     */
    private void joinChannel(long userId, String roomId) {
        Log.i(TAG, "joinChannel userId: " + userId);
        NERtcEx.getInstance().joinChannel(null, roomId, userId);

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
    /**
     * 切换前后置摄像头
     */
    private void switchCamera(){
        mIsFrontCamera = !mIsFrontCamera;
        NERtcEx.getInstance().switchCamera();
        if(mIsFrontCamera){
            mSwitchCameraBtn.setText(getString(R.string.videocall_user_back_camera));
        }else{
            mSwitchCameraBtn.setText(getString(R.string.videocall_user_front_camera));
        }
    }
    /**
     * 改变摄像头可用状态
     */
    private void changeVideoEnable() {
        mEnableLocalVideo = !mEnableLocalVideo;
        setLocalVideoEnable(mEnableLocalVideo);
        if(mEnableLocalVideo){
            mMuteCameraBtn.setText(getString(R.string.videocall_close_camera));
        }else{
            mMuteCameraBtn.setText(getString(R.string.videocall_open_camera));
        }
    }

    /**
     * 改变音频可用状态
     */
    private void changeAudioEnable(){
        mEnableLocalAudio = !mEnableLocalAudio;
        setLocalAudioEnable(mEnableLocalAudio);
        if(mEnableLocalAudio){
            mMuteMkBtn.setText(getString(R.string.videocall_close_audio));
        }else{
            mMuteMkBtn.setText(getString(R.string.videocall_open_audio));
        }
    }

    /**
     * 切换听筒，扬声器
     */
    private void changeAudioRoute(){
        mIsSpeakerPhone = !mIsSpeakerPhone;
        NERtcEx.getInstance().setSpeakerphoneOn(mIsSpeakerPhone);
        if(mIsSpeakerPhone){
            mAudioRouteBtn.setText(getString(R.string.videocall_use_receiver));
        }else{
            mAudioRouteBtn.setText(getString(R.string.videocall_use_speaker));
        }
    }

    /**
     * 初始化远端用户画面
     * @param userId
     * @param index
     */
    private void setupRemoteVideo(long userId, int index) {
        // 如果小画布置于大画布上面，小画布需要 setZOrderMediaOverlay(true)
        // mRemoteVvList.setZOrderMediaOverlay(true);
        mRemoteVvList.get(index).setScalingType(IVideoRender.ScalingType.SCALE_ASPECT_FIT);
        NERtc.getInstance().setupRemoteVideoCanvas(mRemoteVvList.get(index), userId);
    }
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        exit();
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
        finish();
    }

    @Override
    public void onUserJoined(long userId){
        Log.i(TAG, "onUserJoined userId: " + userId);
        for(int i = 0;i < mRemoteVvList.size();i++){
            if(mRemoteVvList.get(i).getTag() == null){
                setupRemoteVideo(userId, i);
                mRemoteVvList.get(i).setTag(userId);
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
            //不展示远端
            userView.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onUserAudioStart(long userId) {
        Log.i(TAG, "onUserAudioStart uid: " + userId);
    }

    @Override
    public void onUserAudioStop(long userId) {
        Log.i(TAG, "onUserAudioStop, uid=" + userId);
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
            NERtcEx.getInstance().subscribeRemoteVideoStream(userId, NERtcRemoteVideoStreamType.kNERtcRemoteVideoStreamTypeHigh, false);
            userView.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onDisconnect(int i) {
        Log.i(TAG, "onDisconnect uid: " + i);
        finish();
    }

    @Override
    public void onClientRoleChange(int old, int newRole) {
        Log.i(TAG, "onUserAudioStart old: " + old + ", newRole : " + newRole);
    }
    @Override
    public void onClick(View view) {
        int id = view.getId();
        if(id == R.id.iv_back){
            exit();
        }else if(id == R.id.btn_switch_camera){
            switchCamera();
        }else if(id == R.id.btn_mute_video){
            changeVideoEnable();
        }else if(id == R.id.btn_change_mk){
            changeAudioEnable();
        }else if(id == R.id.btn_audio_route){
            changeAudioRoute();
        }
    }
}