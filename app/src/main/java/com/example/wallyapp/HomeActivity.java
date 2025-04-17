package com.example.wallyapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import com.google.android.material.navigation.NavigationBarView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationBarView;

public class HomeActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private FloatingActionButton fab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);  // Make sure this is your layout file name

        // Initialize views
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        fab = findViewById(R.id.fab);

        // Make background transparent to allow FAB cradle effect
        bottomNavigationView.setBackground(null);

        // Default fragment
        loadFragment(new HomeFragment());

        // Bottom nav listener using the new method
        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment selectedFragment = null;

                if (item.getItemId() == R.id.nav_home) {
                    selectedFragment = new HomeFragment();
                } else if (item.getItemId() == R.id.nav_transactions) {
                    selectedFragment = new TransactionFragment();
                } else if (item.getItemId() == R.id.nav_categories) {
                    selectedFragment = new CategoriesFragment();
                } else if (item.getItemId() == R.id.nav_reports) {
                    selectedFragment = new SettingsFragment();
                }

                return loadFragment(selectedFragment);
            }
        });

        // FAB click: open add transaction
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, AddTransactionActivity.class);
            startActivity(intent);
        });
    }

    private boolean loadFragment(Fragment fragment) {
        // Check if the fragment is already the current one
        if (fragment != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.frame_layout, fragment)
                    .commit();
            return true;
        }
        return false;
    }
}
