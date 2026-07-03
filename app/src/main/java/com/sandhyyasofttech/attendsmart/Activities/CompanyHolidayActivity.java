package com.sandhyyasofttech.attendsmart.Activities;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.sandhyyasofttech.attendsmart.Adapters.HolidayAdapter;
import com.sandhyyasofttech.attendsmart.Models.CompanyHolidayModel;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;

import java.util.ArrayList;
import java.util.Calendar;

public class CompanyHolidayActivity extends AppCompatActivity {

    private Toolbar toolbar;

    private TextInputEditText etHolidayName;

    private Button btnSelectDate;
    private Button btnSaveHoliday;

    private TextView tvSelectedDate;

    private RecyclerView rvHolidays;

    private ArrayList<CompanyHolidayModel> holidayList;

    private HolidayAdapter adapter;

    private DatabaseReference companyRef;

    private String selectedDate = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_company_holiday);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setTitle("Company Holidays");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        toolbar.setNavigationOnClickListener(v -> finish());

        etHolidayName = findViewById(R.id.etHolidayName);

        btnSelectDate = findViewById(R.id.btnSelectDate);

        btnSaveHoliday = findViewById(R.id.btnSaveHoliday);

        tvSelectedDate = findViewById(R.id.tvSelectedDate);

        rvHolidays = findViewById(R.id.rvHolidays);

        rvHolidays.setLayoutManager(
                new LinearLayoutManager(this)
        );

        holidayList = new ArrayList<>();

        adapter = new HolidayAdapter(this, holidayList);

        rvHolidays.setAdapter(adapter);

        String companyKey =
                new PrefManager(this).getCompanyKey();

        companyRef = FirebaseDatabase
                .getInstance()
                .getReference("Companies")
                .child(companyKey);

        btnSelectDate.setOnClickListener(v -> openDatePicker());

        btnSaveHoliday.setOnClickListener(v -> {

            String holidayName = etHolidayName.getText().toString().trim();

            if (holidayName.isEmpty()) {
                etHolidayName.setError("Enter Holiday Name");
                return;
            }

            if (selectedDate.isEmpty()) {
                tvSelectedDate.setError("Select Date");
                return;
            }

            CompanyHolidayModel model =
                    new CompanyHolidayModel(
                            holidayName,
                            selectedDate,
                            true
                    );

            companyRef
                    .child("holidays")
                    .child(selectedDate)
                    .setValue(model)
                    .addOnSuccessListener(unused -> {

                        markHolidayForAllEmployees(
                                selectedDate,
                                holidayName
                        );

                    });

        });

        loadHolidays();

    }

    private void markHolidayForAllEmployees(
            String date,
            String holidayName
    ) {

        companyRef
                .child("employees")
                .get()
                .addOnSuccessListener(snapshot -> {

                    for (com.google.firebase.database.DataSnapshot emp :
                            snapshot.getChildren()) {

                        String mobile = emp.getKey();

                        if (mobile == null) continue;

                        DatabaseReference attendanceRef =
                                companyRef
                                        .child("attendance")
                                        .child(date)
                                        .child(mobile);

                        attendanceRef.child("status")
                                .setValue("Holiday");

                        attendanceRef.child("finalStatus")
                                .setValue("Holiday");

                        attendanceRef.child("holidayName")
                                .setValue(holidayName);

                        attendanceRef.child("markedBy")
                                .setValue("Admin");

                        attendanceRef.child("totalHours")
                                .setValue("0.0");

                        attendanceRef.child("totalMinutes")
                                .setValue(0);

                        attendanceRef.child("lastModified")
                                .setValue(System.currentTimeMillis());

                    }

                    etHolidayName.setText("");
                    selectedDate = "";
                    tvSelectedDate.setText("No Date Selected");

                    loadHolidays();

                });

    }

    private void openDatePicker() {

        Calendar calendar = Calendar.getInstance();

        DatePickerDialog dialog =
                new DatePickerDialog(
                        this,
                        (view, year, month, day) -> {

                            month++;

                            selectedDate =
                                    String.format(
                                            "%04d-%02d-%02d",
                                            year,
                                            month,
                                            day
                                    );

                            tvSelectedDate.setText(selectedDate);

                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                );

        dialog.show();

    }

    private void loadHolidays() {

        companyRef
                .child("holidays")
                .get()
                .addOnSuccessListener(snapshot -> {

                    holidayList.clear();

                    for (com.google.firebase.database.DataSnapshot ds :
                            snapshot.getChildren()) {

                        CompanyHolidayModel model =
                                ds.getValue(CompanyHolidayModel.class);

                        if (model != null) {

                            model.holidayId = ds.getKey();

                            holidayList.add(model);

                        }

                    }

                    adapter.notifyDataSetChanged();

                });

    }
}