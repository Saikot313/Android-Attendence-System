package com.example.myapplication.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.Student;

import java.util.ArrayList;
import java.util.List;

public class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.VH> {
    private final List<Student> items = new ArrayList<>();

    public void setItems(List<Student> list) {
        items.clear();
        items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_report, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Student s = items.get(position);
        h.roll.setText(s.roll);
        h.name.setText(s.name);
        h.status.setText(s.present ? "Present" : "Absent");
        h.status.setTextColor(ContextCompat.getColor(h.status.getContext(),
                s.present ? R.color.present_green : R.color.absent_red));
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView roll, name, status;

        VH(View v) {
            super(v);
            roll = v.findViewById(R.id.tvRoll);
            name = v.findViewById(R.id.tvName);
            status = v.findViewById(R.id.tvStatus);
        }
    }
}