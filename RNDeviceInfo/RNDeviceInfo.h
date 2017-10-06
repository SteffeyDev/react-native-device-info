//
//  RNDeviceInfo.h
//  Learnium
//
//  Created by Rebecca Hughes on 03/08/2015.
//  Copyright © 2015 Learnium Limited. All rights reserved.
//

#import <UIKit/UIKit.h>
#import <sys/utsname.h>

#if __has_include(<React/RCTAssert.h>)
#import <React/RCTBridgeModule.h>
#import <React/RCTEventEmitter.h>
#else
#import "RCTBridgeModule.h"
#import "RCTEventEmitter.h"
#endif

@interface RNDeviceInfo : RCTEventEmitter <RCTBridgeModule>

+ (void)viewWillTransitionToSize:(CGSize)size withTransitionCoordinator:(id<UIViewControllerTransitionCoordinator>)coordinator;
+ (void)willTransitionToTraitCollection:(UITraitCollection *)newCollection withTransitionCoordinator:(id<UIViewControllerTransitionCoordinator>)coordinator;

@end
