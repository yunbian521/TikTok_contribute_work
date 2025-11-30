package com.bytedance.tiktok_contribute;

public class Topic {
    private String name; // 话题名（如“上热门”）
    private boolean isHot; // 是否带热门标识

    public Topic(String name, boolean isHot) {
        this.name = name;
        this.isHot = isHot;
    }

    // getter
    public String getName() { return name; }
    public boolean isHot() { return isHot; }
}
