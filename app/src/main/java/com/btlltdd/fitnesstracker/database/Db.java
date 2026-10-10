package com.btlltdd.fitnesstracker.database;

import android.database.Cursor;

final class Db {
    private Db() {
    }

    static String str(Cursor cursor, String column) {
        int index = cursor.getColumnIndex(column);
        if (index < 0 || cursor.isNull(index)) {
            return "";
        }
        return cursor.getString(index);
    }

    static long lng(Cursor cursor, String column) {
        int index = cursor.getColumnIndex(column);
        if (index < 0 || cursor.isNull(index)) {
            return 0;
        }
        return cursor.getLong(index);
    }

    static int num(Cursor cursor, String column) {
        return (int) lng(cursor, column);
    }

    static double dbl(Cursor cursor, String column) {
        int index = cursor.getColumnIndex(column);
        if (index < 0 || cursor.isNull(index)) {
            return 0;
        }
        return cursor.getDouble(index);
    }

    static boolean flag(Cursor cursor, String column) {
        return lng(cursor, column) == 1;
    }
}
