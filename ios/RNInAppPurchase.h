#import <React/RCTBridgeModule.h>
#import <React/RCTEventEmitter.h>
#import <StoreKit/StoreKit.h>

@interface RNInAppPurchase : RCTEventEmitter <RCTBridgeModule, SKProductsRequestDelegate, SKPaymentTransactionObserver> {
}

@end
