package com.sandhyyasofttech.attendsmart.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.sandhyyasofttech.attendsmart.Models.ExpenseItem;
import com.sandhyyasofttech.attendsmart.R;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class ExpenseItemAdapter extends RecyclerView.Adapter<ExpenseItemAdapter.ViewHolder> {

    private List<ExpenseItem> items;
    private OnItemDeleteListener deleteListener;
    private DecimalFormat df = new DecimalFormat("#,##0.00");

    public interface OnItemDeleteListener {
        void onDelete(int position);
    }

    public ExpenseItemAdapter(OnItemDeleteListener deleteListener) {
        this.items = new ArrayList<>();
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_expense_entry, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ExpenseItem item = items.get(position);
        
        holder.tvCategory.setText(item.getCategory());
        holder.tvAmount.setText("₹" + df.format(item.getAmount()));
        holder.tvDescription.setText(item.getDescription());
        
        // Load bill image if available
        if (item.getBillImageUrl() != null && !item.getBillImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(item.getBillImageUrl())
                    .thumbnail(0.1f)
                    .into(holder.ivBill);
            holder.ivBill.setVisibility(View.VISIBLE);
        } else {
            holder.ivBill.setVisibility(View.GONE);
        }
        
        holder.btnDelete.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onDelete(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void addItem(ExpenseItem item) {
        items.add(item);
        notifyItemInserted(items.size() - 1);
    }

    public void removeItem(int position) {
        items.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, items.size());
    }

    public List<ExpenseItem> getItems() {
        return items;
    }

    public void clear() {
        items.clear();
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView tvCategory, tvAmount, tvDescription;
        ImageView ivBill, btnDelete;
        
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            ivBill = itemView.findViewById(R.id.ivBill);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}