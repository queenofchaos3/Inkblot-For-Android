package com.inkblot.app;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class DeadlineNotificationWorker extends Worker {

    public static final String KEY_TITLE = "title";
    public static final String KEY_DAYS_LEFT = "daysLeft";
    private static final String CHANNEL_ID = "inkblot_deadlines";

    public DeadlineNotificationWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        String title = getInputData().getString(KEY_TITLE);
        int daysLeft = getInputData().getInt(KEY_DAYS_LEFT, 1);

        String message = daysLeft == 1
                ? "\"" + title + "\" is due tomorrow!"
                : "\"" + title + "\" is due in " + daysLeft + " days.";

        NotificationManager manager = (NotificationManager)
                getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

        // Create channel (required for Android 8+)
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "Deadline Alerts", NotificationManager.IMPORTANCE_HIGH);
        manager.createNotificationChannel(channel);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                getApplicationContext(), CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Deadline coming up ✦")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        manager.notify((int) System.currentTimeMillis(), builder.build());

        return Result.success();
    }
}