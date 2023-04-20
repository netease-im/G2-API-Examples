//
//  NEBeautyManager.h
//  NERTC-API-Example-OC
//
//  Created by test on 2022/11/24.
//

#import <UIKit/UIKit.h>
#import "NEBeautyMacro.h"
#import <NERtcSDK/NERtcSDK.h>

@interface NEBeautyManager : NSObject

@property (nonatomic, assign) BeautyCompanyType companyType;

+ (instancetype)sharedManager;

- (void)prepareResource;

- (void)initNEBeauty;

- (void)initFUBeauty;
    
- (void)destroyNEBeauty;

- (void)enableNEBeauty:(BOOL)enable;

- (void)processVideoFrameUsingFaceUnityWithFrame:(CVPixelBufferRef)pixelBuffer
                                        rotation:(NERtcVideoRotationType)rotation;

/// 展示菜单
/// @param type 菜单类型（可以组合）
/// @param container 父视图
- (void)displayMenuWithType:(NEBeautyConfigViewType)type container:(UIView *)container;

/// 关闭菜单
- (void)dismissMenuWithType:(NEBeautyConfigViewType)type;

- (void)switchCompanyType:(BeautyCompanyType)type;

@end
