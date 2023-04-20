//
//  basicViewController.h
//  NERTC-API-Example-OC
//
//  Created by test on 2022/10/27.
//

#import <UIKit/UIKit.h>

NS_ASSUME_NONNULL_BEGIN

@interface basicViewController : UIViewController

- (instancetype)initWithRoomId:(NSString *)roomId userId:(uint64_t)userId;

@property (assign, nonatomic) uint64_t userId;
@property (copy, nonatomic) NSString *roomId;

@end

NS_ASSUME_NONNULL_END
