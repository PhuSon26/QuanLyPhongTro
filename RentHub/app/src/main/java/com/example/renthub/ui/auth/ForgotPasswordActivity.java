package com.example.renthub.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.renthub.R;

public class ForgotPasswordActivity extends AppCompatActivity {

    public static final String EXTRA_ACCOUNT = "EXTRA_ACCOUNT";

    private ImageView btnBack;
    private EditText edtAccount;
    private Button btnSendCode;
    private TextView tvBackToLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        initViews();
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        edtAccount = findViewById(R.id.edtAccount);
        btnSendCode = findViewById(R.id.btnSendCode);
        tvBackToLogin = findViewById(R.id.tvBackToLogin);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        tvBackToLogin.setOnClickListener(v -> finish());

        btnSendCode.setOnClickListener(v -> {
            String account = edtAccount.getText().toString().trim();
            if (TextUtils.isEmpty(account)) {
                edtAccount.setError("Vui lòng nhập email hoặc số điện thoại");
                edtAccount.requestFocus();
                return;
            }

            // Chuyển sang màn hình Nhập mã xác nhận OTP
            Intent intent = new Intent(ForgotPasswordActivity.this, OtpVerificationActivity.class);
            intent.putExtra(EXTRA_ACCOUNT, account);
            startActivity(intent);
        });
    }
}
