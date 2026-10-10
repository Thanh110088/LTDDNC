package com.btlltdd.fitnesstracker.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.btlltdd.fitnesstracker.model.User;

/**
 * R02: đăng nhập lưu userId và role. Đăng xuất gọi clear().
 */
public final class SessionManager {
    private static final String PREF = "fitness_session";
    private static final String KEY_ID = "user_id";
    private static final String KEY_ROLE = "role";
    private static final String KEY_NAME = "full_name";
    private static final String KEY_USERNAME = "username";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public void save(User user) {
        prefs.edit()
                .putLong(KEY_ID, user.id)
                .putString(KEY_ROLE, user.role)
                .putString(KEY_NAME, user.fullName)
                .putString(KEY_USERNAME, user.username)
                .apply();
    }

    public void clear() {
        prefs.edit().clear().apply();
    }

    public boolean isLoggedIn() {
        return getUserId() > 0;
    }

    public long getUserId() {
        return prefs.getLong(KEY_ID, -1);
    }

    public String getRole() {
        return prefs.getString(KEY_ROLE, "user");
    }

    public String getFullName() {
        return prefs.getString(KEY_NAME, "");
    }

    public boolean isAdmin() {
        return "admin".equals(getRole());
    }
}
