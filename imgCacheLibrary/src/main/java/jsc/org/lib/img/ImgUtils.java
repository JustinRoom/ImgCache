/*
 * Copyright (c) 2023.
 *
 * Author: JiangShiCheng
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jsc.org.lib.img;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.opengl.GLES10;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import java.io.Closeable;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public final class ImgUtils {
    private static final int SIZE_DEFAULT = 2048;
    private static final int SIZE_LIMIT = 4096;
    public static int sInputImageWidth = 0;
    public static int sInputImageHeight = 0;

    public static int[] formatSize(String baseOn, int baseWidth, int baseHeight, int value) {
        int base = "width".equals(baseOn) ? baseWidth : baseHeight;
        int multi = value / base;
        if (multi * base < value) {
            multi++;
        }
        multi = Math.max(1, multi);
        return new int[]{baseWidth * multi, baseHeight * multi};
    }

    public static void asBitmap(String netUrl, ImgCallback callback) {
        ImgCacheManager.defaultImgCache().asBitmap(netUrl, callback);
    }

    private static final String SCHEME_FILE = "file";
    private static final String SCHEME_CONTENT = "content";

    public static void closeSilently(@Nullable Closeable c) {
        if (c == null) return;
        try {
            c.close();
        } catch (Throwable t) {
            // Do nothing
        }
    }

    public static int getExifRotation(File imageFile) {
        if (imageFile == null) return 0;
        try {
            ExifInterface exif = new ExifInterface(imageFile.getAbsolutePath());
            // We only recognize a subset of orientation tag values
            switch (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return 90;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return 180;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return 270;
                default:
                    return ExifInterface.ORIENTATION_UNDEFINED;
            }
        } catch (IOException e) {
            return 0;
        }
    }

    public static int getExifRotation(Context context, Uri uri) {
        Cursor cursor = null;
        String[] projection = { MediaStore.Images.ImageColumns.ORIENTATION };
        try {
            cursor = context.getContentResolver().query(uri, projection, null, null, null);
            if (cursor == null || !cursor.moveToFirst()) {
                return 0;
            }
            return cursor.getInt(0);
        } catch (RuntimeException ignored) {
            return 0;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public static boolean copyExifRotation(File sourceFile, File destFile) {
        if (sourceFile == null || destFile == null) return false;
        try {
            ExifInterface exifSource = new ExifInterface(sourceFile.getAbsolutePath());
            ExifInterface exifDest = new ExifInterface(destFile.getAbsolutePath());
            exifDest.setAttribute(ExifInterface.TAG_ORIENTATION, exifSource.getAttribute(ExifInterface.TAG_ORIENTATION));
            exifDest.saveAttributes();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static int getExifOrientation(Context context, Uri uri) {
        String authority = uri.getAuthority().toLowerCase();
        int orientation;
        if (authority.endsWith("media")) {
            orientation = getExifRotation(context, uri);
        } else {
            orientation = getExifRotation(queryImageFile(context, uri));
        }
        return orientation;
    }

    @Nullable
    public static File getFromMediaUri(Context context, ContentResolver resolver, Uri uri) {
        if (uri == null) return null;

        if (SCHEME_FILE.equals(uri.getScheme())) {
            return new File(uri.getPath());
        } else if (SCHEME_CONTENT.equals(uri.getScheme())) {
            final String[] filePathColumn = {MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.DISPLAY_NAME};
            Cursor cursor = null;
            try {
                cursor = resolver.query(uri, filePathColumn, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    final int columnIndex = (uri.toString().startsWith("content://com.google.android.gallery3d")) ?
                            cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME) :
                            cursor.getColumnIndex(MediaStore.MediaColumns.DATA);
                    // Picasa images on API 13+
                    if (columnIndex != -1) {
                        String filePath = cursor.getString(columnIndex);
                        if (!TextUtils.isEmpty(filePath)) {
                            return new File(filePath);
                        }
                    }
                }
            } catch (IllegalArgumentException e) {
                // Google Drive images
                return getFromMediaUriPfd(context, resolver, uri);
            } catch (SecurityException ignored) {
                // Nothing we can do
            } finally {
                if (cursor != null) cursor.close();
            }
        }
        return null;
    }

    private static String getTempFilename(Context context) throws IOException {
        File outputDir = context.getCacheDir();
        File outputFile = File.createTempFile("image", "tmp", outputDir);
        return outputFile.getAbsolutePath();
    }

    @Nullable
    private static File getFromMediaUriPfd(Context context, ContentResolver resolver, Uri uri) {
        if (uri == null) return null;

        FileInputStream input = null;
        FileOutputStream output = null;
        try {
            ParcelFileDescriptor pfd = resolver.openFileDescriptor(uri, "r");
            FileDescriptor fd = pfd.getFileDescriptor();
            input = new FileInputStream(fd);

            String tempFilename = getTempFilename(context);
            output = new FileOutputStream(tempFilename);

            int read;
            byte[] bytes = new byte[4096];
            while ((read = input.read(bytes)) != -1) {
                output.write(bytes, 0, read);
            }
            return new File(tempFilename);
        } catch (IOException ignored) {
            // Nothing we can do
        } finally {
            closeSilently(input);
            closeSilently(output);
        }
        return null;
    }

    public static File queryImageFile(Context context, Uri uri) {
        String path = queryImagePath(context, uri);
        return TextUtils.isEmpty(path) ? null : new File(path);
    }

    public static String queryImagePath(Context context, Uri uri) {
        String[] projection = new String[]{
                MediaStore.MediaColumns._ID,
                MediaStore.Images.ImageColumns.ORIENTATION,
                MediaStore.Images.Media.DATA
        };
        Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
            try {
                String path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA));
                if (new File(path).exists()) {
                    return path;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            cursor.close();
        }
        return null;
    }

    /**
     * Copy EXIF info to new file
     * <p>
     * =========================================
     * <p>
     * NOTE: PNG cannot not have EXIF info.
     * <p>
     * source: JPEG, save: JPEG
     * copies all EXIF data
     * <p>
     * source: JPEG, save: PNG
     * saves no EXIF data
     * <p>
     * source: PNG, save: JPEG
     * saves only width and height EXIF data
     * <p>
     * source: PNG, save: PNG
     * saves no EXIF data
     * <p>
     * =========================================
     */
    public static void copyExifInfo(Context context, Uri sourceUri, Uri saveUri, int outputWidth,
                                    int outputHeight) {
        if (sourceUri == null || saveUri == null) return;
        try {
            String sourcePath = queryImagePath(context, sourceUri);
            String savePath = queryImagePath(context, saveUri);
            File sourceFile = TextUtils.isEmpty(sourcePath) ? null : new File(sourcePath);
            File saveFile = TextUtils.isEmpty(sourcePath) ? null : new File(savePath);
            if (sourceFile == null || saveFile == null) {
                return;
            }
            ExifInterface sourceExif = new ExifInterface(sourcePath);
            List<String> tags = new ArrayList<>();
            tags.add(ExifInterface.TAG_DATETIME);
            tags.add(ExifInterface.TAG_FLASH);
            tags.add(ExifInterface.TAG_FOCAL_LENGTH);
            tags.add(ExifInterface.TAG_GPS_ALTITUDE);
            tags.add(ExifInterface.TAG_GPS_ALTITUDE_REF);
            tags.add(ExifInterface.TAG_GPS_DATESTAMP);
            tags.add(ExifInterface.TAG_GPS_LATITUDE);
            tags.add(ExifInterface.TAG_GPS_LATITUDE_REF);
            tags.add(ExifInterface.TAG_GPS_LONGITUDE);
            tags.add(ExifInterface.TAG_GPS_LONGITUDE_REF);
            tags.add(ExifInterface.TAG_GPS_PROCESSING_METHOD);
            tags.add(ExifInterface.TAG_GPS_TIMESTAMP);
            tags.add(ExifInterface.TAG_MAKE);
            tags.add(ExifInterface.TAG_MODEL);
            tags.add(ExifInterface.TAG_WHITE_BALANCE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                tags.add(ExifInterface.TAG_EXPOSURE_TIME);
                //noinspection deprecation
                tags.add(ExifInterface.TAG_APERTURE);
                //noinspection deprecation
                tags.add(ExifInterface.TAG_ISO);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                tags.add(ExifInterface.TAG_DATETIME_DIGITIZED);
                tags.add(ExifInterface.TAG_SUBSEC_TIME);
                //noinspection deprecation
                tags.add(ExifInterface.TAG_SUBSEC_TIME_DIG);
                //noinspection deprecation
                tags.add(ExifInterface.TAG_SUBSEC_TIME_ORIG);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                tags.add(ExifInterface.TAG_F_NUMBER);
                tags.add(ExifInterface.TAG_ISO_SPEED_RATINGS);
                tags.add(ExifInterface.TAG_SUBSEC_TIME_DIGITIZED);
                tags.add(ExifInterface.TAG_SUBSEC_TIME_ORIGINAL);
            }

            ExifInterface saveExif = new ExifInterface(savePath);
            String value;
            for (String tag : tags) {
                value = sourceExif.getAttribute(tag);
                if (!TextUtils.isEmpty(value)) {
                    saveExif.setAttribute(tag, value);
                }
            }
            saveExif.setAttribute(ExifInterface.TAG_IMAGE_WIDTH, String.valueOf(outputWidth));
            saveExif.setAttribute(ExifInterface.TAG_IMAGE_LENGTH, String.valueOf(outputHeight));
            saveExif.setAttribute(ExifInterface.TAG_ORIENTATION,
                    String.valueOf(ExifInterface.ORIENTATION_UNDEFINED));

            saveExif.saveAttributes();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static int getMaxSize() {
        int maxSize = SIZE_DEFAULT;
        int[] arr = new int[1];
        GLES10.glGetIntegerv(GLES10.GL_MAX_TEXTURE_SIZE, arr, 0);
        if (arr[0] > 0) {
            maxSize = Math.min(arr[0], SIZE_LIMIT);
        }
        return maxSize;
    }

    public static void updateGalleryInfo(Context context, Uri uri) {
        if (!ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            return;
        }

        ContentValues values = new ContentValues();
        File file = queryImageFile(context, uri);
        if (file != null && file.exists()) {
            values.put(MediaStore.Images.Media.SIZE, file.length());
        }
        ContentResolver resolver = context.getContentResolver();
        resolver.update(uri, values, null, null);
    }

    public static Bitmap getScaledBitmap(Bitmap bitmap, int outWidth, int outHeight) {
        int currentWidth = bitmap.getWidth();
        int currentHeight = bitmap.getHeight();
        Matrix scaleMatrix = new Matrix();
        scaleMatrix.postScale((float) outWidth / (float) currentWidth,
                (float) outHeight / (float) currentHeight);
        return Bitmap.createBitmap(bitmap, 0, 0, currentWidth, currentHeight, scaleMatrix, true);
    }

    public static Bitmap decodeSampledBitmapFromUri(Context context, Uri sourceUri, int requestSize) {
        InputStream stream = null;
        Bitmap bitmap = null;
        try {
            stream = context.getContentResolver().openInputStream(sourceUri);
            if (stream != null) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = calculateInSampleSize(context, sourceUri, requestSize);
                options.inJustDecodeBounds = false;
                bitmap = BitmapFactory.decodeStream(stream, null, options);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                if (stream != null) {
                    stream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return bitmap;
    }

    public static int calculateInSampleSize(Context context, Uri sourceUri, int requestSize) {
        InputStream is = null;
        // check image size
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        try {
            is = context.getContentResolver().openInputStream(sourceUri);
            BitmapFactory.decodeStream(is, null, options);
        } catch (FileNotFoundException ignored) {
        } finally {
            closeSilently(is);
        }
        int inSampleSize = 1;
        sInputImageWidth = options.outWidth;
        sInputImageHeight = options.outHeight;
        while (options.outWidth / inSampleSize > requestSize
                || options.outHeight / inSampleSize > requestSize) {
            inSampleSize *= 2;
        }
        return inSampleSize;
    }
}
