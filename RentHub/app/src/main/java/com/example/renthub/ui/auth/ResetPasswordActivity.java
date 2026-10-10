package com.example.renthub.ui.auth;

import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.renthub.R;

public class ResetPasswordActivity extends AppCompatActivity {

    private ImageView btnBack;
    private EditText edtNewPassword, edtConfirmNewPassword;
    private ImageView ivToggleNewPassword, ivToggleConfirmNewPassword;
    private Button btnChangePassword;

    private boolean isNewPasswordVisible = false;
    private boolean isConfirmNewPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

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

        // Toggle ẩn/hiện xác nhận mật khẩu mới
        ivToggleConfirmNewPassword.setOnClickListener(v -> {
            isConfirmNewPasswordVisible = !isConfirmNewPasswordVisible;
            toggleVisibility(edtConfirmNewPassword, ivToggleConfirmNewPassword, isConfirmNewPasswordVisible);
        });

        // Đổi mật khẩu
        btnChangePassword.setOnClickListener(v -> {
            String newPass = edtNewPassword.getText().toString().trim();
            String confirmPass = edtConfirmNewPassword.getText().toString().trim();

            if (TextUtils.isEmpty(newPass) || newPass.length() < 8) {
                edtNewPassword.setError("Mật khẩu phải có ít nhất 8 ký tự");
                edtNewPassword.requestFocus();
                return;
            }

            if (!newPass.equals(confirmPass)) {
                edtConfirmNewPassword.setError("Mật khẩu xác nhận không khớp");
                edtConfirmNewPassword.requestFocus();
                return;
            }

            Toast.makeText(this, "Đổi mật khẩu thành công! Vui lòng đăng nhập lại.", Toast.LENGTH_LONG).show();
            // Đóng màn hình quay về
            finish();
        });
    }

    private void toggleVisibility(EditText editText, ImageView toggleView, boolean isVisible) {
        if (isVisible) {
            editText.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            toggleView.setImageResource(R.drawable.ic_visibility_off);
        } else {
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            toggleView.setImageResource(R.drawable.ic_visibility);
        }
        editText.setSelection(editText.getText().length());
    }
}
