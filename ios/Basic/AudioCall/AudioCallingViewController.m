//
//  AudioCallingViewController.m
//  NERtc_ios
//
//  Created by test on 2022/9/22.
//
#import "AudioCallingViewController.h"
#import "NTESAppConfig.h"
static const NSInteger maxRemoteUserNum = 6;

@interface AudioCallingViewController ()<NERtcEngineDelegateEx>

@property (strong, nonatomic) IBOutletCollection(UIView) NSArray *remoteViewArr;
@property (strong, nonatomic) IBOutletCollection(UILabel) NSArray *remoteLabelArr;
@property (strong, nonatomic) NSMutableOrderedSet *remoteUidSet;

@end

@implementation AudioCallingViewController

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
    [self joinCurrentRoom];
    [NERtcEngine.sharedEngine enableLocalAudio:YES];
}

- (NSMutableOrderedSet *)remoteUidSet {
    if (!_remoteUidSet) {
        _remoteUidSet = [[NSMutableOrderedSet alloc] initWithCapacity:maxRemoteUserNum];
    }

    return _remoteUidSet;
}

- (void)setupUI {
    self.navigationItem.leftBarButtonItem = [[UIBarButtonItem alloc] initWithTitle:@"Leave" style:UIBarButtonItemStylePlain target:self action:@selector(onLeaveAction:)];
    self.title = [@"房间号:" stringByAppendingString:(self.roomId)];

    for (UIView *remoteView in _remoteViewArr) {
        remoteView.hidden = YES;
    }
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
- (IBAction)onSwitchSpeakerClick:(UIButton *)sender {
    sender.selected = !sender.selected;

    if ([sender isSelected]) {
        [NERtcEngine.sharedEngine setLoudspeakerMode:NO];//启用或关闭扬声器播放。
    } else {
        [NERtcEngine.sharedEngine setLoudspeakerMode:YES];
    }
}

- (IBAction)onMicCaptureClick:(UIButton *)sender {
    sender.selected = !sender.selected;

    if ([sender isSelected]) {
        [NERtcEngine.sharedEngine muteLocalAudio:YES];//开启或关闭本地音频主流的发送。
    } else {
        [NERtcEngine.sharedEngine muteLocalAudio:NO];
    }
}

- (IBAction)onAudioCallStopClick:(UIButton *)sender {
    [NERtcEngine.sharedEngine enableLocalAudio:NO];//开启/关闭本地音频采集和发送。
    [NERtcEngine.sharedEngine leaveChannel];
    [self.navigationController popViewControllerAnimated:YES];
}

- (void)onLeaveAction:(id)sender {
    [NERtcEngine.sharedEngine leaveChannel];
    [self destroyEngine];
    [self.navigationController popViewControllerAnimated:YES];
}

#pragma mark - NERtcEngine Delegate

- (void)onNERtcEngineUserDidJoinWithUserID:(uint64_t)userID userName:(NSString *)userName {
    [self.remoteUidSet addObject:[NSString stringWithFormat:@"%llu", userID]];
    [self refreshRemoteAudioViews];
}

- (void)onNERtcEngineUserDidLeaveWithUserID:(uint64_t)userID reason:(NERtcSessionLeaveReason)reason {
    NSInteger index = [_remoteUidSet indexOfObject:[NSString stringWithFormat:@"%llu", userID]];

    if (index != NSNotFound) {
        [[_remoteViewArr objectAtIndex:index] setHidden:YES];
    }

    [self.remoteUidSet removeObject:[NSString stringWithFormat:@"%llu", userID]];
    [self refreshRemoteAudioViews];
}

- (void)onNERtcEngineDidDisconnectWithReason:(NERtcError)reason {
    NSLog(@"%s", __FUNCTION__);

    [self destroyEngine];
    [self.navigationController popViewControllerAnimated:YES];
}

- (void)onNERtcEngineDidLeaveChannelWithResult:(NERtcError)result {
}

- (void)refreshRemoteAudioViews {
    NSInteger index = 0;

    for (UIView *remoteView in _remoteViewArr) {
        remoteView.hidden = YES;
    }

    for (NSString *userId in _remoteUidSet) {
        if (index >= maxRemoteUserNum) {
            return;
        }

        [_remoteViewArr[index] setHidden:NO];
        [_remoteLabelArr[index++] setText:userId];
    }
}

@end
