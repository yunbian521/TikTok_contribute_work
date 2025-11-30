package com.bytedance.tiktok_contribute;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class HotTopicAdapter extends RecyclerView.Adapter<HotTopicAdapter.ViewHolder> {
    private final Context context;
    private final List<Topic> topics;
    private final OnTopicClickListener listener;

    public interface OnTopicClickListener {
        void onTopicClick(Topic topic);
    }

    public HotTopicAdapter(Context context, List<Topic> topics, OnTopicClickListener listener) {
        this.context = context;
        this.topics = topics;
        this.listener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_hot_topic, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Topic topic = topics.get(position);
        // 显示“#话题名”+热门标识（🔥）
        String text = "#" + topic.getName() + (topic.isHot() ? " 🔥" : "");
        holder.tvTopic.setText(text);
        //holder.tvTopic.setTextColor(ContextCompat.getColor(context, android.R.color.black));
        holder.itemView.setOnClickListener(v -> listener.onTopicClick(topic));
    }

    @Override
    public int getItemCount() { return topics.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTopic;
        ViewHolder(View itemView) {
            super(itemView);
            tvTopic = itemView.findViewById(R.id.tv_hot_topic);
        }
    }
}