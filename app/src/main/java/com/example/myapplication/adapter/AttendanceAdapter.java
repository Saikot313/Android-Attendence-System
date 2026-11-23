package com.example.myapplication.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.Student;

import java.util.ArrayList;
import java.util.List;

public class AttendanceAdapter extends RecyclerView.Adapter<AttendanceAdapter.VH> {
    public interface OnChange { void onChanged(); }

    private final List<Student> items = new ArrayList<>();
    private final OnChange listener;

    public AttendanceAdapter(OnChange listener) { this.listener = listener; }

    public List<Student> getItems() { return items; }

    public void setItems(List<Student> list) {
        items.clear();
        items.addAll(list);
        notifyDataSetChanged();
    }

    public void markAll(boolean present) {
        for (Student s : items) s.present = present;
        notifyDataSetChanged();
        listener.onChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_attendance, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Student s = items.get(position);
        h.roll.setText(s.roll);
        h.name.setText(s.name);
        h.check.setOnCheckedChangeListener(null);
        h.check.setChecked(s.present);
        style(h.check, s.present);
        h.check.setOnCheckedChangeListener((btn, checked) -> {
            s.present = checked;
            style(h.check, checked);
            listener.onChanged();
        });
    }

    private void style(CheckBox cb, boolean present) {
        cb.setText(present ? "Present" : "Absent");
        cb.setTextColor(ContextCompat.getColor(cb.getContext(),
                present ? R.color.present_green : R.color.absent_red));
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView roll, name;
        CheckBox check;

        VH(View v) {
            super(v);
            roll = v.findViewById(R.id.tvRoll);
            name = v.findViewById(R.id.tvName);
            check = v.findViewById(R.id.cbPresent);
        }
    }
}