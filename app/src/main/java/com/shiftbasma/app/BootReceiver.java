package com.shiftbasma.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action)
                && !"android.intent.action.QUICKBOOT_POWERON".equals(action)) {
            return;
        }

        SharedPreferences sp = context.getSharedPreferences("shift_prefs", Context.MODE_PRIVATE);
        String checkIn = sp.getString("open_checkin", null);
        float hours = sp.getFloat("open_hours", 8.5f);

        if (checkIn != null) {
            AlarmScheduler.scheduleNext(context, checkIn, hours);
        }
    }
}
