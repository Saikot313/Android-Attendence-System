package com.example.myapplication.util;

import android.net.Uri;

import com.example.myapplication.db.DatabaseHelper;
import com.example.myapplication.model.AttendanceRecord;
import com.example.myapplication.model.Student;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CloudSync {
    public interface Callback { void onDone(boolean ok, String message); }

    private static final String NOT_SIGNED =
            "Cloud account not connected. Logout and sign in again while online.";

    /** Call after a successful local sign up / sign in. Best-effort, works offline-safe. */
    public static void ensureSignedIn(String email, String password) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        auth.signInWithEmailAndPassword(email, password).addOnFailureListener(e -> {
            if (e instanceof FirebaseAuthInvalidUserException
                    || e instanceof FirebaseAuthInvalidCredentialsException) {
                auth.createUserWithEmailAndPassword(email, password); // ignored if it already exists
            }
        });
    }

    public static void signOut() {
        FirebaseAuth.getInstance().signOut();
    }

    private static DocumentReference root() {
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u == null) return null;
        return FirebaseFirestore.getInstance().collection("users").document(u.getUid());
    }

    // ---------------- Backup: mirror local -> cloud ----------------
    @SuppressWarnings("unchecked")
    public static void upload(DatabaseHelper db, long userId, Callback cb) {
        DocumentReference root = root();
        if (root == null) { cb.onDone(false, NOT_SIGNED); return; }
        CollectionReference sRef = root.collection("students");
        CollectionReference aRef = root.collection("attendance");

        Tasks.whenAllSuccess(sRef.get(), aRef.get()).addOnSuccessListener(results -> {
            List<Object[]> ops = new ArrayList<>(); // {DocumentReference, Map or null(=delete)}
            Set<String> keepS = new HashSet<>();
            Set<String> keepA = new HashSet<>();

            List<Student> students = db.getStudents(userId);
            for (Student s : students) {
                String id = "r_" + Uri.encode(s.roll);
                keepS.add(id);
                Map<String, Object> m = new HashMap<>();
                m.put("roll", s.roll);
                m.put("name", s.name);
                ops.add(new Object[]{sRef.document(id), m});
            }
            List<AttendanceRecord> records = db.getAllAttendance(userId);
            for (AttendanceRecord r : records) {
                String id = "r_" + Uri.encode(r.roll) + "_" + r.date;
                keepA.add(id);
                Map<String, Object> m = new HashMap<>();
                m.put("roll", r.roll);
                m.put("date", r.date);
                m.put("present", r.present);
                ops.add(new Object[]{aRef.document(id), m});
            }
            for (DocumentSnapshot d : ((QuerySnapshot) results.get(0)).getDocuments()) {
                if (!keepS.contains(d.getId())) ops.add(new Object[]{d.getReference(), null});
            }
            for (DocumentSnapshot d : ((QuerySnapshot) results.get(1)).getDocuments()) {
                if (!keepA.contains(d.getId())) ops.add(new Object[]{d.getReference(), null});
            }

            if (ops.isEmpty()) { cb.onDone(true, "Nothing to back up"); return; }

            FirebaseFirestore fs = FirebaseFirestore.getInstance();
            List<Task<Void>> commits = new ArrayList<>();
            for (int i = 0; i < ops.size(); i += 400) {
                WriteBatch batch = fs.batch();
                for (Object[] op : ops.subList(i, Math.min(i + 400, ops.size()))) {
                    DocumentReference ref = (DocumentReference) op[0];
                    if (op[1] == null) batch.delete(ref);
                    else batch.set(ref, (Map<String, Object>) op[1]);
                }
                commits.add(batch.commit());
            }
            Tasks.whenAllSuccess(commits)
                    .addOnSuccessListener(x -> cb.onDone(true,
                            "Backup done: " + students.size() + " students, " + records.size() + " records"))
                    .addOnFailureListener(e -> cb.onDone(false, "Backup failed: " + e.getMessage()));
        }).addOnFailureListener(e -> cb.onDone(false, "Backup failed: " + e.getMessage()));
    }

    // ---------------- Restore: merge cloud -> local ----------------
    public static void download(DatabaseHelper db, long userId, Callback cb) {
        DocumentReference root = root();
        if (root == null) { cb.onDone(false, NOT_SIGNED); return; }

        Tasks.whenAllSuccess(root.collection("students").get(), root.collection("attendance").get())
                .addOnSuccessListener(results -> {
                    List<Student> students = new ArrayList<>();
                    List<AttendanceRecord> records = new ArrayList<>();

                    for (DocumentSnapshot d : ((QuerySnapshot) results.get(0)).getDocuments()) {
                        String roll = d.getString("roll");
                        String name = d.getString("name");
                        if (roll == null || name == null) continue;
                        Student s = new Student();
                        s.roll = roll;
                        s.name = name;
                        students.add(s);
                    }
                    for (DocumentSnapshot d : ((QuerySnapshot) results.get(1)).getDocuments()) {
                        String roll = d.getString("roll");
                        String date = d.getString("date");
                        Boolean present = d.getBoolean("present");
                        if (roll == null || date == null || present == null) continue;
                        records.add(new AttendanceRecord(roll, date, present));
                    }
                    db.restoreFromCloud(userId, students, records);
                    cb.onDone(true, "Restored: " + students.size() + " students, " + records.size() + " records");
                })
                .addOnFailureListener(e -> cb.onDone(false, "Restore failed: " + e.getMessage()));
    }
}