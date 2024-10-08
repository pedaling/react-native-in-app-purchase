#import <React/RCTBridgeModule.h>
#import <React/RCTEventEmitter.h>
#import <React/RCTViewManager.h>
#import <React/RCTUIManager.h>
#import "RNInAppPurchase.h"

#import <StoreKit/StoreKit.h>

@implementation RNInAppPurchase

- (instancetype) init {
    if (self = [super init]) {
        [[SKPaymentQueue defaultQueue] addTransactionObserver: self];
    }
    productsMap = [[NSMutableDictionary alloc] init];
    transactionsMap = [[NSMutableDictionary alloc] init];

    return self;
}

- (void) dealloc {
    [[SKPaymentQueue defaultQueue] removeTransactionObserver: self];
}

+ (BOOL) requiresMainQueueSetup {
    return YES;
}

RCT_EXPORT_MODULE();

RCT_EXPORT_METHOD(fetchProducts: (NSArray*) products) {
    NSSet* identifiers = [NSSet setWithArray: [products valueForKey: @"id"]];
    SKProductsRequest* productsRequest = [[SKProductsRequest alloc] initWithProductIdentifiers: identifiers];
    productsRequest.delegate = self;
    [productsRequest start];
}

RCT_EXPORT_METHOD(onFetchProducts:(RCTResponseSenderBlock)listener) {
    fetchProductsListener = listener;
}

RCT_EXPORT_METHOD(onPurchase:(RCTResponseSenderBlock)listener) {
    purchaseListener = listener;
}

RCT_EXPORT_METHOD(onAlternativeBillingFlow:(RCTResponseSenderBlock)listener) {
    alternativeBillingFlowListener = listener;
}

RCT_EXPORT_METHOD(onError:(RCTResponseSenderBlock)listener) {
    errorListener = listener;
}

RCT_EXPORT_METHOD(flush: (RCTPromiseResolveBlock) resolve
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

        if (!transaction.transactionIdentifier) {
            continue;
        }

        NSArray* productIds = @[transaction.payment.productIdentifier];
        NSDictionary* item = @{
                               @"productIds": productIds,
                               @"transactionId": transaction.transactionIdentifier,
                               @"transactionDate": @(transaction.transactionDate.timeIntervalSince1970 * 1000),
                               @"receipt": [receipt base64EncodedStringWithOptions: 0]
                               };

        [transactionsMap setObject: transaction forKey: transaction.transactionIdentifier];
        [items addObject: item];
    }

    resolve(items);
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
                
                NSArray* productIds = @[transaction.payment.productIdentifier];
                NSDictionary* item = @{
                    @"productIds": productIds,
                    @"transactionId": transaction.transactionIdentifier,
                    @"transactionDate": @(transaction.transactionDate.timeIntervalSince1970 * 1000),
                    @"receipt": [receipt base64EncodedStringWithOptions: 0]
                };
                
                [transactionsMap setObject: transaction forKey: transaction.transactionIdentifier];
                purchaseListener(@[item]);
                break;
            }
            case SKPaymentTransactionStateFailed: {
                [[SKPaymentQueue defaultQueue] finishTransaction: transaction];
                NSDictionary* error = @{
                                        @"code": [@(transaction.error.code) stringValue],
                                        @"message": transaction.error.localizedDescription
                                        };
                [self sendError: @"PURCHASE" body: error];
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
        [productsMap setObject: product forKey: product.productIdentifier];
    }

    fetchProductsListener(@[items]); // or iap:onFetchProductsFailure
}

- (void) sendError: (NSString*) errorType body: (NSDictionary*) body {
    NSMutableDictionary *mutableDict = [body mutableCopy];
    [mutableDict setObject:errorType forKey:@"type"];
    errorListener(@[mutableDict]);
}


RCT_EXPORT_METHOD(clear) {
    
}


RCT_EXPORT_METHOD(fetchReceipt: (RCTPromiseResolveBlock) resolve
                  rejector: (RCTPromiseRejectBlock) reject) {
    NSURL* receiptURL = [[NSBundle mainBundle] appStoreReceiptURL];
    NSData* receipt = [[NSData alloc] initWithContentsOfURL: receiptURL];
    
    if (receipt == nil) {
        resolve(nil);
        return;
    }
    
    NSString* encodedReceipt = [receipt base64EncodedStringWithOptions: 0];
    resolve(encodedReceipt);
}

