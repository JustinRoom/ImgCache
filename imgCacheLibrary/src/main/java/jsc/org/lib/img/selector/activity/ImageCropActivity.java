package jsc.org.lib.img.selector.activity;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.opengl.GLES10;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import jsc.org.lib.img.ImgUtils;
import jsc.org.lib.img.R;
import jsc.org.lib.img.crop.CropImageView;
import jsc.org.lib.img.selector.provider.RoundViewOutlineProvider;

public class ImageCropActivity extends AppCompatActivity {
    public static final String EXTRA_PATH = "extraPath";
    public static final String OUTPUT_PATH = "outputPath";
    private static final int SIZE_DEFAULT = 2048;
    private static final int SIZE_LIMIT = 4096;

    private CropImageView civCropImage;

    private Uri sourceUri;
    private Uri saveUri;

    private final Handler mHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.img_activity_image_crop);
        String path = getIntent().getStringExtra(EXTRA_PATH);
        sourceUri = Uri.fromFile(new File(path));
        findViewById(R.id.title_bar).setBackgroundColor(0xDD393A3E);
        findViewById(R.id.iv_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        TextView tvDone = findViewById(R.id.tv_done);
        tvDone.setClipToOutline(true);
        tvDone.setOutlineProvider(new RoundViewOutlineProvider().dpRadius(4));
        tvDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ProgressDialog.show(ImageCropActivity.this, null, getString(R.string.save_ing), true, false);
                saveUri = createSaveUri();
                saveOutput(civCropImage.getCroppedBitmap());
            }
        });
        civCropImage = findViewById(R.id.civ_crop_image);
        civCropImage.setHandleSizeInDp(10);
        loadImage();
    }

    private Uri createSaveUri() {
        ContentValues contentValues = new ContentValues(2);
        contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, "image_crop_" + System.currentTimeMillis() + ".JPEG");
        contentValues.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        return getContentResolver().insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues);
    }

    public void loadImage() {
        int exifRotation = ImgUtils.getExifRotation(ImgUtils.getFromMediaUri(this, getContentResolver(), sourceUri));
        InputStream is = null;
        try {
            int sampleSize = calculateBitmapSampleSize(sourceUri);
            is = getContentResolver().openInputStream(sourceUri);
            BitmapFactory.Options option = new BitmapFactory.Options();
            option.inSampleSize = sampleSize;
            Bitmap sizeBitmap = BitmapFactory.decodeStream(is, null, option);
            if (sizeBitmap == null) return;
            Matrix matrix = getRotateMatrix(sizeBitmap, exifRotation % 360);
            Bitmap rotated = Bitmap.createBitmap(sizeBitmap, 0, 0, sizeBitmap.getWidth(), sizeBitmap.getHeight(), matrix, true);
            civCropImage.setImageBitmap(rotated);
        } catch (IOException | OutOfMemoryError e) {
            e.printStackTrace();
        } finally {
            ImgUtils.closeSilently(is);
        }
    }

    public Matrix getRotateMatrix(@NonNull Bitmap bitmap, int rotation) {
        Matrix matrix = new Matrix();
        if (rotation != 0) {
            int cx = bitmap.getWidth() / 2;
            int cy = bitmap.getHeight() / 2;
            matrix.preTranslate(-cx, -cy);
            matrix.postRotate(rotation);
            matrix.postTranslate(bitmap.getWidth() / 2.0f, bitmap.getHeight() / 2.0f);
        }
        return matrix;
    }

    private int calculateBitmapSampleSize(Uri bitmapUri) throws IOException {
        InputStream is = null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        try {
            is = getContentResolver().openInputStream(bitmapUri);
            BitmapFactory.decodeStream(is, null, options); // Just get image size
        } finally {
            ImgUtils.closeSilently(is);
        }

        int maxSize = getMaxImageSize();
        int sampleSize = 1;
        while (options.outHeight / sampleSize > maxSize || options.outWidth / sampleSize > maxSize) {
            sampleSize = sampleSize << 1;
        }
        return sampleSize;
    }

    private int getMaxImageSize() {
        int textureLimit = getMaxTextureSize();
        if (textureLimit == 0) {
            return SIZE_DEFAULT;
        } else {
            return Math.min(textureLimit, SIZE_LIMIT);
        }
    }

    private int getMaxTextureSize() {
        int[] maxSize = new int[1];
        GLES10.glGetIntegerv(GLES10.GL_MAX_TEXTURE_SIZE, maxSize, 0);
        return maxSize[0];
    }

    private void saveOutput(Bitmap croppedImage) {
        if (saveUri != null) {
            OutputStream outputStream = null;
            try {
                outputStream = getContentResolver().openOutputStream(saveUri);
                if (outputStream != null) {
                    croppedImage.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                ImgUtils.closeSilently(outputStream);
            }
            String path = ImgUtils.queryImagePath(getApplicationContext(), saveUri);
            setResult(RESULT_OK, new Intent().putExtra(OUTPUT_PATH, path));
        }
        final Bitmap b = croppedImage;
        mHandler.post(new Runnable() {
            public void run() {
                b.recycle();
            }
        });
        finish();
    }
}
