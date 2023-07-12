package com.netease.nertc.soundeffectsetting;

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
import com.netease.lava.nertc.sdk.NERtcUserJoinExtraInfo;
import com.netease.lava.nertc.sdk.NERtcUserLeaveExtraInfo;
import com.netease.lava.nertc.sdk.audio.NERtcVoiceBeautifierType;
import com.netease.lava.nertc.sdk.audio.NERtcVoiceChangerType;
import com.netease.lava.nertc.sdk.video.NERtcRemoteVideoStreamType;
import com.netease.lava.nertc.sdk.video.NERtcVideoView;
import com.netease.nertc.config.DemoDeploy;

import java.util.ArrayList;
import java.util.Random;

public class SoundEffectSeetingActivity extends AppCompatActivity implements NERtcCallback, View.OnClickListener{
    private static final String TAG = "SoundEctActivity";

    private boolean mPushStreamState = false;
    private boolean mEnableLocalAudio = true;
    private boolean mEnableLocalVideo = true;
    private boolean mJoinChannel = false;

    private int mVoiceEffectIndex = 0;//变声状态
    private int mVoiceBeautifulIndex = 0;//美声状态
    private long mUserId;
    private String mRoomId;

    private ImageView mBackIv;
    private Button mStartJoinBtn;
    private EditText mRoomIdView;
    private EditText mUserIdView;
    private NERtcVideoView mLocalUserVv;
    private ArrayList<NERtcVideoView> mRemoteVideoList;
    private RelativeLayout mContainer;
    private ArrayList<Button> mBtnsVoiceChange;
    private ArrayList<Button> mBtnsEffectChange;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sound_effect_seeting);
        initView();
    }

    @Override
    protected void onDestroy() {
        NERtcEx.getInstance().release();
        super.onDestroy();
    }

    private void initView() {
        mRemoteVideoList = new ArrayList<>();
        mBtnsVoiceChange = new ArrayList<>();
        mBtnsEffectChange = new ArrayList<>();

        mContainer = findViewById(R.id.rl_container);
        mBackIv = findViewById(R.id.iv_back);

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

        mBtnsVoiceChange.add(findViewById(R.id.btn_vocie_1));
        mBtnsVoiceChange.add(findViewById(R.id.btn_vocie_2));
        mBtnsVoiceChange.add(findViewById(R.id.btn_vocie_3));
        mBtnsVoiceChange.add(findViewById(R.id.btn_vocie_4));
        mBtnsVoiceChange.add(findViewById(R.id.btn_vocie_5));

        mBtnsEffectChange.add(findViewById(R.id.btn_effect_1));
        mBtnsEffectChange.add(findViewById(R.id.btn_effect_2));
        mBtnsEffectChange.add(findViewById(R.id.btn_effect_3));
        mBtnsEffectChange.add(findViewById(R.id.btn_effect_4));
        mBtnsEffectChange.add(findViewById(R.id.btn_effect_5));

        for(Button btn : mBtnsVoiceChange){
            btn.setOnClickListener(this);
        }
        for(Button btn : mBtnsEffectChange){
            btn.setOnClickListener(this);
        }
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
     * 初始化远端用户视频
     * @param userId 用户id
     * @param index 组件索引
     */
    private void setupRemoteVideo(long userId, int index) {
        mRemoteVideoList.get(index).setZOrderMediaOverlay(true);
        mRemoteVideoList.get(index).setScalingType(IVideoRender.ScalingType.SCALE_ASPECT_FIT);
        NERtc.getInstance().setupRemoteVideoCanvas(mRemoteVideoList.get(index), userId);
    }

    /**
     * 初始化本地用户视频
     */
    private void setuplocalVideo() {
        mLocalUserVv.setZOrderMediaOverlay(false);
        mLocalUserVv.setScalingType(IVideoRender.ScalingType.SCALE_ASPECT_FIT);
        NERtcEx.getInstance().setupLocalVideoCanvas(mLocalUserVv);
    }
    private void startPushStream() {
        userInfo();
        setupNERtc();
        setuplocalVideo();
        joinChannel(mRoomId, mUserId);
    }
    private void joinChannel(String roomId, long userId) {
        NERtcEx.getInstance().joinChannel("", roomId, userId);
    }
    @Override
    public void onClick(View view) {
        int id = view.getId();
        if(id == R.id.iv_back){
            exit();
        } else if (id == R.id.btn_vocie_1 || id == R.id.btn_vocie_2 || id == R.id.btn_vocie_3 || id == R.id.btn_vocie_4 || id == R.id.btn_vocie_5) {
            selectVoiceState(id);
            if (mPushStreamState) {
                changeVoiceEffect();
            }
            refreshVoiceButton(id,mBtnsVoiceChange);
        } else if (id == R.id.btn_effect_1 || id == R.id.btn_effect_2 || id == R.id.btn_effect_3 || id == R.id.btn_effect_4 || id == R.id.btn_effect_5) {
            selectVoiceBeautifierState(id);
            if (mPushStreamState) {
                changeBeautyEffect();
            }
            refreshVoiceButton(id,mBtnsEffectChange);
        } else if (id == R.id.btn_join_channel) {
            mPushStreamState = true;
            startPushStream();
        }
    }


    private void selectVoiceState(int btn) {
        if(btn == R.id.btn_vocie_1){
            mVoiceEffectIndex = NERtcVoiceChangerType.AUDIO_EFFECT_OFF;
        }else if(btn == R.id.btn_vocie_2){
            mVoiceEffectIndex = NERtcVoiceChangerType.VOICE_CHANGER_EFFECT_MANTOWOMAN;
        }else if(btn == R.id.btn_vocie_3){
            mVoiceEffectIndex = NERtcVoiceChangerType.VOICE_CHANGER_EFFECT_WOMANTOMAN;
        }else if(btn == R.id.btn_vocie_4){
            mVoiceEffectIndex = NERtcVoiceChangerType.VOICE_CHANGER_EFFECT_MANTOLOLI;
        }else if(btn == R.id.btn_vocie_5){
            mVoiceEffectIndex = NERtcVoiceChangerType.VOICE_CHANGER_EFFECT_WOMANTOLOLI;
        }
    }

    private void changeVoiceEffect() {
        NERtcEx.getInstance().setAudioEffectPreset(mVoiceEffectIndex);
    }

    private void changeBeautyEffect() {
        NERtcEx.getInstance().setVoiceBeautifierPreset(mVoiceBeautifulIndex);
    }

    private void selectVoiceBeautifierState(int btn) {
        if(btn == R.id.btn_effect_1){
            mVoiceBeautifulIndex = NERtcVoiceBeautifierType.VOICE_BEAUTIFIER_OFF;
        }else if(btn == R.id.btn_effect_2){
            mVoiceBeautifulIndex = NERtcVoiceBeautifierType.VOICE_BEAUTIFIER_KTV;
        }else if(btn == R.id.btn_effect_3){
            mVoiceBeautifulIndex = NERtcVoiceBeautifierType.VOICE_BEAUTIFIER_BEDROOM;
        }else if(btn == R.id.btn_effect_4){
            mVoiceBeautifulIndex = NERtcVoiceBeautifierType.VOICE_BEAUTIFIER_MAGNETIC;
        }else if(btn == R.id.btn_effect_5){
            mVoiceBeautifulIndex = NERtcVoiceBeautifierType.VOICE_BEAUTIFIER_REMOTE;
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
        finish();
    }

    @Override
    public void onUserJoined(long userId) {
        Log.i(TAG, "onUserJoined userId: " + userId);
        for(int i = 0;i < mRemoteVideoList.size();i++){
            if(mRemoteVideoList.get(i).getTag() == null){
                setupRemoteVideo(userId, i);
                mRemoteVideoList.get(i).setTag(userId);
                break;
            }
        }
    }

    @Override
    public void onUserJoined(long uid, NERtcUserJoinExtraInfo joinExtraInfo) {

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        exit();
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
        finish();
    }

    @Override
    public void onClientRoleChange(int i, int i1) {

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
        mPushStreamState = false;
        setLocalAudioEnable(false);
        setLocalVideoEnable(false);
        NERtcEx.getInstance().stopAudioMixing();
        int ret = NERtcEx.getInstance().leaveChannel();
        return ret == NERtcConstants.ErrorCode.OK;
    }
    private void refreshVoiceButton(int id, ArrayList<Button> Btns) {
        for(Button btn : Btns){
            if(btn.getId() == id) {
                btn.setBackgroundColor(getResources().getColor(R.color.base_blue));
                continue;
            }
            btn.setBackgroundColor(getResources().getColor(R.color.button_select_off));
        }
    }

}