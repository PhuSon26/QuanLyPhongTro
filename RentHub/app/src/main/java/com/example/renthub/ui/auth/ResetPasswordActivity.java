package com.example.renthub.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.renthub.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException;
import com.google.firebase.auth.FirebaseUser;

public class ResetPasswordActivity extends AppCompatActivity {

    private ImageView btnBack;
    private EditText edtNewPassword, edtConfirmNewPassword;
    private ImageView ivToggleNewPassword, ivToggleConfirmNewPassword;
    private Button btnChangePassword;

    private boolean isNewPasswordVisible = false;
    private boolean isConfirmNewPasswordVisible = false;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        mAuth = FirebaseAuth.getInstance();

        initViews();
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        edtNewPassword = findViewById(R.id.edtNewPassword);
        edtConfirmNewPassword = findViewById(R.id.edtConfirmNewPassword);
        ivToggleNewPassword = findViewById(R.id.ivToggleNewPassword);
        ivToggleConfirmNewPassword = findViewById(R.id.ivToggleConfirmNewPassword);
        btnChangePassword = findViewById(R.id.btnChangePassword);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        // Toggle ẩn/hiện mật khẩu mới
        ivToggleNewPassword.setOnClickListener(v -> {
            isNewPasswordVisible = !isNewPasswordVisible;
            toggleVisibility(edtNewPassword, ivToggleNewPassword, isNewPasswordVisible);
        });

        // Toggle ẩn/hiện xác nhận mật khẩu
        ivToggleConfirmNewPassword.setOnClickListener(v -> {
            isConfirmNewPasswordVisible = !isConfirmNewPasswordVisible;
            toggleVisibility(edtConfirmNewPassword, ivToggleConfirmNewPassword, isConfirmNewPasswordVisible);
        });

        // Thực hiện đổi mật khẩu
        btnChangePassword.setOnClickListener(v -> handleResetPassword());
    }

    private void handleResetPassword() {
        String newPass = edtNewPassword.getText().toString().trim();
        String confirmPass = edtConfirmNewPassword.getText().toString().trim();

        if (TextUtils.isEmpty(newPass) || newPass.length() < 6) {
            edtNewPassword.setError("Mật khẩu phải có ít nhất 6 ký tự");
            edtNewPassword.requestFocus();
            return;
        }

        if (!newPass.equals(confirmPass)) {
            edtConfirmNewPassword.setError("Mật khẩu xác nhận không khớp");
            edtConfirmNewPassword.requestFocus();
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Phiên xác minh đã hết hạn. Vui lòng yêu cầu lại mã xác minh.", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(ResetPasswordActivity.this, ForgotPasswordActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            return;
        }

        btnChangePassword.setEnabled(false);
        user.updatePassword(newPass)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(ResetPasswordActivity.this, "Đổi mật khẩu thành công! Vui lòng đăng nhập lại.", Toast.LENGTH_LONG).show();
                        mAuth.signOut();
                        Intent intent = new Intent(ResetPasswordActivity.this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        btnChangePassword.setEnabled(true);
                        Exception exception = task.getException();
                        if (exception instanceof FirebaseAuthRecentLoginRequiredException) {
                            Toast.makeText(ResetPasswordActivity.this, "Phiên làm việc quá hạn. Vui lòng xác minh lại OTP.", Toast.LENGTH_LONG).show();
                        } else {
                            String err = exception != null ? exception.getMessage() : "Cập nhật mật khẩu thất bại";
                            Toast.makeText(ResetPasswordActivity.this, "Lỗi: " + err, Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void toggleVisibility(EditText editText, ImageView toggleView, boolean isVisible) {
        // Lưu lại font chữ hiện tại để tránh bị đổi font ngẫu nhiên
        android.graphics.Typeface typeface = editText.getTypeface();

        if (isVisible) {
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            toggleView.setImageResource(R.drawable.ic_visibility_off);
        } else {
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            toggleView.setImageResource(R.drawable.ic_visibility);
        }

        // Khôi phục lại font chữ ban đầu và đưa con trỏ về cuối dòng
        editText.setTypeface(typeface);
        editText.setSelection(editText.getText().length());
    }
}