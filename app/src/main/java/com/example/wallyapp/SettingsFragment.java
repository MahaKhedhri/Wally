package com.example.wallyapp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SettingsFragment extends Fragment {

    private DatabaseReference database;
    private EditText totalBudgetInput;
    private Button saveTotalBudgetBtn;
    private LinearLayout categoryBudgetsContainer;
    private RadioButton rbExpense, rbIncome;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        totalBudgetInput = view.findViewById(R.id.totalBudgetInput);
        saveTotalBudgetBtn = view.findViewById(R.id.saveTotalBudgetBtn);
        categoryBudgetsContainer = view.findViewById(R.id.categoryBudgetsContainer);

        RadioGroup radioGroup = view.findViewById(R.id.radioGroupFilter);
        rbIncome = view.findViewById(R.id.rbIncome);
        rbExpense = view.findViewById(R.id.rbExpense);

        // Set Expense as default selected
        rbExpense.setChecked(true);

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance().getReference("users").child(mAuth.getUid());

        loadExistingTotalBudget();
        loadCategoryBudgets("Expense"); // Load Expense by default

        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbIncome) {
                loadCategoryBudgets("Income");
            } else if (checkedId == R.id.rbExpense) {
                loadCategoryBudgets("Expense");
            }
        });

        saveTotalBudgetBtn.setOnClickListener(v -> {
            String totalBudgetStr = totalBudgetInput.getText().toString().trim();
            if (!totalBudgetStr.isEmpty()) {
                double totalBudget = Double.parseDouble(totalBudgetStr);
                database.child("budgets").child("totalBudget").setValue(totalBudget);
                Toast.makeText(getContext(), "Total budget saved!", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        checkMonthlyBudgetUsage(); // Only budget check here now
    }

    private void loadExistingTotalBudget() {
        database.child("budgets").child("totalBudget").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Double total = snapshot.getValue(Double.class);
                    totalBudgetInput.setText(String.valueOf(total));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadCategoryBudgets(String typeFilter) {
        database.child("categories").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                categoryBudgetsContainer.removeAllViews();
                boolean foundAny = false;

                for (DataSnapshot categorySnapshot : snapshot.getChildren()) {
                    String categoryId = categorySnapshot.getKey();
                    String categoryName = categorySnapshot.child("name").getValue(String.class);
                    String categoryType = categorySnapshot.child("type").getValue(String.class);

                    if (categoryName != null && categoryType != null && categoryType.equalsIgnoreCase(typeFilter)) {
                        addCategoryBudgetRow(categoryId, categoryName);
                        foundAny = true;
                    }
                }

                if (!foundAny) {
                    TextView emptyMessage = new TextView(getContext());
                    emptyMessage.setText("No categories found for " + typeFilter);
                    emptyMessage.setTextColor(getResources().getColor(android.R.color.darker_gray));
                    emptyMessage.setTextSize(16);
                    categoryBudgetsContainer.addView(emptyMessage);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void addCategoryBudgetRow(String categoryId, String categoryName) {
        View row = LayoutInflater.from(getContext()).inflate(R.layout.item_category_budget, categoryBudgetsContainer, false);

        TextView categoryLabel = row.findViewById(R.id.categoryName);
        EditText budgetInput = row.findViewById(R.id.categoryBudgetInput);
        TextView categoryUsage = row.findViewById(R.id.categoryUsage);
        ProgressBar categoryProgress = row.findViewById(R.id.categoryProgress);
        Button saveBtn = row.findViewById(R.id.saveCategoryBudgetBtn);

        categoryLabel.setText(categoryName);

        database.child("budgets").child("categoryBudgets").child(categoryId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            Double value = snapshot.getValue(Double.class);
                            budgetInput.setText(String.valueOf(value));

                            database.child("transactions")
                                    .addValueEventListener(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot transSnap) {
                                            double used = 0;
                                            for (DataSnapshot t : transSnap.getChildren()) {
                                                String tCategory = t.child("category").getValue(String.class);
                                                String date = t.child("date").getValue(String.class);
                                                if (tCategory != null && tCategory.equals(categoryId) && date != null && isInCurrentMonth(date)) {
                                                    Double amount = t.child("amount").getValue(Double.class);
                                                    used += (amount != null) ? amount : 0;
                                                }
                                            }

                                            int progress = (int) ((used / value) * 100);
                                            categoryUsage.setText("Used: " + used + " tnd / " + value + " tnd");
                                            categoryProgress.setProgress(Math.min(progress, 100));
                                        }

                                        @Override public void onCancelled(@NonNull DatabaseError error) {}
                                    });
                        }
                    }

                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });

        saveBtn.setOnClickListener(v -> {
            String input = budgetInput.getText().toString().trim();
            if (!input.isEmpty()) {
                double value = Double.parseDouble(input);

                calculateTotalCategoryBudgets(total -> {
                    double totalBudget = Double.parseDouble(totalBudgetInput.getText().toString().trim());

                    if (total > totalBudget) {
                        Toast.makeText(getContext(), "Total category budgets exceed total budget!", Toast.LENGTH_SHORT).show();
                    } else {
                        database.child("budgets").child("categoryBudgets").child(categoryId).setValue(value);
                        Toast.makeText(getContext(), categoryName + " budget saved!", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        categoryBudgetsContainer.addView(row);
    }

    private void calculateTotalCategoryBudgets(TotalCallback callback) {
        database.child("budgets").child("categoryBudgets").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                double total = 0;
                for (DataSnapshot snap : snapshot.getChildren()) {
                    Double val = snap.getValue(Double.class);
                    total += (val != null) ? val : 0;
                }
                callback.onTotalReady(total);
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void checkMonthlyBudgetUsage() {
        database.child("budgets").child("totalBudget").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Double totalBudget = snapshot.getValue(Double.class);
                    if (totalBudget != null && totalBudget > 0) {
                        calculateMonthlySpending(totalSpent -> {
                            double usagePercent = (totalSpent / totalBudget) * 100;
                            Log.d("SettingsFragment", "Usage percent: " + usagePercent); // Log usage percentage
                            if (usagePercent >= 85) {
                                showBudgetWarningNotification(totalSpent, totalBudget);
                            }
                        });
                    }
                }
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void calculateMonthlySpending(TotalCallback callback) {
        database.child("transactions").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                double totalSpent = 0;
                for (DataSnapshot t : snapshot.getChildren()) {
                    String date = t.child("date").getValue(String.class);
                    String type = t.child("type").getValue(String.class);
                    Double amount = t.child("amount").getValue(Double.class);

                    if (date != null && isInCurrentMonth(date) && "Expense".equalsIgnoreCase(type)) {
                        totalSpent += (amount != null) ? amount : 0;
                    }
                }
                callback.onTotalReady(totalSpent);
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showBudgetWarningNotification(double spent, double budget) {
        String channelId = "budget_channel";
        String channelName = "Budget Alerts";

        createNotificationChannel(); // Ensure the channel is created

        NotificationManager manager = (NotificationManager) requireContext().getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(requireContext(), channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Budget Alert")
                .setContentText("You’ve used " + (int)((spent / budget) * 100) + "% of your monthly budget.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        manager.notify(1, builder.build());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Budget Alerts";
            String description = "Alerts for budget usage";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel("budget_channel", name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = requireContext().getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private String getCurrentMonthYear() {
        return new SimpleDateFormat("MM-yyyy", Locale.getDefault()).format(new Date());
    }

    private boolean isInCurrentMonth(String dateStr) {
        try {
            Date date = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(dateStr);
            String transMonth = new SimpleDateFormat("MM-yyyy", Locale.getDefault()).format(date);
            return transMonth.equals(getCurrentMonthYear());
        } catch (Exception e) {
            return false;
        }
    }

    interface TotalCallback {
        void onTotalReady(double total);
    }
}
