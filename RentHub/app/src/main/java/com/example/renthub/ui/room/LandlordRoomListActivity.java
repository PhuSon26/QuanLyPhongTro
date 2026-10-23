package com.example.renthub.ui.room;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.renthub.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LandlordRoomListActivity extends AppCompatActivity {

    private ImageButton btnBackRooms;
    private MaterialButton btnAddRoom;
    private TextInputEditText edtSearchRoom;
    private MaterialButton btnFilterAll, btnFilterAvailable, btnFilterOccupied, btnFilterMaintenance;
    private TextView tvRoomCount;
    private RecyclerView rvRooms;

    // THỐNG KÊ
    private TextView tvTotalRooms, tvOccupiedRooms, tvAvailableRooms;

    private RoomAdapter adapter;
    private final List<Room> fullRoomList = new ArrayList<>();
    private final List<Room> filteredRoomList = new ArrayList<>();

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String currentFilter = "ALL";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landlord_room_list);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        initViews();
        setupRecyclerView();
        setupListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRoomsData();
    }

    private void initViews() {
        btnBackRooms = findViewById(R.id.btnBackRooms);
        btnAddRoom = findViewById(R.id.btnAddRoom);
        edtSearchRoom = findViewById(R.id.edtSearchRoom);

        btnFilterAll = findViewById(R.id.btnFilterAll);
        btnFilterAvailable = findViewById(R.id.btnFilterAvailable);
        btnFilterOccupied = findViewById(R.id.btnFilterOccupied);
        btnFilterMaintenance = findViewById(R.id.btnFilterMaintenance);

        tvRoomCount = findViewById(R.id.tvRoomCount);
        rvRooms = findViewById(R.id.rvRooms);

        tvTotalRooms = findViewById(R.id.tvTotalRooms);
        tvOccupiedRooms = findViewById(R.id.tvOccupiedRooms);
        tvAvailableRooms = findViewById(R.id.tvAvailableRooms);
    }

    private void setupRecyclerView() {
        adapter = new RoomAdapter(filteredRoomList, new RoomAdapter.OnRoomActionListener() {
            @Override
            public void onView(Room room) {
                Intent intent = new Intent(LandlordRoomListActivity.this, RoomDetailActivity.class);
                intent.putExtra(RoomDetailActivity.EXTRA_ROOM, room);
                startActivity(intent);
            }

            @Override
            public void onDelete(Room room) {
                confirmDeleteRoom(room);
            }
        });

        rvRooms.setLayoutManager(new LinearLayoutManager(this));
        rvRooms.setAdapter(adapter);
    }

    private void setupListeners() {
        btnBackRooms.setOnClickListener(v -> finish());
        btnAddRoom.setOnClickListener(v -> openAddEditRoomScreen(null));

        btnFilterAll.setOnClickListener(v -> setFilter("ALL"));
        btnFilterAvailable.setOnClickListener(v -> setFilter("AVAILABLE"));
        btnFilterOccupied.setOnClickListener(v -> setFilter("OCCUPIED"));
        btnFilterMaintenance.setOnClickListener(v -> setFilter("MAINTENANCE"));

        edtSearchRoom.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilterAndSearch();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void openAddEditRoomScreen(Room room) {
        Intent intent = new Intent(LandlordRoomListActivity.this, AddEditRoomActivity.class);
        if (room != null) {
            intent.putExtra(AddEditRoomActivity.EXTRA_ROOM, room);
        }
        startActivity(intent);
    }

    private void confirmDeleteRoom(Room room) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa phòng")
                .setMessage("Bạn có chắc chắn muốn xóa " + room.getName() + " không?")
                .setPositiveButton("Xóa", (dialog, which) -> deleteRoomFromFirestore(room))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void deleteRoomFromFirestore(Room room) {
        if (room.getId() == null) return;

        db.collection("rooms").document(room.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Đã xóa phòng thành công!", Toast.LENGTH_SHORT).show();
                    loadRoomsData();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Xóa thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void loadRoomsData() {
        if (mAuth.getCurrentUser() != null) {
            String uid = mAuth.getCurrentUser().getUid();
            db.collection("rooms")
                    .whereEqualTo("landlordId", uid)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        fullRoomList.clear();
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            Room room = doc.toObject(Room.class);
                            if (room != null) {
                                room.setId(doc.getId());
                                fullRoomList.add(room);
                            }
                        }
                        updateSummaryStatistics();
                        applyFilterAndSearch();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Lỗi tải danh sách phòng", Toast.LENGTH_SHORT).show());
        }
    }

    private void updateSummaryStatistics() {
        int total = fullRoomList.size();
        int occupied = 0;
        int available = 0;

        for (Room room : fullRoomList) {
            String status = room.getStatus() != null ? room.getStatus().toLowerCase() : "";
            if (status.contains("thuê") || status.contains("occupied")) {
                occupied++;
            } else if (status.contains("trống") || status.contains("available")) {
                available++;
            }
        }

        if (tvTotalRooms != null) tvTotalRooms.setText(String.format(Locale.getDefault(), "%02d", total));
        if (tvOccupiedRooms != null) tvOccupiedRooms.setText(String.format(Locale.getDefault(), "%02d", occupied));
        if (tvAvailableRooms != null) tvAvailableRooms.setText(String.format(Locale.getDefault(), "%02d", available));
    }

    private void setFilter(String filterType) {
        this.currentFilter = filterType;
        updateFilterButtonsUI();
        applyFilterAndSearch();
    }

    private void updateFilterButtonsUI() {
        resetButtonState(btnFilterAll);
        resetButtonState(btnFilterAvailable);
        resetButtonState(btnFilterOccupied);
        resetButtonState(btnFilterMaintenance);

        switch (currentFilter) {
            case "AVAILABLE":
                setButtonActive(btnFilterAvailable);
                break;
            case "OCCUPIED":
                setButtonActive(btnFilterOccupied);
                break;
            case "MAINTENANCE":
                setButtonActive(btnFilterMaintenance);
                break;
            default:
                setButtonActive(btnFilterAll);
                break;
        }
    }

    private void resetButtonState(MaterialButton btn) {
        btn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E9EFED")));
        btn.setTextColor(Color.parseColor("#63737C"));
    }

    private void setButtonActive(MaterialButton btn) {
        btn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#148369")));
        btn.setTextColor(Color.parseColor("#FFFFFF"));
    }

    private void applyFilterAndSearch() {
        filteredRoomList.clear();
        String searchText = edtSearchRoom.getText() != null ? edtSearchRoom.getText().toString().trim().toLowerCase(Locale.getDefault()) : "";

        for (Room room : fullRoomList) {
            boolean matchesFilter = false;
            String status = room.getStatus() != null ? room.getStatus().toLowerCase() : "";

            if ("ALL".equals(currentFilter)) {
                matchesFilter = true;
            } else if ("AVAILABLE".equals(currentFilter) && (status.contains("trống") || status.contains("available"))) {
                matchesFilter = true;
            } else if ("OCCUPIED".equals(currentFilter) && (status.contains("thuê") || status.contains("occupied"))) {
                matchesFilter = true;
            } else if ("MAINTENANCE".equals(currentFilter) && (status.contains("trì") || status.contains("maintenance"))) {
                matchesFilter = true;
            }

            boolean matchesSearch = searchText.isEmpty()
                    || (room.getName() != null && room.getName().toLowerCase(Locale.getDefault()).contains(searchText))
                    || (room.getLocation() != null && room.getLocation().toLowerCase(Locale.getDefault()).contains(searchText));

            if (matchesFilter && matchesSearch) {
                filteredRoomList.add(room);
            }
        }

        adapter.updateList(filteredRoomList);
        tvRoomCount.setText(filteredRoomList.size() + " phòng");
    }
}