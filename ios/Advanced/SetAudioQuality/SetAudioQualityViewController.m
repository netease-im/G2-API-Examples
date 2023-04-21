//
//  SetAudioQualityViewController.m
//  NERTC-API-Example-OC
//
//  Created by test on 2022/10/9.
//

#import "NSString+Common.h"
#import "NTESAppConfig.h"
#import "SetAudioQualityViewController.h"
#import "UIColor+Hex.h"

@interface SetAudioQualityViewController ()<NERtcEngineDelegateEx>

@property (weak, nonatomic) IBOutlet UILabel *volumeLabel;

@property (weak, nonatomic) IBOutlet UIButton *audioSpeechButton;
@property (weak, nonatomic) IBOutlet UIButton *audioDefaultButton;
@property (weak, nonatomic) IBOutlet UIButton *audioMusicButton;
@property (weak, nonatomic) IBOutlet UIButton *audioChatButton;

@property (weak, nonatomic) IBOutlet UISlider *volumeSlider;
@property (weak, nonatomic) IBOutlet UIView *localUserView;
@property (strong, nonatomic) IBOutletCollection(UIView) NSArray *remoteViewArr;

@property (assign, nonatomic) NERtcAudioScenarioType audioScenarioType;

@end

@implementation SetAudioQualityViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    [self setupUI];
    _audioScenarioType = kNERtcAudioScenarioDefault;
    [self setupNERtcEngine];
    [NERtcEngine.sharedEngine enableLocalAudio:YES];
    [NERtcEngine.sharedEngine enableLocalVideo:YES];
    [self setupLocalVideoCanvas];
    [self joinCurrentRoom];
}

- (void)setupUI {
    self.navigationItem.leftBarButtonItem = [[UIBarButtonItem alloc] initWithTitle:@"Leave" style:UIBarButtonItemStylePlain target:self action:@selector(onLeaveAction:)];
    self.title = [@"房间号:" stringByAppendingString:(self.roomId)];
    _volumeSlider.value = [_volumeLabel.text intValue];
}

- (void)setupNERtcEngine {
    //默认情况下日志会存储在App沙盒的Documents目录下
    NERtcLogSetting *logSetting = [[NERtcLogSetting alloc] init];

#if DEBUG
    logSetting.logLevel = kNERtcLogLevelInfo;
#else
    logSetting.logLevel = kNERtcLogLevelWarning;
#endif
    NERtcEngine *coreEngine = [NERtcEngine sharedEngine];
    NERtcEngineContext *context = [[NERtcEngineContext alloc] init];
    // 设置通话相关信息的回调
    context.engineDelegate = self;
    // 设置当前应用的appKey
    context.appKey = AppKey;
    context.logSetting = logSetting;
    [coreEngine setupEngineWithContext:context];
}

- (void)setupLocalVideoCanvas {
    NERtcVideoCanvas *canvas = [[NERtcVideoCanvas alloc] init];

    canvas.container = self.localUserView;
    [NERtcEngine.sharedEngine setupLocalVideoCanvas:canvas];
}

- (void)joinCurrentRoom {
    //如采用安全模式，则需填入相应Token
    [NERtcEngine.sharedEngine joinChannelWithToken:@""
                                       channelName:self.roomId
                                             myUid:self.userId
                                        completion:^(NSError *_Nullable error, uint64_t channelId, uint64_t elapesd, uint64_t uid) {
        if (error) {
            NSLog(@"加入房间失败");
        } else {
            NSLog(@"加入房间成功");
        }
    }];
}

- (void)destroyEngine {
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_HIGH, 0), ^{
        [NERtcEngine destroyEngine];
    });
}

# pragma mark - Helper

- (void)changeButtonColor:(UIButton *)btn {
    _audioSpeechButton.backgroundColor = [UIColor themeGrayColor];
    _audioDefaultButton.backgroundColor = [UIColor themeGrayColor];
    _audioMusicButton.backgroundColor = [UIColor themeGrayColor];
    _audioChatButton.backgroundColor = [UIColor themeGrayColor];
    btn.backgroundColor = [UIColor themeBlueColor];
}

# pragma mark - IBActions

- (IBAction)onSpeechButtonClick:(id)sender {
    _audioScenarioType = kNERtcAudioScenarioSpeech;
    [NERtcEngine.sharedEngine setAudioProfile:kNERtcAudioProfileDefault scenario:_audioScenarioType];
    [self changeButtonColor: _audioSpeechButton];
}

- (IBAction)onDefaultButtonClick:(id)sender {
    _audioScenarioType = kNERtcAudioScenarioDefault;
    [NERtcEngine.sharedEngine setAudioProfile:kNERtcAudioProfileDefault scenario:_audioScenarioType];
    [self changeButtonColor:_audioDefaultButton];
}

- (IBAction)onMusicButtonClick:(id)sender {
    _audioScenarioType = kNERtcAudioScenarioMusic;
    [NERtcEngine.sharedEngine setAudioProfile:kNERtcAudioProfileDefault scenario:_audioScenarioType];
    [self changeButtonColor:_audioMusicButton];
}

- (IBAction)onChatButtonClick:(id)sender {
    _audioScenarioType = kNERtcAudioScenarioChatRoom;
    [NERtcEngine.sharedEngine setAudioProfile:kNERtcAudioProfileDefault scenario:_audioScenarioType];
    [self changeButtonColor:_audioChatButton];
}

- (IBAction)onVolumeChanged:(UISlider *)sender {
    _volumeLabel.text = [@((uint32_t)_volumeSlider.value) stringValue];
    [NERtcEngine.sharedEngine adjustRecordingSignalVolume:(uint32_t)_volumeSlider.value];
}

- (void)onLeaveAction:(id)sender {
    [NERtcEngine.sharedEngine leaveChannel];
    [self destroyEngine];
    [self.navigationController popViewControllerAnimated:YES];
}

#pragma mark - NERtcEngine Delegate

- (void)onNERtcEngineUserDidJoinWithUserID:(uint64_t)userID userName:(NSString *)userName {
    //建立远端canvas，用来渲染远端画面
    for (UIView *view in self.remoteViewArr) {
        if (view.tag == 0) {
            NERtcVideoCanvas *canvas = [[NERtcVideoCanvas alloc] init];
            canvas.container = view;
            [NERtcEngine.sharedEngine setupRemoteVideoCanvas:canvas forUserID:userID];
            view.tag = (NSInteger)userID;
            break;
        }
    }
}

- (void)onNERtcEngineUserVideoDidStartWithUserID:(uint64_t)userID videoProfile:(NERtcVideoProfileType)profile {
    [NERtcEngine.sharedEngine subscribeRemoteVideo:YES forUserID:userID streamType:kNERtcRemoteVideoStreamTypeHigh];
}

- (void)onNERtcEngineUserDidLeaveWithUserID:(uint64_t)userID reason:(NERtcSessionLeaveReason)reason {
    //如果远端的人离开了，重置远端模型和UI
    [self.view viewWithTag:(NSInteger)userID].tag = 0;
}

- (void)onNERtcEngineDidDisconnectWithReason:(NERtcError)reason {
    NSLog(@"%s", __FUNCTION__);

    [self destroyEngine];
    [self.navigationController popViewControllerAnimated:YES];
}

- (void)onNERtcEngineDidLeaveChannelWithResult:(NERtcError)result {
}

@end
