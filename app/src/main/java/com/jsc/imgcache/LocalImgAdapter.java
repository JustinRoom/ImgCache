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

package com.jsc.imgcache;

import android.content.res.Resources;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.jsc.imgcache.utils.ViewOutlineUtils;

import java.io.File;

public class LocalImgAdapter extends RecyclerView.Adapter<LocalImgAdapter.IViewHolder> {

    File[] files = null;

    public void setFiles(File[] files) {
        this.files = files;
        notifyDataSetChanged();
    }

    public File[] getFiles() {
        return files;
    }

    @NonNull
    @Override
    public IViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new IViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_img, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull IViewHolder holder, int position) {
        holder.ivImg.setImageResource(R.mipmap.p_default_header);
    }

    @Override
    public int getItemCount() {
        return files == null ? 0 : files.length;
    }

    public static class IViewHolder extends RecyclerView.ViewHolder{

        ImageView ivImg;

        public IViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImg = itemView.findViewById(R.id.iv_img);
            Resources resources = itemView.getContext().getResources();
            ViewOutlineUtils.applyRoundOutline(ivImg, TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6, resources.getDisplayMetrics()));
        }
    }
}
