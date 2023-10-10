package jsc.org.lib.img.refreshlayout;

import android.view.View;

public interface IHeaderWrapper {

    /**
     * 获取刷新布局
     */
    View getHeaderView();

    /**
     * 下拉中
     */
    void pullDown();

    /**
     * 下拉可刷新
     */
    void pullDownReleasable();

    /**
     * 下拉刷新中
     */
    void pullDownRelease();

    /**
     * 刷新完毕
     */
    void refreshCompleted();
}
