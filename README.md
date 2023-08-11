# ImgCache
轻量级超好用的图片缓存框架。
异步加载机制，丝滑流畅，超棒的体验。

![效果展示](screenshots/record.gif)

### 1、延迟加载一定尺寸大小的图片，避免加载多张大图导致内存溢出
```
recyclerView.setAnticipatedImgSize("width", 60, 80, getResources().getDisplayMetrics().widthPixels / column);
recyclerView.setLazyLoader(new ILazyLoader() {
            @Override
            public void lazyLoad(int position, RecyclerView.ViewHolder holder, int anticipatedImgWidth, int anticipatedImgHeight) {
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
```

### 2、加载首页图片
```
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
        recyclerView.setMorePage(true);
        recyclerView.loadImgDelay(300);
```

### 3、分页加载
在加载完数据手一定要手动调用延时加载图片```recyclerView.loadImgDelay(100);```
```
recyclerView.setPageLoader(new IPageLoader() {
            @Override
            public void onLoadPage(LazilyLoadableRecyclerView recyclerView, int loadedCount, int pageCapacity) {
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
                recyclerView.setMorePage(false);
                recyclerView.loadImgDelay(100);
            }
        });
```

# 联系我
添加微信，请备注"图片缓存框架"
![我的微信](/screenshots/wechat.png)