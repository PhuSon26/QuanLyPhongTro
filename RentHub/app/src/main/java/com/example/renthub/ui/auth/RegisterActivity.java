package com.example.renthub.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
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
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private TextView tvRoleTitle;
    private EditText edtFullName, edtEmail, edtPhone, edtPassword, edtConfirmPassword;
    private ImageView btnBack, ivTogglePassword, ivToggleConfirmPassword;
    private Button btnRegister;
    private TextView tvLoginAction;

    private boolean isPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;
    private String currentRole = RoleSelectionActivity.ROLE_LANDLORD;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();
        setupRoleData();
        setupListeners();
    }

    private void initViews() {
        tvRoleTitle = findViewById(R.id.tvRoleTitle);
        btnBack = findViewById(R.id.btnBack);
        edtFullName = findViewById(R.id.edtFullName);
        edtEmail = findViewById(R.id.edtEmail);
        edtPhone = findViewById(R.id.edtPhone);
        edtPassword = findViewById(R.id.edtPassword);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);
        ivTogglePassword = findViewById(R.id.ivTogglePassword);
        ivToggleConfirmPassword = findViewById(R.id.ivToggleConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        tvLoginAction = findViewById(R.id.tvLoginAction);
    }

    private void setupRoleData() {
        if (getIntent() != null && getIntent().hasExtra(RoleSelectionActivity.EXTRA_ROLE)) {
            currentRole = getIntent().getStringExtra(RoleSelectionActivity.EXTRA_ROLE);
        }

        if (RoleSelectionActivity.ROLE_TENANT.equals(currentRole)) {
            tvRoleTitle.setText("Tài khoản Người thuê");
        } else {
            tvRoleTitle.setText("Tài khoản Chủ trọ");
        }
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        ivTogglePassword.setOnClickListener(v -> {
            isPasswordVisible = !isPasswordVisible;
            togglePasswordVisibility(edtPassword, ivTogglePassword, isPasswordVisible);
        });

        ivToggleConfirmPassword.setOnClickListener(v -> {
            isConfirmPasswordVisible = !isConfirmPasswordVisible;
            togglePasswordVisibility(edtConfirmPassword, ivToggleConfirmPassword, isConfirmPasswordVisible);
        });

        btnRegister.setOnClickListener(v -> handleRegister());

        tvLoginAction.setOnClickListener(v -> finish());
    }

    private void togglePasswordVisibility(EditText editText, ImageView toggleView, boolean isVisible) {
        if (isVisible) {
            editText.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            toggleView.setImageResource(R.drawable.ic_visibility_off);
        } else {
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            toggleView.setImageResource(R.drawable.ic_visibility);
        }
        editText.setSelection(editText.getText().length());
    }

    private void handleRegister() {
        String fullName = edtFullName.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String phone = edtPhone.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();
        String confirmPassword = edtConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(fullName)) {
            edtFullName.setError("Vui lòng nhập họ và tên");
            edtFullName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            edtEmail.setError("Email không hợp lệ");
            edtEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(phone)) {
            edtPhone.setError("Vui lòng nhập số điện thoại");
            edtPhone.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password) || password.length() < 6) {
            edtPassword.setError("Mật khẩu phải từ 6 ký tự trở lên");
            edtPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            edtConfirmPassword.setError("Mật khẩu xác nhận không khớp");
            edtConfirmPassword.requestFocus();
            return;
        }

        btnRegister.setEnabled(false);

        // Tạo tài khoản Firebase Auth bằng Email & Mật khẩu
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            saveUserToFirestore(user.getUid(), fullName, email, phone, currentRole);
                        }
                    } else {
                        btnRegister.setEnabled(true);
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Đăng ký thất bại";
                        Toast.makeText(RegisterActivity.this, "Lỗi đăng ký: " + errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserToFirestore(String userId, String fullName, String email, String phone, String role) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("uid", userId);
        userData.put("fullName", fullName);
        userData.put("email", email);
        userData.put("phone", phone);
        userData.put("role", role);
        userData.put("createdAt", FieldValue.serverTimestamp());

        db.collection("users").document(userId)
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(RegisterActivity.this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnRegister.setEnabled(true);
                    Toast.makeText(RegisterActivity.this, "Lỗi lưu thông tin: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}