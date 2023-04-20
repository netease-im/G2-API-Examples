package com.netease.nertc.externalaudioshare;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
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
import com.netease.lava.nertc.sdk.video.NERtcRemoteVideoStreamType;
import com.netease.lava.nertc.sdk.video.NERtcVideoView;
import com.netease.nertc.config.DemoDeploy;
import com.netease.nertc.externalaudioshare.externalvideo.ExternalAudioSource;
import com.netease.nertc.externalaudioshare.externalvideo.ExternalPCMAudioSource;


import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;

public class ExternalAudioCaptureActivity extends AppCompatActivity implements NERtcCallback, View.OnClickListener {
    private static final String TAG = "ExternalAudioActivity";
    private static final String PCM_DIR = "pcm";
    private static final String PCM1 = "test_48000_1.pcm";
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
    private boolean mStartPlay = false;
    private String mRoomId;
    private long mUserId;
    private String mExternalAudioPath;
    private int mExternalAudioSampleRate = 48000;
    private int mExternalAudioChannel = 1;
    private EditText mVideoPathView;
    private Button mStartPlayBtn;
    private ExternalAudioSource mExternalAudioSource;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_external_audio_capture);
        initPCM();
        initView();
    }

    @Override
    protected void onDestroy() {
        NERtcEx.getInstance().release();
        super.onDestroy();
    }

    private void initPCM() {
        new Thread(() -> {
            String root = ensureMusicDirectory();
            mExternalAudioPath = extractPCMFile(root, PCM1);
            runOnUiThread(() -> {
                mVideoPathView.setText(mExternalAudioPath);
            });
        }).start();
    }

    private String extractPCMFile(String path, String name) {
        copyAssetToFile(this, PCM_DIR + "/" + name, path, name);
        return new File(path, name).getAbsolutePath();
    }

    private void copyAssetToFile(Context context, String assetName, String savePath, String saveName) {
        File dir = new File(savePath);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File destFile = new File(dir, saveName);
        InputStream inputstream = null;
        OutputStream outputstream = null;
        try {
            inputstream = context.getResources().getAssets().open(assetName);
            if (destFile.exists() && inputstream.available() == destFile.length()) {
                return;
            }
            destFile.deleteOnExit();
            outputstream = new FileOutputStream(destFile);
            byte[] buffer = new byte[4096];
            int count;
            while ((count = inputstream.read(buffer)) != -1) {
                outputstream.write(buffer, 0, count);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeQuiet(inputstream);
            closeQuiet(outputstream);
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
        File dir = getExternalFilesDir(PCM_DIR);
        if (dir == null) {
            dir = getDir(PCM_DIR, 0);
        }
        if (dir != null) {
            dir.mkdirs();
            return dir.getAbsolutePath();
        }
        return "";
    }

    private void initView() {
        mContainer = findViewById(R.id.container);
        mBackIv = findViewById(R.id.iv_back);
        mVideoPathView = findViewById(R.id.et_filepath);
        mStartJoinBtn = findViewById(R.id.btn_join_channel);
        mRoomIdEt = findViewById(R.id.et_room_id);
        mUserIdEt = findViewById(R.id.et_user_id);
        mStartPlayBtn = findViewById(R.id.btn_start_play);
        mLocalUserVv = findViewById(R.id.vv_local_user);

        mRemoteUserVv = findViewById(R.id.vv_remote_user_1);
        mUserId = new Random().nextInt(100000);
        mUserIdEt.setText(String.valueOf(mUserId));

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
        } else if (id == R.id.btn_start_play) {
            if(mJoinChannel)
                toggleExternalAudio();
        }
    }

    private void toggleExternalAudio() {
        mStartPlay = !mStartPlay;
        if (mStartPlay) {
            NERtcEx.getInstance().setExternalAudioSource(true, mExternalAudioSampleRate, mExternalAudioChannel);
            if (mExternalAudioSource == null) {
                mExternalAudioSource = new ExternalPCMAudioSource(mExternalAudioPath, mExternalAudioSampleRate, mExternalAudioChannel);
            }
            mExternalAudioSource.start();
            mStartPlayBtn.setText(R.string.stop_external_audio);
        } else {
            if(mExternalAudioSource != null){
                mExternalAudioSource.stop();
                mExternalAudioSource = null;
            }
            NERtcEx.getInstance().setExternalAudioSource(false, mExternalAudioSampleRate, mExternalAudioChannel);
            mStartPlayBtn.setText(R.string.startplay);
        }
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
    public void onDisconnect(int i) {
        finish();
    }

    @Override
    public void onClientRoleChange(int i, int i1) {

    }
}