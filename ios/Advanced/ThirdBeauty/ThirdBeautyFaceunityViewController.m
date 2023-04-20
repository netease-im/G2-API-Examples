//
//  ThirdBeautyFaceunityViewController.m
//  NERTC-API-Example-OC
//
//  Created by test on 2022/12/8.
//

#import "NEBeautyConfigView.h"
#import "NEBeautyManager.h"
#import "NTESAppConfig.h"
#import "ThirdBeautyFaceunityViewController.h"

@interface ThirdBeautyFaceunityViewController ()<NERtcEngineDelegateEx>

@property (assign, nonatomic) BOOL enableFUBeauty;
@property (strong, nonatomic) IBOutlet UIView *localUserView;
@property (strong, nonatomic) IBOutletCollection(UIView) NSArray *remoteViewArr;

@end

@implementation ThirdBeautyFaceunityViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    [self setupUI];
    
    //准备美颜资源
    [[NEBeautyManager sharedManager] prepareResource];
    
    //加入channel
    [self setupNERtcEngine];
    
    //初始化美颜模块
    [[NEBeautyManager sharedManager] initFUBeauty];
    [self setBeautyCompany];
    
    //开启美颜功能
    _enableFUBeauty = YES;
    [NERtcEngine.sharedEngine enableLocalAudio:YES];
    [NERtcEngine.sharedEngine enableLocalVideo:YES streamType:kNERtcStreamChannelTypeMainStream];
    [self setupLocalVideoCanvas];
    [self joinCurrentRoom];
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

    // 配置音视频引擎,将摄像头采集的数据回调给用户
    NSDictionary *params = @{
            kNERtcKeyVideoCaptureObserverEnabled: @YES // 将摄像头采集的数据回调给用户
    };
    [coreEngine setParameters:params];
}

- (void)setupUI {
    self.navigationItem.leftBarButtonItem = [[UIBarButtonItem alloc] initWithTitle:@"Leave" style:UIBarButtonItemStylePlain target:self action:@selector(onLeaveAction:)];
    self.title = [@"房间号:" stringByAppendingString:(self.roomId)];
}

- (void)setBeautyCompany {
    BeautyCompanyType newType = BeautyCompanyTypeFaceUnity;

    [NEBeautyManager sharedManager].companyType = newType;
    [[NEBeautyManager sharedManager] switchCompanyType:newType];
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
    [[NEBeautyManager sharedManager] destroyNEBeauty];
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_HIGH, 0), ^{
        [NERtcEngine destroyEngine];
    });
}

#pragma mark - IBActio

- (void)onLeaveAction:(id)sender {
    [NERtcEngine.sharedEngine leaveChannel];
    [self destroyEngine];
    [self.navigationController popViewControllerAnimated:YES];
}

- (IBAction)onFilterButtonClick:(UIButton *)sender {
    [[NEBeautyManager sharedManager] displayMenuWithType:NEBeautyConfigViewTypeFilter container:self.view];
}

- (IBAction)onBeautyButtonClick:(UIButton *)sender {
    [[NEBeautyManager sharedManager] displayMenuWithType:NEBeautyConfigViewTypeBeauty container:self.view];
}

- (IBAction)onSwitchButtonClick:(UISwitch *)sender {
    if (sender.on == YES) {
        NSLog(@"开启美颜效果");
        _enableFUBeauty = YES;
    } else {
        NSLog(@"关闭美颜效果");
        _enableFUBeauty = NO;
    }
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

// 在代理方法中对视频数据进行处理
- (void)onNERtcEngineVideoFrameCaptured:(CVPixelBufferRef)bufferRef rotation:(NERtcVideoRotationType)rotation
{
    // 对视频数据 bufferRef 进行处理, 务必保证 CVPixelBufferRef 的地址值不变，分辨率不变
    if (_enableFUBeauty) {
        [[NEBeautyManager sharedManager] processVideoFrameUsingFaceUnityWithFrame:bufferRef rotation:rotation];
    }
}

@end
