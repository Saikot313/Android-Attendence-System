package com.example.myapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.adapter.AttendanceAdapter;
import com.example.myapplication.db.DatabaseHelper;
import com.example.myapplication.model.Student;
import com.example.myapplication.util.SessionManager;

import java.util.List;

public class AttendanceActivity extends BaseActivity {
    private DatabaseHelper db;
    private SessionManager session;
    private AttendanceAdapter adapter;
    private Button btnDate, btnSave;
    private TextView tvSummary, tvEmpty;
    private String date = today();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance);
        enableBack("Take Attendance");

        db = new DatabaseHelper(this);
        session = new SessionManager(this);

        btnDate = findViewById(R.id.btnDate);
        btnSave = findViewById(R.id.btnSave);
        tvSummary = findViewById(R.id.tvSummary);
        tvEmpty = findViewById(R.id.tvEmpty);
        Button btnAllPresent = findViewById(R.id.btnAllPresent);

        RecyclerView rv = findViewById(R.id.rvAttendance);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AttendanceAdapter(this::updateSummary);
        rv.setAdapter(adapter);

        btnDate.setText(date);
        btnDate.setOnClickListener(v -> pickDate(date, d -> {
            date = d;
            btnDate.setText(date);
            load();
        }));
        btnAllPresent.setOnClickListener(v -> adapter.markAll(true));
        btnSave.setOnClickListener(v -> {
            db.saveAttendance(adapter.getItems(), date);
            Toast.makeText(this, "Attendance saved for " + date, Toast.LENGTH_SHORT).show();
        });

        load();
    }

    private void load() {
        List<Student> list = db.getAttendanceForDate(session.getUserId(), date);
        adapter.setItems(list);
        boolean empty = list.isEmpty();
        tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        btnSave.setEnabled(!empty);
        updateSummary();
    }

    private void updateSummary() {
        int present = 0;
        List<Student> items = adapter.getItems();
        for (Student s : items) if (s.present) present++;
        tvSummary.setText("Present: " + present + "   Absent: " + (items.size() - present)
                + "   Total: " + items.size());
    }
}