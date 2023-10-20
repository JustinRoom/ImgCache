package jsc.org.lib.img;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;

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
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable r = new Runnable() {
        @Override
        public void run() {
            int position = firstVisiblePosition;
            while (position <= lastVisiblePosition) {
                ViewHolder holder = findViewHolderForAdapterPosition(position);
                if (mLazyLoader != null && holder != null) {
                    mLazyLoader.lazyLoad(position, holder, anticipatedImgWidth, anticipatedImgHeight);
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
            mLastHorScrollDirection = 0;
            mLastVerScrollDirection = 0;
            directionChangedCount = 0;
            boolean changed = calculatePos();
            if (visibleItemPositionChanged || changed) {
                visibleItemPositionChanged = false;
                Log.d("LazilyLoadable", "onScrollStateChanged: Reload Img");
                handler.removeCallbacks(r);
                handler.postDelayed(r, 50);
            }
        } else {
            handler.removeCallbacks(r);
        }
    }

    int mLastHorScrollDirection = 0;
    int mLastVerScrollDirection = 0;
    int directionChangedCount = 0;

    @Override
    public void onScrolled(int dx, int dy) {
        super.onScrolled(dx, dy);
        if (visibleItemPositionChanged) {
            return;
        }
        int horDirection = Integer.compare(dx, 0);
        int verDirection = Integer.compare(dy, 0);
        if (mLastHorScrollDirection != horDirection || mLastVerScrollDirection != verDirection) {
            mLastHorScrollDirection = horDirection;
            mLastVerScrollDirection = verDirection;
            directionChangedCount++;
            //忽略第一次滑动方向改变
            if (directionChangedCount > 1) {
                visibleItemPositionChanged = calculateScrolledPos();
            }
        }
    }

    private boolean visibleItemPositionChanged = false;

    private boolean calculateScrolledPos() {
        if (getAdapter() == null || getAdapter().getItemCount() == 0) {
            return false;
        }
        boolean changed = false;
        LayoutManager layoutManager = getLayoutManager();
        // GridLayoutManager extend LinearLayoutManager
        if (layoutManager instanceof LinearLayoutManager) {
            int firstVPos = ((LinearLayoutManager) layoutManager).findFirstVisibleItemPosition();
            int lastVPos = ((LinearLayoutManager) layoutManager).findLastVisibleItemPosition();
            if (firstVPos != this.firstVisiblePosition || lastVPos != this.lastVisiblePosition) {
                changed = true;
            }
        } else if (layoutManager instanceof StaggeredGridLayoutManager) {
            int[] firstInto = ((StaggeredGridLayoutManager) layoutManager).findFirstVisibleItemPositions(null);
            int[] lastInto = ((StaggeredGridLayoutManager) layoutManager).findLastVisibleItemPositions(null);
            int firstVPos = findMin(firstInto);
            int lastVPos = findMax(lastInto);
            if (firstVPos != this.firstVisiblePosition || lastVPos != this.lastVisiblePosition) {
                changed = true;
            }
        }
        return changed;
    }

    private boolean calculatePos() {
        if (getAdapter() == null || getAdapter().getItemCount() == 0) {
            return false;
        }
        boolean changed = false;
        LayoutManager layoutManager = getLayoutManager();
        // GridLayoutManager extend LinearLayoutManager
        if (layoutManager instanceof LinearLayoutManager) {
            int firstVPos = ((LinearLayoutManager) layoutManager).findFirstVisibleItemPosition();
            int lastVPos = ((LinearLayoutManager) layoutManager).findLastVisibleItemPosition();
            if (firstVPos != this.firstVisiblePosition || lastVPos != this.lastVisiblePosition) {
                this.firstVisiblePosition = firstVPos;
                this.lastVisiblePosition = lastVPos;
                changed = true;
            }
        } else if (layoutManager instanceof StaggeredGridLayoutManager) {
            int[] firstInto = ((StaggeredGridLayoutManager) layoutManager).findFirstVisibleItemPositions(null);
            int[] lastInto = ((StaggeredGridLayoutManager) layoutManager).findLastVisibleItemPositions(null);
            int firstVPos = findMin(firstInto);
            int lastVPos = findMax(lastInto);
            if (firstVPos != this.firstVisiblePosition || lastVPos != this.lastVisiblePosition) {
                this.firstVisiblePosition = firstVPos;
                this.lastVisiblePosition = lastVPos;
                changed = true;
            }
        }
        return changed;
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

    public void setAnticipatedImgSize(String baseOn, int baseWidth, int baseHeight, int value) {
        checkInt(baseWidth, "baseWidth");
        checkInt(baseHeight, "baseHeight");
        checkInt(value, "value");
        int base = "width".equals(baseOn) ? baseWidth : baseHeight;
        int multi = value / base;
        if (multi * base < value) {
            multi++;
        }
        multi = Math.max(1, multi);
        setAnticipatedImgSize(baseWidth * multi, baseHeight * multi);
    }

    public void setAnticipatedImgSize(int anticipatedImgWidth, int anticipatedImgHeight) {
        checkInt(anticipatedImgWidth, "anticipatedImgWidth");
        checkInt(anticipatedImgHeight, "anticipatedImgHeight");
        this.anticipatedImgWidth = anticipatedImgWidth;
        this.anticipatedImgHeight = anticipatedImgHeight;
    }

    public void loadImgDelay250() {
        loadImgDelay(250);
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
