package com.netease.nertc.externalvideoshare;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import com.netease.lava.nertc.sdk.NERtcUserJoinExtraInfo;
import com.netease.lava.nertc.sdk.NERtcUserLeaveExtraInfo;
import com.netease.lava.nertc.sdk.video.NERtcRemoteVideoStreamType;
import com.netease.lava.nertc.sdk.video.NERtcVideoFrame;
import com.netease.lava.nertc.sdk.video.NERtcVideoView;
import com.netease.nertc.config.DemoDeploy;
import com.netease.nertc.externalvideoshare.externalvideo.ExternalTextureVideoSource;
import com.netease.nertc.externalvideoshare.externalvideo.ExternalVideoSource;

import java.util.Random;

public class ExternalVideoShareActivity extends AppCompatActivity implements NERtcCallback, View.OnClickListener {
    private static final String TAG = "ExternalVideoActivity";

    private static final int REQUEST_CODE_CHOOSE_VIDEO_FILE = 10000;
    private static final int REQUEST_CODE_REQUEST_PERMISSION = 10001;

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
    private Button mChooseFileBtn;
    private String videoPath;
    private EditText mVideoPathView;
    private Button mStartPlayBtn;
    private boolean started = false;
    private ExternalVideoSource mExternalVideoSource;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_external_video_share);
        initView();
    }

    private void initView() {
        mContainer = findViewById(R.id.rl_container);
        mBackIv = findViewById(R.id.iv_back);
        mVideoPathView = findViewById(R.id.et_file_path);
        mStartJoinBtn = findViewById(R.id.btn_join_channel);
        mChooseFileBtn = findViewById(R.id.btn_choose_file);
        mRoomIdEt = findViewById(R.id.et_room_id);
        mUserIdEt = findViewById(R.id.et_user_id);
        mStartPlayBtn = findViewById(R.id.btn_start_play);
        mLocalUserVv = findViewById(R.id.vv_local_user);

        mRemoteUserVv = findViewById(R.id.vv_remote_user_1);
        mUserId = new Random().nextInt(100000);
        mUserIdEt.setText(String.valueOf(mUserId));

        mChooseFileBtn.setOnClickListener(this);
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
        }
        finish();
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

    private void setupLocalVideo() {
        // 如果小画布置于大画布上面，小画布需要 setZOrderMediaOverlay(true)
        // mLocalUserVv.setZOrderMediaOverlay(true);
        mLocalUserVv.setScalingType(IVideoRender.ScalingType.SCALE_ASPECT_FIT);
        NERtcEx.getInstance().setupLocalVideoCanvas(mLocalUserVv);
    }

    private void setupRemoteVideo(long userId) {
        // 如果小画布置于大画布上面，小画布需要 setZOrderMediaOverlay(true)
        // mRemoteUserVv.setZOrderMediaOverlay(true);
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
            setupLocalVideo();
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
        } else if (id == R.id.btn_choose_file) {
            chooseVideoFile();
        } else if (id == R.id.btn_start_play) {
            toggleExternalVideo();
        }
    }

    private void toggleExternalVideo() {
        if (!mJoinChannel) {
            Toast.makeText(this, R.string.please_join_Channel, Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(videoPath)) {
            chooseVideoFile();
            return;
        }
        if (!started) {
            // 创建外部视频源，输入文件路径和视频帧处理回调
            mExternalVideoSource = ExternalTextureVideoSource.create(videoPath, this::pushExternalVideoFrame);
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

    private void pushExternalVideoFrame(NERtcVideoFrame videoFrame) {
        // 推送外部视频帧
        NERtcEx.getInstance().pushExternalVideoFrame(videoFrame);
    }

    private void chooseVideoFile() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            openFileChoose(REQUEST_CODE_CHOOSE_VIDEO_FILE);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE_REQUEST_PERMISSION);
        }
    }

    private void openFileChoose(int requestCode) {
        try {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            startActivityForResult(intent, requestCode);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE_REQUEST_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                chooseVideoFile();
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_CHOOSE_VIDEO_FILE) {
            if (resultCode == RESULT_OK) {
                videoPath = FileUtil.getPath(getApplicationContext(), data.getData());
                mVideoPathView.setText(videoPath);
            }
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
        NERtc.getInstance().release();
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
    public void onUserJoined(long uid, NERtcUserJoinExtraInfo joinExtraInfo) {

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
        if (userView != null) {
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