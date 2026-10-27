package com.example.renthub.ui.settings;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.renthub.R;
import com.example.renthub.ui.auth.EditProfileActivity;
import com.example.renthub.ui.auth.LoginActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SettingsActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private ImageView imgSettingsAvatar;
    private TextView tvSettingsName, tvSettingsEmail, tvSettingsPhone, tvSettingsRoleBadge;
    private SwitchMaterial switchPushNotifications;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private SharedPreferences prefs;
    private String currentRole = "";

    private static final String PREF_NAME = "RentHubPrefs";
    private static final String KEY_PUSH_NOTIFICATIONS = "PUSH_NOTIFICATIONS_ENABLED";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        initViews();
        loadUserProfile();
        setupPushNotificationSwitch();
        setupListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserProfile();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        imgSettingsAvatar = findViewById(R.id.imgSettingsAvatar);
        tvSettingsName = findViewById(R.id.tvSettingsName);
        tvSettingsEmail = findViewById(R.id.tvSettingsEmail);
        tvSettingsPhone = findViewById(R.id.tvSettingsPhone);
        tvSettingsRoleBadge = findViewById(R.id.tvSettingsRoleBadge);
        switchPushNotifications = findViewById(R.id.switchPushNotifications);
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
                        String avatarUrl = doc.getString("avatarUrl");

                        if (!TextUtils.isEmpty(fullName)) {
                            tvSettingsName.setText(fullName);
                        } else if (currentUser.getDisplayName() != null) {
                            tvSettingsName.setText(currentUser.getDisplayName());
                        }

                        if (!TextUtils.isEmpty(phone)) {
                            tvSettingsPhone.setText(phone);
                        }

                        if (!TextUtils.isEmpty(avatarUrl) && imgSettingsAvatar != null) {
                            Glide.with(this)
                                    .load(avatarUrl)
                                    .circleCrop()
                                    .placeholder(R.drawable.ic_user)
                                    .into(imgSettingsAvatar);
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

    private void setupPushNotificationSwitch() {
        if (switchPushNotifications == null) return;

        // Đọc cài đặt đã lưu (mặc định bật)
        boolean isEnabled = prefs.getBoolean(KEY_PUSH_NOTIFICATIONS, true);
        switchPushNotifications.setChecked(isEnabled);

        switchPushNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // 1. Lưu cài đặt cục bộ
            prefs.edit().putBoolean(KEY_PUSH_NOTIFICATIONS, isChecked).apply();

            // 2. Đồng bộ lên Firestore
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                Map<String, Object> updateData = new HashMap<>();
                updateData.put("notificationsEnabled", isChecked);
                db.collection("users").document(currentUser.getUid()).update(updateData);
            }

            // 3. Phản hồi Toast cho người dùng
            if (isChecked) {
                Toast.makeText(this, "Đã bật nhận thông báo push", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Đã tắt nhận thông báo push", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        // Chức năng: Xem và chỉnh sửa thông tin cá nhân (Profile: Họ tên, SĐT, Email, Avatar)
        findViewById(R.id.itemEditProfile).setOnClickListener(v -> {
            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null) {
                Intent intent = new Intent(SettingsActivity.this, EditProfileActivity.class);
                intent.putExtra("UID", user.getUid());
                intent.putExtra("ROLE", currentRole);
                startActivity(intent);
            }
        });

        // Chức năng: Đổi mật khẩu
        findViewById(R.id.itemChangePassword).setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, ChangePasswordActivity.class);
            startActivity(intent);
        });

        // Click vào cả hàng thông báo để toggle switch
        findViewById(R.id.itemNotifications).setOnClickListener(v -> {
            if (switchPushNotifications != null) {
                switchPushNotifications.setChecked(!switchPushNotifications.isChecked());
            }
        });

        // Ngôn ngữ
        findViewById(R.id.itemLanguage).setOnClickListener(v ->
                Toast.makeText(this, "Ngôn ngữ mặc định: Tiếng Việt", Toast.LENGTH_SHORT).show()
        );

        // Điều khoản
        findViewById(R.id.itemTerms).setOnClickListener(v ->
                Toast.makeText(this, "Điều khoản & Chính sách bảo mật RentHub 2026", Toast.LENGTH_SHORT).show()
        );

        // Chức năng: Đăng xuất (Chủ trọ & Người thuê)
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
