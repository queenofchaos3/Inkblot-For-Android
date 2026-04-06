package com.inkblot.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import jp.wasabeef.richeditor.RichEditor;
import org.json.JSONObject;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class WriteFragment extends Fragment {

    private static final String CLOUDINARY_CLOUD = "dxugvmcbc";
    private static final String CLOUDINARY_PRESET = "Inkblot";
    private static final String CLOUDINARY_URL =
            "https://api.cloudinary.com/v1_1/" + CLOUDINARY_CLOUD + "/image/upload";

    private FirebaseFirestore db;
    private String userId;
    private RichEditor richEditor;
    private EditText etTitle;
    private TextView tvWordCount;
    private MaterialButton btnSave;
    private String currentDocId = null;

    private ActivityResultLauncher<Intent> imagePickerLauncher;

    // Autosave
    private final Handler autosaveHandler = new Handler(Looper.getMainLooper());
    private Runnable autosaveRunnable;
    private static final int PAUSE_DELAY_MS = 3000;
    private static final int TIMER_DELAY_MS = 30000;
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK
                            && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        uploadImageToCloudinary(imageUri);
                    }
                });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_write, container, false);

        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        etTitle = view.findViewById(R.id.et_title);
        tvWordCount = view.findViewById(R.id.tv_word_count);
        btnSave = view.findViewById(R.id.btn_save);
        richEditor = view.findViewById(R.id.rich_editor);

        // 🦕 Easter egg: long press word count
        tvWordCount.setOnLongClickListener(v -> {
            showDinoMeme("Why must we 'Yenhance' our programming languages\nWhen you can't even 'yenhance' your language");
            return true;
        });

        richEditor.setEditorBackgroundColor(Color.parseColor("#080D0A"));
        richEditor.setEditorFontColor(Color.parseColor("#E8F5F3"));
        richEditor.setEditorFontSize(16);
        richEditor.setPadding(24, 24, 24, 24);
        richEditor.setPlaceholder("Start writing...");

        Bundle args = getArguments();
        String existingDocId = null;

        if (args != null) {
            existingDocId = args.getString("docId");
            String existingTitle = args.getString("title");
            final String existingContent = args.getString("content");

            if (existingTitle != null) etTitle.setText(existingTitle);

            if (existingContent != null && !existingContent.isEmpty()) {
                richEditor.setOnInitialLoadListener(isReady -> {
                    if (isReady) {
                        String htmlToLoad;
                        if (!existingContent.contains("<") && !existingContent.contains(">")) {
                            htmlToLoad = existingContent
                                    .replace("&", "&amp;")
                                    .replace("\r\n", "<br>")
                                    .replace("\n", "<br>");
                        } else {
                            htmlToLoad = existingContent;
                        }
                        richEditor.setHtml(htmlToLoad);
                    }
                });
            }

            if (existingDocId != null) {
                currentDocId = existingDocId;
                btnSave.setText("Save changes");
                TextView tvScreenTitle = view.findViewById(R.id.tv_screen_title);
                tvScreenTitle.setText("Edit Entry");
            }
        }

        richEditor.setOnTextChangeListener(text -> {
            String plain = text.replaceAll("<[^>]*>", "").trim();
            int wordCount = plain.isEmpty() ? 0 : plain.split("\\s+").length;
            tvWordCount.setText(wordCount + " words");
            scheduleAutosave();
        });

        view.findViewById(R.id.fmt_bold).setOnClickListener(v -> richEditor.setBold());
        view.findViewById(R.id.fmt_italic).setOnClickListener(v -> richEditor.setItalic());
        view.findViewById(R.id.fmt_underline).setOnClickListener(v -> richEditor.setUnderline());
        view.findViewById(R.id.fmt_strike).setOnClickListener(v -> richEditor.setStrikeThrough());
        view.findViewById(R.id.fmt_h1).setOnClickListener(v -> richEditor.setHeading(1));
        view.findViewById(R.id.fmt_h2).setOnClickListener(v -> richEditor.setHeading(2));
        view.findViewById(R.id.fmt_bullet).setOnClickListener(v -> richEditor.setBullets());
        view.findViewById(R.id.fmt_quote).setOnClickListener(v -> richEditor.setBlockquote());
        view.findViewById(R.id.fmt_align_left).setOnClickListener(v -> richEditor.setAlignLeft());
        view.findViewById(R.id.fmt_align_center).setOnClickListener(v -> richEditor.setAlignCenter());
        view.findViewById(R.id.fmt_image).setOnClickListener(v -> openImagePicker());

        btnSave.setOnClickListener(v -> performSave(view, true));
        startBackupTimer(view);

        return view;
    }

    private void scheduleAutosave() {
        autosaveHandler.removeCallbacks(autosaveRunnable);
        autosaveRunnable = () -> performSave(getView(), false);
        autosaveHandler.postDelayed(autosaveRunnable, PAUSE_DELAY_MS);
    }

    private void startBackupTimer(View view) {
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                performSave(view, false);
                timerHandler.postDelayed(this, TIMER_DELAY_MS);
            }
        };
        timerHandler.postDelayed(timerRunnable, TIMER_DELAY_MS);
    }

    private void performSave(View view, boolean isManual) {
        if (etTitle == null || richEditor == null) return;

        String title = etTitle.getText().toString().trim();
        String htmlContent = richEditor.getHtml();

        if (title.isEmpty() || htmlContent == null || htmlContent.trim().isEmpty()) {
            if (isManual) {
                Toast.makeText(getContext(),
                        title.isEmpty() ? "Please add a title!" : "Please write something!",
                        Toast.LENGTH_SHORT).show();
            }
            return;
        }

        String plainText = htmlContent.replaceAll("<[^>]*>", "").trim();
        int wordCount = plainText.isEmpty() ? 0 : plainText.split("\\s+").length;

        if (currentDocId != null) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("title", title);
            updates.put("content", htmlContent);
            updates.put("wordCount", wordCount);
            updates.put("updatedAt", com.google.firebase.firestore.FieldValue.serverTimestamp());

            db.collection("users").document(userId)
                    .collection("entries").document(currentDocId)
                    .update(updates)
                    .addOnSuccessListener(unused -> {
                        if (isManual && view != null) {
                            Toast.makeText(getContext(), "Entry updated! ✦", Toast.LENGTH_SHORT).show();
                            androidx.navigation.Navigation.findNavController(view).popBackStack();
                        } else {
                            showAutosaveIndicator();
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (isManual)
                            Toast.makeText(getContext(), "Failed: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                    });

        } else {
            Map<String, Object> entry = new HashMap<>();
            entry.put("title", title);
            entry.put("content", htmlContent);
            entry.put("wordCount", wordCount);
            entry.put("createdAt", com.google.firebase.firestore.FieldValue.serverTimestamp());
            entry.put("isPublic", false);

            db.collection("users").document(userId)
                    .collection("entries").add(entry)
                    .addOnSuccessListener(ref -> {
                        currentDocId = ref.getId();
                        if (isManual && view != null) {
                            Toast.makeText(getContext(), "Entry saved! ✦", Toast.LENGTH_SHORT).show();
                            etTitle.setText("");
                            richEditor.setHtml("");
                            tvWordCount.setText("0 words");
                            currentDocId = null;
                            updateStreak();
                        } else {
                            showAutosaveIndicator();
                            updateStreak();
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (isManual)
                            Toast.makeText(getContext(), "Failed: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void showAutosaveIndicator() {
        if (getContext() != null) {
            Toast.makeText(getContext(), "Draft autosaved ✦", Toast.LENGTH_SHORT).show();
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void uploadImageToCloudinary(Uri imageUri) {
        Toast.makeText(getContext(), "Uploading image... ✦", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            try {
                InputStream inputStream =
                        requireContext().getContentResolver().openInputStream(imageUri);
                byte[] imageBytes = inputStream.readAllBytes();
                inputStream.close();

                String boundary = "----InkblotBoundary" + System.currentTimeMillis();
                URL url = new URL(CLOUDINARY_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type",
                        "multipart/form-data; boundary=" + boundary);
                conn.setConnectTimeout(30000);
                conn.setReadTimeout(30000);

                OutputStream out = conn.getOutputStream();

                String presetPart = "--" + boundary + "\r\n"
                        + "Content-Disposition: form-data; name=\"upload_preset\"\r\n\r\n"
                        + CLOUDINARY_PRESET + "\r\n";
                out.write(presetPart.getBytes("UTF-8"));

                String filePart = "--" + boundary + "\r\n"
                        + "Content-Disposition: form-data; name=\"file\"; filename=\"prompt.jpg\"\r\n"
                        + "Content-Type: image/jpeg\r\n\r\n";
                out.write(filePart.getBytes("UTF-8"));
                out.write(imageBytes);
                out.write(("\r\n--" + boundary + "--\r\n").getBytes("UTF-8"));
                out.flush();
                out.close();

                int responseCode = conn.getResponseCode();
                InputStream responseStream = responseCode >= 400
                        ? conn.getErrorStream() : conn.getInputStream();
                java.io.BufferedReader br = new java.io.BufferedReader(
                        new java.io.InputStreamReader(responseStream, "UTF-8"));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) response.append(line);

                JSONObject json = new JSONObject(response.toString());
                String imageUrl = json.getString("secure_url");

                new Handler(Looper.getMainLooper()).post(() -> {
                    richEditor.focusEditor();
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        String js = "document.execCommand('insertHTML', false, "
                                + "'<img src=\"" + imageUrl + "\" style=\"max-width:100%;height:auto;margin:8px 0;\" /><br>');";
                        richEditor.evaluateJavascript(js, null);
                        Toast.makeText(getContext(), "Image added! ✦", Toast.LENGTH_SHORT).show();
                    }, 300);
                });

            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(getContext(),
                                "Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        autosaveHandler.removeCallbacksAndMessages(null);
        timerHandler.removeCallbacksAndMessages(null);
    }

    private void updateStreak() {
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    Calendar today = Calendar.getInstance();
                    String todayStr = formatDate(today);

                    long currentStreak = 0;
                    long longestStreak = 0;
                    String lastEntryDate = null;
                    boolean graceUsed = false;

                    if (doc.exists()) {
                        Long storedStreak = doc.getLong("currentStreak");
                        Long storedLongest = doc.getLong("longestStreak");
                        Boolean storedGrace = doc.getBoolean("graceUsed");
                        lastEntryDate = doc.getString("lastEntryDate");

                        if (storedStreak != null) currentStreak = storedStreak;
                        if (storedLongest != null) longestStreak = storedLongest;
                        if (storedGrace != null) graceUsed = storedGrace;
                    }

                    if (lastEntryDate != null && lastEntryDate.equals(todayStr)) return;

                    if (lastEntryDate == null) {
                        currentStreak = 1;
                        graceUsed = false;
                    } else {
                        int daysDiff = daysBetween(lastEntryDate, todayStr);
                        if (daysDiff == 1) {
                            currentStreak += 1;
                            graceUsed = false;
                        } else if (daysDiff == 2 && !graceUsed) {
                            currentStreak += 1;
                            graceUsed = true;
                        } else {
                            currentStreak = 1;
                            graceUsed = false;
                        }
                    }

                    if (currentStreak > longestStreak) longestStreak = currentStreak;

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("currentStreak", currentStreak);
                    updates.put("longestStreak", longestStreak);
                    updates.put("lastEntryDate", todayStr);
                    updates.put("graceUsed", graceUsed);

                    db.collection("users").document(userId)
                            .set(updates, com.google.firebase.firestore.SetOptions.merge());
                });
    }

    private String formatDate(Calendar cal) {
        return cal.get(Calendar.YEAR) + "-"
                + String.format("%02d", cal.get(Calendar.MONTH) + 1) + "-"
                + String.format("%02d", cal.get(Calendar.DAY_OF_MONTH));
    }

    private int daysBetween(String from, String to) {
        try {
            String[] f = from.split("-");
            String[] t = to.split("-");
            Calendar c1 = Calendar.getInstance();
            c1.set(Integer.parseInt(f[0]), Integer.parseInt(f[1]) - 1, Integer.parseInt(f[2]), 0, 0, 0);
            c1.set(Calendar.MILLISECOND, 0);
            Calendar c2 = Calendar.getInstance();
            c2.set(Integer.parseInt(t[0]), Integer.parseInt(t[1]) - 1, Integer.parseInt(t[2]), 0, 0, 0);
            c2.set(Calendar.MILLISECOND, 0);
            long diff = c2.getTimeInMillis() - c1.getTimeInMillis();
            return (int) (diff / (1000 * 60 * 60 * 24));
        } catch (Exception e) {
            return 0;
        }
    }
}