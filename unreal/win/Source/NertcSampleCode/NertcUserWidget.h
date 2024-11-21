// Fill out your copyright notice in the Description page of Project Settings.

#pragma once

#include "CoreMinimal.h"
#include "Blueprint/UserWidget.h"
#include "INertcEngineEx.h"
#include "INertcEngineEventHandler.h"
#include "INertcAudioDeviceManager.h"
#include <string>
#include "NertcUserWidget.generated.h"

/**
 * 
 */
using namespace nertc;
UCLASS()
class NERTCSAMPLECODE_API UNertcUserWidget : public UUserWidget, public IRtcEngineEventHandler, public IRtcMediaStatsObserver
{
	GENERATED_BODY()
public:
  void NativeConstruct() override;
  void NativeDestruct() override;

  void InitNertcEngine(std::string appKey, std::string logPath);
  void ReleaseNertcEngine();

  UFUNCTION(BlueprintCallable)
  void OnJoinChannelClicked();
    
  UFUNCTION(BlueprintCallable)
  void OnLeaveChannelClicked();

public:
  //��дonJoinChannel�麯��������joinChannel��SDK���Զ����øú�����֪ͨ���
  void onJoinChannel(channel_id_t cid, nertc::uid_t uid, NERtcErrorCode result, uint64_t elapsed) override;
  //��дonConnectionStateChange�麯��������״̬�������ʱ��SDK���Զ����øú���
  void onConnectionStateChange(NERtcConnectionStateType state, NERtcReasonConnectionChangedType reason)override;
  //��дonLeaveChannel�麯�����뿪�����SDK���Զ����øú���
  void onLeaveChannel(NERtcErrorCode result, uint64_t channel_id) override;
  //����setStatsObserver֮��SDK�ᶨ�ڻص�onRtcStats��������SDK�ڲ����ݣ�����CPUʹ���ʵȣ�֪ͨ����
  void onRtcStats(const NERtcStats& stats) override;

private:
  //��������SDK��������ָ��
  nertc::IRtcEngineEx* nertc_engine_;
  //����������Ƶ�豸��������ָ��
  nertc::IAudioDeviceManager* audio_device_manager_;
};
