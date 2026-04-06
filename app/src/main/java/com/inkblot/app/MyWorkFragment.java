package com.inkblot.app;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import java.util.ArrayList;
import java.util.List;

public class MyWorkFragment extends Fragment {

    private FirebaseFirestore db;
    private String userId;
    private LinearLayout entriesContainer;
    private View rootView;
    private EditText etSearch;

    // Cache all entries for search
    private final List<EntryData> allEntries = new ArrayList<>();

    private static class EntryData {
        String docId, title, content;
        long words;
        boolean isPublic;
        EntryData(String docId, String title, String content, long words, boolean isPublic) {
            this.docId = docId; this.title = title; this.content = content;
            this.words = words; this.isPublic = isPublic;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_my_work, container, false);

        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        entriesContainer = rootView.findViewById(R.id.entries_container);
        etSearch = rootView.findViewById(R.id.et_search);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterEntries(s.toString().trim().toLowerCase());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadEntries();
    }

    private void loadEntries() {
        entriesContainer.removeAllViews();
        allEntries.clear();

        db.collection("users").document(userId).collection("entries")
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshots -> {
                    for (QueryDocumentSnapshot doc : querySnapshots) {
                        String docId = doc.getId();
                        String title = doc.getString("title");
                        String content = doc.getString("content");
                        Long words = doc.getLong("wordCount");
                        long finalWords = words != null ? words : 0;
                        Boolean isPublicRaw = doc.getBoolean("isPublic");
                        boolean isPublic = isPublicRaw != null && isPublicRaw;
                        allEntries.add(new EntryData(docId, title, content, finalWords, isPublic));
                    }
                    filterEntries(etSearch.getText().toString().trim().toLowerCase());
                });
    }

    private void filterEntries(String query) {
        entriesContainer.removeAllViews();
        for (EntryData entry : allEntries) {
            if (query.isEmpty()
                    || (entry.title != null && entry.title.toLowerCase().contains(query))
                    || (entry.content != null && entry.content.toLowerCase().contains(query))) {
                addEntryCard(entry);
            }
        }
    }

    private void addEntryCard(EntryData entry) {
        String docId = entry.docId;
        String title = entry.title;
        String finalContent = entry.content;
        long finalWords = entry.words;
        boolean isPublic = entry.isPublic;

        CardView card = new CardView(requireContext());
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, 20);
        card.setLayoutParams(cardParams);
        card.setCardBackgroundColor(getResources().getColor(R.color.bg_card, null));
        card.setRadius(24f);
        card.setContentPadding(40, 32, 40, 32);

        LinearLayout cardContent = new LinearLayout(requireContext());
        cardContent.setOrientation(LinearLayout.VERTICAL);

        // Title row + visibility badge
        LinearLayout titleRow = new LinearLayout(requireContext());
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setPadding(0, 0, 0, 10);

        TextView titleView = new TextView(requireContext());
        titleView.setText(title);
        titleView.setTextColor(getResources().getColor(R.color.text_primary, null));
        titleView.setTextSize(18);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        titleView.setLayoutParams(titleParams);

        TextView visibilityBadge = new TextView(requireContext());
        visibilityBadge.setText(isPublic ? "🌍 Public" : "🔒 Private");
        visibilityBadge.setTextSize(11);
        visibilityBadge.setTextColor(isPublic
                ? getResources().getColor(R.color.neon_cyan, null)
                : getResources().getColor(R.color.text_muted, null));

        titleRow.addView(titleView);
        titleRow.addView(visibilityBadge);

        // Preview
        TextView previewView = new TextView(requireContext());
        String plainContent = finalContent != null
                ? finalContent.replaceAll("<[^>]*>", "") : "";
        String preview = plainContent.length() > 100
                ? plainContent.substring(0, 100) + "..." : plainContent;
        previewView.setText(preview);
        previewView.setTextColor(getResources().getColor(R.color.text_muted, null));
        previewView.setTextSize(13);
        previewView.setPadding(0, 0, 0, 10);

        // Word count — recalculate from content for accuracy
        int actualWords = plainContent.trim().isEmpty() ? 0
                : plainContent.trim().split("\\s+").length;
        TextView wordView = new TextView(requireContext());
        wordView.setText(actualWords + " words");
        wordView.setTextColor(getResources().getColor(R.color.neon_cyan, null));
        wordView.setTextSize(12);
        wordView.setPadding(0, 0, 0, 14);

        // Actions row
        LinearLayout actionsRow = new LinearLayout(requireContext());
        actionsRow.setOrientation(LinearLayout.HORIZONTAL);
        actionsRow.setPadding(0, 8, 0, 0);

        TextView editBtn = new TextView(requireContext());
        editBtn.setText("✏ Edit");
        editBtn.setTextColor(getResources().getColor(R.color.neon_cyan, null));
        editBtn.setTextSize(13);
        editBtn.setPadding(0, 0, 48, 0);

        TextView toggleBtn = new TextView(requireContext());
        toggleBtn.setText(isPublic ? "🔒 Make Private" : "🌍 Make Public");
        toggleBtn.setTextColor(getResources().getColor(R.color.text_muted, null));
        toggleBtn.setTextSize(13);
        toggleBtn.setPadding(0, 0, 48, 0);

        TextView deleteBtn = new TextView(requireContext());
        deleteBtn.setText("🗑 Delete");
        deleteBtn.setTextColor(getResources().getColor(R.color.text_muted, null));
        deleteBtn.setTextSize(13);

        actionsRow.addView(editBtn);
        actionsRow.addView(toggleBtn);
        actionsRow.addView(deleteBtn);

        cardContent.addView(titleRow);
        cardContent.addView(previewView);
        cardContent.addView(wordView);
        cardContent.addView(actionsRow);
        card.addView(cardContent);
        entriesContainer.addView(card);

        // Tap card → detail
        card.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString("docId", docId);
            args.putString("title", title);
            args.putString("content", finalContent);
            args.putLong("wordCount", finalWords);
            androidx.navigation.Navigation.findNavController(rootView)
                    .navigate(R.id.entryDetailFragment, args);
        });

        // Edit
        editBtn.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString("docId", docId);
            args.putString("title", title);
            args.putString("content", finalContent);
            args.putLong("wordCount", finalWords);
            androidx.navigation.Navigation.findNavController(rootView)
                    .navigate(R.id.writeFragment, args);
        });

        // Public/Private toggle
        final boolean[] currentlyPublic = {isPublic};
        toggleBtn.setOnClickListener(v -> {
            boolean newValue = !currentlyPublic[0];
            db.collection("users").document(userId)
                    .collection("entries").document(docId)
                    .update("isPublic", newValue)
                    .addOnSuccessListener(unused -> {
                        currentlyPublic[0] = newValue;
                        toggleBtn.setText(newValue ? "🔒 Make Private" : "🌍 Make Public");
                        visibilityBadge.setText(newValue ? "🌍 Public" : "🔒 Private");
                        visibilityBadge.setTextColor(newValue
                                ? getResources().getColor(R.color.neon_cyan, null)
                                : getResources().getColor(R.color.text_muted, null));
                        Toast.makeText(getContext(),
                                newValue ? "Entry is now public" : "Entry is now private",
                                Toast.LENGTH_SHORT).show();
                    });
        });

        // Delete
        deleteBtn.setOnClickListener(v ->
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle("Delete entry?")
                        .setMessage("This can't be undone.")
                        .setPositiveButton("Delete", (dialog, which) ->
                                db.collection("users").document(userId)
                                        .collection("entries").document(docId)
                                        .delete()
                                        .addOnSuccessListener(unused -> {
                                            Toast.makeText(getContext(),
                                                    "Entry deleted", Toast.LENGTH_SHORT).show();
                                            loadEntries();
                                        }))
                        .setNegativeButton("Cancel", null)
                        .show());
    }
}