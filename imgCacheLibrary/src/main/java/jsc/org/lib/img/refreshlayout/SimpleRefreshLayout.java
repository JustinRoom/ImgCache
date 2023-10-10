package jsc.org.lib.img.refreshlayout;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.AbsListView;
import android.widget.Scroller;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;
import androidx.core.view.NestedScrollingChild;
import androidx.core.view.NestedScrollingChildHelper;
import androidx.core.view.NestedScrollingParent;
import androidx.core.view.NestedScrollingParentHelper;
import androidx.core.view.ViewCompat;

public class SimpleRefreshLayout extends ViewGroup implements NestedScrollingParent, NestedScrollingChild {

    private static final int MSG_PULL_UP = 100;
    private static final int MSG_DOWN_RESET = 101;
    private static final int MSG_UP_RESET = 102;
    private static final int MSG_NO_MORE = 103;
    private static final int MSG_REBOUND_TO_REFRESH = 104;
    private static final int MSG_REBOUND_TO_LOAD_MORE = 105;
    private static final int MSG_ENABLE_SCROLL = 106;
    private static final int SCROLL_NONE = -1; //无滚动
    private static final int SCROLL_UP = 0;  //下拉(currY>lastY)
    private static final int SCROLL_DOWN = 1;  //上拉(currY<lastY)
    private static final float DECELERATE_INTERPOLATION_FACTOR = 2F; //滑动阻尼因子
    private static final long CHECK_STATUS_INTERNAL_TIME = 10L;
    private static final int REBOUND_DURATION = 500;

    private NestedScrollingParentHelper mNestedScrollingParentHelper;
    private NestedScrollingChildHelper mNestedScrollingChildHelper;

    private final int[] mParentScrollConsumed = new int[2];
    private final int[] mParentOffsetInWindow = new int[2];

    private boolean pullDownEnable = true;  //是否允许下拉刷新
    private boolean pullUpEnable = true;    //是否允许加载更多
    private boolean enableScroll = true;    //是否允许视图滑动
    private boolean showBottom;             //是否显示无更多
    private boolean isLastScrollComplete;   //是否上一次滑动已结束
    private int direction;

    private View mTarget;
    private Scroller mScroller;

    private CanChildScrollDown mCanChildScrollDown;
    private CanChildScrollUp mCanChildScrollUp;

    private int effectivePullDownRange;
    private int effectivePullUpRange;
    private int ignorePullRange;

    private IHeaderWrapper mHeaderWrapper;
    private IFooterWrapper mFooterWrapper;
    private IBottomWrapper mBottomWrapper;

    private View mHeaderView;
    private View mFooterView;
    private View mBottomView;

    private int currentState;
    private float mLastY;
    private float mLastX;

    private OnSimpleRefreshListener mRefreshListener;

    public SimpleRefreshLayout(Context context) {
        this(context, null);
    }

    public SimpleRefreshLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SimpleRefreshLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    private void initView() {
        currentState = State.PULL_DOWN_RESET;
        mNestedScrollingParentHelper = new NestedScrollingParentHelper(this);
        mNestedScrollingChildHelper = new NestedScrollingChildHelper(this);
        mScroller = new Scroller(getContext(), new LinearInterpolator());
        ignorePullRange = (int) (getContext().getResources().getDisplayMetrics().density * 8);

        setNestedScrollingEnabled(true);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
    }

