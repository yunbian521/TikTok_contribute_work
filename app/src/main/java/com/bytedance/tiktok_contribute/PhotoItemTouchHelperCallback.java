package com.bytedance.tiktok_contribute;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import com.bytedance.tiktok_contribute.PreviewAdapter;
import java.util.Collections;
import java.util.List;
import android.net.Uri; // 新增 Uri 导入

public class PhotoItemTouchHelperCallback extends ItemTouchHelper.Callback {
    private final Context mContext;
    private final List<Uri> mPhotoList; // 改为 Uri 类型
    private final PreviewAdapter mAdapter;
    private Toast mDeleteToast;
    private float mLastDragY;

    // 构造方法：接收 List<Uri>
    public PhotoItemTouchHelperCallback(Context context, List<Uri> photoList, PreviewAdapter adapter) {
        mContext = context;
        mPhotoList = photoList;
        mAdapter = adapter;
        initDeleteToast();
    }

    // 初始化删除 Toast（逻辑不变）
    private void initDeleteToast() {
        /*
        mDeleteToast = new Toast(mContext);
        TextView toastView = new TextView(mContext);
        toastView.setText("松手即可删除");
        toastView.setTextColor(Color.WHITE);
        toastView.setBackgroundColor(Color.RED);
        toastView.setPadding(30, 15, 30, 15);
        toastView.setGravity(Gravity.CENTER);


         */
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View toastView = inflater.inflate(R.layout.delet_layout, null);

        mDeleteToast = new Toast(mContext);
        mDeleteToast.setView(toastView);
        //mDeleteToast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 200);
        mDeleteToast.setGravity(Gravity.FILL_HORIZONTAL| Gravity.BOTTOM , 0, 0);
        mDeleteToast.setMargin(0, 0);
        mDeleteToast.setDuration(Toast.LENGTH_LONG);
    }

    // 拖动方向
    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        int dragFlags = ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;
        int swipeFlags = 0;
        return makeMovementFlags(dragFlags, swipeFlags);
    }

    // 拖动交换
    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        /*
        int fromPos = viewHolder.getAdapterPosition();
        int toPos = target.getAdapterPosition();
        Collections.swap(mPhotoList, fromPos, toPos);
        mAdapter.notifyItemMoved(fromPos, toPos);
        return true;
        */
        int fromPos = viewHolder.getAdapterPosition();
        int toPos = target.getAdapterPosition();

        Collections.swap(mPhotoList, fromPos, toPos);
        mAdapter.notifyItemMoved(fromPos, toPos);

        // 2. 若交换涉及第一张（0位），触发封面更新
        if (fromPos == 0 || toPos == 0) {
            if (mContext instanceof MainActivity) {
                ((MainActivity)mContext).updateCover(); // 直接调用MainActivity的方法
            }
        }
        return true;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {}

    // 拖动开始显示 Toast
    @Override
    public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
        super.onSelectedChanged(viewHolder, actionState);
        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
            mDeleteToast.show();
            mDeleteToast.getView().setVisibility(View.VISIBLE);
        }
    }

    // 拖动结束删除
    @Override
    public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);
        viewHolder.itemView.setAlpha(1.0f);
        if (isInDeleteArea(mLastDragY)) {
            int pos = viewHolder.getAdapterPosition();
            mPhotoList.remove(pos); // 删除 Uri 类型元素
            mAdapter.notifyItemRemoved(pos);
            Toast.makeText(mContext, "已删除", Toast.LENGTH_SHORT).show();
        }
        mDeleteToast.cancel();
    }

    // 记录拖动坐标
    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                            @NonNull RecyclerView.ViewHolder viewHolder,
                            float dX, float dY, int actionState, boolean isCurrentlyActive) {
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        viewHolder.itemView.setAlpha(isCurrentlyActive ? 0.5f : 1.0f);
        int[] location = new int[2];
        viewHolder.itemView.getLocationOnScreen(location);
        mLastDragY = location[1] + viewHolder.itemView.getHeight() / 2;
    }

    // 判断删除区域（逻辑不变）
    private boolean isInDeleteArea(float y) {
        if (mDeleteToast.getView() == null) return false;
        int[] toastLocation = new int[2];
        mDeleteToast.getView().getLocationOnScreen(toastLocation);
        int toastTop = toastLocation[1];
        int toastBottom = toastTop + mDeleteToast.getView().getHeight();
        return y >= toastTop && y <= toastBottom;
    }

    @Override
    public boolean isLongPressDragEnabled() {
        return true;
    }
}