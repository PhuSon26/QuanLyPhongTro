package com.example.renthub.ui.room;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.renthub.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class TenantRentedRoomsActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private TextView tvRentedRoomsCountSummary;
    private RecyclerView rvRentedRooms;
    private LinearLayout layoutEmptyRentedRooms;
    private MaterialButton btnExploreRooms;

    private TenantRentedRoomAdapter adapter;
    private final List<Room> rentedRoomList = new ArrayList<>();

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tenant_rented_rooms);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        initViews();
        setupRecyclerView();
        setupListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRentedRooms();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tvRentedRoomsCountSummary = findViewById(R.id.tvRentedRoomsCountSummary);
        rvRentedRooms = findViewById(R.id.rvRentedRooms);
        layoutEmptyRentedRooms = findViewById(R.id.layoutEmptyRentedRooms);
        btnExploreRooms = findViewById(R.id.btnExploreRooms);
    }

    private void setupRecyclerView() {
        rvRentedRooms.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TenantRentedRoomAdapter(rentedRoomList, room -> {
            Intent intent = new Intent(TenantRentedRoomsActivity.this, RoomDetailActivity.class);
            intent.putExtra(RoomDetailActivity.EXTRA_ROOM, room);
            startActivity(intent);
        });
        rvRentedRooms.setAdapter(adapter);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnExploreRooms.setOnClickListener(v -> finish());
    }

    private void loadRentedRooms() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        String currentUid = currentUser.getUid();

        db.collection("rooms")
                .whereEqualTo("tenantId", currentUid)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    rentedRoomList.clear();

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Room room = doc.toObject(Room.class);
                        if (room != null) {
                            room.setId(doc.getId());
                            rentedRoomList.add(room);
                        }
                    }

                    int count = rentedRoomList.size();
                    tvRentedRoomsCountSummary.setText("Đang thuê: " + count + " phòng");

                    if (adapter != null) {
                        adapter.notifyDataSetChanged();
                    }

                    if (count > 0) {
                        rvRentedRooms.setVisibility(View.VISIBLE);
                        layoutEmptyRentedRooms.setVisibility(View.GONE);
                    } else {
                        rvRentedRooms.setVisibility(View.GONE);
                        layoutEmptyRentedRooms.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi tải danh sách phòng: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
