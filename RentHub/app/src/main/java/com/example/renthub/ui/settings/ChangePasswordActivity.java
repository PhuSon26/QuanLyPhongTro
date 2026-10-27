package com.example.renthub.ui.settings;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.renthub.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ChangePasswordActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private TextInputEditText edtCurrentPassword, edtNewPassword, edtConfirmNewPassword;
    private TextView tvChangePassError;
    private MaterialButton btnSubmitChangePassword;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        mAuth = FirebaseAuth.getInstance();

        initViews();
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        edtCurrentPassword = findViewById(R.id.edtCurrentPassword);
        edtNewPassword = findViewById(R.id.edtNewPassword);
        edtConfirmNewPassword = findViewById(R.id.edtConfirmNewPassword);
        tvChangePassError = findViewById(R.id.tvChangePassError);
        btnSubmitChangePassword = findViewById(R.id.btnSubmitChangePassword);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnSubmitChangePassword.setOnClickListener(v -> handleChangePassword());
    }

    private void handleChangePassword() {
        hideError();

        String currentPass = edtCurrentPassword.getText() != null ? edtCurrentPassword.getText().toString().trim() : "";
        String newPass = edtNewPassword.getText() != null ? edtNewPassword.getText().toString().trim() : "";
        String confirmPass = edtConfirmNewPassword.getText() != null ? edtConfirmNewPassword.getText().toString().trim() : "";

        // 1. Kiểm tra để trống
        if (TextUtils.isEmpty(currentPass)) {
            showError("Vui lòng nhập mật khẩu hiện tại");
            edtCurrentPassword.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(newPass)) {
            showError("Vui lòng nhập mật khẩu mới");
            edtNewPassword.requestFocus();
            return;
        }

        if (newPass.length() < 6) {
            showError("Mật khẩu mới phải có tối thiểu 6 ký tự");
            edtNewPassword.requestFocus();
            return;
        }

        if (newPass.equals(currentPass)) {
            showError("Mật khẩu mới không được trùng với mật khẩu hiện tại");
            edtNewPassword.requestFocus();
            return;
        }

        if (!newPass.equals(confirmPass)) {
            showError("Mật khẩu xác nhận không khớp");
            edtConfirmNewPassword.requestFocus();
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || user.getEmail() == null) {
            showError("Không tìm thấy thông tin đăng nhập của bạn");
            return;
        }

        btnSubmitChangePassword.setEnabled(false);
        btnSubmitChangePassword.setText("Đang xử lý...");

        // 2. Xác thực lại mật khẩu hiện tại
        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPass);
        user.reauthenticate(credential)
                .addOnSuccessListener(aVoid -> {
                    // 3. Tiến hành cập nhật mật khẩu mới
                    user.updatePassword(newPass)
                            .addOnSuccessListener(aVoid1 -> {
                                Toast.makeText(this, "Đổi mật khẩu thành công!", Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                btnSubmitChangePassword.setEnabled(true);
                                btnSubmitChangePassword.setText("Xác nhận đổi mật khẩu");
                                showError("Lỗi cập nhật mật khẩu: " + e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    btnSubmitChangePassword.setEnabled(true);
                    btnSubmitChangePassword.setText("Xác nhận đổi mật khẩu");
                    showError("Mật khẩu hiện tại không chính xác!");
                    edtCurrentPassword.requestFocus();
                });
    }

    private void showError(String msg) {
        tvChangePassError.setText(msg);
        tvChangePassError.setVisibility(View.VISIBLE);
    }

    private void hideError() {
        tvChangePassError.setVisibility(View.GONE);
    }
}
