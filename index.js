import {
  NativeModules,
  NativeEventEmitter,
  DeviceEventEmitter,
  Platform,
} from "react-native";

const { RNInAppPurchase } = NativeModules;

export const InAppPurchaseErrorType = {
  FETCH_PRODUCTS: "FETCH_PRODUCTS",
  PURCHASE: "PURCHASE",
  CONNECTION: "CONNECTION",
};

export const InAppPurchaseErrorCode = {
  USER_CANCELED: Platform.select({
    ios: "2",
    android: "1",
  }),
};

export const InAppPurchaseProductType = {
  Subscription: "subs",
  InApp: "inapp",
};

const addListener = (event, listener) =>
  Platform.select({
    ios: new NativeEventEmitter(RNInAppPurchase),
    android: DeviceEventEmitter,
  }).addListener(event, listener);

const removeAllListeners = (event) =>
  Platform.select({
    ios: new NativeEventEmitter(RNInAppPurchase),
    android: DeviceEventEmitter,
  }).removeAllListeners(event);

const onFetchProducts = (listener) =>
  addListener("iap:onFetchProductsSuccess", listener);

const fetchReceipt = () => {
  if (Platform.OS === "ios") {
    return RNInAppPurchase.fetchReceipt();
  } else {
    return Promise.resolve(undefined);
  }
};

const onPurchase = (listener) => addListener("iap:onPurchaseSuccess", listener);

const onAlternativeBillingFlow = (listener) => addListener("iap:onAlternativeBillingFlow", listener);

const onError = (listener) => {
  if (Platform.OS === "android") {
    addListener("iap:onConnectionFailure", (e) =>
      listener(Object.assign(e, { type: InAppPurchaseErrorType.CONNECTION }))
    );
  }
  addListener("iap:onFetchProductsFailure", (e) =>
    listener(Object.assign(e, { type: InAppPurchaseErrorType.FETCH_PRODUCTS }))
  );
  addListener("iap:onPurchaseFailure", (e) =>
    listener(Object.assign(e, { type: InAppPurchaseErrorType.PURCHASE }))
  );
};

const clear = () => {
  removeAllListeners("iap:onFetchProductsSuccess");
  removeAllListeners("iap:onPurchaseSuccess");
  removeAllListeners("iap:onAlternativeBillingFlow");
  removeAllListeners("iap:onFetchProductsFailure");
  removeAllListeners("iap:onPurchaseFailure");
  if (Platform.OS === "android") {
    removeAllListeners("iap:onConnectionFailure");
  }
};

const configure = (options) => {
  return Platform.OS === "android"
    ? RNInAppPurchase.configure(options)
    : RNInAppPurchase.configure();
}

const purchase = (productId, { planId, offerId, userId, keyIdentifier, nonce, signature, timestamp, originalPurchaseToken, obfuscatedAccountId, obfuscatedProfileId }) => {
  if (Platform.OS === "android") {
    RNInAppPurchase.purchase(
      productId,
      planId || null,
      offerId || null,
      originalPurchaseToken || null,
      obfuscatedAccountId || null,
      obfuscatedProfileId || null
    );
  } else {
    RNInAppPurchase.purchase(productId, offerId, userId, keyIdentifier, nonce, signature, timestamp);
  }
};

const finalize = (purchase, isConsumable) => {
  return Platform.OS === "android"
    ? RNInAppPurchase.finalize(purchase, isConsumable)
    : RNInAppPurchase.finalize(purchase);
};

export default {
  configure,
  fetchProducts: RNInAppPurchase.fetchProducts,
  flush: RNInAppPurchase.flush,
  purchase,
  finalize,
  fetchReceipt,
  onFetchProducts,
  onPurchase,
  onAlternativeBillingFlow,
  onError,
  clear,
};
