//
//  VideoCallingViewController.m
//  NERTC-API-Example-OC
//
//  Created by test on 2022/9/26.
//

#import "NTESAppConfig.h"
#import "VideoCallingViewController.h"

@interface VideoCallingViewController ()<NERtcEngineDelegateEx>

@property (strong, nonatomic) IBOutletCollection(UIView) NSArray *remoteViewArr;//远端用户视图
@property (strong, nonatomic) IBOutlet UIView *localUserView;//本地用户视图

@end

@implementation VideoCallingViewController

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

- (void)viewDidLoad {
    [super viewDidLoad];
    [self setupUI];
    [self setupNERtcEngine];
    [NERtcEngine.sharedEngine enableLocalAudio:YES];
    [NERtcEngine.sharedEngine enableLocalVideo:YES];
    [self setupLocalVideoCanvas];
    [self joinCurrentRoom];
}

- (void)setupUI {
    self.navigationItem.leftBarButtonItem = [[UIBarButtonItem alloc] initWithTitle:@"Leave" style:UIBarButtonItemStylePlain target:self action:@selector(onLeaveAction:)];
    self.title = [@"房间号:" stringByAppendingString:(self.roomId)];
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

#pragma mark - IBActions
- (IBAction)onSwitchCameraClick:(UIButton *)sender {
    sender.selected = !sender.selected;
    [NERtcEngine.sharedEngine switchCamera];//切换前置/后置摄像头
}

- (IBAction)onVideoCaptureClick:(UIButton *)sender {
    sender.selected = !sender.selected;

    if ([sender isSelected]) {
        [NERtcEngine.sharedEngine enableLocalVideo:NO];//是否开启本地视频采集
    } else {
        [NERtcEngine.sharedEngine enableLocalVideo:YES];
    }
}

- (IBAction)onMicCaptureClick:(UIButton *)sender {
    sender.selected = !sender.selected;

    if ([sender isSelected]) {
        [NERtcEngine.sharedEngine muteLocalAudio:YES];//开启或关闭本地音频主流的发送
    } else {
        [NERtcEngine.sharedEngine muteLocalAudio:NO];
    }
}

- (IBAction)onSwitchSpeakerClick:(UIButton *)sender {
    sender.selected = !sender.selected;

    if ([sender isSelected]) {
        [NERtcEngine.sharedEngine setLoudspeakerMode:NO];//启用或关闭扬声器播放。
    } else {
        [NERtcEngine.sharedEngine setLoudspeakerMode:YES];
    }
}

- (void)onLeaveAction:(id)sender {
    [NERtcEngine.sharedEngine leaveChannel];
    [self destroyEngine];
    [self.navigationController popViewControllerAnimated:YES];
}

#pragma mark - NERtcEngine Delegate

- (void)onNERtcEngineUserDidJoinWithUserID:(uint64_t)userID userName:(NSString *)userName
{
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
