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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.sandhyyasofttech.attendsmart.Adapters.ExpenseItemAdapter;
import com.sandhyyasofttech.attendsmart.Models.ExpenseClaim;
import com.sandhyyasofttech.attendsmart.Models.ExpenseItem;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Utils.ImageCompressor;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;
import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class EmployeeClaimActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA = 100;
    private static final int REQUEST_GALLERY = 101;
    private static final int PERMISSION_REQUEST_CODE = 102;

    // Views
    private Spinner spinnerCategory;
    private EditText etItemAmount, etItemDescription;
    private Button btnAddItem, btnSubmitClaim;
    private TextView tvTotalAmount;
    private RecyclerView recyclerItems;
    private ProgressBar progressBar;
    private LinearLayout addItemLayout;

    // Adapter and Data
    private ExpenseItemAdapter itemAdapter;
    private List<ExpenseItem> expenseItems = new ArrayList<>();
    private DecimalFormat df = new DecimalFormat("#,##0.00");

    // Temp storage for current item being added
    private Uri currentBillImageUri;
    private String currentBillImageUrl;
    private boolean isAddingItem = false;

    // Firebase
    private PrefManager prefManager;
    private DatabaseReference claimsRef;
    private StorageReference storageRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employee_claim);

        initViews();
        setupRecyclerView();

        prefManager = new PrefManager(this);

        if (!validateUserSession()) {
            return;
        }

        setupFirebase();
        setupClickListeners();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("New Expense Claim");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void initViews() {
        spinnerCategory = findViewById(R.id.spinnerCategory);
        etItemAmount = findViewById(R.id.etItemAmount);
        etItemDescription = findViewById(R.id.etItemDescription);
        btnAddItem = findViewById(R.id.btnAddItem);
        btnSubmitClaim = findViewById(R.id.btnSubmitClaim);
        tvTotalAmount = findViewById(R.id.tvTotalAmount);
        recyclerItems = findViewById(R.id.recyclerItems);
        progressBar = findViewById(R.id.progressBar);
        addItemLayout = findViewById(R.id.addItemLayout);

        // Setup category spinner
        ArrayAdapter<CharSequence> categoryAdapter = ArrayAdapter.createFromResource(this,
                R.array.expense_categories, android.R.layout.simple_spinner_item);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(categoryAdapter);
    }

    private void setupRecyclerView() {
        itemAdapter = new ExpenseItemAdapter(position -> {
            expenseItems.remove(position);
            itemAdapter.removeItem(position);
            updateTotalAmount();
        });
        recyclerItems.setLayoutManager(new LinearLayoutManager(this));
        recyclerItems.setAdapter(itemAdapter);
    }

    private boolean validateUserSession() {
        String companyKey = prefManager.getCompanyKey();
        if (companyKey == null || companyKey.isEmpty()) {
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_LONG).show();
            finish();
            return false;
        }
        return true;
    }

    private void setupFirebase() {
        String companyKey = prefManager.getCompanyKey();
        String userId = prefManager.getUserId();

        if (userId == null || userId.isEmpty()) {
            userId = prefManager.getEmployeeId();
            if (userId == null || userId.isEmpty()) {
                userId = prefManager.getEmployeeMobile();
            }
            if (userId != null && !userId.isEmpty()) {
                prefManager.setUserId(userId);
            }
        }

        claimsRef = FirebaseDatabase.getInstance()
                .getReference("Companies")
                .child(companyKey)
                .child("expenseClaims");

        storageRef = FirebaseStorage.getInstance()
                .getReference("Companies")
                .child(companyKey)
                .child("expenseClaims")
                .child(userId);
    }

    private void setupClickListeners() {
        btnAddItem.setOnClickListener(v -> showAddItemDialog());
        btnSubmitClaim.setOnClickListener(v -> submitClaim());
    }

    private void showAddItemDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_expense_item, null);
        builder.setView(dialogView);

        Spinner dialogSpinner = dialogView.findViewById(R.id.dialogSpinnerCategory);
        EditText etAmount = dialogView.findViewById(R.id.dialogEtAmount);
        EditText etDesc = dialogView.findViewById(R.id.dialogEtDescription);
        Button btnSelectImage = dialogView.findViewById(R.id.dialogBtnSelectImage);
        ImageView ivPreview = dialogView.findViewById(R.id.dialogIvPreview);
        TextView tvImageName = dialogView.findViewById(R.id.dialogTvImageName);

        // Setup spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.expense_categories, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dialogSpinner.setAdapter(adapter);

        final Uri[] tempImageUri = {null};
        final String[] tempImageUrl = {null};

        btnSelectImage.setOnClickListener(v -> {
            showImagePickerForItem((uri, url) -> {
                tempImageUri[0] = uri;
                tempImageUrl[0] = url;
                if (uri != null) {
                    Glide.with(this).load(uri).centerCrop().into(ivPreview);
                    ivPreview.setVisibility(View.VISIBLE);
                    tvImageName.setText("Image selected");
                    tvImageName.setVisibility(View.VISIBLE);
                }
            });
        });

        builder.setTitle("Add Expense Item")
                .setPositiveButton("Add", (dialog, which) -> {
                    String category = dialogSpinner.getSelectedItem().toString();
                    String amountStr = etAmount.getText().toString().trim();
                    String description = etDesc.getText().toString().trim();

                    if (TextUtils.isEmpty(amountStr)) {
                        Toast.makeText(this, "Enter amount", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    double amount;
                    try {
                        amount = Double.parseDouble(amountStr);
                        if (amount <= 0) {
                            Toast.makeText(this, "Amount must be > 0", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (TextUtils.isEmpty(description)) {
                        Toast.makeText(this, "Enter description", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (tempImageUri[0] == null) {
                        Toast.makeText(this, "Please select bill image", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Upload image and add item
                    uploadItemImageAndAdd(category, amount, description, tempImageUri[0]);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void uploadItemImageAndAdd(String category, double amount, String description, Uri imageUri) {
        showProgress(true);
        String fileName = UUID.randomUUID().toString() + ".jpg";
        StorageReference imageRef = storageRef.child("items").child(fileName);

        try {
            File compressedFile = ImageCompressor.saveCompressedImage(this, imageUri, fileName);
            Uri compressedUri = Uri.fromFile(compressedFile);

            imageRef.putFile(compressedUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            ExpenseItem item = new ExpenseItem(category, amount, description, uri.toString());
                            expenseItems.add(item);
                            itemAdapter.addItem(item);
                            updateTotalAmount();
                            showProgress(false);
                            Toast.makeText(this, "Item added", Toast.LENGTH_SHORT).show();
                        }).addOnFailureListener(e -> {
                            showProgress(false);
                            Toast.makeText(this, "Failed to get image URL", Toast.LENGTH_SHORT).show();
                        });
                    })
                    .addOnFailureListener(e -> {
                        showProgress(false);
                        Toast.makeText(this, "Image upload failed", Toast.LENGTH_SHORT).show();
                    });
        } catch (Exception e) {
            showProgress(false);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showImagePickerForItem(OnImageSelectedListener listener) {
        String[] options = {"Camera", "Gallery"};
        new AlertDialog.Builder(this)
                .setTitle("Select Bill Image")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        checkCameraPermissionForItem(listener);
                    } else {
                        openGalleryForItem(listener);
                    }
                })
                .show();
    }

    private void checkCameraPermissionForItem(OnImageSelectedListener listener) {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.CAMERA},
                    PERMISSION_REQUEST_CODE);
        } else {
            openCameraForItem(listener);
        }
    }

    private void openCameraForItem(OnImageSelectedListener listener) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, REQUEST_CAMERA);
        currentImageListener = listener;
    }

    private void openGalleryForItem(OnImageSelectedListener listener) {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_GALLERY);
        currentImageListener = listener;
    }

    private OnImageSelectedListener currentImageListener;

    interface OnImageSelectedListener {
        void onImageSelected(Uri uri, String url);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && currentImageListener != null) {
            if (requestCode == REQUEST_CAMERA && data != null && data.getExtras() != null) {
                android.graphics.Bitmap bitmap = (android.graphics.Bitmap) data.getExtras().get("data");
                if (bitmap != null) {
                    String path = MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, "BillImage", null);
                    currentImageListener.onImageSelected(Uri.parse(path), null);
                }
            } else if (requestCode == REQUEST_GALLERY && data != null && data.getData() != null) {
                currentImageListener.onImageSelected(data.getData(), null);
            }
            currentImageListener = null;
        }
    }

    private void updateTotalAmount() {
        double total = 0;
        for (ExpenseItem item : expenseItems) {
            total += item.getAmount();
        }
        tvTotalAmount.setText("Total: ₹" + df.format(total));
    }

    private void submitClaim() {
        if (expenseItems.isEmpty()) {
            Toast.makeText(this, "Please add at least one expense item", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isAddingItem) return;
        isAddingItem = true;
        showProgress(true);

        String claimId = claimsRef.push().getKey();
        if (claimId == null) {
            showError("Failed to generate claim ID");
            return;
        }

        String userId = prefManager.getUserId();
        String userName = prefManager.getUserName();

        if (TextUtils.isEmpty(userName)) {
            userName = prefManager.getEmployeeName();
            if (TextUtils.isEmpty(userName)) userName = "Employee";
        }

        ExpenseClaim claim = new ExpenseClaim();
        claim.setClaimId(claimId);
        claim.setUserId(userId);
        claim.setUserName(userName);
        claim.setItems(expenseItems);
        claim.setStatus("pending");
        claim.setTimestamp(String.valueOf(System.currentTimeMillis()));

        claimsRef.child(claimId).setValue(claim)
                .addOnSuccessListener(aVoid -> {
                    showProgress(false);
                    Toast.makeText(this, "Claim submitted successfully!", Toast.LENGTH_LONG).show();
                    new android.os.Handler().postDelayed(this::finish, 2000);
                })
                .addOnFailureListener(e -> {
                    showError("Failed to save claim: " + e.getMessage());
                    showProgress(false);
                    isAddingItem = false;
                });
    }

    private void showError(String message) {
        showProgress(false);
        isAddingItem = false;
        new AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage(message + "\n\nDo you want to retry?")
                .setPositiveButton("Retry", (dialog, which) -> submitClaim())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnSubmitClaim.setEnabled(!show);
        btnAddItem.setEnabled(!show);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}