package com.sandhyyasofttech.attendsmart.Adapters;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import com.sandhyyasofttech.attendsmart.Models.ExpenseClaim;
import com.sandhyyasofttech.attendsmart.R;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ExpenseClaimAdapter extends RecyclerView.Adapter<ExpenseClaimAdapter.ViewHolder> {

    private List<ExpenseClaim> claimList;
    private List<ExpenseClaim> claimListFull;
    private OnItemClickListener listener;
    private SimpleDateFormat dateFormat;

    public interface OnItemClickListener {
        void onItemClick(ExpenseClaim claim);
    }

    public ExpenseClaimAdapter(OnItemClickListener listener) {
        this.claimList = new ArrayList<>();
        this.claimListFull = new ArrayList<>();
        this.listener = listener;
        this.dateFormat = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_expense_claim, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ExpenseClaim claim = claimList.get(position);
        
        holder.tvAmount.setText(String.format("₹%.2f", claim.getAmount()));
        holder.tvDescription.setText(claim.getDescription());
        
        try {
            long timestamp = Long.parseLong(claim.getTimestamp());
            holder.tvDate.setText(dateFormat.format(new Date(timestamp)));
        } catch (NumberFormatException e) {
            holder.tvDate.setText(claim.getTimestamp());
        }
        
        // Set status with color
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
                holder.tvStatus.setBackgroundResource(R.drawable.bg_rejection_section);
                holder.tvStatus.setTextColor(holder.itemView.getContext().getColor(R.color.status_rejected_text));
                break;
        }
        
        holder.cardView.setOnClickListener(v -> listener.onItemClick(claim));
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

    public void filter(String query) {
        if (TextUtils.isEmpty(query)) {
            claimList = new ArrayList<>(claimListFull);
        } else {
            List<ExpenseClaim> filteredList = new ArrayList<>();
            String lowerQuery = query.toLowerCase();
            
            for (ExpenseClaim claim : claimListFull) {
                if (String.valueOf(claim.getAmount()).contains(lowerQuery) ||
                    claim.getDescription().toLowerCase().contains(lowerQuery) ||
                    claim.getStatus().toLowerCase().contains(lowerQuery)) {
                    filteredList.add(claim);
                }
            }
            claimList = filteredList;
        }
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView tvAmount, tvDescription, tvDate, tvStatus;
        
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}