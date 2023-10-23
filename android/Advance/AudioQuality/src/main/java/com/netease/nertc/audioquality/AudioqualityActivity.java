package com.netease.nertc.audioquality;

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

import com.netease.lava.nertc.sdk.NERtc;
import com.netease.lava.nertc.sdk.NERtcCallback;
import com.netease.lava.nertc.sdk.NERtcConstants;
import com.netease.lava.nertc.sdk.NERtcEx;
import com.netease.lava.nertc.sdk.NERtcOption;
import com.netease.lava.nertc.sdk.NERtcParameters;
import com.netease.lava.nertc.sdk.NERtcUserJoinExtraInfo;
import com.netease.lava.nertc.sdk.NERtcUserLeaveExtraInfo;
import com.netease.nertc.config.DemoDeploy;

import java.util.ArrayList;
import java.util.Random;

public class AudioqualityActivity extends AppCompatActivity implements NERtcCallback, View.OnClickListener{
    private static final String TAG = "AudioqualityActivity";
    private Button mStartJoinBtn;
    private boolean mJoinChannel = false;
    private boolean mPushStreamState = false;
    private boolean mEnableLocalAudio = true;
    private EditText mRoomIdEt;
    private EditText mUserIdEt;
    private long mUserId;
    private String mRoomId;
    private RelativeLayout mContainer;
    private ArrayList<ImageView> mRemoteUserVv;
    private ArrayList<TextView> mRemoteUserId;
    private int mAudioProfile = NERtcConstants.AudioProfile.DEFAULT;
    private int mAudioScenario = NERtcConstants.AudioScenario.DEFAULT;
    private ArrayList<Button> mProfileButton;
    private Button mSceDefaultBtn;
    private Button mSceSpeechBtn;
    private Button mSceMusicBtn;
    private ImageView mBackIv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audioquality);
        initView();
    }

    private void initView() {
        mBackIv = findViewById(R.id.iv_back);
        mStartJoinBtn = findViewById(R.id.btn_join_channel);
        mRoomIdEt = findViewById(R.id.et_room_id);
        mUserIdEt = findViewById(R.id.et_user_id);
        mContainer = findViewById(R.id.main_Container);

        mUserId = new Random().nextInt(100000);
        mUserIdEt.setText(String.valueOf(mUserId));

        mRemoteUserVv = new ArrayList<>();
        mRemoteUserVv.add((ImageView) findViewById(R.id.iv_user_img1));
        mRemoteUserVv.add((ImageView) findViewById(R.id.iv_user_img2));
        mRemoteUserVv.add((ImageView) findViewById(R.id.iv_user_img3));
        mRemoteUserVv.add((ImageView) findViewById(R.id.iv_user_img4));
        mRemoteUserVv.add((ImageView) findViewById(R.id.iv_user_img5));
        mRemoteUserVv.add((ImageView) findViewById(R.id.iv_user_img6));

        mRemoteUserId = new ArrayList<>();
        mRemoteUserId.add((TextView) findViewById(R.id.tv_user_id1));
        mRemoteUserId.add((TextView) findViewById(R.id.tv_user_id2));
        mRemoteUserId.add((TextView) findViewById(R.id.tv_user_id3));
        mRemoteUserId.add((TextView) findViewById(R.id.tv_user_id4));
        mRemoteUserId.add((TextView) findViewById(R.id.tv_user_id5));
        mRemoteUserId.add((TextView) findViewById(R.id.tv_user_id6));

        mProfileButton = new ArrayList<>();
        mProfileButton.add(findViewById(R.id.btn_quality_default));
        mProfileButton.add(findViewById(R.id.btn_quality_normal));
        mProfileButton.add(findViewById(R.id.btn_quality_medium));
        mProfileButton.add(findViewById(R.id.btn_quality_high));
        mProfileButton.add(findViewById(R.id.btn_Medium_stereo));
        mProfileButton.add(findViewById(R.id.btn_high_stereo));

        for(Button btn : mProfileButton){
            btn.setOnClickListener(this);
        }
        mSceDefaultBtn = findViewById(R.id.btn_Scenario_default);
        mSceSpeechBtn = findViewById(R.id.btn_Scenario_speech);
        mSceMusicBtn = findViewById(R.id.btn_Scenario_music);

        mSceDefaultBtn.setOnClickListener(this);
        mSceSpeechBtn.setOnClickListener(this);
        mSceMusicBtn.setOnClickListener(this);
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

    private boolean leaveChannel(){
        mJoinChannel = false;
        mPushStreamState = false;
        setLocalAudioEnable(false);
        int ret = NERtcEx.getInstance().leaveChannel();
        return ret == NERtcConstants.ErrorCode.OK;
    }
    /**
     * 退出房间并关闭页面
     */
    private void exit(){
        Log.d(TAG, "exit");
        if(mJoinChannel){
            leaveChannel();
        }
        finish();
    }
    private void userInfo() {
        Editable roomIdEdit = mRoomIdEt.getText();
        if(roomIdEdit == null || roomIdEdit.length() <= 0){
            return;
        }
        Editable userIdEdit = mUserIdEt.getText();
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
    }
    private void joinChannel(String roomId, long userId) {
        Log.d(TAG,"joinChannel");
        NERtcEx.getInstance().joinChannel("", roomId, userId);
    }
    private void startPushStream() {
        if(!mJoinChannel) {
            userInfo();
            setupNERtc();
            joinChannel(mRoomId, mUserId);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        exit();
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if(id == R.id.iv_back){
            exit();
        }else if(id == R.id.btn_join_channel){
            mPushStreamState = true;
            startPushStream();
        }else if(id == R.id.btn_quality_default){
            if(mPushStreamState){
                mAudioProfile = NERtcConstants.AudioProfile.DEFAULT;
            }
            onAudioConfigChange();
            refreshButton(id);
        }else if(id == R.id.btn_quality_normal){
            if(mPushStreamState){
                mAudioProfile = NERtcConstants.AudioProfile.STANDARD;
            }
            onAudioConfigChange();
            refreshButton(id);
        }else if(id == R.id.btn_quality_medium){
            if(mPushStreamState){
                mAudioProfile = NERtcConstants.AudioProfile.MIDDLE_QUALITY;
            }
            onAudioConfigChange();
            refreshButton(id);
        }else if(id == R.id.btn_quality_high){
            if(mPushStreamState){
                mAudioProfile = NERtcConstants.AudioProfile.HIGH_QUALITY;
            }
            onAudioConfigChange();
            refreshButton(id);
        }else if(id == R.id.btn_Medium_stereo){
            if(mPushStreamState){
                mAudioProfile = NERtcConstants.AudioProfile.MIDDLE_QUALITY_STEREO;
            }
            onAudioConfigChange();
            refreshButton(id);
        }else if(id == R.id.btn_high_stereo){
            if(mPushStreamState){
                mAudioProfile = NERtcConstants.AudioProfile.HIGH_QUALITY_STEREO;
            }
            onAudioConfigChange();
            refreshButton(id);
        }else if(id == R.id.btn_Scenario_default){
            if(mPushStreamState){
                mAudioScenario = NERtcConstants.AudioScenario.DEFAULT;
            }
            onAudioConfigChange();
            mSceDefaultBtn.setBackgroundColor(getResources().getColor(R.color.base_blue));
            mSceMusicBtn.setBackgroundColor(getResources().getColor(R.color.button_select_off));
            mSceSpeechBtn.setBackgroundColor(getResources().getColor(R.color.button_select_off));
        }else if(id == R.id.btn_Scenario_speech){
            if(mPushStreamState){
                mAudioScenario = NERtcConstants.AudioScenario.SPEECH;
            }
            onAudioConfigChange();
            mSceDefaultBtn.setBackgroundColor(getResources().getColor(R.color.button_select_off));
            mSceMusicBtn.setBackgroundColor(getResources().getColor(R.color.button_select_off));
            mSceSpeechBtn.setBackgroundColor(getResources().getColor(R.color.base_blue));
        }else if(id == R.id.btn_Scenario_music){
            if(mPushStreamState){
                mAudioScenario = NERtcConstants.AudioScenario.MUSIC;
            }
            onAudioConfigChange();
            mSceDefaultBtn.setBackgroundColor(getResources().getColor(R.color.button_select_off));
            mSceMusicBtn.setBackgroundColor(getResources().getColor(R.color.base_blue));
            mSceSpeechBtn.setBackgroundColor(getResources().getColor(R.color.button_select_off));
        }
    }

    private void refreshButton(int id) {
        for(Button btn : mProfileButton){
            if(btn.getId() == id) {
                btn.setBackgroundColor(getResources().getColor(R.color.base_blue));
                continue;
            }
            btn.setBackgroundColor(getResources().getColor(R.color.button_select_off));
        }
    }

    private void onAudioConfigChange() {
        // 关闭本地视频采集以及发送
        NERtc.getInstance().enableLocalAudio(false);
        NERtc.getInstance().setAudioProfile(mAudioProfile, mAudioScenario);
        // 开启本地视频采集以及发送
        NERtc.getInstance().enableLocalAudio(true);
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
        NERtcEx.getInstance().release();
        finish();
    }

    @Override
    public void onUserJoined(long userId) {
        Log.i(TAG, "onUserJoined userId: " + userId);
        for(int i = 0;i < mRemoteUserVv.size();i++){
            if(mRemoteUserVv.get(i).getTag() == null && mRemoteUserId.get(i).getTag() == null){
                mRemoteUserVv.get(i).setImageResource(R.mipmap.yunxin);
                mRemoteUserVv.get(i).setTag(userId);
                mRemoteUserId.get(i).setText("uId:" + userId);
                mRemoteUserId.get(i).setTag(userId);
                mRemoteUserId.get(i).setVisibility(View.VISIBLE);
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
        ImageView userView = mContainer.findViewWithTag(userId);
        if(userView != null){
            //设置TAG为null，代表当前没有订阅
            userView.setTag(null);
            userView.setImageResource(R.mipmap.common_user_portrait);
        }
        TextView userIdView = mContainer.findViewWithTag(userId);
        if(userIdView != null){
            //设置TAG为null，代表当前没有订阅
            userIdView.setTag(null);
            userIdView.setVisibility(View.GONE);
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