package com.example.healthrecorder.teeth;

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

public class TeethRecordAdapter {

    public interface OnItemClickListener { void onItemClick(TeethRecord r); }

    public static class Adapter extends RecyclerView.Adapter<Adapter.VH> {
        private List<TeethRecord> list = new ArrayList<>();
        private final SimpleDateFormat FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        private OnItemClickListener listener;

        public void setOnItemClickListener(OnItemClickListener l) { this.listener = l; }

        public void setList(List<TeethRecord> list) {
            this.list = list != null ? list : new ArrayList<>();
            notifyDataSetChanged();
        }

        @NonNull @Override
        public Adapter.VH onCreateViewHolder(@NonNull ViewGroup p, int vt) {
            return new Adapter.VH(LayoutInflater.from(p.getContext())
                    .inflate(R.layout.item_teeth_record, p, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Adapter.VH h, int pos) {
            TeethRecord r = list.get(pos);
            h.textDate.setText(FMT.format(r.getDate()));
            h.textUserName.setText(r.getUserName() != null ? r.getUserName() : "本人");
            if (r.getNotes() != null && !r.getNotes().isEmpty()) {
                h.textNotes.setVisibility(View.VISIBLE);
                h.textNotes.setText(r.getNotes());
            } else {
                h.textNotes.setVisibility(View.GONE);
            }
            h.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onItemClick(r);
            });
        }

        @Override public int getItemCount() { return list.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView textDate, textUserName, textNotes;
            VH(View v) {
                super(v);
                textDate = v.findViewById(R.id.textDate);
                textUserName = v.findViewById(R.id.textUserName);
                textNotes = v.findViewById(R.id.textNotes);
            }
        }
    }
}
