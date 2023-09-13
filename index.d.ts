declare module "@class101/react-native-in-app-purchase" {
  interface Product {
    productId: string;
    planId?: string;
    offerId?: string;
    title: string;
    description: string;
    price: string;
    currency: string;
  }

  interface Purchase {
    productIds: string[];
    transactionId: string;
    transactionDate: string;
    receipt: string;
    purchaseToken: string;
  }

  interface IAPError {
    type: {
      FETCH_PRODUCTS: "FETCH_PRODUCTS";
      PURCHASE: "PURCHASE";
      CONNECTION: "CONNECTION";
    };
    code?: number;
    message: string;
  }

  const InAppPurchaseErrorType: {
    FETCH_PRODUCTS: "FETCH_PRODUCTS";
    PURCHASE: "PURCHASE";
    CONNECTION: "CONNECTION";
  };

  const InAppPurchaseErrorCode: {
    USER_CANCELED: number;
  };

  enum InAppPurchaseProductType {
    Subscription = "subs",
    InApp = "inapp"
  }

  function onFetchProducts(listener: (products: Product[]) => void): void;

  function onPurchase(listener: (purchase: Purchase) => void): void;

  function onAlternativeBillingFlow(listener: (token: string) => void): void;

  function onError(listener: (error: IAPError) => void): void;

  function clear(): void;

  function configure(): Promise<boolean>;

  function fetchReceipt(): Promise<string | undefined>;

  function fetchProducts(products: {
    id: string;
    type: InAppPurchaseProductType;
    planId?: string;
    offerId?: string;
  }[]): void;

  function purchase(
    productId: string,
    extras: {
      planId?: string;
      offerId?: string;
      userId?: string;
      keyIdentifier?: string;
      nonce?: string;
      signature?: string;
      timestamp?: number;
      originalPurchaseToken?: string;
      obfuscatedAccountId?: string;
      obfuscatedProfileId?: string;
    }
  ): void;

  function finalize(purchase: Purchase, isConsumable: boolean): Promise<void>;

  function flush(): Promise<Purchase[]>;
}
