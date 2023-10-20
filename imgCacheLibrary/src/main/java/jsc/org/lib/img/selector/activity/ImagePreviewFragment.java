package jsc.org.lib.img.selector.activity;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import jsc.org.lib.img.ImgCacheExecutor;
import jsc.org.lib.img.R;

public final class ImagePreviewFragment extends Fragment {

    public static final String PATH = "path";

    public static ImagePreviewFragment getInstance(String path) {
        ImagePreviewFragment fragment = new ImagePreviewFragment();
        Bundle bundle = new Bundle();
        bundle.putString(PATH, path);
        fragment.setArguments(bundle);
        return fragment;
    }

    @NonNull
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View contentView = inflater.inflate(R.layout.img_fragment_image_preview, container, false);
        final ImageView imageView = contentView.findViewById(R.id.preview_image);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Activity activity = getActivity();
                if (activity instanceof ImagePreviewActivity) {
                    ((ImagePreviewActivity) activity).switchBarVisibility();
                }
            }
        });
        String url = getArguments() == null ? "" : getArguments().getString(PATH);
        if (TextUtils.isEmpty(url)) {
            imageView.setImageResource(R.mipmap.img_ic_placeholder);
        } else {
            ImgCacheExecutor.with("ImageSelector")
                    .localSource()
                    .loadFromMemoryCache(true)
                    .loadFromDiskCache(true)
                    .url(url)
                    .size(960, 1280)
                    .into(imageView);
        }
        return contentView;
    }
}
