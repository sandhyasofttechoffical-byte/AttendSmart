package com.sandhyyasofttech.attendsmart.Activities;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.firebase.database.*;
import com.sandhyyasofttech.attendsmart.Adapters.ExpenseItemAdapter;
import com.sandhyyasofttech.attendsmart.Models.ExpenseClaim;
import com.sandhyyasofttech.attendsmart.Models.ExpenseItem;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AdminClaimDetailActivity extends AppCompatActivity {

    private TextView tvEmployeeName, tvEmployeeId, tvTotalAmount, tvStatus, tvDate, tvAdminRemarks;
    private RecyclerView recyclerItems;
    private Button btnApprove, btnReject;
    private ProgressBar progressBar;
    private ImageView ivStatusBadge;

    private DatabaseReference claimRef;
    private PrefManager prefManager;
    private ExpenseClaim currentClaim;
    private String claimId;
    private DecimalFormat df = new DecimalFormat("#,##0.00");
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_claim_detail);

        initViews();
        setupToolbar();

        prefManager = new PrefManager(this);
        claimId = getIntent().getStringExtra("claimId");

        if (claimId == null || claimId.isEmpty()) {
            Toast.makeText(this, "Claim not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupFirebase();
        loadClaimDetails();
        setupClickListeners();
    }

    private void initViews() {
        tvEmployeeName = findViewById(R.id.tvEmployeeName);
        tvEmployeeId = findViewById(R.id.tvEmployeeId);
        tvTotalAmount = findViewById(R.id.tvTotalAmount);
        tvStatus = findViewById(R.id.tvStatus);
        tvDate = findViewById(R.id.tvDate);
        tvAdminRemarks = findViewById(R.id.tvAdminRemarks);
        recyclerItems = findViewById(R.id.recyclerItems);
        btnApprove = findViewById(R.id.btnApprove);
        btnReject = findViewById(R.id.btnReject);
        progressBar = findViewById(R.id.progressBar);
        ivStatusBadge = findViewById(R.id.ivStatusBadge);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Claim Details");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupFirebase() {
        String companyKey = prefManager.getCompanyKey();
        claimRef = FirebaseDatabase.getInstance()
                .getReference("Companies")
                .child(companyKey)
                .child("expenseClaims")
                .child(claimId);
    }

    private void loadClaimDetails() {
        showProgress(true);

        claimRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                currentClaim = snapshot.getValue(ExpenseClaim.class);
                if (currentClaim != null) {
                    displayClaimDetails();
                    updateActionButtons();
                } else {
                    Toast.makeText(AdminClaimDetailActivity.this,
                            "Claim not found", Toast.LENGTH_SHORT).show();
                    finish();
                }
                showProgress(false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showProgress(false);
                Toast.makeText(AdminClaimDetailActivity.this,
                        "Error loading claim: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayClaimDetails() {
        // Employee Info
        tvEmployeeName.setText(currentClaim.getUserName() != null ? currentClaim.getUserName() : "Unknown");
        tvEmployeeId.setText("ID: " + (currentClaim.getUserId() != null ? currentClaim.getUserId() : "N/A"));

        // Amount
        tvTotalAmount.setText("₹" + df.format(currentClaim.getTotalAmount()));

        // Status with color
        tvStatus.setText(currentClaim.getStatus().toUpperCase());
        switch (currentClaim.getStatus().toLowerCase()) {
            case "pending":
                tvStatus.setBackgroundResource(R.drawable.bg_status_pending);
                tvStatus.setTextColor(getColor(R.color.status_pending_text));
                ivStatusBadge.setImageResource(R.drawable.bg_status_pending);
                break;
            case "approved":
                tvStatus.setBackgroundResource(R.drawable.calendar_bg_green);
                tvStatus.setTextColor(getColor(R.color.status_approved_text));
                ivStatusBadge.setImageResource(R.drawable.bg_green_solid);
                break;
            case "rejected":
                tvStatus.setBackgroundResource(R.drawable.calendar_bg_red);
                tvStatus.setTextColor(getColor(R.color.status_rejected_text));
                ivStatusBadge.setImageResource(R.drawable.bg_progress_ring_red);
                break;
        }

        // Date
        try {
            long timestamp = Long.parseLong(currentClaim.getTimestamp());
            tvDate.setText(dateFormat.format(new Date(timestamp)));
        } catch (NumberFormatException e) {
            tvDate.setText(currentClaim.getTimestamp());
        }

        // Admin Remarks
        if (currentClaim.getAdminRemarks() != null && !currentClaim.getAdminRemarks().isEmpty()) {
            tvAdminRemarks.setText(currentClaim.getAdminRemarks());
            tvAdminRemarks.setVisibility(View.VISIBLE);
        } else {
            tvAdminRemarks.setVisibility(View.GONE);
        }

        // Items List
        setupItemsRecyclerView();
    }

    private void setupItemsRecyclerView() {
        if (currentClaim.getItems() != null && !currentClaim.getItems().isEmpty()) {
            ExpenseItemAdapter adapter = new ExpenseItemAdapter(null);
            recyclerItems.setLayoutManager(new LinearLayoutManager(this));
            recyclerItems.setAdapter(adapter);

            for (ExpenseItem item : currentClaim.getItems()) {
                adapter.addItem(item);
            }
        } else {
            TextView tvNoItems = new TextView(this);
            tvNoItems.setText("No expense items found");
            tvNoItems.setPadding(16, 16, 16, 16);
            tvNoItems.setTextColor(getColor(R.color.text_secondary));
            recyclerItems.setVisibility(View.GONE);
        }
    }

    private void updateActionButtons() {
        if (currentClaim.getStatus().equals("pending")) {
            btnApprove.setVisibility(View.VISIBLE);
            btnReject.setVisibility(View.VISIBLE);
        } else {
            btnApprove.setVisibility(View.GONE);
            btnReject.setVisibility(View.GONE);
        }
    }

    private void setupClickListeners() {
        btnApprove.setOnClickListener(v -> showApprovalDialog());
        btnReject.setOnClickListener(v -> showRejectionDialog());
    }

    private void showApprovalDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Approve Claim");
        builder.setMessage("Are you sure you want to approve this expense claim of ₹" +
                df.format(currentClaim.getTotalAmount()) + "?");

        // Optional remarks input
        final EditText etRemarks = new EditText(this);
        etRemarks.setHint("Add remarks (optional)");
        etRemarks.setPadding(32, 16, 32, 16);
        builder.setView(etRemarks);

        builder.setPositiveButton("Approve", (dialog, which) -> {
            String remarks = etRemarks.getText().toString().trim();
            updateClaimStatus("approved", remarks);
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showRejectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Reject Claim");

        final EditText etReason = new EditText(this);
        etReason.setHint("Reason for rejection (required)");
        etReason.setPadding(32, 16, 32, 16);
        builder.setView(etReason);

        builder.setPositiveButton("Reject", (dialog, which) -> {
            String reason = etReason.getText().toString().trim();
            if (reason.isEmpty()) {
                Toast.makeText(this, "Please provide a reason for rejection", Toast.LENGTH_SHORT).show();
                return;
            }
            updateClaimStatus("rejected", reason);
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void updateClaimStatus(String status, String remarks) {
        showProgress(true);

        String adminName = prefManager.getUserName();
        if (adminName == null || adminName.isEmpty()) {
            adminName = "Admin";
        }

        claimRef.child("status").setValue(status);
        claimRef.child("adminRemarks").setValue(remarks);
        claimRef.child("approvedBy").setValue(adminName);
        claimRef.child("approvedAt").setValue(String.valueOf(System.currentTimeMillis()));

        claimRef.child("status").setValue(status)
                .addOnSuccessListener(aVoid -> {
                    showProgress(false);
                    String message = status.equals("approved") ?
                            "Claim approved successfully!" : "Claim rejected successfully!";
                    Toast.makeText(AdminClaimDetailActivity.this, message, Toast.LENGTH_LONG).show();

                    // Send notification to employee
                    sendNotificationToEmployee(status, remarks);

                    new android.os.Handler().postDelayed(() -> finish(), 1500);
                })
                .addOnFailureListener(e -> {
                    showProgress(false);
                    Toast.makeText(AdminClaimDetailActivity.this,
                            "Failed to update: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void sendNotificationToEmployee(String status, String remarks) {
        // This can be implemented with FCM
        // For now, just show a toast
        Log.d("AdminClaim", "Notification would be sent to employee: " +
                currentClaim.getUserId() + " about " + status);
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnApprove.setEnabled(!show);
        btnReject.setEnabled(!show);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}