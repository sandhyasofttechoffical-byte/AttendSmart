//package com.sandhyyasofttech.attendsmart.Subscription;
//
//import android.content.Intent;
//import android.os.Bundle;
//import android.widget.Button;
//import android.widget.RadioGroup;
//import android.widget.Toast;
//
//import androidx.appcompat.app.AppCompatActivity;
//
//import com.google.firebase.database.DatabaseReference;
//import com.google.firebase.database.FirebaseDatabase;
//import com.sandhyyasofttech.attendsmart.Registration.AdminDashboardActivity;
//import com.sandhyyasofttech.attendsmart.R;
//import com.sandhyyasofttech.attendsmart.Registration.AdminDashboardActivity;
//import com.sandhyyasofttech.attendsmart.Utils.PrefManager;
//
//import java.util.HashMap;
//import java.util.Map;
//
//public class SubscriptionSelectActivity extends AppCompatActivity {
//
//    private RadioGroup rgPlans;
//    private Button btnContinue;
//
//    private DatabaseReference subscriptionRef;
//    private PrefManager prefManager;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_subscription_select);
//
//        rgPlans = findViewById(R.id.rgPlans);
//        btnContinue = findViewById(R.id.btnContinue);
//
//        prefManager = new PrefManager(this);
//        String companyKey = prefManager.getCompanyKey();
//
//        subscriptionRef = FirebaseDatabase.getInstance()
//                .getReference("Companies")
//                .child(companyKey)
//                .child("subscription");
//
//        btnContinue.setOnClickListener(v -> saveSelectedPlan());
//    }
//
//    private void saveSelectedPlan() {
//
//        int checkedId = rgPlans.getCheckedRadioButtonId();
//        if (checkedId == -1) {
//            Toast.makeText(this, "Please select a plan", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        String planId;
//        String planName;
//        int durationDays;
//
//        if (checkedId == R.id.rbTrial) {
//            planId = "TRIAL";
//            planName = "Trial Plan";
//            durationDays = 7;
//        } else if (checkedId == R.id.rbMonthly) {
//            planId = "MONTHLY";
//            planName = "1 Month Plan";
//            durationDays = 30;
//        } else if (checkedId == R.id.rbHalfYearly) {
//            planId = "HALF_YEARLY";
//            planName = "6 Months Plan";
//            durationDays = 180;
//        } else {
//            planId = "YEARLY";
//            planName = "1 Year Plan";
//            durationDays = 365;
//        }
//
//        long startDate = System.currentTimeMillis();
//        long endDate = startDate + (durationDays * 24L * 60 * 60 * 1000);
//
//        Map<String, Object> map = new HashMap<>();
//        map.put("planId", planId);
//        map.put("planName", planName);
//        map.put("durationDays", durationDays);
//        map.put("startDate", startDate);
//        map.put("endDate", endDate);
//        map.put("status", "ACTIVE");
//
//        subscriptionRef.setValue(map)
//                .addOnSuccessListener(unused -> {
//                    Toast.makeText(this,
//                            "Subscription activated successfully",
//                            Toast.LENGTH_LONG).show();
//
//                    startActivity(new Intent(this, com.sandhyyasofttech.attendsmart.Registration.AdminDashboardActivity.class));
//                    finish();
//                })
//                .addOnFailureListener(e ->
//                        Toast.makeText(this,
//                                "Failed to activate subscription",
//                                Toast.LENGTH_LONG).show());
//    }
//}



