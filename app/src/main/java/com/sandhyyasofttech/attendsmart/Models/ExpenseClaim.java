package com.sandhyyasofttech.attendsmart.Models;

public class ExpenseClaim {
    private String claimId;
    private String userId;
    private String userName;
    private double amount;
    private String description;
    private String imageUrl;
    private String status;
    private String timestamp;
    private String adminRemarks;
    private String approvedBy;
    private String approvedAt;

    public ExpenseClaim() {
        // Default constructor required for Firebase
    }

    public ExpenseClaim(String claimId, String userId, String userName, double amount, 
                        String description, String imageUrl, String status, String timestamp) {
        this.claimId = claimId;
        this.userId = userId;
        this.userName = userName;
        this.amount = amount;
        this.description = description;
        this.imageUrl = imageUrl;
        this.status = status;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public String getClaimId() { return claimId; }
    public void setClaimId(String claimId) { this.claimId = claimId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

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