package com.jsc.imgcache;

import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jsc.imgcache.databinding.ActivityImgBinding;

import java.util.ArrayList;
import java.util.List;

import jsc.org.lib.img.ILazyLoader;
import jsc.org.lib.img.ImgCacheExecutor;
import jsc.org.lib.img.refreshlayout.SimpleRefreshLayout;
import jsc.org.lib.img.refreshlayout.widget.SimpleBottomView;
import jsc.org.lib.img.refreshlayout.widget.SimpleLoadMoreView;
import jsc.org.lib.img.refreshlayout.widget.SimpleRefreshView;

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
public class NetImgActivity extends BaseActivity {

    ActivityImgBinding binding = null;
    NetImgAdapter netImgAdapter = null;
    Handler mHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityImgBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.srlListContainer.setScrollEnable(true);
        binding.srlListContainer.setPullUpEnable(true);
        binding.srlListContainer.setPullDownEnable(true);
        binding.srlListContainer.setHeaderView(new SimpleRefreshView(this));
        binding.srlListContainer.setFooterView(new SimpleLoadMoreView(this));
        binding.srlListContainer.setBottomView(new SimpleBottomView(this));
        binding.srlListContainer.setOnSimpleRefreshListener(new SimpleRefreshLayout.OnSimpleRefreshListener() {
            @Override
            public void onRefresh(SimpleRefreshLayout refreshLayout) {
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        refreshData();
                        binding.srlListContainer.refreshComplete();
                        binding.recyclerView.loadImgDelay(500);
                    }
                }, 4000);
            }

            @Override
            public void onLoadMore(SimpleRefreshLayout refreshLayout) {
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        loadMoreData();
                        binding.srlListContainer.loadMoreComplete();
                        binding.srlListContainer.setNoMore(true);
                        binding.recyclerView.loadImgDelay(500);
                    }
                }, 1000);
            }
        });

        int column = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT ? 4 : 8;
        binding.recyclerView.setLayoutManager(new GridLayoutManager(this, column));
        binding.recyclerView.setAnticipatedImgSize("width", 60, 80, getResources().getDisplayMetrics().widthPixels / column);
        binding.recyclerView.setLazyLoader(new ILazyLoader() {
            @Override
            public void lazyLoad(int position, @NonNull RecyclerView.ViewHolder holder, int anticipatedImgWidth, int anticipatedImgHeight) {
                NetImgAdapter.IViewHolder mHolder = (NetImgAdapter.IViewHolder) holder;
                ImgCacheExecutor.with("ImgActivity")
                        .netSource()
                        .loadFromMemoryCache(true)
                        .loadFromDiskCache(true)
                        .url(netImgAdapter.getUrls().get(position))
                        .size(anticipatedImgWidth, anticipatedImgHeight)
                        .into(mHolder.ivImg);
            }
        });
        netImgAdapter = new NetImgAdapter();
        binding.recyclerView.setAdapter(netImgAdapter);
        ImgCacheExecutor.clearDiskCache(this, "ImgActivity");
    }

    @Override
    public void onLazyLoad() {
        refreshData();
        binding.recyclerView.loadImgDelay(250);
    }

    private void refreshData() {
        List<String> urls = new ArrayList<>();
        urls.add("https://t7.baidu.com/it/u=1819248061,230866778&fm=193&f=GIF");
        urls.add("https://t7.baidu.com/it/u=737555197,308540855&fm=193&f=GIF");
        urls.add("https://t7.baidu.com/it/u=1297102096,3476971300&fm=193&f=GIF");
        urls.add("https://t7.baidu.com/it/u=2783075563,3362558456&fm=193&f=GIF");
        urls.add("https://t7.baidu.com/it/u=963301259,1982396977&fm=193&f=GIF");
        urls.add("https://t7.baidu.com/it/u=852388090,130270862&fm=193&f=GIF");
        urls.add("https://t7.baidu.com/it/u=2291349828,4144427007&fm=193&f=GIF");
        urls.add("https://t7.baidu.com/it/u=4240641596,3235181048&fm=193&f=GIF");
        urls.add("https://t7.baidu.com/it/u=3652245443,3894439772&fm=193&f=GIF");
        urls.add("https://t7.baidu.com/it/u=12235476,3874255656&fm=193&f=GIF");
        urls.add("https://t7.baidu.com/it/u=3203007717,1062852813&fm=193&f=GIF");
        urls.add("https://t7.baidu.com/it/u=810585695,3039658333&fm=193&f=GIF");
        urls.add("https://t7.baidu.com/it/u=3195384123,421318755&fm=193&f=GIF");
        urls.add("https://t7.baidu.com/it/u=1728637936,3151165212&fm=193&f=GIF");
        urls.add("https://t7.baidu.com/it/u=2671101745,1413589787&fm=193&f=GIF");
        urls.add("https://t7.baidu.com/it/u=2487536464,3153080617&fm=193&f=GIF");
        urls.add("https://t7.baidu.com/it/u=2605426091,1199286953&fm=193&f=GIF");
        urls.add("https://t7.baidu.com/it/u=1620952818,4218424235&fm=193&f=GIF");
        urls.add("https://t7.baidu.com/it/u=805456074,3405546217&fm=193&f=GIF");
        urls.add("https://t7.baidu.com/it/u=3902551096,3717324701&fm=193&f=GIF");
        urls.add("https://t7.baidu.com/it/u=4188671375,2323574798&fm=193&f=GIF");
        netImgAdapter.setUrls(urls);
    }

    private void loadMoreData() {
        List<String> urls = new ArrayList<>();
        urls.add("https://t7.baidu.com/it/u=516589436,706141569&fm=193&f=GIF");
        urls.add("https://t7.baidu.com/it/u=330403314,1054912416&fm=193&f=GIF");
        urls.add("https://t7.baidu.com/it/u=3857292935,259148533&fm=193&f=GIF");
        urls.add("https://t7.baidu.com/it/u=2196286164,316669081&fm=193&f=GIF");
        urls.add("https://t7.baidu.com/it/u=2621658848,3952322712&fm=193&f=GIF");
        urls.add("https://t7.baidu.com/it/u=4080826490,615918710&fm=193&f=GIF");
        urls.add("https://t7.baidu.com/it/u=3713375227,571533122&fm=193&f=GIF");
        urls.add("https://t7.baidu.com/it/u=774679999,2679830962&fm=193&f=GIF");
        urls.add("https://t7.baidu.com/it/u=3631608752,3069876728&fm=193&f=GIF");
        urls.add("https://t7.baidu.com/it/u=801209673,1770377204&fm=193&f=GIF");
        urls.add("https://t7.baidu.com/it/u=1010739515,2488150950&fm=193&f=GIF");
        urls.add("https://t7.baidu.com/it/u=124476473,2583135375&fm=193&f=GIF");
        urls.add("https://t7.baidu.com/it/u=1415984692,3889465312&fm=193&f=GIF");
        urls.add("https://t7.baidu.com/it/u=3988344443,4282949406&fm=193&f=GIF");
        urls.add("https://t7.baidu.com/it/u=1635608122,693552335&fm=193&f=GIF");
        urls.add("https://t7.baidu.com/it/u=813347183,2158335217&fm=193&f=GIF");
        urls.add("https://t7.baidu.com/it/u=738441947,1208408731&fm=193&f=GIF");
        urls.add("https://t7.baidu.com/it/u=334080491,3307726294&fm=193&f=GIF");
        urls.add("https://t7.baidu.com/it/u=2235903830,1856743055&fm=193&f=GIF");
        urls.add("https://t7.baidu.com/it/u=1856946436,1599379154&fm=193&f=GIF");
        urls.add("https://t7.baidu.com/it/u=3683704156,288749744&fm=193&f=GIF");
        urls.add("https://t7.baidu.com/it/u=2084624597,235761712&fm=193&f=GIF");
        urls.add("https://t7.baidu.com/it/u=2359570649,2574326109&fm=193&f=GIF");
        urls.add("https://t7.baidu.com/it/u=2731426114,1290998454&fm=193&f=GIF");
        netImgAdapter.addUrls(urls);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ImgCacheExecutor.clearMemoryCache("ImgActivity");
    }
}
