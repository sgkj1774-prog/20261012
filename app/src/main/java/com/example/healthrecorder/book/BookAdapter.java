package com.example.healthrecorder.book;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.healthrecorder.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BookAdapter extends RecyclerView.Adapter<BookAdapter.VH> {
    private List<BookRecord> list = new ArrayList<>();
    private final SimpleDateFormat FMT = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private OnItemClickListener clickListener;
    private OnItemLongClickListener longClickListener;

    public interface OnItemClickListener { void onItemClick(BookRecord r); }
    public interface OnItemLongClickListener { void onItemLongClick(BookRecord r); }

    public void setOnItemClickListener(OnItemClickListener l) { this.clickListener = l; }
    public void setOnItemLongClickListener(OnItemLongClickListener l) { this.longClickListener = l; }

    public void setList(List<BookRecord> list) {
        this.list = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_book_record, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        BookRecord r = list.get(pos);
        h.textTitle.setText(r.getTitle());
        h.textAuthor.setText(r.getAuthor() != null && !r.getAuthor().isEmpty() ? r.getAuthor() : "未知作者");

        // 状态标签
        int statusColor;
        String statusText;
        if (r.getStatus() == 0) {
            statusColor = Color.parseColor("#2196F3");
            statusText = "想读";
        } else if (r.getStatus() == 1) {
            statusColor = Color.parseColor("#FF9800");
            statusText = "在读";
        } else {
            statusColor = Color.parseColor("#4CAF50");
            statusText = "已读";
        }
        h.textStatus.setText(statusText);
        h.textStatus.getBackground().setTint(statusColor);

        // 在读：进度条
        if (r.getStatus() == 1) {
            h.layoutProgress.setVisibility(View.VISIBLE);
            h.layoutRating.setVisibility(View.GONE);
            int pct = 0;
            if (r.getTotalPages() > 0) {
                pct = (int) (r.getProgress() * 100);
                h.progressBar.setProgress(pct);
            }
            h.textProgress.setText("第 " + r.getCurrentPage() + " 页 / 共 " + r.getTotalPages() + " 页（" + pct + "%）");
        }
        // 已读：星级 + 归档页数 + 读后感
        else if (r.getStatus() == 2) {
            h.layoutProgress.setVisibility(View.GONE);
            h.layoutRating.setVisibility(View.VISIBLE);
            int rating = r.getRating();
            String stars = "";
            for (int i = 0; i < 5; i++) {
                stars += (i < rating) ? "★" : "☆";
            }
            h.textRating.setText(stars);
            // 归档页数
            if (r.getFinishedPage() > 0) {
                h.textFinishedPage.setText("📖 读完于第 " + r.getFinishedPage() + " 页");
                h.textFinishedPage.setVisibility(View.VISIBLE);
            } else {
                h.textFinishedPage.setVisibility(View.GONE);
            }
            // 归档读后感
            if (r.getFinishedNotes() != null && !r.getFinishedNotes().isEmpty()) {
                h.textFinishedNotes.setText(r.getFinishedNotes());
                h.textFinishedNotes.setVisibility(View.VISIBLE);
            } else if (r.getNotes() != null && !r.getNotes().isEmpty()) {
                // 兼容旧数据：直接读 notes
                h.textFinishedNotes.setText(r.getNotes());
                h.textFinishedNotes.setVisibility(View.VISIBLE);
            } else {
                h.textFinishedNotes.setVisibility(View.GONE);
            }
        }
        // 想读
        else {
            h.layoutProgress.setVisibility(View.GONE);
            h.layoutRating.setVisibility(View.GONE);
        }

        // 读后感图标
        if (r.getNotes() != null && !r.getNotes().isEmpty()) {
            h.textNotesIcon.setVisibility(View.VISIBLE);
        } else {
            h.textNotesIcon.setVisibility(View.GONE);
        }

        // 日期
        if (r.getCreatedAt() != null) {
            h.textDate.setText(FMT.format(r.getCreatedAt()));
        } else {
            h.textDate.setText("");
        }

        // 点击事件
        h.itemView.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onItemClick(r);
        });
        h.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) longClickListener.onItemLongClick(r);
            return true;
        });
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView textTitle, textAuthor, textStatus, textProgress, textRating;
        TextView textDate, textNotesIcon, textFinishedPage, textFinishedNotes;
        LinearLayout layoutProgress, layoutRating;
        ProgressBar progressBar;

        VH(View v) {
            super(v);
            textTitle = v.findViewById(R.id.textTitle);
            textAuthor = v.findViewById(R.id.textAuthor);
            textStatus = v.findViewById(R.id.textStatus);
            textProgress = v.findViewById(R.id.textProgress);
            textRating = v.findViewById(R.id.textRating);
            textDate = v.findViewById(R.id.textDate);
            textNotesIcon = v.findViewById(R.id.textNotesIcon);
            textFinishedPage = v.findViewById(R.id.textFinishedPage);
            textFinishedNotes = v.findViewById(R.id.textFinishedNotes);
            layoutProgress = v.findViewById(R.id.layoutProgress);
            layoutRating = v.findViewById(R.id.layoutRating);
            progressBar = v.findViewById(R.id.progressBar);
        }
    }
}
