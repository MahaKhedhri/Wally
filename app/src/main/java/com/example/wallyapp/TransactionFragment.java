package com.example.wallyapp;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.util.*;

public class TransactionFragment extends Fragment {

    private RecyclerView recyclerView;
    private TransactionAdapter adapter;
    private List<Transaction> transactionList;

    private EditText searchInput;
    private Spinner filterSpinner;

    private DatabaseReference transactionsRef;
    private String currentUserId;

    private String currentFilter = "All";
    private String currentSearch = "";

    public TransactionFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_transaction, container, false);

        searchInput = view.findViewById(R.id.searchInput);
        filterSpinner = view.findViewById(R.id.filterSpinner);
        recyclerView = view.findViewById(R.id.recentTransactionsRecycler);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        transactionList = new ArrayList<>();
        adapter = new TransactionAdapter(transactionList);
        recyclerView.setAdapter(adapter);

        // Firebase of the user that signed in (save id ,transactions path,set filter,load transaction)
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            currentUserId = user.getUid();
            transactionsRef = FirebaseDatabase.getInstance()
                    .getReference("users").child(currentUserId).child("transactions");
            setupSpinner();
            setupSearchInput();
            loadTransactions();
        } else {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
        }

        return view;
    }

    private void setupSpinner() {
        String[] filterOptions = {"All", "Income", "Expense"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, filterOptions);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        filterSpinner.setAdapter(spinnerAdapter);

        filterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentFilter = filterOptions[position];
                loadTransactions();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupSearchInput() {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearch = s.toString().toLowerCase();
                loadTransactions();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void loadTransactions() {
        transactionsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                transactionList.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Transaction transaction = child.getValue(Transaction.class);
                    if (transaction != null) {
                        boolean matchesType = currentFilter.equals("All") || transaction.getType().equalsIgnoreCase(currentFilter);
                        boolean matchesSearch = transaction.getTitle().toLowerCase().contains(currentSearch) ||
                                transaction.getNote().toLowerCase().contains(currentSearch);
                        if (matchesType && matchesSearch) {
                            transactionList.add(transaction);
                        }
                    }
                }
                Collections.reverse(transactionList); // Show newest first
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to load transactions", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
