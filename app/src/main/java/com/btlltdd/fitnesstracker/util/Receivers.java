package com.btlltdd.fitnesstracker.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.os.Build;

public final class Receivers {
    private Receivers() {
    }

    /**
     * API 33 bắt buộc cờ exported.
     * Broadcast của hệ thống (mạng) dùng exported = true.
     * Broadcast nội bộ từ Service dùng exported = false.
     */
    public static void register(Context context, BroadcastReceiver receiver, IntentFilter filter, boolean exported) {
        if (Build.VERSION.SDK_INT >= 33) {
            context.registerReceiver(
                    receiver,
                    filter,
                    exported ? Context.RECEIVER_EXPORTED : Context.RECEIVER_NOT_EXPORTED);
        } else {
            context.registerReceiver(receiver, filter);
        }
    }
}
