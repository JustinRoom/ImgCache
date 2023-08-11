package com.jsc.imgcache;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.jsc.imgcache.databinding.ActivityMainBinding;
import com.jsc.imgcache.utils.ViewOutlineUtils;

public class MainActivity extends BaseActivity {

    ActivityMainBinding binding = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        ViewOutlineUtils.applyEllipticOutline(binding.btnNetImg);
        ViewOutlineUtils.applyEllipticOutline(binding.btnLocalImg);
        ViewOutlineUtils.applyEllipticOutline(binding.btnNetImg2Bitmap);
    }

    @Override
    public void onLazyLoad() {

    }
}