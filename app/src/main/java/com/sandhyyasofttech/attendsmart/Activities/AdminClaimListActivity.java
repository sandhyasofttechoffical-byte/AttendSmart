package com.sandhyyasofttech.attendsmart.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.database.*;
import com.sandhyyasofttech.attendsmart.Adapters.AdminExpenseClaimAdapter;
import com.sandhyyasofttech.attendsmart.Models.ExpenseClaim;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AdminClaimListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private AdminExpenseClaimAdapter adapter;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private Spinner spinnerStatusFilter;
    private EditText etSearch;
    private ImageView btnSearch;
    private TextView tvTotalPending, tvTotalApproved, tvTotalRejected, tvTotalAmount;

    private DatabaseReference claimsRef;
    private ValueEventListener claimsListener;
    private PrefManager prefManager;
    private List<ExpenseClaim> allClaims = new ArrayList<>();
    private String currentStatusFilter = "all";
    private String currentSearchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_claim_list);

        initViews();
        setupToolbar();
        setupRecyclerView();
        setupFilterAndSearch();

        prefManager = new PrefManager(this);

        if (!validateAdminSession()) {
            return;
        }

        setupFirebase();
        loadClaims();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);
        spinnerStatusFilter = findViewById(R.id.spinnerStatusFilter);
        etSearch = findViewById(R.id.etSearch);
        btnSearch = findViewById(R.id.btnSearch);
        tvTotalPending = findViewById(R.id.tvTotalPending);
        tvTotalApproved = findViewById(R.id.tvTotalApproved);
        tvTotalRejected = findViewById(R.id.tvTotalRejected);
        tvTotalAmount = findViewById(R.id.tvTotalAmount);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Expense Claims");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private boolean validateAdminSession() {
        String userType = prefManager.getUserType();

        if (userType == null || !userType.equalsIgnoreCase("admin")) {
            Toast.makeText(this, "Admin access required", Toast.LENGTH_LONG).show();
            finish();
            return false;
        }

        return true;
    }
    private void setupFirebase() {
        String companyKey = prefManager.getCompanyKey();

        if (companyKey == null || companyKey.isEmpty()) {
            Toast.makeText(this, "Company not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Path: Companies > companyKey > expenseClaims
        claimsRef = FirebaseDatabase.getInstance()
                .getReference("Companies")
                .child(companyKey)
                .child("expenseClaims");
    }

    private void setupRecyclerView() {
        adapter = new AdminExpenseClaimAdapter(claim -> {
            // Open claim detail activity
            Intent intent = new Intent(AdminClaimListActivity.this, AdminClaimDetailActivity.class);
            intent.putExtra("claimId", claim.getClaimId());
            startActivity(intent);
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupFilterAndSearch() {
        // Status filter spinner
        ArrayAdapter<CharSequence> filterAdapter = ArrayAdapter.createFromResource(this,
                R.array.admin_claim_status_filters, android.R.layout.simple_spinner_item);
        filterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStatusFilter.setAdapter(filterAdapter);

        spinnerStatusFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String[] filters = getResources().getStringArray(R.array.admin_claim_status_values);
                currentStatusFilter = filters[position];
                applyFilters();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                currentStatusFilter = "all";
                applyFilters();
            }
        });

        // Search
        btnSearch.setOnClickListener(v -> {
            currentSearchQuery = etSearch.getText().toString().trim().toLowerCase();
            applyFilters();
        });

        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            currentSearchQuery = etSearch.getText().toString().trim().toLowerCase();
            applyFilters();
            return false;
        });
    }

    private void loadClaims() {
        showProgress(true);

        claimsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allClaims.clear();

                for (DataSnapshot claimSnapshot : snapshot.getChildren()) {
                    ExpenseClaim claim = claimSnapshot.getValue(ExpenseClaim.class);
                    if (claim != null) {
                        allClaims.add(claim);
                    }
                }

                // Sort by timestamp descending (newest first)
                Collections.sort(allClaims, (c1, c2) -> {
                    try {
                        long t1 = Long.parseLong(c1.getTimestamp());
                        long t2 = Long.parseLong(c2.getTimestamp());
                        return Long.compare(t2, t1);
                    } catch (NumberFormatException e) {
                        return 0;
                    }
                });

                updateStatistics();
                applyFilters();
                showProgress(false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showProgress(false);
                Toast.makeText(AdminClaimListActivity.this,
                        "Failed to load claims: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                tvEmpty.setVisibility(View.VISIBLE);
                tvEmpty.setText("Error loading claims");
            }
        };

        claimsRef.addValueEventListener(claimsListener);
    }

    private void updateStatistics() {
        int pending = 0, approved = 0, rejected = 0;
        double totalAmount = 0;

        for (ExpenseClaim claim : allClaims) {
            switch (claim.getStatus()) {
                case "pending":
                    pending++;
                    break;
                case "approved":
                    approved++;
                    break;
                case "rejected":
                    rejected++;
                    break;
            }
            totalAmount += claim.getTotalAmount();
        }

        tvTotalPending.setText(String.valueOf(pending));
        tvTotalApproved.setText(String.valueOf(approved));
        tvTotalRejected.setText(String.valueOf(rejected));
        tvTotalAmount.setText(String.format("₹%,.2f", totalAmount));
    }

    private void applyFilters() {
        List<ExpenseClaim> filteredClaims = new ArrayList<>(allClaims);

        // Apply status filter
        if (!currentStatusFilter.equals("all")) {
            filteredClaims.removeIf(claim -> !claim.getStatus().equals(currentStatusFilter));
        }

        // Apply search filter (employee name or amount)
        if (!TextUtils.isEmpty(currentSearchQuery)) {
            filteredClaims.removeIf(claim -> {
                boolean matchesName = claim.getUserName() != null &&
                        claim.getUserName().toLowerCase().contains(currentSearchQuery);
                boolean matchesAmount = String.valueOf(claim.getTotalAmount()).contains(currentSearchQuery);
                return !matchesName && !matchesAmount;
            });
        }

        adapter.setClaimList(filteredClaims);

        if (filteredClaims.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            if (allClaims.isEmpty()) {
                tvEmpty.setText("No expense claims submitted yet");
            } else {
                tvEmpty.setText("No claims match your filters");
            }
        } else {
            tvEmpty.setVisibility(View.GONE);
        }
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        if (!show) {
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (claimsRef != null && claimsListener != null) {
            claimsRef.removeEventListener(claimsListener);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}