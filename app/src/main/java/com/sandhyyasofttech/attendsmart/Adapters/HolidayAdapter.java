package com.sandhyyasofttech.attendsmart.Adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.FirebaseDatabase;
import com.sandhyyasofttech.attendsmart.Models.CompanyHolidayModel;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;

import java.util.List;

public class HolidayAdapter extends RecyclerView.Adapter<HolidayAdapter.Holder> {

    Context context;
    List<CompanyHolidayModel> list;

    public HolidayAdapter(Context context,
                          List<CompanyHolidayModel> list) {

        this.context = context;
        this.list = list;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent,
                                     int viewType) {

        View v = LayoutInflater.from(context)
                .inflate(R.layout.item_company_holiday,
                        parent,
                        false);

        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h,
                                 int position) {

        CompanyHolidayModel model = list.get(position);

        h.tvName.setText(model.holidayName);
        h.tvDate.setText(model.holidayDate);

        h.btnDelete.setOnClickListener(v -> {

            new AlertDialog.Builder(context)
                    .setTitle("Delete Holiday")
                    .setMessage("Delete this holiday?")
                    .setPositiveButton("Delete",
                            (dialog, which) -> {

                                String company =
                                        new PrefManager(context)
                                                .getCompanyKey();

                                FirebaseDatabase.getInstance()
                                        .getReference("Companies")
                                        .child(company)
                                        .child("holidays")
                                        .child(model.holidayId)
                                        .removeValue();

                            })
                    .setNegativeButton("Cancel", null)
                    .show();

        });

    }

    @Override
    public int getItemCount() {

        return list.size();
    }

    class Holder extends RecyclerView.ViewHolder {

        TextView tvName, tvDate;
        Button btnDelete;

        Holder(View itemView) {

            super(itemView);

            tvName = itemView.findViewById(R.id.tvHolidayName);
            tvDate = itemView.findViewById(R.id.tvHolidayDate);
            btnDelete = itemView.findViewById(R.id.btnDelete);

        }
    }

}