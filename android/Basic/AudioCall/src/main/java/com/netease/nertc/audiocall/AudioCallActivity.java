package com.netease.nertc.audiocall;

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
import com.netease.lava.nertc.sdk.NERtcCallback;
import com.netease.lava.nertc.sdk.NERtcConstants;
import com.netease.lava.nertc.sdk.NERtcEx;
import com.netease.lava.nertc.sdk.NERtcOption;
import com.netease.lava.nertc.sdk.NERtcParameters;
import com.netease.lava.nertc.sdk.stats.NERtcAudioLayerRecvStats;
import com.netease.lava.nertc.sdk.stats.NERtcAudioRecvStats;
import com.netease.lava.nertc.sdk.stats.NERtcAudioSendStats;
import com.netease.lava.nertc.sdk.stats.NERtcNetworkQualityInfo;
import com.netease.lava.nertc.sdk.stats.NERtcStats;
import com.netease.lava.nertc.sdk.stats.NERtcStatsObserver;
import com.netease.lava.nertc.sdk.stats.NERtcVideoRecvStats;
import com.netease.lava.nertc.sdk.stats.NERtcVideoSendStats;
import com.netease.nertc.config.BuildConfig;
import com.netease.nertc.config.DemoDeploy;

import java.util.ArrayList;

public class AudioCallActivity extends AppCompatActivity implements NERtcCallback, View.OnClickListener {
    private static final String EXTRA_ROOM_ID = "extra_room_id";
    private static final String EXTRA_USER_ID = "extra_user_id";
    private static final String TAG = "AudioCallActivity";

    private String mRoomId;
    private long mUserId;
    private boolean mEnableLocalAudio = true;
    private boolean mJoinChannel = false;
    private boolean mIsSpeakerPhone = true;

    private RelativeLayout mContainer;
    private ArrayList<ImageView> mRemoteUserVv;
    private ArrayList<TextView> mRemoteUserId;
    private ArrayList<TextView> mRemoteVolume;
    private ArrayList<TextView> mRemoteNetwork;

