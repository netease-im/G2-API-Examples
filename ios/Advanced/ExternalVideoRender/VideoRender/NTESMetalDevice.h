//
//  NTESMetalDevice.h
//  NERtcDemo
//
//  Created by zhangchenliang on 2023/2/16.
//

#import <Foundation/Foundation.h>
#import <Metal/Metal.h>

@interface NTESMetalDevice : NSObject

+ (instancetype)sharedDevice;

- (id<MTLDevice>)getDevice;

- (id<MTLLibrary>)getLibrary;

@end
