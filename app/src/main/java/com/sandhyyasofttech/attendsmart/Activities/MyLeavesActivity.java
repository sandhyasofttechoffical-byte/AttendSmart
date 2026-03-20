package com.sandhyyasofttech.attendsmart.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.sandhyyasofttech.attendsmart.Adapters.MyLeavesAdapter;
import com.sandhyyasofttech.attendsmart.Models.LeaveModel;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

public class MyLeavesActivity extends AppCompatActivity {

    private static final int RC_DETAIL = 2001;

    private RecyclerView rv;
    private MaterialCardView cardEmpty, layoutFilters;
    private ChipGroup chipGroupFilter, chipGroupSort;
    private LinearLayout layoutStats;
    private TextView tvPendingCount, tvApprovedCount, tvRejectedCount, tvResetFilters;

    private ArrayList<LeaveModel> fullList     = new ArrayList<>();
    private ArrayList<LeaveModel> filteredList = new ArrayList<>();
    private MyLeavesAdapter adapter;

    private String currentStatusFilter = "ALL";
    private String currentSortOrder    = "NEWEST";

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_my_leaves);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.blue_800));
        }
        initViews();
        setupToolbar();
        setupFilters();
        setupRecyclerView();
        loadLeaves();
    }

    private void initViews() {
        rv              = findViewById(R.id.rvLeaves);
        cardEmpty       = findViewById(R.id.cardEmpty);
        chipGroupFilter = findViewById(R.id.chipGroupFilter);
        chipGroupSort   = findViewById(R.id.chipGroupSort);
        layoutFilters   = findViewById(R.id.layoutFilters);
        layoutStats     = findViewById(R.id.layoutStats);
        tvPendingCount  = findViewById(R.id.tvPendingCount);
        tvApprovedCount = findViewById(R.id.tvApprovedCount);
        tvRejectedCount = findViewById(R.id.tvRejectedCount);
        tvResetFilters  = findViewById(R.id.tvResetFilters);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_my_leaves, menu);
        return true;
    }

    @Override public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_apply_leave) {
            startActivity(new Intent(this, ApplyLeaveActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupFilters() {
        chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                Chip chip = findViewById(checkedIds.get(0));
                if (chip != null) {
                    currentStatusFilter = chip.getText().toString().toUpperCase();
                    applyFilters();
                    tvResetFilters.setVisibility(
                            currentStatusFilter.equals("ALL") ? View.GONE : View.VISIBLE);
                }
            }
        });

        chipGroupSort.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                Chip chip = findViewById(checkedIds.get(0));
                if (chip != null) {
                    currentSortOrder = chip.getTag().toString();
                    applyFilters();
                }
            }
        });

        tvResetFilters.setOnClickListener(v -> {
            ((Chip) findViewById(R.id.chipAll)).setChecked(true);
            ((Chip) findViewById(R.id.chipNewest)).setChecked(true);
        });
    }

    private void setupRecyclerView() {
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MyLeavesAdapter(filteredList);
        rv.setAdapter(adapter);

        // Item click → open detail screen
        adapter.setOnItemClickListener(leave -> openLeaveDetail(leave));

        rv.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull android.graphics.Rect outRect, @NonNull View view,
                                       @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                outRect.bottom = 8;
            }
        });
    }

    private void openLeaveDetail(LeaveModel leave) {
        Intent i = new Intent(this, LeaveDetailActivity.class);
        i.putExtra(LeaveDetailActivity.EXTRA_LEAVE_ID,    leave.leaveId);
        i.putExtra(LeaveDetailActivity.EXTRA_FROM_DATE,   leave.fromDate);
        i.putExtra(LeaveDetailActivity.EXTRA_TO_DATE,     leave.toDate);
        i.putExtra(LeaveDetailActivity.EXTRA_LEAVE_TYPE,  leave.leaveType);
        i.putExtra(LeaveDetailActivity.EXTRA_HALF_DAY,    leave.halfDayType);
        i.putExtra(LeaveDetailActivity.EXTRA_REASON,      leave.reason);
        i.putExtra(LeaveDetailActivity.EXTRA_STATUS,      leave.status);
        i.putExtra(LeaveDetailActivity.EXTRA_APPLIED_AT,  leave.appliedAt);
        i.putExtra(LeaveDetailActivity.EXTRA_APPROVED_BY, leave.approvedBy);
        i.putExtra(LeaveDetailActivity.EXTRA_ADMIN_REASON,leave.adminReason);
        i.putExtra(LeaveDetailActivity.EXTRA_EMP_NAME,    leave.employeeName);
        i.putExtra(LeaveDetailActivity.EXTRA_EMP_MOBILE,  leave.employeeMobile);

        if (leave.approvedAt != null)
            i.putExtra(LeaveDetailActivity.EXTRA_APPROVED_AT, (long) leave.approvedAt);
        if (leave.isPaid != null)
            i.putExtra(LeaveDetailActivity.EXTRA_IS_PAID, leave.isPaid);

        startActivityForResult(i, RC_DETAIL);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Firebase listener will auto-refresh list; no extra action needed
    }

    private void loadLeaves() {
        PrefManager pref   = new PrefManager(this);
        String companyKey  = pref.getCompanyKey();
        String mobile      = pref.getEmployeeMobile();

        if (companyKey == null || companyKey.isEmpty() || mobile == null || mobile.isEmpty()) {
            showEmptyState();
            Toast.makeText(this, "Invalid credentials", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("Companies").child(companyKey).child("leaves");

        ref.orderByChild("employeeMobile").equalTo(mobile)
                .addValueEventListener(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snap) {
                        fullList.clear();
                        for (DataSnapshot s : snap.getChildren()) {
                            LeaveModel m = s.getValue(LeaveModel.class);
                            if (m != null) { m.leaveId = s.getKey(); fullList.add(m); }
                        }
                        updateStatistics();
                        applyFilters();
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {
                        showEmptyState();
                        Toast.makeText(MyLeavesActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateStatistics() {
        int pending = 0, approved = 0, rejected = 0;
        for (LeaveModel l : fullList) {
            if (l.status != null) switch (l.status.toUpperCase()) {
                case "PENDING":  pending++;  break;
                case "APPROVED": approved++; break;
                case "REJECTED": rejected++; break;
            }
        }
        tvPendingCount.setText(String.valueOf(pending));
        tvApprovedCount.setText(String.valueOf(approved));
        tvRejectedCount.setText(String.valueOf(rejected));
        layoutStats.setVisibility(fullList.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void applyFilters() {
        filteredList.clear();
        for (LeaveModel l : fullList) {
            if (currentStatusFilter.equals("ALL") ||
                    (l.status != null && l.status.equalsIgnoreCase(currentStatusFilter)))
                filteredList.add(l);
        }
        Collections.sort(filteredList, getComparator());
        if (filteredList.isEmpty()) showEmptyState(); else hideEmptyState();
        adapter.notifyDataSetChanged();
        layoutFilters.setVisibility(fullList.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private Comparator<LeaveModel> getComparator() {
        switch (currentSortOrder) {
            case "OLDEST":     return (l1, l2) -> Long.compare(l1.appliedAt, l2.appliedAt);
            case "START_DATE": return (l1, l2) -> compareDates(l1.fromDate, l2.fromDate);
            default:           return (l1, l2) -> Long.compare(l2.appliedAt, l1.appliedAt);
        }
    }

    private int compareDates(String d1, String d2) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date a = d1 != null ? sdf.parse(d1) : new Date(0);
            Date b = d2 != null ? sdf.parse(d2) : new Date(0);
            if (a != null && b != null) return a.compareTo(b);
        } catch (Exception e) { e.printStackTrace(); }
        return 0;
    }

    private void showEmptyState() {
        cardEmpty.setVisibility(View.VISIBLE);
        rv.setVisibility(View.GONE);
        layoutFilters.setVisibility(View.GONE);
        layoutStats.setVisibility(View.GONE);
    }

    private void hideEmptyState() {
        cardEmpty.setVisibility(View.GONE);
        rv.setVisibility(View.VISIBLE);
        layoutFilters.setVisibility(View.VISIBLE);
        layoutStats.setVisibility(View.VISIBLE);
    }
}