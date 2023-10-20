package jsc.org.lib.img.selector.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import jsc.org.lib.img.ImgCacheExecutor;
import jsc.org.lib.img.R;
import jsc.org.lib.img.selector.model.LocalMediaFolder;
import jsc.org.lib.img.selector.provider.RoundViewOutlineProvider;

public class ImageFolderAdapter extends RecyclerView.Adapter<ImageFolderAdapter.ViewHolder> {

    private List<LocalMediaFolder> folders = null;
    private int size = 0;
    private final OnItemClickListener listener;
    private int lastSelectedPosition = -1;

    public ImageFolderAdapter(int size, OnItemClickListener listener) {
        this.size = size;
        this.listener = listener;
    }

    public void setFolders(List<LocalMediaFolder> folders) {
        this.folders = folders;
        lastSelectedPosition = -1;
        if (folders != null) {
            for (int i = 0; i < folders.size(); i++) {
                if (folders.get(i).selected) {
                    lastSelectedPosition = i;
                    break;
                }
            }
        }
        notifyDataSetChanged();
    }

    public boolean selectedMedia(int pos) {
        if (lastSelectedPosition == pos) return false;
        if (lastSelectedPosition >= 0) {
            getItemAt(lastSelectedPosition).selected = false;
            notifyItemChanged(lastSelectedPosition, "updateSelectStatus");
        }
        getItemAt(pos).selected = false;
        notifyItemChanged(pos, "updateSelectStatus");
        lastSelectedPosition = pos;
        return true;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.img_list_item_folder, parent, false);
        return new ViewHolder(itemView, size);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.bindData(getItemAt(position));
        holder.itemView.setTag(R.id.img_tag_index, position);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int pos = (int) v.getTag(R.id.img_tag_index);
                if (listener != null) {
                    listener.onItemClick(pos);
                }
            }
        });
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (!payloads.isEmpty()) {
            String action = payloads.get(0).toString();
            switch (action) {
                case "updateSelectStatus":
                    holder.updateSelectedStatus();
                    break;
                case "updateFirstImage":
                    holder.updateFirstImage();
                    break;
                case "updateCount":
                    holder.updateCount();
                    break;
            }
        } else {
            super.onBindViewHolder(holder, position, payloads);
        }
    }

    @Override
    public int getItemCount() {
        return folders == null ? 0 : folders.size();
    }

    public LocalMediaFolder getItemAt(int position) {
        return folders.get(position);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivFirstImage;
        TextView tvFolderName;
        TextView ivCount;
        ImageView ivSelectStatus;
        LocalMediaFolder folder;
        int size = 0;

        public ViewHolder(View itemView, int value) {
            super(itemView);
            size = value;
            ivFirstImage = itemView.findViewById(R.id.iv_first_image);
            tvFolderName = itemView.findViewById(R.id.tv_folder_name);
            ivCount = itemView.findViewById(R.id.iv_count);
            ivSelectStatus = itemView.findViewById(R.id.iv_select_status);
            ivFirstImage.setClipToOutline(true);
            ivFirstImage.setOutlineProvider(new RoundViewOutlineProvider().dpRadius(4));
        }

        void bindData(LocalMediaFolder dir) {
            folder = dir;
            ivFirstImage.setImageResource(R.mipmap.img_ic_placeholder);
            tvFolderName.setText(folder.name);
            updateFirstImage();
            updateCount();
            updateSelectedStatus();
        }

        void updateFirstImage() {
            ImgCacheExecutor.with("ImageSelector")
                    .localSource()
                    .loadFromMemoryCache(true)
                    .loadFromDiskCache(true)
                    .url(folder.images.get(0).path)
                    .size(size, size)
                    .into(ivFirstImage);
        }

        void updateCount() {
            ivCount.setText(itemView.getContext().getString(R.string.num_postfix, folder.count()));
        }

        void updateSelectedStatus() {
            ivSelectStatus.setVisibility(folder.selected ? View.VISIBLE : View.GONE);
        }
    }

    public interface OnItemClickListener {
        void onItemClick(int position);
    }
}
