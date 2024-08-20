package net.class101.iap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;
import com.android.billingclient.api.UserChoiceBillingListener;
import com.android.billingclient.api.UserChoiceDetails;
import com.facebook.common.internal.ImmutableList;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeArray;

import net.class101.iap.internal.utils.ReadableMapUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class NativeInAppPurchaseModule extends NativeInAppPurchaseModuleSpec implements PurchasesUpdatedListener, UserChoiceBillingListener {
    private final ReactApplicationContext reactContext;

    private final Map<String, ProductDetails> productDetailsMap;

    private BillingClient client;
    private ReadableMap appliedConfig;

    private Callback fetchProductsListener;
    private Callback purchaseListener;
    private Callback errorListener;
    private Callback alternativeBillingFlowListener;

    public NativeInAppPurchaseModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        this.productDetailsMap = new HashMap<>();
    }

    @Override
    public void configure(@Nullable ReadableMap config, Promise promise) {
        if (client != null) {
            if (client.isReady() && !hasChangedOptions(appliedConfig, config)) {
                promise.resolve(true);
                return;
            }

            client.endConnection();
            client = null;
        }
        this.appliedConfig = config;

        BillingClient.Builder builder = BillingClient.newBuilder(reactContext)
                .setListener(this)
                .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build());

        var isAlternativeBillingEnable = Optional.ofNullable(config).map(it -> it.getBoolean("isAlternativeBillingEnable")).orElse(false);

        if (isAlternativeBillingEnable) {
            builder.enableUserChoiceBilling(this);
        }

        client = builder.build();

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

    @Override
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
                    sendBillingError("FETCH_PRODUCTS", billingResult);
                    return;
                }

                for (int i = 0; i < products.size(); i++) {
                    ReadableMap product = products.getMap(i);

                    String productId = product.getString("id");
                    String productType = product.getString("type");
                    String planId = product.getString("planId");
                    String offerId = product.getString("offerId");

                    if (productId == null || productType == null) {
                        continue;
                    }

                    if (!productType.equals(BillingClient.ProductType.SUBS) && !productType.equals(BillingClient.ProductType.INAPP)) {
                        continue;
                    }

                    Optional<ProductDetails> productDetails = productDetailsList.stream().filter(details -> details.getProductId().equals(productId)).findFirst();
                    if (!productDetails.isPresent()) {
                        continue;
                    }

                    if (productType.equals(BillingClient.ProductType.INAPP)) {
                        WritableMap item = Arguments.createMap();
                        item.putString("productId", productDetails.get().getProductId());
                        item.putString("offerId", offerId);
                        item.putString("title", productDetails.get().getTitle());
                        item.putString("description", productDetails.get().getDescription());

                        ProductDetails.OneTimePurchaseOfferDetails offerDetails = productDetails.get().getOneTimePurchaseOfferDetails();
                        if (offerDetails != null) {
                            item.putString("price", offerDetails.getFormattedPrice());
                            item.putString("currency", offerDetails.getPriceCurrencyCode());
                        }

                        items.pushMap(item);
                        continue;
                    }

                    List<ProductDetails.SubscriptionOfferDetails> offerDetailsList = productDetails.get().getSubscriptionOfferDetails();

                    if (offerDetailsList == null || offerDetailsList.isEmpty()) {
                        continue;
                    }

                    Optional<ProductDetails.SubscriptionOfferDetails> offerDetails = offerDetailsList.stream()
                            .filter(details -> planId == null || details.getBasePlanId().equals(planId))
                            .filter(details -> offerId == null || (details.getOfferId() != null && details.getOfferId().equals(offerId)))
                            .findFirst();

                    if (offerDetails.isPresent()) {
                        WritableMap item = Arguments.createMap();
                        item.putString("productId", productDetails.get().getProductId());
                        item.putString("planId", planId);
                        item.putString("offerId", offerId);
                        item.putString("title", productDetails.get().getTitle());
                        item.putString("description", productDetails.get().getDescription());
                        item.putString("price", offerDetails.get().getPricingPhases().getPricingPhaseList().get(0).getFormattedPrice());
                        item.putString("currency", offerDetails.get().getPricingPhases().getPricingPhaseList().get(0).getPriceCurrencyCode());

                        items.pushMap(item);
                    }
                }

                for (ProductDetails productDetails : productDetailsList) {
                    productDetailsMap.put(productDetails.getProductId(), productDetails);
                }

                if (fetchProductsListener != null) {
                    fetchProductsListener.invoke(items);
                }
            });
        });
    }

    @Override
    public void flush(Promise promise) {
        tryConnect(() -> client.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(),
                (inAppResult, inAppPurchases) -> {
                    if (inAppResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                        promise.reject("flush", inAppResult.getDebugMessage());
                        return;
                    }
                    List<Purchase> purchaseList = new ArrayList<>(inAppPurchases);

                    client.queryPurchasesAsync(
                            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build(),
                            (subscriptionResult, subscriptionPurchases) -> {
                                if (subscriptionResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                                    promise.reject("flush", subscriptionResult.getDebugMessage());
                                    return;
                                }

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

    @Override
    public void purchase(String productId, @Nullable ReadableMap args) {
        tryConnect(() -> {
            if (getCurrentActivity() == null) {
                return;
            }

            ProductDetails productDetails = productDetailsMap.get(productId);
            String obfuscatedAccountId = args != null ? args.getString("obfuscatedAccountId") : null;
            String obfuscatedProfileId = args != null ? args.getString("obfuscatedProfileId") : null;
            String originalPurchaseToken = args != null ? args.getString("originalPurchaseToken") : null;

            String planId = args != null ? args.getString("planId") : null;
            String offerId = args != null ? args.getString("offerId") : null;

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
            List<ProductDetails.SubscriptionOfferDetails> offerDetailsList = productDetails.getSubscriptionOfferDetails();

            productDetailsParamsBuilder.setProductDetails(productDetails);

            if (offerDetailsList != null) {
                offerDetailsList.stream()
                        .filter(details -> planId == null || details.getBasePlanId().equals(planId))
                        .filter(details -> offerId == null || (details.getOfferId() != null && details.getOfferId().equals(offerId)))
                        .findFirst()
                        .ifPresent(details -> productDetailsParamsBuilder.setOfferToken(details.getOfferToken()));
            }

            ImmutableList<BillingFlowParams.ProductDetailsParams> productDetailsParamsList = ImmutableList.of(productDetailsParamsBuilder.build());
            BillingFlowParams params = builder.setProductDetailsParamsList(productDetailsParamsList).build();

            client.launchBillingFlow(getCurrentActivity(), params);
        });
    }

    @Override
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

    @Override
    public void fetchReceipt(Promise promise) {
        promise.reject("fetchReceipt", "Not implemented");
    }

    @Override
    public void onFetchProducts(Callback listener) {
        this.fetchProductsListener = listener;
    }

    @Override
    public void onPurchase(Callback listener) {
        this.purchaseListener = listener;
    }

    @Override
    public void onAlternativeBillingFlow(Callback listener) {
        this.alternativeBillingFlowListener = listener;
    }

    @Override
    public void onError(Callback listener) {
        this.errorListener = listener;
    }

    @Override
    public void clear() {
        this.fetchProductsListener = null;
        this.purchaseListener = null;
        this.errorListener = null;
        this.alternativeBillingFlowListener = null;
    }

    @Override
    public void onPurchasesUpdated(@NonNull BillingResult billingResult, @Nullable List<Purchase> purchases) {
        if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
            sendBillingError("PURCHASE", billingResult);
            return;
        }

        if (purchases != null) {
            for (Purchase purchase : purchases) {
                ReadableMap item = this.buildPurchaseJSON(purchase);
                this.purchaseListener.invoke(item);
            }
        }
    }

    @Override
    public void userSelectedAlternativeBilling(@NonNull UserChoiceDetails userChoiceDetails) {
        if (this.alternativeBillingFlowListener == null) {
            return;
        }

        this.alternativeBillingFlowListener.invoke(userChoiceDetails.getExternalTransactionToken());
    }

    private boolean hasChangedOptions(ReadableMap appliedConfig, ReadableMap newConfig) {
        return !ReadableMapUtils.deepEquals(appliedConfig, newConfig);
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
                    sendBillingError("CONNECTION", result);
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
            }
        });
    }

    private void sendBillingError(String type, BillingResult result) {
        WritableMap exception = Arguments.createMap();
        exception.putString("type", type);
        exception.putInt("code", result.getResponseCode());
        exception.putString("message", result.getDebugMessage());

        if (errorListener != null) {
            errorListener.invoke(exception);
        }
    }

    private ReadableMap buildPurchaseJSON(Purchase purchase) {
        WritableMap item = Arguments.createMap();
        WritableArray productIds = Arguments.createArray();
        for (String sku : purchase.getProducts()) {
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

