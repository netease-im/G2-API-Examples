//
//  basicViewController.m
//  NERTC-API-Example-OC
//
//  Created by test on 2022/10/27.
//

#import "basicViewController.h"

@interface basicViewController ()

@end

@implementation basicViewController

- (void)viewDidLoad {
    [super viewDidLoad];
}

- (instancetype)initWithRoomId:(NSString *)roomId userId:(uint64_t)userId {
    self = [super initWithNibName:NSStringFromClass([self class]) bundle:nil];

    if (self) {
        _roomId = [roomId copy];
        _userId = userId;
    }

    return self;
}

@end
