package jsc.org.lib.img.selector.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.widget.ViewPager2;

import java.util.ArrayList;
import java.util.Locale;

import jsc.org.lib.img.R;
import jsc.org.lib.img.selector.model.LocalMedia;
import jsc.org.lib.img.selector.provider.RoundViewOutlineProvider;

public final class ImagePreviewActivity extends AppCompatActivity {
    public static final String EXTRA_PREVIEW_LIST = "previewList";
    public static final String EXTRA_SELECTED_LIST = "lastSelectedList";
    public static final String EXTRA_POSITION = "position";
    public static final String EXTRA_MAX_SELECT_COUNT = "maxSelectCount";
    public static final String EXTRA_IS_MULTIPLE = "isMultiple";
    public static final String OUTPUT_LIST = "outputList";

    private RelativeLayout rlSelectContainer;
    private FrameLayout titleBar;
    private TextView tvTitle;
    private TextView tvDone;
    private TextView tvSelect;

    private final ArrayList<LocalMedia> images = new ArrayList<>();
    private final ArrayList<LocalMedia> selectedImages = new ArrayList<>();//已选中的图片
    private int maxSelectCount;
    private boolean isMultiple;

    private boolean showing = true;
    private boolean isDone = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        setContentView(R.layout.img_activity_image_preview);
        initView();
    }

    public void initView() {
        images.addAll(getIntent().getParcelableArrayListExtra(EXTRA_PREVIEW_LIST));
        selectedImages.addAll(getIntent().getParcelableArrayListExtra(EXTRA_SELECTED_LIST));
        maxSelectCount = getIntent().getIntExtra(EXTRA_MAX_SELECT_COUNT, 0);
        isMultiple = getIntent().getBooleanExtra(EXTRA_IS_MULTIPLE, false);
        int position = getIntent().getIntExtra(EXTRA_POSITION, 0);

        rlSelectContainer = findViewById(R.id.rl_select_container);
        titleBar = findViewById(R.id.title_bar);
        titleBar.setBackgroundColor(0xDD393A3E);
        titleBar.setFocusable(true);
        titleBar.setClickable(true);
        tvTitle = findViewById(R.id.tv_title);
        tvDone = findViewById(R.id.tv_done);
        tvDone.setClipToOutline(true);
        tvDone.setOutlineProvider(new RoundViewOutlineProvider().dpRadius(4));
        tvDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isDone = true;
                if (!isMultiple) {
                    int pos = (int) tvSelect.getTag(R.id.img_tag_index);
                    selectedImages.clear();
                    selectedImages.add(images.get(pos).copy());
                }
                onBackPressed();
            }
        });
        findViewById(R.id.iv_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        tvSelect = findViewById(R.id.tv_select);
        tvSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int pos = (int) v.getTag(R.id.img_tag_index);
                LocalMedia media = images.get(pos);
                if (v.isSelected()) {
                    media.checkStatus = 0;
                    v.setSelected(false);
                    for (int i = 0; i < selectedImages.size(); i++) {
                        if (media.path.equals(selectedImages.get(i).path)) {
                            selectedImages.remove(i);
                            break;
                        }
                    }
                    updateDoneViewStatus();
                    return;
                }
                if (selectedImages.size() >= maxSelectCount) {
                    Context context = v.getContext().getApplicationContext();
                    Toast.makeText(context, context.getString(R.string.message_max_num, maxSelectCount), Toast.LENGTH_SHORT).show();
                    return;
                }
                media.checkStatus = 1;
                v.setSelected(true);
                selectedImages.add(media.copy());
                updateDoneViewStatus();
            }
        });

        ViewPager2 viewPager = findViewById(R.id.preview_pager);
        viewPager.setAdapter(new SimpleFragmentStateAdapter(this));
        viewPager.setCurrentItem(position);
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels);
            }

            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                tvTitle.setText(String.format(Locale.US, "%d/%d", position + 1, images.size()));
                tvSelect.setTag(R.id.img_tag_index, position);
                tvSelect.setSelected(images.get(position).isChecked());
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);
            }
        });
        updateDoneViewStatus();
        tvTitle.setText(String.format(Locale.US, "%d/%d", position + 1, images.size()));
        tvSelect.setTag(R.id.img_tag_index, position);
        tvSelect.setSelected(images.get(position).isChecked());
        tvSelect.setEnabled(isMultiple);
    }

    public void updateDoneViewStatus() {
        if (!isMultiple) return;
        boolean enable = selectedImages.size() > 0;
        tvDone.setEnabled(enable);
        tvDone.setText(enable ? getString(R.string.done_num, selectedImages.size(), maxSelectCount) : getString(R.string.done));
    }

    public class SimpleFragmentStateAdapter extends androidx.viewpager2.adapter.FragmentStateAdapter {

        public SimpleFragmentStateAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        public SimpleFragmentStateAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }

        public SimpleFragmentStateAdapter(@NonNull FragmentManager fragmentManager, @NonNull Lifecycle lifecycle) {
            super(fragmentManager, lifecycle);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return ImagePreviewFragment.getInstance(images.get(position).path);
        }

        @Override
        public int getItemCount() {
            return images.size();
        }
    }

    public void switchBarVisibility() {
        if (isTitleBarInAnimating
                || isTitleBarOutAnimating
                || isSelectContainerInAnimating
                || isSelectContainerOutAnimating) {
            return;
        }
        showing = !showing;
        if (showing) {
            titleBarIn();
            selectContainerIn();
        } else {
            titleBarOut();
            selectContainerOut();
        }
    }

    private boolean isTitleBarInAnimating = false;
    private boolean isTitleBarOutAnimating = false;
    private boolean isSelectContainerInAnimating = false;
    private boolean isSelectContainerOutAnimating = false;

    private void titleBarIn() {
        titleBar.setVisibility(View.VISIBLE);
        TranslateAnimation animation = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, -1.0f,
                Animation.RELATIVE_TO_SELF, 0.0f);
        animation.setDuration(250);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                isTitleBarInAnimating = true;
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                isTitleBarInAnimating = false;
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        titleBar.startAnimation(animation);
    }

    private void titleBarOut() {
        TranslateAnimation animation = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, -1.0f);
        animation.setDuration(250);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                isTitleBarOutAnimating = true;
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                titleBar.setVisibility(View.GONE);
                isTitleBarOutAnimating = false;
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        titleBar.startAnimation(animation);
    }

    private void selectContainerIn() {
        rlSelectContainer.setVisibility(View.VISIBLE);
        TranslateAnimation animation = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 1.0f,
                Animation.RELATIVE_TO_SELF, 0.0f);
        animation.setDuration(250);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                isSelectContainerInAnimating = true;
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                isSelectContainerInAnimating = false;
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        rlSelectContainer.startAnimation(animation);
    }

    private void selectContainerOut() {
        TranslateAnimation animation = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 1.0f);
        animation.setDuration(250);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                isSelectContainerOutAnimating = true;
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                rlSelectContainer.setVisibility(View.GONE);
                isSelectContainerOutAnimating = false;
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        rlSelectContainer.startAnimation(animation);
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        intent.putParcelableArrayListExtra(OUTPUT_LIST, selectedImages);
        setResult(isDone ? RESULT_OK : RESULT_CANCELED, intent);
        super.onBackPressed();
    }
}
