package com.example.wallyapp;

public class Transaction {
    private String note;
    private String id;
    private String title;
    private String date;
    private double amount;
    private String category;
    private String displayCategoryName;
    private String type; // 👈 Add this

    public Transaction() {
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDisplayCategoryName() {
        return displayCategoryName;
    }

    public void setDisplayCategoryName(String displayCategoryName) {
        this.displayCategoryName = displayCategoryName;
    }

    // ✅ Add this getter/setter for `type`
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getNote() {
        return note != null ? note : "";
    }
}
