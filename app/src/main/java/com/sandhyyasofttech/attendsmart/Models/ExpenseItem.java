package com.sandhyyasofttech.attendsmart.Models;

public class ExpenseItem {
    private String itemId;
    private String category; // Travel, Hotel, Food, Other
    private double amount;
    private String description;
    private String billImageUrl;
    
    public ExpenseItem() {}
    
    public ExpenseItem(String category, double amount, String description, String billImageUrl) {
        this.category = category;
        this.amount = amount;
        this.description = description;
        this.billImageUrl = billImageUrl;
    }
    
    // Getters and Setters
    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getBillImageUrl() { return billImageUrl; }
    public void setBillImageUrl(String billImageUrl) { this.billImageUrl = billImageUrl; }
}