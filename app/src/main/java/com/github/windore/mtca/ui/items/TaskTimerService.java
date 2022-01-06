package com.github.windore.mtca.ui.items;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.github.windore.mtca.MainActivity;
import com.github.windore.mtca.R;

import java.util.Timer;
import java.util.TimerTask;

public class TaskTimerService extends Service {
    public static final String CHANNEL_ID = "taskTimerHigh";
    // Using this static bool is probably the easiest way of limiting services to 1
    // even though it might not be the best way
    private static boolean timerRunning = false;

    private int id;
    private String name;
    private int duration;
    private long timeLeftNanos;

    // There might be a cleaner way other than toggles but these will do.
    private boolean isPaused = false;
    private boolean shouldCancel = false;

    public static boolean isTimerRunning() {
        return timerRunning;
    }

    private PendingIntent getIntent() {
        Intent intent = new Intent(this, MainActivity.class);
        // This is fine since there is only a one activity and this starts it. No back stack needed in my opinion
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        return PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
    }

    private Notification createNotification() {
        String text = formatContent();
        int progress = getProgress();
        Intent togglePauseIntent = new Intent(this, TaskTimerService.class);
        togglePauseIntent.setAction("togglePause");
        PendingIntent pendingTglPIntent = PendingIntent.getService(this, 0, togglePauseIntent, PendingIntent.FLAG_IMMUTABLE);

        Intent cancelIntent = new Intent(this, TaskTimerService.class);
        cancelIntent.setAction("cancel");
        PendingIntent pendingCancelIntent = PendingIntent.getService(this, 0, cancelIntent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_baseline_timer_24)
                .setContentTitle(text)
                .setContentText(name)
                .setProgress(100, progress, false)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(getIntent())
                .setAutoCancel(false)
                .setOnlyAlertOnce(true);

        if (isPaused) {
            builder.addAction(R.drawable.ic_baseline_play_arrow_24, getString(R.string.resume), pendingTglPIntent);
        } else {
            builder.addAction(R.drawable.ic_baseline_pause_24, getString(R.string.pause), pendingTglPIntent);
        }

        builder.addAction(R.drawable.ic_baseline_cancel_24, getString(R.string.cancel), pendingCancelIntent);

        return builder.build();
    }

    private Notification createDoneNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_baseline_timer_24)
                .setContentTitle(getString(R.string.done))
                .setContentText(name)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(getIntent())
                .setAutoCancel(true)
                .build();
    }

    private String formatContent() {
        long timeLeftSeconds = timeLeftNanos / 1_000_000_000;
        long timeLeftMinutes = timeLeftSeconds / 60;
        timeLeftSeconds -= timeLeftMinutes * 60;
        long timeLeftHours = timeLeftMinutes / 60;
        timeLeftMinutes -= timeLeftHours * 60;

        String starting = this.getString(R.string.time_left);
        if (isPaused) {
            starting = this.getString(R.string.paused);
        }

        return starting +
                " " +
                timeLeftHours +
                " " +
                this.getString(R.string.hours_symbol) +
                " " +
                timeLeftMinutes +
                " " +
                this.getString(R.string.minutes_symbol) +
                " " +
                timeLeftSeconds +
                " " +
                this.getString(R.string.seconds_symbol);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        NotificationManagerCompat manager = NotificationManagerCompat.from(this);
        id = 1;

        if ("togglePause".equals(intent.getAction())) {
            isPaused = !isPaused;
            // This causes the notification not to show an proper button pressing animation which is not that great.
            manager.notify(id, createNotification());
            return START_STICKY;
        } else if ("cancel".equals(intent.getAction())) {
            shouldCancel = true;
            return START_STICKY;
        }

        if (isTimerRunning()) {
            throw new IllegalStateException("Timer is already running. Don't restart the timer without stopping it first.");
        }

        timerRunning = true;
        Bundle extras = intent.getExtras();
        name = extras.getString("name");
        duration = (int) extras.getLong("duration");
        timeLeftNanos = (long) duration * 1_000_000_000 * 60;
        isPaused = false;
        shouldCancel = false;

        startForeground(id, createNotification());

        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            private long startTime = System.nanoTime();

            @Override
            public void run() {
                if (shouldCancel || timeLeftNanos < 0) {
                    if (!shouldCancel)
                        // This creates a new notification that is not associated with the service
                        // Only show this if the task isn't cancelled
                        manager.notify(id + 1, createDoneNotification());

                    stopForeground(true);
                    stopSelf();
                    timerRunning = false;
                    timer.cancel();
                    timer.purge();
                } else if (isPaused) {
                    startTime = System.nanoTime();
                } else {
                    long durationNanos = System.nanoTime() - startTime;
                    timeLeftNanos -= durationNanos;
                    manager.notify(id, createNotification());
                    startTime = System.nanoTime();
                }
            }
        }, 0, 1000);

        return START_STICKY;
    }

    private int getProgress() {
        int timeLeftSeconds = (int) Math.round(timeLeftNanos / (double) 1_000_000_000);
        double durationSeconds = duration * 60;
        return (int) ((durationSeconds - timeLeftSeconds) / durationSeconds * 100);
    }
}
