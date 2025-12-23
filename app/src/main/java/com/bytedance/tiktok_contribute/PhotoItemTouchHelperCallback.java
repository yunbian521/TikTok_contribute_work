package com.bytedance.tiktok_contribute;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;

import android.graphics.Rect;

import android.view.Gravity;
import android.view.LayoutInflater;

import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import java.util.Collections;
import java.util.List;
import android.net.Uri; // 新增 Uri 导入

public class PhotoItemTouchHelperCallback extends ItemTouchHelper.Callback {
    private final Context mContext;
    private final List<Uri> mPhotoList; // 改为 Uri 类型
    private final PreviewAdapter mAdapter;
    private Toast mDeleteToast;
    private float mLastDragY;

    // 新增：悬浮View相关变量
    private View mFloatView; // 拖拽的悬浮View
    private int mDraggedPosition = -1; // 当前拖拽的Item位置
    private ViewGroup mRootLayout; // 顶层布局（用于挂载悬浮View）
    private boolean isDragging = false; // 是否正在拖拽
    private boolean isInnerDrag = true;

    // 构造方法：接收 List<Uri>
    public PhotoItemTouchHelperCallback(Context context, List<Uri> photoList, PreviewAdapter adapter, ViewGroup rootLayou) {
        mContext = context;
        mPhotoList = photoList;
        mAdapter = adapter;
        mRootLayout = rootLayou;
        initDeleteToast();
    }

