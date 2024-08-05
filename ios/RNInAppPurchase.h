#ifndef RNInAppPurchase_h
#define RNInAppPurchase_h

#import <React/RCTBridgeModule.h>
#import <React/RCTEventEmitter.h>
#import <StoreKit/StoreKit.h>

#ifdef RCT_NEW_ARCH_ENABLED
#import <InAppPurchase/InAppPurchase.h>
#endif /* RCT_NEW_ARCH_ENABLED */

@interface RNInAppPurchase :
#ifdef RCT_NEW_ARCH_ENABLED
NSObject<NativeInAppPurchaseModuleSpec, SKProductsRequestDelegate, SKPaymentTransactionObserver>
#else
RCTEventEmitter<RCTBridgeModule, SKProductsRequestDelegate, SKPaymentTransactionObserver>
#endif /* RCT_NEW_ARCH_ENABLED */
{
    BOOL hasListeners;
    NSMutableDictionary* productsMap;
    NSMutableDictionary* transactionsMap;
    
    RCTResponseSenderBlock fetchProductsListener;
    RCTResponseSenderBlock purchaseListener;
    RCTResponseSenderBlock errorListener;
    RCTResponseSenderBlock alternativeBillingFlowListener;
}
@end

#endif /* RNInAppPurchase_h */

