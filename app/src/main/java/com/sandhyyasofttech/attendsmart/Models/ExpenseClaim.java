package com.sandhyyasofttech.attendsmart.Models;

import java.util.ArrayList;
import java.util.List;

public class ExpenseClaim {
    private String claimId;
    private String userId;
    private String userName;
    private double totalAmount;
    private List<ExpenseItem> items;
    private String status; // pending, approved, rejected
    private String timestamp;
    private String adminRemarks;
    private String approvedBy;
    private String approvedAt;

    public ExpenseClaim() {
        this.items = new ArrayList<>();
    }

    // Getters and Setters
    public String getClaimId() { return claimId; }
    public void setClaimId(String claimId) { this.claimId = claimId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public List<ExpenseItem> getItems() { return items; }
    public void setItems(List<ExpenseItem> items) { 
        this.items = items;
        calculateTotal();
    }
    
    public void addItem(ExpenseItem item) {
        this.items.add(item);
        calculateTotal();
    }
    
    private void calculateTotal() {
        double total = 0;
        for (ExpenseItem item : items) {
            total += item.getAmount();
        }
        this.totalAmount = total;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public String getAdminRemarks() { return adminRemarks; }
    public void setAdminRemarks(String adminRemarks) { this.adminRemarks = adminRemarks; }

    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }

    public String getApprovedAt() { return approvedAt; }
    public void setApprovedAt(String approvedAt) { this.approvedAt = approvedAt; }
}