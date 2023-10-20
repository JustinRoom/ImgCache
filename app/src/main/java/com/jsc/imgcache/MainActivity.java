package com.jsc.imgcache;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.jsc.imgcache.databinding.ActivityMainBinding;
import com.jsc.imgcache.utils.ViewOutlineUtils;

import java.util.List;

import jsc.org.lib.img.selector.activity.ImageSelectorActivity;

public class MainActivity extends BaseActivity {

    ActivityMainBinding binding = null;
    ActivityResultLauncher<Intent> mSelectImagesLauncher = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSelectImagesLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                if (result.getResultCode() == RESULT_OK) {
                    List<String> paths = result.getData().getStringArrayListExtra(ImageSelectorActivity.OUTPUT_DATA);
                }
            }
        });
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.btnNetImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(v.getContext(), NetImgActivity.class));
            }
        });
        binding.btnLocalImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(v.getContext(), LocalImgActivity.class));
            }
        });
        binding.btnNetImg2Bitmap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(v.getContext(), NetImg2BitmapActivity.class));
            }
        });
        binding.btnSelectImages.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSelectImagesLauncher.launch(ImageSelectorActivity.createMultipleIntent(v.getContext(), 9));
            }
        });
        ViewOutlineUtils.applyEllipticOutline(binding.btnNetImg);
        ViewOutlineUtils.applyEllipticOutline(binding.btnLocalImg);
        ViewOutlineUtils.applyEllipticOutline(binding.btnNetImg2Bitmap);
        ViewOutlineUtils.applyEllipticOutline(binding.btnSelectImages);
    }

    @Override
    public void onLazyLoad() {

    }
}