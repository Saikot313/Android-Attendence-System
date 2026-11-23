package com.example.myapplication;

import android.app.DatePickerDialog;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputLayout;

import java.util.Calendar;
import java.util.Locale;

public abstract class BaseActivity extends AppCompatActivity {

    public interface DateListener { void onDate(String date); }

    protected void enableBack(String title) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    protected String text(TextInputLayout til) {
        return til.getEditText() == null ? "" : til.getEditText().getText().toString().trim();
    }

    protected static String today() {
        Calendar c = Calendar.getInstance();
        return String.format(Locale.US, "%04d-%02d-%02d",
                c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }

    /** date format: yyyy-MM-dd */
    protected void pickDate(String current, DateListener listener) {
        String[] p = current.split("-");
        int y = Integer.parseInt(p[0]);
        int m = Integer.parseInt(p[1]) - 1;
        int d = Integer.parseInt(p[2]);
        DatePickerDialog dialog = new DatePickerDialog(this, (view, yy, mm, dd) ->
                listener.onDate(String.format(Locale.US, "%04d-%02d-%02d", yy, mm + 1, dd)), y, m, d);
        dialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        dialog.show();
    }
}