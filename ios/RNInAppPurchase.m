#import "RNInAppPurchase.h"

#import <StoreKit/StoreKit.h>

@interface RNInAppPurchase() <SKRequestDelegate> {
}

@end

@implementation RNInAppPurchase

- (instancetype) init {
    if (self = [super init]) {
        [[SKPaymentQueue defaultQueue] addTransactionObserver: self];
    }
}

- (void) dealloc {
    [[SKPaymentQueue defaultQueue] removeTransactionObserver: self];
}

- (dispatch_queue_t)methodQueue
{
    return dispatch_get_main_queue();
}

RCT_EXPORT_MODULE();

RCT_EXPORT_METHOD(configure: (RCTPromiseResolveBlock) resolve
                  rejector: (RCTPromiseRejectBlock) reject) {
    resolve(@([SKPaymentQueue canMakePayments]));
}

@end
