package jsc.org.lib.img.selector.adapter;

import android.annotation.SuppressLint;
import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import jsc.org.lib.img.R;
import jsc.org.lib.img.selector.activity.ImageSelectorActivity;
import jsc.org.lib.img.selector.model.LocalMedia;
import jsc.org.lib.img.selector.provider.RoundViewOutlineProvider;

public class ImageListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public static final int TYPE_CAMERA = 1;
    public static final int TYPE_PICTURE = 2;

    private final boolean showCamera;
    private final int selectMode;

    private final List<LocalMedia> images = new ArrayList<>();

    private OnImageOptActionListener mActionListener;

    public ImageListAdapter(int mode, boolean showCamera) {
        this.selectMode = mode;
        this.showCamera = showCamera;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void bindImages(List<LocalMedia> images) {
        this.images.clear();
        if (images != null) {
            this.images.addAll(images);
        }
        notifyDataSetChanged();
    }

    public void addLocalMedia(LocalMedia media) {
        images.add(0, media);
        notifyItemInserted(showCamera ? 1 : 0);
    }

    @Override
    public int getItemViewType(int position) {
        if (showCamera && position == 0) {
            return TYPE_CAMERA;
        } else {
            return TYPE_PICTURE;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_CAMERA) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.img_list_item_camera, parent, false);
            return new HeaderViewHolder(view, mActionListener);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.img_list_item_picture, parent, false);
            return new ContentViewHolder(view, selectMode == ImageSelectorActivity.MODE_MULTIPLE, mActionListener);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == TYPE_CAMERA) {

        } else {
            final ContentViewHolder mContentHolder = (ContentViewHolder) holder;
            mContentHolder.bindData(getMediaAt(position));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads);
        } else {
            final ContentViewHolder mContentHolder = (ContentViewHolder) holder;
            mContentHolder.updateCheckStatus();
        }
    }

    @Override
    public int getItemCount() {
        return showCamera ? images.size() + 1 : images.size();
    }

    public LocalMedia getMediaAt(int position) {
        return images.get(getMediaRealPosition(position));
    }

    public int getMediaRealPosition(int position) {
        return showCamera ? position - 1 : position;
    }

    public List<LocalMedia> getImages() {
        return images;
    }

    public List<LocalMedia> getSelectedImages() {
        List<LocalMedia> list = new ArrayList<>();
        for (LocalMedia media : images) {
            if (media.isChecked()) list.add(media);
        }
        return list;
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        OnImageOptActionListener listener;

        public HeaderViewHolder(View itemView, OnImageOptActionListener l) {
            super(itemView);
            listener = l;
            itemView.setClipToOutline(true);
            itemView.setOutlineProvider(new RoundViewOutlineProvider().dpRadius(8));
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onTakePhoto();
                    }
                }
            });
        }
    }

    public static class ContentViewHolder extends RecyclerView.ViewHolder {
        public ImageView ivPhoto;
        public ImageView ivCheckStatus;
        OnImageOptActionListener listener;
        LocalMedia mLocalMedia = null;

        public ContentViewHolder(View itemView, boolean enableCheck, OnImageOptActionListener l) {
            super(itemView);
            listener = l;
            ivPhoto = itemView.findViewById(R.id.iv_photo);
            ivCheckStatus = itemView.findViewById(R.id.iv_check_status);
            ivCheckStatus.setVisibility(enableCheck ? View.VISIBLE : View.GONE);
            ivCheckStatus.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onPhotoCheck(getAdapterPosition());
                    }
                }
            });
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onPhotoClick(getAdapterPosition());
                    }
                }
            });
            ivPhoto.setClipToOutline(true);
            ivPhoto.setOutlineProvider(new RoundViewOutlineProvider().dpRadius(8));
        }

        void bindData(LocalMedia media) {
            mLocalMedia = media;
            ivPhoto.setImageResource(R.mipmap.img_ic_placeholder);
            updateCheckStatus();
        }

        void updateCheckStatus() {
            boolean isChecked = mLocalMedia.isChecked();
            ivCheckStatus.setSelected(isChecked);
            if (isChecked) {
                ivPhoto.setColorFilter(0x20000000, PorterDuff.Mode.SRC_ATOP);
            } else {
                ivPhoto.setColorFilter(0x80000000, PorterDuff.Mode.SRC_ATOP);
            }
        }
    }

    public interface OnImageOptActionListener {

        void onTakePhoto();

        void onPhotoCheck(int position);

        void onPhotoClick(int position);
    }

    public void setOnImageSelectChangedListener(OnImageOptActionListener imageSelectChangedListener) {
        this.mActionListener = imageSelectChangedListener;
    }
}