package com.sandhyyasofttech.attendsmart.Subscription;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.sandhyyasofttech.attendsmart.Registration.AdminDashboardActivity;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Registration.AdminDashboardActivity;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class SubscriptionSelectActivity extends AppCompatActivity {

    private RadioGroup rgPlans;
    private Button btnContinue;

    private DatabaseReference subscriptionRef;
    private PrefManager prefManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subscription_select);

        rgPlans = findViewById(R.id.rgPlans);
        btnContinue = findViewById(R.id.btnContinue);

        prefManager = new PrefManager(this);
        String companyKey = prefManager.getCompanyKey();

        DatabaseReference trialMetaRef = FirebaseDatabase.getInstance()
                .getReference("Companies")
                .child(companyKey)
                .child("subscriptionMeta")
                .child("trialUsed");

        trialMetaRef.get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists() && Boolean.TRUE.equals(snapshot.getValue(Boolean.class))) {
                RadioButton rbTrial = findViewById(R.id.rbTrial);
                rbTrial.setEnabled(false);
                rbTrial.setText("Free Trial (Already Used)");
            }
        });


        btnContinue.setOnClickListener(v -> saveSelectedPlan());
    }

    private long calculateEndDateAtMidnight(long startDate, int days) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(startDate);
        cal.add(Calendar.DAY_OF_YEAR, days);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        return cal.getTimeInMillis();
    }

    private void saveSelectedPlan() {

        int checkedId = rgPlans.getCheckedRadioButtonId();
        if (checkedId == -1) {
            Toast.makeText(this, "Please select a plan", Toast.LENGTH_SHORT).show();
            return;
        }

        String planId;
        String planName;
        int durationDays;
        boolean isTrial;

        int price;

        if (checkedId == R.id.rbTrial) {
            planId = "TRIAL";
            planName = "Trial Plan";
            durationDays = 7;
            price = 0;
            isTrial = true;
        } else if (checkedId == R.id.rbMonthly) {
            planId = "MONTHLY";
            planName = "1 Month Plan";
            durationDays = 30;
            price = 299;
            isTrial = false;
        } else if (checkedId == R.id.rbHalfYearly) {
            planId = "HALF_YEARLY";
            planName = "6 Months Plan";
            durationDays = 180;
            price = 1499;
            isTrial = false;
        } else {
            planId = "YEARLY";
            planName = "1 Year Plan";
            durationDays = 365;
            price = 2499;
            isTrial = false;
        }


        // -------- DATE CALCULATION --------
        long startDateMillis = System.currentTimeMillis();
        long endDateMillis = calculateEndDateAtMidnight(startDateMillis, durationDays);

        // -------- FORMAT DATES (dd/MM/yyyy) --------
        java.text.SimpleDateFormat sdf =
                new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());

        String startDateFormatted = sdf.format(new java.util.Date(startDateMillis));
        String endDateFormatted = sdf.format(new java.util.Date(endDateMillis));

        // -------- SUBSCRIPTION DATA MAP --------
        Map<String, Object> subscriptionMap = new HashMap<>();
        subscriptionMap.put("planId", planId);
        subscriptionMap.put("planName", planName);
        subscriptionMap.put("price", price);
        subscriptionMap.put("currency", "INR");

        subscriptionMap.put("durationDays", durationDays);
        subscriptionMap.put("paymentStatus", "PENDING"); // 🔴 default

        subscriptionMap.put("startDateMillis", startDateMillis);
        subscriptionMap.put("endDateMillis", endDateMillis);

        subscriptionMap.put("startDate", startDateFormatted);
        subscriptionMap.put("endDate", endDateFormatted);

        subscriptionMap.put("status", "ACTIVE");               // future: PENDING
        if (isTrial) {
            subscriptionMap.put("paymentStatus", "FREE");
        } else {
            subscriptionMap.put("paymentStatus", "PENDING"); // 🔴 DEFAULT
        }

        subscriptionMap.put("isTrial", isTrial);
        subscriptionMap.put("activatedBy", "ADMIN");
        subscriptionMap.put("createdAt", System.currentTimeMillis());




        DatabaseReference companyRef = FirebaseDatabase.getInstance()
                .getReference("Companies")
                .child(prefManager.getCompanyKey());

        // -------- MOVE OLD SUBSCRIPTION TO HISTORY --------
        companyRef.child("subscription").get().addOnSuccessListener(oldSnap -> {

            if (oldSnap.exists()) {
                String historyKey = "hist_" + System.currentTimeMillis();
                companyRef.child("subscriptionHistory")
                        .child(historyKey)
                        .setValue(oldSnap.getValue());
            }

            // -------- SAVE NEW ACTIVE SUBSCRIPTION --------
            companyRef.child("subscription")
                    .setValue(subscriptionMap)
                    .addOnSuccessListener(unused -> {

                        // -------- MARK TRIAL USED (ONLY ONCE) --------
                        if (isTrial) {
                            companyRef.child("subscriptionMeta")
                                    .child("trialUsed")
                                    .setValue(true);
                        }

                        String msg;
                        if (isTrial) {
                            msg = "Trial activated successfully";
                        } else {
                            msg = "Plan selected. Waiting for payment approval.";
                        }

                        Toast.makeText(
                                SubscriptionSelectActivity.this,
                                msg,
                                Toast.LENGTH_LONG
                        ).show();


                        Intent intent = new Intent(
                                SubscriptionSelectActivity.this,
                                com.sandhyyasofttech.attendsmart.Registration.AdminDashboardActivity.class
                        );
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(
                                    SubscriptionSelectActivity.this,
                                    "Failed to activate subscription",
                                    Toast.LENGTH_LONG
                            ).show());
        });
    }

}
