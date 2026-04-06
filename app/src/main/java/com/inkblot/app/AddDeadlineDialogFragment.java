package com.inkblot.app;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class AddDeadlineDialogFragment extends DialogFragment {

    private int selectedYear, selectedMonth, selectedDay;
    private boolean dateSelected = false;
    private Runnable onDeadlineAdded;

    public void setOnDeadlineAdded(Runnable callback) {
        this.onDeadlineAdded = callback;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_add_deadline, null);

        EditText etTitle = view.findViewById(R.id.et_deadline_title);
        TextView tvDate = view.findViewById(R.id.tv_selected_date);
        Button btnPickDate = view.findViewById(R.id.btn_pick_date);
        Button btnSave = view.findViewById(R.id.btn_save_deadline);
        Button btnCancel = view.findViewById(R.id.btn_cancel_deadline);

        btnPickDate.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new DatePickerDialog(requireContext(), (picker, year, month, day) -> {
                selectedYear = year;
                selectedMonth = month;
                selectedDay = day;
                dateSelected = true;
                tvDate.setText(day + "/" + (month + 1) + "/" + year);
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)).show();
        });

        btnSave.setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            if (title.isEmpty()) {
                Toast.makeText(getContext(), "Please enter a title", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!dateSelected) {
                Toast.makeText(getContext(), "Please pick a date", Toast.LENGTH_SHORT).show();
                return;
            }

            // Save to Firestore
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

            Map<String, Object> deadline = new HashMap<>();
            deadline.put("title", title);
            deadline.put("year", selectedYear);
            deadline.put("month", selectedMonth);
            deadline.put("day", selectedDay);

            db.collection("users").document(userId)
                    .collection("deadlines").add(deadline)
                    .addOnSuccessListener(ref -> {
                        scheduleNotifications(title, selectedYear, selectedMonth, selectedDay);
                        if (onDeadlineAdded != null) onDeadlineAdded.run();
                        dismiss();
                    });
        });

        btnCancel.setOnClickListener(v -> dismiss());

        return new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setView(view)
                .create();
    }

    private void scheduleNotifications(String title, int year, int month, int day) {
        Calendar deadline = Calendar.getInstance();
        deadline.set(year, month, day, 9, 0, 0);
        deadline.set(Calendar.MILLISECOND, 0);

        long now = System.currentTimeMillis();

        // Schedule 3 days before
        long threeDaysBefore = deadline.getTimeInMillis() - TimeUnit.DAYS.toMillis(3);
        if (threeDaysBefore > now) {
            scheduleNotification(title, 3, threeDaysBefore - now);
        }

        // Schedule 1 day before
        long oneDayBefore = deadline.getTimeInMillis() - TimeUnit.DAYS.toMillis(1);
        if (oneDayBefore > now) {
            scheduleNotification(title, 1, oneDayBefore - now);
        }
    }

    private void scheduleNotification(String title, int daysLeft, long delayMs) {
        Data data = new Data.Builder()
                .putString(DeadlineNotificationWorker.KEY_TITLE, title)
                .putInt(DeadlineNotificationWorker.KEY_DAYS_LEFT, daysLeft)
                .build();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(DeadlineNotificationWorker.class)
                .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                .setInputData(data)
                .build();

        WorkManager.getInstance(requireContext()).enqueue(request);
    }
}