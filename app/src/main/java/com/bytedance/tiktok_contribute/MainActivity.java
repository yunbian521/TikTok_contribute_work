package com.bytedance.tiktok_contribute;

import android.annotation.SuppressLint;
import android.app.AlertDialog;

import android.graphics.Color;

import android.content.Intent;

import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import android.Manifest;
import androidx.appcompat.app.AppCompatActivity;

import android.widget.ImageView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import android.net.Uri;
import android.os.Build;
import androidx.core.content.ContextCompat;
import android.content.pm.PackageManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;

import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    // 选中的图片Uri集合
    // 存储选中的图片Uri
    private final List<Uri> selectedImages = new ArrayList<>();
    private RecyclerView rvPreview; // 预览框（仅显示图片）
    private ImageView ivCover; // 封面大图
    private PreviewAdapter previewAdapter;

    // 最大字数限制（可根据需求调整）
    private static final int MAX_TITLE_LENGTH = 20;    // 标题最多20字
    private static final int MAX_DESC_LENGTH = 100;    // 描述最多100字

    private EditText etTitle;
    private EditText etDescription;
    private TextView tvTitleCount;
    private TextView tvDescCount;

    private RecyclerView rvCandidates, rvHotTopics;
    private CandidateAdapter candidateAdapter;
    private HotTopicAdapter hotTopicAdapter;
    private List<String> candidateList = new ArrayList<>(); // 候选列表数据
    private List<Topic> hotTopics = new ArrayList<>(); // 热门话题数据
    private List<User> mockUsers = new ArrayList<>(); // Mock用户数据
    private boolean isInTopicMode = false; // 是否处于#话题模式
    private boolean isInAtMode = false; // 是否处于@用户模式
    private TextView tvLocation;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 101;
    private final DecimalFormat latLngFormat = new DecimalFormat("#.00");
    private final ActivityResultLauncher<Intent> pickImagesLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    // 处理多选
                    if (data.getClipData() != null) {
                        int count = data.getClipData().getItemCount();
                        for (int i = 0; i < count; i++) {
                            Uri uri = data.getClipData().getItemAt(i).getUri();
                            selectedImages.add(uri);
                        }
                    } else {
                        // 处理单选
                        Uri uri = data.getData();
                        if (uri != null) {
                            selectedImages.add(uri);
                        }
                    }
                    // 更新封面和预览框
                    updatePreviewAndCover();
                }
            }
    );

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化控件
        ivCover = findViewById(R.id.iv_cover_preview);
        rvPreview = findViewById(R.id.rv_preview);

        etTitle = findViewById(R.id.et_title);
        etDescription = findViewById(R.id.et_description);//描述框
        tvTitleCount = findViewById(R.id.tv_title_count);//标题的计数
        tvDescCount = findViewById(R.id.tv_desc_count);//描述框的计数

        // 初始化预览列表（横向滚动）
        previewAdapter = new PreviewAdapter(selectedImages);
        //这段将RecyclerView变成横向滚动
        rvPreview.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvPreview.setAdapter(previewAdapter);  //rvPreview是RecyclerView预览框

        initViews();
        initMockData();
        initAdapters();
        initListeners();

        //长按照片交换位置和删除功能实现
        ViewGroup rootLayout = findViewById(R.id.activity_root_layout);
        //  绑定ItemTouchHelper（拖动排序+删除）
        PhotoItemTouchHelperCallback callback = new PhotoItemTouchHelperCallback(this, selectedImages, previewAdapter, rootLayout);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(rvPreview);
        // 按钮点击事件
        findViewById(R.id.pre_see).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        findViewById(R.id.btn_add_cover).setOnClickListener(v -> checkPermissionAndOpenAlbum());

        etTitle.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                // 1. 计算当前输入字数（注意：中文、英文、数字都算1个字符）
                int currentLength = s.length();

                // 2. 限制最大字数，超过则截断
                if (currentLength > MAX_TITLE_LENGTH) {
                    // 截断为最大长度的文本
                    s.delete(MAX_TITLE_LENGTH, currentLength);
                    // 更新当前长度为最大值（因为已经截断）
                    currentLength = MAX_TITLE_LENGTH;
                }

                // 3. 更新字数统计显示（如“5/20”）
                tvTitleCount.setText(currentLength + "/" + MAX_TITLE_LENGTH);
            }
        });

        // 给描述输入框设置监听（逻辑和标题类似）
        etDescription.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                int currentLength = s.length();

                if (currentLength > MAX_DESC_LENGTH) {
                    s.delete(MAX_DESC_LENGTH, currentLength);
                    currentLength = MAX_DESC_LENGTH;
                }

                tvDescCount.setText(currentLength + "/" + MAX_DESC_LENGTH);
            }
        });

        tvLocation = findViewById(R.id.tv_location); // 对应布局中的定位显示控件
        //申请定位权限
        requestLocationPermission();
    }
    // 申请定位权限
    /*
    精确位置	ACCESS_FINE_LOCATION	    GPS/WiFi，精度~10米	导航、精确追踪
    粗略位置	ACCESS_COARSE_LOCATION	    基站/WiFi，精度~1公里	天气、本地服务
    后台位置	ACCESS_BACKGROUND_LOCATION	Android 10+ 需要	后台持续定位
     */
    private void requestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            // 已有权限，直接获取定位
            getLocation();
        }
    }
    private void getLocation() {
        new LocationUtil(this, new LocationUtil.OnLocationGetListener() {
            @Override
            public void onSuccess(double latitude, double longitude) {
                // 显示经纬度（格式：纬度, 经度）
                String latStr = latLngFormat.format(latitude);
                String lngStr = latLngFormat.format(longitude);
                String locationText = "经度：" + latStr + ",  纬度：" + lngStr;
                tvLocation.setText(locationText);
            }
            @Override
            public void onFailed(String errorMsg) {
                tvLocation.setText("定位失败：" + errorMsg);
                Toast.makeText(MainActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
            }
        }).getCurrentLocation();
    }
    // 初始化控件
    private void initViews() {
        //etDescription = findViewById(R.id.et_description);
        rvCandidates = findViewById(R.id.rv_candidates); //候选列表RecyclerView（这个用于更新用户和话题列表）
        rvHotTopics = findViewById(R.id.rv_hot_topics);  //话题的RecyclerView（这个用于横向显示话题）
        rvCandidates.setLayoutManager(new LinearLayoutManager(this));
        rvHotTopics.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
    }

    // 初始化Mock数据
    private void initMockData() {
        // 热门话题数据
        hotTopics.add(new Topic("上热门", true));
        hotTopics.add(new Topic("内容太过真实", false));
        hotTopics.add(new Topic("每日一笑", true));
        hotTopics.add(new Topic("生活技巧", false));
        // Mock用户数据
        mockUsers.add(new User("1", "张三"));
        mockUsers.add(new User("2", "李四"));
        mockUsers.add(new User("3", "王五"));
        mockUsers.add(new User("4", "赵六"));
    }

    // 初始化适配器
    private void initAdapters() {
        // 候选列表适配器（#话题或@用户）
        candidateAdapter = new CandidateAdapter(this, candidateList, content -> {
            insertContent(content); // 插入选择的内容
            rvCandidates.setVisibility(View.GONE); // 隐藏候选列表
        });
        rvCandidates.setAdapter(candidateAdapter);

        // 热门话题RecyclerView的适配器
        /*
        hotTopicAdapter = new HotTopicAdapter(this, hotTopics, topic -> {
            // 点击热门话题，插入“#话题#”
            String topicText = "#" + topic.getName() + "# ";
            insertText(topicText);
        });
        等价写法
        hotTopicAdapter = new HotTopicAdapter(this, hotTopics, new HotTopicAdapter.OnTopicClickListener() {
        @Override
        public void onTopicClick(Topic topic) {
        // 点击逻辑和 Lambda 完全一样
        String topicText = "#" + topic.getName() + "# ";
        insertText(topicText);
        }
        });
        */
        hotTopicAdapter = new HotTopicAdapter(this, hotTopics, topic -> {
            // 点击热门话题，插入“#话题#”
            String topicText = "#" + topic.getName() + "# ";
            insertText(topicText);
        });
        rvHotTopics.setAdapter(hotTopicAdapter);
    }

    // 初始化监听事件
    private void initListeners() {
        // 描述输入框文本变化监听
        etDescription.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String text = s.toString();
                int cursorPos = etDescription.getSelectionStart(); // 获取光标位置

                // 检测是否输入了“#”（触发话题候选）
                if (cursorPos > 0 && text.charAt(cursorPos - 1) == '#') {
                    isInTopicMode = true;
                    isInAtMode = false;
                    updateTopicCandidates(); // 显示热门话题候选
                    rvCandidates.setVisibility(View.VISIBLE);
                }
                // 检测是否输入了“@”（触发用户候选）
                else if (cursorPos > 0 && text.charAt(cursorPos - 1) == '@') {
                    isInAtMode = true;
                    isInTopicMode = false;
                    updateUserCandidates(); // 显示用户候选
                    rvCandidates.setVisibility(View.VISIBLE);
                }
                // 其他情况隐藏候选列表
                else {
                    rvCandidates.setVisibility(View.GONE);
                    isInTopicMode = false;
                    isInAtMode = false;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // “#话题”按钮点击：手动触发话题候选
        findViewById(R.id.btn_topic).setOnClickListener(v -> {
            etDescription.getText().insert(etDescription.getSelectionStart(), "#");
            isInTopicMode = true;
            updateTopicCandidates();
            rvCandidates.setVisibility(View.VISIBLE);
        });

        // “@朋友”按钮点击：手动触发用户候选
        findViewById(R.id.btn_mention).setOnClickListener(v -> {
            etDescription.getText().insert(etDescription.getSelectionStart(), "@");
            isInAtMode = true;
            updateUserCandidates();
            rvCandidates.setVisibility(View.VISIBLE);
        });
    }
    // 更新话题候选列表（热门话题）
    //原来每次添加到candidateList中的数据不同，话题和用户列表都是用candidateAdapter来更新的
    private void updateTopicCandidates() {
        candidateList.clear();
        for (Topic topic : hotTopics) {
            candidateList.add("#" + topic.getName() + "#");
        }
        candidateAdapter.notifyDataSetChanged();
    }

    // 更新用户候选列表（Mock用户）
    private void updateUserCandidates() {
        candidateList.clear();
        for (User user : mockUsers) {
            candidateList.add("@" + user.getName());
        }
        candidateAdapter.notifyDataSetChanged();
    }

    // 插入选择的内容（话题或用户）
    private void insertContent(String content) {
        Editable editable = etDescription.getText();
        int cursorPos = etDescription.getSelectionStart();

        // 处于#模式，先删除输入的“#”再插入完整话题
        if (isInTopicMode && cursorPos > 0 && editable.charAt(cursorPos - 1) == '#') {
            editable.delete(cursorPos - 1, cursorPos); // 删除单独的“#”
            cursorPos--;
        }
        // 处于@模式，先删除输入的“@”再插入完整用户
        else if (isInAtMode && cursorPos > 0 && editable.charAt(cursorPos - 1) == '@') {
            editable.delete(cursorPos - 1, cursorPos); // 删除单独的“@”
            cursorPos--;
        }

        // 插入选中的内容
        editable.insert(cursorPos, content + " ");
        // 移动光标到内容后面
        etDescription.setSelection(cursorPos + content.length() + 1);
    }

    // 直接插入文本（如热门话题点击）
    private void insertText(String text) {
        int cursorPos = etDescription.getSelectionStart();
        etDescription.getText().insert(cursorPos, text);
        etDescription.setSelection(cursorPos + text.length());
    }
     //打开系统相册
    private void openAlbum() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*"); // 只选择图片
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true); // 允许多选
        pickImagesLauncher.launch(intent);
    }


     //检查权限并打开相册
    private void checkPermissionAndOpenAlbum() {
        /*
        逻辑：
        Android 13+（TIRAMISU）：相册权限拆分为独立的 READ_MEDIA_IMAGES（仅访问图片），不再需要宽泛的 READ_EXTERNAL_STORAGE（读取外部存储）；
        Android 12 及以下：仍使用传统的 READ_EXTERNAL_STORAGE 权限访问相册；
        目的：适配 Android 13 后的权限细分规则，避免申请冗余权限，同时兼容低版本。
         */
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;  //选择对应相册权限

        //已授权则直接打开相册
        /*
        仅需「读取权限状态」，无需依赖 Activity 生命周期 / 回调，Context 即可完成；
        ContextCompat 更通用（可在 Service/Adapter 等非 Activity 类中调用）
         */
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            openAlbum();
        }
        else {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{permission}, 100);
        }
        /*
        //  未授权，需要判断是否需要向用户解释为何需要权限
        ActivityCompat.shouldShowRequestPermissionRationale(this, permission)：判断「是否需要向用户解释权限用途」；
        返回 true 的场景：用户之前拒绝过该权限，但未勾选 “不再询问”—— 此时系统要求开发者先解释权限用途，再重新申请（合规要求，避免频繁弹窗骚扰用户）；
        返回 false 的场景：① 首次申请权限；② 用户拒绝权限并勾选 “不再询问”；
        核心：该方法必须依赖 Activity 上下文（需要关联页面的权限申请状态），因此用专门面向 Activity 的 ActivityCompat。
        首次申请权限: 用户未接触过该权限，直接弹系统权限弹窗即可，无需开发者提前解释

        else if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) { //判断「是否需要向用户解释权限用途」
            // 弹窗解释权限用途
            new AlertDialog.Builder(this)
                    .setTitle("权限申请")
                    .setMessage("需要访问相册权限才能选择图片，请允许")
                    .setPositiveButton("确定", (dialog, which) -> {
                        // 再次请求权限
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{permission}, 100);
                    })
                    .setNegativeButton("取消", null)
                    .show();
        }
        else {
            new AlertDialog.Builder(this)
                    .setTitle("权限被拒绝")
                    .setMessage("已禁止访问相册权限，请前往设置页开启")
                    .setPositiveButton("去设置", (dialog, which) -> {
                        // 跳转到应用权限设置页
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", getPackageName(), null);
                        intent.setData(uri);
                        startActivity(intent);
                    })
                    .setNegativeButton("取消", null)
                    .show();
        }*/

    }

    //更新预览列表和封面
    @SuppressLint("NotifyDataSetChanged")
    private void updatePreviewAndCover() {
            // 刷新预览列表
            previewAdapter.notifyDataSetChanged();
            // 更新封面（复用updateCover方法）
            updateCover();
    }

    public void updateCover() {
        /* ivCover 是封面的id*/
        if (!selectedImages.isEmpty()) {
            Glide.with(this)
                    .load(selectedImages.get(0))
                    .placeholder(ivCover.getDrawable())
                    .skipMemoryCache(true)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .centerCrop()
                    .into(ivCover);
        } else {
            ivCover.setImageDrawable(null);
            ivCover.setBackgroundColor(Color.parseColor("#333333"));
        }
    }
}