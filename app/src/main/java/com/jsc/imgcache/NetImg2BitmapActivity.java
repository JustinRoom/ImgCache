package com.jsc.imgcache;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;

import androidx.annotation.Nullable;

import com.jsc.imgcache.databinding.ActivityNetImg2BitmapBinding;
import com.jsc.imgcache.utils.ViewOutlineUtils;

import jsc.org.lib.img.ImgCallback;
import jsc.org.lib.img.ImgUtils;

/**
 * <pre class="preprint">
 *   Copyright JiangShiCheng
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * </pre>
 *
 * @author jsc
 * @createDate 2022/3/3
 */
public class NetImg2BitmapActivity extends BaseActivity {

    ActivityNetImg2BitmapBinding binding = null;
    Handler mHandler = new Handler(Looper.getMainLooper());
    Bitmap mBitmap = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNetImg2BitmapBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewOutlineUtils.applyRoundOutline(binding.ivImg, TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6, getResources().getDisplayMetrics()));
    }

    @Override
    public void onLazyLoad() {
        ImgUtils.asBitmap("https://t7.baidu.com/it/u=4188671375,2323574798&fm=193&f=GIF", new ImgCallback() {
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
                mBitmap = bitmap;
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        binding.ivImg.setImageBitmap(mBitmap);
                    }
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
