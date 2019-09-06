declare module 'react-native-in-app-purchase' {
  interface Product {
    productId: string;
    price: string;
    currency: string;
    title: string;
    description: string;
    iconUrl: string;
  }

  interface Purchase {
    productId: string;
    transactionId: string;
    transactionDate: string;
    receipt: string;
    purchaseToken: string;
  }

  interface IAPError {
    type: {
      FETCH_PRODUCTS: 'FETCH_PRODUCTS';
      PURCHASE: 'PURCHASE';
      CONNECTION: 'CONNECTION';
    },
    code?: number;
    message: string;
  }

  function onFetchProducts(listener: (products: Product[]) => void): void;
  
  function onPurchase(listener: (purchase: Purchase) => void): void;

  function onError(listener: (error: IAPError) => void): void;

  function configure(): Promise<boolean>;

  function fetchProducts(productIds: string[]): void;

  function purchase(productId: string): void;

  function finalize(purchase: Purchase): Promise<void>;

  function flush(): Promise<Purchase[]>;
}
