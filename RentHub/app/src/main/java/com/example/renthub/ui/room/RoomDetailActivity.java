package com.example.renthub.ui.room;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.renthub.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

public class RoomDetailActivity extends AppCompatActivity {

    public static final String EXTRA_ROOM = "EXTRA_ROOM";

    private ImageButton btnBack, btnCallTenant;
    private MaterialButton btnEditRoomBottom;
    private TextView tvDetailRoomName, tvDetailLocation, tvDetailStatus, tvDetailPrice, tvDetailArea;
    private TextView tvDetailTenantName, tvDetailTenantPhone;
    private MaterialCardView cardStatusBadge;

    private Room currentRoom;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_detail);

        initViews();
        getIntentData();
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnEditRoomBottom = findViewById(R.id.btnEditRoomBottom);
        btnCallTenant = findViewById(R.id.btnCallTenant);

        tvDetailRoomName = findViewById(R.id.tvDetailRoomName);
        tvDetailLocation = findViewById(R.id.tvDetailLocation);
        tvDetailStatus = findViewById(R.id.tvDetailStatus);
        tvDetailPrice = findViewById(R.id.tvDetailPrice);
        tvDetailArea = findViewById(R.id.tvDetailArea);

        tvDetailTenantName = findViewById(R.id.tvDetailTenantName);
        tvDetailTenantPhone = findViewById(R.id.tvDetailTenantPhone);
        cardStatusBadge = findViewById(R.id.cardStatusBadge);
    }

    private void getIntentData() {
        if (getIntent() != null && getIntent().hasExtra(EXTRA_ROOM)) {
            currentRoom = (Room) getIntent().getSerializableExtra(EXTRA_ROOM);
            if (currentRoom != null) {
                displayRoomData();
            }
        }
    }

    private void displayRoomData() {
        tvDetailRoomName.setText(currentRoom.getName() != null ? currentRoom.getName() : "Phòng trọ");
        tvDetailLocation.setText(currentRoom.getLocation() != null ? currentRoom.getLocation() : "Chưa có vị trí");

        String area = currentRoom.getArea() != null ? currentRoom.getArea() : "0";
        if (!area.contains("m²")) area += " m²";
        tvDetailArea.setText(area);

        String price = currentRoom.getPrice() != null ? currentRoom.getPrice() : "0";
        if (!price.contains("đ")) price += "đ/tháng";
        tvDetailPrice.setText(price);

        String status = currentRoom.getStatus() != null ? currentRoom.getStatus() : "Còn trống";
        String statusLower = status.toLowerCase();

        if (statusLower.contains("thuê") || statusLower.contains("occupied")) {
            tvDetailStatus.setText("Đang thuê");
            tvDetailStatus.setTextColor(Color.parseColor("#2563A6"));
            cardStatusBadge.setCardBackgroundColor(Color.parseColor("#EAF1FC"));
        } else if (statusLower.contains("trì") || statusLower.contains("maintenance")) {
            tvDetailStatus.setText("Bảo trì");
            tvDetailStatus.setTextColor(Color.parseColor("#B7791F"));
            cardStatusBadge.setCardBackgroundColor(Color.parseColor("#FFF3DF"));
        } else {
            tvDetailStatus.setText("Còn trống");
            tvDetailStatus.setTextColor(Color.parseColor("#148369"));
            cardStatusBadge.setCardBackgroundColor(Color.parseColor("#E3F5ED"));
        }

        if (currentRoom.getTenantName() != null && !currentRoom.getTenantName().trim().isEmpty()) {
            tvDetailTenantName.setText(currentRoom.getTenantName());
        } else {
            tvDetailTenantName.setText("Chưa có người thuê");
        }

        if (currentRoom.getTenantPhone() != null && !currentRoom.getTenantPhone().trim().isEmpty()) {
            tvDetailTenantPhone.setText(currentRoom.getTenantPhone());
        } else {
            tvDetailTenantPhone.setText("---");
        }
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        // Nút chuyển sang màn hình Sửa phòng duy nhất ở phía dưới
        btnEditRoomBottom.setOnClickListener(v -> openEditScreen());

        btnCallTenant.setOnClickListener(v -> {
            if (currentRoom != null && currentRoom.getTenantPhone() != null && !currentRoom.getTenantPhone().trim().isEmpty()) {
                Intent callIntent = new Intent(Intent.ACTION_DIAL);
                callIntent.setData(Uri.parse("tel:" + currentRoom.getTenantPhone()));
                startActivity(callIntent);
            } else {
                Toast.makeText(this, "Chưa có số điện thoại người thuê", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openEditScreen() {
        Intent intent = new Intent(RoomDetailActivity.this, AddEditRoomActivity.class);
        intent.putExtra(AddEditRoomActivity.EXTRA_ROOM, currentRoom);
        startActivity(intent);
        finish();
    }
}