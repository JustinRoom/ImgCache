package jsc.org.lib.img.refreshlayout;

public interface IFooterWrapper extends IViewWrapper {

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
