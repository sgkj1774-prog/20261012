package com.example.healthrecorder.cat;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.healthrecorder.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CatRecordAdapter extends RecyclerView.Adapter<CatRecordAdapter.VH> {
    private List<CatRecord> list = new ArrayList<>();
    private final SimpleDateFormat FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    public interface OnItemClickListener { void onItemClick(CatRecord r); }
    private OnItemClickListener listener;
    public void setOnItemClickListener(OnItemClickListener l) { this.listener = l; }

    public void setList(List<CatRecord> list) {
        this.list = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cat_record, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        CatRecord r = list.get(pos);
        h.textDate.setText(FMT.format(r.getDate()));

        String member = r.getUserName();
        if (member != null && !member.isEmpty()) {
            h.textCatName.setText("👥 " + member);
            h.textCatName.setTextColor(Color.parseColor(r.getUserColorHex()));
        } else {
            h.textCatName.setText("👥 未指定成员");
            h.textCatName.setTextColor(Color.parseColor("#999999"));
        }

        int color = r.getScoreColor();
        String scoreText = r.getStoolScore();
        h.textScore.setText(scoreText);
        h.textScore.getBackground().setTint(color);
        int textColor = (color == Color.parseColor("#FFEB3B")) ? Color.parseColor("#212121") : Color.WHITE;
        h.textScore.setTextColor(textColor);

        // 粪便数量
        if (r.getStoolCount() > 0) {
            h.textStoolCount.setText("💩 " + r.getStoolCount() + " 颗");
            h.textStoolCount.setVisibility(View.VISIBLE);
        } else {
            h.textStoolCount.setVisibility(View.GONE);
        }

        // 意愿+过程标签
        if (r.getWillingness() != 0 || r.getProcessType() != 0) {
            h.layoutExtra.setVisibility(View.VISIBLE);
            h.textWillingness.setText(r.getWillingnessLabel());
            h.textProcess.setText(r.getProcessTypeLabel());
            int[] tagColors = {0xFF4CAF50, 0xFFFF9800, 0xFFE53935};
            h.textWillingness.getBackground().setTint(tagColors[r.getWillingness()]);
            h.textProcess.getBackground().setTint(tagColors[r.getProcessType()]);
        } else {
            h.layoutExtra.setVisibility(View.GONE);
        }

        if (r.getNotes() != null && !r.getNotes().isEmpty()) {
            h.textNotes.setVisibility(View.VISIBLE);
            h.textNotes.setText(r.getNotes());
        } else {
            h.textNotes.setVisibility(View.GONE);
        }

        h.itemView.setOnClickListener(v -> { if (listener != null) listener.onItemClick(r); });
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView textDate, textScore, textCatName, textStoolCount, textNotes;
        TextView textWillingness, textProcess;
        View layoutExtra;
        VH(View v) {
            super(v);
            textDate = v.findViewById(R.id.textDate);
            textScore = v.findViewById(R.id.textScore);
            textCatName = v.findViewById(R.id.textCatName);
            textStoolCount = v.findViewById(R.id.textStoolCount);
            textNotes = v.findViewById(R.id.textNotes);
            textWillingness = v.findViewById(R.id.textWillingness);
            textProcess = v.findViewById(R.id.textProcess);
            layoutExtra = v.findViewById(R.id.layoutExtra);
        }
    }
}
