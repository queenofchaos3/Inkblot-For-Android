package com.inkblot.app;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommunityFragment extends Fragment {

    private FirebaseFirestore db;
    private String userId;
    private LinearLayout postsContainer;
    private View rootView;
    private final List<PostData> allPosts = new ArrayList<>();

    private static class PostData {
        String authorName, authorId, content;
        PostData(String authorName, String authorId, String content) {
            this.authorName = authorName;
            this.authorId = authorId;
            this.content = content;
        }
    }

    private void navigateToProfile(String targetUserId, String targetUsername) {
        if (targetUserId == null || targetUserId.isEmpty()) return;
        try {
            NavController nav = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);
            Bundle args = new Bundle();
            args.putString("targetUserId", targetUserId);
            args.putString("targetUsername", targetUsername);
            nav.navigate(R.id.userProfileFragment, args);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Couldn't open profile", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_community, container, false);

        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        EditText etPost = rootView.findViewById(R.id.et_post);
        EditText etSearch = rootView.findViewById(R.id.et_search_users);
        MaterialButton btnPost = rootView.findViewById(R.id.btn_post);
        postsContainer = rootView.findViewById(R.id.posts_container);

        loadPosts();

        // 🦕 Easter egg: tap search bar 5 times fast
        final int[] tapCount = {0};
        final Handler tapHandler = new Handler(Looper.getMainLooper());
        etSearch.setOnClickListener(v -> {
            tapCount[0]++;
            tapHandler.removeCallbacksAndMessages(null);
            tapHandler.postDelayed(() -> tapCount[0] = 0, 800);
            if (tapCount[0] >= 5) {
                tapCount[0] = 0;
                showDinoMeme("Photosynthesis or parasynthesis\nThat is the question");
            }
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim().toLowerCase();
                if (query.isEmpty()) {
                    renderPosts(allPosts);
                } else {
                    searchUsers(query);
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnPost.setOnClickListener(v -> {
            String content = etPost.getText().toString().trim();
            if (content.isEmpty()) {
                Toast.makeText(getContext(), "Write something first!", Toast.LENGTH_SHORT).show();
                return;
            }

            db.collection("users").document(userId).get()
                    .addOnSuccessListener(doc -> {
                        String username = doc.getString("username");
                        if (username == null || username.isEmpty()) {
                            username = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
                        }
                        if (username == null) username = "Anonymous";

                        Map<String, Object> post = new HashMap<>();
                        post.put("authorName", username);
                        post.put("authorId", userId);
                        post.put("content", content);
                        post.put("createdAt", com.google.firebase.firestore.FieldValue.serverTimestamp());

                        db.collection("community").add(post)
                                .addOnSuccessListener(ref -> {
                                    Toast.makeText(getContext(), "Posted! ✦", Toast.LENGTH_SHORT).show();
                                    etPost.setText("");
                                    loadPosts();
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(getContext(), "Failed: " + e.getMessage(),
                                                Toast.LENGTH_SHORT).show());
                    });
        });

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadPosts();
    }

    private void loadPosts() {
        allPosts.clear();
        db.collection("community")
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshots -> {
                    for (QueryDocumentSnapshot doc : querySnapshots) {
                        String author = doc.getString("authorName");
                        String authorId = doc.getString("authorId");
                        String content = doc.getString("content");
                        allPosts.add(new PostData(
                                author != null ? author : "Anonymous",
                                authorId != null ? authorId : "",
                                content != null ? content : ""));
                    }
                    renderPosts(allPosts);
                });
    }

    private void searchUsers(String query) {
        postsContainer.removeAllViews();
        db.collection("users").get()
                .addOnSuccessListener(snapshots -> {
                    boolean found = false;
                    for (QueryDocumentSnapshot doc : snapshots) {
                        String username = doc.getString("username");
                        String about = doc.getString("about");
                        String targetUserId = doc.getId();
                        if (username != null && username.toLowerCase().contains(query)) {
                            found = true;
                            addUserCard(username, about, targetUserId);
                        }
                    }
                    if (!found) {
                        TextView empty = new TextView(requireContext());
                        empty.setText("No writers found for \"" + query + "\"");
                        empty.setTextColor(getResources().getColor(R.color.text_muted, null));
                        empty.setTextSize(13);
                        empty.setPadding(0, 16, 0, 0);
                        postsContainer.addView(empty);
                    }
                });
    }

    private void addUserCard(String username, String about, String targetUserId) {
        CardView card = new CardView(requireContext());
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, 16);
        card.setLayoutParams(cardParams);
        card.setCardBackgroundColor(getResources().getColor(R.color.bg_card, null));
        card.setRadius(24f);
        card.setContentPadding(40, 28, 40, 28);

        LinearLayout content = new LinearLayout(requireContext());
        content.setOrientation(LinearLayout.VERTICAL);

        TextView nameView = new TextView(requireContext());
        nameView.setText("@" + username);
        nameView.setTextColor(getResources().getColor(R.color.neon_cyan, null));
        nameView.setTextSize(16);
        nameView.setPadding(0, 0, 0, 8);

        TextView aboutView = new TextView(requireContext());
        aboutView.setText(about != null && !about.isEmpty() ? about : "No bio yet.");
        aboutView.setTextColor(getResources().getColor(R.color.text_muted, null));
        aboutView.setTextSize(13);

        content.addView(nameView);
        content.addView(aboutView);
        card.addView(content);
        postsContainer.addView(card);

        card.setOnClickListener(v -> navigateToProfile(targetUserId, username));
    }

    private void renderPosts(List<PostData> posts) {
        postsContainer.removeAllViews();

        if (posts.isEmpty()) {
            TextView empty = new TextView(requireContext());
            empty.setText("No posts yet. Be the first!");
            empty.setTextColor(getResources().getColor(R.color.text_muted, null));
            empty.setTextSize(14);
            postsContainer.addView(empty);
            return;
        }

        for (PostData post : posts) {
            CardView card = new CardView(requireContext());
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            cardParams.setMargins(0, 0, 0, 16);
            card.setLayoutParams(cardParams);
            card.setCardBackgroundColor(getResources().getColor(R.color.bg_card, null));
            card.setRadius(24f);
            card.setContentPadding(40, 28, 40, 28);

            LinearLayout cardContent = new LinearLayout(requireContext());
            cardContent.setOrientation(LinearLayout.VERTICAL);

            TextView authorView = new TextView(requireContext());
            authorView.setText("@" + post.authorName);
            authorView.setTextColor(getResources().getColor(R.color.neon_cyan, null));
            authorView.setTextSize(13);
            authorView.setPadding(0, 0, 0, 8);

            TextView contentView = new TextView(requireContext());
            contentView.setText(post.content);
            contentView.setTextColor(getResources().getColor(R.color.text_primary, null));
            contentView.setTextSize(15);

            cardContent.addView(authorView);
            cardContent.addView(contentView);
            card.addView(cardContent);
            postsContainer.addView(card);

            final String authorId = post.authorId;
            final String authorName = post.authorName;
            authorView.setOnClickListener(v -> navigateToProfile(authorId, authorName));
        }
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