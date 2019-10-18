import {
  NativeModules,
  NativeEventEmitter,
  DeviceEventEmitter,
  Platform,
} from 'react-native';

const { RNInAppPurchase } = NativeModules;

const ERROR = {
  FETCH_PRODUCTS: 'FETCH_PRODUCTS',
  PURCHASE: 'PURCHASE',
  CONNECTION: 'CONNECTION',
};

const addListener = (event, listener) => Platform.select({
  ios: new NativeEventEmitter(RNInAppPurchase),
  android: DeviceEventEmitter,
}).addListener(event, listener);

const removeAllListeners = (event) => Platform.select({
  ios: new NativeEventEmitter(RNInAppPurchase),
  android: DeviceEventEmitter,
}).removeAllListeners(event);

const onFetchProducts = listener => addListener('iap:onFetchProductsSuccess', listener);

const onPurchase = listener => addListener('iap:onPurchaseSuccess', listener);

const onError = (listener) => {
  if (Platform.OS === 'android') {
    addListener('iap:onConnectionFailure', e => listener(Object.assign(e, { type: ERROR.CONNECTION })));
  }
  addListener('iap:onFetchProductsFailure', e => listener(Object.assign(e, { type: ERROR.FETCH_PRODUCTS })));
  addListener('iap:onPurchaseFailure', e => listener(Object.assign(e, { type: ERROR.PURCHASE })));
};

const clear = () => {
  removeAllListeners('iap:onFetchProductsSuccess');
  removeAllListeners('iap:onPurchaseSuccess');
  removeAllListeners('iap:onFetchProductsFailure');
  removeAllListeners('iap:onPurchaseFailure');
  if (Platform.OS === 'android') {
    removeAllListeners('iap:onConnectionFailure');
  }
};

const purchase = (productId, oldProductId) => {
  if (Platform.OS === 'android') {
    RNInAppPurchase.purchase(productId, oldProductId || null);
  } else {
    RNInAppPurchase.purchase(productId);
  }
};

const finalize = (purchase, isConsumable) => {
  return Platform.OS === 'android'
    ? RNInAppPurchase.finalize(purchase, isConsumable)
    : RNInAppPurchase.finalize(purchase);
}

export default {
  configure: RNInAppPurchase.configure,
  fetchProducts: RNInAppPurchase.fetchProducts,
  flush: RNInAppPurchase.flush,
  purchase,
  finalize,
  onFetchProducts,
  onPurchase,
  onError,
  clear,
  ERROR,
}
