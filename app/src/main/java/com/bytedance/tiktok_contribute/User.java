package com.bytedance.tiktok_contribute;

public class User {
    private String id; // 用户ID
    private String name; // 用户名（如“张三”）

    public User(String id, String name) {
        this.id = id;
        this.name = name;
    }

    // getter
    public String getName() { return name; }
}