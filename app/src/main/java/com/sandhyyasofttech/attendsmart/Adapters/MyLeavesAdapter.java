package com.sandhyyasofttech.attendsmart.Adapters;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sandhyyasofttech.attendsmart.Models.LeaveModel;
import com.sandhyyasofttech.attendsmart.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MyLeavesAdapter extends RecyclerView.Adapter<MyLeavesAdapter.VH> {

    public interface OnItemClickListener {
        void onItemClick(LeaveModel leave);
    }

    private final List<LeaveModel> list;
    private OnItemClickListener listener;

    public MyLeavesAdapter(List<LeaveModel> list) {
        this.list = list;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_leave, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        LeaveModel m = list.get(position);

        // Accent bar colour by status
        h.viewAccent.setBackgroundColor(accentColor(m.status));

        // Date range
        h.tvDateRange.setText(formatDateRange(m.fromDate, m.toDate));

        // Duration
        h.tvDuration.setText(calcDuration(m.fromDate, m.toDate, m.leaveType));

        // Leave type
        String typeText = formatLeaveType(m.leaveType);
        if (m.halfDayType != null && !m.halfDayType.isEmpty()) {
            typeText += " · " + capitalizeWords(m.halfDayType.replace("_", " "));
        }
        h.tvType.setText(typeText);

        // Status badge
        h.tvStatus.setText(m.status != null ? capitalizeWords(m.status) : "Pending");
        applyStatusStyle(h.tvStatus, m.status);

        // Paid badge
        h.chipPaid.setVisibility(m.isPaid != null && m.isPaid ? View.VISIBLE : View.GONE);

        // Applied date
        String appliedStr = m.getAppliedDate();
        if (appliedStr != null && !appliedStr.isEmpty()) {
            h.tvAppliedDate.setVisibility(View.VISIBLE);
            h.tvAppliedDate.setText("Applied on " + formatDisplayDate(appliedStr));
        } else {
            h.tvAppliedDate.setVisibility(View.GONE);
        }

        // Rejection reason
        boolean showReason = "REJECTED".equalsIgnoreCase(m.status)
                && m.adminReason != null && !m.adminReason.isEmpty();
        if (showReason) {
            h.layoutReason.setVisibility(View.VISIBLE);
            h.tvReason.setVisibility(View.VISIBLE);
            h.tvReason.setText(m.adminReason);
        } else {
            h.layoutReason.setVisibility(View.GONE);
            h.tvReason.setVisibility(View.GONE);
        }

        // Item click
        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(m);
        });
    }

    @Override
    public int getItemCount() { return list.size(); }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private int accentColor(String status) {
        if (status == null) return Color.parseColor("#EF6C00");
        switch (status.toUpperCase()) {
            case "APPROVED":  return Color.parseColor("#2E7D32");
            case "REJECTED":  return Color.parseColor("#C62828");
            case "PENDING":   return Color.parseColor("#EF6C00");
            case "CANCELLED": return Color.parseColor("#9E9E9E");
            default:          return Color.parseColor("#1565C0");
        }
    }

    private void applyStatusStyle(TextView tv, String status) {
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(24f);
        if (status == null) status = "PENDING";
        switch (status.toUpperCase()) {
            case "APPROVED":
                bg.setColor(Color.parseColor("#E8F5E9")); tv.setTextColor(Color.parseColor("#2E7D32")); break;
            case "REJECTED":
                bg.setColor(Color.parseColor("#FFEBEE")); tv.setTextColor(Color.parseColor("#C62828")); break;
            case "PENDING":
                bg.setColor(Color.parseColor("#FFF3E0")); tv.setTextColor(Color.parseColor("#EF6C00")); break;
            case "CANCELLED":
                bg.setColor(Color.parseColor("#F5F5F5")); tv.setTextColor(Color.parseColor("#616161")); break;
            default:
                bg.setColor(Color.parseColor("#E3F2FD")); tv.setTextColor(Color.parseColor("#1565C0")); break;
        }
        tv.setBackground(bg);
    }

    private String formatDateRange(String from, String to) {
        try {
            SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat dm = new SimpleDateFormat("dd MMM", Locale.getDefault());
            SimpleDateFormat yr = new SimpleDateFormat("yyyy", Locale.getDefault());
            Date d1 = in.parse(from), d2 = in.parse(to);
            if (d1 != null && d2 != null) {
                if (from.equals(to)) return dm.format(d1) + " " + yr.format(d1);
                return dm.format(d1) + " – " + dm.format(d2) + " " + yr.format(d2);
            }
        } catch (ParseException e) { e.printStackTrace(); }
        return from + " → " + to;
    }

    private String calcDuration(String from, String to, String leaveType) {
        if (leaveType != null && leaveType.toUpperCase().contains("HALF")) return "0.5 day";
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date d1 = sdf.parse(from), d2 = sdf.parse(to);
            if (d1 != null && d2 != null) {
                long days = TimeUnit.MILLISECONDS.toDays(d2.getTime() - d1.getTime()) + 1;
                return days + (days == 1 ? " day" : " days");
            }
        } catch (ParseException e) { e.printStackTrace(); }
        return "";
    }

    private String formatLeaveType(String type) {
        if (type == null) return "";
        return capitalizeWords(type.replace("_", " "));
    }

    private String formatDisplayDate(String dateStr) {
        try {
            SimpleDateFormat in  = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat out = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            Date d = in.parse(dateStr);
            if (d != null) return out.format(d);
        } catch (ParseException e) { e.printStackTrace(); }
        return dateStr;
    }

    private String capitalizeWords(String text) {
        if (text == null || text.isEmpty()) return text;
        String[] words = text.toLowerCase(Locale.getDefault()).split(" ");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!w.isEmpty()) sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }

    // ── ViewHolder ────────────────────────────────────────────────────────────

    static class VH extends RecyclerView.ViewHolder {
        View         viewAccent;
        TextView     tvDateRange, tvDuration, tvType, tvStatus, tvReason, chipPaid, tvAppliedDate;
        LinearLayout layoutReason;

        VH(View v) {
            super(v);
            viewAccent    = v.findViewById(R.id.viewAccent);
            tvDateRange   = v.findViewById(R.id.tvDateRange);
            tvDuration    = v.findViewById(R.id.tvDuration);
            tvType        = v.findViewById(R.id.tvType);
            tvStatus      = v.findViewById(R.id.tvStatus);
            tvReason      = v.findViewById(R.id.tvReason);
            chipPaid      = v.findViewById(R.id.chipPaid);
            tvAppliedDate = v.findViewById(R.id.tvAppliedDate);
            layoutReason  = v.findViewById(R.id.layoutReason);
        }
    }
}