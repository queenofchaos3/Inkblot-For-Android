package com.inkblot.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class UserProfileFragment extends Fragment {

    private FirebaseFirestore db;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user_profile, container, false);

        db = FirebaseFirestore.getInstance();

        TextView btnBack = view.findViewById(R.id.btn_back);
        TextView tvUsername = view.findViewById(R.id.tv_profile_username);
        TextView tvAbout = view.findViewById(R.id.tv_profile_about);
        LinearLayout entriesContainer = view.findViewById(R.id.profile_entries_container);

        Bundle args = getArguments();
        String targetUserId = args != null ? args.getString("targetUserId") : null;
        String targetUsername = args != null ? args.getString("targetUsername") : "Writer";

        tvUsername.setText("@" + targetUsername);
        btnBack.setOnClickListener(v -> Navigation.findNavController(view).popBackStack());

        if (targetUserId == null) return view;

        // Load about blurb
        db.collection("users").document(targetUserId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String about = doc.getString("about");
                        tvAbout.setText(about != null && !about.isEmpty()
                                ? about : "This writer hasn't added a bio yet.");
                    }
                });

        // Load public entries
        db.collection("users").document(targetUserId).collection("entries")
                .whereEqualTo("isPublic", true)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (snapshots.isEmpty()) {
                        TextView empty = new TextView(requireContext());
                        empty.setText("No public entries yet.");
                        empty.setTextColor(getResources().getColor(R.color.text_muted, null));
                        empty.setTextSize(13);
                        entriesContainer.addView(empty);
                        return;
                    }

                    for (QueryDocumentSnapshot doc : snapshots) {
                        String docId = doc.getId();
                        String title = doc.getString("title");
                        String content = doc.getString("content");
                        Long words = doc.getLong("wordCount");
                        long finalWords = words != null ? words : 0;
                        String finalContent = content;

                        CardView card = new CardView(requireContext());
                        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT);
                        cardParams.setMargins(0, 0, 0, 16);
                        card.setLayoutParams(cardParams);
                        card.setCardBackgroundColor(
                                getResources().getColor(R.color.bg_card, null));
                        card.setRadius(24f);
                        card.setContentPadding(40, 28, 40, 28);

                        LinearLayout cardContent = new LinearLayout(requireContext());
                        cardContent.setOrientation(LinearLayout.VERTICAL);

                        TextView titleView = new TextView(requireContext());
                        titleView.setText(title);
                        titleView.setTextColor(
                                getResources().getColor(R.color.text_primary, null));
                        titleView.setTextSize(17);
                        titleView.setPadding(0, 0, 0, 8);

                        // Plain text preview
                        String plain = content != null
                                ? content.replaceAll("<[^>]*>", "").trim() : "";
                        String preview = plain.length() > 100
                                ? plain.substring(0, 100) + "..." : plain;

                        TextView previewView = new TextView(requireContext());
                        previewView.setText(preview);
                        previewView.setTextColor(
                                getResources().getColor(R.color.text_muted, null));
                        previewView.setTextSize(13);
                        previewView.setPadding(0, 0, 0, 8);

                        int actualWords = plain.isEmpty() ? 0
                                : plain.split("\\s+").length;
                        TextView wordView = new TextView(requireContext());
                        wordView.setText(actualWords + " words");
                        wordView.setTextColor(
                                getResources().getColor(R.color.neon_cyan, null));
                        wordView.setTextSize(12);

                        cardContent.addView(titleView);
                        cardContent.addView(previewView);
                        cardContent.addView(wordView);
                        card.addView(cardContent);
                        entriesContainer.addView(card);

                        // Tap → read full entry (read-only, no edit)
                        card.setOnClickListener(v -> {
                            Bundle entryArgs = new Bundle();
                            entryArgs.putString("docId", docId);
                            entryArgs.putString("title", title);
                            entryArgs.putString("content", finalContent);
                            entryArgs.putLong("wordCount", finalWords);
                            Navigation.findNavController(view)
                                    .navigate(R.id.entryDetailFragment, entryArgs);
                        });
                    }
                });

        return view;
    }
}