    //设置刷新布局
    public void setHeaderView(IHeaderWrapper header) {
        if (header == null) return;
        removeHeaderView();
        this.mHeaderWrapper = header;
        this.mHeaderView = header.getHeaderView();
        addView(mHeaderView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
    }

    public void removeHeaderView() {
        if (mHeaderWrapper == null) return;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child == mHeaderView) {
                removeView(mHeaderView);
                mHeaderView = null;
                mHeaderWrapper = null;
                break;
            }
        }
    }

    //设置加载更多布局
    public void setFooterView(IFooterWrapper footer) {
        if (footer == null) return;
        removeFooterView();
        this.mFooterWrapper = footer;
        this.mFooterView = footer.getFooterView();
        addView(mFooterView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
    }

    public void removeFooterView() {
        if (mFooterWrapper == null) return;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child == mFooterView) {
                removeView(mFooterView);
                mFooterView = null;
                mFooterWrapper = null;
                break;
            }
        }
    }

    //设置加载完成布局
    public void setBottomView(IBottomWrapper bottom) {
        if (bottom == null) return;
        removeBottomView();
        this.mBottomWrapper = bottom;
        this.mBottomView = bottom.getBottomView();
        addView(mBottomView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
    }

    public void removeBottomView() {
        if (mBottomWrapper == null) return;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child == mBottomView) {
                removeView(mBottomView);
                mBottomView = null;
                mBottomWrapper = null;
                break;
            }
        }
    }

    public IBottomWrapper getBottomView() {
        return mBottomWrapper;
    }

    public IHeaderWrapper getHeaderView() {
        return mHeaderWrapper;
    }

    public IFooterWrapper getFooterView() {
        return mFooterWrapper;
    }

    public void setNoMore(boolean noMore) {
        //Handler是为了让上拉回弹先走完，再显示BottomView;
        this.showBottom = noMore;
        if (showBottom && ((currentState != State.PULL_DOWN_FINISH && currentState != State.PULL_UP_FINISH)
                || getScrollY() != 0)) {
            mHandler.sendEmptyMessageDelayed(MSG_NO_MORE, CHECK_STATUS_INTERNAL_TIME);
            return;
        }
        if (mBottomView != null) mBottomView.setVisibility(showBottom ? VISIBLE : GONE);
        if (mFooterView != null) mFooterView.setVisibility(showBottom ? GONE : VISIBLE);
    }

    public void enableScroll(boolean enable) {
        this.enableScroll = enable;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (mTarget == null) {
            ensureTarget();
        }
        measureChildren(widthMeasureSpec, heightMeasureSpec);
        if (mHeaderView != null) {
            effectivePullDownRange = mHeaderView.getMeasuredHeight() * 3 / 5;
        }
        if (mFooterView != null) {
            effectivePullUpRange = mFooterView.getMeasuredHeight() * 3 / 5;
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child == mHeaderView) {
                child.layout(0, -child.getMeasuredHeight(), child.getMeasuredWidth(), 0);
            } else if (child == mFooterView) {
                child.layout(0, getMeasuredHeight(), child.getMeasuredWidth(), getMeasuredHeight() + child.getMeasuredHeight());
            } else if (child == mBottomView) {
                child.layout(0, getMeasuredHeight(), child.getMeasuredWidth(), getMeasuredHeight() + child.getMeasuredHeight());
            } else {
                child.layout(getPaddingLeft(), getPaddingTop(), getPaddingLeft() + child.getMeasuredWidth(), getMeasuredHeight() - getPaddingBottom());
            }
        }
    }

    private void ensureTarget() {
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child != mHeaderView && child != mFooterView && child != mBottomView) {
                mTarget = child;
                break;
            }
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        boolean intercept = false;
        float y = ev.getY();
        float x = ev.getX();
        direction = ev.getAction() == MotionEvent.ACTION_UP || y == mLastY ?
                SCROLL_NONE : y > mLastY ? SCROLL_UP : SCROLL_DOWN;
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                break;
            case MotionEvent.ACTION_MOVE:
                if (mTarget != null && !ViewCompat.isNestedScrollingEnabled(mTarget)) {
                    if (y > mLastY) {//上滑
                        intercept = !canChildScrollUp();
                    } else if (y < mLastY) {
                        intercept = !canChildScrollDown();
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                direction = SCROLL_NONE;
                break;
        }
        boolean vertical = Math.abs(y - mLastY) - Math.abs(x - mLastX) > 0;
        mLastY = y;
        mLastX = x;
        return intercept && vertical;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float y = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                break;
            case MotionEvent.ACTION_MOVE:
                if (Math.abs(getScrollY()) > ignorePullRange) {
                    requestDisallowInterceptTouchEvent(true);
                }
                if (enableScroll) {
                    executeScroll((int) (mLastY - y));
                }
                break;
            case MotionEvent.ACTION_UP:
                onStopScroll();
                requestDisallowInterceptTouchEvent(false);
                break;
        }
        mLastY = y;
        return true;
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        // if this is a List < L or another view that doesn't support nested
        // scrolling, ignore this request so that the vertical scroll event
        // isn't stolen
        if ((Build.VERSION.SDK_INT < 21 && mTarget instanceof AbsListView)
                || (mTarget != null && !ViewCompat.isNestedScrollingEnabled(mTarget))) {
            // Nope.
        } else {
            super.requestDisallowInterceptTouchEvent(disallowIntercept);
        }
    }

    private boolean canChildScrollDown() {
        if (mCanChildScrollDown != null) {
            return mCanChildScrollDown.canChildScrollDown(this, mTarget);
        }
        if (mTarget instanceof AbsListView) {
            AbsListView absListView = (AbsListView) mTarget;
            return absListView.getChildCount() > 0 && (absListView.getLastVisiblePosition() != absListView.getChildCount() - 1
                    || absListView.getChildAt(absListView.getChildCount() - 1).getBottom() > absListView.getMeasuredHeight());
        } else {
            return mTarget.canScrollVertically(1) || mTarget.getScrollY() < mTarget.getMeasuredHeight() - getMeasuredHeight();
        }
    }

    private boolean canChildScrollUp() {
        if (mCanChildScrollUp != null) {
            return mCanChildScrollUp.canChildScrollUp(this, mTarget);
        }
        if (mTarget instanceof AbsListView) {
            AbsListView absListView = (AbsListView) mTarget;
            return absListView.getChildCount() > 0 && (absListView.getChildAt(0).getTop() < absListView.getPaddingTop()
                    || absListView.getFirstVisiblePosition() > 0);
        } else {
            return mTarget.canScrollVertically(-1) || mTarget.getScrollY() > 0;
        }
    }

    /**
     * 拉伸状态判断
     * <p>
     * 上拉中或上拉抬起手指，并且在加载更多，无效
     * 下拉中或下拉抬起手指，并且在下拉刷新，无效
     */
    private boolean isRefreshingOrLoading() {
        return (direction != SCROLL_UP && currentState >= State.PULL_UP_RELEASE && currentState < State.PULL_UP_FINISH)
                || (direction != SCROLL_DOWN && currentState >= State.PULL_DOWN_RELEASE && currentState < State.PULL_DOWN_FINISH);
    }

    private void pullDownReset() {
        if (!rebound()) {
            enableScroll(true);
        }
    }

    private void pullUpReset() {
        if (!rebound()) {
            enableScroll(true);
        }
        mHandler.sendEmptyMessageDelayed(MSG_PULL_UP, CHECK_STATUS_INTERNAL_TIME);
    }

    /**
     * 回弹到原始位置
     */
    private boolean rebound() {
        if (getScrollY() == 0) return false;
        mHandler.removeMessages(MSG_ENABLE_SCROLL);
        //动态计算回弹时间
        int stampTime = Math.abs(getScrollY()) * 3 / 2;
        int duration = Math.min(stampTime, REBOUND_DURATION);
        mHandler.sendEmptyMessageDelayed(MSG_ENABLE_SCROLL, duration);
        mScroller.startScroll(0, getScrollY(), 0, -getScrollY(), duration);
        invalidate();//触发onDraw()
        return true;
    }

    /**
     * 回弹到指定位置
     */
    private void reboundTo(int position, int msgKey) {
        if (getScrollY() == position) {
            if (msgKey == MSG_REBOUND_TO_REFRESH) {
                refreshData();
            } else if (msgKey == MSG_REBOUND_TO_LOAD_MORE) {
                loadMoreData();
            }
            return;
        }
        mHandler.removeMessages(msgKey);
        //动态计算回弹时间
        int dy = position - getScrollY();
        int duration = Math.abs(dy);
        mHandler.sendEmptyMessageDelayed(msgKey, duration);
        mScroller.startScroll(0, getScrollY(), 0, dy, duration);
        invalidate();//触发onDraw()
    }

    private void refreshData() {
        if (mHeaderWrapper != null) {
            mHeaderWrapper.pullDownRelease();
        }
        if (mRefreshListener != null) {
            mRefreshListener.onRefresh(this);
        }
    }

    private void loadMoreData() {
        if (mFooterWrapper != null) {
            mFooterWrapper.pullUpRelease();
        }
        if (mRefreshListener != null) {
            mRefreshListener.onLoadMore(this);
        }
    }

    private void executeScroll(int dy) {
        if (!isLastScrollComplete) return;
        if (isRefreshingOrLoading()) return;

        if (dy > 0) {
            //上拉加载
            if (showBottom) {
                //显示无更多布局
                if (mBottomView != null) mBottomView.setVisibility(VISIBLE);
                if (mFooterView != null) mFooterView.setVisibility(GONE);
                if (getScrollY() < 0) { //下拉过程中的上拉，无效上拉
                    if (Math.abs(getScrollY()) < effectivePullDownRange) {
                        if (currentState != State.PULL_DOWN)
                            updateStatus(State.PULL_DOWN);
                    }
                } else {
                    if (!pullUpEnable) return;
                    int bHeight = 0;
                    if (mBottomView != null)
                        bHeight = mBottomView.getMeasuredHeight();
                    if (Math.abs(getScrollY()) >= bHeight) return;
                    dy /= computeInterpolationFactor(getScrollY());
                    updateStatus(State.BOTTOM);
                }
            } else {
                //显示加载布局
                if (mFooterView == null) return;
                if (mBottomView != null) mBottomView.setVisibility(GONE);
                if (mFooterView != null) mFooterView.setVisibility(VISIBLE);
                if (getScrollY() < 0) { //下拉过程中的上拉，无效上拉
                    if (Math.abs(getScrollY()) < effectivePullDownRange) {
                        if (currentState != State.PULL_DOWN) {
                            updateStatus(State.PULL_DOWN);
                        }
                    }
                } else {
                    if (!pullUpEnable) return;
                    if (Math.abs(getScrollY()) >= effectivePullUpRange) {
                        dy /= computeInterpolationFactor(getScrollY());
                        if (currentState != State.PULL_UP_RELEASABLE) {
                            updateStatus(State.PULL_UP_RELEASABLE);
                        }
                    } else {
                        if (currentState != State.PULL_UP) {
                            updateStatus(State.PULL_UP);
                        }
                    }
                }
            }
        } else {
            //下拉刷新
            if (getScrollY() > 0) {   //说明不是到达顶部的下拉，无效下拉
                if (Math.abs(getScrollY()) < effectivePullUpRange) {
                    if (currentState != State.PULL_UP) {
                        updateStatus(State.PULL_UP);
                    }
                }
            } else {
                if (!pullDownEnable) return;
                if (Math.abs(getScrollY()) >= effectivePullDownRange) {
                    //到达下拉最大距离，增加阻尼因子
                    dy /= computeInterpolationFactor(getScrollY());
                    if (currentState != State.PULL_DOWN_RELEASABLE) {
                        updateStatus(State.PULL_DOWN_RELEASABLE);
                    }
                } else {
                    if (currentState != State.PULL_DOWN) {
                        updateStatus(State.PULL_DOWN);
                    }
                }
            }
        }

        dy /= DECELERATE_INTERPOLATION_FACTOR;
        scrollBy(0, dy);
    }

    private void onStopScroll() {
        if (isRefreshingOrLoading()) return;
        if (showBottom && getScrollY() > 0) {
            //显示无更多布局
            updateStatus(State.BOTTOM);
            rebound();
            return;
        }
        if ((Math.abs(getScrollY()) >= effectivePullDownRange) && getScrollY() < 0) {
            //有效下拉
            updateStatus(State.PULL_DOWN_RELEASE);
        } else if ((Math.abs(getScrollY()) >= effectivePullUpRange) && getScrollY() > 0) {
            //有效上拉
            updateStatus(State.PULL_UP_RELEASE);
        } else {
            //无效距离，还原
            updateStatus(State.PULL_NORMAL);
        }
    }

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            isLastScrollComplete = false;
            scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
            invalidate();
        } else {
            isLastScrollComplete = true;
            if (currentState == State.PULL_DOWN_RESET)
                currentState = State.PULL_DOWN_FINISH;
            if (currentState == State.PULL_UP_RESET)
                currentState = State.PULL_UP_FINISH;
        }
    }

    private void updateStatus(int state) {
        if (currentState == state) return;
        currentState = state;
        switch (state) {
            case State.PULL_NORMAL:
                pullDownReset();
                break;
            case State.PULL_DOWN:
                if (mHeaderWrapper != null) {
                    mHeaderWrapper.pullDown();
                }
                break;
            case State.PULL_DOWN_RELEASABLE:
                if (mHeaderWrapper != null) {
                    mHeaderWrapper.pullDownReleasable();
                }
                break;
            case State.PULL_DOWN_RELEASE:
                setNoMore(false);
                enableScroll(false);
                reboundTo(mHeaderView == null ? 0 : -mHeaderView.getMeasuredHeight(), MSG_REBOUND_TO_REFRESH);
                break;
            case State.PULL_DOWN_RESET:
                pullDownReset();
                if (mHeaderWrapper != null) {
                    mHeaderWrapper.refreshCompleted();
                }
                break;
            case State.PULL_UP_RESET:
                pullUpReset();
                if (mFooterWrapper != null) {
                    mFooterWrapper.loadMoreCompleted();
                }
                break;
            case State.PULL_UP:
                if (mFooterWrapper != null) {
                    mFooterWrapper.pullUp();
                }
                break;
            case State.PULL_UP_RELEASABLE:
                if (mFooterWrapper != null) {
                    mFooterWrapper.pullUpReleasable();
                }
                break;
            case State.PULL_UP_RELEASE:
                enableScroll(false);
                reboundTo(mFooterView == null ? 0 : mFooterView.getMeasuredHeight(), MSG_REBOUND_TO_LOAD_MORE);
                break;
            case State.PULL_UP_FINISH:
                if (mFooterWrapper != null) {
                    mFooterWrapper.loadMoreCompleted();
                }
                break;
            case State.BOTTOM:
                if (mBottomWrapper != null) {
                    mBottomWrapper.showBottom();
                }
                break;
        }
    }

    private final Handler mHandler = new Handler(Looper.getMainLooper()) {
        private int pullCount = 0;

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_PULL_UP:
                    pullCount++;

                    if (canChildScrollDown()) {
                        pullCount = 0;
                        mHandler.removeMessages(MSG_PULL_UP);
                        mTarget.scrollBy(0, (int) (getResources().getDisplayMetrics().density * 6));
                    } else {
                        if (pullCount >= 20) {
                            pullCount = 0;
                            mHandler.removeMessages(MSG_PULL_UP);
                        } else {
                            mHandler.sendEmptyMessageDelayed(MSG_PULL_UP, CHECK_STATUS_INTERNAL_TIME);
                        }
                    }
                    break;
                case MSG_DOWN_RESET:
                    refreshComplete();
                    break;
                case MSG_UP_RESET:
                    loadMoreComplete();
                    break;
                case MSG_NO_MORE:
                    if (getScrollY() == 0 && (currentState == State.PULL_DOWN_FINISH || currentState == State.PULL_UP_FINISH)) {
                        setNoMore(showBottom);
                    } else {
                        mHandler.sendEmptyMessageDelayed(MSG_NO_MORE, CHECK_STATUS_INTERNAL_TIME);
                    }
                    break;
                case MSG_REBOUND_TO_REFRESH:
                    refreshData();
                    break;
                case MSG_REBOUND_TO_LOAD_MORE:
                    loadMoreData();
                    break;
                case MSG_ENABLE_SCROLL:
                    enableScroll(true);
                    break;
            }
        }
    };

    private float computeInterpolationFactor(int dy) {
        int absY = Math.abs(dy);
        int delta;
        if (dy > 0) {
            if (absY <= effectivePullUpRange) return DECELERATE_INTERPOLATION_FACTOR;
            delta = (absY - effectivePullUpRange) / 50;  //增加50，阻尼系数+1
        } else {
            if (absY <= effectivePullDownRange) return DECELERATE_INTERPOLATION_FACTOR;
            delta = (absY - effectivePullDownRange) / 50;  //增加50，阻尼系数+1
        }

        return DECELERATE_INTERPOLATION_FACTOR + delta;
    }

    public void refreshComplete() {
        if (!isLastScrollComplete) {
            mHandler.sendEmptyMessageDelayed(MSG_DOWN_RESET, CHECK_STATUS_INTERNAL_TIME);
            return;
        }
        updateStatus(State.PULL_DOWN_RESET);
    }

    public void loadMoreComplete() {
        if (!isLastScrollComplete) {
            mHandler.sendEmptyMessageDelayed(MSG_UP_RESET, CHECK_STATUS_INTERNAL_TIME);
            return;
        }
        updateStatus(State.PULL_UP_RESET);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mHandler.removeCallbacksAndMessages(null);
    }

    public interface CanChildScrollUp {
        boolean canChildScrollUp(SimpleRefreshLayout parent, View child);
    }

    public interface CanChildScrollDown {
        boolean canChildScrollDown(SimpleRefreshLayout parent, View child);
    }

    public interface OnSimpleRefreshListener {

        void onRefresh(SimpleRefreshLayout refreshLayout);

        void onLoadMore(SimpleRefreshLayout refreshLayout);
    }

    public void setOnSimpleRefreshListener(OnSimpleRefreshListener listener) {
        this.mRefreshListener = listener;
    }

    public void setPullDownEnable(boolean pullDownEnable) {
        this.pullDownEnable = pullDownEnable;
    }

    public void setPullUpEnable(boolean pullUpEnable) {
        this.pullUpEnable = pullUpEnable;
    }

    public void setScrollEnable(boolean enable) {
        this.enableScroll = enable;
    }

    //--------------------  NestedScrollParent  -------------------------------//

    @Override
    public boolean onStartNestedScroll(@NonNull View child, @NonNull View target, int nestedScrollAxes) {
        return isEnabled() && (nestedScrollAxes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0;
    }

    @Override
    public void onNestedScrollAccepted(@NonNull View child, @NonNull View target, int axes) {
        mNestedScrollingParentHelper.onNestedScrollAccepted(child, target, axes);

        //告诉父类开始滑动
        startNestedScroll(axes & ViewCompat.SCROLL_AXIS_VERTICAL);
    }

    @Override
    public void onNestedPreScroll(@NonNull View target, int dx, int dy, @NonNull int[] consumed) {

        if (!enableScroll) return;
        //只有在自己滑动的情形下才进行预消耗
        if (getScrollY() != 0) {

            //这里相当于做了一个边界条件
            if (getScrollY() > 0 && dy < 0 && Math.abs(dy) >= Math.abs(getScrollY())) {  //上拉过程中下拉
                consumed[1] = getScrollY();
                scrollTo(0, 0);
                return;
            }

            if (getScrollY() < 0 && dy > 0 && Math.abs(dy) >= Math.abs(getScrollY())) {
                consumed[1] = getScrollY();
                scrollTo(0, 0);
                return;
            }

            int yConsumed = Math.abs(dy) >= Math.abs(getScrollY()) ? getScrollY() : dy;
            executeScroll(yConsumed);
            consumed[1] = yConsumed;
        }

        //父类消耗剩余距离
        final int[] parentConsumed = mParentScrollConsumed;
        if (dispatchNestedPreScroll(dx - consumed[0], dy - consumed[1], parentConsumed, null)) {
            consumed[0] += parentConsumed[0];
            consumed[1] += parentConsumed[1];
        }
    }

    @Override
    public void onNestedScroll(@NonNull View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed) {

        dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, mParentOffsetInWindow);

        int dy = dyUnconsumed + mParentOffsetInWindow[1];

        if (enableScroll) {
            if (direction == SCROLL_DOWN && !pullUpEnable) return;                  //用户不开启加载
            if (direction == SCROLL_UP && !pullDownEnable) return;                  //用户不开启下拉
            executeScroll(dy);
        }
    }

    @Override
    public void onStopNestedScroll(@NonNull View child) {
        onStopScroll();
        mNestedScrollingParentHelper.onStopNestedScroll(child);

        stopNestedScroll();
    }

    @Override
    public int getNestedScrollAxes() {
        return mNestedScrollingParentHelper.getNestedScrollAxes();
    }

    //------------------------------ NestedScrollChild ---------------------//


    @Override
    public void setNestedScrollingEnabled(boolean enabled) {
        mNestedScrollingChildHelper.setNestedScrollingEnabled(enabled);
    }

    @Override
    public boolean isNestedScrollingEnabled() {
        return mNestedScrollingChildHelper.isNestedScrollingEnabled();
    }

    @Override
    public boolean startNestedScroll(int axes) {
        return mNestedScrollingChildHelper.startNestedScroll(axes);
    }

    @Override
    public void stopNestedScroll() {
        mNestedScrollingChildHelper.stopNestedScroll();
    }

    @Override
    public boolean hasNestedScrollingParent() {
        return mNestedScrollingChildHelper.hasNestedScrollingParent();
    }

    @Override
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, @Nullable @Size(value = 2) int[] offsetInWindow) {
        return mNestedScrollingChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, @Nullable @Size(value = 2) int[] consumed, @Nullable @Size(value = 2) int[] offsetInWindow) {
        return mNestedScrollingChildHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow);
    }

    @Override
    public boolean onNestedPreFling(@NonNull View target, float velocityX, float velocityY) {
        return dispatchNestedPreFling(velocityX, velocityY);
    }

    @Override
    public boolean onNestedFling(@NonNull View target, float velocityX, float velocityY, boolean consumed) {
        return dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
        return mNestedScrollingChildHelper.dispatchNestedPreFling(velocityX, velocityY);
    }

    @Override
    public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
        return mNestedScrollingChildHelper.dispatchNestedFling(velocityX, velocityY, consumed);
    }

    //-------------------------------- 状态 --------------------------------//

    private interface State {

        int PULL_NORMAL = 0;  //普通状态
        int PULL_DOWN = 1;  //下拉中
        int PULL_DOWN_RELEASABLE = 2;  //下拉可刷新
        int PULL_DOWN_RELEASE = 3;  //下拉正在刷新
        int PULL_DOWN_RESET = 4;  //下拉恢复正常
        int PULL_DOWN_FINISH = 5;  //下拉完成
        int PULL_UP = 6;  //上拉中
        int PULL_UP_RELEASABLE = 7;  //上拉可刷新
        int PULL_UP_RELEASE = 8;  //上拉正在刷新
        int PULL_UP_RESET = 9;  //上拉恢复正常
        int PULL_UP_FINISH = 10; //上拉完成
        int BOTTOM = 11; //无更多
    }
}
