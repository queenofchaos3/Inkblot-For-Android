package com.inkblot.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class UsernameSetupActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_username_setup);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        EditText etUsername = findViewById(R.id.et_setup_username);
        EditText etAbout = findViewById(R.id.et_setup_about);
        MaterialButton btnContinue = findViewById(R.id.btn_setup_continue);

        btnContinue.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            String about = etAbout.getText().toString().trim();

            if (username.isEmpty()) {
                Toast.makeText(this, "Please choose a username", Toast.LENGTH_SHORT).show();
                return;
            }
            if (username.length() < 3) {
                Toast.makeText(this, "Username must be at least 3 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> data = new HashMap<>();
            data.put("username", username);
            data.put("about", about);

            db.collection("users").document(userId)
                    .set(data, com.google.firebase.firestore.SetOptions.merge())
                    .addOnSuccessListener(unused -> {
                        startActivity(new Intent(this, MainActivity.class));
                        finish();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show());
        });
    }
}