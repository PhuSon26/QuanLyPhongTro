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

    private RecyclerView recyclerViewAvailableRooms;
    private TextView tvEmptyAvailableRooms;
    private RoomAdapter roomAdapter;
    private final List<Room> availableRoomList = new ArrayList<>();

    // Khu vực hiển thị kết quả yêu cầu thuê phòng (Dành cho người thuê)
    private View layoutRentalRequestsSection;
    private RecyclerView rvMyRentalRequests;
    private RentalRequestAdapter rentalRequestAdapter;
    private final List<RentalRequest> myRentalRequests = new ArrayList<>();

    private MaterialCardView cardRecentRoom1;
    private View cardRecentRoom2;
    private MaterialButton btnHomeViewRooms;
    private Room myRentedRoom;

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
        cardRecentRoom1 = findViewById(R.id.cardRecentRoom1);
        cardRecentRoom2 = findViewById(R.id.cardRecentRoom2);
        btnHomeViewRooms = findViewById(R.id.btnHomeViewRooms);
        recyclerViewAvailableRooms = findViewById(R.id.recyclerViewAvailableRooms);
        tvEmptyAvailableRooms = findViewById(R.id.tvEmptyAvailableRooms);

        layoutRentalRequestsSection = findViewById(R.id.layoutRentalRequestsSection);
        rvMyRentalRequests = findViewById(R.id.rvMyRentalRequests);

        // Ẩn thẻ phòng tĩnh số 2 trong giao diện gốc của chủ trọ
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

        setText(R.id.tvRoomSectionTitle, "Danh sách phòng lân cận còn trống");
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
                                    Toast.makeText(TenantHomeActivity.this, "Yêu cầu: " + request.getRoomName(), Toast.LENGTH_SHORT).show();
                                }
                            });
                }
            });
            rvMyRentalRequests.setAdapter(rentalRequestAdapter);
        }
    }

    private void setupRoomRecyclerView() {
        if (recyclerViewAvailableRooms != null) {
            recyclerViewAvailableRooms.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
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
        // Nút hoặc thẻ "Phòng của tôi"
        View.OnClickListener openMyRoomListener = v -> {
            if (myRentedRoom != null) {
                Intent intent = new Intent(TenantHomeActivity.this, RoomDetailActivity.class);
                intent.putExtra(RoomDetailActivity.EXTRA_ROOM, myRentedRoom);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Bạn chưa thuê phòng nào. Hãy chọn phòng bên dưới để đăng ký!", Toast.LENGTH_SHORT).show();
            }
        };

        if (btnHomeViewRooms != null) {
            btnHomeViewRooms.setOnClickListener(openMyRoomListener);
        }

        if (cardRecentRoom1 != null) {
            cardRecentRoom1.setOnClickListener(openMyRoomListener);
        }

        // Bắt sự kiện menu bottom nav: Chuyển sang màn hình Cài đặt & Tài khoản
        View btnNavSettings = findViewById(R.id.btnNavSettings);
        if (btnNavSettings != null) {
            btnNavSettings.setOnClickListener(v -> {
                Intent intent = new Intent(TenantHomeActivity.this, com.example.renthub.ui.settings.SettingsActivity.class);
                startActivity(intent);
            });
        }
    }

    private void loadMyRentedRoomInfo() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) return;

        String currentUserId = currentUser.getUid();

        // 1. Tìm phòng theo tenantId và cập nhật chính xác số lượng phòng đã thuê
        db.collection("rooms")
                .whereEqualTo("tenantId", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int rentedCount = queryDocumentSnapshots.size();
                    setText(R.id.tvStat1, String.format(java.util.Locale.getDefault(), "%02d", rentedCount));

                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                        myRentedRoom = doc.toObject(Room.class);
                        if (myRentedRoom != null) {
                            myRentedRoom.setId(doc.getId());

                            setText(R.id.tvRoomSectionTitle, "Phòng đang thuê của tôi (" + rentedCount + " phòng)");
                            if (cardRecentRoom1 != null) {
                                cardRecentRoom1.setVisibility(View.VISIBLE);
                            }
                            setText(R.id.tvRecentRoom1, myRentedRoom.getName());
                            setText(R.id.tvRecentRoomDesc1, (myRentedRoom.getArea() != null ? myRentedRoom.getArea() : "") +
                                    " • " + (myRentedRoom.getPrice() != null ? myRentedRoom.getPrice() : "") + "đ/tháng");
                            setText(R.id.tvRecentStatus1, "Đang thuê");

                            TextView tvStatus = findViewById(R.id.tvRecentStatus1);
                            if (tvStatus != null) {
                                tvStatus.setTextColor(Color.parseColor("#2563A6"));
                            }
                        }
                    } else {
                        myRentedRoom = null;
                        setText(R.id.tvRoomSectionTitle, "Phòng đang thuê");
                        if (cardRecentRoom1 != null) {
                            cardRecentRoom1.setVisibility(View.GONE);
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Không thể tải thông tin phòng của bạn", Toast.LENGTH_SHORT).show());
    }

    private void loadMyRentalRequests() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) return;

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
                        layoutRentalRequestsSection.setVisibility(myRentalRequests.isEmpty() ? View.GONE : View.VISIBLE);
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
                        Room room = document.toObject(Room.class);
                        if (room != null) {
                            room.setId(document.getId());

                            String status = room.getStatus() != null ? room.getStatus().toLowerCase() : "";
                            boolean isAvailable = status.contains("trống") || (!status.contains("thuê") && !room.isFull());

                            // Chỉ thêm các phòng còn trống vào danh sách gợi ý cho người thuê
                            if (isAvailable) {
                                availableRoomList.add(room);
                            }
                        }
                    }

                    if (roomAdapter != null) {
                        roomAdapter.notifyDataSetChanged();
                    }

                    if (tvEmptyAvailableRooms != null) {
                        tvEmptyAvailableRooms.setVisibility(availableRoomList.isEmpty() ? View.VISIBLE : View.GONE);
                    }
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