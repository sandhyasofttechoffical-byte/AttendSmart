package com.sandhyyasofttech.attendsmart.Activities;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.sandhyyasofttech.attendsmart.Models.LeaveModel;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class LeaveDetailActivity extends AppCompatActivity {

    public static final String EXTRA_LEAVE_ID     = "leaveId";
    public static final String EXTRA_FROM_DATE    = "fromDate";
    public static final String EXTRA_TO_DATE      = "toDate";
    public static final String EXTRA_LEAVE_TYPE   = "leaveType";
    public static final String EXTRA_HALF_DAY     = "halfDayType";
    public static final String EXTRA_REASON       = "reason";
    public static final String EXTRA_STATUS       = "status";
    public static final String EXTRA_APPLIED_AT   = "appliedAt";
    public static final String EXTRA_APPROVED_BY  = "approvedBy";
    public static final String EXTRA_APPROVED_AT  = "approvedAt";
    public static final String EXTRA_ADMIN_REASON = "adminReason";
    public static final String EXTRA_IS_PAID      = "isPaid";
    public static final String EXTRA_EMP_NAME     = "employeeName";
    public static final String EXTRA_EMP_MOBILE   = "employeeMobile";

    private static final int RC_EDIT = 1001;

    // Views
    private TextView tvStatus, tvLeaveType, tvDateRange, tvDuration;
    private TextView tvReason, tvAppliedOn, tvApprovedBy, tvApprovedOn;
    private TextView tvAdminNote, tvPaidStatus, tvEmployeeName;
    private LinearLayout rowApprovedBy, rowApprovedOn, rowAdminNote, rowPaid;
    private MaterialCardView cardAdminSection;
    private MaterialButton btnEdit, btnDelete;

    private LeaveModel leave;
    private String companyKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leave_detail);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.blue_800));
        }

        extractIntentData();
        initViews();
        setupToolbar();
        bindData();
        setupButtons();
    }

    private void extractIntentData() {
        Intent i = getIntent();
        leave = new LeaveModel();
        leave.leaveId       = i.getStringExtra(EXTRA_LEAVE_ID);
        leave.fromDate      = i.getStringExtra(EXTRA_FROM_DATE);
        leave.toDate        = i.getStringExtra(EXTRA_TO_DATE);
        leave.leaveType     = i.getStringExtra(EXTRA_LEAVE_TYPE);
        leave.halfDayType   = i.getStringExtra(EXTRA_HALF_DAY);
        leave.reason        = i.getStringExtra(EXTRA_REASON);
        leave.status        = i.getStringExtra(EXTRA_STATUS);
        leave.appliedAt     = i.getLongExtra(EXTRA_APPLIED_AT, 0);
        leave.approvedBy    = i.getStringExtra(EXTRA_APPROVED_BY);
        leave.adminReason   = i.getStringExtra(EXTRA_ADMIN_REASON);
        leave.employeeName  = i.getStringExtra(EXTRA_EMP_NAME);
        leave.employeeMobile= i.getStringExtra(EXTRA_EMP_MOBILE);

        long approvedAtMs   = i.getLongExtra(EXTRA_APPROVED_AT, 0);
        leave.approvedAt    = approvedAtMs > 0 ? approvedAtMs : null;

        boolean isPaid      = i.getBooleanExtra(EXTRA_IS_PAID, false);
        boolean hasPaid     = i.hasExtra(EXTRA_IS_PAID);
        leave.isPaid        = hasPaid ? isPaid : null;

        companyKey = new PrefManager(this).getCompanyKey();
    }

    private void initViews() {
        tvStatus        = findViewById(R.id.tvDetailStatus);
        tvLeaveType     = findViewById(R.id.tvDetailLeaveType);
        tvDateRange     = findViewById(R.id.tvDetailDateRange);
        tvDuration      = findViewById(R.id.tvDetailDuration);
        tvReason        = findViewById(R.id.tvDetailReason);
        tvAppliedOn     = findViewById(R.id.tvDetailAppliedOn);
        tvApprovedBy    = findViewById(R.id.tvDetailApprovedBy);
        tvApprovedOn    = findViewById(R.id.tvDetailApprovedOn);
        tvAdminNote     = findViewById(R.id.tvDetailAdminNote);
        tvPaidStatus    = findViewById(R.id.tvDetailPaidStatus);
        tvEmployeeName  = findViewById(R.id.tvDetailEmployeeName);
        rowApprovedBy   = findViewById(R.id.rowApprovedBy);
        rowApprovedOn   = findViewById(R.id.rowApprovedOn);
        rowAdminNote    = findViewById(R.id.rowAdminNote);
        rowPaid         = findViewById(R.id.rowPaid);
        cardAdminSection= findViewById(R.id.cardAdminSection);
        btnEdit         = findViewById(R.id.btnEdit);
        btnDelete       = findViewById(R.id.btnDelete);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Leave Details");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }

    private void bindData() {
        // Employee name
        String name = leave.employeeName != null ? leave.employeeName : "—";
        tvEmployeeName.setText(name);

        // Status badge
        tvStatus.setText(leave.status != null ? capitalize(leave.status) : "Pending");
        applyStatusStyle(tvStatus, leave.status);

        // Leave type
        String typeText = formatLeaveType(leave.leaveType);
        if (leave.halfDayType != null && !leave.halfDayType.isEmpty()) {
            typeText += " · " + capitalize(leave.halfDayType.replace("_", " "));
        }
        tvLeaveType.setText(typeText);

        // Date range
        tvDateRange.setText(formatDateRange(leave.fromDate, leave.toDate));

        // Duration
        tvDuration.setText(calcDuration(leave.fromDate, leave.toDate, leave.leaveType));

        // Reason
        tvReason.setText(leave.reason != null && !leave.reason.isEmpty() ? leave.reason : "—");

        // Applied on
        tvAppliedOn.setText(leave.appliedAt > 0 ? formatTimestamp(leave.appliedAt) : "—");

        // Admin section visibility
        String status = leave.status != null ? leave.status.toUpperCase() : "PENDING";
        boolean isProcessed = status.equals("APPROVED") || status.equals("REJECTED");

        if (isProcessed) {
            cardAdminSection.setVisibility(View.VISIBLE);

            // Approved by
            if (leave.approvedBy != null && !leave.approvedBy.isEmpty()) {
                rowApprovedBy.setVisibility(View.VISIBLE);
                tvApprovedBy.setText(leave.approvedBy);
            } else {
                rowApprovedBy.setVisibility(View.GONE);
            }

            // Approved on
            if (leave.approvedAt != null && leave.approvedAt > 0) {
                rowApprovedOn.setVisibility(View.VISIBLE);
                tvApprovedOn.setText(formatTimestamp(leave.approvedAt));
            } else {
                rowApprovedOn.setVisibility(View.GONE);
            }

            // Admin note / rejection reason
            if (leave.adminReason != null && !leave.adminReason.isEmpty()) {
                rowAdminNote.setVisibility(View.VISIBLE);
                tvAdminNote.setText(leave.adminReason);
            } else {
                rowAdminNote.setVisibility(View.GONE);
            }

            // Paid status (only for approved)
            if (status.equals("APPROVED") && leave.isPaid != null) {
                rowPaid.setVisibility(View.VISIBLE);
                tvPaidStatus.setText(leave.isPaid ? "Paid Leave" : "Unpaid Leave");
                tvPaidStatus.setTextColor(leave.isPaid
                        ? Color.parseColor("#2E7D32")
                        : Color.parseColor("#C62828"));
            } else {
                rowPaid.setVisibility(View.GONE);
            }

        } else {
            cardAdminSection.setVisibility(View.GONE);
        }
    }

    private void setupButtons() {
        String status = leave.status != null ? leave.status.toUpperCase() : "PENDING";
        boolean canModify = status.equals("PENDING");

        if (canModify) {
            btnEdit.setVisibility(View.VISIBLE);
            btnDelete.setVisibility(View.VISIBLE);

            btnEdit.setOnClickListener(v -> openEditLeave());
            btnDelete.setOnClickListener(v -> confirmDelete());
        } else {
            // Hide both buttons — cannot edit/delete a confirmed leave
            btnEdit.setVisibility(View.GONE);
            btnDelete.setVisibility(View.GONE);
        }
    }

    private void openEditLeave() {
        Intent i = new Intent(this, ApplyLeaveActivity.class);
        i.putExtra(ApplyLeaveActivity.EXTRA_EDIT_MODE,  true);
        i.putExtra(ApplyLeaveActivity.EXTRA_LEAVE_ID,   leave.leaveId);
        i.putExtra(ApplyLeaveActivity.EXTRA_FROM_DATE,  leave.fromDate);
        i.putExtra(ApplyLeaveActivity.EXTRA_TO_DATE,    leave.toDate);
        i.putExtra(ApplyLeaveActivity.EXTRA_LEAVE_TYPE, leave.leaveType);
        i.putExtra(ApplyLeaveActivity.EXTRA_HALF_DAY,   leave.halfDayType);
        i.putExtra(ApplyLeaveActivity.EXTRA_REASON,     leave.reason);
        startActivityForResult(i, RC_EDIT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_EDIT && resultCode == RESULT_OK) {
            // Refresh & close — parent list will reload from Firebase
            setResult(RESULT_OK);
            finish();
        }
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Leave Request")
                .setMessage("Are you sure you want to delete this leave request? This action cannot be undone.")
                .setPositiveButton("Delete", (d, w) -> deleteLeave())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteLeave() {
        if (companyKey == null || leave.leaveId == null) {
            Toast.makeText(this, "Unable to delete. Invalid data.", Toast.LENGTH_SHORT).show();
            return;
        }

        btnDelete.setEnabled(false);
        btnEdit.setEnabled(false);

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("Companies")
                .child(companyKey)
                .child("leaves")
                .child(leave.leaveId);

        ref.removeValue()
                .addOnSuccessListener(a -> {
                    Toast.makeText(this, "Leave request deleted.", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to delete. Try again.", Toast.LENGTH_SHORT).show();
                    btnDelete.setEnabled(true);
                    btnEdit.setEnabled(true);
                });
    }

    // ─── Helpers ────────────────────────────────────────────────────────────────

    private void applyStatusStyle(TextView tv, String status) {
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(40f);
        bg.setPadding(32, 12, 32, 12);

        if (status == null) status = "PENDING";
        switch (status.toUpperCase()) {
            case "APPROVED":
                bg.setColor(Color.parseColor("#E8F5E9"));
                tv.setTextColor(Color.parseColor("#2E7D32"));
                break;
            case "REJECTED":
                bg.setColor(Color.parseColor("#FFEBEE"));
                tv.setTextColor(Color.parseColor("#C62828"));
                break;
            case "PENDING":
                bg.setColor(Color.parseColor("#FFF3E0"));
                tv.setTextColor(Color.parseColor("#EF6C00"));
                break;
            default:
                bg.setColor(Color.parseColor("#E3F2FD"));
                tv.setTextColor(Color.parseColor("#1976D2"));
                break;
        }
        tv.setBackground(bg);
    }

    private String formatLeaveType(String type) {
        if (type == null) return "—";
        String[] words = type.replace("_", " ").toLowerCase(Locale.getDefault()).split(" ");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!w.isEmpty()) sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }

    private String formatDateRange(String from, String to) {
        try {
            SimpleDateFormat in  = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat out = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            Date d1 = in.parse(from);
            Date d2 = in.parse(to);
            if (d1 != null && d2 != null) {
                if (from.equals(to)) return out.format(d1);
                SimpleDateFormat dm = new SimpleDateFormat("dd MMM", Locale.getDefault());
                SimpleDateFormat yr = new SimpleDateFormat("yyyy", Locale.getDefault());
                return dm.format(d1) + " – " + dm.format(d2) + " " + yr.format(d2);
            }
        } catch (Exception ignored) {}
        return from + " → " + to;
    }

    private String calcDuration(String from, String to, String type) {
        if (type != null && type.toUpperCase().contains("HALF")) return "0.5 day";
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date d1 = sdf.parse(from);
            Date d2 = sdf.parse(to);
            if (d1 != null && d2 != null) {
                long days = TimeUnit.MILLISECONDS.toDays(d2.getTime() - d1.getTime()) + 1;
                return days + (days == 1 ? " day" : " days");
            }
        } catch (Exception ignored) {}
        return "—";
    }

    private String formatTimestamp(long ts) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
        return sdf.format(new Date(ts));
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }
}