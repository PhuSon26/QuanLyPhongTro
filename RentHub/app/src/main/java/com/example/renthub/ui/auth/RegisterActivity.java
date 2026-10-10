package com.example.renthub.ui.auth;

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

public class RegisterActivity extends AppCompatActivity {

    private TextView tvRoleTitle;
    private EditText edtFullName, edtEmail, edtPhone, edtPassword, edtConfirmPassword;
    private ImageView btnBack, ivTogglePassword, ivToggleConfirmPassword;
    private Button btnRegister;
    private TextView tvLoginAction;

    private boolean isPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;
    private String currentRole = RoleSelectionActivity.ROLE_LANDLORD;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

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

        // Cập nhật tiêu đề phù hợp với vai trò được chọn
        if (RoleSelectionActivity.ROLE_TENANT.equals(currentRole)) {
            tvRoleTitle.setText("Tài khoản Người thuê");
        } else {
            tvRoleTitle.setText("Tài khoản Chủ trọ");
        }
    }

    private void setupListeners() {
        // Nút quay lại
        btnBack.setOnClickListener(v -> finish());

        // Bật/tắt ẩn hiện mật khẩu
        ivTogglePassword.setOnClickListener(v -> {
            isPasswordVisible = !isPasswordVisible;
            togglePasswordVisibility(edtPassword, ivTogglePassword, isPasswordVisible);
        });

        // Bật/tắt ẩn hiện xác nhận mật khẩu
        ivToggleConfirmPassword.setOnClickListener(v -> {
            isConfirmPasswordVisible = !isConfirmPasswordVisible;
            togglePasswordVisibility(edtConfirmPassword, ivToggleConfirmPassword, isConfirmPasswordVisible);
        });

        // Nút đăng ký
        btnRegister.setOnClickListener(v -> handleRegister());

        // Nút chuyển sang Đăng nhập
        tvLoginAction.setOnClickListener(v -> {
            Toast.makeText(this, "Chuyển tới màn hình Đăng nhập", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void togglePasswordVisibility(EditText editText, ImageView toggleView, boolean isVisible) {
        if (isVisible) {
            editText.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            toggleView.setImageResource(R.drawable.ic_visibility_off);
        } else {
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            toggleView.setImageResource(R.drawable.ic_visibility);
        }
        // Giữ con trỏ chuột ở cuối dòng
        editText.setSelection(editText.getText().length());
    }

    private void handleRegister() {
        String fullName = edtFullName.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String phone = edtPhone.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();
        String confirmPassword = edtConfirmPassword.getText().toString().trim();

        // Kiểm tra hợp lệ dữ liệu nhập
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

        // Đã nhập đúng thông tin
        String roleText = RoleSelectionActivity.ROLE_TENANT.equals(currentRole) ? "Người thuê" : "Chủ trọ";
        Toast.makeText(this, "Đăng ký thành công tài khoản " + roleText + "!", Toast.LENGTH_LONG).show();
        
        // TODO: Kết nối Firebase Auth & Firestore tại đây
    }
}
