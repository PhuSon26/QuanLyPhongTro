package com.example.renthub.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.renthub.R;
import com.example.renthub.ui.home.LandlordHomeActivity;
import com.example.renthub.ui.home.TenantHomeActivity;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
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

        initViews();
        setupListeners();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // TỰ ĐỘNG ĐĂNG NHẬP: Nếu đã có tài khoản đăng nhập trước đó thì bỏ qua màn hình đăng nhập
        if (mAuth.getCurrentUser() != null) {
            setLoading(true);
            checkRoleAndNavigate(mAuth.getCurrentUser().getUid());
        }
    }

    private void initViews() {
        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvErrorMessage = findViewById(R.id.tvErrorMessage);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvSignUp = findViewById(R.id.tvSignUp);

        // Tự động điền email/tài khoản lần đăng nhập gần nhất
        android.content.SharedPreferences prefs = getSharedPreferences("RentHubPrefs", MODE_PRIVATE);
        String savedEmail = prefs.getString("SAVED_ACCOUNT", "");
        if (!TextUtils.isEmpty(savedEmail) && edtEmail.getText().toString().isEmpty()) {
            edtEmail.setText(savedEmail);
        }
    }

    private void setupListeners() {
        // Chuyển sang màn hình chọn vai trò để đăng ký
        tvSignUp.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RoleSelectionActivity.class);
            startActivity(intent);
        });

        // Chuyển sang màn hình quên mật khẩu
        tvForgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });

        // Tự động ẩn thông báo lỗi khi người dùng nhập lại thông tin
        TextWatcher clearErrorWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (tvErrorMessage.getVisibility() == View.VISIBLE) {
                    tvErrorMessage.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };
        edtEmail.addTextChangedListener(clearErrorWatcher);
        edtPassword.addTextChangedListener(clearErrorWatcher);

        // Xử lý sự kiện bấm Đăng nhập
        btnLogin.setOnClickListener(v -> handleLogin());
    }

    private void handleLogin() {
        String account = edtEmail.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();

        // 1. Kiểm tra rỗng
        if (TextUtils.isEmpty(account) || TextUtils.isEmpty(password)) {
            showLoginError();
            return;
        }

        setLoading(true);

        // 2. Kiểm tra nếu nhập email thì xác thực trực tiếp
        if (account.contains("@")) {
            loginWithEmail(account, password);
        } else {
            // Nếu người dùng nhập số điện thoại -> tìm email tương ứng trong Firestore
            loginWithPhone(account, password);
        }
    }

    private void loginWithEmail(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful() && mAuth.getCurrentUser() != null) {
                        getSharedPreferences("RentHubPrefs", MODE_PRIVATE)
                                .edit()
                                .putString("SAVED_ACCOUNT", email)
                                .apply();
                        checkRoleAndNavigate(mAuth.getCurrentUser().getUid());
                    } else {
                        String errMsg = "Thông tin đăng nhập không chính xác";
                        if (task.getException() != null && task.getException().getMessage() != null) {
                            String msg = task.getException().getMessage();
                            if (msg.toLowerCase().contains("network") || msg.toLowerCase().contains("timeout") || msg.toLowerCase().contains("unreachable")) {
                                errMsg = "Lỗi kết nối mạng: Máy ảo chưa có Internet!";
                            } else {
                                errMsg = msg;
                            }
                        }
                        showLoginError(errMsg);
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
                            showLoginError("Không tìm thấy email liên kết với số điện thoại này");
                        }
                    } else {
                        showLoginError("Số điện thoại chưa được đăng ký hoặc lỗi mạng");
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

                        // KIỂM TRA THÔNG TIN CƠ BẢN: Nếu chưa có Tên hoặc SĐT -> Yêu cầu cập nhật thông tin trước
                        if (TextUtils.isEmpty(fullName) || TextUtils.isEmpty(phone)) {
                            Toast.makeText(LoginActivity.this, "Vui lòng hoàn thiện thông tin cá nhân!", Toast.LENGTH_SHORT).show();

                            // Lưu ý: Thay 'EditProfileActivity' bằng tên Activity cập nhật thông tin cá nhân trong dự án của bạn (ví dụ: EditProfileActivity / UpdateProfileActivity)
                            Intent intent = new Intent(LoginActivity.this, EditProfileActivity.class);
                            intent.putExtra("UID", uid);
                            intent.putExtra("ROLE", role);
                            startActivity(intent);
                            finish();
                            return;
                        }

                        // Kiểm tra vai trò: Chủ trọ hay Người thuê
                        if (role != null && (role.equalsIgnoreCase("LANDLORD")
                                || role.equalsIgnoreCase("CHỦ TRỌ")
                                || role.toLowerCase().contains("chủ"))) {
                            // Chuyển sang màn hình giao diện Chủ trọ
                            Intent intent = new Intent(LoginActivity.this, LandlordHomeActivity.class);
                            intent.putExtra("FULL_NAME", fullName);
                            intent.putExtra("EMAIL", email);
                            intent.putExtra("PHONE", phone);
                            intent.putExtra("ROLE", "LANDLORD");
                            startActivity(intent);
                            finish();
                        } else {
                            // Chuyển sang màn hình giao diện Người thuê trọ
                            Intent intent = new Intent(LoginActivity.this, TenantHomeActivity.class);
                            intent.putExtra("FULL_NAME", fullName);
                            intent.putExtra("EMAIL", email);
                            intent.putExtra("PHONE", phone);
                            intent.putExtra("ROLE", "TENANT");
                            startActivity(intent);
                            finish();
                        }
                    } else {
                        showLoginError("Không tải được dữ liệu người dùng từ Firestore");
                    }
                });
    }

    private void showLoginError() {
        showLoginError("Thông tin đăng nhập không chính xác");
    }

    private void showLoginError(String message) {
        setLoading(false);
        tvErrorMessage.setText(message);
        tvErrorMessage.setVisibility(View.VISIBLE);
    }

    private void setLoading(boolean isLoading) {
        btnLogin.setEnabled(!isLoading);
        if (isLoading) {
            btnLogin.setText("Đang đăng nhập...");
        } else {
            btnLogin.setText("Đăng nhập");
        }
    }
}