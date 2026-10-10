/**
 * ReminderReceiver: AlarmManager nhắc lịch tập.
 * NetworkSyncReceiver: đăng ký trong code vì Android 8 không giao CONNECTIVITY_ACTION cho receiver khai báo manifest.
 * BootReceiver: đặt lại báo thức sau khi máy khởi động.
 * Cập nhật bước chân gửi bằng LocalBroadcast từ Service tới Fragment.
 */
package com.btlltdd.fitnesstracker.receiver;
