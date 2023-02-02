package net.class101.iap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;
import com.facebook.common.internal.ImmutableList;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RNInAppPurchaseModule extends ReactContextBaseJavaModule implements PurchasesUpdatedListener {

    private final ReactApplicationContext reactContext;

    private final Map<String, ProductDetails> productDetailsMap;

    private BillingClient client;

    public RNInAppPurchaseModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        this.productDetailsMap = new HashMap<>();
    }

    @Override
    public String getName() {
        return "RNInAppPurchase";
    }

    /**
     * Initialize billing client.
     *
     * @param promise Promise that receives initialization result.
     */
    @ReactMethod
    public void configure(final Promise promise) {
        if (client != null && client.isReady()) {
            promise.resolve(true);
            return;
        }

        client = BillingClient.newBuilder(reactContext)
            .enablePendingPurchases()
            .setListener(this)
            .build();

        client.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult result) {
                int code = result.getResponseCode();

                if (code == BillingClient.BillingResponseCode.OK) {
                    promise.resolve(true);
                } else {
                    promise.reject("configure", "Billing service setup failed with code " + code);
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                promise.reject("configure", "Billing service disconnected");
            }
        });
    }

    /**
     * Retrieves a list of in-app and subscription items that match the IDs contained in productIds.
     * For iOS compatibility, this method fetches in-app and subscription items at once.
     *
     * @param products An array of product ID and type of the items to retrieve.
     */
    @ReactMethod
    public void fetchProducts(ReadableArray products) {
        tryConnect(() -> {
            ImmutableList<QueryProductDetailsParams.Product> productList = ImmutableList.of();

            for (int i = 0; i < products.size(); i++) {
                ReadableMap product = products.getMap(i);

                String productId = product.getString("id");
                String productType = product.getString("type");

                if (productId == null || productType == null) {
                    continue;
                }

                if (!productType.equals(BillingClient.ProductType.SUBS) && !productType.equals(BillingClient.ProductType.INAPP)) {
                    continue;
                }

                productList.add(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productId)
                        .setProductType(productType)
                        .build()
                );
            }

            QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build();

            final WritableArray items = new WritableNativeArray();

            // Query in-app items
            client.queryProductDetailsAsync(params, (billingResult, productDetailsList) -> {
                if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                    sendBillingError("iap:onFetchProductsFailure", billingResult);
                    return;
                }

                for (ProductDetails productDetails : productDetailsList) {
                    WritableMap item = Arguments.createMap();
                    item.putString("productId", productDetails.getProductId());
                    item.putString("title", productDetails.getTitle());
                    item.putString("description", productDetails.getDescription());
                    items.pushMap(item);
                    productDetailsMap.put(productDetails.getProductId(), productDetails);
                }

                sendEvent("iap:onFetchProductsSuccess", items);
            });
        });
    }

    /**
     * Purchase in-app or subscription item.
     *
     * @param productId Unique ID of item to purchase.
     */
    @ReactMethod
    public void purchase(
        String productId,
        @Nullable String[] tags,
        @Nullable String originalPurchaseToken,
        @Nullable String obfuscatedAccountId,
        @Nullable String obfuscatedProfileId
    ) {
        tryConnect(() -> {
            if (getCurrentActivity() == null) {
              return;
            }

            ProductDetails productDetails = productDetailsMap.get(productId);

            if (productDetails == null) {
                return;
            }

            BillingFlowParams.Builder builder = BillingFlowParams.newBuilder();

            if (obfuscatedAccountId != null) {
                builder.setObfuscatedAccountId(obfuscatedAccountId);
            }

            if (obfuscatedProfileId != null) {
                builder.setObfuscatedProfileId(obfuscatedProfileId);
            }

            if (originalPurchaseToken != null) {
                builder.setSubscriptionUpdateParams(
                    BillingFlowParams.SubscriptionUpdateParams.newBuilder()
                        .setOldPurchaseToken(originalPurchaseToken)
                        .build()
                );
            }

            BillingFlowParams.ProductDetailsParams.Builder productDetailsParamsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder();
            List<ProductDetails.SubscriptionOfferDetails> offers = productDetails.getSubscriptionOfferDetails();

            if (tags != null && offers != null) {
                offers = offers.stream()
                    .filter(offer -> Arrays.stream(tags).allMatch(tag -> offer.getOfferTags().contains(tag)))
                    .collect(Collectors.toList());

                if (offers.size() > 0) {
                  productDetailsParamsBuilder.setOfferToken(offers.get(0).getOfferToken());
                }
            }

            ImmutableList<BillingFlowParams.ProductDetailsParams> productDetailsParamsList = ImmutableList.of(productDetailsParamsBuilder.build());
            BillingFlowParams params = builder.setProductDetailsParamsList(productDetailsParamsList).build();

            client.launchBillingFlow(getCurrentActivity(), params);
        });
    }

    /**
     * For consumable items, call the {@link BillingClient#consumeAsync} method, otherwise call the
     * {@link BillingClient#acknowledgePurchase} method and finish the purchase flow.
     *
     * @param purchase Purchase object received after payment is made.
     * @param isConsumable Whether it is consumable or not.
     */
    @ReactMethod
    public void finalize(ReadableMap purchase, boolean isConsumable, final Promise promise) {
        tryConnect(() -> {
            String token = purchase.getString("purchaseToken");

            if (token == null) {
                return;
            }

            if (isConsumable) {
                ConsumeParams params = ConsumeParams.newBuilder()
                    .setPurchaseToken(token)
                    .build();

                client.consumeAsync(params, (result, purchaseToken) -> {
                    if (result.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                        promise.reject("finalize", result.getDebugMessage());
                        return;
                    }

                    WritableMap event = Arguments.createMap();
                    event.putString("message", result.getDebugMessage());

                    promise.resolve(event);
                });
                return;
            }

            AcknowledgePurchaseParams acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(token)
                .build();

            client.acknowledgePurchase(acknowledgePurchaseParams, (result) -> {
                if (result.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                    promise.reject("finalize", result.getDebugMessage());
                    return;
                }

                WritableMap event = Arguments.createMap();
                event.putString("message", result.getDebugMessage());

                promise.resolve(event);
            });
        });
    }

    /**
     * Retrieves all in-app and subscription items that were purchased but not finalized. You should
     * validate these payments on your backend server and complete the purchase flow by calling
     * {@link #finalize} method.
     */
    @ReactMethod
    public void flush(final Promise promise) {
        tryConnect(() -> client.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(),
            (inAppResult, inAppPurchases) -> {
                if (inAppResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                    promise.reject("flush", inAppResult.getDebugMessage());
                    return;
                }

                client.queryPurchasesAsync(
                    QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build(),
                    (subscriptionResult, subscriptionPurchases) -> {
                        if (subscriptionResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                            promise.reject("flush", subscriptionResult.getDebugMessage());
                            return;
                        }

                        List<Purchase> purchaseList = new ArrayList<>();

                        purchaseList.addAll(inAppPurchases);
                        purchaseList.addAll(subscriptionPurchases);

                        WritableArray items = new WritableNativeArray();

                        for (Purchase purchase : purchaseList) {
                            if (purchase.isAcknowledged()) {
                                continue;
                            }

                            ReadableMap item = this.buildPurchaseJSON(purchase);
                            items.pushMap(item);
                        }

                        promise.resolve(items);
                    }
                );
            }
        ));
    }

    private void sendEvent(String eventName, Object params) {
        getReactApplicationContext()
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
            .emit(eventName, params);
    }

    private void sendBillingError(String errorName, BillingResult result) {
        WritableMap event = Arguments.createMap();
        event.putInt("code", result.getResponseCode());
        event.putString("message", result.getDebugMessage());

        sendEvent(errorName, event);
    }

    private void tryConnect(final Runnable runnable) {
        if (client.isReady()) {
            runnable.run();
            return;
        }

        client.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult result) {
                if (result.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    runnable.run();
                } else {
                  sendBillingError("iap:onConnectionFailure", result);
                }
            }

            @Override
            public void onBillingServiceDisconnected() {}
        });
    }

    @Override
    public void onPurchasesUpdated(BillingResult result, @Nullable List<Purchase> purchases) {
        if (result.getResponseCode() != BillingClient.BillingResponseCode.OK) {
            sendBillingError("iap:onPurchaseFailure", result);
            return;
        }

        if (purchases != null) {
            for (Purchase purchase : purchases) {
                ReadableMap item = this.buildPurchaseJSON(purchase);
                sendEvent("iap:onPurchaseSuccess", item);
            }
        }
    }

    private ReadableMap buildPurchaseJSON(Purchase purchase) {
        WritableMap item = Arguments.createMap();
        WritableArray productIds = Arguments.createArray();
        for (String sku : purchase.getSkus()) {
            productIds.pushString(sku);
        }
        item.putArray("productIds", productIds);
        item.putString("transactionId", purchase.getOrderId());
        item.putString("transactionDate", String.valueOf(purchase.getPurchaseTime()));
        item.putString("receipt", purchase.getOriginalJson());
        item.putString("purchaseToken", purchase.getPurchaseToken());

        return item;
    }
}
