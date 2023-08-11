package com.jsc.imgcache;

import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Environment;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jsc.imgcache.databinding.ActivityImgBinding;

import java.io.File;

import jsc.org.lib.img.ILazyLoader;
import jsc.org.lib.img.ImgCacheExecutor;

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
public class LocalImgActivity extends BaseActivity {

    ActivityImgBinding binding = null;
    LocalImgAdapter localImgAdapter = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityImgBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        int column = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT ? 4 : 8;
        binding.recyclerView.setLayoutManager(new GridLayoutManager(this, column));
        binding.recyclerView.setAnticipatedImgSize("width", 60, 80, getResources().getDisplayMetrics().widthPixels / column);
        binding.recyclerView.setLazyLoader(new ILazyLoader() {
            @Override
            public void lazyLoad(int position, RecyclerView.ViewHolder holder, int anticipatedImgWidth, int anticipatedImgHeight) {
                LocalImgAdapter.IViewHolder mHolder = (LocalImgAdapter.IViewHolder) holder;
                ImgCacheExecutor.with("ImgActivity")
                        .localSource()
                        .url(localImgAdapter.getFiles()[position].getPath())
                        .size(anticipatedImgWidth, anticipatedImgHeight)
                        .into(mHolder.ivImg);
            }
        });
        localImgAdapter = new LocalImgAdapter();
        binding.recyclerView.setAdapter(localImgAdapter);
        ImgCacheExecutor.clearDiskCache(this, "ImgActivity");
    }

    @Override
    public void onLazyLoad() {
        String dir = Environment.getExternalStorageDirectory().getPath()
                + File.separator + "413";
        localImgAdapter.setFiles(new File(dir).listFiles());
        binding.recyclerView.loadImgDelay(300);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ImgCacheExecutor.clearMemoryCache("ImgActivity");
    }
}
