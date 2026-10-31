package com.example.renthub.ui.home;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.renthub.R;
import com.example.renthub.ui.room.LandlordRequestAdapter;
import com.example.renthub.ui.room.LandlordRoomListActivity;
import com.example.renthub.ui.room.RentalRequest;
import com.example.renthub.ui.room.Room;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class LandlordHomeActivity extends AppCompatActivity {

    private TextView tvGreeting;
    private MaterialButton btnHomeViewRooms;
    private ImageButton btnHomeNotification;

    // THỐNG KÊ
    private TextView tvStat1, tvStat2, tvStat3;

    // YÊU CẦU THUÊ PHÒNG CHỜ DUYỆT
    private View layoutLandlordRequestsSection;
    private TextView tvLandlordRequestsTitle;
    private RecyclerView rvLandlordRequests;
    private LandlordRequestAdapter landlordRequestAdapter;
    private final List<RentalRequest> pendingRequestList = new ArrayList<>();

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

        // Ánh xạ khu vực yêu cầu thuê phòng cần duyệt
        layoutLandlordRequestsSection = findViewById(R.id.layoutLandlordRequestsSection);
        tvLandlordRequestsTitle = findViewById(R.id.tvLandlordRequestsTitle);
        rvLandlordRequests = findViewById(R.id.rvLandlordRequests);

        if (rvLandlordRequests != null) {
            rvLandlordRequests.setLayoutManager(new LinearLayoutManager(this));
            landlordRequestAdapter = new LandlordRequestAdapter(pendingRequestList, new LandlordRequestAdapter.OnLandlordActionListener() {
                @Override
                public void onApprove(RentalRequest request) {
                    confirmApproveRequest(request);
                }

                @Override
                public void onReject(RentalRequest request) {
                    confirmRejectRequest(request);
                }
            });
            rvLandlordRequests.setAdapter(landlordRequestAdapter);
        }

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
            btnHomeNotification.setOnClickListener(v -> {
                if (!pendingRequestList.isEmpty()) {
                    Toast.makeText(LandlordHomeActivity.this, "Bạn có " + pendingRequestList.size() + " yêu cầu thuê phòng chờ duyệt!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(LandlordHomeActivity.this, "Không có thông báo mới", Toast.LENGTH_SHORT).show();
                }
            });
        }

        View btnNavIssues = findViewById(R.id.btnNavIssues);
        if (btnNavIssues != null) {
            btnNavIssues.setOnClickListener(v -> {
                Intent intent = new Intent(LandlordHomeActivity.this, com.example.renthub.ui.issue.IssueListActivity.class);
package com.example.renthub.ui.home;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.renthub.R;
import com.example.renthub.ui.invoice.InvoiceListActivity;
import com.example.renthub.ui.room.LandlordRequestAdapter;
import com.example.renthub.ui.room.LandlordRoomListActivity;
import com.example.renthub.ui.room.RentalRequest;
import com.example.renthub.ui.room.Room;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class LandlordHomeActivity extends AppCompatActivity {

    private TextView tvGreeting;
    private MaterialButton btnHomeViewRooms;
    private ImageButton btnHomeNotification;

    // THỐNG KÊ (ĐỦ 4 THẺ)
    private TextView tvStat1, tvStat2, tvStat3, tvStat4;

    // YÊU CẦU THUÊ PHÒNG CHỜ DUYỆT
    private View layoutLandlordRequestsSection;
    private TextView tvLandlordRequestsTitle;
    private RecyclerView rvLandlordRequests;
    private LandlordRequestAdapter landlordRequestAdapter;
    private final List<RentalRequest> pendingRequestList = new ArrayList<>();

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

        tvStat1 = findViewById(R.id.tvStat1); // Tổng phòng
        tvStat2 = findViewById(R.id.tvStat2); // Đang thuê
        tvStat3 = findViewById(R.id.tvStat3); // Còn trống
        tvStat4 = findViewById(R.id.tvStat4); // Bảo trì

        layoutLandlordRequestsSection = findViewById(R.id.layoutLandlordRequestsSection);
        tvLandlordRequestsTitle = findViewById(R.id.tvLandlordRequestsTitle);
        rvLandlordRequests = findViewById(R.id.rvLandlordRequests);

        if (rvLandlordRequests != null) {
            rvLandlordRequests.setLayoutManager(new LinearLayoutManager(this));
            landlordRequestAdapter = new LandlordRequestAdapter(pendingRequestList, new LandlordRequestAdapter.OnLandlordActionListener() {
                @Override
                public void onApprove(RentalRequest request) {
                    confirmApproveRequest(request);
                }

                @Override
                public void onReject(RentalRequest request) {
                    confirmRejectRequest(request);
                }
            });
            rvLandlordRequests.setAdapter(landlordRequestAdapter);
        }

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
            btnHomeNotification.setOnClickListener(v -> {
                if (!pendingRequestList.isEmpty()) {
                    Toast.makeText(LandlordHomeActivity.this, "Bạn có " + pendingRequestList.size() + " yêu cầu thuê phòng chờ duyệt!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(LandlordHomeActivity.this, "Không có thông báo mới", Toast.LENGTH_SHORT).show();
                }
            });
        }

        View btnNavInvoice = findViewById(R.id.btnNavInvoice);
        if (btnNavInvoice != null) {
            btnNavInvoice.setOnClickListener(v -> {
                Intent intent = new Intent(LandlordHomeActivity.this, InvoiceListActivity.class);
                startActivity(intent);
            });
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
                    List<String> myRoomIds = new ArrayList<>();
                    int occupied = 0;
                    int available = 0;

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Room room = doc.toObject(Room.class);
                        if (room != null) {
                            room.setId(doc.getId());
                            roomList.add(room);
                            myRoomIds.add(doc.getId());

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
                    loadPendingRentalRequests(uid, myRoomIds);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void loadPendingRentalRequests(String uid, List<String> myRoomIds) {
        db.collection("rental_requests")
                .whereEqualTo("status", "PENDING")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    pendingRequestList.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        RentalRequest req = doc.toObject(RentalRequest.class);
                        if (req != null) {
                            req.setId(doc.getId());
                            boolean isMyRequest = (req.getLandlordId() != null && req.getLandlordId().equals(uid))
                                    || (req.getRoomId() != null && myRoomIds.contains(req.getRoomId()));
                            if (isMyRequest) {
                                pendingRequestList.add(req);
                            }
                        }
                    }

                    if (landlordRequestAdapter != null) {
                        landlordRequestAdapter.notifyDataSetChanged();
                    }

                    if (layoutLandlordRequestsSection != null) {
                        if (!pendingRequestList.isEmpty()) {
                            layoutLandlordRequestsSection.setVisibility(View.VISIBLE);
                            if (tvLandlordRequestsTitle != null) {
                                tvLandlordRequestsTitle.setText("Yêu cầu thuê phòng cần duyệt (" + pendingRequestList.size() + ")");
                            }
                        } else {
                            layoutLandlordRequestsSection.setVisibility(View.GONE);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    // Xử lý im lặng nếu mạng bận
                });
    }

    private void confirmApproveRequest(RentalRequest request) {
        String tenantName = request.getTenantName() != null ? request.getTenantName() : "người thuê này";
        String roomName = request.getRoomName() != null ? request.getRoomName() : "phòng trọ";

        new AlertDialog.Builder(this)
                .setTitle("Duyệt yêu cầu thuê phòng")
                .setMessage("Bạn có chắc chắn đồng ý cho " + tenantName + " thuê " + roomName + " không?\nSau khi duyệt, phòng sẽ chuyển sang trạng thái 'Đang thuê'.")
                .setPositiveButton("Duyệt ngay", (dialog, which) -> executeApproveRequest(request))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void executeApproveRequest(RentalRequest request) {
        if (request.getId() == null || request.getRoomId() == null) return;

        // 1. Cập nhật rental_requests: status = "APPROVED"
        db.collection("rental_requests").document(request.getId())
                .update("status", "APPROVED")
                .addOnSuccessListener(aVoid -> {
                    // 2. Cập nhật room: status = "Đang thuê", tenantId, tenantName, tenantPhone, occupants
                    Map<String, Object> roomUpdates = new HashMap<>();
                    roomUpdates.put("status", "Đang thuê");
                    roomUpdates.put("tenantId", request.getTenantId());
                    roomUpdates.put("tenantName", request.getTenantName());
                    roomUpdates.put("tenantPhone", request.getTenantPhone());
                    roomUpdates.put("occupants", request.getOccupants());

                    db.collection("rooms").document(request.getRoomId())
                            .update(roomUpdates)
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(this, "Đã duyệt yêu cầu thuê phòng thành công!", Toast.LENGTH_SHORT).show();
                                loadDashboardData();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Lỗi cập nhật phòng: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                loadDashboardData();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi duyệt yêu cầu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void confirmRejectRequest(RentalRequest request) {
        String tenantName = request.getTenantName() != null ? request.getTenantName() : "người thuê này";

        new AlertDialog.Builder(this)
                .setTitle("Từ chối yêu cầu thuê phòng")
                .setMessage("Bạn có chắc chắn muốn từ chối yêu cầu thuê phòng của " + tenantName + " không?")
                .setPositiveButton("Từ chối", (dialog, which) -> executeRejectRequest(request))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void executeRejectRequest(RentalRequest request) {
        if (request.getId() == null) return;

        db.collection("rental_requests").document(request.getId())
                .update("status", "REJECTED", "rejectReason", "Chủ trọ đã từ chối yêu cầu thuê")
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Đã từ chối yêu cầu thuê phòng", Toast.LENGTH_SHORT).show();
                    loadDashboardData();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi cập nhật: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
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
                    List<String> myRoomIds = new ArrayList<>();
                    int occupied = 0;
                    int available = 0;
                    int maintenance = 0;

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Room room = doc.toObject(Room.class);
                        if (room != null) {
                            room.setId(doc.getId());
                            roomList.add(room);
                            myRoomIds.add(doc.getId());

                            String status = room.getStatus() != null ? room.getStatus().toLowerCase() : "";
                            if (status.contains("thuê") || status.contains("occupied")) {
                                occupied++;
                            } else if (status.contains("trì") || status.contains("maintenance")) {
                                maintenance++;
                            } else if (status.contains("trống") || status.contains("available")) {
                                available++;
                            }
                        }
                    }

                    int total = roomList.size();

                    if (tvStat1 != null) tvStat1.setText(String.format(Locale.getDefault(), "%02d", total));
                    if (tvStat2 != null) tvStat2.setText(String.format(Locale.getDefault(), "%02d", occupied));
                    if (tvStat3 != null) tvStat3.setText(String.format(Locale.getDefault(), "%02d", available));
                    if (tvStat4 != null) tvStat4.setText(String.format(Locale.getDefault(), "%02d", maintenance));

                    updateRecentRoomsUI(roomList);
                    loadPendingRentalRequests(uid, myRoomIds);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void loadPendingRentalRequests(String uid, List<String> myRoomIds) {
        db.collection("rental_requests")
                .whereEqualTo("status", "PENDING")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    pendingRequestList.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        RentalRequest req = doc.toObject(RentalRequest.class);
                        if (req != null) {
                            req.setId(doc.getId());
                            boolean isMyRequest = (req.getLandlordId() != null && req.getLandlordId().equals(uid))
                                    || (req.getRoomId() != null && myRoomIds.contains(req.getRoomId()));
                            if (isMyRequest) {
                                pendingRequestList.add(req);
                            }
                        }
                    }

                    if (landlordRequestAdapter != null) {
                        landlordRequestAdapter.notifyDataSetChanged();
                    }

                    if (layoutLandlordRequestsSection != null) {
                        if (!pendingRequestList.isEmpty()) {
                            layoutLandlordRequestsSection.setVisibility(View.VISIBLE);
                            if (tvLandlordRequestsTitle != null) {
                                tvLandlordRequestsTitle.setText("Yêu cầu thuê phòng cần duyệt (" + pendingRequestList.size() + ")");
                            }
                        } else {
                            layoutLandlordRequestsSection.setVisibility(View.GONE);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                });
    }

    private void confirmApproveRequest(RentalRequest request) {
        String tenantName = request.getTenantName() != null ? request.getTenantName() : "người thuê này";
        String roomName = request.getRoomName() != null ? request.getRoomName() : "phòng trọ";

        new AlertDialog.Builder(this)
                .setTitle("Duyệt yêu cầu thuê phòng")
                .setMessage("Bạn có chắc chắn đồng ý cho " + tenantName + " thuê " + roomName + " không?\nSau khi duyệt, phòng sẽ chuyển sang trạng thái 'Đang thuê'.")
                .setPositiveButton("Duyệt ngay", (dialog, which) -> executeApproveRequest(request))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void executeApproveRequest(RentalRequest request) {
        if (request.getId() == null || request.getRoomId() == null) return;

        db.collection("rental_requests").document(request.getId())
                .update("status", "APPROVED")
                .addOnSuccessListener(aVoid -> {
                    Map<String, Object> roomUpdates = new HashMap<>();
                    roomUpdates.put("status", "Đang thuê");
                    roomUpdates.put("tenantId", request.getTenantId());
                    roomUpdates.put("tenantName", request.getTenantName());
                    roomUpdates.put("tenantPhone", request.getTenantPhone());
                    roomUpdates.put("occupants", request.getOccupants());

                    db.collection("rooms").document(request.getRoomId())
                            .update(roomUpdates)
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(this, "Đã duyệt yêu cầu thuê phòng thành công!", Toast.LENGTH_SHORT).show();
                                loadDashboardData();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Lỗi cập nhật phòng: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                loadDashboardData();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi duyệt yêu cầu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void confirmRejectRequest(RentalRequest request) {
        String tenantName = request.getTenantName() != null ? request.getTenantName() : "người thuê này";

        new AlertDialog.Builder(this)
                .setTitle("Từ chối yêu cầu thuê phòng")
                .setMessage("Bạn có chắc chắn muốn từ chối yêu cầu thuê phòng của " + tenantName + " không?")
                .setPositiveButton("Từ chối", (dialog, which) -> executeRejectRequest(request))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void executeRejectRequest(RentalRequest request) {
        if (request.getId() == null) return;

        db.collection("rental_requests").document(request.getId())
                .update("status", "REJECTED", "rejectReason", "Chủ trọ đã từ chối yêu cầu thuê")
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Đã từ chối yêu cầu thuê phòng", Toast.LENGTH_SHORT).show();
                    loadDashboardData();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi cập nhật: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateRecentRoomsUI(List<Room> rooms) {
        if (rooms == null || rooms.isEmpty()) {
            if (cardRecentRoom1 != null) cardRecentRoom1.setVisibility(View.GONE);
            if (cardRecentRoom2 != null) cardRecentRoom2.setVisibility(View.GONE);
            return;
        }

        if (cardRecentRoom1 != null) {
            cardRecentRoom1.setVisibility(View.VISIBLE);
            bindRoomDataToView(rooms.get(0), tvRecentRoom1, tvRecentRoomDesc1, tvRecentStatus1);
        }

        if (rooms.size() > 1) {
            if (cardRecentRoom2 != null) {
                cardRecentRoom2.setVisibility(View.VISIBLE);
                bindRoomDataToView(rooms.get(1), tvRecentRoom2, tvRecentRoomDesc2, tvRecentStatus2);
            }
        } else {
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
