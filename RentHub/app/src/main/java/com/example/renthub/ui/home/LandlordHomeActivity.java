package com.example.renthub.ui.home;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.renthub.R;
import com.example.renthub.ui.room.LandlordRoomListActivity;
import com.example.renthub.ui.room.Room;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LandlordHomeActivity extends AppCompatActivity {

    private TextView tvGreeting;
    private MaterialButton btnHomeViewRooms;
    private ImageButton btnHomeNotification;

    // THỐNG KÊ
    private TextView tvStat1, tvStat2, tvStat3;

    // PHÒNG GẦN ĐÂY
    private View cardRecentRoom1, cardRecentRoom2;
    private TextView tvRecentRoom1, tvRecentRoomDesc1, tvRecentStatus1;
    private TextView tvRecentRoom2, tvRecentRoomDesc2, tvRecentStatus2;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        initViews();
        displayGreeting();
        setupListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDashboardData();
    }

    private void initViews() {
        tvGreeting = findViewById(R.id.tvGreeting);
        btnHomeViewRooms = findViewById(R.id.btnHomeViewRooms);
        btnHomeNotification = findViewById(R.id.btnHomeNotification);

        tvStat1 = findViewById(R.id.tvStat1);
        tvStat2 = findViewById(R.id.tvStat2);
        tvStat3 = findViewById(R.id.tvStat3);

        // Ánh xạ CardView đại diện cho từng thẻ phòng
        cardRecentRoom1 = findViewById(R.id.cardRecentRoom1);
        cardRecentRoom2 = findViewById(R.id.cardRecentRoom2);

        tvRecentRoom1 = findViewById(R.id.tvRecentRoom1);
        tvRecentRoomDesc1 = findViewById(R.id.tvRecentRoomDesc1);
        tvRecentStatus1 = findViewById(R.id.tvRecentStatus1);

        tvRecentRoom2 = findViewById(R.id.tvRecentRoom2);
        tvRecentRoomDesc2 = findViewById(R.id.tvRecentRoomDesc2);
        tvRecentStatus2 = findViewById(R.id.tvRecentStatus2);
    }

    private void displayGreeting() {
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("FULL_NAME")) {
            String fullName = intent.getStringExtra("FULL_NAME");
            if (fullName != null && !fullName.isEmpty()) {
                tvGreeting.setText("Xin chào, " + fullName + "!");
            }
        }
    }

    private void setupListeners() {
        if (btnHomeViewRooms != null) {
            btnHomeViewRooms.setOnClickListener(v -> {
                Intent intent = new Intent(LandlordHomeActivity.this, LandlordRoomListActivity.class);
                startActivity(intent);
            });
        }

        if (btnHomeNotification != null) {
            btnHomeNotification.setOnClickListener(v ->
                    Toast.makeText(LandlordHomeActivity.this, "Không có thông báo mới", Toast.LENGTH_SHORT).show()
            );
        }

        View btnNavSettings = findViewById(R.id.btnNavSettings);
        if (btnNavSettings != null) {
            btnNavSettings.setOnClickListener(v -> {
                Intent intent = new Intent(LandlordHomeActivity.this, com.example.renthub.ui.settings.SettingsActivity.class);
                startActivity(intent);
            });
        }
    }

    private void loadDashboardData() {
        if (mAuth.getCurrentUser() == null) return;

        String uid = mAuth.getCurrentUser().getUid();

        db.collection("rooms")
                .whereEqualTo("landlordId", uid)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Room> roomList = new ArrayList<>();
                    int occupied = 0;
                    int available = 0;

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Room room = doc.toObject(Room.class);
                        if (room != null) {
                            room.setId(doc.getId());
                            roomList.add(room);

                            String status = room.getStatus() != null ? room.getStatus().toLowerCase() : "";
                            if (status.contains("thuê") || status.contains("occupied")) {
                                occupied++;
                            } else if (status.contains("trống") || status.contains("available")) {
                                available++;
                            }
                        }
                    }

                    int total = roomList.size();

                    if (tvStat1 != null) tvStat1.setText(String.format(Locale.getDefault(), "%02d", total));
                    if (tvStat2 != null) tvStat2.setText(String.format(Locale.getDefault(), "%02d", occupied));
                    if (tvStat3 != null) tvStat3.setText(String.format(Locale.getDefault(), "%02d", available));

                    updateRecentRoomsUI(roomList);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    /**
     * ẨN/HIỆN THẺ PHÒNG THEO SỐ LƯỢNG PHÒNG THỰC TẾ
     */
    private void updateRecentRoomsUI(List<Room> rooms) {
        if (rooms == null || rooms.isEmpty()) {
            if (cardRecentRoom1 != null) cardRecentRoom1.setVisibility(View.GONE);
            if (cardRecentRoom2 != null) cardRecentRoom2.setVisibility(View.GONE);
            return;
        }

        // Hiển thị Phòng 1
        if (cardRecentRoom1 != null) {
            cardRecentRoom1.setVisibility(View.VISIBLE);
            bindRoomDataToView(rooms.get(0), tvRecentRoom1, tvRecentRoomDesc1, tvRecentStatus1);
        }

        // Kiểm tra xem có Phòng thứ 2 hay không
        if (rooms.size() > 1) {
            if (cardRecentRoom2 != null) {
                cardRecentRoom2.setVisibility(View.VISIBLE);
                bindRoomDataToView(rooms.get(1), tvRecentRoom2, tvRecentRoomDesc2, tvRecentStatus2);
            }
        } else {
            // Nếu chỉ có 1 phòng -> ẨN THẺ PHÒNG 2 HOÀN TOÀN
            if (cardRecentRoom2 != null) {
                cardRecentRoom2.setVisibility(View.GONE);
            }
        }
    }

    private void bindRoomDataToView(Room room, TextView tvName, TextView tvDesc, TextView tvStatus) {
        if (tvName != null) tvName.setText(room.getName());

        if (tvDesc != null) {
            String area = room.getArea() != null ? (room.getArea().contains("m²") ? room.getArea() : room.getArea() + " m²") : "";
            String price = room.getPrice() != null ? (room.getPrice().contains("đ") ? room.getPrice() : room.getPrice() + "đ/tháng") : "";
            tvDesc.setText(area + " • " + price);
        }

        if (tvStatus != null) {
            tvStatus.setVisibility(View.VISIBLE);
            String status = room.getStatus() != null ? room.getStatus().toLowerCase() : "";
            if (status.contains("thuê") || status.contains("occupied")) {
                tvStatus.setText("Đang thuê");
                tvStatus.setTextColor(Color.parseColor("#2563A6"));
            } else if (status.contains("trì") || status.contains("maintenance")) {
                tvStatus.setText("Bảo trì");
                tvStatus.setTextColor(Color.parseColor("#B7791F"));
            } else {
                tvStatus.setText("Còn trống");
                tvStatus.setTextColor(Color.parseColor("#148369"));
            }
        }
    }
}