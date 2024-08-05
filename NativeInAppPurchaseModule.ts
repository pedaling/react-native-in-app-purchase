import { TurboModule, TurboModuleRegistry } from "react-native";
import type { 
  Int32,
} from 'react-native/Libraries/Types/CodegenTypes';

type FetchProductInput = {
  id: string;

  // Android only
  type?: 'inapp' | 'subs';
  planId?: string;
  offerId?: string;
}

type Purchase = {
  productIds: string[];
  transactionId: string;
  transactionDate: string;
  receipt: string;
  purchaseToken: string;
}

type PurchaseArgs = {
  // Android
  planId?: string,
  offerId?: string,
  originalPurchaseToken?: string,
  obfuscatedAccountId?: string,
  obfuscatedProfileId?: string,

  // iOS
  userId?: string,
  keyIdentifier?: string,
  nonce?: string,
  signature?: string,
  timestamp?: Int32,
}

export interface Spec extends TurboModule {
  readonly getConstants: () => {};
  configure:(config?: {
    isAlternativeBillingEnable?: boolean,
  }) => Promise<boolean>;
  fetchProducts: (products: FetchProductInput[]) => void;
  flush: () => Promise<Purchase[]>;
  purchase: (productId: string, args: PurchaseArgs ) => void;
  finalize: (purchase: Purchase, isConsumable: boolean) => Promise<void>;
  fetchReceipt: () => Promise<string | undefined>;
  onFetchProducts: (listener: (products: { id: string, productId: string, type: string, planId: string, offerId: string, title: string, 
    description: string, price: string, currency: string }[]) => void) => void;
  onPurchase: (listener: (purchase: {
    productIds: string[],
    transactionId: string,
    transactionDate: string,
    receipt: string,
    purchaseToken: string,
  }) => void) => void,
  onAlternativeBillingFlow: (listener: (token: string) => void) => void;
  onError: (listener: ( exception: {code: number, message: string }) => void) => void;
  clear: () => void;
}

export default TurboModuleRegistry.get<Spec>("NativeInAppPurchase");
