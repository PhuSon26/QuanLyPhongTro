package com.example.renthub.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.renthub.R;
import com.example.renthub.ui.home.LandlordHomeActivity;
import com.example.renthub.ui.home.TenantHomeActivity;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private EditText edtEmail;
    private EditText edtPassword;
    private MaterialButton btnLogin;
    private TextView tvErrorMessage;
    private TextView tvForgotPassword;
    private TextView tvSignUp;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Kiểm tra Auto-login nếu đã đăng nhập trước đó
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            setLoading(true);
            checkRoleAndNavigate(currentUser.getUid());
            return;
        }

        initViews();
        setupListeners();
    }

    private void initViews() {
        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvErrorMessage = findViewById(R.id.tvErrorMessage);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvSignUp = findViewById(R.id.tvSignUp);
    }

    private void setupListeners() {
        // Chuyển sang chọn vai trò để đăng ký
        tvSignUp.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RoleSelectionActivity.class);
            startActivity(intent);
        });

        // Chuyển sang màn hình quên mật khẩu Gmail
        tvForgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });

        // Ẩn thông báo lỗi khi bắt đầu nhập lại thông tin
        TextWatcher clearErrorWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (tvErrorMessage.getVisibility() == View.VISIBLE) {
                    tvErrorMessage.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };
        edtEmail.addTextChangedListener(clearErrorWatcher);
        edtPassword.addTextChangedListener(clearErrorWatcher);

        btnLogin.setOnClickListener(v -> handleLogin());
    }

    private void handleLogin() {
        String account = edtEmail.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();

        if (TextUtils.isEmpty(account) || TextUtils.isEmpty(password)) {
            showLoginError();
            return;
        }

        setLoading(true);

        if (account.contains("@")) {
            loginWithEmail(account, password);
        } else {
            loginWithPhone(account, password);
        }
    }

    private void loginWithEmail(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful() && mAuth.getCurrentUser() != null) {
                        checkRoleAndNavigate(mAuth.getCurrentUser().getUid());
                    } else {
                        showLoginError();
                    }
                });
    }

    private void loginWithPhone(String phone, String password) {
        db.collection("users")
                .whereEqualTo("phone", phone)
                .limit(1)
                .get()
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                        DocumentSnapshot doc = task.getResult().getDocuments().get(0);
                        String email = doc.getString("email");
                        if (email != null && !email.isEmpty()) {
                            loginWithEmail(email, password);
                        } else {
                            showLoginError();
                        }
                    } else {
                        showLoginError();
                    }
                });
    }

    private void checkRoleAndNavigate(String uid) {
        db.collection("users").document(uid).get()
                .addOnCompleteListener(this, task -> {
                    setLoading(false);
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                        DocumentSnapshot snapshot = task.getResult();
                        String role = snapshot.getString("role");
                        String fullName = snapshot.getString("fullName");
                        String email = snapshot.getString("email");
                        String phone = snapshot.getString("phone");

                        Intent intent;
                        // Kiểm tra vai trò Chủ trọ -> Mở LandlordHomeActivity
                        if (role != null && (role.equalsIgnoreCase("LANDLORD")
                                || role.equalsIgnoreCase("CHỦ TRỌ")
                                || role.toLowerCase().contains("chủ"))) {
                            intent = new Intent(LoginActivity.this, LandlordHomeActivity.class);
                            intent.putExtra("ROLE", "LANDLORD");
                        } else {
                            intent = new Intent(LoginActivity.this, TenantHomeActivity.class);
                            intent.putExtra("ROLE", "TENANT");
                        }

                        intent.putExtra("FULL_NAME", fullName);
                        intent.putExtra("EMAIL", email);
                        intent.putExtra("PHONE", phone);

                        // Xóa stack màn hình Login để nhấn nút Back không bị quay lại
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        showLoginError();
                    }
                });
    }

    private void showLoginError() {
        setLoading(false);
        if (tvErrorMessage != null) {
            tvErrorMessage.setText("Thông tin đăng nhập không chính xác");
            tvErrorMessage.setVisibility(View.VISIBLE);
        }
    }

    private void setLoading(boolean isLoading) {
        if (btnLogin != null) {
            btnLogin.setEnabled(!isLoading);
            if (isLoading) {
                btnLogin.setText("Đang đăng nhập...");
            } else {
                btnLogin.setText("Đăng nhập");
            }
        }
    }
}