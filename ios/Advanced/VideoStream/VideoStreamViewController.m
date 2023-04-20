//
//  VideoStreamViewController.m
//  NERTC-API-Example-OC
//
//  Created by test on 2022/10/26.
//

#import "NSString+Common.h"
#import "NTESAppConfig.h"
#import "VideoStreamViewController.h"

@interface VideoStreamViewController ()<NERtcEngineDelegateEx>

@property (weak, nonatomic) IBOutlet UITextView *pushSteamUrlText;
@property (weak, nonatomic) IBOutlet UIView *localUserView;
@property (strong, nonatomic) IBOutletCollection(UIView) NSArray *remoteViewArr;
@property (weak, nonatomic) IBOutlet NSLayoutConstraint *bottomConstraint;

@property (strong, nonatomic) NERtcLiveStreamTaskInfo *liveStreamTask;
@property (assign, nonatomic) BOOL isPushingStream;  //是否在推流中
@property (strong, nonatomic) NSMutableArray<NSNumber *> *userList;

@end

@implementation VideoStreamViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    [self setupUI];
    [self setupNERtcEngine];
    [NERtcEngine.sharedEngine enableLocalAudio:YES];
    [NERtcEngine.sharedEngine enableLocalVideo:YES];
    [self setupLocalVideoCanvas];
    [self joinCurrentRoom];
    [self addKeyboardObserver];
}

- (void)setupUI {
    self.navigationItem.leftBarButtonItem = [[UIBarButtonItem alloc] initWithTitle:@"Leave" style:UIBarButtonItemStylePlain target:self action:@selector(onLeaveAction:)];
    self.title = [@"房间号:" stringByAppendingString:(self.roomId)];
    self.userList = [NSMutableArray arrayWithObject:@(self.userId)];
}

- (void)setupNERtcEngine
{
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
    //打开推流
    [coreEngine setParameters:@{ kNERtcKeyPublishSelfStreamEnabled: @YES }];
}

- (void)setupLocalVideoCanvas {
    NERtcVideoCanvas *canvas = [[NERtcVideoCanvas alloc] init];

    canvas.container = self.localUserView;
    [NERtcEngine.sharedEngine setupLocalVideoCanvas:canvas];
}

- (void)joinCurrentRoom
{
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

- (void)updateLiveStreamTask
{
    int ret = [NERtcEngine.sharedEngine updateLiveStreamTask:self.liveStreamTask
                                                  compeltion:^(NSString *_Nonnull taskId, kNERtcLiveStreamError errorCode) {
        self.isPushingStream = !errorCode ? YES : NO;//errorCode == 0表示成功
        NSString *message = !errorCode ? @"更新成功" : [NSString stringWithFormat:@"更新失败 error = %@", NERtcErrorDescription(errorCode)];
        NSLog(@"%@", message);
    }];

    if (ret != 0) {
        self.isPushingStream = NO;
        NSLog(@"更新推流任务失败");
    }
}

- (void)addLiveStream:(NSString *)streamURL
{
    self.liveStreamTask = [[NERtcLiveStreamTaskInfo alloc] init];
    NSString *taskID = [NSString stringWithFormat:@"%d", arc4random() / 100];
    self.liveStreamTask.taskID = taskID;
    self.liveStreamTask.streamURL = streamURL;
    self.liveStreamTask.lsMode = kNERtcLsModeVideo;

    NSInteger layoutWidth = 720;
    NSInteger layoutHeight = 1280;
    //设置整体布局
    NERtcLiveStreamLayout *layout = [[NERtcLiveStreamLayout alloc] init];
    layout.width = layoutWidth; //整体布局宽度
    layout.height = layoutHeight; //整体布局高度
    // layout.bgImage = <#在这里设置背景图片(可选)#>
    // layout.backgroundColor = <#在这里设置背景色(可选)#>
    self.liveStreamTask.layout = layout;

    [self reloadUsers];
    int ret = [NERtcEngine.sharedEngine addLiveStreamTask:self.liveStreamTask
                                               compeltion:^(NSString *_Nonnull taskId, kNERtcLiveStreamError errorCode) {
        if (errorCode == 0) {
            self.isPushingStream = YES;
        } else {
            self.isPushingStream = NO;
            self.liveStreamTask = nil;
        }

        NSString *message = !errorCode ? @"添加成功" : [NSString stringWithFormat:@"添加失败 error = %@", NERtcErrorDescription(errorCode)];
        NSLog(@"%@", message);
    }];

    if (ret != 0) {
        self.isPushingStream = NO;
        self.liveStreamTask = nil;
        NSLog(@"添加推流任务失败");
    }
}

//根据self.usersForStreaming生成直播成员信息
//设置4人视频画面“田”字布局，旁路推流是将多路视频流同步到云端进行混流成一路流，客户端可以通过拉流地址获取到多人画面，此UI配置是指定服务端混流后各个画面的布局。
- (void)reloadUsers
{
    NSInteger layoutWidth = self.liveStreamTask.layout.width;
    NSInteger userWidth = 320;
    NSInteger userHeight = 480;
    NSInteger horizPadding = (layoutWidth - userWidth * 2) / 3;
    NSInteger vertPadding = 16;
    NSMutableArray *res = NSMutableArray.array;

    for (NSInteger i = 0; i < self.userList.count; i++) {
        NSInteger column = i % 2;
        NSInteger row = i / 2;
        NSNumber *userID = self.userList[i];
        NERtcLiveStreamUserTranscoding *userTranscoding = [[NERtcLiveStreamUserTranscoding alloc] init];
        userTranscoding.uid = userID.unsignedLongValue;
        userTranscoding.audioPush = YES;
        userTranscoding.videoPush = YES;
        userTranscoding.x = column == 0 ? horizPadding : horizPadding * 2 + userWidth;
        userTranscoding.y = vertPadding * (row + 1) + userHeight * row;
        userTranscoding.width = userWidth;
        userTranscoding.height = userHeight;
        userTranscoding.adaption = kNERtcLsModeVideoScaleCropFill;
        [res addObject:userTranscoding];
    }

    self.liveStreamTask.layout.users = [NSArray arrayWithArray:res];
}

#pragma mark - IBActions

- (IBAction)onStartPushStreamButtonClick:(UIButton *)sender {
    if ([sender isSelected]) {
        if (self.isPushingStream) {
            //停止推流
            [self stopPushStream];
        }
    } else {
        if (self.pushSteamUrlText.text.length) {
            [self addLiveStream:self.pushSteamUrlText.text];
        }
    }

    sender.selected = !sender.selected;
}

- (void)onLeaveAction:(id)sender {
    [NERtcEngine.sharedEngine leaveChannel];
    [self removeKeyboardObserver];
    [self destroyEngine];
    [self.navigationController popViewControllerAnimated:YES];
}

#pragma mark - Notification
- (void)addKeyboardObserver {
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(keyboardWillShow:) name:UIKeyboardWillShowNotification object:nil];
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(keyboardWillHide:) name:UIKeyboardWillHideNotification object:nil];
}

