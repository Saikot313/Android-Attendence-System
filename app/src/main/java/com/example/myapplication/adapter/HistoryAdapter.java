package com.example.myapplication.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.AttendanceRecord;

import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.VH> {
    private final List<AttendanceRecord> items;

    public HistoryAdapter(List<AttendanceRecord> items) { this.items = items; }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        AttendanceRecord r = items.get(position);
        h.date.setText(r.date);
        h.status.setText(r.present ? "Present" : "Absent");
        h.status.setTextColor(ContextCompat.getColor(h.status.getContext(),
                r.present ? R.color.present_green : R.color.absent_red));
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView date, status;

        VH(View v) {
            super(v);
            date = v.findViewById(R.id.tvDate);
            status = v.findViewById(R.id.tvStatus);
        }
    }
}