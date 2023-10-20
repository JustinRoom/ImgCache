package jsc.org.lib.img.crop.animation;

public interface SimpleValueAnimatorListener {
    void onAnimationStarted();

    void onAnimationUpdated(float scale);

    void onAnimationFinished();
}
