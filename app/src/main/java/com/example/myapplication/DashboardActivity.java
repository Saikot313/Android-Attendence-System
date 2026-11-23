package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.db.DatabaseHelper;
import com.example.myapplication.util.CloudSync;
import com.example.myapplication.util.SessionManager;

public class DashboardActivity extends AppCompatActivity {
    private DatabaseHelper db;
    private SessionManager session;
    private TextView tvCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
        if (getSupportActionBar() != null) getSupportActionBar().setTitle("Dashboard");

        db = new DatabaseHelper(this);
        session = new SessionManager(this);

        TextView tvWelcome = findViewById(R.id.tvWelcome);
        tvCount = findViewById(R.id.tvCount);
        tvWelcome.setText("Hello, " + session.getUserName());

        findViewById(R.id.btnStudents).setOnClickListener(v ->
                startActivity(new Intent(this, StudentsActivity.class)));
        findViewById(R.id.btnAttendance).setOnClickListener(v ->
                startActivity(new Intent(this, AttendanceActivity.class)));
        findViewById(R.id.btnReports).setOnClickListener(v ->
                startActivity(new Intent(this, ReportActivity.class)));

        findViewById(R.id.btnBackup).setOnClickListener(v -> {
            Toast.makeText(this, "Backing up...", Toast.LENGTH_SHORT).show();
            CloudSync.upload(db, session.getUserId(),
                    (ok, msg) -> Toast.makeText(this, msg, Toast.LENGTH_LONG).show());
        });
        findViewById(R.id.btnRestore).setOnClickListener(v -> {
            Toast.makeText(this, "Restoring...", Toast.LENGTH_SHORT).show();
            CloudSync.download(db, session.getUserId(), (ok, msg) -> {
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                refreshCount();
            });
        });

        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            CloudSync.signOut();
            session.logout();
            Intent i = new Intent(this, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshCount();
    }

    private void refreshCount() {
        tvCount.setText("Total students: " + db.countStudents(session.getUserId()));
    }
}