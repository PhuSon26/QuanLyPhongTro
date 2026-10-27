package com.example.renthub.ui.home;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.renthub.R;
import com.example.renthub.ui.auth.EditProfileActivity;
import com.example.renthub.ui.room.RentalRequest;
import com.example.renthub.ui.room.RentalRequestAdapter;
import com.example.renthub.ui.room.Room;
import com.example.renthub.ui.room.RoomAdapter;
import com.example.renthub.ui.room.RoomDetailActivity;
import com.example.renthub.ui.room.TenantRentedRoomsActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class TenantHomeActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private TextView tvAvailableRoomsTitle;
    private RecyclerView recyclerViewAvailableRooms;
    private TextView tvEmptyAvailableRooms;
    private RoomAdapter roomAdapter;
    private final List<Room> availableRoomList = new ArrayList<>();

    // Khu vực hiển thị kết quả yêu cầu thuê phòng (Dành cho người thuê)
    private View layoutRentalRequestsSection;
    private RecyclerView rvMyRentalRequests;
    private RentalRequestAdapter rentalRequestAdapter;
    private final List<RentalRequest> myRentalRequests = new ArrayList<>();

    private View tvRoomSectionTitle;
    private MaterialCardView cardRecentRoom1;
    private View cardRecentRoom2;
    private MaterialButton btnHomeViewRooms;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        initViews();
        setupTenantUI();
        setupRentalRequestsRecyclerView();
        setupRoomRecyclerView();
        setupListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadMyRentedRoomInfo();
        loadMyRentalRequests();
        loadAvailableRoomsFromFirebase();
    }

    private void initViews() {
        tvRoomSectionTitle = findViewById(R.id.tvRoomSectionTitle);
        cardRecentRoom1 = findViewById(R.id.cardRecentRoom1);
        cardRecentRoom2 = findViewById(R.id.cardRecentRoom2);
        btnHomeViewRooms = findViewById(R.id.btnHomeViewRooms);

        tvAvailableRoomsTitle = findViewById(R.id.tvAvailableRoomsTitle);
        recyclerViewAvailableRooms = findViewById(R.id.recyclerViewAvailableRooms);
        tvEmptyAvailableRooms = findViewById(R.id.tvEmptyAvailableRooms);

        layoutRentalRequestsSection = findViewById(R.id.layoutRentalRequestsSection);
        rvMyRentalRequests = findViewById(R.id.rvMyRentalRequests);

        // Ẩn mục phòng gần đây / phòng tĩnh của giao diện cũ ở trang chủ
        if (tvRoomSectionTitle != null) {
            tvRoomSectionTitle.setVisibility(View.GONE);
        }
        if (cardRecentRoom1 != null) {
            cardRecentRoom1.setVisibility(View.GONE);
        }
        if (cardRecentRoom2 != null) {
            cardRecentRoom2.setVisibility(View.GONE);
        }
    }

    private void setupTenantUI() {
        setText(R.id.tvHomeRole, "NGƯỜI THUÊ");

        // Hiển thị tên người dùng nếu có truyền từ Intent hoặc Firebase
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("FULL_NAME")) {
            String fullName = intent.getStringExtra("FULL_NAME");
            setText(R.id.tvGreeting, "Xin chào, " + fullName + "!");
        } else {
            FirebaseUser user = auth.getCurrentUser();
            if (user != null && user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
                setText(R.id.tvGreeting, "Xin chào, " + user.getDisplayName() + "!");
            } else {
                setText(R.id.tvGreeting, "Xin chào, Người thuê!");
            }
        }

        setText(R.id.tvHomeSubtitle, "Không gian sống của bạn, trong tầm tay");

        setText(R.id.tvHeroTitle, "Quản lý phòng thuê");
        setText(R.id.tvHeroSubtitle, "Theo dõi phòng đang thuê và các dịch vụ tiện ích.");
        if (btnHomeViewRooms != null) {
            btnHomeViewRooms.setText("Phòng của tôi");
        }

        setText(R.id.tvStat1, "00");
        setText(R.id.tvStat2, "00");
        setText(R.id.tvStat3, "00");

        setText(R.id.tvStatLabel1, "Phòng của tôi");
        setText(R.id.tvStatLabel2, "Hóa đơn");
        setText(R.id.tvStatLabel3, "Sự cố");

        // Tiêu đề cho danh sách phòng trống ở dưới: "Phòng trọ gần đây"
        if (tvAvailableRoomsTitle != null) {
            tvAvailableRoomsTitle.setVisibility(View.VISIBLE);
        }
    }

    private void setupRentalRequestsRecyclerView() {
        if (rvMyRentalRequests != null) {
            rvMyRentalRequests.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
            rentalRequestAdapter = new RentalRequestAdapter(myRentalRequests, request -> {
                if (request.getRoomId() != null && !request.getRoomId().isEmpty()) {
                    db.collection("rooms").document(request.getRoomId()).get()
                            .addOnSuccessListener(doc -> {
                                if (doc.exists()) {
                                    Room room = doc.toObject(Room.class);
                                    if (room != null) {
                                        room.setId(doc.getId());
                                        Intent intent = new Intent(TenantHomeActivity.this, RoomDetailActivity.class);
                                        intent.putExtra(RoomDetailActivity.EXTRA_ROOM, room);
                                        startActivity(intent);
                                    }
                                } else {
                                    Toast.makeText(TenantHomeActivity.this, "Yêu cầu: " + request.getRoomName(),
                                            Toast.LENGTH_SHORT).show();
                                }
                            });
                }
            });
            rvMyRentalRequests.setAdapter(rentalRequestAdapter);
        }
    }

    private void setupRoomRecyclerView() {
        if (recyclerViewAvailableRooms != null) {
            recyclerViewAvailableRooms
                    .setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
            // isLandlord = false để người thuê không nhìn thấy nút Xóa phòng
            roomAdapter = new RoomAdapter(availableRoomList, false, new RoomAdapter.OnRoomActionListener() {
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
            recyclerViewAvailableRooms.setVisibility(View.VISIBLE);
        }
    }

    private void setupListeners() {
        // Nút hoặc thẻ "Phòng của tôi": Mở màn hình danh sách các phòng mình đang thuê
        View.OnClickListener openMyRoomsListener = v -> {
            Intent intent = new Intent(TenantHomeActivity.this, TenantRentedRoomsActivity.class);
            startActivity(intent);
        };

        if (btnHomeViewRooms != null) {
            btnHomeViewRooms.setOnClickListener(openMyRoomsListener);
        }

        // Bắt sự kiện menu bottom nav: Nút Phòng trọ -> Chuyển sang màn hình Phòng đang
        // thuê của tôi
        View btnNavRooms = findViewById(R.id.btnNavRooms);
        if (btnNavRooms != null) {
            btnNavRooms.setOnClickListener(openMyRoomsListener);
        }

        // Bắt sự kiện menu bottom nav: Chuyển sang màn hình Cài đặt & Tài khoản
        View btnNavSettings = findViewById(R.id.btnNavSettings);
        if (btnNavSettings != null) {
            btnNavSettings.setOnClickListener(v -> {
                Intent intent = new Intent(TenantHomeActivity.this,
                        com.example.renthub.ui.settings.SettingsActivity.class);
                startActivity(intent);
            });
        }
    }

    private void loadMyRentedRoomInfo() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null)
            return;

        String currentUserId = currentUser.getUid();

        // Cập nhật số lượng phòng đã thuê lên thẻ thống kê (Phòng của tôi)
        db.collection("rooms")
                .whereEqualTo("tenantId", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int rentedCount = queryDocumentSnapshots != null ? queryDocumentSnapshots.size() : 0;
                    setText(R.id.tvStat1, String.format(java.util.Locale.getDefault(), "%02d", rentedCount));
                })
                .addOnFailureListener(e -> {
                    // Xử lý lỗi tải dữ liệu
                });
    }

    private void loadMyRentalRequests() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null)
            return;

        db.collection("rental_requests")
                .whereEqualTo("tenantId", currentUser.getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    myRentalRequests.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        RentalRequest request = doc.toObject(RentalRequest.class);
                        if (request != null) {
                            request.setId(doc.getId());
                            myRentalRequests.add(request);
                        }
                    }

                    if (rentalRequestAdapter != null) {
                        rentalRequestAdapter.notifyDataSetChanged();
                    }

                    if (layoutRentalRequestsSection != null) {
                        layoutRentalRequestsSection
                                .setVisibility(myRentalRequests.isEmpty() ? View.GONE : View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    // Xử lý khi lỗi hoặc không thể tải danh sách yêu cầu
                });
    }

    private void loadAvailableRoomsFromFirebase() {
        db.collection("rooms")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    availableRoomList.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            Room room = document.toObject(Room.class);
                            if (room != null) {
                                room.setId(document.getId());

                                String status = room.getStatus() != null ? room.getStatus().toLowerCase() : "";
                                boolean isMaintenance = status.contains("trì") || status.contains("maintenance");
                                boolean isFull = room.isFull() || status.contains("đủ") || status.contains("hết");

                                // Phòng còn có thể đăng ký được: Không bảo trì và chưa đủ số người tối đa (kể cả phòng còn trống hoặc đang thuê 1 phần)
                                boolean isAvailable = !isMaintenance && !isFull;

                                if (isAvailable) {
                                    availableRoomList.add(room);
                                }
                            }
                        } catch (Exception ignored) {
                            // Bỏ qua nếu có tài liệu phòng định dạng sai
                        }
                    }

                    if (roomAdapter != null) {
                        roomAdapter.notifyDataSetChanged();
                    }

                    if (tvEmptyAvailableRooms != null) {
                        tvEmptyAvailableRooms.setVisibility(availableRoomList.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                })
                .addOnFailureListener(e -> Toast
                        .makeText(this, "Lỗi tải danh sách phòng: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void setText(int id, String text) {
        TextView view = findViewById(id);
        if (view != null) {
            view.setText(text);
        }
    }
}