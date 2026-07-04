package com.sandhyyasofttech.attendsmart.Adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.sandhyyasofttech.attendsmart.Models.CompanyHolidayModel;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;

import java.util.List;

public class HolidayAdapter extends RecyclerView.Adapter<HolidayAdapter.Holder> {

    private final Context context;
    private final List<CompanyHolidayModel> list;

    public HolidayAdapter(
            Context context,
            List<CompanyHolidayModel> list
    ) {
        this.context = context;
        this.list = list;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view = LayoutInflater.from(context)
                .inflate(
                        R.layout.item_company_holiday,
                        parent,
                        false
                );

        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull Holder holder,
            int position
    ) {
        CompanyHolidayModel model = list.get(position);

        holder.tvName.setText(model.holidayName);
        holder.tvDate.setText(model.holidayDate);

        holder.btnDelete.setOnClickListener(v ->
                showDeleteDialog(model)
        );
    }

    private void showDeleteDialog(
            CompanyHolidayModel model
    ) {
        new AlertDialog.Builder(context)
                .setTitle("Delete Company Holiday")
                .setMessage(
                        "This holiday will also be removed from employee attendance records. Continue?"
                )
                .setPositiveButton(
                        "Delete",
                        (dialog, which) -> deleteHolidayCompletely(model)
                )
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteHolidayCompletely(
            CompanyHolidayModel model
    ) {
        String companyKey =
                new PrefManager(context).getCompanyKey();

        if (companyKey == null || companyKey.trim().isEmpty()) {
            Toast.makeText(
                    context,
                    "Company not found",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        String holidayDate = model.holidayDate;

        if (holidayDate == null || holidayDate.trim().isEmpty()) {
            Toast.makeText(
                    context,
                    "Holiday date not found",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        DatabaseReference companyRef =
                FirebaseDatabase.getInstance()
                        .getReference("Companies")
                        .child(companyKey);

        DatabaseReference attendanceDateRef =
                companyRef
                        .child("attendance")
                        .child(holidayDate);

        attendanceDateRef
                .get()
                .addOnSuccessListener(snapshot -> {

                    removeHolidayAttendanceRecords(
                            snapshot,
                            companyRef,
                            model
                    );

                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            context,
                            "Delete failed: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();

                });
    }

    private void removeHolidayAttendanceRecords(
            DataSnapshot snapshot,
            DatabaseReference companyRef,
            CompanyHolidayModel model
    ) {
        int totalChildren = (int) snapshot.getChildrenCount();

        // Attendance node empty asel tar direct master holiday delete
        if (totalChildren == 0) {
            deleteMasterHoliday(companyRef, model);
            return;
        }

        final int[] processedCount = {0};

        for (DataSnapshot employeeSnap : snapshot.getChildren()) {

            String mobile = employeeSnap.getKey();

            String status =
                    employeeSnap
                            .child("status")
                            .getValue(String.class);

            String finalStatus =
                    employeeSnap
                            .child("finalStatus")
                            .getValue(String.class);

            String savedHolidayName =
                    employeeSnap
                            .child("holidayName")
                            .getValue(String.class);

            String markedBy =
                    employeeSnap
                            .child("markedBy")
                            .getValue(String.class);

            boolean isHolidayRecord =
                    "Holiday".equalsIgnoreCase(status)
                            || "Holiday".equalsIgnoreCase(finalStatus);

            boolean sameHolidayName =
                    model.holidayName != null
                            && savedHolidayName != null
                            && model.holidayName.equalsIgnoreCase(
                            savedHolidayName
                    );

            boolean autoCreatedByAdmin =
                    "Admin".equalsIgnoreCase(markedBy);

            /*
             * Safe delete:
             * Holiday record asla pahije
             * AND
             * same holiday name kiwa admin-created asla pahije.
             *
             * Present / Half Day / Absent records delete honar nahi.
             */
            boolean shouldDelete =
                    isHolidayRecord
                            && (
                            sameHolidayName
                                    || autoCreatedByAdmin
                    );

            if (shouldDelete && mobile != null) {

                companyRef
                        .child("attendance")
                        .child(model.holidayDate)
                        .child(mobile)
                        .removeValue()
                        .addOnCompleteListener(task -> {

                            processedCount[0]++;

                            if (processedCount[0] == totalChildren) {
                                deleteMasterHoliday(
                                        companyRef,
                                        model
                                );
                            }

                        });

            } else {

                processedCount[0]++;

                if (processedCount[0] == totalChildren) {
                    deleteMasterHoliday(
                            companyRef,
                            model
                    );
                }
            }
        }
    }

    private void deleteMasterHoliday(
            DatabaseReference companyRef,
            CompanyHolidayModel model
    ) {
        String holidayKey =
                model.holidayId != null
                        && !model.holidayId.trim().isEmpty()
                        ? model.holidayId
                        : model.holidayDate;

        companyRef
                .child("holidays")
                .child(holidayKey)
                .removeValue()
                .addOnSuccessListener(unused -> {

                    Toast.makeText(
                            context,
                            "Holiday deleted successfully",
                            Toast.LENGTH_SHORT
                    ).show();

                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            context,
                            "Holiday list delete failed: "
                                    + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();

                });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class Holder extends RecyclerView.ViewHolder {

        TextView tvName;
        TextView tvDate;
        Button btnDelete;

        Holder(@NonNull View itemView) {
            super(itemView);

            tvName =
                    itemView.findViewById(
                            R.id.tvHolidayName
                    );

            tvDate =
                    itemView.findViewById(
                            R.id.tvHolidayDate
                    );

            btnDelete =
                    itemView.findViewById(
                            R.id.btnDelete
                    );
        }
    }
}