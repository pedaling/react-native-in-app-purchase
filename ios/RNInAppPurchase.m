#import "RNInAppPurchase.h"

#import <StoreKit/StoreKit.h>

@interface RNInAppPurchase() <SKRequestDelegate> {
    BOOL hasListeners;
    NSDictionary* productsMap;
    NSDictionary* transactionsMap;
}

@end

@implementation RNInAppPurchase

- (instancetype) init {
    if (self = [super init]) {
        [[SKPaymentQueue defaultQueue] addTransactionObserver: self];
    }
    productsMap = @{};
    transactionsMap = @{};

    return self;
}

- (void) dealloc {
    [[SKPaymentQueue defaultQueue] removeTransactionObserver: self];
}

- (dispatch_queue_t) methodQueue {
    return dispatch_get_main_queue();
}

RCT_EXPORT_MODULE();

RCT_EXPORT_METHOD(configure: (RCTPromiseResolveBlock) resolve
                  rejector: (RCTPromiseRejectBlock) reject) {
    resolve(@([SKPaymentQueue canMakePayments]));
}

RCT_EXPORT_METHOD(getProductList: (NSArray*) productIds) {
    NSSet* identifiers = [NSSet setWithArray: productIds];
    SKProductsRequest* productsRequest = [[SKProductsRequest alloc] initWithProductIdentifiers: identifiers];
    productsRequest.delegate = self;
    [productsRequest start];
}

RCT_EXPORT_METHOD(purchase: (NSString*) productId) {
    SKProduct* product = productsMap[productId];

    if (!product) {
        [self sendEvent: @"iap:onPurchaseFailure" body: @{ @"message": @"Invalid product id" }];
        return;
    }

    SKPayment* payment = [SKPayment paymentWithProduct: product];
    [[SKPaymentQueue defaultQueue] addPayment: payment];
}

RCT_EXPORT_METHOD(finalize: (NSDictionary*) purchase
                  resolver: (RCTPromiseResolveBlock) resolve
                  rejector: (RCTPromiseRejectBlock) reject) {
    NSString* productId = purchase[@"productId"];
    SKPaymentTransaction* transaction = transactionsMap[productId];
    [[SKPaymentQueue defaultQueue] finishTransaction: transaction];
    resolve(@{ @"message": @"Finalize success" });
}

RCT_EXPORT_METHOD(restore: (RCTPromiseResolveBlock) resolve
                  rejector: (RCTPromiseRejectBlock) reject) {
    NSArray<SKPaymentTransaction*>* transactions = [[SKPaymentQueue defaultQueue] transactions];
    NSMutableArray* items = [NSMutableArray array];

    for (SKPaymentTransaction* transaction in transactions) {
        NSURL* receiptURL = [[NSBundle mainBundle] appStoreReceiptURL];
        NSData* receipt = [[NSData alloc] initWithContentsOfURL: receiptURL];

        if (!receipt) {
            [[SKPaymentQueue defaultQueue] finishTransaction: transaction];
            continue;
        }

        NSDictionary* item = @{
                               @"productId": transaction.payment.productIdentifier,
                               @"transactionId": transaction.transactionIdentifier,
                               @"transactionDate": @(transaction.transactionDate.timeIntervalSince1970 * 1000),
                               @"receipt": [receipt base64EncodedStringWithOptions: 0]
                               };

        [items addObject: item];
    }

    resolve(items);
}

- (void) startObserving {
    hasListeners = true;
}

- (void) stopObserving {
    hasListeners = false;
}

- (void) sendEvent: (NSString*) eventName body: (id) body {
    if (hasListeners) {
        [self sendEventWithName: eventName body: body];
    }
}

- (NSArray<NSString*>*) supportedEvents {
    return @[
             @"iap:onProductListSuccess",
             @"iap:onProductListFailure",
             @"iap:onPurchaseSuccess",
             @"iap:onPurchaseFailure"
             ];
}

- (void) productsRequest: (SKProductsRequest*) request didReceiveResponse: (SKProductsResponse*) response {
    NSMutableArray* items = [NSMutableArray array];

    for (SKProduct* product in response.products) {
        NSString* title = product.localizedTitle ? product.localizedTitle : @"";
        NSString* description = product.localizedDescription ? product.localizedDescription : @"";
        NSString* currency = @"";

        if (@available(iOS 10.0, *)) {
            currency = product.priceLocale.currencyCode;
        }

        NSDictionary* item = @{
                               @"productId": product.productIdentifier,
                               @"price": [product.price stringValue],
                               @"currency": currency,
                               @"title": title,
                               @"description": description
                               };
        [items addObject: item];
        [productsMap setValue: item forKey: product.productIdentifier];
    }

    [self sendEvent: @"iap:onProductListSuccess" body: items];
}

- (void) paymentQueue: (SKPaymentQueue*) queue updatedTransactions: (NSArray<SKPaymentTransaction*>*) transactions {
    for (SKPaymentTransaction* transaction in transactions) {
        switch (transaction.transactionState) {
            case SKPaymentTransactionStatePurchased: {
                NSURL* receiptURL = [[NSBundle mainBundle] appStoreReceiptURL];
                NSData* receipt = [[NSData alloc] initWithContentsOfURL: receiptURL];
                
                if (!receipt) {
                    [[SKPaymentQueue defaultQueue] finishTransaction: transaction];
                    return;
                }
                
                NSDictionary* item = @{
                                       @"productId": transaction.payment.productIdentifier,
                                       @"transactionId": transaction.transactionIdentifier,
                                       @"transactionDate": @(transaction.transactionDate.timeIntervalSince1970 * 1000),
                                       @"receipt": [receipt base64EncodedStringWithOptions: 0]
                                       };
                [self sendEvent: @"iap:onPurchaseSuccess" body: item];
                break;
            }
            case SKPaymentTransactionStateFailed: {
                [[SKPaymentQueue defaultQueue] finishTransaction: transaction];
                NSDictionary* error = @{
                                        @"code": [@(transaction.error.code) stringValue],
                                        @"message": transaction.error.localizedDescription
                                        };
                [self sendEvent: @"iap:onPurchaseFailure" body: error];
                break;
            }
            case SKPaymentTransactionStateRestored:
                [[SKPaymentQueue defaultQueue] finishTransaction: transaction];
                break;
            case SKPaymentTransactionStateDeferred:
                break;
            case SKPaymentTransactionStatePurchasing:
                break;
        }
    }
}

@end
