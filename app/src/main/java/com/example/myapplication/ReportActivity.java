package com.example.myapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.adapter.ReportAdapter;
import com.example.myapplication.db.DatabaseHelper;
import com.example.myapplication.model.Student;
import com.example.myapplication.util.SessionManager;
import android.widget.Toast;
import com.example.myapplication.util.ExportUtil;
import java.util.ArrayList;

import java.util.List;

public class ReportActivity extends BaseActivity {
    private DatabaseHelper db;
    private SessionManager session;
    private ReportAdapter adapter;
    private Button btnDate;
    private TextView tvSummary, tvEmpty;
    private String date = today();
    private List<Student> current = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);
        enableBack("Attendance Report");

        db = new DatabaseHelper(this);
        session = new SessionManager(this);

        btnDate = findViewById(R.id.btnDate);
        tvSummary = findViewById(R.id.tvSummary);
        tvEmpty = findViewById(R.id.tvEmpty);

        RecyclerView rv = findViewById(R.id.rvReport);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ReportAdapter();
        rv.setAdapter(adapter);

        btnDate.setText(date);
        btnDate.setOnClickListener(v -> pickDate(date, d -> {
            date = d;
            btnDate.setText(date);
            load();
        }));


        findViewById(R.id.btnCsv).setOnClickListener(v -> export(false));
        findViewById(R.id.btnPdf).setOnClickListener(v -> export(true));

        load();
    }

    private void load() {
        List<Student> list = db.getReport(session.getUserId(), date);
        current = list;
        adapter.setItems(list);
        if (list.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            tvSummary.setText("");
            return;
        }
        tvEmpty.setVisibility(View.GONE);
        int present = 0;
        for (Student s : list) if (s.present) present++;
        int total = list.size();
        tvSummary.setText("Present: " + present + "   Absent: " + (total - present)
                + "   Total: " + total + "   (" + Math.round(present * 100f / total) + "%)");
    }


    private void export(boolean pdf) {
        if (current.isEmpty()) {
            Toast.makeText(this, "Nothing to export for this date", Toast.LENGTH_SHORT).show();
            return;
        }
        List<String[]> rows = new ArrayList<>();
        for (Student s : current) {
            rows.add(new String[]{date, s.roll, s.name, s.present ? "Present" : "Absent"});
        }
        String[] header = {"Date", "Roll", "Name", "Status"};
        if (pdf) {
            ExportUtil.exportPdf(this, "report_" + date + ".pdf", "Attendance Report - " + date,
                    tvSummary.getText().toString(), header, rows, new float[]{2.2f, 1.3f, 3.5f, 1.8f});
        } else {
            ExportUtil.exportCsv(this, "report_" + date + ".csv", header, rows);
        }
    }

    
}