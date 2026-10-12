package com.example.healthrecorder.exercise;

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

public class ExerciseRecordAdapter extends RecyclerView.Adapter<ExerciseRecordAdapter.VH> {
    private List<ExerciseRecord> list = new ArrayList<>();
    private final SimpleDateFormat FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    public interface OnItemClickListener { void onItemClick(ExerciseRecord r); }
    private OnItemClickListener listener;
    public void setOnItemClickListener(OnItemClickListener l) { this.listener = l; }

    public void setList(List<ExerciseRecord> list) {
        this.list = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_exercise_record, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        ExerciseRecord r = list.get(pos);
        h.textType.setText(r.getTypeLabel());
        h.textType.setTextColor(r.getType() == ExerciseRecord.TYPE_RUNNING
                ? Color.parseColor("#FF5722") : Color.parseColor("#795548"));
        h.textDate.setText(FMT.format(r.getDate()));
        h.textDistance.setText(String.format(Locale.US, "%.2f km", r.getDistance()));
        h.textDuration.setText(r.getDuration() + " min");
        String paceStr = r.getType() == ExerciseRecord.TYPE_HIKING
                ? (r.getElevationGain() > 0 ? "+" + r.getElevationGain() + "m" : "—")
                : r.getPaceFormatted();
        h.textPace.setText(paceStr);

        if (r.getLocation() != null && !r.getLocation().isEmpty()) {
            h.textLocation.setVisibility(View.VISIBLE);
            h.textLocation.setText("📍 " + r.getLocation());
        } else {
            h.textLocation.setVisibility(View.GONE);
        }

        if (r.getNotes() != null && !r.getNotes().isEmpty()) {
            h.textNotes.setVisibility(View.VISIBLE);
            h.textNotes.setText(r.getNotes());
        } else {
            h.textNotes.setVisibility(View.GONE);
        }
        h.itemView.setOnClickListener(v -> { if (listener != null) listener.onItemClick(r); });
    }

    @Override public int getItemCount() { return list.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView textType, textDate, textLocation, textDistance, textDuration, textPace, textNotes;
        VH(View v) {
            super(v);
            textType = v.findViewById(R.id.textType);
            textDate = v.findViewById(R.id.textDate);
            textLocation = v.findViewById(R.id.textLocation);
            textDistance = v.findViewById(R.id.textDistance);
            textDuration = v.findViewById(R.id.textDuration);
            textPace = v.findViewById(R.id.textPace);
            textNotes = v.findViewById(R.id.textNotes);
        }
    }
}
