    package com.sandhyyasofttech.attendsmart.Activities;
    
    import android.app.DatePickerDialog;
    import android.content.Intent;
    import android.os.Bundle;
    import android.util.Log;
    import android.view.View;
    import android.widget.ArrayAdapter;
    import android.widget.Button;
    import android.widget.EditText;
    import android.widget.Spinner;
    import android.widget.Toast;
    
    import androidx.annotation.NonNull;
    import androidx.appcompat.app.AppCompatActivity;
    import androidx.appcompat.widget.Toolbar;
    
    import com.google.firebase.database.DataSnapshot;
    import com.google.firebase.database.DatabaseError;
    import com.google.firebase.database.DatabaseReference;
    import com.google.firebase.database.FirebaseDatabase;
    import com.google.firebase.database.ValueEventListener;
    import com.sandhyyasofttech.attendsmart.Models.MonthlyAttendanceSummary;
    import com.sandhyyasofttech.attendsmart.Models.SalaryCalculationResult;
    import com.sandhyyasofttech.attendsmart.Models.SalaryConfig;
    import com.sandhyyasofttech.attendsmart.Models.SalaryPreviewData;
    import com.sandhyyasofttech.attendsmart.Models.SalarySnapshot;
    import com.sandhyyasofttech.attendsmart.R;
    import com.sandhyyasofttech.attendsmart.Utils.PrefManager;
    import com.sandhyyasofttech.attendsmart.payroll.SalaryCalculator;
    
    import java.text.SimpleDateFormat;
    import java.util.ArrayList;
    import java.util.Calendar;
    import java.util.HashSet;
    import java.util.Locale;
    import java.util.Set;
    
    public class GenerateSalaryActivity extends AppCompatActivity {
    
        private EditText etMonth;
        private Spinner spEmployee;
        private Button btnGenerate;
        private Button btnViewSalary;
        private Toolbar toolbar;
    
        private String companyKey;
        private DatabaseReference companyRef;
        private final ArrayList<String> employeeMobiles = new ArrayList<>();


        @Override

        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_generate_salary);
            // Set status bar color
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                getWindow().setStatusBarColor(getResources().getColor(R.color.blue_800));
            }
    
    
            // Initialize all views
            toolbar = findViewById(R.id.toolbar);
            etMonth = findViewById(R.id.etMonth);
            spEmployee = findViewById(R.id.spEmployee);
            btnGenerate = findViewById(R.id.btnGenerateSalary);
            btnViewSalary = findViewById(R.id.btnViewSalary);
    
            // Setup toolbar with back arrow
            setupToolbar();
    
            PrefManager pref = new PrefManager(this);
            companyKey = pref.getCompanyKey(); // Use getCompanyKey() instead of getUserEmail()
    
            companyRef = FirebaseDatabase.getInstance()
                    .getReference("Companies")
                    .child(companyKey);
    
            setupMonthPicker();
            loadEmployees();
    
            btnGenerate.setOnClickListener(v -> validateAndGenerate());
        }
    
        // GenerateSalaryActivity मध्ये add कर
        private void fetchEmployeeWeeklyHoliday(String employeeMobile, HolidayCallback callback) {
            companyRef.child("employees")
                    .child(employeeMobile)
                    .child("info")
                    .child("weeklyHoliday")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            String weeklyHoliday = snapshot.getValue(String.class);
                            // "Sunday", "Monday", "Friday", "Saturday & Sunday", "None"
                            callback.onSuccess(weeklyHoliday);
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            callback.onError();
                        }
                    });
        }
        private int calculateWorkingDaysForMonth(String month, String weeklyHoliday, String employeeMobile) {
            // month format: "MM-yyyy" e.g., "01-2026"
            String[] parts = month.split("-");
            int monthNum = Integer.parseInt(parts[0]);  // 1-12
            int year = Integer.parseInt(parts[1]);
    
            Calendar cal = Calendar.getInstance();
            cal.set(year, monthNum - 1, 1);
            int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
    
            // Parse weekly holiday
            Set<Integer> weeklyOffDays = parseWeeklyHoliday(weeklyHoliday);
    
            // Fetch company holidays for this month
//            Set<String> companyHolidays = fetchCompanyHolidaysForMonth(month);
    
            int workingDays = 0;
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    
            for (int day = 1; day <= daysInMonth; day++) {
                cal.set(year, monthNum - 1, day);
                int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
                String dateStr = String.format("%04d-%02d-%02d", year, monthNum, day);
    
                // Check if weekly off
//                if (weeklyOffDays.contains(dayOfWeek)) {
//                    continue;  // Weekly holiday - no salary
//                }
    
                // Check if company holiday
//                if (companyHolidays.contains(dateStr)) {
//                    continue;  // Public holiday - no salary
//                }
    
                workingDays++;
            }
    
            return workingDays;
        }
