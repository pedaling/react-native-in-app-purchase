package net.yeoubi.iap;

import androidx.annotation.Nullable;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RNInAppPurchaseModule extends ReactContextBaseJavaModule implements PurchasesUpdatedListener {

    private final ReactApplicationContext reactContext;

    private BillingClient client;

    private Map<String, SkuDetails> skuDetailsMap;

    public RNInAppPurchaseModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        this.skuDetailsMap = new HashMap<>();
    }

    @Override
    public String getName() {
        return "RNInAppPurchase";
    }

    @Nullable
    @Override
    public Map<String, Object> getConstants() {
        final Map<String, Object> constants = new HashMap<>();

        constants.put("EVENT_GET_PRODUCT_LIST_SUCCESS", "EVENT:GET_PRODUCT_LIST_SUCCESS");
        constants.put("EVENT_GET_PRODUCT_LIST_FAILURE", "EVENT:GET_PRODUCT_LIST_FAILURE");
        constants.put("EVENT_PURCHASE_SUCCESS", "EVENT:PURCHASE_SUCCESS");
        constants.put("EVENT_PURCHASE_FAILURE", "EVENT:PURCHASE_FAILURE");
        constants.put("EVENT_FINALIZE_SUCCESS", "EVENT:FINALIZE_SUCCESS");
        constants.put("EVENT_FINALIZE_FAILURE", "EVENT:FINALIZE_FAILURE");

        return constants;
    }

    @ReactMethod
    public void init(final Promise promise) {
        client = BillingClient.newBuilder(reactContext).enablePendingPurchases().setListener(this).build();
        client.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    promise.resolve(true);
                }

                promise.reject("init", "Billing service setup failed with code " + billingResult.getResponseCode());
            }

            @Override
            public void onBillingServiceDisconnected() {
                promise.reject("init", "Billing service disconnected");
            }
        });
    }

    @ReactMethod
    public void getProductList(ReadableArray productIds) {
        final List<String> skuList = new ArrayList<>();

        for (int i = 0; i < productIds.size(); i++) {
            skuList.add(productIds.getString(i));
        }

        final SkuDetailsParams inAppParams = SkuDetailsParams.newBuilder()
            .setSkusList(skuList)
            .setType(BillingClient.SkuType.INAPP).build();

        final SkuDetailsParams subscribeParams = SkuDetailsParams.newBuilder()
            .setSkusList(skuList)
            .setType(BillingClient.SkuType.SUBS).build();

        final WritableArray items = new WritableNativeArray();

        client.querySkuDetailsAsync(inAppParams, new SkuDetailsResponseListener() {
            @Override
            public void onSkuDetailsResponse(BillingResult result, List<SkuDetails> skuDetailsList) {
                if (result.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                    sendBillingError("EVENT:GET_PRODUCT_LIST_FAILURE", result);
                    return;
                }

                for (SkuDetails skuDetails : skuDetailsList) {
                    WritableMap item = Arguments.createMap();
                    item.putString("productId", skuDetails.getSku());
                    item.putString("price", skuDetails.getPrice());
                    item.putString("currency", skuDetails.getPriceCurrencyCode());
                    item.putString("title", skuDetails.getTitle());
                    item.putString("description", skuDetails.getDescription());
                    item.putString("iconUrl", skuDetails.getIconUrl());
                    items.pushMap(item);
                    skuDetailsMap.put(skuDetails.getSku(), skuDetails);
                }

                client.querySkuDetailsAsync(subscribeParams, new SkuDetailsResponseListener() {
                    @Override
                    public void onSkuDetailsResponse(BillingResult result, List<SkuDetails> skuDetailsList) {
                        if (result.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                            sendBillingError("EVENT:GET_PRODUCT_LIST_FAILURE", result);
                            return;
                        }

                        for (SkuDetails skuDetails : skuDetailsList) {
                            WritableMap item = Arguments.createMap();
                            item.putString("productId", skuDetails.getSku());
                            item.putString("price", skuDetails.getPrice());
                            item.putString("currency", skuDetails.getPriceCurrencyCode());
                            item.putString("title", skuDetails.getTitle());
                            item.putString("description", skuDetails.getDescription());
                            item.putString("iconUrl", skuDetails.getIconUrl());
                            items.pushMap(item);
                            skuDetailsMap.put(skuDetails.getSku(), skuDetails);
                        }

                        sendEvent("EVENT:GET_PRODUCT_LIST_SUCCESS", items);
                    }
                });
            }
        });
    }

    @ReactMethod
    public void purchase(String productId, String oldProductId) {
        BillingFlowParams.Builder builder = BillingFlowParams.newBuilder();

        if (oldProductId != null) {
            builder.setOldSku(oldProductId);
        }

        BillingFlowParams params = builder.setSkuDetails(skuDetailsMap.get(productId)).build();

        client.launchBillingFlow(getCurrentActivity(), params);
    }

    @ReactMethod
    public void finalize(ReadableMap purchase, boolean isConsumable) {
        String token = purchase.getString("purchaseToken");

        if (isConsumable) {
            ConsumeParams params = ConsumeParams.newBuilder()
                .setPurchaseToken(token)
                .build();

            client.consumeAsync(params, new ConsumeResponseListener() {
                @Override
                public void onConsumeResponse(BillingResult result, String purchaseToken) {
                    if (result.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                        sendBillingError("EVENT:FINALIZE_FAILURE", result);
                        return;
                    }

                    WritableMap event = Arguments.createMap();
                    event.putString("message", result.getDebugMessage());

                    sendEvent("EVENT:FINALIZE_SUCCESS", event);
                }
            });
            return;
        }

        AcknowledgePurchaseParams acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(token)
            .build();

        client.acknowledgePurchase(acknowledgePurchaseParams, new AcknowledgePurchaseResponseListener() {
            @Override
            public void onAcknowledgePurchaseResponse(BillingResult result) {
                if (result.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                    sendBillingError("EVENT:FINALIZE_FAILURE", result);
                }

                WritableMap event = Arguments.createMap();
                event.putString("message", result.getDebugMessage());

                sendEvent("EVENT:FINALIZE_SUCCESS", event);
            }
        });
    }

    @ReactMethod
    public void restorePurchases() {
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

    @Override
    public void onPurchasesUpdated(BillingResult result, @Nullable List<Purchase> purchases) {
        if (result.getResponseCode() != BillingClient.BillingResponseCode.OK) {
            sendBillingError("EVENT:PURCHASE_FAILURE", result);
            return;
        }

        if (purchases != null) {
            for (Purchase purchase : purchases) {
                WritableMap item = Arguments.createMap();
                item.putString("productId", purchase.getSku());
                item.putString("transactionId", purchase.getOrderId());
                item.putString("transactionDate", String.valueOf(purchase.getPurchaseTime()));
                item.putString("receipt", purchase.getOriginalJson());
                item.putString("purchaseToken", purchase.getPurchaseToken());

                sendEvent("EVENT:PURCHASE_SUCCESS", item);
            }
        }
    }
}
