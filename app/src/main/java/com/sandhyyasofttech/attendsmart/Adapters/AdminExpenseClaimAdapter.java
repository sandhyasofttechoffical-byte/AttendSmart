package com.sandhyyasofttech.attendsmart.Adapters;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import com.sandhyyasofttech.attendsmart.Models.ExpenseClaim;
import com.sandhyyasofttech.attendsmart.R;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdminExpenseClaimAdapter extends RecyclerView.Adapter<AdminExpenseClaimAdapter.ViewHolder> {

    private List<ExpenseClaim> claimList;
    private List<ExpenseClaim> claimListFull;
    private OnItemClickListener listener;
    private SimpleDateFormat dateFormat;
    private DecimalFormat df;

    public interface OnItemClickListener {
        void onItemClick(ExpenseClaim claim);
    }

    public AdminExpenseClaimAdapter(OnItemClickListener listener) {
        this.claimList = new ArrayList<>();
        this.claimListFull = new ArrayList<>();
        this.listener = listener;
        this.dateFormat = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
        this.df = new DecimalFormat("#,##0.00");
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_expense_claim, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ExpenseClaim claim = claimList.get(position);
        
        // Employee Name
        holder.tvEmployeeName.setText(claim.getUserName() != null ? claim.getUserName() : "Unknown");
        
        // Total Amount
        holder.tvAmount.setText("₹" + df.format(claim.getTotalAmount()));
        
        // Items count
        int itemCount = claim.getItems() != null ? claim.getItems().size() : 0;
        holder.tvItemsCount.setText(itemCount + " item" + (itemCount != 1 ? "s" : ""));
        
        // Date
        try {
            long timestamp = Long.parseLong(claim.getTimestamp());
            holder.tvDate.setText(dateFormat.format(new Date(timestamp)));
        } catch (NumberFormatException e) {
            holder.tvDate.setText(claim.getTimestamp());
        }
        
        // Status with color
        holder.tvStatus.setText(claim.getStatus().toUpperCase());
        switch (claim.getStatus().toLowerCase()) {
            case "pending":
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_pending);
                holder.tvStatus.setTextColor(holder.itemView.getContext().getColor(R.color.status_pending_text));
                break;
            case "approved":
                holder.tvStatus.setBackgroundResource(R.drawable.calendar_bg_green);
                holder.tvStatus.setTextColor(holder.itemView.getContext().getColor(R.color.status_approved_text));
                break;
            case "rejected":
                holder.tvStatus.setBackgroundResource(R.drawable.calendar_bg_red);
                holder.tvStatus.setTextColor(holder.itemView.getContext().getColor(R.color.status_rejected_text));
                break;
        }
        
        holder.cardView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(claim);
            }
        });
    }

    @Override
    public int getItemCount() {
        return claimList.size();
    }

    public void setClaimList(List<ExpenseClaim> claims) {
        this.claimList = claims;
        this.claimListFull = new ArrayList<>(claims);
        notifyDataSetChanged();
    }

    public void filterByStatus(String status) {
        if (status.equals("all")) {
            claimList = new ArrayList<>(claimListFull);
        } else {
            List<ExpenseClaim> filteredList = new ArrayList<>();
            for (ExpenseClaim claim : claimListFull) {
                if (claim.getStatus().equals(status)) {
                    filteredList.add(claim);
                }
            }
            claimList = filteredList;
        }
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView tvEmployeeName, tvAmount, tvItemsCount, tvDate, tvStatus;
        
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            tvEmployeeName = itemView.findViewById(R.id.tvEmployeeName);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvItemsCount = itemView.findViewById(R.id.tvItemsCount);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}