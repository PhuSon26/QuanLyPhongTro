package com.example.renthub;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        // Tự động mở màn hình Chọn vai trò (Đăng ký)
        startActivity(new android.content.Intent(this, com.example.renthub.ui.auth.RoleSelectionActivity.class));
        finish();
    }
}