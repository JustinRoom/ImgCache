package jsc.org.lib.img.refreshlayout.widget;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import jsc.org.lib.img.R;
import jsc.org.lib.img.refreshlayout.IFooterWrapper;

public class SimpleLoadMoreView extends LinearLayout implements IFooterWrapper {

    ImageView ivLoading;
    TextView tvLoading;

    public SimpleLoadMoreView(Context context) {
        this(context, null);
    }

    public SimpleLoadMoreView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SimpleLoadMoreView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        LayoutInflater.from(getContext()).inflate(R.layout.img_view_refresh_footer, this, true);
        ivLoading = findViewById(R.id.iv_loading);
        tvLoading = findViewById(R.id.tv_loading);
    }

    @Override
    public View getView() {
        return this;
    }

    @Override
    public void pullUp() {
        tvLoading.setText(R.string.img_pull_up_to_load_more);
    }

    @Override
    public void pullUpReleasable() {
        tvLoading.setText(R.string.img_release_to_load_more);
    }

    @Override
    public void pullUpRelease() {
        tvLoading.setText(R.string.img_loading);
        Drawable drawable = ivLoading.getDrawable();
        if (drawable instanceof AnimationDrawable) {
            ((AnimationDrawable) drawable).start();
        }
    }

    @Override
    public void loadMoreCompleted() {
        //do anything you want
        //such as show a toast like "load more finish with 10 new messages"
        Drawable drawable = ivLoading.getDrawable();
        if (drawable instanceof AnimationDrawable) {
            ((AnimationDrawable) drawable).stop();
        }
        tvLoading.setText(R.string.img_pull_up_to_load_more);
    }
}
