package com.example.navigationbarstarter.ui.access;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.navigationbarstarter.R;

public class AccessActivity extends AppCompatActivity {

    private Button btnLogin, btnRegistration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_access);

        initialize();
        setUpListeners();
    }

    private void initialize() {
        btnLogin = findViewById(R.id.btnLoginAccess);
        btnRegistration = findViewById(R.id.btnRegistrationAccess);
    }

    private void setUpListeners() {
        btnLogin.setOnClickListener((v -> {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
        }));

        btnRegistration.setOnClickListener((v -> {
            Intent intent = new Intent(this, RegisterActivity.class);
            startActivity(intent);
        }));

    }


}
