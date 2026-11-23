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

public class SignInActivity extends BaseActivity {
    private TextInputLayout tilEmail, tilPass;
    private DatabaseHelper db;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);
        enableBack("Sign In");

        db = new DatabaseHelper(this);
        session = new SessionManager(this);

        tilEmail = findViewById(R.id.tilEmail);
        tilPass = findViewById(R.id.tilPassword);
        Button btn = findViewById(R.id.btnSubmit);
        btn.setOnClickListener(v -> signIn());
    }

    private void signIn() {
        String email = text(tilEmail).toLowerCase(Locale.ROOT);
        String pass = text(tilPass);
        tilEmail.setError(null);
        tilPass.setError(null);

        boolean ok = true;
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { tilEmail.setError("Enter a valid email"); ok = false; }
        if (pass.isEmpty()) { tilPass.setError("Password is required"); ok = false; }
        if (!ok) return;

        long id = db.login(email, HashUtil.sha256(email + ":" + pass));
        if (id == -1) {
            tilPass.setError("Wrong email or password");
            return;
        }
        session.login(id, db.getUserName(id));
        CloudSync.ensureSignedIn(email, pass);
        Intent i = new Intent(this, DashboardActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
    }
}