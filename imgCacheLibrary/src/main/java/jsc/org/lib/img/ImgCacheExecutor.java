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
import android.text.TextUtils;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class ImgCacheExecutor {
    String cacheName;
    String type = ImgCache.TYPE_LOCAL;
    String url;
    int width;
    int height;
    String key;
    boolean sample;
    boolean loadFromMemoryCache = true;
    boolean loadFromDiskCache = true;

    public static void clearMemoryCache(String name) {
        ImgCacheManager.clearMemoryCache(name);
    }

    public static void clearDiskCache(Context context, String name) {
        ImgCacheManager.clearDiskCache(context, name);
    }

    public static ImgCacheExecutor with(String name) {
        return new ImgCacheExecutor(name);
    }

    private ImgCacheExecutor(String name) {
        this.cacheName = name;
    }

    public ImgCacheExecutor localSource() {
        this.type = ImgCache.TYPE_LOCAL;
        return this;
    }

    public ImgCacheExecutor netSource() {
        this.type = ImgCache.TYPE_NET;
        return this;
    }

    public ImgCacheExecutor url(String pathName) {
        this.url = pathName;
        return this;
    }

    public ImgCacheExecutor size(int width, int height) {
        this.width = width;
        this.height = height;
        return this;
    }

    public ImgCacheExecutor loadFromMemoryCache(boolean loadMemoryCache) {
        this.loadFromMemoryCache = loadMemoryCache;
        return this;
    }

    public ImgCacheExecutor loadFromDiskCache(boolean loadDiskCache) {
        this.loadFromDiskCache = loadDiskCache;
        return this;
    }

    public ImgCacheExecutor build() {
        if (TextUtils.isEmpty(type)
                || TextUtils.isEmpty(url))
            throw new IllegalArgumentException("\"type\" or \"pathName\" is empty.");
        sample = width > 0 && height > 0;
        String val = sample ? url + "|" + width + "|" + height : url;
        key = calculateMD5(val);
        return this;
    }

    public String shortUrl() {
        return sample ? calculateMD5(url) : key;
    }

    public void into(ImageView view) {
        ImgCache imgCache = ImgCacheManager.imgCache(cacheName);
        imgCache.into(view, this);
    }

    /**
     * 字符串MD5加密
     */
    private String calculateMD5(String string) {
        if (TextUtils.isEmpty(string)) {
            return "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] bytes = digest.digest(string.getBytes());
            StringBuilder result = new StringBuilder();
            for (byte b : bytes) {
                String temp = Integer.toHexString(b & 0xff);
                if (temp.length() == 1) {
                    temp = "0" + temp;
                }
                result.append(temp);
            }
            return result.toString().toUpperCase();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }
}