    // 初始化删除 Toast（逻辑不变）
    private void initDeleteToast() {
    try

    {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View toastView = inflater.inflate(R.layout.delet_layout, null);
        // 布局加载失败时，创建默认Toast兜底
        if (toastView == null) {
            mDeleteToast = Toast.makeText(mContext, "松手即可删除", Toast.LENGTH_LONG);
            mDeleteToast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 200);
            return;
        }
        mDeleteToast = new Toast(mContext);
        mDeleteToast.setView(toastView);
        mDeleteToast.setGravity(Gravity.FILL_HORIZONTAL | Gravity.BOTTOM, 0, 0);
        mDeleteToast.setMargin(0, 0);
        mDeleteToast.setDuration(Toast.LENGTH_LONG);
    } catch(Exception e)
    {
        // 捕获所有异常，避免Toast初始化失败崩溃
        e.printStackTrace();
        mDeleteToast = Toast.makeText(mContext, "松手即可删除", Toast.LENGTH_LONG);
        mDeleteToast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 200);
    }
}
    // 拖动方向
    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        //int dragFlags = ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;
        int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;
        int swipeFlags = 0;
        return makeMovementFlags(dragFlags, swipeFlags);
    }

    // 拖动交换
    //onMove   拖拽过程中，每次位置变化时都会调用
    //onMoved  拖拽结束后，一次拖拽只调用一次
    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        if (!isInnerDrag) return false;
        // 1. 校验列表非空
        if (mPhotoList == null || mPhotoList.isEmpty()) return false;
        int fromPos = viewHolder.getAdapterPosition();
        int toPos = target.getAdapterPosition();
        // 2. 校验位置有效性
        if (fromPos == RecyclerView.NO_POSITION || toPos == RecyclerView.NO_POSITION) return false;
        if (fromPos >= mPhotoList.size() || toPos >= mPhotoList.size()) return false;

        // 执行交换
        Collections.swap(mPhotoList, fromPos, toPos);
        mAdapter.notifyItemMoved(fromPos, toPos);

        // 3. 修复：MainActivity强转增加类型校验
        if ((fromPos == 0 || toPos == 0) && mContext instanceof MainActivity) {
            ((MainActivity) mContext).updateCover();
        }
        return true;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {}

    // 拖动开始显示 Toast
    /*
    * 当某条 item 第一次被激活（手指按下开始拖拽或滑动）或者 完全释放（手指抬起、动画结束）时，
    * 系统会立即把这一变化通知给你，以便一次性地改变视觉样式、记录日志、播放音效等。
    * */
    @Override
    public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
        super.onSelectedChanged(viewHolder, actionState);

        // 记录拖拽位置
        // 仅处理拖拽状态，且viewHolder非空时执行
        if (actionState != ItemTouchHelper.ACTION_STATE_DRAG || viewHolder == null) {
            return;
        }

        mDraggedPosition = viewHolder.getAdapterPosition();
        if (mDraggedPosition == RecyclerView.NO_POSITION) return;

        isDragging = true;
        isInnerDrag = true;
        View originalItem = viewHolder.itemView;

        // Toast操作增加判空
        if (mDeleteToast != null) {
            try {
                mDeleteToast.show();  //只要我拖拽就显示删除toast
                View toastView = mDeleteToast.getView();
                if (toastView != null) toastView.setVisibility(View.VISIBLE);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // 创建悬浮View前校验mRootLayout非空
        if (mRootLayout != null) {
            createFloatView(originalItem);   //只要我拖拽就创建 mFloatView 悬浮View 以便后面的外部拖拽
        }
        originalItem.setAlpha(0.3f);
    }

    // 每一帧绘制时都会调用  在其中记录拖动坐标    是不是可以在这里实时显示拖拖拽的照片？
    /*
    RecyclerView            recyclerView表示当前的RecyclerView对象
    RecyclerView.ViewHolder viewHolder表示被拖动/滑动的item的ViewHolder
    dX	                    item在水平方向的偏移量（正值向右，负值向左）
    dY	                    item在垂直方向的偏移量（正值向下，负值向上）
    actionState	            当前动作状态（拖动或滑动）
            ItemTouchHelper.ACTION_STATE_IDLE     = 0; // 空闲状态（无操作）
            ItemTouchHelper.ACTION_STATE_SWIPE    = 1; // 正在滑动
            ItemTouchHelper.ACTION_STATE_DRAG     = 2; // 正在拖动
    isCurrentlyActive	    当前item是否处于激活状态（正在被用户操作）
    */
    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                            @NonNull RecyclerView.ViewHolder viewHolder,
                            float dX, float dY, int actionState, boolean isCurrentlyActive) {
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);

        if (isDragging && mFloatView != null && isCurrentlyActive) {
            View originalItem = viewHolder.itemView; //originalItem原来的Item也会
            // 1. 同步原Item的偏移（让系统能识别Item交换）
            originalItem.setTranslationX(dX);
            originalItem.setTranslationY(dY);
            System.out.println("dX:" + dX);
            System.out.println("dY:" + dY);

            // 2. 判断是否拖出RecyclerView边界（区分内部/外部拖拽）
            Rect rvRect = new Rect(); //创建一个矩形对象
            recyclerView.getGlobalVisibleRect(rvRect); //将 RecyclerView 在整个屏幕上的可视区域坐标写入rvRect：
            // 获取Item当前的屏幕坐标
            int[] itemLoc = new int[2];
            originalItem.getLocationOnScreen(itemLoc);
            System.out.println("originalItem:" + originalItem);
            /*
            Rect.contains(x, y)：判断坐标点(x,y)是否在矩形区域内（包含边界）；
            返回true：Item 中心在 RecyclerView 可视区域内（内部拖拽）；
            返回false：Item 中心超出 RecyclerView 可视区域（外部拖拽）。
             */
            boolean isOutBound = !rvRect.contains(itemLoc[0] + originalItem.getWidth()/2, itemLoc[1] + originalItem.getHeight()/2);
            // 3. 外部拖拽：显示悬浮View；内部拖拽：隐藏悬浮View（避免干扰）
            if (isOutBound) {
                isInnerDrag = false;
                mFloatView.setVisibility(View.VISIBLE);
                // 更新悬浮View位置（跟随手指）
                int[] originalLoc = new int[2];
                originalItem.getLocationOnScreen(originalLoc);
                float targetX = originalLoc[0];//+ dX;
                float targetY = originalLoc[1];//+ dY;
                updateFloatViewPosition(targetX, targetY);
                // 内部拖拽时隐藏原Item
                originalItem.setAlpha(0f);
            } else {
                isInnerDrag = true;
                //mFloatView.setVisibility(View.INVISIBLE);
                mFloatView.setVisibility(View.INVISIBLE);
                // 内部拖拽时原Item半透明
                originalItem.setAlpha(0.3f);
            }
            // 记录拖拽坐标（用于删除判断）
            mLastDragY = itemLoc[1] + originalItem.getHeight()/2;//+ dY;
        }
    }
    private void createFloatView(View originalItem) {
        if (originalItem == null || mRootLayout == null) return;
        try {
            originalItem.setDrawingCacheEnabled(true);
            originalItem.buildDrawingCache(true);
            Bitmap bitmap = Bitmap.createBitmap(originalItem.getDrawingCache());
            originalItem.setDrawingCacheEnabled(false);

            ImageView floatView = new ImageView(mContext);
            floatView.setImageBitmap(bitmap);
            floatView.setLayoutParams(new FrameLayout.LayoutParams(originalItem.getWidth(), originalItem.getHeight()));
            floatView.setVisibility(View.INVISIBLE);

            mRootLayout.addView(floatView);
            mFloatView = floatView;

            int[] originalLoc = new int[2];
            originalItem.getLocationOnScreen(originalLoc);
            int[] rootLoc = new int[2];
            mRootLayout.getLocationOnScreen(rootLoc);
            floatView.setTranslationX(originalLoc[0] - rootLoc[0]);
            floatView.setTranslationY(originalLoc[1] - rootLoc[1]);
        } catch (Exception e) {
            // 捕获创建悬浮View的异常，避免崩溃
            e.printStackTrace();
            mFloatView = null;
        }
    }
    // 拖拽结束 放手时调用
    @Override
    public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);

        View originalItem = viewHolder.itemView;

        // 1. 恢复原Item的位置和透明度
        originalItem.setAlpha(1.0f);
        originalItem.setTranslationX(0);
        originalItem.setTranslationY(0);

        // 2. 处理删除/还原逻辑
        System.out.println("mLastDragY:" + mLastDragY);
        System.out.println("isInDeleteArea(mLastDragY):" + isInDeleteArea(mLastDragY));
        if (isInDeleteArea(mLastDragY)) {
            int pos = viewHolder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && pos < mPhotoList.size()) {
                mPhotoList.remove(pos);
                mAdapter.notifyItemRemoved(pos);
                if (mContext instanceof MainActivity) {
                    ((MainActivity) mContext).updateCover();
                }
                //Toast.makeText(mContext, "已删除", Toast.LENGTH_SHORT).show();
            }
        }

        // Toast操作增加判空
        if (mDeleteToast != null) {
            try {
                mDeleteToast.cancel();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        destroyFloatView();
        isDragging = false;
        isInnerDrag = true;
        mDraggedPosition = -1;
        mLastDragY = 0;
    }
    /**
     * 更新悬浮View位置（跟随手指）
     */
    private void updateFloatViewPosition(float targetX, float targetY) {
        if (mFloatView == null || mRootLayout == null) return;

        // 计算顶层布局的屏幕偏移（解决状态栏/导航栏影响）
        int[] rootLoc = new int[2];
        mRootLayout.getLocationOnScreen(rootLoc);
        float finalX = targetX - rootLoc[0];
        float finalY = targetY - rootLoc[1];
        // 更新悬浮View的位置（使用translation避免重新布局，更流畅）
        mFloatView.setTranslationX(finalX);
        mFloatView.setTranslationY(finalY);
    }
    /**
     * 销毁悬浮View
     */
    private void destroyFloatView() {
        if (mFloatView != null && mRootLayout != null) {
            try { // 增加异常捕获，避免移除View失败崩溃
                mRootLayout.removeView(mFloatView);
            } catch (Exception e) {
                e.printStackTrace();
            }
            mFloatView = null;
        }
    }
    // 修复：Toast判空 + 异常捕获
    private boolean isInDeleteArea(float y) {
        if (mDeleteToast == null || mDeleteToast.getView() == null) return false;
        try {
            int[] toastLocation = new int[2];
            mDeleteToast.getView().getLocationOnScreen(toastLocation);
            int toastTop = toastLocation[1];
            int toastBottom = toastTop + mDeleteToast.getView().getHeight();
            System.out.println("toastTop:" + toastTop);
            System.out.println("toastBottom:" + toastBottom);
            System.out.println("y >= toastTop && y <= toastBottom" + (y >= toastTop && y <= toastBottom));
            return y >= toastTop && y <= toastBottom;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean isLongPressDragEnabled() {//允许长按拖拽
        return true;
    }
}