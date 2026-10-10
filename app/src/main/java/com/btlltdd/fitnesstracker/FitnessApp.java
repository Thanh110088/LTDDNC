package com.btlltdd.fitnesstracker;

import android.app.Application;
import android.content.IntentFilter;
import android.net.ConnectivityManager;

import com.btlltdd.fitnesstracker.receiver.NetworkSyncReceiver;
import com.btlltdd.fitnesstracker.util.NotificationHelper;
import com.btlltdd.fitnesstracker.util.Receivers;

/**
 * onCreate chạy một lần mỗi process: tạo kênh thông báo và lắng nghe mạng.
 */
public class FitnessApp extends Application {
    private final NetworkSyncReceiver networkSyncReceiver = new NetworkSyncReceiver();

    @Override
    public void onCreate() {
        super.onCreate();
        NotificationHelper.createChannels(this);
        try {
            IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
            Receivers.register(this, networkSyncReceiver, filter, true);
        } catch (RuntimeException ignored) {
            // Máy chặn CONNECTIVITY_ACTION thì mở app vẫn đồng bộ ở MainActivity.onResume.
        }
    }
}
