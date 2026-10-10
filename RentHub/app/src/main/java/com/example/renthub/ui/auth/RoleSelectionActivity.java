package com.example.renthub.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.renthub.R;

public class RoleSelectionActivity extends AppCompatActivity {

    public static final String EXTRA_ROLE = "EXTRA_ROLE";
    public static final String ROLE_LANDLORD = "LANDLORD";
    public static final String ROLE_TENANT = "TENANT";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_role_selection);

        ImageView btnBack = findViewById(R.id.btnBack);
        View cardLandlord = findViewById(R.id.cardLandlord);
        View cardTenant = findViewById(R.id.cardTenant);

        // Nút quay lại
        btnBack.setOnClickListener(v -> finish());

        // Chọn vai trò Chủ trọ -> Chuyển sang màn Đăng ký tài khoản Chủ trọ
        cardLandlord.setOnClickListener(v -> {
            Intent intent = new Intent(RoleSelectionActivity.this, RegisterActivity.class);
            intent.putExtra(EXTRA_ROLE, ROLE_LANDLORD);
            startActivity(intent);
        });

        // Chọn vai trò Người thuê -> Chuyển sang màn Đăng ký tài khoản Người thuê
        cardTenant.setOnClickListener(v -> {
            Intent intent = new Intent(RoleSelectionActivity.this, RegisterActivity.class);
            intent.putExtra(EXTRA_ROLE, ROLE_TENANT);
            startActivity(intent);
        });
    }
}