//         ================= FETCH COMPANY HOLIDAYS =================
//        private Set<String> fetchCompanyHolidaysForMonth(String month) {
//
//            Set<String> holidays = new HashSet<>();
//
//            try {
//
//                DataSnapshot snapshot =
//                        com.google.android.gms.tasks.Tasks.await(
//                                companyRef.child("holidays").get()
//                        );
//
//                String[] parts = month.split("-");
//                String searchMonth = parts[1] + "-" + parts[0];
//
//                for (DataSnapshot holiday : snapshot.getChildren()) {
//
//                    String date = holiday.child("date").getValue(String.class);
//
//                    if (date == null) continue;
//
//                    if (date.startsWith(searchMonth)) {
//                        holidays.add(date);
//                    }
//
//                }
//
//            } catch (Exception e) {
//
//                e.printStackTrace();
//
//            }
//
//            return holidays;
//        }
    
//        private interface HolidayListCallback{
//
//            void onSuccess(Set<String> holidays);
//
//        }
    
        // ================= GET WORKING DATES FOR MONTH =================
        private Set<String> getWorkingDatesForMonth(String month, String weeklyHoliday, String employeeMobile) {
            Set<String> workingDates = new HashSet<>();
    
            String[] parts = month.split("-");
            int monthNum = Integer.parseInt(parts[0]);
            int year = Integer.parseInt(parts[1]);
    
            Calendar cal = Calendar.getInstance();
            cal.set(year, monthNum - 1, 1);
            int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
    
            Set<Integer> weeklyOffDays = parseWeeklyHoliday(weeklyHoliday);
//            Set<String> companyHolidays = fetchCompanyHolidaysForMonth(month);
    
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    
            for (int day = 1; day <= daysInMonth; day++) {
                cal.set(year, monthNum - 1, day);
                int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
                String dateStr = String.format("%04d-%02d-%02d", year, monthNum, day);
    
                // Skip weekly off
//                if (weeklyOffDays.contains(dayOfWeek)) {
//                    continue;
//                }
//
//                // Skip company holiday
//                if (companyHolidays.contains(dateStr)) {
//                    continue;
//                }

                workingDates.add(dateStr);
            }
    
            return workingDates;
        }
    
        // ================= PROPORTIONAL ATTENDANCE SUMMARY (WITH HOLIDAYS) =================
        private void buildMonthlyAttendanceSummaryProportional(
                String month,
                String employeeMobile,
                SalaryConfig config,
                AttendanceCallback callback
        ) {
            // ✅ First fetch employee's weekly holiday
            fetchEmployeeWeeklyHoliday(employeeMobile, new HolidayCallback() {
                @Override
                public void onSuccess(String weeklyHoliday) {
                    MonthlyAttendanceSummary summary = new MonthlyAttendanceSummary();
    
                    // ✅ नवीन: Total days in month set कर
                    int totalDaysInMonth = getTotalDaysInMonth(month);
                    summary.totalDaysInMonth = totalDaysInMonth;
    
                    // Working days calculate कर (Sundays & holidays वगळून)
                    int workingDaysInMonth = calculateWorkingDaysForMonth(month, weeklyHoliday, employeeMobile);
                    summary.workingDaysInMonth = workingDaysInMonth;
    
                    Log.d("SALARY_DEBUG", "Month: " + month);
                    Log.d("SALARY_DEBUG", "Total days in month: " + totalDaysInMonth);
                    Log.d("SALARY_DEBUG", "Working days: " + workingDaysInMonth);
    
                    // Fetch attendance
                    fetchAttendanceForMonth(month, employeeMobile, weeklyHoliday, summary, config, callback);
                }
    
                @Override
                public void onError() {
                    // Error झाल्यास default "Sunday" वापर
                    MonthlyAttendanceSummary summary = new MonthlyAttendanceSummary();
                    int totalDaysInMonth = getTotalDaysInMonth(month);
                    summary.totalDaysInMonth = totalDaysInMonth;
                    int workingDaysInMonth = calculateWorkingDaysForMonth(month, "Sunday", employeeMobile);
                    summary.workingDaysInMonth = workingDaysInMonth;
                    fetchAttendanceForMonth(month, employeeMobile, "Sunday", summary, config, callback);
                }
            });
        }
    
        private void fetchAttendanceForMonth(
                String month,
                String employeeMobile,
                String weeklyHoliday,
                MonthlyAttendanceSummary summary,
                SalaryConfig config,
                AttendanceCallback callback
        ) {
            Set<String> workingDates = getWorkingDatesForMonth(month, weeklyHoliday, employeeMobile);
    
            companyRef.child("attendance").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot attendanceSnap) {
                    Set<String> countedDates = new HashSet<>();
                    for (String workingDate : workingDates) {

                        Calendar cal = Calendar.getInstance();

                        try {
                            SimpleDateFormat sdf =
                                    new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

                            cal.setTime(sdf.parse(workingDate));

                        } catch (Exception e) {
                            continue;
                        }

                        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);


                        Log.d(
                                "WEEKLY_DEBUG",
                                "Date=" + workingDate +
                                        " Day=" + dayOfWeek +
                                        " Weekly=" + weeklyHoliday +
                                        " Match=" + parseWeeklyHoliday(weeklyHoliday).contains(dayOfWeek)
                        );


                        if (parseWeeklyHoliday(weeklyHoliday).contains(dayOfWeek)) {
                            summary.weeklyHolidayCount++;
                        }

//                        if (fetchCompanyHolidaysForMonth(month).contains(workingDate)) {
//                            summary.companyHolidayCount++;
//                        }
                    }
    
                    // Track which working dates have attendance
                    Set<String> attendedDates = new HashSet<>();
    
                    for (DataSnapshot dateSnap : attendanceSnap.getChildren()) {
                        String dateKey = dateSnap.getKey();
                        if (dateKey == null) continue;
    
                        // Check if this date belongs to selected month
                        String recordMonth = dateKey.substring(5, 7) + "-" + dateKey.substring(0, 4);
                        if (!recordMonth.equals(month)) continue;
    
                        // Check if this is a working day
                        if (!workingDates.contains(dateKey)) continue;
    
                        DataSnapshot empSnap = dateSnap.child(employeeMobile);
                        if (!empSnap.exists()) {
                            continue;
                        }
    
                        String finalStatus = empSnap.child("finalStatus").getValue(String.class);
    
                        if (finalStatus == null) {
                            continue;
                        }


                        String lateStatus = empSnap.child("lateStatus").getValue(String.class);

                        if (lateStatus != null
                                && lateStatus.trim().toLowerCase(Locale.ROOT).contains("late")) {

                            summary.lateCount++;

                            Log.d(
                                    "LATE_DEBUG",
                                    "Date=" + dateKey +
                                            ", Employee=" + employeeMobile +
                                            ", LateStatus=" + lateStatus +
                                            ", TotalLate=" + summary.lateCount
                            );
                        }

                        attendedDates.add(dateKey);

                        if ("Holiday".equalsIgnoreCase(finalStatus)) {
                            summary.companyHolidayCount++;
                            continue;
                        }

                        finalStatus = finalStatus.toLowerCase();

                        if (finalStatus.contains("present")) {
                            summary.presentDays++;
                        } else if (finalStatus.contains("half")) {
                            summary.halfDays++;
                        } else if (finalStatus.contains("absent")) {
                            summary.absentDays++;
                        }


                    }
    
                    // Check for working days with no attendance (absent)
                    for (String workingDate : workingDates) {

                        Calendar cal = Calendar.getInstance();

                        try {
                            SimpleDateFormat sdf =
                                    new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

                            cal.setTime(sdf.parse(workingDate));

                        } catch (Exception e) {
                            continue;
                        }

                        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);

                        // Weekly Holiday skip
                        if (parseWeeklyHoliday(weeklyHoliday).contains(dayOfWeek)) {
                            continue;
                        }


                        if (!attendedDates.contains(workingDate)) {
                            summary.absentDays++;
                        }
                    }
    
                    // Now count leaves
                    countLeavesProportional(month, employeeMobile, config, summary, callback);
                }
    
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    callback.onError(error.getMessage());
                }
            });
        }
    
        private void countLeavesProportional(
                String month,
                String employeeMobile,
                SalaryConfig config,
                MonthlyAttendanceSummary summary,
                AttendanceCallback callback
        ) {
            companyRef.child("leaves").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    int paidLimit = config.paidLeaves;
                    int paidUsed = 0;
    
                    for (DataSnapshot l : snapshot.getChildren()) {
                        if (!employeeMobile.equals(l.child("employeeMobile").getValue(String.class)))
                            continue;
    
                        if (!"APPROVED".equals(l.child("status").getValue(String.class)))
                            continue;
    
                        boolean isPaid = Boolean.TRUE.equals(l.child("isPaid").getValue(Boolean.class));
    
                        int days = countDaysInMonth(
                                l.child("fromDate").getValue(String.class),
                                l.child("toDate").getValue(String.class),
                                month
                        );
    
                        Log.d("LEAVE_DEBUG",
                                "Employee=" + employeeMobile +
                                        " From=" + l.child("fromDate").getValue(String.class) +
                                        " To=" + l.child("toDate").getValue(String.class) +
                                        " Days=" + days);
    
                        if (isPaid && paidUsed < paidLimit) {
                            int allowed = Math.min(days, paidLimit - paidUsed);
                            summary.paidLeavesUsed += allowed;
                            paidUsed += allowed;
                            summary.unpaidLeaves += (days - allowed);
                        } else {
                            summary.unpaidLeaves += days;
                        }
                    }
                    Log.d("SALARY_FINAL",
                            "Present = " + summary.presentDays);
    
                    Log.d("SALARY_FINAL",
                            "Absent = " + summary.absentDays);
    
                    Log.d("SALARY_FINAL",
                            "Working Days = " + summary.workingDaysInMonth);
                    callback.onReady(summary);
                }
    
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    callback.onError(error.getMessage());
                }
            });
        }
        private Set<Integer> parseWeeklyHoliday(String holiday) {
            Set<Integer> days = new HashSet<>();
            if (holiday == null || holiday.equals("None") || holiday.isEmpty()) {
                return days;  // No weekly off
            }
    
            if (holiday.contains("&")) {
                // "Saturday & Sunday" case
                if (holiday.contains("Saturday")) days.add(Calendar.SATURDAY);
                if (holiday.contains("Sunday")) days.add(Calendar.SUNDAY);
            } else {
                // Single day: "Sunday", "Monday", etc.
                switch (holiday.trim()) {
                    case "Sunday": days.add(Calendar.SUNDAY); break;
                    case "Monday": days.add(Calendar.MONDAY); break;
                    case "Tuesday": days.add(Calendar.TUESDAY); break;
                    case "Wednesday": days.add(Calendar.WEDNESDAY); break;
                    case "Thursday": days.add(Calendar.THURSDAY); break;
                    case "Friday": days.add(Calendar.FRIDAY); break;
                    case "Saturday": days.add(Calendar.SATURDAY); break;
                }
            }
            return days;
        }
        // ================= TOOLBAR SETUP WITH BACK ARROW =================
        private void setupToolbar() {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setDisplayShowHomeEnabled(true);
                getSupportActionBar().setTitle("Generate Salary");
            }
    
            // Handle back arrow click
            toolbar.setNavigationOnClickListener(v -> onBackPressed());
        }
    
        // ================= MONTH PICKER =================
        private void setupMonthPicker() {
            etMonth.setOnClickListener(v -> {
                Calendar c = Calendar.getInstance();
                DatePickerDialog dialog = new DatePickerDialog(
                        this,
                        (view, year, month, day) -> {
                            String value = String.format(
                                    Locale.getDefault(),
                                    "%02d-%d",
                                    month + 1, year);
                            etMonth.setText(value);
                        },
                        c.get(Calendar.YEAR),
                        c.get(Calendar.MONTH),
                        c.get(Calendar.DAY_OF_MONTH)
                );
                dialog.show();
            });
        }
    
        // ================= LOAD EMPLOYEES =================
        private void loadEmployees() {
            companyRef.child("employees")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {

                            employeeMobiles.clear();
                            for (DataSnapshot s : snapshot.getChildren()) {
                                employeeMobiles.add(s.getKey());
                            }
    
                            if (employeeMobiles.isEmpty()) {
                                Toast.makeText(GenerateSalaryActivity.this,
                                        "No employees found",
                                        Toast.LENGTH_SHORT).show();
                                return;
                            }
    
                            ArrayAdapter<String> adapter =
                                    new ArrayAdapter<>(GenerateSalaryActivity.this,
                                            android.R.layout.simple_spinner_item,
                                            employeeMobiles);
                            adapter.setDropDownViewResource(
                                    android.R.layout.simple_spinner_dropdown_item);
                            spEmployee.setAdapter(adapter);
                        }
    
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Toast.makeText(GenerateSalaryActivity.this,
                                    "Failed to load employees",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    
        // ================= VALIDATE =================
        private void validateAndGenerate() {
            String month = etMonth.getText().toString().trim();
            if (month.isEmpty()) {
                etMonth.setError("Select month");
                return;
            }
    
            if (spEmployee.getSelectedItem() == null) {
                Toast.makeText(this, "Select employee", Toast.LENGTH_SHORT).show();
                return;
            }
    
            String employeeMobile = spEmployee.getSelectedItem().toString();
            fetchSalaryConfigAndAttendance(month, employeeMobile);
        }
    
        // ================= FETCH CONFIG + ATTENDANCE =================
        private void fetchSalaryConfigAndAttendance(String month, String employeeMobile) {
            btnGenerate.setEnabled(false);
            btnGenerate.setText("Processing...");
    
            companyRef.child("employees")
                    .child(employeeMobile)
                    .child("salaryConfig")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (!snapshot.exists()) {
                                Toast.makeText(GenerateSalaryActivity.this,
                                        "Salary config not set for this employee",
                                        Toast.LENGTH_LONG).show();
                                resetGenerateButton();
                                return;
                            }
    
                            SalaryConfig config = parseSalaryConfig(snapshot);
    
                            if (config.monthlySalary <= 0) {
                                Toast.makeText(GenerateSalaryActivity.this,
                                        "Invalid salary configuration",
                                        Toast.LENGTH_LONG).show();
                                resetGenerateButton();
                                return;
                            }
    
                            // ✅ नवीन proportional method call कर
                            buildMonthlyAttendanceSummaryProportional(
                                    month,
                                    employeeMobile,
                                    config,
                                    new AttendanceCallback() {
                                        @Override
                                        public void onReady(MonthlyAttendanceSummary summary) {
                                            generateAndSaveSalary(month, employeeMobile, summary, config);
                                        }
    
                                        @Override
                                        public void onError(String error) {
                                            Toast.makeText(GenerateSalaryActivity.this,
                                                    error, Toast.LENGTH_LONG).show();
                                            resetGenerateButton();
                                        }
                                    }
                            );
                        }
    
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Toast.makeText(GenerateSalaryActivity.this,
                                    "Failed to fetch salary config", Toast.LENGTH_SHORT).show();
                            resetGenerateButton();
                        }
                    });
        }
    
        private void resetGenerateButton() {
            btnGenerate.setEnabled(true);
            btnGenerate.setText("Generate Salary");
        }
    
        // ================= ATTENDANCE SUMMARY =================
        private void buildMonthlyAttendanceSummary(
                String month,
                String employeeMobile,
                SalaryConfig config,
                AttendanceCallback callback
        ) {
            MonthlyAttendanceSummary summary = new MonthlyAttendanceSummary();
    
            // ================= 1️⃣ ATTENDANCE =================
            companyRef.child("attendance")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot attendanceSnap) {
    
                            for (DataSnapshot dateSnap : attendanceSnap.getChildren()) {
                                String dateKey = dateSnap.getKey(); // yyyy-MM-dd
                                if (dateKey == null || dateKey.length() < 7) continue;
    
                                String recordMonth =
                                        dateKey.substring(5, 7) + "-" +
                                                dateKey.substring(0, 4);
    
                                if (!recordMonth.equals(month)) continue;
    
                                DataSnapshot empSnap =
                                        dateSnap.child(employeeMobile);
    
                                if (!empSnap.exists()) continue;
    
                                String finalStatus =
                                        empSnap.child("finalStatus").getValue(String.class);
    
                                if (finalStatus == null) continue;
    
                                finalStatus = finalStatus.toLowerCase();



                                    if (finalStatus.contains("present")) {
                                        summary.presentDays++;
                                    }

                                else if (finalStatus.contains("half")) {
                                    summary.halfDays++;
                                } else {
                                    // summary.absentDays++;
                                }
                            }
    
                            // ================= 2️⃣ LEAVES =================
                            countLeaves(month, employeeMobile, config, summary, callback);
                        }
    
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            callback.onError(error.getMessage());
                        }
                    });
        }
        // ================= TOTAL DAYS IN MONTH =================
        private int getTotalDaysInMonth(String month) {
            String[] parts = month.split("-");
            int monthNum = Integer.parseInt(parts[0]);
            int year = Integer.parseInt(parts[1]);
    
            Calendar cal = Calendar.getInstance();
            cal.set(year, monthNum - 1, 1);
            return cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        }
        private int countDaysInMonth(
                String fromDate,
                String toDate,
                String month
        ) {
            if (fromDate == null || toDate == null)
                return 0;
    
            try {
    
                java.text.SimpleDateFormat sdf =
                        new java.text.SimpleDateFormat(
                                "yyyy-MM-dd",
                                java.util.Locale.getDefault());
    
                java.util.Date start =
                        sdf.parse(fromDate);
    
                java.util.Date end =
                        sdf.parse(toDate);
    
                java.util.Calendar cal =
                        java.util.Calendar.getInstance();
    
                cal.setTime(start);
    
                int count = 0;
    
                while (!cal.getTime().after(end)) {
    
                    String current =
                            sdf.format(cal.getTime());
    
                    String currentMonth =
                            current.substring(5, 7)
                                    + "-"
                                    + current.substring(0, 4);
    
                    if (currentMonth.equals(month)) {
                        count++;
                    }
    
                    cal.add(
                            java.util.Calendar.DAY_OF_MONTH,
                            1
                    );
                }
    
                return count;
    
            } catch (Exception e) {
                return 0;
            }
        }
    
        private void countLeaves(
                String month,
                String employeeMobile,
                SalaryConfig config,
                MonthlyAttendanceSummary summary,
                AttendanceCallback callback
        ) {
            companyRef.child("leaves")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
    
                            int paidLimit = config.paidLeaves;
                            int paidUsed = 0;
    
                            for (DataSnapshot l : snapshot.getChildren()) {
                                if (!employeeMobile.equals(
                                        l.child("employeeMobile").getValue(String.class)))
                                    continue;
    
                                if (!"APPROVED".equals(
                                        l.child("status").getValue(String.class)))
                                    continue;
    
                                boolean isPaid =
                                        Boolean.TRUE.equals(l.child("isPaid").getValue(Boolean.class));
    
                                int days = countDaysInMonth(
                                        l.child("fromDate").getValue(String.class),
                                        l.child("toDate").getValue(String.class),
                                        month
                                );
    
                                if (isPaid && paidUsed < paidLimit) {
                                    int allowed = Math.min(days, paidLimit - paidUsed);
                                    summary.paidLeavesUsed += allowed;
                                    paidUsed += allowed;
                                    summary.unpaidLeaves += (days - allowed);
                                } else {
                                    summary.unpaidLeaves += days;
                                }
                                if (summary.absentDays < 0) summary.absentDays = 0;
                            }
    
                            // ✅ FINAL CALLBACK (ONLY ONCE)
                            callback.onReady(summary);
                        }
    
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            callback.onError(error.getMessage());
                        }
                    });
        }
    
        // ================= GENERATE & SAVE =================
    // ================= GENERATE & SAVE =================
        private void generateAndSaveSalary(
                String month,
                String employeeMobile,
                MonthlyAttendanceSummary summary,
                SalaryConfig config
        ) {
            SalaryCalculationResult result = SalaryCalculator.calculateSalary(summary, config);
    
            // ✅ आता योग्य field names वापरा
            // result मध्ये basicSalary नाही, grossSalary आहे
            // result मध्ये otherDeductions नाही, otherDeduction आहे
    
            // ✅ Direct save नाही, तर Preview screen वर navigate करा
            SalaryPreviewData previewData = new SalaryPreviewData();
            previewData.month = month;
            previewData.employeeMobile = employeeMobile;
            previewData.attendanceSummary = summary;
            previewData.salaryConfig = config;
            previewData.calculationResult = result;
    
            // Original values सेट करा (योग्य field names वापरून)
            previewData.manualGrossSalary = result.grossSalary;          // basicSalary च्या ऐवजी
            previewData.manualPfAmount = result.pfAmount;
            previewData.manualEsiAmount = result.esiAmount;
            previewData.manualOtherDeduction = result.otherDeduction;   // otherDeductions च्या ऐवजी
            previewData.manualNetSalary = result.netSalary;
    
            // Preview Activity ला navigate करा
            Intent intent = new Intent(
                    GenerateSalaryActivity.this,
                    SalaryPreviewActivity.class
            );
            intent.putExtra("previewData", previewData);
            startActivityForResult(intent, 101);
    
            resetGenerateButton();
        }
        // ================= SAFE PARSING =================
        private SalaryConfig parseSalaryConfig(DataSnapshot s) {
            SalaryConfig c = new SalaryConfig();
    
            c.monthlySalary = getDouble(s, "monthlySalary");
            c.workingDays = getInt(s, "workingDays");
            c.paidLeaves = getInt(s, "paidLeaves");
    
            c.pfPercent = getDouble(s, "pfPercent");
            c.esiPercent = getDouble(s, "esiPercent");
            c.otherDeduction = getDouble(s, "otherDeduction");
    
            c.deductionEnabled =
                    Boolean.TRUE.equals(
                            s.child("deductionEnabled").getValue(Boolean.class));
    
            c.lateRule = getString(s, "lateRule");
            c.effectiveFrom = getString(s, "effectiveFrom");
            c.deductionNote = getString(s, "deductionNote");
    
            return c;
        }
    
        private double getDouble(DataSnapshot s, String key) {
            try {
                Object v = s.child(key).getValue();
                if (v == null) return 0;
                if (v instanceof Number) return ((Number) v).doubleValue();
                return Double.parseDouble(v.toString());
            } catch (Exception e) {
                return 0;
            }
        }
    
        private int getInt(DataSnapshot s, String key) {
            try {
                Object v = s.child(key).getValue();
                if (v == null) return 0;
                if (v instanceof Number) return ((Number) v).intValue();
                return Integer.parseInt(v.toString());
            } catch (Exception e) {
                return 0;
            }
        }
    
        private String getString(DataSnapshot s, String key) {
            Object v = s.child(key).getValue();
            return v == null ? "" : v.toString();
        }
    
        // ================= CALLBACK =================
        private interface AttendanceCallback {
            void onReady(MonthlyAttendanceSummary summary);
    
            void onError(String error);
        }
        // ✅ ही नवीन interface add कर
        private interface HolidayCallback {
            void onSuccess(String weeklyHoliday);
            void onError();
        }
        @Override
        public boolean onSupportNavigateUp() {
            onBackPressed();
            return true;
        }
    }