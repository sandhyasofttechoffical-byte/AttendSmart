package com.sandhyyasofttech.attendsmart.Activities;

import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sandhyyasofttech.attendsmart.Adapters.EmployeeModelListAdapter;
import com.sandhyyasofttech.attendsmart.Models.EmployeeModel;
import com.sandhyyasofttech.attendsmart.Models.LocationSnapshot;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdminMovementActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "AdminMovement";

    // UI Elements
    private GoogleMap mMap;
    private SupportMapFragment mapFragment;
    private TextView tvSelectedEmployee, tvSelectedDate, tvStatsInfo;
    private EditText etSearchEmployee;
    private Spinner spinnerDateFilter;
    private CalendarView calendarView;
    private Button btnSelectDate, btnShowRoute, btnCenterMap, btnExportData;
    private ProgressBar progressBar;
    private RecyclerView rvEmployees;
    private LinearLayout calendarLayout, employeeListLayout;
    private Button btnSelectEmployee;

    // Data
    private String companyKey;
    private List<EmployeeModel> employees = new ArrayList<>(); // Changed to EmployeeModel
    private Map<String, List<LocationSnapshot>> employeeRoutes = new HashMap<>();
    private EmployeeModel selectedEmployee; // Changed to EmployeeModel
    private String selectedDate;
    private boolean mapReady = false;

    // Firebase
    private DatabaseReference employeesRef;
    private DatabaseReference attendanceRef;

    // Map elements
    private List<Marker> markers = new ArrayList<>();
    private Map<String, Polyline> routePolylines = new HashMap<>();
    private LatLngBounds.Builder boundsBuilder;

    // Colors for different employee routes
    private int[] routeColors = {
            Color.parseColor("#2196F3"), // Blue
            Color.parseColor("#4CAF50"), // Green
            Color.parseColor("#FF9800"), // Orange
            Color.parseColor("#9C27B0"), // Purple
            Color.parseColor("#F44336"), // Red
            Color.parseColor("#00BCD4"), // Cyan
            Color.parseColor("#FFC107"), // Amber
            Color.parseColor("#795548")  // Brown
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_movement);

        companyKey = new PrefManager(this).getCompanyKey();
        selectedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        initViews();
        setupFirebase();
        loadEmployees();
    }

    private void initViews() {
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        tvSelectedEmployee = findViewById(R.id.tvSelectedEmployee);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        tvStatsInfo = findViewById(R.id.tvStatsInfo);
        btnSelectEmployee = findViewById(R.id.btnSelectEmployee);

        etSearchEmployee = findViewById(R.id.etSearchEmployee);
        spinnerDateFilter = findViewById(R.id.spinnerDateFilter);
        calendarView = findViewById(R.id.calendarView);
        btnSelectDate = findViewById(R.id.btnSelectDate);
        btnShowRoute = findViewById(R.id.btnShowRoute);
        btnCenterMap = findViewById(R.id.btnCenterMap);
        btnExportData = findViewById(R.id.btnExportData);

        progressBar = findViewById(R.id.progressBar);
        rvEmployees = findViewById(R.id.rvEmployees);
        calendarLayout = findViewById(R.id.calendarLayout);
        employeeListLayout = findViewById(R.id.employeeListLayout);

        // Setup date filter spinner
        String[] dateOptions = {"Today", "Select Date"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, dateOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDateFilter.setAdapter(adapter);

        spinnerDateFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) { // Today
                    selectedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            .format(new Date());
                    calendarLayout.setVisibility(View.GONE);
                } else if (position == 1) { // Select Date
                    calendarLayout.setVisibility(View.VISIBLE);
                }
                updateSelectedDateText();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar cal = Calendar.getInstance();
            cal.set(year, month, dayOfMonth);
            selectedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    .format(cal.getTime());
            updateSelectedDateText();
        });

        btnSelectDate.setOnClickListener(v -> calendarLayout.setVisibility(View.GONE));

        btnSelectEmployee.setOnClickListener(v -> {
            employeeListLayout.setVisibility(View.VISIBLE);
            loadEmployees();
        });

        btnShowRoute.setOnClickListener(v -> loadMovementData());

        btnCenterMap.setOnClickListener(v -> centerMapOnRoutes());

        btnExportData.setOnClickListener(v -> showExportDialog());

        // Search functionality
        etSearchEmployee.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterEmployees(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Setup RecyclerView
        rvEmployees.setLayoutManager(new LinearLayoutManager(this));

        updateSelectedDateText();
    }

    private void setupFirebase() {
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        employeesRef = db.getReference("Companies").child(companyKey).child("employees");
        attendanceRef = db.getReference("Companies").child(companyKey).child("attendance");
    }

    private void loadEmployees() {
        showLoading(true);

        employeesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                employees.clear();

                for (DataSnapshot empSnapshot : snapshot.getChildren()) {
                    DataSnapshot info = empSnapshot.child("info");
                    String mobile = empSnapshot.getKey();
                    String name = info.child("employeeName").getValue(String.class);
                    String role = info.child("employeeRole").getValue(String.class);
                    String email = info.child("employeeEmail").getValue(String.class);

                    if (name != null && mobile != null) {
                        // Create EmployeeModel with required fields
                        EmployeeModel employee = new EmployeeModel(mobile, name, mobile, role, email);

                        // Set additional fields if available
                        Boolean requiresGeoFencing = info.child("requiresGeoFencing").getValue(Boolean.class);
                        if (requiresGeoFencing != null) {
                            employee.setRequiresGeoFencing(requiresGeoFencing);
                        }

                        String shift = info.child("employeeShift").getValue(String.class);
                        if (shift != null) {
                            employee.setEmployeeShift(shift);
                        }

                        employees.add(employee);
                    }
                }

                updateEmployeeList(employees);
                showLoading(false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showLoading(false);
                toast("Failed to load employees: " + error.getMessage());
            }
        });
    }

    private void updateEmployeeList(List<EmployeeModel> employeeList) {
        EmployeeModelListAdapter adapter = new EmployeeModelListAdapter(employeeList, employee -> {
            selectedEmployee = employee;
            tvSelectedEmployee.setText(employee.getEmployeeName() + " - " + employee.getEmployeeRole());
            employeeListLayout.setVisibility(View.GONE);
        });

        rvEmployees.setAdapter(adapter);
    }

    private void filterEmployees(String query) {
        if (query.isEmpty()) {
            updateEmployeeList(employees);
            return;
        }

        List<EmployeeModel> filtered = new ArrayList<>();
        String lowerQuery = query.toLowerCase();

        for (EmployeeModel emp : employees) {
            if (emp.getEmployeeName().toLowerCase().contains(lowerQuery) ||
                    emp.getEmployeeMobile().contains(query)) {
                filtered.add(emp);
            }
        }

        updateEmployeeList(filtered);
    }

    private void loadMovementData() {
        if (selectedEmployee == null) {
            toast("Please select an employee first");
            return;
        }

        showLoading(true);
        clearMap();

        employeeRoutes.clear();

        attendanceRef.child(selectedDate).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<LocationSnapshot> snapshots = new ArrayList<>();

                // Get snapshots for selected employee
                DataSnapshot empData = snapshot.child(selectedEmployee.getEmployeeMobile());
                if (empData.exists()) {
                    DataSnapshot movementSnap = empData.child("movement").child("snapshots");
                    for (DataSnapshot snap : movementSnap.getChildren()) {
                        LocationSnapshot locationSnap = snap.getValue(LocationSnapshot.class);
                        if (locationSnap != null) {
                            snapshots.add(locationSnap);
                        }
                    }
                }

                if (!snapshots.isEmpty()) {
                    Collections.sort(snapshots, (s1, s2) -> {
                        if (s1.getTimestamp() == null || s2.getTimestamp() == null) return 0;
                        return s1.getTimestamp().compareTo(s2.getTimestamp());
                    });

                    employeeRoutes.put(selectedEmployee.getEmployeeMobile(), snapshots);

                    if (mapReady) {
                        drawRoutes();
                    }

                    // Show stats
                    updateStats(selectedEmployee, snapshots);
                } else {
                    toast("No movement data found for selected date");
                    tvStatsInfo.setText("No movement data available for " + selectedDate);
                }

                showLoading(false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showLoading(false);
                toast("Failed to load movement data: " + error.getMessage());
            }
        });
    }

    private void drawRoutes() {
        if (mMap == null || employeeRoutes.isEmpty()) return;

        boundsBuilder = new LatLngBounds.Builder();
        int colorIndex = 0;

        for (Map.Entry<String, List<LocationSnapshot>> entry : employeeRoutes.entrySet()) {
            String employeeId = entry.getKey();
            List<LocationSnapshot> snapshots = entry.getValue();

            if (snapshots.size() < 2) continue;

            List<LatLng> points = new ArrayList<>();
            int color = routeColors[colorIndex % routeColors.length];

            // Find employee name
            String employeeName = employeeId;
            for (EmployeeModel emp : employees) {
                if (emp.getEmployeeMobile().equals(employeeId)) {
                    employeeName = emp.getEmployeeName();
                    break;
                }
            }

            // Add markers for start and end
            LocationSnapshot first = snapshots.get(0);
            LocationSnapshot last = snapshots.get(snapshots.size() - 1);

            // Use getLat() and getLng() instead of getLatitude()/getLongitude()
            LatLng startLatLng = new LatLng(first.getLat(), first.getLng());
            Marker startMarker = mMap.addMarker(new MarkerOptions()
                    .position(startLatLng)
                    .title(employeeName + " - Start")
                    .snippet(first.getTimestamp())
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
            if (startMarker != null) {
                markers.add(startMarker);
            }
            boundsBuilder.include(startLatLng);

            LatLng endLatLng = new LatLng(last.getLat(), last.getLng());
            Marker endMarker = mMap.addMarker(new MarkerOptions()
                    .position(endLatLng)
                    .title(employeeName + " - End")
                    .snippet(last.getTimestamp())
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
            if (endMarker != null) {
                markers.add(endMarker);
            }
            boundsBuilder.include(endLatLng);

            // Collect all points for polyline
            for (LocationSnapshot snap : snapshots) {
                LatLng latLng = new LatLng(snap.getLat(), snap.getLng());
                points.add(latLng);
                boundsBuilder.include(latLng);
            }

            // Draw polyline
            PolylineOptions polylineOptions = new PolylineOptions()
                    .addAll(points)
                    .width(6)
                    .color(color)
                    .geodesic(true);

            Polyline polyline = mMap.addPolyline(polylineOptions);
            routePolylines.put(employeeId, polyline);

            colorIndex++;
        }

        centerMapOnRoutes();
    }

    private void centerMapOnRoutes() {
        if (boundsBuilder != null && mMap != null) {
            try {
                LatLngBounds bounds = boundsBuilder.build();
                int padding = 100;
                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
            } catch (Exception e) {
                Log.e(TAG, "Error centering map: " + e.getMessage());
                // If bounds are too small, show default view
                if (mMap != null) {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                            new LatLng(20.5937, 78.9629), 5)); // Default to India view
                }
            }
        }
    }

    private void clearMap() {
        if (mMap != null) {
            for (Polyline polyline : routePolylines.values()) {
                if (polyline != null) {
                    polyline.remove();
                }
            }
            routePolylines.clear();

            for (Marker marker : markers) {
                if (marker != null) {
                    marker.remove();
                }
            }
            markers.clear();
        }
    }

    private void updateStats(EmployeeModel employee, List<LocationSnapshot> snapshots) {
        if (snapshots.isEmpty()) return;

        double totalDistance = 0;
        float totalAccuracy = 0;
        int mockCount = 0;
        int insideGeofenceCount = 0;
        int validAccuracyCount = 0;

        Location lastLocation = null;

        for (LocationSnapshot snap : snapshots) {
            if (lastLocation != null) {
                float[] results = new float[1];
                Location.distanceBetween(
                        lastLocation.getLatitude(),
                        lastLocation.getLongitude(),
                        snap.getLat(),  // Changed from getLatitude()
                        snap.getLng(),  // Changed from getLongitude()
                        results);
                totalDistance += results[0];
            }

            lastLocation = new Location("");
            lastLocation.setLatitude(snap.getLat());  // Changed from getLatitude()
            lastLocation.setLongitude(snap.getLng()); // Changed from getLongitude()

            if (snap.getAccuracy() > 0) {
                totalAccuracy += snap.getAccuracy();
                validAccuracyCount++;
            }

            // Note: isMocked() doesn't exist in your LocationSnapshot class
            // You may need to add this field or remove this check
            // if (snap.isMocked()) mockCount++;

            if (snap.isInsideGeofence()) insideGeofenceCount++;
        }

        float avgAccuracy = validAccuracyCount > 0 ? totalAccuracy / validAccuracyCount : 0;
        float mockPercentage = snapshots.size() > 0 ? (mockCount * 100f) / snapshots.size() : 0;

        String stats = String.format(Locale.getDefault(),
                "Employee: %s\nSnapshots: %d | Distance: %.2f km | Avg Accuracy: %.1f m\n" +
                        "Inside Office: %d times",
                employee.getEmployeeName(),
                snapshots.size(),
                totalDistance / 1000,
                avgAccuracy,
                insideGeofenceCount);
        // Removed mock detection from stats

        tvStatsInfo.setText(stats);
    }

    private void showExportDialog() {
        if (employeeRoutes.isEmpty()) {
            toast("No data to export");
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Export Movement Data");
        builder.setMessage("Export data for " + selectedDate + "?");

        builder.setPositiveButton("Export CSV", (dialog, which) -> {
            exportToCSV();
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void exportToCSV() {
        // Build CSV content
        StringBuilder csv = new StringBuilder();
        csv.append("Employee,Timestamp,Latitude,Longitude,Accuracy,DistanceFromOffice,InsideGeofence\n");

        for (Map.Entry<String, List<LocationSnapshot>> entry : employeeRoutes.entrySet()) {
            String employeeId = entry.getKey();
            String employeeName = employeeId;

            for (EmployeeModel emp : employees) {
                if (emp.getEmployeeMobile().equals(employeeId)) {
                    employeeName = emp.getEmployeeName();
                    break;
                }
            }

            for (LocationSnapshot snap : entry.getValue()) {
                csv.append(String.format(Locale.US,
                        "%s,%s,%.6f,%.6f,%.1f,%.1f,%s\n",
                        employeeName,
                        snap.getTimestamp() != null ? snap.getTimestamp() : "",
                        snap.getLat(),  // Changed from getLatitude()
                        snap.getLng(),  // Changed from getLongitude()
                        snap.getAccuracy(),
                        snap.getDistance(),
                        snap.isInsideGeofence() ? "Yes" : "No"
                ));
            }
        }

        // Here you would save the CSV file
        toast("Export feature - CSV data prepared");
        Log.d(TAG, "CSV Data: \n" + csv.toString());
    }

    private void updateSelectedDateText() {
        tvSelectedDate.setText(selectedDate);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mapReady = true;

        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.setTrafficEnabled(false);
        mMap.setBuildingsEnabled(false);

        // If we already have data, draw it
        if (!employeeRoutes.isEmpty()) {
            drawRoutes();
        }
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (btnShowRoute != null) {
            btnShowRoute.setEnabled(!show);
        }
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        clearMap();
    }
}