package com.bytedance.tiktok_contribute;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class PreviewAdapter extends RecyclerView.Adapter<PreviewAdapter.ViewHolder> {
    private final List<Uri> images;

    //public PreviewAdapter(List<Uri> images) {
      //  this.images = images;
    //}
    private OnCoverChangeListener coverChangeListener;
    public PreviewAdapter(List<Uri> images) {
        this.images = images;
        notifyCoverChanged(); // 初始化时触发封面更新
    }
    // 封面更新回调接口
    public interface OnCoverChangeListener {
        void onCoverChanged(Uri coverUri);
    }
    // 设置回调
    public void setOnCoverChangeListener(OnCoverChangeListener listener) {
        this.coverChangeListener = listener;
        notifyCoverChanged();
    }
    // 触发回调
    private void notifyCoverChanged() {
        if (coverChangeListener != null) {
            Uri coverUri = images.isEmpty() ? null : images.get(0);
            coverChangeListener.onCoverChanged(coverUri);
        }
    }
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.preview_image, parent, false);
        return new PreviewAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // 加载图片（用Glide避免OOM）
        Glide.with(holder.itemView.getContext())
                .load(images.get(position))
                .into(holder.ivPreview);
    }

    @Override
    public int getItemCount() {
        return images.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPreview;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPreview = itemView.findViewById(R.id.iv_preview);
        }
    }
}
