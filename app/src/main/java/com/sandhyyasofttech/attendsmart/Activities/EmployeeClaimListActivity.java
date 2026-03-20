package com.sandhyyasofttech.attendsmart.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.*;
import com.sandhyyasofttech.attendsmart.Adapters.ExpenseClaimAdapter;
import com.sandhyyasofttech.attendsmart.Models.ExpenseClaim;
import com.sandhyyasofttech.attendsmart.Models.ExpenseItem;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EmployeeClaimListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ExpenseClaimAdapter adapter;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private Spinner spinnerFilter;
    private EditText etSearch;
    private ImageView btnSearch;
    private FloatingActionButton fabAdd;

    private DatabaseReference claimsRef;
    private ValueEventListener claimsListener;
    private PrefManager prefManager;
    private List<ExpenseClaim> allClaims = new ArrayList<>();
    private String currentFilter = "all";
    private DecimalFormat df = new DecimalFormat("#,##0.00");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employee_claim_list);

        initViews();
        setupRecyclerView();
        setupFilterAndSearch();

        prefManager = new PrefManager(this);

        if (!validateUserSession()) {
            return;
        }

        setupFirebase();
        loadClaims();

        // Set action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("My Expense Claims");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);
        spinnerFilter = findViewById(R.id.spinnerFilter);
        etSearch = findViewById(R.id.etSearch);
        btnSearch = findViewById(R.id.btnSearch);
        fabAdd = findViewById(R.id.fabAdd);
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

        // Path: Companies > companyKey > expenseClaims
        claimsRef = FirebaseDatabase.getInstance()
                .getReference("Companies")
                .child(companyKey)
                .child("expenseClaims");
    }

    private void setupRecyclerView() {
        adapter = new ExpenseClaimAdapter(claim -> {
            // Open claim detail activity
            Intent intent = new Intent(EmployeeClaimListActivity.this, EmployeeClaimListActivity.class);
            intent.putExtra("claimId", claim.getClaimId());
            startActivity(intent);
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // FAB Click Listener
        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(EmployeeClaimListActivity.this, EmployeeClaimActivity.class);
            startActivity(intent);
        });
    }

    private void setupFilterAndSearch() {
        // Setup filter spinner
        ArrayAdapter<CharSequence> filterAdapter = ArrayAdapter.createFromResource(this,
                R.array.claim_status_filters, android.R.layout.simple_spinner_item);
        filterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilter.setAdapter(filterAdapter);

        spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String[] filters = getResources().getStringArray(R.array.claim_status_values);
                currentFilter = filters[position];
                filterClaims();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                currentFilter = "all";
                filterClaims();
            }
        });

        // Setup search
        btnSearch.setOnClickListener(v -> filterClaims());

        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            filterClaims();
            return false;
        });
    }

    private void loadClaims() {
        showProgress(true);

        final String userId = prefManager.getUserId();
        if (userId == null || userId.isEmpty()) {
            showProgress(false);
            tvEmpty.setVisibility(View.VISIBLE);
            tvEmpty.setText("User information missing");
            return;
        }

        claimsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allClaims.clear();

                for (DataSnapshot claimSnapshot : snapshot.getChildren()) {
                    ExpenseClaim claim = claimSnapshot.getValue(ExpenseClaim.class);
                    if (claim != null && claim.getUserId() != null &&
                            claim.getUserId().equals(userId)) {
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

                filterClaims();
                showProgress(false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showProgress(false);
                Toast.makeText(EmployeeClaimListActivity.this,
                        "Failed to load claims: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                tvEmpty.setVisibility(View.VISIBLE);
                tvEmpty.setText("Error loading claims");
            }
        };

        claimsRef.addValueEventListener(claimsListener);
    }

    private void filterClaims() {
        List<ExpenseClaim> filteredClaims = new ArrayList<>(allClaims);

        // Apply status filter
        if (!currentFilter.equals("all")) {
            filteredClaims.removeIf(claim -> !claim.getStatus().equals(currentFilter));
        }

        // Apply search filter
        String searchQuery = etSearch.getText().toString().trim().toLowerCase();
        if (!TextUtils.isEmpty(searchQuery)) {
            filteredClaims.removeIf(claim -> {
                // Search in total amount
                boolean matchesAmount = String.valueOf(claim.getTotalAmount()).contains(searchQuery);

                // Search in items descriptions
                boolean matchesDescription = false;
                if (claim.getItems() != null) {
                    for (ExpenseItem item : claim.getItems()) {
                        if (item.getDescription().toLowerCase().contains(searchQuery) ||
                                item.getCategory().toLowerCase().contains(searchQuery)) {
                            matchesDescription = true;
                            break;
                        }
                    }
                }

                return !matchesAmount && !matchesDescription;
            });
        }

        adapter.setClaimList(filteredClaims);

        if (filteredClaims.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            if (allClaims.isEmpty()) {
                tvEmpty.setText("No expense claims submitted yet.\nTap + to add your first claim");
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