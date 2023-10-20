package jsc.org.lib.img.selector.activity;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.widget.PopupWindow;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import jsc.org.lib.img.R;
import jsc.org.lib.img.selector.adapter.ImageFolderAdapter;
import jsc.org.lib.img.selector.model.LocalMediaFolder;

public class FoldersPopupWindow extends PopupWindow {
    private final ImageFolderAdapter adapter;
    private OnSelectChangedListener changedListener = null;
    private int height = 0;

    public FoldersPopupWindow(Context context) {
        RecyclerView recyclerView = new RecyclerView(context);
        recyclerView.setVerticalScrollBarEnabled(false);
        recyclerView.setBackgroundColor(Color.WHITE);
        this.setContentView(recyclerView);
        boolean isLandscape = context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        int width = isLandscape ? metrics.widthPixels / 2 : metrics.widthPixels;
        height = isLandscape ? metrics.heightPixels * 4 / 5 : metrics.heightPixels * 3 / 4;
        this.setWidth(width);
        this.setHeight(height);
        this.setAnimationStyle(R.style.WindowStyle);
        this.setFocusable(true);
        this.setOutsideTouchable(false);
        this.update();
        this.setBackgroundDrawable(new ColorDrawable(Color.argb(153, 0, 0, 0)));
        adapter = new ImageFolderAdapter(width / 5, new ImageFolderAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                dismiss();
                if (adapter.selectedMedia(position) && changedListener != null) {
                    changedListener.onChanged(adapter.getItemAt(position));
                }
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setAdapter(adapter);
    }

    public void updateFolders(List<LocalMediaFolder> folders) {
        adapter.setFolders(folders);
    }

    public void insert(int position) {
        adapter.notifyItemInserted(position);
    }

    public void updateFirstImage(int position) {
        adapter.notifyItemChanged(position, "updateFirstImage");
    }

    public void updateCount(int position) {
        adapter.notifyItemChanged(position, "updateCount");
    }

    public void showAbove(View anchor, View parent) {
        int[] location = new int[2];
        anchor.getLocationOnScreen(location);
        showAtLocation(parent, Gravity.TOP, 0, location[1] - height);
    }

    public void setOnItemClickListener(OnSelectChangedListener listener) {
        changedListener = listener;
    }

    @Override
    public void dismiss() {
        super.dismiss();
    }

    public interface OnSelectChangedListener {
        void onChanged(LocalMediaFolder folder);
    }
}
