package jsc.org.lib.img.selector.activity;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jsc.org.lib.img.ILazyLoader;
import jsc.org.lib.img.ImgCacheExecutor;
import jsc.org.lib.img.ImgCacheManager;
import jsc.org.lib.img.ImgUtils;
import jsc.org.lib.img.LazilyLoadableRecyclerView;
import jsc.org.lib.img.R;
import jsc.org.lib.img.selector.adapter.ImageListAdapter;
import jsc.org.lib.img.selector.model.LocalMedia;
import jsc.org.lib.img.selector.model.LocalMediaFolder;
import jsc.org.lib.img.selector.provider.RoundViewOutlineProvider;

public final class ImageSelectorActivity extends AppCompatActivity {
    public final static String BUNDLE_CAMERA_PATH = "CameraPath";
    public final static String OUTPUT_DATA = "outputList";
    public final static String EXTRA_SELECT_MODE = "SelectMode";
    public final static String EXTRA_SHOW_CAMERA = "ShowCamera";
    public final static String EXTRA_ENABLE_PREVIEW = "EnablePreview";
    public final static String EXTRA_ENABLE_CROP = "EnableCrop";
    public final static String EXTRA_MAX_SELECT_NUM = "MaxSelectNum";
    public final static int MODE_MULTIPLE = 1;
    public final static int MODE_SINGLE = 2;
    private final static String[] IMAGE_PROJECTION = {
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media._ID};

    private int maxSelectCount = 9;
    private int selectMode = MODE_MULTIPLE;
    private boolean showCamera = true;
    private boolean enablePreview = true;
    private boolean enableCrop = true;
    private int spanCount = 3;

    private ActivityResultLauncher<Intent> mViewPhotoLauncher = null;
    private ActivityResultLauncher<Intent> mTakePhotoLauncher = null;
    private ActivityResultLauncher<Intent> mCropPhotoLauncher = null;
    private FoldersPopupWindow mFoldersPopupWindow;
    private ImageListAdapter adapter;
    private String cameraPath;

    public static Intent createMultipleIntent(Context context, int maxSelectNum) {
        Intent intent = new Intent(context, ImageSelectorActivity.class);
        intent.putExtra(EXTRA_MAX_SELECT_NUM, maxSelectNum);
        intent.putExtra(EXTRA_SELECT_MODE, MODE_MULTIPLE);
        return intent;
    }

    public static Intent createSingleIntent(Context context, int maxSelectNum) {
        Intent intent = new Intent(context, ImageSelectorActivity.class);
        intent.putExtra(EXTRA_MAX_SELECT_NUM, maxSelectNum);
        intent.putExtra(EXTRA_SELECT_MODE, MODE_SINGLE);
        return intent;
    }

    private FrameLayout titleBar;
    private TextView tvDone;
    private LazilyLoadableRecyclerView recyclerView;
    private ConstraintLayout clBottomContainer;
    private LinearLayout lyFolderName;
    private TextView tvFolderName;
    private TextView tvPreview;

    @Override
    protected void onStart() {
        super.onStart();
        ImgCacheManager.initImgCache("ImageSelector");
    }

