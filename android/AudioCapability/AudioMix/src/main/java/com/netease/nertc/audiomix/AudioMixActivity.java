package com.netease.nertc.audiomix;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
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
import com.netease.lava.nertc.sdk.audio.NERtcCreateAudioMixingOption;
import com.netease.lava.nertc.sdk.video.NERtcRemoteVideoStreamType;
import com.netease.lava.nertc.sdk.video.NERtcVideoView;
import com.netease.nertc.config.DemoDeploy;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Random;


public class AudioMixActivity extends AppCompatActivity implements NERtcCallback,View.OnClickListener {
    private static final String TAG = "AudioMixActivity";

    private static final String MUSIC_DIR = "music";
    private static final String MUSIC1 = "music1.m4a";
    private static final String MUSIC2 = "music2.m4a";
    private static boolean mMusicState = false;
    private boolean mEnableLocalAudio = true;
    private boolean mEnableLocalVideo = true;
    private boolean mJoinChannel = false;

    private String[] musicPathArray;
    private int mAudioMixingVolume = 100; //音量
    private int mMusicIndex = 0;//默认伴音数组下标
    private long mUserId;
    private String mRoomId;

    private ImageView mBackIv;
    private Button mBGM1Btn;
    private Button mBGM2Btn;
    private Button mStartJoinBtn;
    private EditText mRoomIdEt;
    private EditText mUserIdEt;
    private SeekBar mVolumeSeekBar;
    private NERtcVideoView mLocalUserVv;
    private TextView mTextVolume;
    private ArrayList<NERtcVideoView> mRemoteVideoList;
    private RelativeLayout mContainer;

