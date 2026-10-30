package com.example.renthub.ui.settings;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.renthub.R;
import com.example.renthub.ui.auth.EditProfileActivity;
import com.example.renthub.ui.auth.LoginActivity;
import com.example.renthub.ui.invoice.Invoice;
import com.example.renthub.ui.invoice.InvoiceListActivity;
import com.example.renthub.ui.invoice.TenantInvoiceDetailActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
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

        boolean isEnabled = prefs.getBoolean(KEY_PUSH_NOTIFICATIONS, true);
        switchPushNotifications.setChecked(isEnabled);

        switchPushNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(KEY_PUSH_NOTIFICATIONS, isChecked).apply();

            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                Map<String, Object> updateData = new HashMap<>();
                updateData.put("notificationsEnabled", isChecked);
                db.collection("users").document(currentUser.getUid()).update(updateData);
            }

            if (isChecked) {
                Toast.makeText(this, "Đã bật nhận thông báo push", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Đã tắt nhận thông báo push", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupListeners() {
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        // CHUYỂN TAB BOTTOM NAVIGATION
        View btnNavRoom = findViewById(R.id.btnNavRoom);
        if (btnNavRoom != null) {
            btnNavRoom.setOnClickListener(v -> finish());
        }

        View btnNavInvoice = findViewById(R.id.btnNavInvoice);
        if (btnNavInvoice != null) {
            btnNavInvoice.setOnClickListener(v -> {
                boolean isLandlord = currentRole.equalsIgnoreCase("LANDLORD")
                        || currentRole.equalsIgnoreCase("CHỦ TRỌ")
                        || currentRole.toLowerCase().contains("chủ");

                if (isLandlord) {
                    startActivity(new Intent(SettingsActivity.this, InvoiceListActivity.class));
                    finish();
                } else {
                    loadTenantInvoiceAndNavigate();
                }
            });
        }

        findViewById(R.id.itemEditProfile).setOnClickListener(v -> {
            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null) {
                Intent intent = new Intent(SettingsActivity.this, EditProfileActivity.class);
                intent.putExtra("UID", user.getUid());
                intent.putExtra("ROLE", currentRole);
                startActivity(intent);
            }
        });

        findViewById(R.id.itemChangePassword).setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, ChangePasswordActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.itemNotifications).setOnClickListener(v -> {
            if (switchPushNotifications != null) {
                switchPushNotifications.setChecked(!switchPushNotifications.isChecked());
            }
        });

        findViewById(R.id.itemLanguage).setOnClickListener(v ->
                Toast.makeText(this, "Ngôn ngữ mặc định: Tiếng Việt", Toast.LENGTH_SHORT).show()
        );

        findViewById(R.id.itemTerms).setOnClickListener(v ->
                Toast.makeText(this, "Điều khoản & Chính sách bảo mật RentHub 2026", Toast.LENGTH_SHORT).show()
        );

        View btnLogout = findViewById(R.id.btnDoLogout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> showLogoutConfirmDialog());
        }
    }

    /**
     * TÌM KIẾM HÓA ĐƠN TRỌN GÓI DÀNH CHO NGƯỜI THUÊ:
     * Khớp lần lượt qua các trường: UID -> Email -> Họ tên -> Phòng người dùng thuê -> Collection "rooms".
     */
    private void loadTenantInvoiceAndNavigate() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        String uid = currentUser.getUid();
        String email = currentUser.getEmail() != null ? currentUser.getEmail().trim() : "";

        // 1. Lấy thông tin tài khoản người dùng
        db.collection("users").document(uid).get().addOnSuccessListener(userDoc -> {
            String fullName = (userDoc.exists() && userDoc.getString("fullName") != null) ? userDoc.getString("fullName").trim() : "";
            String userRoomName = (userDoc.exists() && userDoc.getString("roomName") != null) ? userDoc.getString("roomName").trim() : "";
            String userRoomId = (userDoc.exists() && userDoc.getString("roomId") != null) ? userDoc.getString("roomId").trim() : "";

            // 2. Lấy danh sách hóa đơn và so sánh đối chiếu nhiều tiêu chí
            db.collection("invoices").get().addOnSuccessListener(querySnapshot -> {
                DocumentSnapshot matchedDoc = null;

                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                    String invTenantId = doc.getString("tenantId");
                    String invTenantEmail = doc.getString("tenantEmail");
                    String invTenantName = doc.getString("tenantName");
                    String invRoomName = doc.getString("roomName");
                    String invRoomId = doc.getString("roomId");

                    // Khớp UID
                    if (invTenantId != null && invTenantId.equals(uid)) {
                        matchedDoc = doc;
                        break;
                    }
                    // Khớp Email
                    if (!email.isEmpty() && invTenantEmail != null && invTenantEmail.equalsIgnoreCase(email)) {
                        matchedDoc = doc;
                        break;
                    }
                    // Khớp Họ tên
                    if (!fullName.isEmpty() && invTenantName != null && invTenantName.equalsIgnoreCase(fullName)) {
                        matchedDoc = doc;
                        break;
                    }
                    // Khớp Tên phòng
                    if (!userRoomName.isEmpty() && invRoomName != null && invRoomName.equalsIgnoreCase(userRoomName)) {
                        matchedDoc = doc;
                        break;
                    }
                    // Khớp Room ID
                    if (!userRoomId.isEmpty() && invRoomId != null && invRoomId.equalsIgnoreCase(userRoomId)) {
                        matchedDoc = doc;
                        break;
                    }
                }

                if (matchedDoc != null) {
                    openTenantInvoiceDetail(matchedDoc);
                } else {
                    // Tiếp tục tìm phòng của người thuê trong collection "rooms" để tìm đúng hóa đơn phòng đó
                    checkRoomsAndFindInvoice(uid, email, fullName);
                }
            }).addOnFailureListener(e ->
                    Toast.makeText(this, "Bạn chưa có hóa đơn thanh toán nào!", Toast.LENGTH_SHORT).show()
            );
        }).addOnFailureListener(e ->
                Toast.makeText(this, "Bạn chưa có hóa đơn thanh toán nào!", Toast.LENGTH_SHORT).show()
        );
    }

    private void checkRoomsAndFindInvoice(String uid, String email, String fullName) {
        db.collection("rooms").get().addOnSuccessListener(roomsSnapshot -> {
            String rentedRoomName = "";
            String rentedRoomId = "";

            for (DocumentSnapshot roomDoc : roomsSnapshot.getDocuments()) {
                String rTenantId = roomDoc.getString("tenantId");
                String rTenantEmail = roomDoc.getString("tenantEmail");
                String rTenantName = roomDoc.getString("tenantName");

                if ((rTenantId != null && rTenantId.equals(uid)) ||
                        (!email.isEmpty() && rTenantEmail != null && rTenantEmail.equalsIgnoreCase(email)) ||
                        (!fullName.isEmpty() && rTenantName != null && rTenantName.equalsIgnoreCase(fullName))) {
                    rentedRoomId = roomDoc.getId();
                    rentedRoomName = roomDoc.getString("roomName");
                    if (rentedRoomName == null) rentedRoomName = roomDoc.getString("name");
                    break;
                }
            }

            final String finalRoomName = rentedRoomName != null ? rentedRoomName.trim() : "";
            final String finalRoomId = rentedRoomId;

            if (!finalRoomName.isEmpty() || !finalRoomId.isEmpty()) {
                db.collection("invoices").get().addOnSuccessListener(invSnapshot -> {
                    DocumentSnapshot foundDoc = null;
                    for (DocumentSnapshot doc : invSnapshot.getDocuments()) {
                        String invRoomName = doc.getString("roomName");
                        String invRoomId = doc.getString("roomId");

                        if (!finalRoomName.isEmpty() && invRoomName != null && invRoomName.equalsIgnoreCase(finalRoomName)) {
                            foundDoc = doc;
                            break;
                        }
                        if (!finalRoomId.isEmpty() && invRoomId != null && invRoomId.equalsIgnoreCase(finalRoomId)) {
                            foundDoc = doc;
                            break;
                        }
                    }

                    if (foundDoc != null) {
                        openTenantInvoiceDetail(foundDoc);
                    } else {
                        Toast.makeText(this, "Bạn chưa có hóa đơn thanh toán nào!", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Toast.makeText(this, "Bạn chưa có hóa đơn thanh toán nào!", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e ->
                Toast.makeText(this, "Bạn chưa có hóa đơn thanh toán nào!", Toast.LENGTH_SHORT).show()
        );
    }

    private void openTenantInvoiceDetail(DocumentSnapshot doc) {
        Invoice invoice = doc.toObject(Invoice.class);
        if (invoice != null) {
            invoice.setId(doc.getId());
            Intent intent = new Intent(SettingsActivity.this, TenantInvoiceDetailActivity.class);
            intent.putExtra(TenantInvoiceDetailActivity.EXTRA_INVOICE, invoice);
            startActivity(intent);
            finish();
        }
    }

    private void showLogoutConfirmDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Đăng xuất tài khoản")
                .setMessage("Bạn có chắc chắn muốn đăng xuất khỏi ứng dụng RentHub không?")
                .setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("Đăng xuất", (dialog, which) -> performLogout())
                .show();
    }

    private void performLogout() {
        mAuth.signOut();
        Toast.makeText(SettingsActivity.this, "Đã đăng xuất thành công!", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finishAffinity();
    }
}