    private boolean isMultipleMode() {
        return selectMode == MODE_MULTIPLE;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        registerViewPhotoLauncher();
        registerTakePhotoLauncher();
        registerCropPhotoLauncher();
        setContentView(R.layout.img_activity_image_selector);
        spanCount = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE ? 6 : 3;
        maxSelectCount = getIntent().getIntExtra(EXTRA_MAX_SELECT_NUM, 9);
        selectMode = getIntent().getIntExtra(EXTRA_SELECT_MODE, MODE_MULTIPLE);
        showCamera = getIntent().getBooleanExtra(EXTRA_SHOW_CAMERA, true);
        enablePreview = getIntent().getBooleanExtra(EXTRA_ENABLE_PREVIEW, true);
        enableCrop = getIntent().getBooleanExtra(EXTRA_ENABLE_CROP, true);
        if (savedInstanceState != null) {
            cameraPath = savedInstanceState.getString(BUNDLE_CAMERA_PATH);
        }
        initViews();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            loadImages();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0x10);
        }
    }

    private void loadImages() {
        LoaderManager.getInstance(this).initLoader(0, null, new LoaderManager.LoaderCallbacks<Cursor>() {
            @NonNull
            @Override
            public Loader<Cursor> onCreateLoader(int id, Bundle args) {
                return new CursorLoader(getApplicationContext(), MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        IMAGE_PROJECTION, MediaStore.Images.Media.MIME_TYPE + "=? or "
                        + MediaStore.Images.Media.MIME_TYPE + "=?",
                        new String[]{"image/jpeg", "image/png"}, IMAGE_PROJECTION[2] + " DESC");
            }

            @Override
            public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor cursor) {
                Log.i("ImageSelector", "onLoadFinished: ");
                if (cursor == null || cursor.isClosed()) return;
                LocalMediaFolder all = folderMap.get("empty");
                if (all == null) {
                    all = new LocalMediaFolder();
                    all.name = "All Images";
                    all.path = "empty";
                    folders.add(all);
                    folderMap.put("empty", all);
                }
                while (!cursor.isClosed() && cursor.moveToNext()) {
                    // 获取图片的路径
                    int index = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
                    if (index < 0) continue;
                    String path = cursor.getString(index);
                    File file = new File(path);
                    if (!file.exists())
                        continue;
                    // 获取该图片的目录路径名
                    File parent = file.getParentFile();
                    if (parent == null || !parent.exists())
                        continue;

                    String dirPath = parent.getPath();
                    LocalMediaFolder dir = folderMap.get(dirPath);
                    if (dir == null) {
                        dir = new LocalMediaFolder();
                        dir.name = parent.getName();
                        dir.path = dirPath;
                        folders.add(dir);
                        folderMap.put(dirPath, dir);
                    }
                    LocalMedia media = new LocalMedia(path);
                    dir.images.add(media);
                    all.images.add(media);
                }

                all.selected = true;
                tvFolderName.setText(all.name);
                adapter.bindImages(all.images);
                recyclerView.loadImgDelay250();
                mFoldersPopupWindow.updateFolders(folders);
                cursor.close();
            }

            @Override
            public void onLoaderReset(@NonNull Loader<Cursor> loader) {
                Log.i("ImageSelector", "onLoaderReset: ");
            }
        });
    }

    private void registerViewPhotoLauncher() {
        if (mViewPhotoLauncher == null) {
            mViewPhotoLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    List<LocalMedia> newSelectedImages = result.getData().getParcelableArrayListExtra(ImagePreviewActivity.OUTPUT_LIST);
                    if (result.getResultCode() == RESULT_OK) {
                        finishSelect(newSelectedImages);
                    } else {
                        updateSelectedImages(newSelectedImages);
                    }
                }
            });
        }
    }

    private void registerTakePhotoLauncher() {
        if (mTakePhotoLauncher == null) {
            mTakePhotoLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() != RESULT_OK) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            getContentResolver().delete(mPhotoUri, null);
                        } else {
                            //do nothing
                        }
                        return;
                    }
                    String path = ImgUtils.queryImagePath(getApplicationContext(), mPhotoUri);
                    if (enableCrop) {
                        addImage(path, false);
                        cropImage(path);
                        return;
                    }
                    if (isMultipleMode()) {
                        addImage(path, true);
                    } else {
                        finishSelect(path);
                    }
                }
            });
        }
    }

    private void registerCropPhotoLauncher() {
        if (mCropPhotoLauncher == null) {
            mCropPhotoLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() != RESULT_OK) {
                        return;
                    }
                    String path = result.getData().getStringExtra(ImageCropActivity.OUTPUT_PATH);
                    if (isMultipleMode()) {
                        addImage(path, true);
                    } else {
                        finishSelect(path);
                    }
                }
            });
        }
    }

    private void initViews() {
        titleBar = findViewById(R.id.title_bar);
        findViewById(R.id.iv_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        ((TextView) findViewById(R.id.tv_title)).setText(R.string.picture);
        tvDone = findViewById(R.id.tv_done);
        tvDone.setClipToOutline(true);
        tvDone.setOutlineProvider(new RoundViewOutlineProvider().dpRadius(4));
        tvDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finishSelect(getSelectedImages());
            }
        });
        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.addItemDecoration(new BlankItemDecoration(this, 2));
        recyclerView.setLayoutManager(new GridLayoutManager(this, spanCount));
        recyclerView.setAnticipatedImgSize("width", 40, 40, getResources().getDisplayMetrics().widthPixels / spanCount);
        recyclerView.setLazyLoader(new ILazyLoader() {
            @Override
            public void lazyLoad(int position, @NonNull RecyclerView.ViewHolder holder, int anticipatedImgWidth, int anticipatedImgHeight) {
                if (holder instanceof ImageListAdapter.ContentViewHolder) {
                    ImgCacheExecutor.with("ImageSelector")
                            .localSource()
                            .loadFromMemoryCache(true)
                            .loadFromDiskCache(true)
                            .url(adapter.getMediaAt(position).path)
                            .size(anticipatedImgWidth, anticipatedImgHeight)
                            .into(((ImageListAdapter.ContentViewHolder) holder).ivPhoto);
                }
            }
        });
        adapter = new ImageListAdapter(selectMode, showCamera);
        adapter.setOnImageSelectChangedListener(new ImageListAdapter.OnImageOptActionListener() {

            @Override
            public void onTakePhoto() {
                takePhoto();
            }

            @Override
            public void onPhotoCheck(int position) {
                int selectedSize = getSelectedImages().size();
                LocalMedia media = adapter.getMediaAt(position);
                if (media.isChecked()) {
                    media.checkStatus = 0;
                    adapter.notifyItemChanged(position, String.valueOf(position));
                    selectedSize--;
                    updateUI(selectedSize);
                    return;
                }
                if (selectedSize >= maxSelectCount) {
                    Context context = getApplicationContext();
                    Toast.makeText(context, context.getString(R.string.message_max_num, maxSelectCount), Toast.LENGTH_SHORT).show();
                    return;
                }
                media.checkStatus = 1;
                adapter.notifyItemChanged(position, String.valueOf(position));
                selectedSize++;
                updateUI(selectedSize);
            }

            @Override
            public void onPhotoClick(int position) {
                previewImages(adapter.getImages(), getSelectedImages(), adapter.getMediaRealPosition(position));
            }
        });
        recyclerView.setAdapter(adapter);
        clBottomContainer = findViewById(R.id.cl_bottom_container);
        lyFolderName = findViewById(R.id.ly_folder_name);
        lyFolderName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mFoldersPopupWindow.isShowing()) {
                    mFoldersPopupWindow.dismiss();
                } else {
                    mFoldersPopupWindow.showAbove(clBottomContainer, findViewById(android.R.id.content));
                }
            }
        });
        tvFolderName = findViewById(R.id.tv_folder_name);
        mFoldersPopupWindow = new FoldersPopupWindow(this);
        mFoldersPopupWindow.setOnItemClickListener(new FoldersPopupWindow.OnSelectChangedListener() {
            @Override
            public void onChanged(LocalMediaFolder folder) {
                tvFolderName.setText(folder.name);
                adapter.bindImages(folder.images);
                recyclerView.loadImgDelay250();
            }
        });
        tvPreview = findViewById(R.id.tv_preview);
        tvPreview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                previewImages(getSelectedImages(), 0);
            }
        });
        tvDone.setEnabled(false);
        tvDone.setVisibility(isMultipleMode() ? View.VISIBLE : View.GONE);
        tvPreview.setVisibility(isMultipleMode() && enablePreview ? View.VISIBLE : View.GONE);
    }

    private void updateUI(int size) {
        boolean enable = size != 0;
        tvDone.setEnabled(enable);
        tvPreview.setEnabled(enable);
        if (enable) {
            tvDone.setText(getString(R.string.done_num, size, maxSelectCount));
            tvPreview.setText(getString(R.string.preview_num, size));
        } else {
            tvDone.setText(R.string.done);
            tvPreview.setText(R.string.preview);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ImgCacheManager.clearMemoryCache("ImageSelector");
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(BUNDLE_CAMERA_PATH, cameraPath);
    }

    private Uri mPhotoUri = null;

    private void takePhoto() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 0x11);
            return;
        }
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            ContentValues contentValues = new ContentValues(2);
            contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, "image_" + System.currentTimeMillis() + ".JPEG");
            contentValues.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            mPhotoUri = getContentResolver().insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, mPhotoUri);
            mTakePhotoLauncher.launch(intent);
        }
    }

    public void previewImages(List<LocalMedia> selectedImages, int startViewPosition) {
        previewImages(selectedImages, selectedImages, startViewPosition);
    }

    public void previewImages(List<LocalMedia> allImages, List<LocalMedia> selectedImages, int startViewPosition) {
        Intent intent = new Intent(this, ImagePreviewActivity.class);
        intent.putParcelableArrayListExtra(ImagePreviewActivity.EXTRA_PREVIEW_LIST, new ArrayList<>(allImages));
        intent.putParcelableArrayListExtra(ImagePreviewActivity.EXTRA_SELECTED_LIST, new ArrayList<>(selectedImages));
        intent.putExtra(ImagePreviewActivity.EXTRA_POSITION, startViewPosition);
        intent.putExtra(ImagePreviewActivity.EXTRA_MAX_SELECT_COUNT, maxSelectCount);
        intent.putExtra(ImagePreviewActivity.EXTRA_IS_MULTIPLE, isMultipleMode());
        mViewPhotoLauncher.launch(intent);
    }

    public void cropImage(String path) {
        Intent intent = new Intent(this, ImageCropActivity.class);
        intent.putExtra(ImageCropActivity.EXTRA_PATH, path);
        mCropPhotoLauncher.launch(intent);
    }

    /**
     * on select done
     *
     * @param medias
     */
    public void finishSelect(List<LocalMedia> medias) {
        ArrayList<String> images = new ArrayList<>();
        for (LocalMedia media : medias) {
            images.add(media.path);
        }
        onResult(images);
    }

    public void finishSelect(String path) {
        ArrayList<String> images = new ArrayList<>();
        images.add(path);
        onResult(images);
    }

    public void onResult(ArrayList<String> images) {
        setResult(RESULT_OK, new Intent().putStringArrayListExtra(OUTPUT_DATA, images));
        finish();
    }

    private static class BlankItemDecoration extends RecyclerView.ItemDecoration {

        private final int divider;

        public BlankItemDecoration(int divider) {
            this.divider = divider;
        }

        public BlankItemDecoration(Context context, int dpDivider) {
            this.divider = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dpDivider, context.getResources().getDisplayMetrics());
        }

        //获取分割线尺寸
        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            super.getItemOffsets(outRect, view, parent, state);
            outRect.set(divider, divider, divider, divider);
        }
    }

    //>>>>>>>>>>data area
    private final Map<String, LocalMediaFolder> folderMap = new HashMap<>();
    private final List<LocalMediaFolder> folders = new ArrayList<>();

    private ArrayList<LocalMedia> getSelectedImages() {
        ArrayList<LocalMedia> list = new ArrayList<>();
        for (LocalMedia media : folders.get(0).images) {
            if (media.isChecked()) list.add(media);
        }
        return list;
    }

    private void addImage(String path, boolean autoSelected) {
        File file = new File(path);
        if (!file.exists()) return;
        File parent = file.getParentFile();
        if (parent == null || !parent.exists()) {
            return;
        }
        int selectedCount = getSelectedImages().size();
        String dirPath = parent.getPath();
        LocalMediaFolder dir = folderMap.get(dirPath);
        boolean isNewDir = false;
        if (dir == null) {
            isNewDir = true;
            dir = new LocalMediaFolder();
            dir.name = parent.getName();
            dir.path = dirPath;
            folders.add(dir);
            folderMap.put(dirPath, dir);
        }
        LocalMedia media = new LocalMedia(path);
        if (autoSelected && selectedCount < maxSelectCount) {
            media.checkStatus = 1;
            selectedCount++;
            updateUI(selectedCount);
        }
        dir.images.add(0, media);
        if (isNewDir) {
            mFoldersPopupWindow.insert(folders.size() - 1);
        } else {
            int pos = -1;
            for (int i = 0; i < folders.size(); i++) {
                if (dirPath.equals(folders.get(i).path)) {
                    pos = i;
                    break;
                }
            }
            if (pos >= 0) {
                mFoldersPopupWindow.updateFirstImage(pos);
                mFoldersPopupWindow.updateCount(pos);
            }
        }
        folders.get(0).images.add(0, media);
        mFoldersPopupWindow.updateFirstImage(0);
        mFoldersPopupWindow.updateCount(0);
        //插入当前的列表
        if (dir.selected || folders.get(0).selected) {
            adapter.addLocalMedia(media);
            recyclerView.loadImgDelay250();
        }
    }

    private void updateSelectedImages(List<LocalMedia> newSelectedImages) {
        List<LocalMedia> oldSelectImages = getSelectedImages();
        if (oldSelectImages.size() > 0 && oldSelectImages.size() == newSelectedImages.size()) {
            //检查是否有变动
            Map<String, LocalMedia> map = new HashMap<>();
            for (LocalMedia media : oldSelectImages) {
                map.put(media.path, media);
            }
            for (LocalMedia media : newSelectedImages) {
                map.put(media.path, media);
            }
            if (map.size() == oldSelectImages.size()) return;
        }

        for (LocalMedia media : folders.get(0).images) {
            int checkStatus = 0;
            for (LocalMedia select : newSelectedImages) {
                if (media.path.equals(select.path)) {
                    checkStatus = select.checkStatus;
                }
            }
            media.checkStatus = checkStatus;
        }
        adapter.notifyDataSetChanged();
        recyclerView.loadImgDelay250();
        updateUI(newSelectedImages.size());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 0x10) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                loadImages();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, requestCode);
            }
        } else if (requestCode == 0x11 && ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            takePhoto();
        }
    }
}
