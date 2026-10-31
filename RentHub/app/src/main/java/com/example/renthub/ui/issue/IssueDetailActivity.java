package com.example.renthub.ui.issue;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.renthub.R;
import com.example.renthub.ui.room.Room;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Màn hình hiển thị chi tiết sự cố và cho phép Chủ trọ cập nhật tiến độ:
 * - Đồng bộ theo chuẩn Design System của màn hình Danh sách sự cố (Toolbar xanh ngọc, chuẩn CardView, màu chữ, nút bấm).
 * - Hiển thị đầy đủ thông tin: Tiêu đề, Loại sự cố, Trạng thái, Phòng, Người thuê, Mô tả, Hình ảnh.
 * - Phân quyền: Chỉ Chủ trọ quản lý sự cố/phòng này mới thấy khu vực chỉnh sửa.
 * - Cho phép Chủ trọ chọn trạng thái qua Spinner (Chờ xử lý, Đang xử lý, Đã xử lý) và nhập ghi chú phản hồi.
 * - Lưu cập nhật lên Firebase Firestore; Người thuê sử dụng SnapshotListener nên sẽ tự động thấy thay đổi ngay lập tức theo thời gian thực.
 */
public class IssueDetailActivity extends AppCompatActivity {

    // Header Toolbar chuẩn Design System
    private MaterialToolbar toolbar;

    // Card 1: Tổng quan sự cố
    private TextView tvDetailTitle, tvDetailStatus, tvDetailType, tvDetailDate;

    // Card 2: Thông tin phòng & Người thuê
    private TextView tvRoomName, tvRoomLocation;
    private TextView tvTenantName, tvTenantPhone;
    private ImageButton btnCallTenant;

    // Card 3: Mô tả & Hình ảnh
    private TextView tvDetailDescription;
    private MaterialCardView cardImageContainer;
    private ImageView ivDetailImage;
    private TextView tvNoImage;

    // Card 4: Phản hồi từ chủ trọ (Hiển thị cho cả Người thuê và Chủ trọ cùng đọc)
    private TextView tvLandlordNote, tvNoLandlordNote;

    // Card 5: Khu vực cập nhật dành riêng cho Chủ trọ
    private MaterialCardView cardLandlordAction;
    private Spinner spinnerUpdateStatus;
    private TextInputEditText edtLandlordReply;
    private MaterialButton btnSaveUpdate;
    private ProgressBar pbUpdating;

    // Dữ liệu ánh xạ cho Spinner trạng thái
    private final String[] statusDisplayOptions = new String[]{"Chờ xử lý", "Đang xử lý", "Đã xử lý"};
    private final String[] statusCodeMapping = new String[]{"PENDING", "PROCESSING", "DONE"};

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ListenerRegistration issueListener;

