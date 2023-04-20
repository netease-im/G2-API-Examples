//
//  CustomCaptureViewController.m
//  NERTC-API-Example-OC
//
//  Created by test on 2022/10/19.
//

#import <AVFoundation/AVFoundation.h>
#import <CoreMedia/CoreMedia.h>
#import <CoreServices/CoreServices.h>
#import <NERtcSDK/NERtcSDK.h>
#import "CustomCaptureViewController.h"
#import "NTESAppConfig.h"
#import "NTESExternalVideoReader.h"

@interface CustomCaptureViewController ()<NERtcEngineDelegateEx, UIImagePickerControllerDelegate, UINavigationControllerDelegate, NTESExternalVideoReaderDelegate>

@property (strong, nonatomic) IBOutletCollection(UIView) NSArray *remoteViewArr;//远端用户视图
@property (weak, nonatomic) IBOutlet UIView *localUserView;

@property (strong, nonatomic) NTESExternalVideoReader *videoReader;

@end

@implementation CustomCaptureViewController

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
    //开启外部视频源
    [NERtcEngine.sharedEngine setExternalVideoSource:YES isScreen:NO];
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

#pragma mark - NERtcEngine Delegate

- (void)onNERtcEngineUserDidJoinWithUserID:(uint64_t)userID userName:(NSString *)userName {
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
    [self.view viewWithTag:(NSInteger)userID].tag = 0;
}

- (void)onNERtcEngineDidDisconnectWithReason:(NERtcError)reason {
    NSLog(@"%s", __FUNCTION__);

    [self destroyEngine];
    [self.navigationController popViewControllerAnimated:YES];
}

- (void)onNERtcEngineDidLeaveChannelWithResult:(NERtcError)result {
}

#pragma mark - UIImagePickerControllerDelegate

- (void)imagePickerController:(UIImagePickerController *)picker didFinishPickingMediaWithInfo:(NSDictionary<UIImagePickerControllerInfoKey, id> *)info {
    [self dismissViewControllerAnimated:YES completion:nil];
    NSURL *videoURL = info[UIImagePickerControllerMediaURL];

    if (videoURL) {
        NSError *error;
        self.videoReader = [[NTESExternalVideoReader alloc] initWithURL:videoURL error:&error];

        if (error) {
            NSLog(@"Create video reader error: \n%@", error.localizedDescription);
            return;
        }

        self.videoReader.delegate = self;
        [self.videoReader startReading];
    }
}

- (void)imagePickerControllerDidCancel:(UIImagePickerController *)picker {
    [self dismissViewControllerAnimated:YES completion:nil];
}

#pragma mark - NTESExternalVideoReaderDelegate

- (void)videoReader:(NTESExternalVideoReader *)videoReader didReadSampleBuffer:(CMSampleBufferRef)sampleBuffer totalFramesWritten:(NSUInteger)totalFramesWritten totalFrames:(NSUInteger)totalFrames {
    CVImageBufferRef pixelBuffer = CMSampleBufferGetImageBuffer(sampleBuffer);
    NERtcVideoFrame *frame = [[NERtcVideoFrame alloc] init];

    frame.format = kNERtcVideoFormatNV12;
    frame.width = (uint32_t)CVPixelBufferGetWidth(pixelBuffer);
    frame.height = (uint32_t)CVPixelBufferGetHeight(pixelBuffer);
    frame.buffer = (void *)pixelBuffer;
    switch (videoReader.rotationDegree) {
        case 90:
            frame.rotation = kNERtcVideoRotation_90;
            break;

        case 180:
            frame.rotation = kNERtcVideoRotation_180;
            break;

        case 270:
            frame.rotation = kNERtcVideoRotation_270;
            break;

        case 0:
        default:
            frame.rotation = kNERtcVideoRotation_0;
            break;
    }
    [NERtcEngine.sharedEngine pushExternalVideoFrame:frame];
}

#pragma mark - Action

- (void)onLeaveAction:(id)sender {
    [NERtcEngine.sharedEngine leaveChannel];
    [self destroyEngine];
    [self.navigationController popViewControllerAnimated:YES];
}

- (IBAction)onSelectVideoButtonClick:(id)sender {
    UIImagePickerController *imagePicker = [[UIImagePickerController alloc] init];

    imagePicker.delegate = self;
    imagePicker.sourceType = UIImagePickerControllerSourceTypePhotoLibrary;
    imagePicker.mediaTypes = @[(__bridge NSString *)kUTTypeMovie, (__bridge NSString *)kUTTypeVideo];
    [self presentViewController:imagePicker animated:YES completion:nil];
}

- (IBAction)onExitRoomButtonClick:(id)sender {
    [self.videoReader stopReading];
    [NERtcEngine.sharedEngine leaveChannel];
    [self destroyEngine];
    [self.navigationController popViewControllerAnimated:YES];
}

@end
