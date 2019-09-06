
# react-native-in-app-purchase
👻 A dead simple In-App Purchase library for React Native

## Getting started

`$ npm install react-native-in-app-purchase --save`

### Mostly automatic installation

`$ react-native link react-native-in-app-purchase`

### Manual installation


#### iOS

1. In XCode, in the project navigator, right click `Libraries` ➜ `Add Files to [your project's name]`
2. Go to `node_modules` ➜ `react-native-in-app-purchase` and add `RNInAppPurchase.xcodeproj`
3. In XCode, in the project navigator, select your project. Add `libRNInAppPurchase.a` to your project's `Build Phases` ➜ `Link Binary With Libraries`
4. Run your project (`Cmd+R`)<

#### Android

1. Open up `android/app/src/main/java/[...]/MainActivity.java`
  - Add `import net.yeoubi.iap.RNInAppPurchasePackage;` to the imports at the top of the file
  - Add `new RNInAppPurchasePackage()` to the list returned by the `getPackages()` method
2. Append the following lines to `android/settings.gradle`:
  	```
  	include ':react-native-in-app-purchase'
  	project(':react-native-in-app-purchase').projectDir = new File(rootProject.projectDir, 	'../node_modules/react-native-in-app-purchase/android')
  	```
3. Insert the following lines inside the dependencies block in `android/app/build.gradle`:
  	```
      compile project(':react-native-in-app-purchase')
  	```

## Usage

⚠️ This project is still under development.
  