    private Button mAudioRouteBtn;
    private Button mMuteMicBtn;
    private Button mHangUpBtn;
    private ImageView mBackIv;
    private TextView mRoomTittleTv;


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
            finish(); //其他Activity 都同样处理下
        }
    }

    /**
     * 离开房间
     *
     * @return
     */
    private boolean leaveChannel() {

        mJoinChannel = false;
        setLocalAudioEnable(false);
        int ret = NERtcEx.getInstance().leaveChannel();
        return ret == NERtcConstants.ErrorCode.OK;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_call);
        mRoomId = getIntent().getStringExtra(EXTRA_ROOM_ID);
        mUserId = getIntent().getLongExtra(EXTRA_USER_ID, -1);
        initView();
        setupNERtc();
        joinChannel(mUserId, mRoomId);
    }

    @Override
    protected void onDestroy() { //其他Activity 都同样处理下
        NERtcEx.getInstance().release();
        super.onDestroy();
    }

    private void initView() {
        mContainer = findViewById(R.id.rl_main_container);
        mRemoteUserVv = new ArrayList<>();
        mRemoteUserVv.add((ImageView) findViewById(R.id.user_img1));
        mRemoteUserVv.add((ImageView) findViewById(R.id.user_img2));
        mRemoteUserVv.add((ImageView) findViewById(R.id.user_img3));
        mRemoteUserVv.add((ImageView) findViewById(R.id.user_img4));
        mRemoteUserVv.add((ImageView) findViewById(R.id.user_img5));
        mRemoteUserVv.add((ImageView) findViewById(R.id.user_img6));

        mRemoteUserId = new ArrayList<>();
        mRemoteUserId.add((TextView) findViewById(R.id.user_id1));
        mRemoteUserId.add((TextView) findViewById(R.id.user_id2));
        mRemoteUserId.add((TextView) findViewById(R.id.user_id3));
        mRemoteUserId.add((TextView) findViewById(R.id.user_id4));
        mRemoteUserId.add((TextView) findViewById(R.id.user_id5));
        mRemoteUserId.add((TextView) findViewById(R.id.user_id6));

        mRemoteVolume = new ArrayList<>();
        mRemoteVolume.add((TextView) findViewById(R.id.user_voice1));
        mRemoteVolume.add((TextView) findViewById(R.id.user_voice2));
        mRemoteVolume.add((TextView) findViewById(R.id.user_voice3));
        mRemoteVolume.add((TextView) findViewById(R.id.user_voice4));
        mRemoteVolume.add((TextView) findViewById(R.id.user_voice5));
        mRemoteVolume.add((TextView) findViewById(R.id.user_voice6));

        mRemoteNetwork = new ArrayList<>();
        mRemoteNetwork.add((TextView) findViewById(R.id.user_net1));
        mRemoteNetwork.add((TextView) findViewById(R.id.user_net2));
        mRemoteNetwork.add((TextView) findViewById(R.id.user_net3));
        mRemoteNetwork.add((TextView) findViewById(R.id.user_net4));
        mRemoteNetwork.add((TextView) findViewById(R.id.user_net5));
        mRemoteNetwork.add((TextView) findViewById(R.id.user_net6));

        mAudioRouteBtn = findViewById(R.id.btn_audio_route);
        mMuteMicBtn = findViewById(R.id.btn_mute_audio);
        mHangUpBtn = findViewById(R.id.btn_hangup);
        mBackIv = findViewById(R.id.iv_back);
        mRoomTittleTv = findViewById(R.id.tv_room_id);

        mBackIv.setOnClickListener(this);
        mAudioRouteBtn.setOnClickListener(this);
        mMuteMicBtn.setOnClickListener(this);
        mHangUpBtn.setOnClickListener(this);

        if (!TextUtils.isEmpty(mRoomId)) {
            mRoomTittleTv.setText("房间号:" + mRoomId);
        }
    }

    public static void startActivity(Activity from, String roomId, long userId) {
        Intent intent = new Intent(from, AudioCallActivity.class);
        intent.putExtra(EXTRA_ROOM_ID, roomId);
        intent.putExtra(EXTRA_USER_ID, userId);
        from.startActivity(intent);
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
        //设置质量透明回调
        NERtcEx.getInstance().setStatsObserver(new NERtcStatsObserver() {
            @Override
            public void onRtcStats(NERtcStats neRtcStats) {

            }

            @Override
            public void onLocalAudioStats(NERtcAudioSendStats neRtcAudioSendStats) {

            }

            @Override
            public void onRemoteAudioStats(NERtcAudioRecvStats[] neRtcAudioRecvStats) {
                Log.d(TAG, "onRemoteAudioStats:" + neRtcAudioRecvStats.length);
                int index = 0;
                for (; index < neRtcAudioRecvStats.length; index++) {
                    if (index < mRemoteVolume.size()) {
                        NERtcAudioLayerRecvStats tmp = neRtcAudioRecvStats[index].layers.get(0);
                        mRemoteVolume.get(index).setText("uId：" + neRtcAudioRecvStats[index].uid + "  音量：" + tmp.volume);
                        mRemoteVolume.get(index).setVisibility(View.VISIBLE);
                    }
                }

                for (; index < mRemoteVolume.size(); index++) {
                    mRemoteVolume.get(index).setVisibility(View.GONE);
                }
            }

            @Override
            public void onLocalVideoStats(NERtcVideoSendStats neRtcVideoSendStats) {

            }

            @Override
            public void onRemoteVideoStats(NERtcVideoRecvStats[] neRtcVideoRecvStats) {

            }

            @Override
            public void onNetworkQuality(NERtcNetworkQualityInfo[] neRtcNetworkQualityInfos) {
                Log.d(TAG, "onNetworkQuality:" + neRtcNetworkQualityInfos.length);
                int index = 0;
                for (; index < neRtcNetworkQualityInfos.length; index++) {
                    if (index < mRemoteNetwork.size()) {
                        NERtcNetworkQualityInfo tmp = neRtcNetworkQualityInfos[index];
                        if (tmp.userId == mUserId)
                            continue;
                        mRemoteNetwork.get(index).setText("uId：" + tmp.userId + "  网络质量：" + NetQuality.getMsg(tmp.downStatus));
                        mRemoteNetwork.get(index).setVisibility(View.VISIBLE);
                    }
                }
                for (; index < mRemoteNetwork.size(); index++) {
                    mRemoteNetwork.get(index).setVisibility(View.GONE);
                }
            }
        });

        setLocalAudioEnable(true);
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
     * 加入房间
     *
     * @param userId 用户ID
     * @param roomId 房间ID
     */
    private void joinChannel(final long userId, String roomId) {
        Log.i(TAG, "joinChannel userId: " + userId);
        NERtcEx.getInstance().joinChannel(null, roomId, userId);
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
        for (int i = 0; i < mRemoteUserVv.size(); i++) {
            if (mRemoteUserVv.get(i).getTag() == null && mRemoteUserId.get(i).getTag() == null) {
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
    public void onUserLeave(long userId, int i) {
        Log.i(TAG, "onUserLeave uid: " + userId);
        ImageView userView = mContainer.findViewWithTag(userId);
        if (userView != null) {
            //设置TAG为null，代表当前没有订阅
            userView.setTag(null);
            userView.setImageResource(R.mipmap.common_user_portrait);
        }
        TextView userIdView = mContainer.findViewWithTag(userId);
        if (userIdView != null) {
            //设置TAG为null，代表当前没有订阅
            userIdView.setTag(null);
            userIdView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onUserAudioStart(long userId) {
        Log.i(TAG, "onUserAudioStart uid: " + userId);
    }

    @Override
    public void onUserAudioStop(long userId) {
        Log.i(TAG, "onUserAudioStop uid: " + userId);
    }

    @Override
    public void onUserVideoStart(long userId, int profile) {
        Log.i(TAG, "onUserVideoStart uid: " + userId + " profile: " + profile);
    }

    @Override
    public void onUserVideoStop(long userId) {
        Log.i(TAG, "onUserVideoStop, uid=" + userId);
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

    /**
     * 切换听筒和扬声器
     */
    private void changeAudioRoute() {
        mIsSpeakerPhone = !mIsSpeakerPhone;
        NERtcEx.getInstance().setSpeakerphoneOn(mIsSpeakerPhone);
        if (mIsSpeakerPhone) {
            mAudioRouteBtn.setText(getString(R.string.videocall_use_receiver));
        } else {
            mAudioRouteBtn.setText(getString(R.string.videocall_use_speaker));
        }
    }

    /**
     * 改变音频可用状态
     */
    private void changeAudioEnable() {
        mEnableLocalAudio = !mEnableLocalAudio;
        setLocalAudioEnable(mEnableLocalAudio);
        if (mEnableLocalAudio) {
            mMuteMicBtn.setText(getString(R.string.videocall_close_audio));
        } else {
            mMuteMicBtn.setText(getString(R.string.videocall_open_audio));
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.iv_back) {
            exit();
        } else if (id == R.id.btn_audio_route) {
            changeAudioRoute();
        } else if (id == R.id.btn_mute_audio) {
            changeAudioEnable();
        } else if (id == R.id.btn_hangup) {
            hangup();
        }
    }

    /**
     * 挂断
     */
    private void hangup() {
        exit();
    }

    /**
     * 网络状态枚举
     */
    public enum NetQuality {
        UNKNOWN(0, "未知"),
        EXCELLENT(1, "非常好"),
        GOOD(2, "好"),
        POOR(3, "不太好"),
        BAD(4, "差"),
        VERYBAD(5, "非常差"),
        DOWN(6, "无网络");


        private int num;
        private String msg;


        NetQuality(int num, String msg) {
            this.num = num;
            this.msg = msg;
        }

        public static String getMsg(int code) {
            for (NetQuality item : NetQuality.values()) {
                if (item.num == code) {
                    return item.msg;
                }
            }
            return "未定义";
        }
    }
}