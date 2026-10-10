package com.btlltdd.fitnesstracker.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

import com.btlltdd.fitnesstracker.network.SyncManager;

/**
 * R21: CONNECTIVITY_ACTION.
 * Từ Android 7, receiver khai báo trong manifest không còn nhận sự kiện này,
 * nên FitnessApp đăng ký động lúc process được tạo.
 */
public class NetworkSyncReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (isOnline(context)) {
            SyncManager.sync(context, null);
        }
    }

    public static boolean isOnline(Context context) {
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (manager == null) {
            return false;
        }
        Network network = manager.getActiveNetwork();
        if (network == null) {
            return false;
        }
        NetworkCapabilities capabilities = manager.getNetworkCapabilities(network);
        return capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }
}
