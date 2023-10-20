package jsc.org.lib.img.selector.provider;

import android.graphics.Outline;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewOutlineProvider;

public final class RoundViewOutlineProvider extends ViewOutlineProvider {

    int type = 0;
    float value = 4.0f;

    public RoundViewOutlineProvider pixelRadius(float value) {
        type = 0;
        this.value = value;
        return this;
    }

    public RoundViewOutlineProvider dpRadius(float value) {
        type = 1;
        this.value = value;
        return this;
    }

    @Override
    public void getOutline(View view, Outline outline) {
        if (view.getWidth() > 0 && view.getHeight() > 0) {
            float radius = type == 0 ? value : TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, view.getContext().getResources().getDisplayMetrics());
            outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radius);
        }
    }
}
