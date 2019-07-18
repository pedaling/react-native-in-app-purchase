
package net.yeoubi.iap;

import androidx.annotation.Nullable;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;

import java.util.List;

public class RNInAppPurchaseModule extends ReactContextBaseJavaModule implements PurchasesUpdatedListener {

  private final ReactApplicationContext reactContext;

  private BillingClient client;

  public RNInAppPurchaseModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
  }

  @Override
  public String getName() {
    return "RNInAppPurchase";
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
  public void getProductList() {}

  @ReactMethod
  public void purchase(String id) {}

  @ReactMethod
  public void restorePurchases() {}

  @Override
  public void onPurchasesUpdated(BillingResult billingResult, @Nullable List<Purchase> purchases) {

  }
}
