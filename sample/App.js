import React, { Component } from 'react';
import {
  Alert,
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
} from 'react-native';
import InAppPurchase from '@class101/react-native-in-app-purchase';

const PRODUCT_IDS = [
  'rniap.sample.normal',
  'rniap.sample.consumable',
  'rniap.sample.subscribe',
];

export default class App extends Component {
  state = {
    products: [],
  };

  componentDidMount() {
    // Set event handlers
    InAppPurchase.onFetchProducts(this.onFetchProducts);
    InAppPurchase.onPurchase(this.onPurchase);
    InAppPurchase.onError(this.onError);

    // Configure and fetch products
    InAppPurchase.configure().then(() => {
      InAppPurchase.fetchProducts(PRODUCT_IDS);
    });
  }

  onPurchase = (purchase) => {
    // Validate payment on your backend server with purchase object.
    setTimeout(() => {
      // Complete the purchase flow by calling finalize function.
      InAppPurchase.finalize(purchase, purchase.productId === 'rniap.sample.consumable').then(() => {
        Alert.alert('In App Purchase', 'Purchase Succeed!');
      });
    });
  }

  onFetchProducts = (products) => {
    console.log(products);
    this.setState({ products });
  }

  onError = (e) => {
    console.log(e);
  }

  flush = () => {
    // If the validation - finalization process is not performed properly, (ex: Internet connection)
    // call this function to fetch pending purchases, and restart the validation process.
    InAppPurchase.flush().then((purchases) => {
      console.log(purchases);
      purchases.forEach(this.onPurchase);
    });
  }

  renderItem = (item) => (
    <TouchableOpacity
      key={item.title}
      activeOpacity={0.8}
      onPress={() => InAppPurchase.purchase(item.productId)}
      style={styles.item}
    >
      <Text style={styles.title}>
        {item.title}
      </Text>
      <View style={styles.priceTag}>
        <Text style={styles.priceText}>
          {item.currency} {item.price}
        </Text>
      </View>
    </TouchableOpacity>
  )

  render() {
    return (
      <View style={styles.container}>
        {this.state.products.map(this.renderItem)}
        <TouchableOpacity
          activeOpacity={0.8}
          onPress={this.flush}
          style={[styles.item, styles.button]}
        >
          <Text style={styles.text}>
            Flush uncompleted purchases
          </Text>
        </TouchableOpacity>
      </View>
    );
  }
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    backgroundColor: '#FFF',
  },
  item: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 24,
    marginHorizontal: 24,
    paddingVertical: 20,
    paddingHorizontal: 16,
    backgroundColor: '#F2F4F9',
  },
  title: {
    fontSize: 16,
    color: '#191919',
  },
  priceTag: {
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 2,
    backgroundColor: '#2D2D2D'
  },
  priceText: {
    fontSize: 12,
    color: '#FAFAFA',
  },
  button: {
    marginTop: 16,
    marginBottom: 0,
    justifyContent: 'center',
    backgroundColor: '#2D2D2D',
    borderRadius: 32,
  },
  text: {
    fontSize: 16,
    color: '#FAFAFA',
  },
});
