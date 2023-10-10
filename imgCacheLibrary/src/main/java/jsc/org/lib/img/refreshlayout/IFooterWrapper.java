package jsc.org.lib.img.refreshlayout;

import android.view.View;

public interface IFooterWrapper {

    /**
     * 获取加载更多布局
     *
     */
    View getFooterView();

    /**
     * 上拉中
     */
    void pullUp();

    /**
     * 上拉可释放
     */
    void pullUpReleasable();

    /**
     * 上拉已释放
     */
    void pullUpRelease();

    /**
     * 加载完成
     */
    void loadMoreCompleted();
}
