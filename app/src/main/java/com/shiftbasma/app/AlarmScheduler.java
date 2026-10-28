package com.shiftbasma.app;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.Calendar;

public class AlarmScheduler {

    public static final String CHANNEL_ID = "shift_alarm_channel";
    public static final int NOTIF_ID_MID = 100;
    public static final int NOTIF_ID_END = 101;

    public static void scheduleNext(Context ctx, String checkIn, double shiftHours) {
        try {
            String[] parts = checkIn.split(":");
            int h = Integer.parseInt(parts[0]);
            int m = Integer.parseInt(parts[1]);

            Calendar start = Calendar.getInstance();
            start.set(Calendar.HOUR_OF_DAY, h);
            start.set(Calendar.MINUTE, m);
            start.set(Calendar.SECOND, 0);
            start.set(Calendar.MILLISECOND, 0);

            if (start.getTimeInMillis() < System.currentTimeMillis()) {
                start.add(Calendar.DAY_OF_YEAR, 1);
            }

            long midMs = start.getTimeInMillis() + (long) (shiftHours / 2 * 3600_000L);
            long endMs = start.getTimeInMillis() + (long) (shiftHours * 3600_000L);

            scheduleExact(ctx, midMs, "mid");
            scheduleExact(ctx, endMs, "end");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void scheduleExact(Context ctx, long triggerAt, String type) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        Intent intent = new Intent(ctx, AlarmReceiver.class);
        intent.setAction("com.shiftbasma.app.ALARM");
        intent.putExtra("type", type);

        int reqCode = type.equals("mid") ? NOTIF_ID_MID : NOTIF_ID_END;
        PendingIntent pi = PendingIntent.getBroadcast(ctx, reqCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
        } else {
            AlarmManager.AlarmClockInfo info =
                    new AlarmManager.AlarmClockInfo(triggerAt, pi);
            am.setAlarmClock(info, pi);
        }
    }

    public static void cancelAll(Context ctx) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        for (int reqCode : new int[]{NOTIF_ID_MID, NOTIF_ID_END}) {
            Intent intent = new Intent(ctx, AlarmReceiver.class);
            PendingIntent pi = PendingIntent.getBroadcast(ctx, reqCode, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            am.cancel(pi);
        }
    }

    public static void sendSimpleNotification(Context ctx, String title, String body) {
        createChannel(ctx);
        NotificationCompat.Builder b = new NotificationCompat.Builder(ctx, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);
        try {
            NotificationManagerCompat.from(ctx)
                    .notify((int) (System.currentTimeMillis() % 100000), b.build());
        } catch (SecurityException ignored) {}
    }

    public static void createChannel(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = ctx.getSystemService(NotificationManager.class);
            if (nm != null && nm.getNotificationChannel(CHANNEL_ID) == null) {
                NotificationChannel ch = new NotificationChannel(
                        CHANNEL_ID, "تنبيهات الشفت",
                        NotificationManager.IMPORTANCE_HIGH);
                ch.setDescription("منبهات منتصف ونهاية الدوام");
                nm.createNotificationChannel(ch);
            }
        }
    }
}
