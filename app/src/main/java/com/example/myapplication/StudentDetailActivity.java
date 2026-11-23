package com.example.myapplication;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.adapter.HistoryAdapter;
import com.example.myapplication.db.DatabaseHelper;
import com.example.myapplication.model.AttendanceRecord;
import com.example.myapplication.util.ExportUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class StudentDetailActivity extends BaseActivity {
    private String roll, name, subtitle;
    private List<AttendanceRecord> records;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_detail);
        enableBack("Student Report");

        long id = getIntent().getLongExtra("id", -1);
        roll = getIntent().getStringExtra("roll");
        name = getIntent().getStringExtra("name");
        records = new DatabaseHelper(this).getStudentHistory(id);

        int total = records.size();
        int present = 0;
        Map<String, int[]> months = new TreeMap<>(Collections.reverseOrder());
        for (AttendanceRecord r : records) {
            if (r.present) present++;
            String key = r.date.substring(0, 7);
            int[] v = months.get(key);
            if (v == null) {
                v = new int[2];
                months.put(key, v);
            }
            if (r.present) v[0]++;
            v[1]++;
        }
        int percent = total == 0 ? 0 : Math.round(present * 100f / total);
        subtitle = "Roll: " + roll + "    Present " + present + " / " + total + " (" + percent + "%)";

        ((TextView) findViewById(R.id.tvTitle)).setText(roll + " · " + name);
        ((TextView) findViewById(R.id.tvSummary)).setText("Total days: " + total
                + "   Present: " + present + "   Absent: " + (total - present)
                + "   Attendance: " + percent + "%");

        StringBuilder sb = new StringBuilder("Monthly breakdown\n");
        for (Map.Entry<String, int[]> e : months.entrySet()) {
            int[] v = e.getValue();
            sb.append(e.getKey()).append(":  ").append(v[0]).append("/").append(v[1])
                    .append("  (").append(Math.round(v[0] * 100f / v[1])).append("%)\n");
        }
        ((TextView) findViewById(R.id.tvMonthly)).setText(
                total == 0 ? "No attendance recorded yet." : sb.toString().trim());

        RecyclerView rv = findViewById(R.id.rvHistory);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(new HistoryAdapter(records));

        findViewById(R.id.btnCsv).setOnClickListener(v -> export(false));
        findViewById(R.id.btnPdf).setOnClickListener(v -> export(true));
    }

    private void export(boolean pdf) {
        if (records.isEmpty()) {
            Toast.makeText(this, "Nothing to export", Toast.LENGTH_SHORT).show();
            return;
        }
        List<String[]> rows = new ArrayList<>();
        for (AttendanceRecord r : records) {
            rows.add(new String[]{roll, name, r.date, r.present ? "Present" : "Absent"});
        }
        String[] header = {"Roll", "Name", "Date", "Status"};
        if (pdf) {
            ExportUtil.exportPdf(this, "student_" + roll + ".pdf", "Attendance Report - " + name,
                    subtitle, header, rows, new float[]{1.2f, 3f, 2f, 1.6f});
        } else {
            ExportUtil.exportCsv(this, "student_" + roll + ".csv", header, rows);
        }
    }
}