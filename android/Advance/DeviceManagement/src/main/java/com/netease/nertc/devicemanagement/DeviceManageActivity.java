package com.netease.nertc.devicemanagement;

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
import android.widget.SeekBar;
import android.widget.Switch;
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

import java.util.Random;

public class DeviceManageActivity extends AppCompatActivity implements NERtcCallback,View.OnClickListener {
    private static final String TAG = "DeviceManageActivity";

    private Button mStartJoinBtn;
    private EditText mRoomIdView;
    private EditText mUserIdView;
    private NERtcVideoView mLocalUserVv;
    private ImageView mBackIv;
    private RelativeLayout mContainer;
    private boolean mJoinChannel = false;
    private boolean mEnableLocalAudio = true;
    private boolean mEnableLocalVideo = true;
    private boolean mflashon;
    private String mRoomId;
    private long mUserId;
    private Switch mMirrorMode;
    private AppCompatSpinner mSwatchCamera;
    private SeekBar mCameraZoomSB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_manage);
        initView();
    }

    private void initView() {
        mContainer = findViewById(R.id.container);
        mBackIv = findViewById(R.id.iv_back);
        mMirrorMode = findViewById(R.id.st_flash_on);

        mSwatchCamera = findViewById(R.id.acs_swatch_camera);
        mCameraZoomSB = findViewById(R.id.sb_camera_zoom);
        mStartJoinBtn = findViewById(R.id.btn_join_channel);
        mRoomIdView = findViewById(R.id.et_room_id);
        mUserIdView = findViewById(R.id.et_user_id);
        mLocalUserVv = findViewById(R.id.vv_local_user);

        mUserId = new Random().nextInt(100000);
        mUserIdView.setText(String.valueOf(mUserId));
        initSwitchCameraAcs();
        initCameraZoomSB();

        mStartJoinBtn.setOnClickListener(this);
        mMirrorMode.setOnClickListener(this);
        mBackIv.setOnClickListener(this);

    }

    private void initCameraZoomSB() {
        mCameraZoomSB.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if(!mJoinChannel)
                    return;
                NERtcEx.getInstance().setCameraZoomFactor(seekBar.getProgress());
            }
        });
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
    }
    private void setuplocalVideo() {
        mLocalUserVv.setZOrderMediaOverlay(true);
        mLocalUserVv.setScalingType(IVideoRender.ScalingType.SCALE_ASPECT_FIT);
        NERtcEx.getInstance().setupLocalVideoCanvas(mLocalUserVv);
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
        }else if(id == R.id.st_flash_on){
            if(mJoinChannel)
                openflash();
        }
    }

    private void openflash() {
        if(!NERtcEx.getInstance().isCameraTorchSupported()){
            Toast.makeText(this, "前置摄像头不支持闪光灯常开！",Toast.LENGTH_SHORT).show();
            mMirrorMode.setChecked(false);
            return;
        }
        mflashon = !mflashon;
        if(mflashon){
            NERtcEx.getInstance().setCameraTorchOn(mflashon);
        }else{
            NERtcEx.getInstance().setCameraTorchOn(mflashon);
        }
    }

    public void initSwitchCameraAcs(){
        mSwatchCamera.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String[] options = getResources().getStringArray(R.array.camera);
                switch (options[position]){
                    case "Front":
                        NERtcEx.getInstance().switchCameraWithPosition(NERtcConstants.CameraPosition.CAMERA_POSITION_FRONT);
                        break;
                    case "Back":
                        NERtcEx.getInstance().switchCameraWithPosition(NERtcConstants.CameraPosition.CAMERA_POSITION_BACK);
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

    }

    @Override
    public void onUserLeave(long userId, int i) {
        Log.i(TAG, "onUserLeave uid: " + userId);
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

}