package com.example.renthub.ui.auth;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.renthub.R;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    private ImageView btnBack;
    private EditText edtAccount;
    private Button btnSendCode;
    private TextView tvBackToLogin;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        mAuth = FirebaseAuth.getInstance();

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
            String email = edtAccount.getText().toString().trim();

            if (TextUtils.isEmpty(email)) {
                edtAccount.setError("Vui lòng nhập email của bạn");
                edtAccount.requestFocus();
                return;
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                edtAccount.setError("Email không hợp lệ (VD: example@gmail.com)");
                edtAccount.requestFocus();
                return;
            }

            sendEmailReset(email);
        });
    }

    private void sendEmailReset(String email) {
        btnSendCode.setEnabled(false);
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    btnSendCode.setEnabled(true);
                    if (task.isSuccessful()) {
                        Toast.makeText(ForgotPasswordActivity.this,
                                "Đã gửi liên kết đặt lại mật khẩu! Vui lòng kiểm tra hộp thư Gmail của bạn.",
                                Toast.LENGTH_LONG).show();
                        finish();
                    } else {
                        String err = task.getException() != null ? task.getException().getMessage() : "Gửi email thất bại";
                        Toast.makeText(ForgotPasswordActivity.this, "Lỗi: " + err, Toast.LENGTH_LONG).show();
                    }
                });
    }
}