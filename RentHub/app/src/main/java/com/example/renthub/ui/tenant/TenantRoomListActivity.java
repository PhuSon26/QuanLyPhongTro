package com.example.renthub.ui.tenant;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.renthub.R;
import com.example.renthub.ui.room.Room;
import com.example.renthub.ui.room.RoomAdapter;
import com.example.renthub.ui.room.RoomDetailActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class TenantRoomListActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private RecyclerView recyclerViewRooms;
    private RoomAdapter roomAdapter;
    private List<Room> roomList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tenant_room_list);

        // Khởi tạo Firebase Firestore
        db = FirebaseFirestore.getInstance();

        // Ánh xạ View
        ImageButton btnBack = findViewById(R.id.btnBack);
        recyclerViewRooms = findViewById(R.id.recyclerViewRooms);

        // Bắt sự kiện quay lại
        btnBack.setOnClickListener(v -> finish());

        // Thiết lập RecyclerView danh sách theo chiều dọc
        recyclerViewRooms.setLayoutManager(new LinearLayoutManager(this));
        roomList = new ArrayList<>();

        // Tận dụng RoomAdapter sẵn có
        roomAdapter = new RoomAdapter(roomList, true, new RoomAdapter.OnRoomActionListener() {
            @Override
            public void onView(Room room) {
                // Mở màn hình Chi tiết phòng khi bấm vào item
                Intent intent = new Intent(TenantRoomListActivity.this, RoomDetailActivity.class);
                intent.putExtra(RoomDetailActivity.EXTRA_ROOM, room);
                startActivity(intent);
            }

            @Override
            public void onDelete(Room room) {
                // Người thuê không có quyền xóa phòng
            }
        });

        recyclerViewRooms.setAdapter(roomAdapter);

        // Tải danh sách phòng còn trống từ Firebase
        loadAvailableRooms();
    }

    private void loadAvailableRooms() {
        // Truy vấn lọc danh sách các phòng có status là "Còn trống"
        db.collection("rooms")
                .whereEqualTo("status", "Còn trống")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    roomList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Room room = document.toObject(Room.class);
                        roomList.add(room);
                    }
                    roomAdapter.notifyDataSetChanged();

                    if (roomList.isEmpty()) {
                        Toast.makeText(this, "Hiện tại không có phòng nào trống", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi tải danh sách phòng: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }
}