package com.sandhyyasofttech.attendsmart.Adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.sandhyyasofttech.attendsmart.Models.LeaveModel;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

public class AdminLeaveAdapter extends RecyclerView.Adapter<AdminLeaveAdapter.VH> {

    private final List<LeaveModel> list;
    private final DatabaseReference leavesRef;
    private final Context context;

    public AdminLeaveAdapter(Context c, List<LeaveModel> l, DatabaseReference ref) {
        context = c;
        list = l;
        leavesRef = ref;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup p, int v) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.row_admin_leave, p, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int i) {
        LeaveModel m = list.get(i);

        // ✅ NULL SAFE BINDING
        safeSetText(h.tvName, m.employeeName);
        safeSetText(h.tvDates, m.fromDate + " → " + m.toDate);
        safeSetText(h.tvReason, m.reason);
        safeSetText(h.tvStatus, m.status);

        // ✅ HIDE BUTTONS FOR NON-PENDING LEAVES
        boolean isPending = "PENDING".equals(getSafeString(m.status));

        if (isPending) {
            // Show buttons for pending leaves
            h.btnApprove.setVisibility(View.VISIBLE);
            h.btnReject.setVisibility(View.VISIBLE);
            h.btnApprove.setEnabled(true);
            h.btnReject.setEnabled(true);

            // Green color for approve button
            h.btnApprove.setBackgroundColor(context.getResources().getColor(android.R.color.holo_green_dark));
        } else {
            // ✅ HIDE BUTTONS for approved/rejected leaves
            h.btnApprove.setVisibility(View.GONE);
            h.btnReject.setVisibility(View.GONE);
        }

        // Set click listeners (only work when buttons are visible)
        h.btnApprove.setOnClickListener(v -> showApproveDialog(m));
        h.btnReject.setOnClickListener(v -> showRejectDialog(m.leaveId));
    }

    // ✅ HELPER METHODS
    private void safeSetText(TextView tv, String text) {
        if (tv == null) {
            android.util.Log.e("AdminLeaveAdapter", "⚠️ TextView is NULL - Skipping!");
            return;
        }

        if (text == null || text.trim().isEmpty()) {
            tv.setText("N/A");
        } else {
            tv.setText(text);
        }
    }

    private String getSafeString(String value) {
        return value != null ? value : "";
    }

    // ✅ APPROVE LOGIC
    private void showApproveDialog(LeaveModel m) {
        String companyKey = new PrefManager(context).getCompanyKey();

        DatabaseReference salaryRef = FirebaseDatabase.getInstance()
                .getReference("Companies")
                .child(companyKey)
                .child("employees")
                .child(m.employeeMobile)
                .child("salaryConfig")
                .child("paidLeaves");

        salaryRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snap) {
                int allowedPaidLeaves = 0;

                if (snap.exists()) {

                    Object val = snap.getValue();

                    if (val instanceof Number) {

                        // Firebase Long / Double / Integer sagle safe
                        allowedPaidLeaves =
                                ((Number) val).intValue();

                    } else if (val instanceof String) {

                        String value =
                                ((String) val).trim();

                        if (!value.isEmpty()) {
                            try {
                                allowedPaidLeaves =
                                        (int) Double.parseDouble(value);

                            } catch (NumberFormatException e) {

                                allowedPaidLeaves = 0;

                                android.util.Log.e(
                                        "LEAVE_DEBUG",
                                        "Invalid paidLeaves value: " + value,
                                        e
                                );
                            }
                        } else {
                            allowedPaidLeaves = 0;
                        }
                    }
                }

                countUsedPaidLeaves(m, allowedPaidLeaves);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError e) {
                Toast.makeText(context, "Error loading leave data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ✅ COUNT USED PAID LEAVES (CURRENT MONTH ONLY)
// ✅ COUNT USED PAID LEAVES
// Request chya leave month nusar count hoil
    private void countUsedPaidLeaves(
            LeaveModel currentLeave,
            int allowedPaidLeaves
    ) {

        Query q = leavesRef
                .orderByChild("employeeMobile")
                .equalTo(currentLeave.employeeMobile);

        q.addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot snap) {

                double usedPaidLeaves = 0.0;

                try {

                    SimpleDateFormat sdf =
                            new SimpleDateFormat(
                                    "yyyy-MM-dd",
                                    Locale.getDefault()
                            );

                    // Current request kontya month/year chi aahe
                    Date currentFromDate =
                            sdf.parse(currentLeave.fromDate);

                    if (currentFromDate == null) {
                        showDecisionDialog(
                                currentLeave,
                                allowedPaidLeaves,
                                0
                        );
                        return;
                    }

                    Calendar requestCal = Calendar.getInstance();
                    requestCal.setTime(currentFromDate);

                    int requestMonth =
                            requestCal.get(Calendar.MONTH);

                    int requestYear =
                            requestCal.get(Calendar.YEAR);


                    // Employee chya approved leaves check
                    for (DataSnapshot d : snap.getChildren()) {

                        LeaveModel lm =
                                d.getValue(LeaveModel.class);

                        if (lm == null) continue;

                        // Firebase key set kar
                        lm.leaveId = d.getKey();

                        // Current leave skip
                        if (lm.leaveId != null
                                && lm.leaveId.equals(currentLeave.leaveId)) {
                            continue;
                        }

                        // Fakt approved leaves
                        if (!"APPROVED".equalsIgnoreCase(
                                getSafeString(lm.status))) {
                            continue;
                        }

                        // fromDate missing asel tar skip
                        if (lm.fromDate == null
                                || lm.fromDate.trim().isEmpty()) {
                            continue;
                        }

                        Date oldLeaveFrom =
                                sdf.parse(lm.fromDate);

                        if (oldLeaveFrom == null) continue;

                        Calendar oldLeaveCal =
                                Calendar.getInstance();

                        oldLeaveCal.setTime(oldLeaveFrom);

                        boolean sameLeaveMonth =
                                oldLeaveCal.get(Calendar.MONTH)
                                        == requestMonth
                                        &&
                                        oldLeaveCal.get(Calendar.YEAR)
                                                == requestYear;

                        if (!sameLeaveMonth) {
                            continue;
                        }


                        // New split records
                        if (lm.paidDays > 0) {

                            usedPaidLeaves += lm.paidDays;

                        }
                        // Old records backward compatibility
                        else if (Boolean.TRUE.equals(lm.isPaid)) {

                            usedPaidLeaves +=
                                    calculateLeaveDays(lm);
                        }
                    }

                    android.util.Log.d(
                            "LEAVE_BALANCE_DEBUG",
                            "Employee=" + currentLeave.employeeMobile
                                    + ", RequestMonth="
                                    + (requestMonth + 1)
                                    + "-" + requestYear
                                    + ", Allowed="
                                    + allowedPaidLeaves
                                    + ", Used="
                                    + usedPaidLeaves
                    );

                    showDecisionDialog(
                            currentLeave,
                            allowedPaidLeaves,
                            usedPaidLeaves
                    );

                } catch (Exception e) {

                    android.util.Log.e(
                            "LEAVE_BALANCE_DEBUG",
                            "Failed to calculate paid leave balance",
                            e
                    );

                    showDecisionDialog(
                            currentLeave,
                            allowedPaidLeaves,
                            0
                    );
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError e) {

                Toast.makeText(
                        context,
                        "Error counting paid leaves: "
                                + e.getMessage(),
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }
    // ✅ SHOW DECISION DIALOG
    private void showDecisionDialog(
            LeaveModel m,
            int allowed,
            double used
    ) {

        // First calculate actual chargeable leave days
        // Weekly holidays + company holidays will be skipped
        calculateChargeableLeaveDays(
                m,
                chargeableDays -> {

                    double remaining =
                            Math.max(0, allowed - used);

                    double requestedDays =
                            chargeableDays;

                    double paidDays =
                            Math.min(requestedDays, remaining);

                    double unpaidDays =
                            Math.max(0, requestedDays - paidDays);


                    String msg =
                            "📊 Leave Balance Information\n\n" +

                                    "Paid Leaves Allowed: "
                                    + formatDays(allowed) + "\n" +

                                    "Already Used: "
                                    + formatDays(used) + "\n" +

                                    "Paid Balance: "
                                    + formatDays(remaining) + "\n\n" +

                                    "📅 Leave Period:\n" +
                                    getSafeString(m.fromDate)
                                    + " → "
                                    + getSafeString(m.toDate)
                                    + "\n\n" +

                                    "Current Chargeable Leave: "
                                    + formatDays(requestedDays)
                                    + " day(s)\n" +

                                    "Employee: "
                                    + getSafeString(m.employeeName)
                                    + "\n\n" +

                                    "ℹ️ Weekly holidays and company holidays "
                                    + "are excluded automatically.\n\n" +

                                    "📌 If approved using Paid/Split:\n" +

                                    "Paid Days: "
                                    + formatDays(paidDays)
                                    + "\n" +

                                    "Unpaid Days: "
                                    + formatDays(unpaidDays);


                    AlertDialog dialog =
                            new AlertDialog.Builder(context)
                                    .setTitle("Approve Leave Request")
                                    .setMessage(msg)

                                    .setPositiveButton(
                                            paidDays > 0
                                                    ? (
                                                    unpaidDays > 0
                                                            ? "✅ Approve SPLIT"
                                                            : "✅ Approve as PAID"
                                            )
                                                    : "No Paid Balance",
                                            null
                                    )

                                    .setNegativeButton(
                                            "🔴 Approve as UNPAID",
                                            null
                                    )

                                    .setNeutralButton(
                                            "Cancel",
                                            null
                                    )

                                    .create();


                    dialog.setOnShowListener(unused -> {

                        // =========================
                        // PAID / SPLIT APPROVAL
                        // =========================

                        dialog.getButton(
                                        AlertDialog.BUTTON_POSITIVE
                                )
                                .setOnClickListener(v -> {

                                    if (requestedDays <= 0) {

                                        Toast.makeText(
                                                context,
                                                "No chargeable leave days found. "
                                                        + "Selected dates may contain only holidays.",
                                                Toast.LENGTH_LONG
                                        ).show();

                                        return;
                                    }


                                    if (paidDays <= 0) {

                                        Toast.makeText(
                                                context,
                                                "No paid leave balance available",
                                                Toast.LENGTH_LONG
                                        ).show();

                                        return;
                                    }


                                    approveLeaveWithSplit(
                                            m,
                                            requestedDays,
                                            paidDays,
                                            unpaidDays
                                    );

                                    dialog.dismiss();
                                });


                        // =========================
                        // FULL UNPAID APPROVAL
                        // =========================

                        dialog.getButton(
                                        AlertDialog.BUTTON_NEGATIVE
                                )
                                .setOnClickListener(v -> {

                                    if (requestedDays <= 0) {

                                        Toast.makeText(
                                                context,
                                                "No chargeable leave days found.",
                                                Toast.LENGTH_LONG
                                        ).show();

                                        return;
                                    }

                                    approveLeaveWithSplit(
                                            m,
                                            requestedDays,
                                            0,
                                            requestedDays
                                    );

                                    dialog.dismiss();
                                });

                    });

                    dialog.show();
                }
        );
    }
    private interface LeaveDaysCallback {
        void onResult(double chargeableDays);
    }

    private void calculateChargeableLeaveDays(
            LeaveModel m,
            LeaveDaysCallback callback
    ) {

        String companyKey =
                new PrefManager(context).getCompanyKey();

        DatabaseReference companyRef =
                FirebaseDatabase.getInstance()
                        .getReference("Companies")
                        .child(companyKey);

        // ==========================================
        // STEP 1: EMPLOYEE WEEKLY HOLIDAY LOAD
        // Path:
        // employees/{mobile}/info/weeklyHoliday
        // ==========================================

        companyRef
                .child("employees")
                .child(m.employeeMobile)
                .child("info")
                .child("weeklyHoliday")
                .addListenerForSingleValueEvent(
                        new ValueEventListener() {

                            @Override
                            public void onDataChange(
                                    @NonNull DataSnapshot weeklySnap
                            ) {

                                String weeklyHoliday =
                                        weeklySnap.getValue(String.class);

                                if (weeklyHoliday == null) {
                                    weeklyHoliday = "";
                                }

                                weeklyHoliday =
                                        weeklyHoliday.trim();

                                final String employeeWeeklyHoliday =
                                        weeklyHoliday;

                                android.util.Log.d(
                                        "LEAVE_WEEKLY_DEBUG",
                                        "Employee="
                                                + m.employeeMobile
                                                + ", WeeklyHoliday="
                                                + employeeWeeklyHoliday
                                );


                                // ==========================================
                                // STEP 2: COMPANY HOLIDAYS LOAD
                                // ==========================================

                                companyRef
                                        .child("holidays")
                                        .addListenerForSingleValueEvent(
                                                new ValueEventListener() {

                                                    @Override
                                                    public void onDataChange(
                                                            @NonNull DataSnapshot holidaysSnap
                                                    ) {

                                                        try {

                                                            Set<String> companyHolidayDates =
                                                                    new HashSet<>();


                                                            // ==========================================
                                                            // LOAD COMPANY HOLIDAY DATES
                                                            // ==========================================

                                                            for (DataSnapshot holidaySnap :
                                                                    holidaysSnap.getChildren()) {

                                                                // Date as Firebase key
                                                                String dateKey =
                                                                        holidaySnap.getKey();

                                                                if (dateKey != null
                                                                        && !dateKey.trim().isEmpty()) {

                                                                    companyHolidayDates.add(
                                                                            dateKey.trim()
                                                                    );
                                                                }


                                                                // Fallback holidayDate field
                                                                String holidayDate =
                                                                        holidaySnap
                                                                                .child("holidayDate")
                                                                                .getValue(String.class);

                                                                if (holidayDate != null
                                                                        && !holidayDate.trim().isEmpty()) {

                                                                    companyHolidayDates.add(
                                                                            holidayDate.trim()
                                                                    );
                                                                }
                                                            }


                                                            // ==========================================
                                                            // STEP 3: DATE PARSE
                                                            // ==========================================

                                                            SimpleDateFormat sdf =
                                                                    new SimpleDateFormat(
                                                                            "yyyy-MM-dd",
                                                                            Locale.getDefault()
                                                                    );

                                                            sdf.setLenient(false);


                                                            Date fromDate =
                                                                    sdf.parse(m.fromDate);

                                                            Date toDate =
                                                                    sdf.parse(m.toDate);


                                                            if (fromDate == null
                                                                    || toDate == null) {

                                                                callback.onResult(0);
                                                                return;
                                                            }


                                                            if (fromDate.after(toDate)) {

                                                                callback.onResult(0);
                                                                return;
                                                            }


                                                            // ==========================================
                                                            // STEP 4: LOOP EACH LEAVE DATE
                                                            // ==========================================

                                                            Calendar current =
                                                                    Calendar.getInstance();

                                                            current.setTime(fromDate);


                                                            Calendar end =
                                                                    Calendar.getInstance();

                                                            end.setTime(toDate);


                                                            double chargeableDays = 0.0;

                                                            int totalCalendarDays = 0;
                                                            int weeklyHolidayCount = 0;
                                                            int companyHolidayCount = 0;


                                                            while (!current.after(end)) {

                                                                totalCalendarDays++;


                                                                String dateKey =
                                                                        sdf.format(
                                                                                current.getTime()
                                                                        );


                                                                // ==========================================
                                                                // CURRENT DATE DAY NAME
                                                                // Example:
                                                                // Sunday / Monday / Saturday
                                                                // ==========================================

                                                                String currentDayName =
                                                                        new SimpleDateFormat(
                                                                                "EEEE",
                                                                                Locale.ENGLISH
                                                                        ).format(
                                                                                current.getTime()
                                                                        );


                                                                // ==========================================
                                                                // DYNAMIC EMPLOYEE WEEKLY HOLIDAY
                                                                // ==========================================

                                                                boolean isWeeklyHoliday =
                                                                        !employeeWeeklyHoliday.isEmpty()
                                                                                &&
                                                                                currentDayName.equalsIgnoreCase(
                                                                                        employeeWeeklyHoliday
                                                                                );


                                                                // ==========================================
                                                                // COMPANY HOLIDAY
                                                                // ==========================================

                                                                boolean isCompanyHoliday =
                                                                        companyHolidayDates.contains(
                                                                                dateKey
                                                                        );


                                                                // ==========================================
                                                                // COUNT / SKIP
                                                                // ==========================================

                                                                if (isWeeklyHoliday) {

                                                                    weeklyHolidayCount++;

                                                                    android.util.Log.d(
                                                                            "LEAVE_DAY_DEBUG",
                                                                            "SKIP WEEKLY HOLIDAY"
                                                                                    + " | Date="
                                                                                    + dateKey
                                                                                    + " | Day="
                                                                                    + currentDayName
                                                                                    + " | Configured="
                                                                                    + employeeWeeklyHoliday
                                                                    );

                                                                } else if (isCompanyHoliday) {

                                                                    companyHolidayCount++;

                                                                    android.util.Log.d(
                                                                            "LEAVE_DAY_DEBUG",
                                                                            "SKIP COMPANY HOLIDAY"
                                                                                    + " | Date="
                                                                                    + dateKey
                                                                    );

                                                                } else {

                                                                    // ==========================================
                                                                    // NORMAL WORKING DAY
                                                                    // HALF DAY = 0.5
                                                                    // FULL DAY = 1.0
                                                                    // ==========================================

                                                                    boolean isHalfDay =
                                                                            "HALF_DAY".equalsIgnoreCase(
                                                                                    getSafeString(m.leaveType).trim()
                                                                            );

                                                                    double dayValue =
                                                                            isHalfDay ? 0.5 : 1.0;

                                                                    chargeableDays += dayValue;

                                                                    android.util.Log.d(
                                                                            "LEAVE_DAY_DEBUG",
                                                                            "COUNT LEAVE DAY"
                                                                                    + " | Date="
                                                                                    + dateKey
                                                                                    + " | Day="
                                                                                    + currentDayName
                                                                                    + " | LeaveType="
                                                                                    + getSafeString(m.leaveType)
                                                                                    + " | Added="
                                                                                    + dayValue
                                                                    );
                                                                }   


                                                                current.add(
                                                                        Calendar.DAY_OF_MONTH,
                                                                        1
                                                                );
                                                            }


                                                            // ==========================================
                                                            // FINAL DEBUG
                                                            // ==========================================

                                                            android.util.Log.d(
                                                                    "LEAVE_CHARGE_DEBUG",

                                                                    "Employee="
                                                                            + m.employeeMobile +

                                                                            ", WeeklyHoliday="
                                                                            + employeeWeeklyHoliday +

                                                                            ", From="
                                                                            + m.fromDate +

                                                                            ", To="
                                                                            + m.toDate +

                                                                            ", CalendarDays="
                                                                            + totalCalendarDays +

                                                                            ", WeeklySkipped="
                                                                            + weeklyHolidayCount +

                                                                            ", CompanySkipped="
                                                                            + companyHolidayCount +

                                                                            ", ChargeableDays="
                                                                            + chargeableDays
                                                            );


                                                            callback.onResult(
                                                                    chargeableDays
                                                            );


                                                        } catch (Exception e) {

                                                            android.util.Log.e(
                                                                    "LEAVE_CHARGE_DEBUG",
                                                                    "Failed calculating chargeable leave",
                                                                    e
                                                            );

                                                            callback.onResult(0);
                                                        }
                                                    }


                                                    @Override
                                                    public void onCancelled(
                                                            @NonNull DatabaseError error
                                                    ) {

                                                        android.util.Log.e(
                                                                "LEAVE_CHARGE_DEBUG",
                                                                "Holiday load error: "
                                                                        + error.getMessage()
                                                        );

                                                        callback.onResult(0);
                                                    }
                                                }
                                        );
                            }


                            @Override
                            public void onCancelled(
                                    @NonNull DatabaseError error
                            ) {

                                android.util.Log.e(
                                        "LEAVE_WEEKLY_DEBUG",
                                        "Weekly holiday load error: "
                                                + error.getMessage()
                                );

                                callback.onResult(0);
                            }
                        }
                );
    }

    private void approveLeaveWithSplit(
            LeaveModel m,
            double requestedDays,
            double paidDays,
            double unpaidDays
    ) {
        Map<String, Object> updates = new HashMap<>();

        updates.put("status", "APPROVED");

        // Total approved days
        updates.put("approvedDays", requestedDays);

        // Exact payroll split
        updates.put("paidDays", paidDays);
        updates.put("unpaidDays", unpaidDays);

        // Backward compatibility
        updates.put("isPaid", paidDays > 0);

        updates.put(
                "approvalType",
                unpaidDays > 0 && paidDays > 0
                        ? "PARTIALLY_PAID"
                        : paidDays > 0
                        ? "PAID"
                        : "UNPAID"
        );

        updates.put(
                "approvedAt",
                System.currentTimeMillis()
        );

        String adminName =
                new PrefManager(context).getUserName();

        updates.put(
                "approvedBy",
                adminName
        );

        leavesRef
                .child(m.leaveId)
                .updateChildren(updates)
                .addOnSuccessListener(unused -> {

                    String message;

                    if (paidDays > 0 && unpaidDays > 0) {

                        message =
                                "Leave Approved\n" +
                                        formatDays(paidDays) + " Paid + " +
                                        formatDays(unpaidDays) + " Unpaid";

                    } else if (paidDays > 0) {

                        message =
                                "Paid Leave Approved: " +
                                        formatDays(paidDays) + " day(s)";

                    } else {

                        message =
                                "Unpaid Leave Approved: " +
                                        formatDays(unpaidDays) + " day(s)";
                    }

                    Toast.makeText(
                            context,
                            message,
                            Toast.LENGTH_LONG
                    ).show();

                })
                .addOnFailureListener(e ->

                        Toast.makeText(
                                context,
                                "Failed to approve leave: "
                                        + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    private String formatDays(double value) {

        if (value == Math.floor(value)) {
            return String.valueOf((int) value);
        }

        return String.format(
                Locale.getDefault(),
                "%.1f",
                value
        );
    }

    private double calculateLeaveDays(LeaveModel m) {

        if ("HALF_DAY".equals(m.leaveType)) {
            return 0.5;
        }

        try {

            SimpleDateFormat sdf =
                    new SimpleDateFormat(
                            "yyyy-MM-dd",
                            Locale.getDefault()
                    );

            Date from = sdf.parse(m.fromDate);
            Date to = sdf.parse(m.toDate);

            long diff =
                    to.getTime() - from.getTime();

            return (diff / (1000 * 60 * 60 * 24)) + 1;

        } catch (Exception e) {
            e.printStackTrace();
            return 1;
        }
    }

    // ✅ APPROVE LEAVE
    private void approveLeave(LeaveModel m, boolean isPaid) {
        Map<String, Object> map = new HashMap<>();
        map.put("status", "APPROVED");
        map.put("isPaid", isPaid);
        map.put("approvedAt", System.currentTimeMillis());

        String adminName = new PrefManager(context).getUserName();
        map.put("approvedBy", adminName);

        leavesRef.child(m.leaveId).updateChildren(map)
                .addOnSuccessListener(a -> {
                    String msg = isPaid ? "✅ Paid Leave Approved" : "⚠️ Unpaid Leave Approved";
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();

                    // ✅ AUTO-HIDE: Firebase listener in Activity will refresh list → buttons auto-hide
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "❌ Failed to approve leave", Toast.LENGTH_SHORT).show();
                });
    }


    // ✅ REJECT LEAVE
    private void showRejectDialog(String leaveId) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.dialog_reject_leave, null);

        EditText et = view.findViewById(R.id.etReason);

        new AlertDialog.Builder(context)
                .setTitle("Reject Leave Request")
                .setView(view)
                .setPositiveButton("Reject", (d, w) -> {
                    String r = et.getText().toString().trim();
                    if (r.isEmpty()) {
                        Toast.makeText(context, "⚠️ Reason required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("status", "REJECTED");
                    updates.put("adminReason", r);
                    updates.put("actionAt", System.currentTimeMillis());

                    String adminName = new PrefManager(context).getUserName();
                    updates.put("rejectedBy", adminName);

                    leavesRef.child(leaveId).updateChildren(updates)
                            .addOnSuccessListener(a -> {
                                Toast.makeText(context, "❌ Leave Rejected", Toast.LENGTH_SHORT).show();
                                // ✅ AUTO-HIDE: Firebase listener will refresh → buttons auto-hide
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(context, "❌ Failed to reject leave", Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }


    @Override
    public int getItemCount() {
        return list.size();
    }


    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvDates, tvReason, tvStatus;
        MaterialButton btnApprove, btnReject;

        VH(View v) {
            super(v);

            tvName = v.findViewById(R.id.tvName);
            tvDates = v.findViewById(R.id.tvDates);
            tvReason = v.findViewById(R.id.tvReason);
            tvStatus = v.findViewById(R.id.tvStatus);
            btnApprove = v.findViewById(R.id.btnApprove);
            btnReject = v.findViewById(R.id.btnReject);

            // ✅ DEBUG: Log missing views
            if (tvName == null) android.util.Log.e("AdminLeaveAdapter", "❌ tvName is NULL!");
            if (tvDates == null) android.util.Log.e("AdminLeaveAdapter", "❌ tvDates is NULL!");
            if (tvReason == null) android.util.Log.e("AdminLeaveAdapter", "❌ tvReason is NULL!");
            if (tvStatus == null) android.util.Log.e("AdminLeaveAdapter", "❌ tvStatus is NULL!");
            if (btnApprove == null) android.util.Log.e("AdminLeaveAdapter", "❌ btnApprove is NULL!");
            if (btnReject == null) android.util.Log.e("AdminLeaveAdapter", "❌ btnReject is NULL!");
        }
    }

}
