import {
  NativeModules,
  NativeEventEmitter,
  DeviceEventEmitter,
  Platform,
} from 'react-native';

const { RNInAppPurchase } = NativeModules;

const addListener = (event, listener) => Platform.select({
  ios: new NativeEventEmitter(RNInAppPurchase),
  android: DeviceEventEmitter,
}).addListener(event, listener);

const onProductListSuccess = e => addListener('iap:onProductListSuccess', e);

const onProductListFailure = e => addListener('iap:onProductListFailure', e);

const onPurchaseSuccess = e => addListener('iap:onPurchaseSuccess', e);

const onPurchaseFailure = e => addListener('iap:onPurchaseFailure', e);

export default {
  configure: RNInAppPurchase.configure,
  fetchProducts: RNInAppPurchase.fetchProducts,
  purchase: RNInAppPurchase.purchase,
  restore: RNInAppPurchase.restore,
  finalize: RNInAppPurchase.finalize,
  onProductListSuccess,
  onProductListFailure,
  onPurchaseSuccess,
  onPurchaseFailure,
}
