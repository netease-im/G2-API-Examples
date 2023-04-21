//
//  ThirdBeautyEntranceViewController.m
//  NERTC-API-Example-OC
//
//  Created by test on 2022/12/8.
//

#import "ThirdBeautyEntranceViewController.h"
#import "ThirdBeautyFaceunityViewController.h"
#import "VideoCallingEnterViewController.h"


@interface ThirdBeautyEntranceViewController ()

@end

@implementation ThirdBeautyEntranceViewController

- (void)viewDidLoad {
    [super viewDidLoad];
}

- (IBAction)onFaceunityButtonClick:(id)sender {
    VideoCallingEnterViewController *vc = [[VideoCallingEnterViewController alloc] init];
    vc.vcName = @"ThirdBeautyFaceunityViewController";
    [self.navigationController pushViewController:vc animated:YES];
}

@end
