package com.example.renthub.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.renthub.R;
import com.example.renthub.ui.home.LandlordHomeActivity;
import com.example.renthub.ui.home.TenantHomeActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private TextInputEditText edtFullName, edtPhone;
    private MaterialButton btnSaveProfile;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String userRole = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        initViews();
        getIntentData();
        setupListeners();
    }

    private void initViews() {
        edtFullName = findViewById(R.id.edtFullName);
        edtPhone = findViewById(R.id.edtPhone);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
    }

    private void getIntentData() {
        if (getIntent() != null) {
            userRole = getIntent().getStringExtra("ROLE");
        }
    }

    private void setupListeners() {
        btnSaveProfile.setOnClickListener(v -> saveProfileToFirestore());
    }

    private void saveProfileToFirestore() {
        String fullName = edtFullName.getText().toString().trim();
        String phone = edtPhone.getText().toString().trim();

        if (TextUtils.isEmpty(fullName)) {
            edtFullName.setError("Vui lòng nhập họ và tên");
            return;
        }

        if (TextUtils.isEmpty(phone)) {
            edtPhone.setError("Vui lòng nhập số điện thoại");
            return;
        }

        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        btnSaveProfile.setEnabled(false);
        btnSaveProfile.setText("Đang lưu...");

        Map<String, Object> updateData = new HashMap<>();
        updateData.put("fullName", fullName);
        updateData.put("phone", phone);

        db.collection("users").document(uid)
                .update(updateData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Cập nhật thông tin thành công!", Toast.LENGTH_SHORT).show();
                    navigateToHome(fullName, phone);
                })
                .addOnFailureListener(e -> {
                    // Nếu document chưa tồn tại, tiến hành tạo mới
                    db.collection("users").document(uid)
                            .set(updateData)
                            .addOnSuccessListener(aVoid1 -> navigateToHome(fullName, phone))
                            .addOnFailureListener(e1 -> {
                                btnSaveProfile.setEnabled(true);
                                btnSaveProfile.setText("Lưu & Tiếp tục");
                                Toast.makeText(this, "Lỗi: " + e1.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                });
    }

    private void navigateToHome(String fullName, String phone) {
        Intent intent;
        if (userRole != null && (userRole.equalsIgnoreCase("LANDLORD")
                || userRole.equalsIgnoreCase("CHỦ TRỌ")
                || userRole.toLowerCase().contains("chủ"))) {
            intent = new Intent(EditProfileActivity.this, LandlordHomeActivity.class);
            intent.putExtra("ROLE", "LANDLORD");
        } else {
            intent = new Intent(EditProfileActivity.this, TenantHomeActivity.class);
            intent.putExtra("ROLE", "TENANT");
        }

        intent.putExtra("FULL_NAME", fullName);
        intent.putExtra("PHONE", phone);
        startActivity(intent);
        finish();
    }
}