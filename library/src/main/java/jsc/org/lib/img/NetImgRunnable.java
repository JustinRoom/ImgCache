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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public class NetImgRunnable implements Runnable {

    ImgCacheExecutor mBuilder;
    ImgCallback callback = null;
    File file = null;

    public NetImgRunnable(ImgCacheExecutor builder, File file, ImgCallback callback) {
        this.mBuilder = builder;
        this.file = file;
        this.callback = callback;
    }

    @Override
    public void run() {
        assert callback != null;
        Bitmap bitmap = mBuilder.loadFromDiskCache ? loadLocalImg() : null;
        if (bitmap != null) {
            callback.callback(mBuilder.key, bitmap);
            return;
        }

        InputStream is = null;
        FileOutputStream fos = null;
        try {
            callback.onDownloadStart(mBuilder.key);
            boolean dr = file.delete();
            boolean cr = file.createNewFile();
            URL imgUrl = new URL(mBuilder.url);
            // 使用HttpURLConnection打开连接
            URLConnection conn = imgUrl.openConnection();
            conn.setConnectTimeout(10_000);
            conn.connect();
            long totalLength = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N ?
                    conn.getContentLengthLong() : conn.getContentLength();
            // 将得到的数据转化成InputStream
            is = conn.getInputStream();
            fos = new FileOutputStream(file);
            byte[] buffer = new byte[4096];
            long downLoadLength = 0L;
            int counter = 0;
            while (true) {
                int read = is.read(buffer);
                if (read == -1) {
                    break;
                }
                counter++;
                fos.write(buffer, 0, read);
                downLoadLength += read;
                if (counter >= 25) {
                    counter = 0;
                    fos.flush();
                    callback.onDownloadProgress(mBuilder.key, downLoadLength, totalLength);
                }
            }
            if (counter > 0) {
                callback.onDownloadProgress(mBuilder.key, downLoadLength, totalLength);
            }
            fos.flush();
            is.close();
            fos.close();
            callback.onDownloadSuccess(mBuilder.key);
            bitmap = loadLocalImg();
            callback.callback(mBuilder.key, bitmap);
        } catch (NullPointerException | IOException e) {
            boolean dr = file.delete();
            callback.onDownloadFailed(mBuilder.key, e);
            callback.callback(mBuilder.key, null);
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private Bitmap loadLocalImg() {
        if (!file.exists()) return null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = mBuilder.sample;
        options.inSampleSize = 1;
        if (mBuilder.sample) {
            Log.d("NetImg", "Decode picture just for bounds.");
            BitmapFactory.decodeFile(file.getPath(), options);
            Log.d("NetImg", "Image mime type:" + options.outMimeType);
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
        Log.d("NetImg", "Decode picture.");
        return BitmapFactory.decodeFile(file.getPath(), options);
    }
}
