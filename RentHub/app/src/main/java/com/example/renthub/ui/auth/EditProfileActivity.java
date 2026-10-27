package com.example.renthub.ui.auth;

import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.renthub.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private ShapeableImageView imgProfileAvatar;
    private FloatingActionButton btnChangeAvatar;
    private TextView tvRoleBadge;
    private TextInputEditText edtProfileEmail, edtFullName, edtPhone;
    private MaterialButton btnSaveProfile;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String userRole = "";
    private String selectedAvatarUriString = "";

    private ActivityResultLauncher<String> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        setupImagePicker();
        initViews();
        getIntentData();
        loadExistingProfile();
        setupListeners();
    }

    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedAvatarUriString = uri.toString();
                        Glide.with(this)
                                .load(uri)
                                .circleCrop()
                                .into(imgProfileAvatar);
                    }
                }
        );
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        imgProfileAvatar = findViewById(R.id.imgProfileAvatar);
        btnChangeAvatar = findViewById(R.id.btnChangeAvatar);
        tvRoleBadge = findViewById(R.id.tvRoleBadge);
        edtProfileEmail = findViewById(R.id.edtProfileEmail);
        edtFullName = findViewById(R.id.edtFullName);
        edtPhone = findViewById(R.id.edtPhone);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
    }

    private void getIntentData() {
        if (getIntent() != null) {
            userRole = getIntent().getStringExtra("ROLE");
        }
    }

    private void loadExistingProfile() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        if (currentUser.getEmail() != null) {
            edtProfileEmail.setText(currentUser.getEmail());
        }

        String uid = currentUser.getUid();
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String fullName = documentSnapshot.getString("fullName");
                        String phone = documentSnapshot.getString("phone");
                        String role = documentSnapshot.getString("role");
                        String avatarUrl = documentSnapshot.getString("avatarUrl");

                        if (!TextUtils.isEmpty(fullName)) {
                            edtFullName.setText(fullName);
                        } else if (currentUser.getDisplayName() != null) {
                            edtFullName.setText(currentUser.getDisplayName());
                        }

                        if (!TextUtils.isEmpty(phone)) {
                            edtPhone.setText(phone);
                        }

                        if (!TextUtils.isEmpty(avatarUrl)) {
                            selectedAvatarUriString = avatarUrl;
                            Glide.with(this)
                                    .load(avatarUrl)
                                    .circleCrop()
                                    .placeholder(R.drawable.ic_user)
                                    .into(imgProfileAvatar);
                        }

                        if (role != null && !role.isEmpty()) {
                            userRole = role;
                        }
                    }

                    updateRoleBadgeUI();
                });
    }

    private void updateRoleBadgeUI() {
        boolean isLandlord = userRole != null && (userRole.equalsIgnoreCase("LANDLORD")
                || userRole.equalsIgnoreCase("CHỦ TRỌ")
                || userRole.toLowerCase().contains("chủ"));

        if (isLandlord) {
            tvRoleBadge.setText("CHỦ TRỌ");
            tvRoleBadge.setTextColor(Color.parseColor("#148369"));
            tvRoleBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#E3F5ED")));
        } else {
            tvRoleBadge.setText("NGƯỜI THUÊ");
            tvRoleBadge.setTextColor(Color.parseColor("#2563A6"));
            tvRoleBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#EAF1FC")));
        }
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnChangeAvatar.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        imgProfileAvatar.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

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

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;
        String uid = currentUser.getUid();

        btnSaveProfile.setEnabled(false);
        btnSaveProfile.setText("Đang lưu thông tin...");

        Map<String, Object> updateData = new HashMap<>();
        updateData.put("fullName", fullName);
        updateData.put("phone", phone);
        if (!TextUtils.isEmpty(selectedAvatarUriString)) {
            updateData.put("avatarUrl", selectedAvatarUriString);
        }

        // Cập nhật tên hiển thị lên Firebase Auth
        UserProfileChangeRequest.Builder profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(fullName);
        if (!TextUtils.isEmpty(selectedAvatarUriString)) {
            profileUpdates.setPhotoUri(Uri.parse(selectedAvatarUriString));
        }
        currentUser.updateProfile(profileUpdates.build());

        db.collection("users").document(uid)
                .update(updateData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Cập nhật hồ sơ thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    // Nếu document chưa có -> tạo mới
                    db.collection("users").document(uid)
                            .set(updateData)
                            .addOnSuccessListener(aVoid1 -> {
                                Toast.makeText(this, "Lưu thông tin thành công!", Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e1 -> {
                                btnSaveProfile.setEnabled(true);
                                btnSaveProfile.setText("Lưu thông tin cá nhân");
                                Toast.makeText(this, "Lỗi: " + e1.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                });
    }
}