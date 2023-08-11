package jsc.org.lib.img;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import java.util.Locale;

/**
 * 支持分页加载<br>
 * 支持图片懒加载<br>
 *
 * @author jsc
 */
public class LazilyLoadableRecyclerView extends RecyclerView {

    private ILazyLoader mLazyLoader = null;
    private IPageLoader mPageLoader = null;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable r = new Runnable() {
        @Override
        public void run() {
            int position = firstVisiblePosition;
            while (position <= lastVisiblePosition) {
                if (mLazyLoader != null) {
                    mLazyLoader.lazyLoad(position, findViewHolderForAdapterPosition(position), anticipatedImgWidth, anticipatedImgHeight);
                }
                position++;
            }
        }
    };
    private int firstVisiblePosition = -1;
    private int lastVisiblePosition = -1;
    //图片加载预想宽度
    private int anticipatedImgWidth;
    //图片加载预想高度
    private int anticipatedImgHeight;
    private boolean morePage = false;
    private int pageCapacity = 12;

    public LazilyLoadableRecyclerView(@NonNull Context context) {
        super(context);
    }

    public LazilyLoadableRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public LazilyLoadableRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void onScrollStateChanged(int state) {
        super.onScrollStateChanged(state);
        //stopped the scrolling
        if (state == RecyclerView.SCROLL_STATE_IDLE) {
            calculatePos();
            handler.removeCallbacks(r);
            handler.postDelayed(r, 50);
            int loadedCount = getAdapter() == null ? 0 : getAdapter().getItemCount();
            if (morePage && mPageLoader != null) {
                mPageLoader.onLoadPage(this, loadedCount, pageCapacity);
            }
        } else {
            handler.removeCallbacks(r);
        }
    }

    private void calculatePos() {
        if (getAdapter() == null || getAdapter().getItemCount() == 0) {
            return;
        }
        LayoutManager layoutManager = getLayoutManager();
        // GridLayoutManager extend LinearLayoutManager
        if (layoutManager instanceof LinearLayoutManager) {
            int firstVPos = ((LinearLayoutManager) layoutManager).findFirstVisibleItemPosition();
            int lastVPos = ((LinearLayoutManager) layoutManager).findLastVisibleItemPosition();
            if (firstVPos != this.firstVisiblePosition || lastVPos != this.lastVisiblePosition) {
                this.firstVisiblePosition = firstVPos;
                this.lastVisiblePosition = lastVPos;
            }
        } else if (layoutManager instanceof StaggeredGridLayoutManager) {
            int[] firstInto = ((StaggeredGridLayoutManager) layoutManager).findFirstVisibleItemPositions(null);
            int[] lastInto = ((StaggeredGridLayoutManager) layoutManager).findLastVisibleItemPositions(null);
            int firstVPos = findMin(firstInto);
            int lastVPos = findMax(lastInto);
            if (firstVPos != this.firstVisiblePosition || lastVPos != this.lastVisiblePosition) {
                this.firstVisiblePosition = firstVPos;
                this.lastVisiblePosition = lastVPos;
            }
        }
    }

    private int findMin(int[] into) {
        int min = into[0];
        for (int v : into) {
            if (v < min) {
                min = v;
            }
        }
        return min;
    }

    private int findMax(int[] into) {
        int max = into[0];
        for (int v : into) {
            if (v > max) {
                max = v;
            }
        }
        return max;
    }

    private void checkInt(int value, String name) {
        if (value <= 0)
            throw new IllegalArgumentException(String.format(Locale.US, "\"%s\" must be more than 0.", name));
    }

    public void setLazyLoader(ILazyLoader lazyLoader) {
        this.mLazyLoader = lazyLoader;
    }

    public void setPageLoader(IPageLoader pageLoader) {
        this.mPageLoader = pageLoader;
    }

    public void setPageCapacity(int capacity) {
        checkInt(capacity, "capacity");
        this.pageCapacity = capacity;
    }

    public void setMorePage(int lastPageCount) {
        setMorePage(lastPageCount >= pageCapacity);
    }

    public void setMorePage(boolean morePage) {
        this.morePage = morePage;
    }

    public void setAnticipatedImgSize(String baseOn, int baseWidth, int baseHeight, int value) {
        checkInt(baseWidth,"baseWidth");
        checkInt(baseHeight,"baseHeight");
        checkInt(value,"value");
        int base = "width".equals(baseOn) ? baseWidth : baseHeight;
        int multi = value / base;
        if (multi * base < value) {
            multi++;
        }
        multi = Math.max(1, multi);
        setAnticipatedImgSize(baseWidth * multi, baseHeight * multi);
    }

    public void setAnticipatedImgSize(int anticipatedImgWidth, int anticipatedImgHeight) {
        checkInt(anticipatedImgWidth,"anticipatedImgWidth");
        checkInt(anticipatedImgHeight,"anticipatedImgHeight");
        this.anticipatedImgWidth = anticipatedImgWidth;
        this.anticipatedImgHeight = anticipatedImgHeight;
    }

    public void loadImgDelay(long delay) {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                //如果上一次滑动位置超过第一页，重新加载数据后必须先计算
                calculatePos();
                if (firstVisiblePosition >= 0 && lastVisiblePosition >= 0) {
                    handler.removeCallbacks(r);
                    handler.post(r);
                }
            }
        }, delay);
    }
}
