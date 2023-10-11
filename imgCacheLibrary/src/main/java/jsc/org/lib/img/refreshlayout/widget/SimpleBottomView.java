package jsc.org.lib.img.refreshlayout.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import jsc.org.lib.img.R;
import jsc.org.lib.img.refreshlayout.IBottomWrapper;

public class SimpleBottomView extends LinearLayout implements IBottomWrapper {

    public SimpleBottomView(Context context) {
        this(context, null);
    }

    public SimpleBottomView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SimpleBottomView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setBackgroundColor(0xFFF5F8F9);
        LayoutInflater.from(getContext()).inflate(R.layout.img_view_refresh_bottom,this,true);
    }

    @Override
    public View getView() {
        return this;
    }

    @Override
    public void onShow() {

    }
}
