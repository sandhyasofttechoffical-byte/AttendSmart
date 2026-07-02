package com.sandhyyasofttech.attendsmart.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sandhyyasofttech.attendsmart.Models.EmployeeModel;
import com.sandhyyasofttech.attendsmart.R;

import java.util.List;

public class EmployeeModelListAdapter extends RecyclerView.Adapter<EmployeeModelListAdapter.ViewHolder> {

    private List<EmployeeModel> employees;
    private OnEmployeeClickListener listener;

    public interface OnEmployeeClickListener {
        void onEmployeeClick(EmployeeModel employee);
    }

    public EmployeeModelListAdapter(List<EmployeeModel> employees, OnEmployeeClickListener listener) {
        this.employees = employees;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_employee_simple, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        EmployeeModel employee = employees.get(position);
        holder.tvName.setText(employee.getEmployeeName());
        holder.tvRole.setText(employee.getEmployeeRole());
        holder.tvMobile.setText(employee.getEmployeeMobile());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEmployeeClick(employee);
            }
        });
    }

    @Override
    public int getItemCount() {
        return employees.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvRole, tvMobile;

        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvEmployeeName);
            tvRole = itemView.findViewById(R.id.tvEmployeeRole);
            tvMobile = itemView.findViewById(R.id.tvEmployeeMobile);
        }
    }
}