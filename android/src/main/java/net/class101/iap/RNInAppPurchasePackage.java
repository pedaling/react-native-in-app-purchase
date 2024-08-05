package net.class101.iap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

import com.facebook.react.TurboReactPackage;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.module.model.ReactModuleInfo;
import com.facebook.react.module.model.ReactModuleInfoProvider;

public class RNInAppPurchasePackage extends TurboReactPackage {

    @Nullable
    @Override
    public NativeModule getModule(@NonNull String name, @NonNull ReactApplicationContext reactApplicationContext) {
        if (name.equals(NativeInAppPurchaseModule.NAME)) {
            if (BuildConfig.IS_NEW_ARCHITECTURE_ENABLED) {
                return new NativeInAppPurchaseModule(reactApplicationContext);
            }
            return new RNInAppPurchaseModule(reactApplicationContext);
        } else {
            return null;
        }
    }

    @Override
    public ReactModuleInfoProvider getReactModuleInfoProvider() {
        return () -> {
            final Map<String, ReactModuleInfo> moduleInfos = new HashMap<>();
            boolean isTurboModule = BuildConfig.IS_NEW_ARCHITECTURE_ENABLED;

            moduleInfos.put(
                    NativeInAppPurchaseModule.NAME,
                    new ReactModuleInfo(
                            NativeInAppPurchaseModule.NAME,
                            NativeInAppPurchaseModule.NAME,
                            false, // canOverrideExistingModule
                            false, // needsEagerInit
                            false, // isCxxModule
                            isTurboModule
                    ));
            return moduleInfos;
        };
    }
}