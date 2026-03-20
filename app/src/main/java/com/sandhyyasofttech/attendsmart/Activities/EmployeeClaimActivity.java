package com.sandhyyasofttech.attendsmart.Activities;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.sandhyyasofttech.attendsmart.Models.ExpenseClaim;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Utils.ImageCompressor;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class EmployeeClaimActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA = 100;
    private static final int REQUEST_GALLERY = 101;
    private static final int PERMISSION_REQUEST_CODE = 102;

    private EditText etAmount, etDescription;
    private TextView tvImageName;
    private ImageView ivBillPreview;
    private Button btnSelectImage, btnSubmit;
    private ProgressBar progressBar;

    private Uri selectedImageUri;
    private PrefManager prefManager;
    private DatabaseReference claimsRef;
    private StorageReference storageRef;

    private boolean isSubmitting = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employee_claim);

        initViews();

        // First check if user is logged in properly
        prefManager = new PrefManager(this);

        // Validate user session
        if (!validateUserSession()) {
            return;
        }

        setupFirebase();
        setupClickListeners();
    }

    private boolean validateUserSession() {
        String companyKey = prefManager.getCompanyKey();
        String userId = prefManager.getUserId();

        if (companyKey == null || companyKey.isEmpty()) {
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_LONG).show();
            finish();
            return false;
        }

        if (userId == null || userId.isEmpty()) {
            // Try to get employee ID
            userId = prefManager.getEmployeeId();
            if (userId == null || userId.isEmpty()) {
                Toast.makeText(this, "User information missing. Please login again.", Toast.LENGTH_LONG).show();
                finish();
                return false;
            }
            prefManager.setUserId(userId);
        }

        return true;
    }
    private void initViews() {
        etAmount = findViewById(R.id.etAmount);
        etDescription = findViewById(R.id.etDescription);
        tvImageName = findViewById(R.id.tvImageName);
        ivBillPreview = findViewById(R.id.ivBillPreview);
        btnSelectImage = findViewById(R.id.btnSelectImage);
        btnSubmit = findViewById(R.id.btnSubmit);
        progressBar = findViewById(R.id.progressBar);

        prefManager = new PrefManager(this);

        // Set action bar title
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Submit Expense Claim");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupFirebase() {
        if (prefManager == null) {
            prefManager = new PrefManager(this);
        }

        String companyKey = prefManager.getCompanyKey();
        if (companyKey == null || companyKey.isEmpty()) {
            Toast.makeText(this, "Company information not found", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        String userId = prefManager.getUserId();
        if (userId == null || userId.isEmpty()) {
            userId = prefManager.getEmployeeId();
            if (userId == null || userId.isEmpty()) {
                userId = prefManager.getEmployeeMobile();
                if (userId == null || userId.isEmpty()) {
                    Toast.makeText(this, "User information missing", Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
            }
            prefManager.setUserId(userId);
        }

        String userName = prefManager.getUserName();
        if (userName == null || userName.isEmpty()) {
            userName = prefManager.getEmployeeName();
            if (userName == null || userName.isEmpty()) {
                userName = "Employee";
            }
            prefManager.setUserName(userName);
        }

        // ✅ IMPORTANT: expenseClaims COMPANY च्या UNDER मध्ये ठेवा
        // Path: Companies > companyKey > expenseClaims
        claimsRef = FirebaseDatabase.getInstance()
                .getReference("Companies")
                .child(companyKey)
                .child("expenseClaims");

        // Storage path same pattern
        storageRef = FirebaseStorage.getInstance()
                .getReference("Companies")
                .child(companyKey)
                .child("expenseClaims")
                .child(userId);

        Log.d("ExpenseClaim", "Path: Companies/" + companyKey + "/expenseClaims");
    }
    private void setupClickListeners() {
        btnSelectImage.setOnClickListener(v -> showImagePickerDialog());
        btnSubmit.setOnClickListener(v -> submitClaim());
    }

    private void showImagePickerDialog() {
        String[] options = {"Camera", "Gallery"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Image");
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                checkCameraPermission();
            } else {
                openGallery();
            }
        });
        builder.show();
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.CAMERA},
                    PERMISSION_REQUEST_CODE);
        } else {
            openCamera();
        }
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, REQUEST_CAMERA);
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_GALLERY);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CAMERA && data != null && data.getExtras() != null) {
                // Handle camera image
                android.graphics.Bitmap bitmap = (android.graphics.Bitmap) data.getExtras().get("data");
                if (bitmap != null) {
                    selectedImageUri = getImageUriFromBitmap(bitmap);
                    displayImagePreview(selectedImageUri);
                }
            } else if (requestCode == REQUEST_GALLERY && data != null && data.getData() != null) {
                selectedImageUri = data.getData();
                displayImagePreview(selectedImageUri);
            }
        }
    }

    private Uri getImageUriFromBitmap(android.graphics.Bitmap bitmap) {
        String path = MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, "BillImage", null);
        return Uri.parse(path);
    }

    private void displayImagePreview(Uri imageUri) {
        Glide.with(this)
                .load(imageUri)
                .centerCrop()
                .into(ivBillPreview);
        ivBillPreview.setVisibility(View.VISIBLE);

        String fileName = getFileNameFromUri(imageUri);
        tvImageName.setText(fileName != null ? fileName : "Image selected");
        tvImageName.setVisibility(View.VISIBLE);
    }

    private String getFileNameFromUri(Uri uri) {
        String fileName = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex);
                    }
                }
            }
        }
        if (fileName == null) {
            fileName = uri.getPath();
            int cut = fileName.lastIndexOf('/');
            if (cut != -1) {
                fileName = fileName.substring(cut + 1);
            }
        }
        return fileName;
    }

    private void submitClaim() {
        if (isSubmitting) {
            Toast.makeText(this, "Please wait, already submitting...", Toast.LENGTH_SHORT).show();
            return;
        }

        String amountStr = etAmount.getText().toString().trim();
        String description = etDescription.getText().toString().trim();

        // Validation
        if (TextUtils.isEmpty(amountStr)) {
            etAmount.setError("Amount is required");
            etAmount.requestFocus();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                etAmount.setError("Amount must be greater than 0");
                etAmount.requestFocus();
                return;
            }
            if (amount > 100000) {
                etAmount.setError("Amount is too high. Please contact admin");
                etAmount.requestFocus();
                return;
            }
        } catch (NumberFormatException e) {
            etAmount.setError("Invalid amount");
            etAmount.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(description)) {
            etDescription.setError("Description is required");
            etDescription.requestFocus();
            return;
        }

        if (description.length() < 10) {
            etDescription.setError("Please provide more details (min 10 characters)");
            etDescription.requestFocus();
            return;
        }

        if (selectedImageUri == null) {
            Toast.makeText(this, "Please select a bill image", Toast.LENGTH_SHORT).show();
            return;
        }

        // All validations passed
        isSubmitting = true;
        showProgress(true);

        // Upload image first
        uploadImageAndSaveClaim(amount, description);
    }

    private void uploadImageAndSaveClaim(double amount, String description) {
        String imageFileName = UUID.randomUUID().toString() + ".jpg";
        StorageReference imageRef = storageRef.child(imageFileName);

        try {
            // Compress image before upload
            File compressedFile = ImageCompressor.saveCompressedImage(this, selectedImageUri, imageFileName);
            Uri compressedUri = Uri.fromFile(compressedFile);

            imageRef.putFile(compressedUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            String imageUrl = uri.toString();
                            saveClaimToDatabase(amount, description, imageUrl);
                        }).addOnFailureListener(e -> {
                            showError("Failed to get image URL: " + e.getMessage());
                        });
                    })
                    .addOnFailureListener(e -> {
                        showError("Image upload failed: " + e.getMessage());
                    });
        } catch (Exception e) {
            showError("Image compression failed: " + e.getMessage());
        }
    }

    private void saveClaimToDatabase(double amount, String description, String imageUrl) {
        String claimId = claimsRef.push().getKey();
        if (claimId == null) {
            showError("Failed to generate claim ID");
            return;
        }

        String timestamp = String.valueOf(System.currentTimeMillis());
        String userId = prefManager.getUserId();
        String userName = prefManager.getUserName();

        if (TextUtils.isEmpty(userName)) {
            userName = prefManager.getEmployeeName();
        }

        ExpenseClaim claim = new ExpenseClaim();
        claim.setClaimId(claimId);
        claim.setUserId(userId);
        claim.setUserName(userName);
        claim.setAmount(amount);
        claim.setDescription(description);
        claim.setImageUrl(imageUrl);
        claim.setStatus("pending");
        claim.setTimestamp(timestamp);

        claimsRef.child(claimId).setValue(claim)
                .addOnSuccessListener(aVoid -> {
                    showProgress(false);
                    isSubmitting = false;
                    Toast.makeText(EmployeeClaimActivity.this,
                            "Claim submitted successfully!", Toast.LENGTH_LONG).show();

                    // Clear form
                    etAmount.setText("");
                    etDescription.setText("");
                    selectedImageUri = null;
                    ivBillPreview.setVisibility(View.GONE);
                    tvImageName.setVisibility(View.GONE);

                    // Finish after 2 seconds
                    new android.os.Handler().postDelayed(() -> finish(), 2000);
                })
                .addOnFailureListener(e -> {
                    showError("Failed to save claim: " + e.getMessage());
                    showProgress(false);
                    isSubmitting = false;
                });
    }

    private void showError(String message) {
        showProgress(false);
        isSubmitting = false;
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();

        // Show retry option
        new AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage(message + "\n\nDo you want to retry?")
                .setPositiveButton("Retry", (dialog, which) -> submitClaim())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnSubmit.setEnabled(!show);
        btnSelectImage.setEnabled(!show);
        etAmount.setEnabled(!show);
        etDescription.setEnabled(!show);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}