    private Handler mHandler;
    private Button mSwitchStateBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_mix);
        initView();
    }

    @Override
    protected void onDestroy() {
        NERtcEx.getInstance().release();
        super.onDestroy();
    }

    private void initView() {
        mRemoteVideoList = new ArrayList<>();
        mContainer = findViewById(R.id.rl_container);
        mBackIv = findViewById(R.id.iv_back);
        mBGM1Btn = findViewById(R.id.btn_bgm_1);
        mBGM2Btn = findViewById(R.id.btn_bgm_2);
        mSwitchStateBtn = findViewById(R.id.btn_switch_state);
        mStartJoinBtn = findViewById(R.id.btn_join_channel);
        mRoomIdEt = findViewById(R.id.et_room_id);
        mUserIdEt = findViewById(R.id.et_user_id);
        mVolumeSeekBar = findViewById(R.id.sb_voice_volume);

        mLocalUserVv = findViewById(R.id.vv_local_user);
        mTextVolume = findViewById(R.id.tv_volume);
        mTextVolume.setText(mVolumeSeekBar.getProgress() + "");

        mRemoteVideoList.add((NERtcVideoView) findViewById(R.id.vv_remote_user_1));
        mRemoteVideoList.add((NERtcVideoView) findViewById(R.id.vv_remote_user_2));
        mRemoteVideoList.add((NERtcVideoView) findViewById(R.id.vv_remote_user_3));
        mRemoteVideoList.add((NERtcVideoView) findViewById(R.id.vv_remote_user_4));
        mUserId = new Random().nextInt(100000);
        mUserIdEt.setText(String.valueOf(mUserId));

        mBGM1Btn.setOnClickListener(this);
        mBGM2Btn.setOnClickListener(this);
        mStartJoinBtn.setOnClickListener(this);
        mBackIv.setOnClickListener(this);
        mSwitchStateBtn.setOnClickListener(this);

        mVolumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Log.d("onStopTrackingTouch", "onStopTrackingTouch : progrsss = " + seekBar.getProgress());
                mTextVolume.setText(seekBar.getProgress() + "");
                if(mJoinChannel) {
                    onMusicVolumeChange(seekBar.getProgress());
                }
            }
        });
    }
    /**
     * 初始化伴音
     */
    protected void initOptions(){
        mHandler = new Handler(getMainLooper());
        initMusic();
    }
    /**
     * 伴音模式切换
     *
     * @return
     */
    private boolean startMusicmix() {
        int result;
        NERtcCreateAudioMixingOption option = new NERtcCreateAudioMixingOption();
        option.path = musicPathArray[mMusicIndex];
        option.playbackVolume = mAudioMixingVolume;
        option.sendVolume = mAudioMixingVolume;
        option.loopCount = 1;
        result = NERtcEx.getInstance().startAudioMixing(option);
        return result == 0;
    }
    /**
     * 初始化伴音
     */
    private void initMusic(){
        new Thread(() -> {
            String root = ensureMusicDirectory();
            musicPathArray = new String[]{extractMusicFile(root, MUSIC1),extractMusicFile(root, MUSIC2)};
            mHandler.post(() -> {
                startMusicmix();
            });
        }).start();
    }

    private String extractMusicFile(String path, String name) {
        copyAssetToFile(this, MUSIC_DIR + "/" + name, path, name);
        return new File(path, name).getAbsolutePath();
    }

    private void copyAssetToFile(Context context, String assetsName,
                                 String savePath, String saveName) {

        File dir = new File(savePath);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File destFile = new File(dir, saveName);
        InputStream inputStream = null;
        FileOutputStream outputStream = null;
        try {
            inputStream = context.getResources().getAssets().open(assetsName);
            if (destFile.exists() && inputStream.available() == destFile.length()) {
                return;
            }
            destFile.deleteOnExit();
            outputStream = new FileOutputStream(destFile);
            byte[] buffer = new byte[4096];
            int count;
            while ((count = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, count);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeQuiet(inputStream);
            closeQuiet(outputStream);
        }
    }

    private void closeQuiet(Closeable closeable) {
        if (closeable == null) {
            return;
        }

        try {
            closeable.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private String ensureMusicDirectory() {
        File dir = getExternalFilesDir(MUSIC_DIR);
        if (dir == null) {
            dir = getDir(MUSIC_DIR, 0);
        }
        if (dir != null) {
            dir.mkdirs();
            return dir.getAbsolutePath();
        }
        return "";
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

    private void onMusicVolumeChange(int progress) {
        mAudioMixingVolume = progress;
        NERtcEx.getInstance().setAudioMixingSendVolume(mAudioMixingVolume);
        NERtcEx.getInstance().setAudioMixingPlaybackVolume(mAudioMixingVolume);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if(id == R.id.iv_back){
            exit();
        }else if(id == R.id.btn_bgm_1){
            mMusicIndex = 0;
            if(mJoinChannel) {
                NERtcEx.getInstance().stopAudioMixing();
                startMusicmix();
            }
            mBGM1Btn.setBackgroundColor(getResources().getColor(R.color.base_blue));
            mBGM2Btn.setBackgroundColor(getResources().getColor(R.color.button_select_off));
        }else if(id == R.id.btn_bgm_2){
            mMusicIndex = 1;
            if(mJoinChannel) {
                NERtcEx.getInstance().stopAudioMixing();
                startMusicmix();
            }
            mBGM2Btn.setBackgroundColor(getResources().getColor(R.color.base_blue));
            mBGM1Btn.setBackgroundColor(getResources().getColor(R.color.button_select_off));
        }else if(id == R.id.btn_join_channel){
            startJoinHome();
            mSwitchStateBtn.setVisibility(View.VISIBLE);
        }else if(id == R.id.btn_switch_state){
            mMusicState = !mMusicState;
            if(mMusicState){
                mSwitchStateBtn.setText(R.string.bgm_resume);
                NERtcEx.getInstance().pauseAudioMixing();
            }else{
                mSwitchStateBtn.setText(R.string.bgm_pause);
                NERtcEx.getInstance().resumeAudioMixing();
            }
        }
    }
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        exit();
    }

    private void startJoinHome() {
        if(!mJoinChannel) {
            userInfo();
            setupNERtc();
            setuplocalVideo();
            joinChannel(mRoomId, mUserId);
        }
    }

    private void joinChannel(String roomId, long userId) {
        NERtcEx.getInstance().joinChannel("", roomId, userId);
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
    public void onJoinChannel(int result, long channelId, long elapsed, long l2) {
        Log.i(TAG, "onJoinChannel result: " + result + " channelId: " + channelId + " elapsed: " + elapsed);
        if(result == NERtcConstants.ErrorCode.OK){
            initOptions();
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
    public void onUserAudioStart(long userId) {
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
        setLocalAudioEnable(false);
        setLocalVideoEnable(false);
        NERtcEx.getInstance().stopAudioMixing();
        int ret = NERtcEx.getInstance().leaveChannel();
        return ret == NERtcConstants.ErrorCode.OK;
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
}
