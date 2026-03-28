package org.dragon.util;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

/**
 * Description:
 * Author: zhz
 * Version: 1.0
 * Create Date Time: 2026/3/14 15:13
 * Update Date Time:
 *
 */
public class GsonUtils {

    private static final Gson gson;

    static {
        gson = new GsonBuilder()
                .enableComplexMapKeySerialization()
                .disableHtmlEscaping()
                .create();
    }

    public static String toJson(Object object) {
        return gson.toJson(object);
    }

    public static <T> T fromJson(String json, Class<T> tClass) {
        return gson.fromJson(json, tClass);
    }

    public static <T> List<T> fromJsonList(String json, Class<T> tClass) {
        Type listType = new TypeToken<List<T>>() {}.getType();
        return gson.fromJson(json, listType);
    }




}
