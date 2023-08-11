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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public class NetImgToBitmapRunnable implements Runnable {

    String url;
    ImgCallback callback = null;

    public NetImgToBitmapRunnable(String url, ImgCallback callback) {
        this.url = url;
        this.callback = callback;
    }

    @Override
    public void run() {
        Bitmap bitmap = null;
        InputStream is = null;
        try {
            URL imgUrl = new URL(url);
            // 使用HttpURLConnection打开连接
            URLConnection conn = imgUrl.openConnection();
            conn.setConnectTimeout(10_000);
            conn.connect();
            long totalLength = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N ?
                    conn.getContentLengthLong() : conn.getContentLength();
            // 将得到的数据转化成InputStream
            is = conn.getInputStream();
            bitmap = BitmapFactory.decodeStream(is);
            is.close();
        } catch (NullPointerException | IOException ignore) {

        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (callback != null) {
            callback.callback(url, bitmap);
        }
    }
}
