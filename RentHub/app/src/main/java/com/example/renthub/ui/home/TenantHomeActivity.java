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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class TenantHomeActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private RecyclerView recyclerViewAvailableRooms;
    private RoomAdapter roomAdapter;
    private List<Room> availableRoomList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Khởi tạo Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // 1. Cài đặt chữ, nhãn đổi sang giao diện Người thuê dựa trên layout activity_home.xml hiện tại
        setupTenantUI();

        // 2. Thiết lập RecyclerView cho danh sách phòng lân cận/có sẵn
        setupRoomRecyclerView();

        // 3. Tải dữ liệu thật từ Firebase Firestore
        loadMyRentedRoomInfo();
        loadAvailableRoomsFromFirebase();
    }

    private void setupTenantUI() {
        // Đổi header và tiêu đề thành thông tin Người thuê
        setText(R.id.tvHomeRole, "NGƯỜI THUÊ");
        setText(R.id.tvGreeting, "Xin chào, Người thuê!");
        setText(R.id.tvHomeSubtitle, "Không gian sống của bạn, trong tầm tay");

        setText(R.id.tvHeroTitle, "Quản lý phòng thuê");
        setText(R.id.tvHeroSubtitle, "Theo dõi phòng đang thuê và các dịch vụ tiện ích.");
        setText(R.id.btnHomeViewRooms, "Phòng của tôi");

        // Thay đổi nhãn 3 thẻ thống kê trên giao diện cho phù hợp với người thuê
        setText(R.id.tvStat1, "01");
        setText(R.id.tvStat2, "01");
        setText(R.id.tvStat3, "00");

        setText(R.id.tvStatLabel1, "Phòng của tôi");
        setText(R.id.tvStatLabel2, "Hóa đơn mới");
        setText(R.id.tvStatLabel3, "Sự cố");

        setText(R.id.tvRoomSectionTitle, "Danh sách phòng lân cận");
    }

    private void setupRoomRecyclerView() {
        // Tùy chọn: Nếu bạn muốn chèn RecyclerView vào activity_home để hiển thị danh sách phòng lân cận,
        // hãy đảm bảo trong activity_home.xml có một RecyclerView hoặc dùng các item tĩnh sẵn có.
        recyclerViewAvailableRooms = findViewById(R.id.recyclerViewAvailableRooms);
        if (recyclerViewAvailableRooms != null) {
            recyclerViewAvailableRooms.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            availableRoomList = new ArrayList<>();
            // Tận dụng RoomAdapter sẵn có của bạn
            roomAdapter = new RoomAdapter(availableRoomList, new RoomAdapter.OnRoomActionListener() {
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

    private void loadMyRentedRoomInfo() {
        String currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "";
        if (currentUserId.isEmpty()) return;

        // Truy vấn tìm phòng mà người dùng hiện tại đang thuê trên Firestore (dựa vào tenantId)
        db.collection("rooms")
                .whereEqualTo("tenantId", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        QueryDocumentSnapshot document = (QueryDocumentSnapshot) queryDocumentSnapshots.getDocuments().get(0);
                        Room myRoom = document.toObject(Room.class);

                        // Đổ dữ liệu phòng đang thuê vào thẻ "Phòng 1" (tvRecentRoom1) trên giao diện hiện tại của bạn
                        setText(R.id.tvRecentRoom1, myRoom.getName());
                        setText(R.id.tvRecentRoomDesc1, myRoom.getArea() + " • " + myRoom.getPrice() + "đ/tháng");
                        setText(R.id.tvRecentStatus1, myRoom.getStatus());
                    } else {
                        setText(R.id.tvRecentRoom1, "Chưa thuê phòng nào");
                        setText(R.id.tvRecentRoomDesc1, "Hãy chọn phòng bên dưới");
                        setText(R.id.tvRecentStatus1, "Trống");
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Không thể tải thông tin phòng của bạn", Toast.LENGTH_SHORT).show());
    }

    private void loadAvailableRoomsFromFirebase() {
        if (recyclerViewAvailableRooms == null) return;

        // Truy vấn danh sách các phòng còn trống (status = "Còn trống" hoặc "Trống") để người thuê tham khảo
        db.collection("rooms")
                .whereEqualTo("status", "Còn trống")
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
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi tải danh sách phòng lân cận", Toast.LENGTH_SHORT).show());
    }

    private void setText(int id, String text) {
        TextView view = findViewById(id);
        if (view != null) {
            view.setText(text);
        }
    }
}