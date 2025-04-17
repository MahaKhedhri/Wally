package com.example.wallyapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TransactionFragment extends Fragment {

    private RecyclerView recyclerView;
    private TransactionAdapter adapter;
    private List<Transaction> transactionList;
    private DatabaseReference transactionsRef;
    private String currentUserId;
    private RadioGroup radioGroup;

    public TransactionFragment() {
        // Required empty constructor
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_transaction, container, false);

        recyclerView = view.findViewById(R.id.recentTransactionsRecycler);
        radioGroup = view.findViewById(R.id.radioGroupType);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Initialize the global transactionList
        transactionList = new ArrayList<>();
        adapter = new TransactionAdapter(transactionList);
        recyclerView.setAdapter(adapter);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            currentUserId = user.getUid();
            transactionsRef = FirebaseDatabase.getInstance()
                    .getReference("users").child(currentUserId).child("transactions");

            loadTransactions("All");
        } else {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
        }

        // Set the default RadioButton to "All"
        radioGroup.check(R.id.rbAll);  // This will select the "All" RadioButton by default

        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbAll) {
                loadTransactions("All");
            } else if (checkedId == R.id.rbExpense) {
                loadTransactions("Expense");
            } else if (checkedId == R.id.rbIncome) {
                loadTransactions("Income");
            }
        });

        return view;
    }



    private void loadTransactions(String filterType) {
        transactionsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                transactionList.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Transaction transaction = child.getValue(Transaction.class);
                    if (transaction != null) {
                        if (filterType.equals("All") || transaction.getType().equalsIgnoreCase(filterType)) {
                            transactionList.add(transaction);
                        }
                    }
                }
                Collections.reverse(transactionList); // Show latest first
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to load transactions", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
