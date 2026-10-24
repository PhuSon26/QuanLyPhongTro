package com.example.renthub.ui.room;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.renthub.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LandlordRoomListActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private MaterialButton btnAddRoom, btnFilterAll, btnFilterAvailable, btnFilterOccupied, btnFilterMaintenance;
    private EditText edtSearch;
    private TextView tvTotalCount, tvStatTotal, tvStatOccupied, tvStatAvailable, tvStatMaintenance;
    private RecyclerView rvRooms;
    private NestedScrollView scrollView;

    private RoomAdapter adapter;
    private final List<Room> fullRoomList = new ArrayList<>();
    private final List<Room> displayRoomList = new ArrayList<>();

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
        loadLandlordRooms();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnAddRoom = findViewById(R.id.btnAddRoom);
        edtSearch = findViewById(R.id.edtSearch);
        scrollView = findViewById(R.id.scrollView);

        tvTotalCount = findViewById(R.id.tvTotalCount);
        tvStatTotal = findViewById(R.id.tvStatTotal);
        tvStatOccupied = findViewById(R.id.tvStatOccupied);
        tvStatAvailable = findViewById(R.id.tvStatAvailable);
        tvStatMaintenance = findViewById(R.id.tvStatMaintenance);

        btnFilterAll = findViewById(R.id.btnFilterAll);
        btnFilterAvailable = findViewById(R.id.btnFilterAvailable);
        btnFilterOccupied = findViewById(R.id.btnFilterOccupied);
        btnFilterMaintenance = findViewById(R.id.btnFilterMaintenance);

        rvRooms = findViewById(R.id.rvRooms);
    }

    private void setupRecyclerView() {
        if (rvRooms == null) return;

        adapter = new RoomAdapter(displayRoomList, new RoomAdapter.OnRoomActionListener() {
            @Override
            public void onView(Room room) {
                Intent intent = new Intent(LandlordRoomListActivity.this, RoomDetailActivity.class);
                intent.putExtra(RoomDetailActivity.EXTRA_ROOM, room);
                startActivity(intent);
            }

            @Override
            public void onDelete(Room room) {
                deleteRoom(room);
            }
        });

        rvRooms.setLayoutManager(new LinearLayoutManager(this));
        rvRooms.setAdapter(adapter);
        // TẮT XUNG ĐỘT CUỘN GIỮA RECYCLERVIEW VÀ NESTEDSCROLLVIEW
        rvRooms.setNestedScrollingEnabled(false);
    }

    private void loadLandlordRooms() {
        if (mAuth.getCurrentUser() == null) return;

        String currentUid = mAuth.getCurrentUser().getUid();

        // CHỈ NẠP CÁC PHÒNG DO CHỦ TRỌ NÀY TẠO
        db.collection("rooms")
                .whereEqualTo("landlordId", currentUid)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    fullRoomList.clear();
                    int occupied = 0;
                    int available = 0;
                    int maintenance = 0;

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Room room = doc.toObject(Room.class);
                        if (room != null) {
                            room.setId(doc.getId());
                            fullRoomList.add(room);

                            String status = room.getStatus() != null ? room.getStatus().toLowerCase() : "";
                            if (status.contains("thuê") || status.contains("occupied")) {
                                occupied++;
                            } else if (status.contains("trống") || status.contains("available")) {
                                available++;
                            } else if (status.contains("trì") || status.contains("maintenance")) {
                                maintenance++;
                            }
                        }
                    }

                    int total = fullRoomList.size();

                    if (tvStatTotal != null) tvStatTotal.setText(String.format(Locale.getDefault(), "%02d", total));
                    if (tvStatOccupied != null) tvStatOccupied.setText(String.format(Locale.getDefault(), "%02d", occupied));
                    if (tvStatAvailable != null) tvStatAvailable.setText(String.format(Locale.getDefault(), "%02d", available));
                    if (tvStatMaintenance != null) tvStatMaintenance.setText(String.format(Locale.getDefault(), "%02d", maintenance));

                    filterAndSearch();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi tải danh sách phòng: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void filterAndSearch() {
        String query = (edtSearch != null) ? edtSearch.getText().toString().trim().toLowerCase() : "";
        displayRoomList.clear();

        for (Room room : fullRoomList) {
            boolean matchesFilter;
            String status = room.getStatus() != null ? room.getStatus().toLowerCase() : "";

            switch (currentFilter) {
                case "AVAILABLE":
                    matchesFilter = status.contains("trống") || status.contains("available");
                    break;
                case "OCCUPIED":
                    matchesFilter = status.contains("thuê") || status.contains("occupied");
                    break;
                case "MAINTENANCE":
                    matchesFilter = status.contains("trì") || status.contains("maintenance");
                    break;
                default:
                    matchesFilter = true;
                    break;
            }

            boolean matchesSearch = TextUtils.isEmpty(query) ||
                    (room.getName() != null && room.getName().toLowerCase().contains(query)) ||
                    (room.getLocation() != null && room.getLocation().toLowerCase().contains(query));

            if (matchesFilter && matchesSearch) {
                displayRoomList.add(room);
            }
        }

        if (tvTotalCount != null) {
            tvTotalCount.setText(displayRoomList.size() + " phòng");
        }

        if (adapter != null) {
            adapter.updateList(displayRoomList);
        }
    }

    private void setupListeners() {
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        if (btnAddRoom != null) {
            btnAddRoom.setOnClickListener(v -> {
                Intent intent = new Intent(LandlordRoomListActivity.this, AddEditRoomActivity.class);
                startActivity(intent);
            });
        }

        if (btnFilterAll != null) btnFilterAll.setOnClickListener(v -> setFilter("ALL"));
        if (btnFilterAvailable != null) btnFilterAvailable.setOnClickListener(v -> setFilter("AVAILABLE"));
        if (btnFilterOccupied != null) btnFilterOccupied.setOnClickListener(v -> setFilter("OCCUPIED"));
        if (btnFilterMaintenance != null) btnFilterMaintenance.setOnClickListener(v -> setFilter("MAINTENANCE"));

        if (edtSearch != null) {
            edtSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filterAndSearch(); }
                @Override public void afterTextChanged(Editable s) {}
            });
        }
    }

    private void setFilter(String filter) {
        currentFilter = filter;
        updateFilterButtonUI();
        filterAndSearch();
    }

    private void updateFilterButtonUI() {
        int activeBg = Color.parseColor("#148369");
        int activeText = Color.parseColor("#FFFFFF");
        int inactiveBg = Color.parseColor("#EBF3F0");
        int inactiveText = Color.parseColor("#4A5568");

        setButtonStyle(btnFilterAll, currentFilter.equals("ALL"), activeBg, activeText, inactiveBg, inactiveText);
        setButtonStyle(btnFilterAvailable, currentFilter.equals("AVAILABLE"), activeBg, activeText, inactiveBg, inactiveText);
        setButtonStyle(btnFilterOccupied, currentFilter.equals("OCCUPIED"), activeBg, activeText, inactiveBg, inactiveText);
        setButtonStyle(btnFilterMaintenance, currentFilter.equals("MAINTENANCE"), activeBg, activeText, inactiveBg, inactiveText);
    }

    private void setButtonStyle(MaterialButton btn, boolean isActive, int activeBg, int activeText, int inactiveBg, int inactiveText) {
        if (btn == null) return;
        if (isActive) {
            btn.setBackgroundTintList(ColorStateList.valueOf(activeBg));
            btn.setTextColor(activeText);
        } else {
            btn.setBackgroundTintList(ColorStateList.valueOf(inactiveBg));
            btn.setTextColor(inactiveText);
        }
    }

    private void deleteRoom(Room room) {
        if (room == null || room.getId() == null) return;

        db.collection("rooms").document(room.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Đã xóa phòng thành công!", Toast.LENGTH_SHORT).show();
                    loadLandlordRooms();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi xóa phòng: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}