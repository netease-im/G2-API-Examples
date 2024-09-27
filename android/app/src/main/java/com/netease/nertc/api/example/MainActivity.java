package com.netease.nertc.api.example;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.netease.nertc.audiocall.AudioCallEntryActivity;
import com.netease.nertc.audioeffect.AudioEffectActivity;
import com.netease.nertc.audiomix.AudioMixActivity;
import com.netease.nertc.audiomainsubstream.AudioMainSubActivity;
import com.netease.nertc.audioquality.AudioqualityActivity;
import com.netease.nertc.audiorecord.AudioRecordActivity;
import com.netease.nertc.beauty.BeautyActivity;
import com.netease.nertc.config.DemoDeploy;
import com.netease.nertc.devicemanagement.DeviceManageActivity;
import com.netease.nertc.externalaudioshare.ExternalAudioCaptureActivity;
import com.netease.nertc.externalvideocapture.ExternalVideoCaptureActivity;
import com.netease.nertc.externalvideorender.ExternalVideoRenderActivity;
import com.netease.nertc.externalvideoshare.ExternalVideoShareActivity;
import com.netease.nertc.fastswitchrooms.FastSwitchRoomsActivity;
import com.netease.nertc.mediaencryption.MediaEncryption;
import com.netease.nertc.networktest.NetworkTestActivity;
import com.netease.nertc.rawaudiocallback.RawAudioCallbackActivity;
import com.netease.nertc.rawvideocallback.RawVideoCallbackActivity;
import com.netease.nertc.screenshare.ScreenShareActivity;
import com.netease.nertc.sendseimsg.SendSEIMsgActivity;
import com.netease.nertc.snapshotwatermark.SnapshotWatermarkActivity;
import com.netease.nertc.soundeffectsetting.SoundEffectSeetingActivity;
import com.netease.nertc.superresolution.SuperResolutionActivity;
import com.netease.nertc.videocall.VideoCallEntryActivity;
import com.netease.nertc.videoconfiguration.VideoConfigurationActivity;
import com.netease.nertc.videomainsubstream.VideoMainSubStreamActivity;
import com.netease.nertc.videoquality.VideoqualityActivity;
import com.netease.nertc.videostream.VideoStreamActivity;
import com.netease.nertc.virtualbackground.VirtualBackgroundActivity;

/**
 * NERtc API-Example 主页面
 */

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        DemoDeploy.requestPermissionsIfNeeded(this);//应用获取权限
        findViewById(R.id.ll_video_call).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, VideoCallEntryActivity.class);
                startActivity(intent);
            }
        });
        findViewById(R.id.ll_audio_call).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, AudioCallEntryActivity.class);
                startActivity(intent);
            }
        });
        findViewById(R.id.ll_video_share).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ExternalVideoShareActivity.class);
                startActivity(intent);
            }
        });
        findViewById(R.id.ll_media_encryption).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, MediaEncryption.class);
                startActivity(intent);
            }
        });
        findViewById(R.id.ll_video_capture).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ExternalVideoCaptureActivity.class);
                startActivity(intent);
            }
        });
        findViewById(R.id.ll_video_external_render).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ExternalVideoRenderActivity.class);
                startActivity(intent);
            }
        });
        findViewById(R.id.ll_rawVideo_callback).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, RawVideoCallbackActivity.class);
                startActivity(intent);
            }
        });
        findViewById(R.id.ll_room_switch).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, FastSwitchRoomsActivity.class);
                startActivity(intent);
            }
        });

        findViewById(R.id.ll_screen_share).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ScreenShareActivity.class);
                startActivity(intent);
            }
        });

        findViewById(R.id.ll_video_stream).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, VideoStreamActivity.class);
                startActivity(intent);
            }
        });

        findViewById(R.id.ll_audio_mix).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, AudioMixActivity.class);
                startActivity(intent);
            }
        });
        findViewById(R.id.ll_video_set).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, VideoqualityActivity.class);
                startActivity(intent);
            }
        });
        findViewById(R.id.ll_video_config).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, VideoConfigurationActivity.class);
                startActivity(intent);
            }
        });
        findViewById(R.id.ll_audio_set).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, AudioqualityActivity.class);
                startActivity(intent);
            }
        });
        findViewById(R.id.ll_audio_effect).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, AudioEffectActivity.class);
                startActivity(intent);
            }
        });
        findViewById(R.id.ll_audio_record).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, AudioRecordActivity.class);
                startActivity(intent);
            }
        });
        findViewById(R.id.ll_rawAudio_callback).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, RawAudioCallbackActivity.class);
                startActivity(intent);
            }
        });
        findViewById(R.id.ll_audio_capture_render).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, ExternalAudioCaptureActivity.class);
                startActivity(intent);
            }
        });
        findViewById(R.id.ll_sound_effect_seeting).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SoundEffectSeetingActivity.class);
                startActivity(intent);
            }
        });
        findViewById(R.id.ll_network_test).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, NetworkTestActivity.class);
                startActivity(intent);
            }
        });
        findViewById(R.id.ll_SEI_send).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SendSEIMsgActivity.class);
                startActivity(intent);
            }
        });
        findViewById(R.id.ll_beauty).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, BeautyActivity.class);
                startActivity(intent);
            }
        });
        findViewById(R.id.ll_vitual_background).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, VirtualBackgroundActivity.class);
                startActivity(intent);
            }
        });
        findViewById(R.id.ll_super_resolution).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SuperResolutionActivity.class);
                startActivity(intent);
            }
        });
        findViewById(R.id.ll_snapshot_watermark).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SnapshotWatermarkActivity.class);
                startActivity(intent);
            }
        });
        findViewById(R.id.ll_device_management).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, DeviceManageActivity.class);
                startActivity(intent);
            }
        });
        findViewById(R.id.ll_audio_pri_sec).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, AudioMainSubActivity.class);
                startActivity(intent);
            }
        });
        findViewById(R.id.main_video_pri_sec).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, VideoMainSubStreamActivity.class);
                startActivity(intent);
            }
        });
    }
}