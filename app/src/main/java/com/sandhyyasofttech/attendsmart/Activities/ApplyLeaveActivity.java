package com.sandhyyasofttech.attendsmart.Activities;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ApplyLeaveActivity extends AppCompatActivity {

    // ── Extras used by LeaveDetailActivity to pre-fill edit mode ──────────────
    public static final String EXTRA_EDIT_MODE  = "editMode";
    public static final String EXTRA_LEAVE_ID   = "leaveId";
    public static final String EXTRA_FROM_DATE  = "fromDate";
    public static final String EXTRA_TO_DATE    = "toDate";
    public static final String EXTRA_LEAVE_TYPE = "leaveType";
    public static final String EXTRA_HALF_DAY   = "halfDayType";
    public static final String EXTRA_REASON     = "reason";

    private TextView tvFromDate, tvToDate;
    private EditText etReason;
    private RadioGroup rgLeaveType, rgHalfDay;
    private MaterialButton btnSubmit;

    private String fromDate, toDate, leaveType, halfDayType = null;
    private boolean isEditMode = false;
    private String editLeaveId = null;

    private DatabaseReference leavesRef;
    private String companyKey, employeeMobile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_apply_leave);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.blue_800));
        }

        initViews();
        setupToolbar();
        setupFirebase();
        setupListeners();
        checkEditMode();
    }

    private void initViews() {
        tvFromDate = findViewById(R.id.tvFromDate);
        tvToDate   = findViewById(R.id.tvToDate);
        etReason   = findViewById(R.id.etReason);
        rgLeaveType= findViewById(R.id.rgLeaveType);
        rgHalfDay  = findViewById(R.id.rgHalfDay);
        btnSubmit  = findViewById(R.id.btnSubmit);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_apply_leave, menu);
        return true;
    }

    @Override public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_view_leaves) {
            startActivity(new Intent(this, MyLeavesActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupFirebase() {
        PrefManager pref = new PrefManager(this);
        companyKey      = pref.getCompanyKey();
        employeeMobile  = pref.getEmployeeMobile();
        leavesRef = FirebaseDatabase.getInstance()
                .getReference("Companies").child(companyKey).child("leaves");
    }

    /** Pre-fill all fields when launched from LeaveDetailActivity in edit mode */
    private void checkEditMode() {
        Intent i = getIntent();
        if (i == null) return;

        isEditMode  = i.getBooleanExtra(EXTRA_EDIT_MODE, false);
        editLeaveId = i.getStringExtra(EXTRA_LEAVE_ID);

        if (!isEditMode || editLeaveId == null) return;

        // Update toolbar title & button text
        if (getSupportActionBar() != null) getSupportActionBar().setTitle("Edit Leave Request");
        btnSubmit.setText("Update Request");

        // Pre-fill dates
        fromDate = i.getStringExtra(EXTRA_FROM_DATE);
        toDate   = i.getStringExtra(EXTRA_TO_DATE);
        if (fromDate != null) tvFromDate.setText(fromDate);
        if (toDate   != null) tvToDate.setText(toDate);

        // Pre-fill reason
        String reason = i.getStringExtra(EXTRA_REASON);
        if (reason != null) etReason.setText(reason);

        // Pre-fill leave type radio
        String lt = i.getStringExtra(EXTRA_LEAVE_TYPE);
        if (lt != null) {
            if (lt.equalsIgnoreCase("HALF_DAY")) {
                ((RadioButton) findViewById(R.id.rbHalfDay)).setChecked(true);
                rgHalfDay.setVisibility(View.VISIBLE);
                String hdt = i.getStringExtra(EXTRA_HALF_DAY);
                if (hdt != null) {
                    if (hdt.equalsIgnoreCase("MORNING")) {
                        ((RadioButton) findViewById(R.id.rbMorning)).setChecked(true);
                    } else {
                        ((RadioButton) findViewById(R.id.rbAfternoon)).setChecked(true);
                    }
                }
            } else {
                ((RadioButton) findViewById(R.id.rbFullDay)).setChecked(true);
                rgHalfDay.setVisibility(View.GONE);
            }
        }
    }

    private void setupListeners() {
        rgLeaveType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbHalfDay) {
                rgHalfDay.setVisibility(View.VISIBLE);
            } else {
                rgHalfDay.setVisibility(View.GONE);
                halfDayType = null;
            }
        });
        tvFromDate.setOnClickListener(v -> pickDate(true));
        tvToDate.setOnClickListener(v   -> pickDate(false));
        btnSubmit.setOnClickListener(v  -> submitLeave());
    }

    private void pickDate(boolean isFrom) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, 1);
        DatePickerDialog dp = new DatePickerDialog(this, (view, y, m, d) -> {
            String date = String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d);
            if (isFrom) { fromDate = date; tvFromDate.setText(date); }
            else        { toDate   = date; tvToDate.setText(date);   }
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        dp.getDatePicker().setMinDate(cal.getTimeInMillis());
        dp.show();
    }

    private void submitLeave() {
        leaveType = rgLeaveType.getCheckedRadioButtonId() == R.id.rbHalfDay ? "HALF_DAY" : "FULL_DAY";

        if (leaveType.equals("HALF_DAY")) {
            int id = rgHalfDay.getCheckedRadioButtonId();
            if (id == -1) { toast("Select half day type"); return; }
            halfDayType = id == R.id.rbMorning ? "MORNING" : "AFTERNOON";
        }

        if (fromDate == null || toDate == null) { toast("Select dates"); return; }
        if (leaveType.equals("HALF_DAY") && !fromDate.equals(toDate)) {
            toast("Half day must be same date"); return;
        }

        String reason = etReason.getText().toString().trim();
        if (reason.isEmpty()) { toast("Enter reason"); return; }

        if (isEditMode && editLeaveId != null) {
            updateLeave(reason);
        } else {
            checkDuplicateAndSave(reason);
        }
    }

    private void updateLeave(String reason) {
        Map<String, Object> map = new HashMap<>();
        map.put("leaveType",   leaveType);
        map.put("halfDayType", halfDayType);
        map.put("fromDate",    fromDate);
        map.put("toDate",      toDate);
        map.put("reason",      reason);
        // Keep status as PENDING after edit
        map.put("status",      "PENDING");

        leavesRef.child(editLeaveId).updateChildren(map)
                .addOnSuccessListener(a -> {
                    toast("✅ Leave request updated");
                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e -> toast("❌ Failed to update leave"));
    }

    private void checkDuplicateAndSave(String reason) {
        leavesRef.orderByChild("employeeMobile").equalTo(employeeMobile)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot s : snapshot.getChildren()) {
                            String f = s.child("fromDate").getValue(String.class);
                            String t = s.child("toDate").getValue(String.class);
                            if (f != null && t != null &&
                                    !(toDate.compareTo(f) < 0 || fromDate.compareTo(t) > 0)) {
                                toast("Leave already applied for selected dates");
                                return;
                            }
                        }
                        saveLeave(reason);
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void saveLeave(String reason) {
        DatabaseReference empNameRef = FirebaseDatabase.getInstance()
                .getReference("Companies").child(companyKey)
                .child("employees").child(employeeMobile).child("info").child("employeeName");

        empNameRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                String empName = snapshot.getValue(String.class);
                if (empName == null || empName.trim().isEmpty()) empName = "Employee";

                String leaveId = leavesRef.push().getKey();
                if (leaveId == null) { toast("Failed to generate leave id"); return; }

                Map<String, Object> map = new HashMap<>();
                map.put("employeeMobile", employeeMobile);
                map.put("employeeName",   empName);
                map.put("leaveType",      leaveType);
                map.put("halfDayType",    halfDayType);
                map.put("fromDate",       fromDate);
                map.put("toDate",         toDate);
                map.put("reason",         reason);
                map.put("status",         "PENDING");
                map.put("isPaid",         null);
                map.put("approvedBy",     null);
                map.put("approvedAt",     null);
                map.put("appliedAt",      System.currentTimeMillis());

                leavesRef.child(leaveId).setValue(map)
                        .addOnSuccessListener(a -> { toast("✅ Leave submitted"); finish(); })
                        .addOnFailureListener(e -> toast("❌ Failed to submit leave"));
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                toast("Failed to fetch employee name");
            }
        });
    }

    private void toast(String m) { Toast.makeText(this, m, Toast.LENGTH_SHORT).show(); }
}