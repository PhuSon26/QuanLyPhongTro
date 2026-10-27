package com.example.renthub.ui.settings;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.renthub.R;
import com.example.renthub.ui.auth.EditProfileActivity;
import com.example.renthub.ui.auth.LoginActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class SettingsActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private TextView tvSettingsName, tvSettingsEmail, tvSettingsPhone, tvSettingsRoleBadge;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String currentRole = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();
        loadUserProfile();
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tvSettingsName = findViewById(R.id.tvSettingsName);
        tvSettingsEmail = findViewById(R.id.tvSettingsEmail);
        tvSettingsPhone = findViewById(R.id.tvSettingsPhone);
        tvSettingsRoleBadge = findViewById(R.id.tvSettingsRoleBadge);
    }

    private void loadUserProfile() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        if (currentUser.getEmail() != null) {
            tvSettingsEmail.setText(currentUser.getEmail());
        }

        db.collection("users").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String fullName = doc.getString("fullName");
                        String phone = doc.getString("phone");
                        String role = doc.getString("role");

                        if (!TextUtils.isEmpty(fullName)) {
                            tvSettingsName.setText(fullName);
                        } else if (currentUser.getDisplayName() != null) {
                            tvSettingsName.setText(currentUser.getDisplayName());
                        }

                        if (!TextUtils.isEmpty(phone)) {
                            tvSettingsPhone.setText(phone);
                        }

                        currentRole = role != null ? role : "";
                        boolean isLandlord = currentRole.equalsIgnoreCase("LANDLORD")
                                || currentRole.equalsIgnoreCase("CHỦ TRỌ")
                                || currentRole.toLowerCase().contains("chủ");

                        if (isLandlord) {
                            tvSettingsRoleBadge.setText("CHỦ TRỌ");
                            tvSettingsRoleBadge.setTextColor(Color.parseColor("#148369"));
                            tvSettingsRoleBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#E3F5ED")));
                        } else {
                            tvSettingsRoleBadge.setText("NGƯỜI THUÊ");
                            tvSettingsRoleBadge.setTextColor(Color.parseColor("#2563A6"));
                            tvSettingsRoleBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#EAF1FC")));
                        }
                    }
                });
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        // Chỉnh sửa thông tin cá nhân
        findViewById(R.id.itemEditProfile).setOnClickListener(v -> {
            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null) {
                Intent intent = new Intent(SettingsActivity.this, EditProfileActivity.class);
                intent.putExtra("UID", user.getUid());
                intent.putExtra("ROLE", currentRole);
                startActivity(intent);
            }
        });

        // Đổi mật khẩu
        findViewById(R.id.itemChangePassword).setOnClickListener(v ->
                Toast.makeText(this, "Tính năng đổi mật khẩu đang được hoàn thiện", Toast.LENGTH_SHORT).show()
        );

        // Thông báo
        findViewById(R.id.itemNotifications).setOnClickListener(v ->
                Toast.makeText(this, "Thông báo đang bật", Toast.LENGTH_SHORT).show()
        );

        // Ngôn ngữ
        findViewById(R.id.itemLanguage).setOnClickListener(v ->
                Toast.makeText(this, "Ngôn ngữ mặc định: Tiếng Việt", Toast.LENGTH_SHORT).show()
        );

        // Điều khoản
        findViewById(R.id.itemTerms).setOnClickListener(v ->
                Toast.makeText(this, "Điều khoản & Chính sách bảo mật RentHub 2026", Toast.LENGTH_SHORT).show()
        );

        // XỬ LÝ CHỨC NĂNG ĐĂNG XUẤT (CHỦ TRỌ & NGƯỜI THUÊ)
        findViewById(R.id.cardLogout).setOnClickListener(v -> showLogoutConfirmDialog());
    }

    private void showLogoutConfirmDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Đăng xuất tài khoản")
                .setMessage("Bạn có chắc chắn muốn đăng xuất khỏi ứng dụng RentHub không?")
                .setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("Đăng xuất", (dialog, which) -> {
                    // Thực hiện đăng xuất tài khoản Firebase
                    mAuth.signOut();

                    Toast.makeText(SettingsActivity.this, "Đã đăng xuất thành công!", Toast.LENGTH_SHORT).show();

                    // Chuyển về màn hình đăng nhập và dọn dẹp task stack
                    Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .show();
    }
}
