package com.example.myapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.content.Intent;

import com.example.myapplication.adapter.StudentAdapter;
import com.example.myapplication.db.DatabaseHelper;
import com.example.myapplication.model.Student;
import com.example.myapplication.util.SessionManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputLayout;

import java.util.List;

public class StudentsActivity extends BaseActivity {
    private DatabaseHelper db;
    private SessionManager session;
    private StudentAdapter adapter;
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_students);
        enableBack("Students");

        db = new DatabaseHelper(this);
        session = new SessionManager(this);

        tvEmpty = findViewById(R.id.tvEmpty);
        RecyclerView rv = findViewById(R.id.rvStudents);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new StudentAdapter(this::confirmDelete, this::openDetail);
        rv.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fabAdd);
        fab.setOnClickListener(v -> showAddDialog());

        refresh();
    }

    private void refresh() {
        List<Student> list = db.getStudents(session.getUserId());
        adapter.setItems(list);
        tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void confirmDelete(Student s) {
        new AlertDialog.Builder(this)
                .setTitle("Delete student")
                .setMessage("Delete " + s.name + " (" + s.roll + ") and all their attendance?")
                .setPositiveButton("Delete", (d, w) -> {
                    db.deleteStudent(s.id);
                    refresh();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showAddDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_add_student, null);
        TextInputLayout tilRoll = view.findViewById(R.id.tilRoll);
        TextInputLayout tilStudentName = view.findViewById(R.id.tilStudentName);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Add Student")
                .setView(view)
                .setPositiveButton("Add", null)
                .setNegativeButton("Cancel", null)
                .create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String roll = text(tilRoll);
            String name = text(tilStudentName);
            tilRoll.setError(null);
            tilStudentName.setError(null);

            boolean ok = true;
            if (roll.isEmpty()) { tilRoll.setError("Roll is required"); ok = false; }
            if (name.isEmpty()) { tilStudentName.setError("Name is required"); ok = false; }
            if (!ok) return;

            if (!db.addStudent(session.getUserId(), roll, name)) {
                tilRoll.setError("This roll already exists");
                return;
            }
            dialog.dismiss();
            refresh();
        });
    }


    private void openDetail(Student s) {
        Intent i = new Intent(this, StudentDetailActivity.class);
        i.putExtra("id", s.id);
        i.putExtra("roll", s.roll);
        i.putExtra("name", s.name);
        startActivity(i);
    }



}