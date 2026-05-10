package com.example.myapp;

import android.os.Bundle;
import androidx.fragment.app.FragmentActivity;

public class SearchActivity extends FragmentActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.search_fragment, new SearchFragment())
                .commitAllowingStateLoss();
        }
    }
}