- (void)removeKeyboardObserver {
    [[NSNotificationCenter defaultCenter] removeObserver:self name:UIKeyboardWillShowNotification object:nil];
    [[NSNotificationCenter defaultCenter] removeObserver:self name:UIKeyboardWillHideNotification object:nil];
}

- (BOOL)keyboardWillShow:(NSNotification *)noti {
    CGFloat animationDuration = [[[noti userInfo] objectForKey:UIKeyboardAnimationDurationUserInfoKey] floatValue];
    CGRect keyboardBounds = [[[noti userInfo] objectForKey:UIKeyboardFrameEndUserInfoKey] CGRectValue];

    [UIView animateWithDuration:animationDuration
                     animations:^{
        self.bottomConstraint.constant = keyboardBounds.size.height;
    }];
    return YES;
}

- (BOOL)keyboardWillHide:(NSNotification *)noti {
    CGFloat animationDuration = [[[noti userInfo] objectForKey:UIKeyboardAnimationDurationUserInfoKey] floatValue];

    [UIView animateWithDuration:animationDuration
                     animations:^{
        self.bottomConstraint.constant = 25;
    }];
    return YES;
}

#pragma mark - NTESVideoConfigVCDelegate

- (void)stopPushStream
{
    int res = [NERtcEngine.sharedEngine removeLiveStreamTask:self.liveStreamTask.taskID
                                                  compeltion:^(NSString *_Nonnull taskId, kNERtcLiveStreamError errorCode) {
        if (errorCode == 0) {
            self.isPushingStream = NO;
            self.liveStreamTask = nil;
        } else {
            NSString *errorMsg = [NSString stringWithFormat:@"移除推流任务失败:%@", NERtcErrorDescription(errorCode)];
            NSLog(@"%@", errorMsg);
        }
    }];

    if (res != 0) {
        NSLog(@"移除推流任务失败");
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

- (void)onNERtcEngineUserVideoDidStartWithUserID:(uint64_t)userID videoProfile:(NERtcVideoProfileType)profile
{
    [NERtcEngine.sharedEngine subscribeRemoteVideo:YES forUserID:userID streamType:kNERtcRemoteVideoStreamTypeHigh];

    if (![self.userList containsObject:@(userID)]) {
        // 新加入用户，添加至直播成员
        [self.userList addObject:@(userID)];
        [self reloadUsers];
        [self updateLiveStreamTask];
    }
}

- (void)onNERtcEngineUserVideoDidStop:(uint64_t)userID
{
    if ([self.userList containsObject:@(userID)]) {
        // 用户离开，从直播成员中移除
        [self.userList removeObject:@(userID)];
        [self reloadUsers];
        [self updateLiveStreamTask];
    }
}

- (void)onNERTCEngineLiveStreamState:(NERtcLiveStreamStateCode)state taskID:(NSString *)taskID url:(NSString *)url
{
    switch (state) {
        case kNERtcLsStatePushing:
            NSLog(@"Pushing stream for task [%@]", taskID);
            break;

        case kNERtcLsStatePushStopped:
            NSLog(@"Stream for task [%@] stopped", taskID);
            break;

        case kNERtcLsStatePushFail:
            NSLog(@"Stream for task [%@] failed", taskID);
            break;

        default:
            NSLog(@"Unknown state for task [%@]", taskID);
            break;
    }
}

- (void)onNERtcEngineUserDidLeaveWithUserID:(uint64_t)userID reason:(NERtcSessionLeaveReason)reason
{
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

- (void)touchesBegan:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    [super touchesBegan:touches withEvent:event];
    [self.view endEditing:YES];
}

@end
