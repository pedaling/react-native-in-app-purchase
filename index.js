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
}

const clear = () => {
  removeAllListeners('iap:onFetchProductsSuccess')
  removeAllListeners('iap:onPurchaseSuccess')
  removeAllListeners('iap:onFetchProductsFailure')
  removeAllListeners('iap:onPurchaseFailure')
  if (Platform.OS === 'android') {
    removeAllListeners('iap:onConnectionFailure')
  }
}

export default {
  configure: RNInAppPurchase.configure,
  fetchProducts: RNInAppPurchase.fetchProducts,
  purchase: RNInAppPurchase.purchase,
  finalize: RNInAppPurchase.finalize,
  flush: RNInAppPurchase.flush,
  onFetchProducts,
  onPurchase,
  onError,
  clear,
  ERROR,
}
