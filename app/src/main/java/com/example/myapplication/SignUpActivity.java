package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;

import com.example.myapplication.util.CloudSync;
import com.example.myapplication.db.DatabaseHelper;
import com.example.myapplication.util.HashUtil;
import com.example.myapplication.util.SessionManager;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Locale;

public class SignUpActivity extends BaseActivity {
    private TextInputLayout tilName, tilEmail, tilPass, tilConfirm;
    private DatabaseHelper db;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);
        enableBack("Sign Up");

        db = new DatabaseHelper(this);
        session = new SessionManager(this);

        tilName = findViewById(R.id.tilName);
        tilEmail = findViewById(R.id.tilEmail);
        tilPass = findViewById(R.id.tilPassword);
        tilConfirm = findViewById(R.id.tilConfirm);
        Button btn = findViewById(R.id.btnSubmit);
        btn.setOnClickListener(v -> register());
    }

    private void register() {
        String name = text(tilName);
        String email = text(tilEmail).toLowerCase(Locale.ROOT);
        String pass = text(tilPass);
        String confirm = text(tilConfirm);

        tilName.setError(null);
        tilEmail.setError(null);
        tilPass.setError(null);
        tilConfirm.setError(null);

        boolean ok = true;
        if (name.isEmpty()) { tilName.setError("Name is required"); ok = false; }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { tilEmail.setError("Enter a valid email"); ok = false; }
        if (pass.length() < 6) { tilPass.setError("Minimum 6 characters"); ok = false; }
        if (!pass.equals(confirm)) { tilConfirm.setError("Passwords do not match"); ok = false; }
        if (!ok) return;

        long id = db.registerUser(name, email, HashUtil.sha256(email + ":" + pass));
        if (id == -1) {
            tilEmail.setError("Email already registered");
            return;
        }
        session.login(id, name);
        CloudSync.ensureSignedIn(email, pass);
        Intent i = new Intent(this, DashboardActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        
    }
}