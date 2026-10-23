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

public class LandlordHomeActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private RecyclerView rvRooms;
    private RoomAdapter roomAdapter;
    private List<Room> roomList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landlord_room_list); // Đảm bảo tên file layout xml khớp với file của bạn

        // Khởi tạo Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // 1. Thiết lập RecyclerView cho Chủ trọ
        setupRoomRecyclerView();

        // 2. Tải danh sách phòng từ Firestore
        loadLandlordRoomsFromFirebase();
    }

    private void setupRoomRecyclerView() {
        rvRooms = findViewById(R.id.rvRooms);
        if (rvRooms != null) {
            rvRooms.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
            roomList = new ArrayList<>();

            // Truyền `isTenant = false` để chủ trọ hiển thị đầy đủ nút Xóa và các tính năng quản lý
            roomAdapter = new RoomAdapter(roomList, false, new RoomAdapter.OnRoomActionListener() {
                @Override
                public void onView(Room room) {
                    Intent intent = new Intent(LandlordHomeActivity.this, RoomDetailActivity.class);
                    intent.putExtra(RoomDetailActivity.EXTRA_ROOM, room);
                    startActivity(intent);
                }

                @Override
                public void onDelete(Room room) {
                    // Xử lý sự kiện xóa phòng trực tiếp trên Firestore
                    if (room.getId() != null) {
                        db.collection("rooms").document(room.getId())
                                .delete()
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(LandlordHomeActivity.this, "Đã xóa phòng thành công", Toast.LENGTH_SHORT).show();
                                    loadLandlordRoomsFromFirebase(); // Tải lại danh sách
                                })
                                .addOnFailureListener(e -> Toast.makeText(LandlordHomeActivity.this, "Lỗi khi xóa: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    }
                }
            });
            rvRooms.setAdapter(roomAdapter);
        }
    }

    private void loadLandlordRoomsFromFirebase() {
        // Truy vấn toàn bộ danh sách phòng (hoặc lọc theo landlordId nếu cần)
        db.collection("rooms")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    roomList.clear();
                    int total = 0;
                    int occupied = 0;
                    int available = 0;

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Room room = document.toObject(Room.class);
                        roomList.add(room);

                        total++;
                        String status = room.getStatus();
                        if ("Đang thuê".equals(status)) {
                            occupied++;
                        } else if ("Còn trống".equals(status) || "Trống".equals(status)) {
                            available++;
                        }
                    }

                    if (roomAdapter != null) {
                        roomAdapter.notifyDataSetChanged();
                    }

                    // Cập nhật số liệu thống kê lên các thẻ tổng quan ở đầu màn hình
                    setText(R.id.tvTotalRooms, String.format("%02d", total));
                    setText(R.id.tvOccupiedRooms, String.format("%02d", occupied));
                    setText(R.id.tvAvailableRooms, String.format("%02d", available));
                    setText(R.id.tvRoomCount, total + " phòng");
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi tải danh sách phòng: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void setText(int id, String text) {
        TextView view = findViewById(id);
        if (view != null) {
            view.setText(text);
        }
    }
}