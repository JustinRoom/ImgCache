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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

public class LocalImgRunnable implements Runnable {

    ImgCacheExecutor mBuilder;
    ImgCallback callback = null;

    public LocalImgRunnable(ImgCacheExecutor builder, ImgCallback callback) {
        this.mBuilder = builder;
        this.callback = callback;
    }

    @Override
    public void run() {
        assert callback != null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = mBuilder.sample;
        options.inSampleSize = 1;
        if (mBuilder.sample) {
            Log.d("LocalImg", "Decode picture just for bounds.");
            BitmapFactory.decodeFile(mBuilder.url, options);
            int outWidth = options.outWidth;
            int outHeight = options.outHeight;
            int sampleSize = 1;
            while (outWidth / sampleSize > mBuilder.width
                    || outHeight / sampleSize > mBuilder.height) {
                sampleSize = sampleSize * 2;
            }
            options.inJustDecodeBounds = false;
            options.inSampleSize = sampleSize;
        }
        Log.d("LocalImg", "Decode picture.");
        Bitmap bitmap = BitmapFactory.decodeFile(mBuilder.url, options);
        callback.callback(mBuilder.key, bitmap);
    }
}
