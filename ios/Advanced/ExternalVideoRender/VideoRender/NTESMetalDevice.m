//
//  NTESMetalDevice.m
//  NERtcDemo
//
//  Created by zhangchenliang on 2023/2/16.
//

#import "NTESMetalDevice.h"

@interface NTESMetalDevice ()

@property (nonatomic, strong) id<MTLDevice> device;
@property (nonatomic, strong) id<MTLLibrary> defaultLibrary;

@end

@implementation NTESMetalDevice

+ (instancetype)sharedDevice {
    static NTESMetalDevice *device = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        device = [[NTESMetalDevice alloc] init];
    });
    
    return device;
}

- (instancetype)init {
    self = [super init];
    if (self) {
        // 获取系统默认的GPU设备
        _device = MTLCreateSystemDefaultDevice();
        
        // 加载Library
//        _defaultLibrary = [_device newDefaultLibrary];
        NSError *libraryError = nil;
        id<MTLLibrary> sourceLibrary = nil;
        // https://stackoverflow.com/questions/46742403/metal-file-as-part-of-an-ios-framework
        // 综合了易用性、安全性、效率的考虑，外部无法看到内部的Shader语言
        NSBundle *bundle = [NSBundle bundleForClass:[self class]];
        NSString *filePath = [bundle pathForResource:@"NTESMetalShader" ofType:@"metallib"];
        BOOL exist = [[NSFileManager defaultManager] fileExistsAtPath:filePath];
        if (exist) {
            sourceLibrary = [_device newLibraryWithFile:filePath error:&libraryError];
            if (libraryError) {
                NSLog(@"Metal: Library with source failed\n%@", libraryError);
            }
            if (!sourceLibrary) {
                NSLog(@"Metal: Failed to load library. %@", libraryError);
            }
            _defaultLibrary = sourceLibrary;
        }
    }
    
    return self;
}

- (id<MTLDevice>)getDevice {
    return _device;
}

- (id<MTLLibrary>)getLibrary {
    return _defaultLibrary;
}

@end
