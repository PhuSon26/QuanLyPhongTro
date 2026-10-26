package com.example.renthub.ui.landlord;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.renthub.R;
import com.example.renthub.ui.auth.LoginActivity;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class LandlordMainActivity extends AppCompatActivity {

    private TextView tvWelcomeTitle;
    private TextView tvFullName;
    private TextView tvEmail;
    private TextView tvPhone;
    private TextView tvRole;
    private MaterialButton btnLogout;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landlord_main);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();
        loadUserData();
        setupListeners();
    }

    private void initViews() {
        tvWelcomeTitle = findViewById(R.id.tvWelcomeTitle);
        tvFullName = findViewById(R.id.tvFullName);
        tvEmail = findViewById(R.id.tvEmail);
        tvPhone = findViewById(R.id.tvPhone);
        tvRole = findViewById(R.id.tvRole);
        btnLogout = findViewById(R.id.btnLogout);
    }

    private void loadUserData() {
        // Lấy thông tin từ Intent trước nếu có
        Intent intent = getIntent();
        String nameFromIntent = intent != null ? intent.getStringExtra("FULL_NAME") : null;
        String emailFromIntent = intent != null ? intent.getStringExtra("EMAIL") : null;
        String phoneFromIntent = intent != null ? intent.getStringExtra("PHONE") : null;
        String roleFromIntent = intent != null ? intent.getStringExtra("ROLE") : null;

        if (nameFromIntent != null && !nameFromIntent.isEmpty()) {
            tvWelcomeTitle.setText("Chào " + nameFromIntent + "! 👋");
            tvFullName.setText(nameFromIntent);
        }
        if (emailFromIntent != null) tvEmail.setText(emailFromIntent);
        if (phoneFromIntent != null) tvPhone.setText(phoneFromIntent);
        if (roleFromIntent != null) tvRole.setText(roleFromIntent + " (Chủ trọ)");

        // Đồng thời tải từ Firestore để đảm bảo dữ liệu mới nhất
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            if (emailFromIntent == null || emailFromIntent.isEmpty()) {
                tvEmail.setText(currentUser.getEmail());
            }

            db.collection("users").document(currentUser.getUid()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String fullName = documentSnapshot.getString("fullName");
                            String email = documentSnapshot.getString("email");
                            String phone = documentSnapshot.getString("phone");
                            String role = documentSnapshot.getString("role");

                            if (fullName != null) {
                                tvWelcomeTitle.setText("Chào " + fullName + "! 👋");
                                tvFullName.setText(fullName);
                            }
                            if (email != null) tvEmail.setText(email);
                            if (phone != null) tvPhone.setText(phone);
                            if (role != null) tvRole.setText(role + " (Chủ trọ)");
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(LandlordMainActivity.this, "Không thể tải dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void setupListeners() {
        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Toast.makeText(LandlordMainActivity.this, "Đã đăng xuất tài khoản", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(LandlordMainActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}
