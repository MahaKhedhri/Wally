package com.example.wallyapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class HomeFragment extends Fragment {

    private TextView tvCurrentDate, balance, income, expense;
    private RecyclerView recentTransactionsRecycler;
    private FirebaseAuth auth;
    private DatabaseReference dbRef;
    private ArrayList<Transaction> transactionList;
    private TransactionAdapter transactionAdapter;
    private Map<String, String> categoryMap = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize Views
        tvCurrentDate = view.findViewById(R.id.tvCurrentDate);
        balance = view.findViewById(R.id.balance);
        income = view.findViewById(R.id.income);
        expense = view.findViewById(R.id.expense);
        recentTransactionsRecycler = view.findViewById(R.id.recentTransactionsRecycler);

        // Firebase setup
        auth = FirebaseAuth.getInstance();
        dbRef = FirebaseDatabase.getInstance().getReference("users")
                .child(auth.getCurrentUser().getUid());

        // Set current date
        String currentDate = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date());
        tvCurrentDate.setText(currentDate);

        // Setup RecyclerView
        transactionList = new ArrayList<>();
        transactionAdapter = new TransactionAdapter(transactionList);
        recentTransactionsRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        recentTransactionsRecycler.setAdapter(transactionAdapter);

        // Load categories first, then transactions
        loadCategories();

        return view;
    }

    private void loadCategories() {
        dbRef.child("categories").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                categoryMap.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    String id = data.getKey();
                    String name = data.child("name").getValue(String.class);
                    if (id != null && name != null) {
                        categoryMap.put(id, name);
                    }
                }
                loadTransactionData(); // Load transactions after categories are ready
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error
            }
        });
    }

    private void loadTransactionData() {
        dbRef.child("transactions").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                double totalIncome = 0;
                double totalExpense = 0;
                transactionList.clear();

                ArrayList<Transaction> allTransactions = new ArrayList<>();

                for (DataSnapshot data : snapshot.getChildren()) {
                    Transaction transaction = data.getValue(Transaction.class);
                    if (transaction != null) {

                        // Replace category ID with name for display
                        if (transaction.getCategory() != null && categoryMap.containsKey(transaction.getCategory())) {
                            transaction.setDisplayCategoryName(categoryMap.get(transaction.getCategory()));
                        }

                        allTransactions.add(transaction);

                        if ("Income".equals(transaction.getType())) {
                            totalIncome += transaction.getAmount();
                        } else if ("Expense".equals(transaction.getType())) {
                            totalExpense += transaction.getAmount();
                        }
                    }
                }

                // Sort by date descending
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                Collections.sort(allTransactions, (t1, t2) -> {
                    try {
                        Date d1 = sdf.parse(t1.getDate());
                        Date d2 = sdf.parse(t2.getDate());
                        return d2.compareTo(d1); // latest first
                    } catch (Exception e) {
                        return 0;
                    }
                });

                // Add top 3 to display
                for (int i = 0; i < Math.min(3, allTransactions.size()); i++) {
                    transactionList.add(allTransactions.get(i));
                }

                double totalBalance = totalIncome - totalExpense;
                income.setText("Income: $" + totalIncome);
                expense.setText("Expenses: $" + totalExpense);
                balance.setText("$" + totalBalance);

                transactionAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error
            }
        });
    }
}
