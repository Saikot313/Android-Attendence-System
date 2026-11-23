package com.example.myapplication.util;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF = "session";
    private final SharedPreferences sp;

    public SessionManager(Context c) {
        sp = c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public void login(long userId, String name) {
        sp.edit().putLong("user_id", userId).putString("user_name", name).apply();
    }

    public boolean isLoggedIn() { return sp.getLong("user_id", -1) != -1; }
    public long getUserId() { return sp.getLong("user_id", -1); }
    public String getUserName() { return sp.getString("user_name", ""); }
    public void logout() { sp.edit().clear().apply(); }
}