package com.inkblot.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class SettingsFragment extends Fragment {

    private FirebaseFirestore db;
    private String userId;
    private GoogleSignInClient mGoogleSignInClient;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        TextView btnBack = view.findViewById(R.id.btn_back);
        EditText etUsername = view.findViewById(R.id.et_username);
        EditText etAbout = view.findViewById(R.id.et_about);
        MaterialButton btnSave = view.findViewById(R.id.btn_save_username);
        TextView tvEmail = view.findViewById(R.id.tv_email);
        MaterialButton btnSignOut = view.findViewById(R.id.btn_sign_out);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(requireContext(), gso);

        String email = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        tvEmail.setText(email != null ? email : "");

        // Load existing username + about
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String username = doc.getString("username");
                        String about = doc.getString("about");
                        if (username != null && !username.isEmpty()) {
                            etUsername.setText(username);
                            etUsername.setSelection(username.length());
                            setSaveButtonSaved(btnSave);
                        }
                        if (about != null && !about.isEmpty()) {
                            etAbout.setText(about);
                        }
                    }
                });

        // Re-enable save on any change
        android.text.TextWatcher watcher = new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                btnSave.setEnabled(true);
                btnSave.setAlpha(1f);
                btnSave.setText("Save profile");
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        };
        etUsername.addTextChangedListener(watcher);
        etAbout.addTextChangedListener(watcher);

        btnBack.setOnClickListener(v -> Navigation.findNavController(view).popBackStack());

        btnSave.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            String about = etAbout.getText().toString().trim();

            if (username.isEmpty()) {
                Toast.makeText(getContext(), "Please enter a username", Toast.LENGTH_SHORT).show();
                return;
            }
            if (username.length() < 3) {
                Toast.makeText(getContext(), "Username must be at least 3 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> data = new HashMap<>();
            data.put("username", username);
            data.put("about", about);

            db.collection("users").document(userId)
                    .set(data, com.google.firebase.firestore.SetOptions.merge())
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(getContext(), "Profile saved! ✦", Toast.LENGTH_SHORT).show();
                        setSaveButtonSaved(btnSave);
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(getContext(), "Failed: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show());
        });

        btnSignOut.setOnClickListener(v ->
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle("Sign out?")
                        .setMessage("You'll be asked to choose a Google account on next sign in.")
                        .setPositiveButton("Sign out", (dialog, which) -> {
                            FirebaseAuth.getInstance().signOut();
                            mGoogleSignInClient.signOut().addOnCompleteListener(task ->
                                    mGoogleSignInClient.revokeAccess().addOnCompleteListener(t -> {
                                        Intent intent = new Intent(requireActivity(), LoginActivity.class);
                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(intent);
                                    }));
                        })
                        .setNegativeButton("Cancel", null)
                        .show());

        return view;
    }

    private void setSaveButtonSaved(MaterialButton btn) {
        btn.setText("Saved ✓");
        btn.setEnabled(false);
        btn.setAlpha(0.5f);
    }
}