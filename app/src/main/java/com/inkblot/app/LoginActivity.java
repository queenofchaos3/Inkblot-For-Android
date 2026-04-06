package com.inkblot.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.*;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseFirestore db;

    private final ActivityResultLauncher<Intent> signInLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                try {
                    GoogleSignInAccount account = task.getResult(ApiException.class);
                    firebaseAuthWithGoogle(account.getIdToken());
                } catch (ApiException e) {
                    Toast.makeText(this, "Sign in failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // If already logged in, check username then route
        if (mAuth.getCurrentUser() != null) {
            checkUsernameAndRoute(mAuth.getCurrentUser().getUid());
            return;
        }

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        findViewById(R.id.btn_google_signin).setOnClickListener(v -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            signInLauncher.launch(signInIntent);
        });
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        checkUsernameAndRoute(mAuth.getCurrentUser().getUid());
                    } else {
                        Toast.makeText(this, "Authentication failed!", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkUsernameAndRoute(String userId) {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(doc -> {
                    String username = doc.exists() ? doc.getString("username") : null;
                    if (username == null || username.isEmpty()) {
                        // First time — go to username setup
                        startActivity(new Intent(this, UsernameSetupActivity.class));
                    } else {
                        // Already has username — go straight to main
                        startActivity(new Intent(this, MainActivity.class));
                    }
                    finish();
                })
                .addOnFailureListener(e -> {
                    // On failure, go to main anyway
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                });
    }
}