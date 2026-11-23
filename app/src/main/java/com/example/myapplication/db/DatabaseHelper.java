package com.example.myapplication.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.example.myapplication.model.AttendanceRecord;
import java.util.HashMap;
import java.util.Map;

import com.example.myapplication.model.Student;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "attendance.db";
    private static final int DB_VERSION = 1;

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE users(id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL, email TEXT NOT NULL UNIQUE, password TEXT NOT NULL)");
        db.execSQL("CREATE TABLE students(id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id INTEGER NOT NULL, roll TEXT NOT NULL, name TEXT NOT NULL, " +
                "UNIQUE(user_id, roll), " +
                "FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE)");
        db.execSQL("CREATE TABLE attendance(id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "student_id INTEGER NOT NULL, date TEXT NOT NULL, present INTEGER NOT NULL, " +
                "UNIQUE(student_id, date), " +
                "FOREIGN KEY(student_id) REFERENCES students(id) ON DELETE CASCADE)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        db.execSQL("DROP TABLE IF EXISTS attendance");
        db.execSQL("DROP TABLE IF EXISTS students");
        db.execSQL("DROP TABLE IF EXISTS users");
        onCreate(db);
    }

    // ---------- Users ----------
    public long registerUser(String name, String email, String passwordHash) {
        ContentValues cv = new ContentValues();
        cv.put("name", name);
        cv.put("email", email);
        cv.put("password", passwordHash);
        return getWritableDatabase().insert("users", null, cv); // -1 if email exists
    }

    public long login(String email, String passwordHash) {
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT id FROM users WHERE email=? AND password=?",
                new String[]{email, passwordHash})) {
            return c.moveToFirst() ? c.getLong(0) : -1;
        }
    }

    public String getUserName(long userId) {
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT name FROM users WHERE id=?", new String[]{String.valueOf(userId)})) {
            return c.moveToFirst() ? c.getString(0) : "";
        }
    }

    // ---------- Students ----------
    public boolean addStudent(long userId, String roll, String name) {
        ContentValues cv = new ContentValues();
        cv.put("user_id", userId);
        cv.put("roll", roll);
        cv.put("name", name);
        return getWritableDatabase().insert("students", null, cv) != -1;
    }

    public void deleteStudent(long studentId) {
        getWritableDatabase().delete("students", "id=?", new String[]{String.valueOf(studentId)});
    }

    public int countStudents(long userId) {
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM students WHERE user_id=?",
                new String[]{String.valueOf(userId)})) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }

    public List<Student> getStudents(long userId) {
        List<Student> list = new ArrayList<>();
        String sql = "SELECT s.id, s.roll, s.name, COUNT(a.id), COALESCE(SUM(a.present),0) " +
                "FROM students s LEFT JOIN attendance a ON a.student_id = s.id " +
                "WHERE s.user_id=? GROUP BY s.id ORDER BY LENGTH(s.roll), s.roll";
        try (Cursor c = getReadableDatabase().rawQuery(sql, new String[]{String.valueOf(userId)})) {
            while (c.moveToNext()) {
                Student s = new Student();
                s.id = c.getLong(0);
                s.roll = c.getString(1);
                s.name = c.getString(2);
                s.total = c.getInt(3);
                s.presentCount = c.getInt(4);
                list.add(s);
            }
        }
        return list;
    }

    // ---------- Attendance ----------
    /** All students for the date; default = present if not saved yet. */
    public List<Student> getAttendanceForDate(long userId, String date) {
        List<Student> list = new ArrayList<>();
        String sql = "SELECT s.id, s.roll, s.name, a.present FROM students s " +
                "LEFT JOIN attendance a ON a.student_id = s.id AND a.date = ? " +
                "WHERE s.user_id=? ORDER BY LENGTH(s.roll), s.roll";
        try (Cursor c = getReadableDatabase().rawQuery(sql, new String[]{date, String.valueOf(userId)})) {
            while (c.moveToNext()) {
                Student s = new Student();
                s.id = c.getLong(0);
                s.roll = c.getString(1);
                s.name = c.getString(2);
                s.present = c.isNull(3) || c.getInt(3) == 1;
                list.add(s);
            }
        }
        return list;
    }

    /** Only students who have a saved record on that date. */
    public List<Student> getReport(long userId, String date) {
        List<Student> list = new ArrayList<>();
        String sql = "SELECT s.id, s.roll, s.name, a.present FROM students s " +
                "INNER JOIN attendance a ON a.student_id = s.id AND a.date = ? " +
                "WHERE s.user_id=? ORDER BY LENGTH(s.roll), s.roll";
        try (Cursor c = getReadableDatabase().rawQuery(sql, new String[]{date, String.valueOf(userId)})) {
            while (c.moveToNext()) {
                Student s = new Student();
                s.id = c.getLong(0);
                s.roll = c.getString(1);
                s.name = c.getString(2);
                s.present = c.getInt(3) == 1;
                list.add(s);
            }
        }
        return list;
    }

    public void saveAttendance(List<Student> students, String date) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            for (Student s : students) {
                ContentValues cv = new ContentValues();
                cv.put("student_id", s.id);
                cv.put("date", date);
                cv.put("present", s.present ? 1 : 0);
                db.insertWithOnConflict("attendance", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

        // ---------- History / Sync ----------
    public List<AttendanceRecord> getStudentHistory(long studentId) {
        List<AttendanceRecord> list = new ArrayList<>();
        String sql = "SELECT s.roll, a.date, a.present FROM attendance a " +
                "INNER JOIN students s ON s.id = a.student_id " +
                "WHERE a.student_id=? ORDER BY a.date DESC";
        try (Cursor c = getReadableDatabase().rawQuery(sql, new String[]{String.valueOf(studentId)})) {
            while (c.moveToNext()) {
                list.add(new AttendanceRecord(c.getString(0), c.getString(1), c.getInt(2) == 1));
            }
        }
        return list;
    }

    public List<AttendanceRecord> getAllAttendance(long userId) {
        List<AttendanceRecord> list = new ArrayList<>();
        String sql = "SELECT s.roll, a.date, a.present FROM attendance a " +
                "INNER JOIN students s ON s.id = a.student_id WHERE s.user_id=?";
        try (Cursor c = getReadableDatabase().rawQuery(sql, new String[]{String.valueOf(userId)})) {
            while (c.moveToNext()) {
                list.add(new AttendanceRecord(c.getString(0), c.getString(1), c.getInt(2) == 1));
            }
        }
        return list;
    }

    /** Merge cloud data into local DB (insert new, update existing). */
    public void restoreFromCloud(long userId, List<Student> students, List<AttendanceRecord> records) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            Map<String, Long> ids = new HashMap<>();
            for (Student s : students) {
                ids.put(s.roll, upsertStudent(db, userId, s.roll, s.name));
            }
            for (AttendanceRecord r : records) {
                Long sid = ids.get(r.roll);
                if (sid == null) continue;
                ContentValues cv = new ContentValues();
                cv.put("student_id", sid);
                cv.put("date", r.date);
                cv.put("present", r.present ? 1 : 0);
                db.insertWithOnConflict("attendance", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private long upsertStudent(SQLiteDatabase db, long userId, String roll, String name) {
        try (Cursor c = db.rawQuery("SELECT id FROM students WHERE user_id=? AND roll=?",
                new String[]{String.valueOf(userId), roll})) {
            if (c.moveToFirst()) {
                long id = c.getLong(0);
                ContentValues up = new ContentValues();
                up.put("name", name);
                db.update("students", up, "id=?", new String[]{String.valueOf(id)});
                return id;
            }
        }
        ContentValues cv = new ContentValues();
        cv.put("user_id", userId);
        cv.put("roll", roll);
        cv.put("name", name);
        return db.insert("students", null, cv);
    }
}