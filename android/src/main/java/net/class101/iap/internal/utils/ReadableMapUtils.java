package net.class101.iap.internal.utils;

import com.facebook.react.bridge.Dynamic;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableMapKeySetIterator;

public class ReadableMapUtils {
    public static boolean deepEquals(ReadableMap map1, ReadableMap map2) {
        if (map1 == null && map2 == null) {
            return true;
        }
        if (map1 == null || map2 == null) {
            return false;
        }

        ReadableMapKeySetIterator iterator = map1.keySetIterator();
        while (iterator.hasNextKey()) {
            String key = iterator.nextKey();
            if (!map2.hasKey(key) || !deepEquals(map1.getDynamic(key), map2.getDynamic(key))) {
                return false;
            }
        }

        iterator = map2.keySetIterator();
        while (iterator.hasNextKey()) {
            String key = iterator.nextKey();
            if (!map1.hasKey(key)) {
                return false;
            }
        }

        return true;
    }

    private static boolean deepEquals(Dynamic value1, Dynamic value2) {
        if (value1.getType() != value2.getType()) return false;
        switch (value1.getType()) {
            case Null:
                return true;
            case Boolean:
                return value1.asBoolean() == value2.asBoolean();
            case Number:
                return value1.asDouble() == value2.asDouble();
            case String:
                return value1.asString().equals(value2.asString());
            case Map:
                return deepEquals(value1.asMap(), value2.asMap());
            case Array:
                return deepEquals(value1.asArray(), value2.asArray());
            default:
                return false;
        }
    }

    private static boolean deepEquals(ReadableArray array1, ReadableArray array2) {
        if (array1.size() != array2.size()) return false;
        for (int i = 0; i < array1.size(); i++) {
            if (!deepEquals(array1.getDynamic(i), array2.getDynamic(i))) return false;
        }
        return true;
    }
}
