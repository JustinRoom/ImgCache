package jsc.org.lib.img.refreshlayout;

public interface IHeaderWrapper extends IViewWrapper {

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
