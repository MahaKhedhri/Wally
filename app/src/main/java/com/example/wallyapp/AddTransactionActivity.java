package com.example.wallyapp;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Calendar;

public class AddTransactionActivity extends AppCompatActivity {

    private RadioGroup radioGroupType;
    private RadioButton rbExpense, rbIncome;
    private EditText etAmount, etTitle, etDate, etNote;
    private GridLayout categoryGrid;
    private ImageButton checkButton;
    private ImageButton backButton;
    private String selectedCategoryId;
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference transactionsRef;

    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_transaction);

        // Initialize views
        radioGroupType = findViewById(R.id.radioGroupType);
        rbExpense = findViewById(R.id.rbExpense);
        rbIncome = findViewById(R.id.rbIncome);
        etAmount = findViewById(R.id.etAmount);
        etTitle = findViewById(R.id.etTitle);
        etDate = findViewById(R.id.etDate);
        etNote = findViewById(R.id.etNote);
        categoryGrid = findViewById(R.id.categoryGrid);
        checkButton = findViewById(R.id.CheckButton);
        backButton = findViewById(R.id.backButton);

        firebaseDatabase = FirebaseDatabase.getInstance();
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        transactionsRef = firebaseDatabase.getReference("users").child(userId).child("transactions");

        rbExpense.setChecked(true);  // Default to Expense type

        addCategoryButtons();

        etDate.setOnClickListener(v -> openDatePicker());

        checkButton.setOnClickListener(v -> addTransaction());
        backButton.setOnClickListener(v -> finish());

        radioGroupType.setOnCheckedChangeListener((group, checkedId) -> addCategoryButtons());
    }

    // Load categories based on Expense or Income selection
    private void addCategoryButtons() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference categoriesRef = FirebaseDatabase.getInstance().getReference("users").child(userId).child("categories");
        String type = rbExpense.isChecked() ? "Expense" : "Income";

        categoriesRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                categoryGrid.removeAllViews();

                for (DataSnapshot categorySnapshot : task.getResult().getChildren()) {
                    String categoryId = categorySnapshot.getKey();
                    String categoryName = categorySnapshot.child("name").getValue(String.class);
                    String categoryType = categorySnapshot.child("type").getValue(String.class);

                    if (categoryType != null && categoryType.equals(type)) {
                        Button categoryButton = new Button(this);
                        categoryButton.setText(categoryName);
                        categoryButton.setBackgroundResource(R.drawable.category_button);
                        categoryButton.setOnClickListener(v -> selectCategory(categoryId));

                        categoryGrid.addView(categoryButton);
                    }
                }
            } else {
                Toast.makeText(this, "Failed to load categories", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Store the selected category
    private void selectCategory(String categoryId) {
        selectedCategoryId = categoryId;
        Toast.makeText(this, "Category selected", Toast.LENGTH_SHORT).show();
    }

    // Show date picker for selecting the transaction date
    private void openDatePicker() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                AddTransactionActivity.this,
                (view, year1, month1, dayOfMonth) -> {
                    String date = dayOfMonth + "/" + (month1 + 1) + "/" + year1;
                    etDate.setText(date);
                },
                year, month, day
        );
        datePickerDialog.show();
    }

    // Add transaction after checking the budget
    private void addTransaction() {
        String amountStr = etAmount.getText().toString().trim();
        String title = etTitle.getText().toString().trim();
        String date = etDate.getText().toString().trim();
        String note = etNote.getText().toString().trim();

        if (amountStr.isEmpty() || title.isEmpty() || selectedCategoryId == null || date.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = 0.0;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check the budget before adding the transaction
        checkCategoryBudget(selectedCategoryId, amount, () -> {
            String type = rbExpense.isChecked() ? "Expense" : "Income";
            addTransactionToDatabase(amountStr, title, date, note, type);
        });
    }

    // Check if the transaction amount exceeds the category budget
    private void checkCategoryBudget(String categoryId, double amount, Runnable onSuccess) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference categoryBudgetRef = firebaseDatabase.getReference("users")
                .child(userId).child("budgets").child("categoryBudgets").child(categoryId);

        categoryBudgetRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Double budget = task.getResult().getValue(Double.class);
                if (budget != null) {
                    double remainingBudget = budget - amount;
                    if (remainingBudget >= 0) {
                        // Proceed with adding the transaction
                        onSuccess.run();
                    } else {
                        Toast.makeText(AddTransactionActivity.this, "Insufficient budget for this category!", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(AddTransactionActivity.this, "No budget set for this category", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(AddTransactionActivity.this, "Failed to check category budget", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Add transaction to the database
    private void addTransactionToDatabase(String amountStr, String title, String date, String note, String type) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference transactionsRef = firebaseDatabase.getReference("users").child(userId).child("transactions");

        String transactionId = transactionsRef.push().getKey();
        if (transactionId != null) {
            Transaction transaction = new Transaction(transactionId, title, Double.parseDouble(amountStr), selectedCategoryId, date, note, type);

            transactionsRef.child(transactionId).setValue(transaction)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Transaction added!", Toast.LENGTH_SHORT).show();
                            finish(); // Close the activity
                        } else {
                            Toast.makeText(this, "Failed to add transaction", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    public static class Transaction {
        public String transactionId;
        public String title;
        public double amount;
        public String category;
        public String date;
        public String note;
        public String type;

        public Transaction() {}

        public Transaction(String transactionId, String title, double amount, String category, String date, String note, String type) {
            this.transactionId = transactionId;
            this.title = title;
            this.amount = amount;
            this.category = category;
            this.date = date;
            this.note = note;
            this.type = type;
        }
    }
}