#if RCT_NEW_ARCH_ENABLED
RCT_EXPORT_METHOD(configure:(JS::NativeInAppPurchaseModule::SpecConfigureConfig &)config
                  resolve:(RCTPromiseResolveBlock)resolve
                   reject:(RCTPromiseRejectBlock)reject) {
    resolve(@([SKPaymentQueue canMakePayments]));
}

RCT_EXPORT_METHOD(purchase:(NSString *)productId
                  args:(JS::NativeInAppPurchaseModule::PurchaseArgs &)args) {
    SKProduct* product = productsMap[productId];

    if (!product) {
        [self sendError:@"PURCHASE" body: @{ @"message": @"Invalid product id" }];
        return;
    }
    
    SKMutablePayment* payment = [SKMutablePayment paymentWithProduct: product];
    payment.applicationUsername = args.userId();
    
    if (@available(iOS 12.2, *)) {
        NSUUID* nonce = nil;
        if (args.nonce() != nil) {
            nonce = [[NSUUID new] initWithUUIDString:args.nonce()];
        }
        
        if (args.offerId() != nil && args.keyIdentifier() != nil && nonce != nil && args.signature() != nil && args.timestamp() != std::nullopt) {
            SKPaymentDiscount* paymentDiscount = [[SKPaymentDiscount new] initWithIdentifier:args.offerId() keyIdentifier:args.keyIdentifier() nonce:nonce signature:args.signature() timestamp:[NSNumber numberWithDouble: args.timestamp().value()]];
            payment.paymentDiscount = paymentDiscount;
        }
    }
    
    [[SKPaymentQueue defaultQueue] addPayment: payment];
}

RCT_EXPORT_METHOD(finalize:(JS::NativeInAppPurchaseModule::Purchase &)purchase
                  isConsumable:(BOOL)isConsumable
                       resolve:(RCTPromiseResolveBlock)resolve
                        reject:(RCTPromiseRejectBlock)reject) {
    NSString* transactionId = purchase.transactionId();
    SKPaymentTransaction* transaction = transactionsMap[transactionId];
    [[SKPaymentQueue defaultQueue] finishTransaction: transaction];
    resolve(@{ @"message": @"Finalize success" });
}

- (std::shared_ptr<facebook::react::TurboModule>)getTurboModule:(const facebook::react::ObjCTurboModule::InitParams &)params {
  return std::make_shared<facebook::react::NativeInAppPurchaseModuleSpecJSI>(params);
}

#else
RCT_EXPORT_METHOD(configure: (NSDictionary*) config
                  resolver: (RCTPromiseResolveBlock) resolve
                  rejector: (RCTPromiseRejectBlock) reject) {
    resolve(@([SKPaymentQueue canMakePayments]));
}

RCT_EXPORT_METHOD(purchase: (NSString*) productId
                  extras: (NSDictionary*) extras) {
    SKProduct* product = productsMap[productId];

    if (!product) {
        [self sendError:@"PURCHASE" body: @{ @"message": @"Invalid product id" }];
        return;
    }
    
    NSString* offerId = extras[@"offerId"];
    NSString* userId = extras[@"userId"];
    NSString* keyIdentifier = extras[@"keyIdentifier"];
    NSString* nonceString = extras[@"nonce"];
    NSString* signature = extras[@"signature"];
    NSNumber* timestamp = extras[@"timestamp"];

    SKMutablePayment* payment = [SKMutablePayment paymentWithProduct: product];
    payment.applicationUsername = userId;
    
    if (@available(iOS 12.2, *)) {
        NSUUID* nonce = nil;
        if (nonceString != nil) {
            nonce = [[NSUUID new] initWithUUIDString:nonceString];
        }
        
        if (offerId != nil && keyIdentifier != nil && nonce != nil && signature != nil && timestamp != nil) {
            SKPaymentDiscount* paymentDiscount = [[SKPaymentDiscount new] initWithIdentifier:offerId keyIdentifier:keyIdentifier nonce:nonce signature:signature timestamp:timestamp];
            payment.paymentDiscount = paymentDiscount;
        }
    }
    
    [[SKPaymentQueue defaultQueue] addPayment: payment];
}

RCT_EXPORT_METHOD(finalize: (NSDictionary*) purchase
                  resolver: (RCTPromiseResolveBlock) resolve
                  rejector: (RCTPromiseRejectBlock) reject) {
    NSString* transactionId = purchase[@"transactionId"];
    SKPaymentTransaction* transaction = transactionsMap[transactionId];
    [[SKPaymentQueue defaultQueue] finishTransaction: transaction];
    resolve(@{ @"message": @"Finalize success" });
}

#endif /* RCT_NEW_ARCH_ENABLED */

@end
