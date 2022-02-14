package com.github.windore.mtca.ui;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.github.windore.mtca.MtcApplication;
import com.github.windore.mtca.R;
import com.github.windore.mtca.mtc.Mtc;
import com.github.windore.mtca.mtc.MtcItem;

import java.sql.Date;
import java.time.Instant;
import java.util.Calendar;
import java.util.List;

public class EventCheckReceiver extends BroadcastReceiver {
    private final String CHANNEL_ID = "EventNotifications";

    @Override
    public void onReceive(Context context, Intent intent) {
        Mtc mtc = ((MtcApplication) context.getApplicationContext()).getMtc();
        if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
            checkForNotifications(context, mtc);
            setAlarm(context);
        } else if ("CHECK_NOTIFICATIONS".equals(intent.getAction())) {
            checkForNotifications(context, mtc);
            setAlarm(context);
        }
    }

    private void checkForNotifications(Context context, Mtc mtc) {
        List<MtcItem> eventsForToday = mtc.getItemsForDate(MtcItem.ItemType.Event, Date.from(Instant.now()));
        createNotificationChannel(context);
        NotificationManagerCompat manager = NotificationManagerCompat.from(context);

        for (int i = 0, eventsForTodaySize = eventsForToday.size(); i < eventsForTodaySize; i++) {
            MtcItem event = eventsForToday.get(i);
            manager.notify(i, createEventNotification(context, event.getString()));
        }
    }

    private void setAlarm(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, EventCheckReceiver.class);
        intent.setAction("CHECK_NOTIFICATIONS");
        PendingIntent alarmIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, 7);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        // If 7:00 Is already in the past then the alarm should be set for tomorrow
        if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), alarmIntent);
    }

    private void createNotificationChannel(Context context) {
        CharSequence name = context.getString(R.string.event_notification_channel);
        String description = context.getString(R.string.event_notification_channel_desc);
        int importance = NotificationManager.IMPORTANCE_HIGH;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
        channel.setDescription(description);
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        manager.createNotificationChannel(channel);
    }

    private Notification createEventNotification(Context context, String message) {
        return new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_baseline_timer_24)
                .setContentTitle(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build();
    }
}
