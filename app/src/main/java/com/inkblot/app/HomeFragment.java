package com.inkblot.app;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.Calendar;

public class HomeFragment extends Fragment {

    private FirebaseFirestore db;
    private String userId;
    private View rootView;
    private TextView greeting;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_home, container, false);

        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        greeting = rootView.findViewById(R.id.tv_greeting);
        TextView btnSettings = rootView.findViewById(R.id.btn_settings);

        btnSettings.setOnClickListener(v ->
                Navigation.findNavController(rootView).navigate(R.id.action_home_to_settings));

        setGreeting(null);

        TextView btnAddDeadline = rootView.findViewById(R.id.btn_add_deadline);
        btnAddDeadline.setOnClickListener(v -> {
            AddDeadlineDialogFragment dialog = new AddDeadlineDialogFragment();
            dialog.setOnDeadlineAdded(() -> loadDeadlines());
            dialog.show(getParentFragmentManager(), "add_deadline");
        });

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUsername();
        loadStats();
        loadDeadlines();
    }

    private void setGreeting(String username) {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String timeGreeting;
        if (hour < 12) timeGreeting = "Good morning";
        else if (hour < 17) timeGreeting = "Good afternoon";
        else timeGreeting = "Good evening";

        if (username != null && !username.isEmpty()) {
            greeting.setText(timeGreeting + ", " + username + " ✦");
        } else {
            greeting.setText(timeGreeting + ", writer ✦");
        }
    }

    private void loadUsername() {
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String username = doc.getString("username");
                        setGreeting(username);
                    }
                });
    }

    private void loadStats() {
        TextView tvEntries = rootView.findViewById(R.id.tv_total_entries);
        TextView tvWords = rootView.findViewById(R.id.tv_total_words);
        TextView tvAvg = rootView.findViewById(R.id.tv_avg_words);
        TextView tvStreak = rootView.findViewById(R.id.tv_streak);
        LinearLayout recentContainer = rootView.findViewById(R.id.recent_entries_container);
        recentContainer.removeAllViews();

        // Load streak
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(userDoc -> {
                    if (userDoc.exists()) {
                        Long streak = userDoc.getLong("currentStreak");
                        tvStreak.setText((streak != null ? streak : 0) + "🔥");

                        // 🦕 Easter egg: triple tap the streak
                        final int[] tapCount = {0};
                        final Handler tapHandler = new Handler(Looper.getMainLooper());
                        tvStreak.setOnClickListener(v -> {
                            tapCount[0]++;
                            tapHandler.removeCallbacksAndMessages(null);
                            tapHandler.postDelayed(() -> tapCount[0] = 0, 600);
                            if (tapCount[0] >= 3) {
                                tapCount[0] = 0;
                                showDinoMeme("When you zone out in an exam and the prof looks at you\nSo you gotta pull out this pose");
                            }
                        });
                    }
                });

        // Load entries
        db.collection("users").document(userId).collection("entries")
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshots -> {
                    int totalEntries = querySnapshots.size();
                    long totalWords = 0;

                    for (QueryDocumentSnapshot doc : querySnapshots) {
                        Long words = doc.getLong("wordCount");
                        if (words != null) totalWords += words;
                    }

                    long avgWords = totalEntries > 0 ? totalWords / totalEntries : 0;
                    tvEntries.setText(String.valueOf(totalEntries));
                    tvWords.setText(String.valueOf(totalWords));
                    tvAvg.setText(String.valueOf(avgWords));

                    int count = 0;
                    for (QueryDocumentSnapshot doc : querySnapshots) {
                        if (count >= 3) break;

                        String title = doc.getString("title");
                        String docId = doc.getId();
                        String content = doc.getString("content");
                        Long words = doc.getLong("wordCount");
                        long finalWords = words != null ? words : 0;
                        String finalContent = content;

                        CardView card = new CardView(requireContext());
                        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                        );
                        cardParams.setMargins(0, 0, 0, 12);
                        card.setLayoutParams(cardParams);
                        card.setCardBackgroundColor(getResources().getColor(R.color.bg_card, null));
                        card.setRadius(24f);
                        card.setContentPadding(32, 24, 32, 24);

                        LinearLayout cardContent = new LinearLayout(requireContext());
                        cardContent.setOrientation(LinearLayout.VERTICAL);

                        TextView titleView = new TextView(requireContext());
                        titleView.setText(title);
                        titleView.setTextColor(getResources().getColor(R.color.text_primary, null));
                        titleView.setTextSize(16);

                        TextView wordView = new TextView(requireContext());
                        wordView.setText(finalWords + " words");
                        wordView.setTextColor(getResources().getColor(R.color.text_muted, null));
                        wordView.setTextSize(13);

                        cardContent.addView(titleView);
                        cardContent.addView(wordView);
                        card.addView(cardContent);

                        card.setOnClickListener(v -> {
                            Bundle args = new Bundle();
                            args.putString("docId", docId);
                            args.putString("title", title);
                            args.putString("content", finalContent);
                            args.putLong("wordCount", finalWords);
                            Navigation.findNavController(rootView)
                                    .navigate(R.id.entryDetailFragment, args);
                        });

                        recentContainer.addView(card);
                        count++;
                    }
                });
    }

    private void loadDeadlines() {
        LinearLayout deadlinesContainer = rootView.findViewById(R.id.deadlines_container);
        deadlinesContainer.removeAllViews();

        db.collection("users").document(userId).collection("deadlines")
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (snapshots.isEmpty()) {
                        TextView empty = new TextView(requireContext());
                        empty.setText("No deadlines yet.");
                        empty.setTextColor(getResources().getColor(R.color.text_muted, null));
                        empty.setTextSize(13);
                        deadlinesContainer.addView(empty);
                        return;
                    }

                    Calendar today = Calendar.getInstance();
                    today.set(Calendar.HOUR_OF_DAY, 0);
                    today.set(Calendar.MINUTE, 0);
                    today.set(Calendar.SECOND, 0);
                    today.set(Calendar.MILLISECOND, 0);

                    for (QueryDocumentSnapshot doc : snapshots) {
                        String docId = doc.getId();
                        String title = doc.getString("title");
                        Long year = doc.getLong("year");
                        Long month = doc.getLong("month");
                        Long day = doc.getLong("day");

                        if (title == null || year == null || month == null || day == null) continue;

                        Calendar deadlineCal = Calendar.getInstance();
                        deadlineCal.set(year.intValue(), month.intValue(), day.intValue(), 0, 0, 0);
                        deadlineCal.set(Calendar.MILLISECOND, 0);

                        long diffMs = deadlineCal.getTimeInMillis() - today.getTimeInMillis();
                        long daysLeft = diffMs / (1000 * 60 * 60 * 24);

                        String badge;
                        int badgeColor;
                        if (daysLeft < 0) {
                            badge = "Overdue";
                            badgeColor = android.graphics.Color.parseColor("#FF4444");
                        } else if (daysLeft == 0) {
                            badge = "Due today!";
                            badgeColor = android.graphics.Color.parseColor("#FF4444");
                        } else if (daysLeft == 1) {
                            badge = "Due tomorrow";
                            badgeColor = android.graphics.Color.parseColor("#FF8800");
                        } else if (daysLeft <= 3) {
                            badge = "In " + daysLeft + " days";
                            badgeColor = android.graphics.Color.parseColor("#FFD700");
                        } else {
                            badge = "In " + daysLeft + " days";
                            badgeColor = getResources().getColor(R.color.text_muted, null);
                        }

                        CardView card = new CardView(requireContext());
                        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                        );
                        cardParams.setMargins(0, 0, 0, 12);
                        card.setLayoutParams(cardParams);
                        card.setCardBackgroundColor(getResources().getColor(R.color.bg_card, null));
                        card.setRadius(24f);
                        card.setContentPadding(32, 24, 32, 24);

                        LinearLayout row = new LinearLayout(requireContext());
                        row.setOrientation(LinearLayout.HORIZONTAL);
                        row.setGravity(android.view.Gravity.CENTER_VERTICAL);

                        TextView titleView = new TextView(requireContext());
                        titleView.setText(title);
                        titleView.setTextColor(getResources().getColor(R.color.text_primary, null));
                        titleView.setTextSize(15);
                        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                        titleView.setLayoutParams(titleParams);

                        TextView badgeView = new TextView(requireContext());
                        badgeView.setText(badge);
                        badgeView.setTextColor(badgeColor);
                        badgeView.setTextSize(12);

                        card.setOnLongClickListener(v -> {
                            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                                    .setTitle("Delete deadline?")
                                    .setMessage("\"" + title + "\" will be removed.")
                                    .setPositiveButton("Delete", (dialog, which) -> {
                                        db.collection("users").document(userId)
                                                .collection("deadlines").document(docId)
                                                .delete()
                                                .addOnSuccessListener(unused -> loadDeadlines());
                                    })
                                    .setNegativeButton("Cancel", null)
                                    .show();
                            return true;
                        });

                        row.addView(titleView);
                        row.addView(badgeView);
                        card.addView(row);
                        deadlinesContainer.addView(card);
                    }
                });
    }

    private void showDinoMeme(String text) {
        android.app.AlertDialog.Builder builder =
                new android.app.AlertDialog.Builder(requireContext());
        android.view.View dialogView = android.view.LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_dino_meme, null);
        ((android.widget.ImageView) dialogView.findViewById(R.id.iv_dino))
                .setImageResource(R.drawable.dino_meme);
        ((android.widget.TextView) dialogView.findViewById(R.id.tv_dino_text))
                .setText(text);
        builder.setView(dialogView);
        android.app.AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialogView.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }
}