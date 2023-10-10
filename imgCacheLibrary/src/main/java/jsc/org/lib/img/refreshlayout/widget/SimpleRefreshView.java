package jsc.org.lib.img.refreshlayout.widget;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import jsc.org.lib.img.R;
import jsc.org.lib.img.refreshlayout.IHeaderWrapper;

public class SimpleRefreshView extends LinearLayout implements IHeaderWrapper {

    ImageView ivRefresh;
    TextView tvRefresh;

    public SimpleRefreshView(Context context) {
        this(context, null);
    }

    public SimpleRefreshView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SimpleRefreshView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        LayoutInflater.from(getContext()).inflate(R.layout.img_view_refresh_header, this, true);
        ivRefresh = (ImageView) findViewById(R.id.iv_refresh);
        tvRefresh = (TextView) findViewById(R.id.tv_refresh);
    }

    @Override
    public View getHeaderView() {
        return this;
    }

    @Override
    public void pullDown() {
        tvRefresh.setText(R.string.img_pull_down_to_refresh);
    }

    @Override
    public void pullDownReleasable() {
        tvRefresh.setText(R.string.img_release_to_refresh);
    }

    @Override
    public void pullDownRelease() {
        tvRefresh.setText(R.string.img_refreshing);
        Drawable drawable = ivRefresh.getDrawable();
        if (drawable instanceof AnimationDrawable) {
            ((AnimationDrawable) drawable).start();
        }
    }

    @Override
    public void refreshCompleted() {
        Drawable drawable = ivRefresh.getDrawable();
        if (drawable instanceof AnimationDrawable) {
            ((AnimationDrawable) drawable).stop();
        }
        tvRefresh.setText(R.string.img_pull_down_to_refresh);
    }
}
