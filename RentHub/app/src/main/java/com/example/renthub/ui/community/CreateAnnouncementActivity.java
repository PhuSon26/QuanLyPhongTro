package com.example.renthub.ui.community;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.renthub.R;
import com.example.renthub.ui.notification.NotificationHelper;
import com.example.renthub.ui.room.Room;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Màn hình Đăng thông báo mới cho Chủ trọ:
 * - Hỗ trợ gửi thông báo cho Toàn bộ người thuê hoặc chọn từng phòng cụ thể.
 * - Lưu bài đăng vào collection 'announcements' trên Cloud Firestore.
 * - Tự động phát thông báo trong ứng dụng đến những người thuê thuộc phạm vi nhận.
 */
public class CreateAnnouncementActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private TextInputEditText edtAnnouncementTitle, edtAnnouncementContent;
    private AutoCompleteTextView actvCategory;

    private RadioGroup rgTargetScope;
    private RadioButton rbTargetAll, rbTargetSpecific;
    private LinearLayout layoutSpecificRooms;
    private MaterialCheckBox cbSelectAllRooms;
    private TextView tvSelectedRoomsCount;
    private ProgressBar pbLoadingRooms;
    private TextView tvEmptyRooms;
    private RecyclerView rvRoomsToSelect;

    private MaterialButton btnSubmitAnnouncement;
    private ProgressBar pbSubmitting;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String currentUserId = "";
    private String currentUserName = "Chủ trọ";

    private final List<Room> landlordRooms = new ArrayList<>();
    private RoomSelectAdapter roomSelectAdapter;

    private final String[] categories = new String[]{
            "Thông báo chung",
            "Khẩn cấp",
            "Nhắc nhở tiền phòng",
            "Bảo trì - Sửa chữa",
            "Nội quy phòng trọ",
            "Khác"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_announcement);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            currentUserId = user.getUid();
        } else {
            Toast.makeText(this, "Vui lòng đăng nhập lại!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupCategoryDropdown();
        setupScopeListeners();
        setupRoomsRecyclerView();
        loadLandlordProfileAndRooms();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        edtAnnouncementTitle = findViewById(R.id.edtAnnouncementTitle);
        edtAnnouncementContent = findViewById(R.id.edtAnnouncementContent);
        actvCategory = findViewById(R.id.actvCategory);

        rgTargetScope = findViewById(R.id.rgTargetScope);
        rbTargetAll = findViewById(R.id.rbTargetAll);
        rbTargetSpecific = findViewById(R.id.rbTargetSpecific);
        layoutSpecificRooms = findViewById(R.id.layoutSpecificRooms);
        cbSelectAllRooms = findViewById(R.id.cbSelectAllRooms);
        tvSelectedRoomsCount = findViewById(R.id.tvSelectedRoomsCount);
        pbLoadingRooms = findViewById(R.id.pbLoadingRooms);
        tvEmptyRooms = findViewById(R.id.tvEmptyRooms);
        rvRoomsToSelect = findViewById(R.id.rvRoomsToSelect);

        btnSubmitAnnouncement = findViewById(R.id.btnSubmitAnnouncement);
        pbSubmitting = findViewById(R.id.pbSubmitting);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (btnSubmitAnnouncement != null) {
            btnSubmitAnnouncement.setOnClickListener(v -> submitAnnouncement());
        }
    }

    private void setupCategoryDropdown() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, categories);
        actvCategory.setAdapter(adapter);
        actvCategory.setText(categories[0], false);
    }

    private void setupScopeListeners() {
        rgTargetScope.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbTargetSpecific) {
                layoutSpecificRooms.setVisibility(View.VISIBLE);
            } else {
                layoutSpecificRooms.setVisibility(View.GONE);
            }
        });

        cbSelectAllRooms.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (roomSelectAdapter != null) {
                roomSelectAdapter.selectAll(isChecked);
            }
        });
    }

    private void setupRoomsRecyclerView() {
        rvRoomsToSelect.setLayoutManager(new LinearLayoutManager(this));
        roomSelectAdapter = new RoomSelectAdapter(landlordRooms, selectedCount -> {
            tvSelectedRoomsCount.setText("Đã chọn: " + selectedCount + " phòng");
            cbSelectAllRooms.setOnCheckedChangeListener(null);
            cbSelectAllRooms.setChecked(selectedCount > 0 && selectedCount == landlordRooms.size());
            setupScopeListeners();
        });
        rvRoomsToSelect.setAdapter(roomSelectAdapter);
    }

    /**
     * Tải thông tin chủ trọ và danh sách các phòng thuộc quyền quản lý
     */
    private void loadLandlordProfileAndRooms() {
        // 1. Tải tên chủ trọ
        db.collection("users").document(currentUserId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("fullName");
                        if (name != null && !name.trim().isEmpty()) {
                            currentUserName = name.trim();
                        }
                    }
                });

        // 2. Tải danh sách phòng
        pbLoadingRooms.setVisibility(View.VISIBLE);
        db.collection("rooms")
                .whereEqualTo("landlordId", currentUserId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    pbLoadingRooms.setVisibility(View.GONE);
                    landlordRooms.clear();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Room room = doc.toObject(Room.class);
                        if (room != null) {
                            room.setId(doc.getId());
                            landlordRooms.add(room);
                        }
                    }

                    if (roomSelectAdapter != null) {
                        roomSelectAdapter.notifyDataSetChanged();
                    }

                    if (tvEmptyRooms != null) {
                        tvEmptyRooms.setVisibility(landlordRooms.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    pbLoadingRooms.setVisibility(View.GONE);
                    Toast.makeText(this, "Lỗi tải phòng: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Kiểm tra dữ liệu và thực hiện đăng thông báo lên Firestore
     */
    private void submitAnnouncement() {
        String title = edtAnnouncementTitle.getText() != null ? edtAnnouncementTitle.getText().toString().trim() : "";
        String category = actvCategory.getText() != null ? actvCategory.getText().toString().trim() : "";
        String content = edtAnnouncementContent.getText() != null ? edtAnnouncementContent.getText().toString().trim() : "";

        if (title.isEmpty()) {
            edtAnnouncementTitle.setError("Vui lòng nhập tiêu đề thông báo");
            edtAnnouncementTitle.requestFocus();
            return;
        }

        if (category.isEmpty()) {
            actvCategory.setError("Vui lòng chọn loại thông báo");
            actvCategory.requestFocus();
            return;
        }

        if (content.isEmpty()) {
            edtAnnouncementContent.setError("Vui lòng nhập nội dung thông báo");
            edtAnnouncementContent.requestFocus();
            return;
        }

        boolean isSpecific = rbTargetSpecific.isChecked();
        List<String> targetRoomIds = new ArrayList<>();
        List<String> targetRoomNames = new ArrayList<>();
        List<String> recipientTenantIds = new ArrayList<>();

        if (isSpecific) {
            List<Room> selectedRooms = roomSelectAdapter.getSelectedRooms();
            if (selectedRooms.isEmpty()) {
                Toast.makeText(this, "Vui lòng chọn ít nhất 1 phòng nhận thông báo!", Toast.LENGTH_SHORT).show();
                return;
            }

            for (Room r : selectedRooms) {
                targetRoomIds.add(r.getId());
                targetRoomNames.add(r.getName() != null ? r.getName() : "Phòng " + r.getId());
                if (r.getTenantId() != null && !r.getTenantId().trim().isEmpty()) {
                    recipientTenantIds.add(r.getTenantId().trim());
                }
            }
        } else {
            // Trường hợp gửi toàn bộ: nạp tất cả tenantId của các phòng đang có người thuê
            for (Room r : landlordRooms) {
                if (r.getTenantId() != null && !r.getTenantId().trim().isEmpty()) {
                    recipientTenantIds.add(r.getTenantId().trim());
                }
            }
        }

        btnSubmitAnnouncement.setEnabled(false);
        btnSubmitAnnouncement.setText("Đang đăng thông báo...");
        pbSubmitting.setVisibility(View.VISIBLE);

        String announcementId = UUID.randomUUID().toString();
        Announcement announcement = new Announcement(
                announcementId,
                currentUserId,
                currentUserName,
                title,
                content,
                category,
                isSpecific ? Announcement.TARGET_SPECIFIC_ROOMS : Announcement.TARGET_ALL,
                targetRoomIds,
                targetRoomNames,
                System.currentTimeMillis()
        );

        db.collection("announcements").document(announcementId)
                .set(announcement)
                .addOnSuccessListener(aVoid -> {
                    // Phát thông báo cá nhân đến người thuê tương ứng
                    NotificationHelper.sendAnnouncementNotification(CreateAnnouncementActivity.this, announcement, recipientTenantIds);

                    Toast.makeText(CreateAnnouncementActivity.this, "Đăng thông báo thành công!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e -> {
                    pbSubmitting.setVisibility(View.GONE);
                    btnSubmitAnnouncement.setEnabled(true);
                    btnSubmitAnnouncement.setText("Đăng thông báo ngay");
                    Toast.makeText(CreateAnnouncementActivity.this, "Lỗi đăng thông báo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}