    private String issueId = "";
    private Issue currentIssue;
    private Room currentRoom;
    private String currentTenantPhone = "";
    private boolean isUserLandlord = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_issue_detail);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        initViews();
        setupToolbar();
        setupStatusSpinner();
        setupListeners();

        issueId = getIntent().getStringExtra("ISSUE_ID");
        if (issueId != null && !issueId.trim().isEmpty()) {
            checkCurrentUserRole();
            listenToIssueDetails(issueId);
        } else {
            Toast.makeText(this, "Không tìm thấy thông tin sự cố", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /**
     * Ánh xạ các thành phần giao diện
     */
    private void initViews() {
        toolbar = findViewById(R.id.toolbar);

        // Card 1
        tvDetailTitle = findViewById(R.id.tvDetailTitle);
        tvDetailStatus = findViewById(R.id.tvDetailStatus);
        tvDetailType = findViewById(R.id.tvDetailType);
        tvDetailDate = findViewById(R.id.tvDetailDate);

        // Card 2
        tvRoomName = findViewById(R.id.tvRoomName);
        tvRoomLocation = findViewById(R.id.tvRoomLocation);
        tvTenantName = findViewById(R.id.tvTenantName);
        tvTenantPhone = findViewById(R.id.tvTenantPhone);
        btnCallTenant = findViewById(R.id.btnCallTenant);

        // Card 3
        tvDetailDescription = findViewById(R.id.tvDetailDescription);
        cardImageContainer = findViewById(R.id.cardImageContainer);
        ivDetailImage = findViewById(R.id.ivDetailImage);
        tvNoImage = findViewById(R.id.tvNoImage);

        // Card 4
        tvLandlordNote = findViewById(R.id.tvLandlordNote);
        tvNoLandlordNote = findViewById(R.id.tvNoLandlordNote);

        // Card 5
        cardLandlordAction = findViewById(R.id.cardLandlordAction);
        spinnerUpdateStatus = findViewById(R.id.spinnerUpdateStatus);
        edtLandlordReply = findViewById(R.id.edtLandlordReply);
        btnSaveUpdate = findViewById(R.id.btnSaveUpdate);
        pbUpdating = findViewById(R.id.pbUpdating);
    }

    /**
     * Thiết lập Toolbar theo chuẩn Design System
     */
    private void setupToolbar() {
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }
    }

    /**
     * Khởi tạo Spinner danh sách trạng thái sự cố
     */
    private void setupStatusSpinner() {
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                statusDisplayOptions
        );
        spinnerUpdateStatus.setAdapter(statusAdapter);
    }

    /**
     * Cài đặt các sự kiện bấm nút
     */
    private void setupListeners() {
        // Gọi điện cho người thuê
        btnCallTenant.setOnClickListener(v -> {
            if (currentTenantPhone != null && !currentTenantPhone.trim().isEmpty()) {
                Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + currentTenantPhone.trim()));
                startActivity(intent);
            } else {
                Toast.makeText(IssueDetailActivity.this, "Không có số điện thoại người thuê", Toast.LENGTH_SHORT).show();
            }
        });

        // Bấm nút Lưu cập nhật và phản hồi
        btnSaveUpdate.setOnClickListener(v -> saveLandlordUpdatesToFirebase());
    }

    /**
     * Kiểm tra vai trò của người dùng hiện tại (Chủ trọ hay Người thuê)
     */
    private void checkCurrentUserRole() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            cardLandlordAction.setVisibility(View.GONE);
            return;
        }

        db.collection("users").document(currentUser.getUid()).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String role = doc.getString("role");
                        isUserLandlord = role != null && (role.equalsIgnoreCase("LANDLORD")
                                || role.equalsIgnoreCase("CHỦ TRỌ")
                                || role.toLowerCase().contains("chủ"));

                        evaluateLandlordActionVisibility();
                    }
                })
                .addOnFailureListener(e -> evaluateLandlordActionVisibility());
    }

    /**
     * Đánh giá và hiển thị khu vực chỉnh sửa của Chủ trọ:
     * - Chỉ hiển thị khi người dùng là Chủ trọ hoặc sở hữu phòng/sự cố này.
     * - Người thuê trọ sẽ bị ẩn hoàn toàn khu vực này.
     */
    private void evaluateLandlordActionVisibility() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            cardLandlordAction.setVisibility(View.GONE);
            return;
        }

        String uid = currentUser.getUid();
        boolean ownsRoom = currentRoom != null && uid.equals(currentRoom.getLandlordId());
        boolean ownsIssue = currentIssue != null && uid.equals(currentIssue.getLandlordId());

        if (isUserLandlord || ownsRoom || ownsIssue) {
            cardLandlordAction.setVisibility(View.VISIBLE);
        } else {
            cardLandlordAction.setVisibility(View.GONE);
        }
    }

    /**
     * Lắng nghe dữ liệu chi tiết sự cố theo thời gian thực (Realtime SnapshotListener)
     * Đảm bảo khi Chủ trọ cập nhật dữ liệu, phía Người thuê sẽ tự động nhận được thay đổi tức thời.
     */
    private void listenToIssueDetails(String id) {
        issueListener = db.collection("issues").document(id)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Toast.makeText(IssueDetailActivity.this, "Lỗi tải dữ liệu: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (snapshot != null && snapshot.exists()) {
                        Issue issue = snapshot.toObject(Issue.class);
                        if (issue != null) {
                            issue.setIssueId(snapshot.getId());
                            currentIssue = issue;
                            displayIssueData(issue);
                            loadRoomInformation(issue.getRoomId());
                            loadTenantInformation(issue.getTenantId());
                            evaluateLandlordActionVisibility();
                        }
                    }
                });
    }

    /**
     * Hiển thị dữ liệu sự cố lên giao diện
     */
    private void displayIssueData(Issue issue) {
        // Tiêu đề
        tvDetailTitle.setText(issue.getTitle() != null && !issue.getTitle().isEmpty() ? issue.getTitle() : "Sự cố không có tiêu đề");

        // Mô tả chi tiết
        tvDetailDescription.setText(issue.getDescription() != null && !issue.getDescription().isEmpty() ? issue.getDescription() : "Không có mô tả chi tiết.");

        // Loại sự cố
        if (issue.getType() != null && !issue.getType().trim().isEmpty()) {
            tvDetailType.setVisibility(View.VISIBLE);
            tvDetailType.setText("Loại: " + issue.getType().trim());
        } else {
            tvDetailType.setVisibility(View.VISIBLE);
            tvDetailType.setText("Loại: Sự cố chung");
        }

        // Thời gian gửi
        if (issue.getCreatedAt() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            tvDetailDate.setText("Thời gian gửi: " + sdf.format(issue.getCreatedAt().toDate()));
        } else {
            tvDetailDate.setText("Thời gian gửi: Chưa cập nhật");
        }

        // Trạng thái sự cố
        displayStatus(issue.getStatus());

        // Hình ảnh sự cố
        if (issue.getImageUrl() != null && !issue.getImageUrl().trim().isEmpty()) {
            cardImageContainer.setVisibility(View.VISIBLE);
            tvNoImage.setVisibility(View.GONE);
            Glide.with(this)
                    .load(issue.getImageUrl().trim())
                    .fitCenter()
                    .into(ivDetailImage);
        } else {
            cardImageContainer.setVisibility(View.GONE);
            tvNoImage.setVisibility(View.VISIBLE);
        }

        // Khung phản hồi từ chủ trọ
        String note = issue.getLandlordNote();
        if (note != null && !note.trim().isEmpty()) {
            tvLandlordNote.setText(note);
            tvLandlordNote.setVisibility(View.VISIBLE);
            tvNoLandlordNote.setVisibility(View.GONE);
        } else {
            tvLandlordNote.setVisibility(View.GONE);
            tvNoLandlordNote.setVisibility(View.VISIBLE);
        }

        // Đồng bộ Spinner và EditText của Chủ trọ theo dữ liệu mới nhất
        syncLandlordActionForm(issue.getStatus(), note);
    }

    /**
     * Hiển thị trạng thái với badge bo tròn và màu sắc tương ứng
     */
    private void displayStatus(String rawStatus) {
        String s = rawStatus != null ? rawStatus.trim().toUpperCase() : "PENDING";

        if (s.contains("DONE") || s.contains("RESOLVED") || s.contains("ĐÃ")) {
            tvDetailStatus.setText("Đã xử lý");
            tvDetailStatus.setBackgroundResource(R.drawable.bg_status_done);
        } else if (s.contains("PROCESSING") || s.contains("PROCESS") || s.contains("ĐANG")) {
            tvDetailStatus.setText("Đang xử lý");
            tvDetailStatus.setBackgroundResource(R.drawable.bg_status_processing);
        } else {
            tvDetailStatus.setText("Chờ xử lý");
            tvDetailStatus.setBackgroundResource(R.drawable.bg_status_pending);
        }
    }

    /**
     * Đồng bộ dữ liệu hiện tại vào Spinner trạng thái và EditText ghi chú của Chủ trọ
     */
    private void syncLandlordActionForm(String status, String note) {
        String s = status != null ? status.trim().toUpperCase() : "PENDING";

        // Chọn vị trí Spinner tương ứng
        if (s.contains("DONE") || s.contains("RESOLVED") || s.contains("ĐÃ")) {
            spinnerUpdateStatus.setSelection(2); // "Đã xử lý"
        } else if (s.contains("PROCESSING") || s.contains("PROCESS") || s.contains("ĐANG")) {
            spinnerUpdateStatus.setSelection(1); // "Đang xử lý"
        } else {
            spinnerUpdateStatus.setSelection(0); // "Chờ xử lý"
        }

        // Điền phản hồi nếu người dùng chưa chủ động gõ nội dung mới
        if (note != null && !note.trim().isEmpty()) {
            if (edtLandlordReply.getText() == null || edtLandlordReply.getText().toString().trim().isEmpty()
                    || !edtLandlordReply.hasFocus()) {
                edtLandlordReply.setText(note);
            }
        }
    }

    /**
     * Tải thông tin phòng trọ từ Firestore
     */
    private void loadRoomInformation(String roomId) {
        if (roomId == null || roomId.trim().isEmpty()) {
            tvRoomName.setText("Phòng: Chưa gán phòng");
            tvRoomLocation.setText("Vị trí: Không rõ");
            return;
        }

        db.collection("rooms").document(roomId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Room room = doc.toObject(Room.class);
                        if (room != null) {
                            currentRoom = room;

                            String name = room.getName() != null && !room.getName().isEmpty()
                                    ? room.getName()
                                    : "Phòng " + roomId;
                            tvRoomName.setText(name);

                            String location = room.getLocation() != null && !room.getLocation().isEmpty()
                                    ? room.getLocation()
                                    : "Địa chỉ: Đang cập nhật";
                            tvRoomLocation.setText(location);

                            evaluateLandlordActionVisibility();
                        }
                    } else {
                        tvRoomName.setText("Phòng (Mã: " + roomId + ")");
                        tvRoomLocation.setText("Vị trí: Không tìm thấy dữ liệu phòng");
                    }
                })
                .addOnFailureListener(e -> {
                    tvRoomName.setText("Phòng (Mã: " + roomId + ")");
                    tvRoomLocation.setText("Lỗi nạp địa chỉ phòng");
                });
    }

    /**
     * Tải thông tin người báo cáo (Người thuê) từ Firestore
     */
    private void loadTenantInformation(String tenantId) {
        if (tenantId == null || tenantId.trim().isEmpty()) {
            tvTenantName.setText("Người thuê: Chưa xác định");
            tvTenantPhone.setText("SĐT: Chưa cập nhật");
            btnCallTenant.setVisibility(View.GONE);
            return;
        }

        db.collection("users").document(tenantId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("fullName");
                        if (name == null || name.trim().isEmpty()) {
                            name = doc.getString("email");
                        }
                        tvTenantName.setText(name != null ? name : "Người thuê trọ");

                        String phone = doc.getString("phone");
                        if (phone != null && !phone.trim().isEmpty()) {
                            currentTenantPhone = phone.trim();
                            tvTenantPhone.setText("SĐT: " + currentTenantPhone);
                            btnCallTenant.setVisibility(View.VISIBLE);
                        } else {
                            tvTenantPhone.setText("SĐT: Chưa cập nhật");
                            btnCallTenant.setVisibility(View.GONE);
                        }
                    } else {
                        tvTenantName.setText("Người thuê (Mã: " + tenantId + ")");
                        tvTenantPhone.setText("SĐT: Chưa có thông tin");
                        btnCallTenant.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    tvTenantName.setText("Người thuê (Mã: " + tenantId + ")");
                    tvTenantPhone.setText("Lỗi tải thông tin liên hệ");
                    btnCallTenant.setVisibility(View.GONE);
                });
    }

    /**
     * Hàm lưu các thay đổi về trạng thái và ghi chú phản hồi vào document trên Firebase Firestore.
     * Khi lưu thành công, Firebase sẽ phát tín hiệu realtime đến mọi máy đang lắng nghe sự cố này.
     */
    private void saveLandlordUpdatesToFirebase() {
        if (currentIssue == null || issueId == null || issueId.trim().isEmpty()) {
            Toast.makeText(this, "Không tìm thấy mã sự cố để cập nhật!", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1. Lấy trạng thái được chọn từ Spinner
        int selectedPosition = spinnerUpdateStatus.getSelectedItemPosition();
        String selectedStatusCode = "PENDING";
        if (selectedPosition >= 0 && selectedPosition < statusCodeMapping.length) {
            selectedStatusCode = statusCodeMapping[selectedPosition];
        }

        // 2. Lấy ghi chú phản hồi từ EditText
        String replyNote = edtLandlordReply.getText() != null ? edtLandlordReply.getText().toString().trim() : "";

        // 3. Hiển thị vòng xoay đang tải và vô hiệu hóa nút bấm
        pbUpdating.setVisibility(View.VISIBLE);
        btnSaveUpdate.setEnabled(false);

        // 4. Tạo map chứa các trường cần cập nhật
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", selectedStatusCode);
        updates.put("landlordNote", replyNote);
        updates.put("updatedAt", Timestamp.now());

        // 5. Cập nhật vào document tương ứng trong collection 'issues' trên Firebase Firestore
        db.collection("issues").document(issueId).update(updates)
                .addOnSuccessListener(aVoid -> {
                    pbUpdating.setVisibility(View.GONE);
                    btnSaveUpdate.setEnabled(true);
                    Toast.makeText(IssueDetailActivity.this, "Cập nhật sự cố thành công! Người thuê sẽ thấy thay đổi ngay lập tức.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    pbUpdating.setVisibility(View.GONE);
                    btnSaveUpdate.setEnabled(true);
                    Toast.makeText(IssueDetailActivity.this, "Lỗi cập nhật: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (issueListener != null) {
            issueListener.remove();
        }
    }
}
