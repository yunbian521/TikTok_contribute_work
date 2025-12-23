package com.bytedance.tiktok_contribute;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CandidateAdapter extends RecyclerView.Adapter<CandidateAdapter.ViewHolder> {
    private final Context context;
    private final List<String> candidates; // 候选内容（话题名或用户名）
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(String content);
    }

    public CandidateAdapter(Context context, List<String> candidates, OnItemClickListener listener) {
        this.context = context;
        this.candidates = candidates;
        this.listener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_candidate, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        String content = candidates.get(position);
        holder.tvContent.setText(content);
        //holder.tvContent.setTextColor(ContextCompat.getColor(context, android.R.color.black));
        holder.itemView.setOnClickListener(v -> listener.onItemClick(content));
    }

    @Override
    public int getItemCount() { return candidates.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvContent;
        ViewHolder(View itemView) {
            super(itemView);
            tvContent = itemView.findViewById(R.id.tv_candidate); //这个是放在候选列表RecyclerView中的单个子项
        }
    }
}