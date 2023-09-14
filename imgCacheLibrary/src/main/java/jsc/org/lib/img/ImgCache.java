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
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Base64;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import java.io.File;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class ImgCache {

    private final static String TAG = "ImgCache";
    private final static String ROOT_DIR_NAME = "imgCache";
    public static final String TYPE_LOCAL = "local";
    public static final String TYPE_NET = "net";

    String subDirName;
    Map<String, WeakReference<ImageView>> mImageViewCache = new HashMap<>();
    Map<String, SoftReference<Bitmap>> mBitmapCache = new HashMap<>();
    Map<String, Future<?>> mFutureCache = new HashMap<>();
    ExecutorService mService = Executors.newSingleThreadExecutor();
    Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (msg.what == 0x556) {
                String key = msg.obj.toString();
                Bitmap bitmap = findMemoryCache(key);
                List<ImageView> views = findImageView(key);
                for (ImageView view : views) {
                    view.setImageBitmap(bitmap);
                }
            }
        }
    };

    public ImgCache(String subDirName) {
        this.subDirName = subDirName;
        mBitmapCache.clear();
    }

    public void asBitmap(String netUrl, ImgCallback callback) {
        mService.submit(new NetImgToBitmapRunnable(netUrl, callback));
    }

    public void into(ImageView view, ImgCacheExecutor builder) {
        if (TYPE_LOCAL.equals(builder.type)) {
            loadLocalResource(view, builder);
        } else {
            final String tempUrl = builder.url;
            String fileName = Base64.encodeToString(builder.url.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP) + ".bmp";
            File folder = new File(view.getContext().getExternalFilesDir(ROOT_DIR_NAME), subDirName);
            if (!folder.exists()) {
                boolean mr = folder.mkdirs();
            }
            final File file = new File(folder, fileName);
            builder.localSource();
            builder.url = file.getPath();
            builder.build();
            Bitmap bitmap = builder.loadFromMemoryCache ? findMemoryCache(builder.key) : null;
            if (bitmap != null) {
                Log.d(TAG, "Use cache.");
                view.setImageBitmap(bitmap);
                return;
            }
            builder.netSource();
            builder.url = tempUrl;
            loadNetResource(view, builder, file);
        }
    }

    public void clearMemoryCache(String key, boolean recycleBitmap) {
        if (recycleBitmap) {
            SoftReference<Bitmap> reference = mBitmapCache.get(key);
            Bitmap bitmap = reference == null ? null : reference.get();
            if (bitmap != null) {
                bitmap.recycle();
            }
        }
        mBitmapCache.remove(key);
    }

    public void clearMemoryCache(boolean recycleBitmap) {
        if (recycleBitmap) {
            Collection<SoftReference<Bitmap>> collection = mBitmapCache.values();
            for (SoftReference<Bitmap> reference : collection) {
                Bitmap bitmap = reference.get();
                if (bitmap != null) {
                    bitmap.recycle();
                }
            }
        }
        mBitmapCache.clear();
    }

    public void clearDiskCache(Context context) {
        File folder = new File(context.getExternalFilesDir(ROOT_DIR_NAME), subDirName);
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                boolean dr = file.delete();
            }
        }
    }

    private void loadLocalResource(ImageView view, ImgCacheExecutor builder) {
        builder.build();
        Bitmap bitmap = builder.loadFromMemoryCache ? findMemoryCache(builder.key) : null;
        if (bitmap == null) {
            Log.d(TAG, "Load local picture.");
            final String shortUrl = builder.shortUrl();
            Future<?> future = mFutureCache.get(shortUrl);
            if (future != null && !future.isDone()) {
                future.cancel(true);
            }
            mImageViewCache.put(builder.key + "_" + view.hashCode(), new WeakReference<>(view));
            mFutureCache.put(shortUrl, mService.submit(new LocalImgRunnable(builder, new ImgCallback() {
                @Override
                public void onDownloadStart(String key) {

                }

                @Override
                public void onDownloadProgress(String key, long progress, long total) {

                }

                @Override
                public void onDownloadFailed(String key, Exception e) {

                }

                @Override
                public void onDownloadSuccess(String key) {

                }

                @Override
                public void callback(String key, Bitmap bitmap) {
                    mFutureCache.remove(key);
                    if (bitmap != null) {
                        mBitmapCache.put(key, new SoftReference<>(bitmap));
                    }
                    Message msg = Message.obtain();
                    msg.what = 0x556;
                    msg.obj = key;
                    mHandler.sendMessage(msg);
                }
            })));
        } else {
            Log.d(TAG, "Use cache.");
            view.setImageBitmap(bitmap);
        }
    }

    private void loadNetResource(ImageView view, ImgCacheExecutor builder, File file) {
        Log.d(TAG, "Load net picture.");
        final String shortUrl = builder.shortUrl();
        Future<?> future = mFutureCache.get(shortUrl);
        if (future != null && !future.isDone()) {
            future.cancel(true);
        }
        mImageViewCache.put(builder.key + "_" + view.hashCode(), new WeakReference<>(view));
        mFutureCache.put(shortUrl, mService.submit(new NetImgRunnable(builder, file, new ImgCallback() {
            @Override
            public void onDownloadStart(String key) {

            }

            @Override
            public void onDownloadProgress(String key, long progress, long total) {
                Log.d(TAG, String.format(Locale.US, "onDownloadProgress:%s-%d/%d", key, progress, total));
            }

            @Override
            public void onDownloadFailed(String key, Exception e) {
                Log.d(TAG, String.format(Locale.US, "onDownloadFailed:%s", e.getLocalizedMessage()));
            }

            @Override
            public void onDownloadSuccess(String key) {

            }

            @Override
            public void callback(String key, Bitmap bitmap) {
                mFutureCache.remove(key);
                if (bitmap != null) {
                    mBitmapCache.put(key, new SoftReference<>(bitmap));
                }
                Message msg = Message.obtain();
                msg.what = 0x556;
                msg.obj = key;
                mHandler.sendMessage(msg);
            }
        })));
    }

    private List<ImageView> findImageView(String key) {
        List<ImageView> list = new ArrayList<>();
        final List<String> keys = new ArrayList<>(mImageViewCache.keySet());
        for (String keyStr : keys) {
            int index = keyStr.lastIndexOf("_");
            if (key.equals(keyStr.substring(0, index))) {
                WeakReference<ImageView> viewReference = mImageViewCache.get(keyStr);
                ImageView view = viewReference == null ? null : viewReference.get();
                if (view != null) {
                    list.add(view);
                }
                mImageViewCache.remove(keyStr);
            }
        }
        return list;
    }

    private Bitmap findMemoryCache(String key) {
        SoftReference<Bitmap> reference = mBitmapCache.get(key);
        Bitmap bitmap = reference == null ? null : reference.get();
        return bitmap == null || bitmap.isRecycled() ? null : bitmap;
    }
}
