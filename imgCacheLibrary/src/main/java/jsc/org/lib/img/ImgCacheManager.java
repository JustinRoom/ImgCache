/*
 * Copyright (c) 2023.
 *
 * Author: JiangShiCheng
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jsc.org.lib.img;

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

public final class ImgCacheManager {

    private final static Map<String, ImgCache> map = new HashMap<>(16);

    public static void initImgCache(String name) {
        ImgCache cache = map.get(name);
        if (cache == null) {
            cache = new ImgCache(name);
            map.put(name, cache);
        }
    }

    public static ImgCache defaultImgCache() {
        return imgCache("mDefaultImgCache");
    }

    @NonNull
    public static ImgCache imgCache(String name) {
        ImgCache cache = map.get(name);
        if (cache == null) {
            cache = new ImgCache(name);
            map.put(name, cache);
        }
        return cache;
    }

    /**
     * See {@link #clearMemoryCache(String, boolean)}.
     * @param name
     */
    public static void clearMemoryCache(String name) {
        clearMemoryCache(name, true);
    }

    public static void clearMemoryCache(String name, boolean recycleBitmap) {
        if (isEmpty(name)) {
            //clear all
            for (ImgCache cache : map.values()) {
                cache.clearMemoryCache(recycleBitmap);
            }
            return;
        }
        //clear special one
        ImgCache cache = map.get(name);
        if (cache != null) {
            cache.clearMemoryCache(recycleBitmap);
        }
    }

    public static void clearMemoryCache(@NonNull String name, @NonNull String key, boolean recycleBitmap) {
        ImgCache cache = map.get(name);
        if (cache != null) {
            cache.clearMemoryCache(key, recycleBitmap);
        }
    }

    public static void clearDiskCache(Context context, String name) {
        if (isEmpty(name)) {
            //clear all
            for (ImgCache cache : map.values()) {
                cache.clearDiskCache(context);
            }
            return;
        }
        //clear special one
        ImgCache cache = map.get(name);
        if (cache != null) {
            cache.clearDiskCache(context);
        }
    }

    private static boolean isEmpty(String str) {
        return str == null || str.length() == 0;
    }
}
