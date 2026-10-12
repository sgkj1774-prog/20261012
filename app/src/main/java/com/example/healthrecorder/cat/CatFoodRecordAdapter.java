package com.example.healthrecorder.cat;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.healthrecorder.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** 猫粮记录列表适配器；当前在吃的记录显示绿色边框特殊样式 */
public class CatFoodRecordAdapter extends RecyclerView.Adapter<CatFoodRecordAdapter.VH> {
    private List<CatFoodRecord> list = new ArrayList<>();
    private final SimpleDateFormat FMT = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private boolean deleteMode = false;

    private OnItemClickListener clickListener;
    private OnItemDeleteListener deleteListener;

    public interface OnItemClickListener { void onItemClick(CatFoodRecord r); }
    public interface OnItemDeleteListener { void onItemDelete(CatFoodRecord r); }

    public void setOnItemClickListener(OnItemClickListener l) { this.clickListener = l; }
    public void setOnItemDeleteListener(OnItemDeleteListener l) { this.deleteListener = l; }

    public void setList(List<CatFoodRecord> list) {
        this.list = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setDeleteMode(boolean deleteMode) {
        this.deleteMode = deleteMode;
        notifyDataSetChanged();
    }

    public boolean isDeleteMode() { return deleteMode; }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cat_food_record, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        CatFoodRecord r = list.get(pos);

        // 品牌型号
        h.textBrand.setText(r.getBrand() != null ? r.getBrand() : "未知品牌");

        // 类型标签
        h.textType.setText(r.getFoodType() != null ? r.getFoodType() : "干粮");
        h.textType.getBackground().setTint(r.getTypeColor());

        // 进行中 / 已结束状态
        if (r.isOngoing()) {
            h.textStatus.setText("进行中");
            h.textStatus.getBackground().setTint(Color.parseColor("#4CAF50"));
            // 当前在吃：绿色边框特殊样式
            h.layoutRoot.setBackgroundResource(R.drawable.bg_cat_food_current);
        } else {
            h.textStatus.setText("已结束");
            h.textStatus.getBackground().setTint(Color.parseColor("#9E9E9E"));
            h.layoutRoot.setBackgroundResource(R.drawable.bg_cat_food_normal);
        }

        // 日期区间 + 持续天数
        String start = r.getStartDate() != null ? FMT.format(r.getStartDate()) : "?";
        if (r.isOngoing()) {
            h.textDateRange.setText("📅 " + start + " 开始 · 已持续 " + r.durationDays() + " 天");
            h.textDateRange.setTextColor(Color.parseColor("#4CAF50"));
        } else {
            String end = FMT.format(r.getEndDate());
            h.textDateRange.setText("📅 " + start + " ~ " + end + " · 共 " + r.durationDays() + " 天");
            h.textDateRange.setTextColor(Color.parseColor("#999999"));
        }

        // 备注
        if (r.getNotes() != null && !r.getNotes().isEmpty()) {
            h.textNotes.setVisibility(View.VISIBLE);
            h.textNotes.setText("📝 " + r.getNotes());
        } else {
            h.textNotes.setVisibility(View.GONE);
        }

        // 删除模式：显示删除按钮
        h.textDeleteIcon.setVisibility(deleteMode ? View.VISIBLE : View.GONE);
        h.textDeleteIcon.setOnClickListener(v -> {
            if (deleteListener != null) deleteListener.onItemDelete(r);
        });

        // 点击：删除模式下触发删除，否则进入编辑
        h.itemView.setOnClickListener(v -> {
            if (deleteMode) {
                if (deleteListener != null) deleteListener.onItemDelete(r);
            } else {
                if (clickListener != null) clickListener.onItemClick(r);
            }
        });
        h.itemView.setOnLongClickListener(v -> {
            if (deleteListener != null) deleteListener.onItemDelete(r);
            return true;
        });
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class VH extends RecyclerView.ViewHolder {
        LinearLayout layoutRoot;
        TextView textBrand, textType, textStatus, textDateRange, textNotes, textDeleteIcon;

        VH(View v) {
            super(v);
            layoutRoot = v.findViewById(R.id.layoutRoot);
            textBrand = v.findViewById(R.id.textBrand);
            textType = v.findViewById(R.id.textType);
            textStatus = v.findViewById(R.id.textStatus);
            textDateRange = v.findViewById(R.id.textDateRange);
            textNotes = v.findViewById(R.id.textNotes);
            textDeleteIcon = v.findViewById(R.id.textDeleteIcon);
        }
    }
}
