package com.example.myapplication.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.Student;

import java.util.ArrayList;
import java.util.List;

public class StudentAdapter extends RecyclerView.Adapter<StudentAdapter.VH> {
    public interface OnDelete { void onDelete(Student s); }
    public interface OnClick { void onClick(Student s); }

    private final List<Student> items = new ArrayList<>();
    private final OnDelete deleteListener;
    private final OnClick clickListener;

    public StudentAdapter(OnDelete deleteListener, OnClick clickListener) {
        this.deleteListener = deleteListener;
        this.clickListener = clickListener;
    }

    public void setItems(List<Student> list) {
        items.clear();
        items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_student, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Student s = items.get(position);
        h.roll.setText(s.roll);
        h.name.setText(s.name);
        if (s.total == 0) {
            h.info.setText("No attendance yet");
            h.info.setTextColor(h.defaultInfoColor);
        } else {
            h.info.setText("Attendance: " + s.percent() + "% (" + s.presentCount + "/" + s.total + ")");
            h.info.setTextColor(s.percent() < 75
                    ? ContextCompat.getColor(h.info.getContext(), R.color.absent_red)
                    : h.defaultInfoColor);
        }
        h.itemView.setOnClickListener(v -> clickListener.onClick(s));
        h.delete.setOnClickListener(v -> deleteListener.onDelete(s));
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView roll, name, info;
        ImageButton delete;
        int defaultInfoColor;

        VH(View v) {
            super(v);
            roll = v.findViewById(R.id.tvRoll);
            name = v.findViewById(R.id.tvName);
            info = v.findViewById(R.id.tvInfo);
            delete = v.findViewById(R.id.btnDelete);
            defaultInfoColor = info.getCurrentTextColor();
        }
    }
}