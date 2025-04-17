package com.example.wallyapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {

    private List<Transaction> transactionList;

    public TransactionAdapter(List<Transaction> transactionList) {
        this.transactionList = transactionList;
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transaction, parent, false); // Use the XML file you provided
        return new TransactionViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        Transaction transaction = transactionList.get(position);
        holder.transactionTitle.setText(transaction.getTitle());
        holder.transactionDate.setText(transaction.getDate());
        holder.transactionAmount.setText( transaction.getAmount()+ " tnd" );
        holder.transactionCategory.setText(transaction.getDisplayCategoryName());
    }

    @Override
    public int getItemCount() {
        return transactionList.size();
    }

    public static class TransactionViewHolder extends RecyclerView.ViewHolder {
        public TextView transactionTitle, transactionDate, transactionAmount, transactionCategory;

        public TransactionViewHolder(View view) {
            super(view);
            transactionTitle = view.findViewById(R.id.transactionTitle);
            transactionDate = view.findViewById(R.id.transactionDate);
            transactionAmount = view.findViewById(R.id.transactionAmount);
            transactionCategory = view.findViewById(R.id.transactioncategory);

        }
    }
}
