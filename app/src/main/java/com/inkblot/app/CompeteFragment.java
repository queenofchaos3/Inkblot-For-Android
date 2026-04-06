package com.inkblot.app;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class CompeteFragment extends Fragment {

    private FirebaseFirestore db;
    private String userId;
    private boolean isAdmin = false;
    private LinearLayout competitionsContainer;
    private View rootView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_compete, container, false);

        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        competitionsContainer = rootView.findViewById(R.id.competitions_container);

        TextView btnAdd = rootView.findViewById(R.id.btn_add_competition);

        db.collection("users").document(userId).get()
                .addOnSuccessListener(doc -> {
                    Boolean admin = doc.getBoolean("isAdmin");
                    isAdmin = admin != null && admin;
                    btnAdd.setVisibility(View.VISIBLE);
                    loadCompetitions();
                });

        btnAdd.setOnClickListener(v -> showCompetitionDialog(null, null));

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadCompetitions();
    }

    private void loadCompetitions() {
        competitionsContainer.removeAllViews();

        db.collection("competitions")
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshots -> {
                    android.util.Log.d("COMPETE", "Docs found: " + snapshots.size());

                    if (snapshots.isEmpty()) {
                        TextView empty = new TextView(requireContext());
                        empty.setText("No competitions yet. Check back soon!");
                        empty.setTextColor(getResources().getColor(R.color.text_muted, null));
                        empty.setTextSize(14);
                        competitionsContainer.addView(empty);
                        return;
                    }

                    for (QueryDocumentSnapshot doc : snapshots) {
                        String docId = doc.getId();
                        String name = doc.getString("name");
                        String description = doc.getString("description");
                        String deadline = doc.getString("deadline");
                        String status = doc.getString("status");
                        String mode = doc.getString("mode");
                        String location = doc.getString("location");
                        String eventDate = doc.getString("eventDate");
                        String eventTime = doc.getString("eventTime");
                        Long deadlineYear = doc.getLong("deadlineYear");
                        Long deadlineMonth = doc.getLong("deadlineMonth");
                        Long deadlineDay = doc.getLong("deadlineDay");

                        android.util.Log.d("COMPETE", "Doc: " + docId + " name=" + name);

                        if (name == null) continue;

                        CardView card = new CardView(requireContext());
                        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                        );
                        cardParams.setMargins(0, 0, 0, 16);
                        card.setLayoutParams(cardParams);
                        card.setCardBackgroundColor(getResources().getColor(R.color.bg_card, null));
                        card.setRadius(24f);
                        card.setContentPadding(32, 24, 32, 24);

                        LinearLayout cardContent = new LinearLayout(requireContext());
                        cardContent.setOrientation(LinearLayout.VERTICAL);

                        // Status + mode row
                        LinearLayout topRow = new LinearLayout(requireContext());
                        topRow.setOrientation(LinearLayout.HORIZONTAL);
                        topRow.setPadding(0, 0, 0, 8);

                        TextView statusView = new TextView(requireContext());
                        statusView.setText(status != null ? status : "Upcoming");
                        statusView.setTextSize(11);
                        statusView.setPadding(0, 0, 16, 0);
                        boolean ongoing = "Ongoing".equals(status);
                        statusView.setTextColor(ongoing
                                ? getResources().getColor(R.color.neon_green, null)
                                : getResources().getColor(R.color.text_muted, null));

                        TextView modeView = new TextView(requireContext());
                        modeView.setText(mode != null ? "· " + mode : "");
                        modeView.setTextSize(11);
                        modeView.setTextColor(getResources().getColor(R.color.neon_cyan, null));

                        topRow.addView(statusView);
                        topRow.addView(modeView);

                        TextView nameView = new TextView(requireContext());
                        nameView.setText(name);
                        nameView.setTextColor(getResources().getColor(R.color.text_primary, null));
                        nameView.setTextSize(17);
                        nameView.setPadding(0, 4, 0, 8);

                        TextView descView = new TextView(requireContext());
                        descView.setText(description);
                        descView.setTextColor(getResources().getColor(R.color.text_muted, null));
                        descView.setTextSize(13);
                        descView.setPadding(0, 0, 0, 8);

                        cardContent.addView(topRow);
                        cardContent.addView(nameView);
                        cardContent.addView(descView);

                        if (location != null && !location.isEmpty()) {
                            TextView locationView = new TextView(requireContext());
                            locationView.setText("📍 " + location);
                            locationView.setTextColor(getResources().getColor(R.color.text_muted, null));
                            locationView.setTextSize(12);
                            locationView.setPadding(0, 0, 0, 8);
                            cardContent.addView(locationView);
                        }

                        if (eventDate != null && !eventDate.isEmpty()) {
                            TextView eventView = new TextView(requireContext());
                            String eventStr = "🗓 " + eventDate;
                            if (eventTime != null && !eventTime.isEmpty()) eventStr += " at " + eventTime;
                            eventView.setText(eventStr);
                            eventView.setTextColor(getResources().getColor(R.color.neon_green, null));
                            eventView.setTextSize(12);
                            eventView.setPadding(0, 0, 0, 8);
                            cardContent.addView(eventView);
                        }

                        if (deadline != null && !deadline.isEmpty()) {
                            TextView deadlineView = new TextView(requireContext());
                            deadlineView.setText("⏰ Deadline: " + deadline);
                            deadlineView.setTextColor(getResources().getColor(R.color.neon_cyan, null));
                            deadlineView.setTextSize(12);
                            deadlineView.setPadding(0, 0, 0, 12);
                            cardContent.addView(deadlineView);
                        }

                        if (deadlineYear != null && deadlineMonth != null && deadlineDay != null) {
                            final long yr = deadlineYear, mo = deadlineMonth, dy = deadlineDay;
                            final String finalName = name;
                            TextView addDeadlineBtn = new TextView(requireContext());
                            addDeadlineBtn.setText("+ Add to my deadlines");
                            addDeadlineBtn.setTextColor(getResources().getColor(R.color.neon_green, null));
                            addDeadlineBtn.setTextSize(12);
                            addDeadlineBtn.setPadding(0, 0, 0, 8);
                            addDeadlineBtn.setOnClickListener(v -> {
                                Map<String, Object> dl = new HashMap<>();
                                dl.put("title", finalName);
                                dl.put("year", yr);
                                dl.put("month", mo);
                                dl.put("day", dy);
                                db.collection("users").document(userId)
                                        .collection("deadlines").add(dl)
                                        .addOnSuccessListener(ref -> Toast.makeText(getContext(),
                                                "Added to your deadlines!", Toast.LENGTH_SHORT).show())
                                        .addOnFailureListener(e -> Toast.makeText(getContext(),
                                                "Failed to add deadline", Toast.LENGTH_SHORT).show());
                            });
                            cardContent.addView(addDeadlineBtn);
                        }

                        if (isAdmin) {
                            LinearLayout adminRow = new LinearLayout(requireContext());
                            adminRow.setOrientation(LinearLayout.HORIZONTAL);
                            adminRow.setPadding(0, 8, 0, 0);

                            TextView editBtn = new TextView(requireContext());
                            editBtn.setText("✏ Edit");
                            editBtn.setTextColor(getResources().getColor(R.color.neon_cyan, null));
                            editBtn.setTextSize(13);
                            editBtn.setPadding(0, 0, 40, 0);

                            TextView deleteBtn = new TextView(requireContext());
                            deleteBtn.setText("🗑 Delete");
                            deleteBtn.setTextColor(getResources().getColor(R.color.text_muted, null));
                            deleteBtn.setTextSize(13);

                            final String finalDocId = docId;
                            final String finalName = name;
                            final String finalDesc = description;
                            final String finalDeadline = deadline;
                            final String finalStatus = status;
                            final String finalMode = mode;
                            final String finalLocation = location;
                            final String finalEventDate = eventDate;
                            final String finalEventTime = eventTime;
                            final Long finalYear = deadlineYear;
                            final Long finalMonth = deadlineMonth;
                            final Long finalDay = deadlineDay;

                            editBtn.setOnClickListener(v -> {
                                Map<String, Object> existing = new HashMap<>();
                                existing.put("name", finalName);
                                existing.put("description", finalDesc);
                                existing.put("deadline", finalDeadline);
                                existing.put("status", finalStatus);
                                existing.put("mode", finalMode);
                                existing.put("location", finalLocation);
                                existing.put("eventDate", finalEventDate);
                                existing.put("eventTime", finalEventTime);
                                if (finalYear != null) existing.put("deadlineYear", finalYear);
                                if (finalMonth != null) existing.put("deadlineMonth", finalMonth);
                                if (finalDay != null) existing.put("deadlineDay", finalDay);
                                showCompetitionDialog(finalDocId, existing);
                            });

                            deleteBtn.setOnClickListener(v ->
                                    new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                                            .setTitle("Delete competition?")
                                            .setMessage("This can't be undone.")
                                            .setPositiveButton("Delete", (dialog, which) ->
                                                    db.collection("competitions").document(finalDocId)
                                                            .delete()
                                                            .addOnSuccessListener(unused -> {
                                                                Toast.makeText(getContext(), "Deleted",
                                                                        Toast.LENGTH_SHORT).show();
                                                                loadCompetitions();
                                                            }))
                                            .setNegativeButton("Cancel", null)
                                            .show());

                            adminRow.addView(editBtn);
                            adminRow.addView(deleteBtn);
                            cardContent.addView(adminRow);
                        }

                        card.addView(cardContent);
                        competitionsContainer.addView(card);
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("COMPETE", "Error loading: " + e.toString());
                    Toast.makeText(getContext(), "Failed to load: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void showCompetitionDialog(String docId, Map<String, Object> existing) {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_add_competition, null);

        EditText etName = dialogView.findViewById(R.id.et_comp_name);
        EditText etDesc = dialogView.findViewById(R.id.et_comp_desc);
        EditText etLocation = dialogView.findViewById(R.id.et_comp_location);
        TextView tvDate = dialogView.findViewById(R.id.tv_comp_date);
        TextView btnPickDate = dialogView.findViewById(R.id.btn_comp_pick_date);
        TextView tvEventDate = dialogView.findViewById(R.id.tv_comp_event_date);
        TextView btnPickEventDate = dialogView.findViewById(R.id.btn_comp_pick_event_date);
        TextView tvTime = dialogView.findViewById(R.id.tv_comp_time);
        TextView btnPickTime = dialogView.findViewById(R.id.btn_comp_pick_time);
        TextView chipOngoing = dialogView.findViewById(R.id.chip_ongoing);
        TextView chipUpcoming = dialogView.findViewById(R.id.chip_upcoming);
        TextView chipOnline = dialogView.findViewById(R.id.chip_online);
        TextView chipOffline = dialogView.findViewById(R.id.chip_offline);

        final String[] selectedStatus = {"Upcoming"};
        final String[] selectedMode = {"Online"};
        final int[] deadlineDate = {0, 0, 0};
        final boolean[] deadlinePicked = {false};
        final String[] deadlineStr = {""};
        final String[] eventDateStr = {""};
        final String[] eventTimeStr = {""};

        java.util.function.Consumer<Boolean> updateStatusChips = ongoing -> {
            chipOngoing.setTextColor(getResources().getColor(
                    ongoing ? R.color.bg_dark : R.color.text_muted, null));
            chipOngoing.setBackgroundColor(getResources().getColor(
                    ongoing ? R.color.neon_green : R.color.bg_dark, null));
            chipUpcoming.setTextColor(getResources().getColor(
                    !ongoing ? R.color.bg_dark : R.color.text_muted, null));
            chipUpcoming.setBackgroundColor(getResources().getColor(
                    !ongoing ? R.color.neon_cyan : R.color.bg_dark, null));
        };

        java.util.function.Consumer<Boolean> updateModeChips = online -> {
            chipOnline.setTextColor(getResources().getColor(
                    online ? R.color.bg_dark : R.color.text_muted, null));
            chipOnline.setBackgroundColor(getResources().getColor(
                    online ? R.color.neon_cyan : R.color.bg_dark, null));
            chipOffline.setTextColor(getResources().getColor(
                    !online ? R.color.bg_dark : R.color.text_muted, null));
            chipOffline.setBackgroundColor(getResources().getColor(
                    !online ? R.color.neon_green : R.color.bg_dark, null));
            etLocation.setVisibility(!online ? View.VISIBLE : View.GONE);
        };

        if (existing != null) {
            if (existing.get("name") != null) etName.setText((String) existing.get("name"));
            if (existing.get("description") != null) etDesc.setText((String) existing.get("description"));
            if (existing.get("location") != null) etLocation.setText((String) existing.get("location"));
            if (existing.get("deadline") != null) {
                deadlineStr[0] = (String) existing.get("deadline");
                tvDate.setText(deadlineStr[0]);
                deadlinePicked[0] = true;
            }
            if (existing.get("eventDate") != null) {
                eventDateStr[0] = (String) existing.get("eventDate");
                tvEventDate.setText(eventDateStr[0]);
            }
            if (existing.get("eventTime") != null) {
                eventTimeStr[0] = (String) existing.get("eventTime");
                tvTime.setText(eventTimeStr[0]);
            }
            if (existing.get("deadlineYear") != null) {
                deadlineDate[0] = ((Long) existing.get("deadlineYear")).intValue();
                deadlineDate[1] = ((Long) existing.get("deadlineMonth")).intValue();
                deadlineDate[2] = ((Long) existing.get("deadlineDay")).intValue();
            }
            if ("Ongoing".equals(existing.get("status"))) selectedStatus[0] = "Ongoing";
            if ("Offline".equals(existing.get("mode"))) selectedMode[0] = "Offline";
        }

        updateStatusChips.accept("Ongoing".equals(selectedStatus[0]));
        updateModeChips.accept("Online".equals(selectedMode[0]));

        chipOngoing.setOnClickListener(v -> {
            selectedStatus[0] = "Ongoing";
            updateStatusChips.accept(true);
        });
        chipUpcoming.setOnClickListener(v -> {
            selectedStatus[0] = "Upcoming";
            updateStatusChips.accept(false);
        });
        chipOnline.setOnClickListener(v -> {
            selectedMode[0] = "Online";
            updateModeChips.accept(true);
        });
        chipOffline.setOnClickListener(v -> {
            selectedMode[0] = "Offline";
            updateModeChips.accept(false);
        });

        btnPickDate.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new DatePickerDialog(requireContext(), (picker, year, month, day) -> {
                deadlineDate[0] = year;
                deadlineDate[1] = month;
                deadlineDate[2] = day;
                deadlinePicked[0] = true;
                deadlineStr[0] = (month + 1) + "/" + day + "/" + year;
                tvDate.setText(deadlineStr[0]);
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
        });

        btnPickEventDate.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new DatePickerDialog(requireContext(), (picker, year, month, day) -> {
                eventDateStr[0] = (month + 1) + "/" + day + "/" + year;
                tvEventDate.setText(eventDateStr[0]);
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
        });

        btnPickTime.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new TimePickerDialog(requireContext(), (picker, hour, minute) -> {
                eventTimeStr[0] = String.format("%02d:%02d", hour, minute);
                tvTime.setText(eventTimeStr[0]);
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show();
        });

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setPositiveButton(docId == null ? "Add" : "Save", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String desc = etDesc.getText().toString().trim();
                    String loc = etLocation.getText().toString().trim();

                    if (name.isEmpty()) {
                        Toast.makeText(getContext(), "Please enter a name", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Map<String, Object> data = new HashMap<>();
                    data.put("name", name);
                    data.put("description", desc);
                    data.put("status", selectedStatus[0]);
                    data.put("mode", selectedMode[0]);
                    data.put("location", "Offline".equals(selectedMode[0]) ? loc : "");
                    data.put("deadline", deadlineStr[0]);
                    data.put("eventDate", eventDateStr[0]);
                    data.put("eventTime", eventTimeStr[0]);
                    if (deadlinePicked[0]) {
                        data.put("deadlineYear", deadlineDate[0]);
                        data.put("deadlineMonth", deadlineDate[1]);
                        data.put("deadlineDay", deadlineDate[2]);
                    }

                    if (docId == null) {
                        data.put("createdAt",
                                com.google.firebase.firestore.FieldValue.serverTimestamp());
                        db.collection("competitions").add(data)
                                .addOnSuccessListener(ref -> {
                                    android.util.Log.d("COMPETE", "Added: " + ref.getId());
                                    Toast.makeText(getContext(), "Competition added!",
                                            Toast.LENGTH_SHORT).show();
                                    loadCompetitions();
                                })
                                .addOnFailureListener(e -> {
                                    android.util.Log.e("COMPETE", "Add failed: " + e.toString());
                                    Toast.makeText(getContext(), "Failed: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        db.collection("competitions").document(docId).update(data)
                                .addOnSuccessListener(unused -> {
                                    Toast.makeText(getContext(), "Competition updated!",
                                            Toast.LENGTH_SHORT).show();
                                    loadCompetitions();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(getContext(), "Failed: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                });
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}