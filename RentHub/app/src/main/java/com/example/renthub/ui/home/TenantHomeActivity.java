package com.example.renthub.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.renthub.R;
import com.example.renthub.ui.room.Room;
import com.example.renthub.ui.room.RoomAdapter;
import com.example.renthub.ui.room.RoomDetailActivity;
import com.example.renthub.ui.tenant.TenantRoomListActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TenantHomeActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private RecyclerView recyclerViewAvailableRooms;
    private RoomAdapter roomAdapter;
    private List<Room> availableRoomList;

    private void createSampleRoomsQuickly() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Dữ liệu Mẫu 1: Phòng đang thuê
        Map<String, Object> room1 = new HashMap<>();
        room1.put("id", "room_01");
        room1.put("name", "Phòng A102");
        room1.put("location", "Quận Bình Thạnh, TP.HCM");
        room1.put("area", "24 m²");
        room1.put("price", "3.000.000");
        room1.put("status", "Đang thuê");
        room1.put("landlordId", "ID_CHU_TRO_CUA_BAN");
        room1.put("tenantName", "Lê Quang Quý");
        room1.put("tenantPhone", "0912345678");

        // Dữ liệu Mẫu 2: Phòng còn trống (Phòng lân cận)
        Map<String, Object> room2 = new HashMap<>();
        room2.put("id", "room_02");
        room2.put("name", "Phòng B201");
        room2.put("location", "Quận 1, TP.HCM");
        room2.put("area", "20 m²");
        room2.put("price", "2.500.000");
        room2.put("status", "Còn trống");
        room2.put("landlordId", "ID_CHU_TRO_CUA_BAN");
        room2.put("tenantName", "");
        room2.put("tenantPhone", "");

        db.collection("rooms").document("room_01").set(room1);
        db.collection("rooms").document("room_02").set(room2);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Có thể comment dòng này lại nếu đã tạo xong dữ liệu mẫu trên Firebase
        // createSampleRoomsQuickly();

        // Khởi tạo Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // 1. Cài đặt chữ, nhãn đổi sang giao diện Người thuê
        setupTenantUI();

        // 2. Thiết lập RecyclerView cho danh sách phòng lân cận
        setupRoomRecyclerView();

        // 3. Tải dữ liệu thật từ Firebase Firestore
        loadMyRentedRoomForRecyclerView();
    }

    private void setupTenantUI() {
        setText(R.id.tvHomeRole, "NGƯỜI THUÊ");
        setText(R.id.tvGreeting, "Xin chào, Người thuê!");
        setText(R.id.tvHomeSubtitle, "Không gian sống của bạn, trong tầm tay");

        setText(R.id.tvHeroTitle, "Quản lý phòng thuê");
        setText(R.id.tvHeroSubtitle, "Theo dõi phòng đang thuê và các dịch vụ tiện ích.");
        setText(R.id.btnHomeViewRooms, "Xem danh sách phòng trống");

        setText(R.id.tvStat1, "01");
        setText(R.id.tvStat2, "01");
        setText(R.id.tvStat3, "00");

        setText(R.id.tvStatLabel1, "Phòng của tôi");
        setText(R.id.tvStatLabel2, "Hóa đơn mới");
        setText(R.id.tvStatLabel3, "Sự cố");

        // Tiêu đề phần danh sách phía dưới
        setText(R.id.tvRoomSectionTitle, "Phòng của tôi");

        // Bắt sự kiện bấm nút xem danh sách phòng trống chuyển sang TenantRoomListActivity
        findViewById(R.id.btnHomeViewRooms).setOnClickListener(v -> {
            Intent intent = new Intent(TenantHomeActivity.this, TenantRoomListActivity.class);
            startActivity(intent);
        });
    }

    private void setupRoomRecyclerView() {
        recyclerViewAvailableRooms = findViewById(R.id.recyclerViewAvailableRooms);
        if (recyclerViewAvailableRooms != null) {
            recyclerViewAvailableRooms.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
            availableRoomList = new ArrayList<>();

            // Truyền tham số `true` vào constructor để ẩn nút Xóa (quyền của người thuê)
            roomAdapter = new RoomAdapter(availableRoomList, true, new RoomAdapter.OnRoomActionListener() {
                @Override
                public void onView(Room room) {
                    Intent intent = new Intent(TenantHomeActivity.this, RoomDetailActivity.class);
                    intent.putExtra(RoomDetailActivity.EXTRA_ROOM, room);
                    startActivity(intent);
                }

                @Override
                public void onDelete(Room room) {
                    // Người thuê không có quyền xoá phòng
                }
            });
            recyclerViewAvailableRooms.setAdapter(roomAdapter);
        }
    }

    private void loadMyRentedRoomForRecyclerView() {
        if (recyclerViewAvailableRooms == null) return;

        // Lấy UID của tài khoản đang đăng nhập hiện tại
        String currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "";

        if (currentUserId.isEmpty()) {
            Toast.makeText(this, "Vui lòng đăng nhập lại", Toast.LENGTH_SHORT).show();
            return;
        }

        // Truy vấn phòng đang thuê khớp với ID của người dùng hiện tại
        // (Giả sử trên Firestore trường lưu ID người thuê là "tenantId" hoặc "userId")
        db.collection("rooms")
                .whereEqualTo("tenantId", currentUserId)
                .whereEqualTo("status", "Đang thuê")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    availableRoomList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Room room = document.toObject(Room.class);
                        availableRoomList.add(room);
                    }
                    if (roomAdapter != null) {
                        roomAdapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi tải thông tin phòng: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void setText(int id, String text) {
        TextView view = findViewById(id);
        if (view != null) {
            view.setText(text);
        }
    }
}