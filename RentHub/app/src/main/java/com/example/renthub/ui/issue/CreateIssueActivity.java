package com.example.renthub.ui.issue;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

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
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Màn hình tạo và gửi báo cáo sự cố phòng trọ
 * Đồng bộ chuẩn thiết kế Design System theo giao diện Danh sách sự cố.
 */
public class CreateIssueActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private TextInputEditText edtTitle, edtDescription;
    private AutoCompleteTextView actvIssueType, actvRoom;
    private MaterialButton btnSelectImage, btnSubmit;
    private MaterialCardView cardPreviewContainer;
    private ImageView ivPreview;
    private ProgressBar pbSubmitting;
    private Uri imageUri;

    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private String currentTenantId;
    private String selectedRoomId = "";

    // Trình khởi chạy để chọn ảnh từ thư viện
    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    imageUri = result.getData().getData();
                    ivPreview.setImageURI(imageUri);
                    if (cardPreviewContainer != null) {
                        cardPreviewContainer.setVisibility(View.VISIBLE);
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_issue);

        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        // 1. Lấy ID người dùng ĐẦU TIÊN để tránh bị null khi query Firestore
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            currentTenantId = currentUser.getUid();
        } else {
            Toast.makeText(this, "Vui lòng đăng nhập lại!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Ánh xạ View
        initViews();
        setupToolbar();

        String[] issueTypes = new String[]{"Điện", "Nước", "Thiết bị", "Mạng", "Khác"};
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, issueTypes);
        actvIssueType.setAdapter(typeAdapter);

        // 2. Nạp danh sách phòng (sau khi đã khởi tạo currentTenantId thành công)
        setupRoomDropdown();

        btnSelectImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        });

        btnSubmit.setOnClickListener(v -> uploadImageAndSaveIssue());
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        edtTitle = findViewById(R.id.edtTitle);
        edtDescription = findViewById(R.id.edtDescription);
        actvIssueType = findViewById(R.id.actvIssueType);
        btnSelectImage = findViewById(R.id.btnSelectImage);
        btnSubmit = findViewById(R.id.btnSubmit);
        cardPreviewContainer = findViewById(R.id.cardPreviewContainer);
        ivPreview = findViewById(R.id.ivPreview);
        actvRoom = findViewById(R.id.actvRoom);
        pbSubmitting = findViewById(R.id.pbSubmitting);
    }

    private void setupToolbar() {
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }
    }

    private void setupRoomDropdown() {
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("EXTRA_ROOM_IDS")) {
            ArrayList<String> roomIds = intent.getStringArrayListExtra("EXTRA_ROOM_IDS");
            ArrayList<String> roomNames = intent.getStringArrayListExtra("EXTRA_ROOM_NAMES");

            if (roomIds != null && roomNames != null && !roomIds.isEmpty()) {
                populateRoomDropdown(roomIds, roomNames);
                return; // Nếu đã có dữ liệu từ Intent thì dừng lại, KHÔNG gọi Firestore nữa
            }
        }

        // Chỉ khi Intent rỗng mới fallback tự tải từ Firestore
        loadRentedRoomsFromFirestore();
    }

    private void populateRoomDropdown(List<String> roomIds, List<String> roomNames) {
        ArrayAdapter<String> roomAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, roomNames);
        actvRoom.setAdapter(roomAdapter);

        // Mặc định chọn phòng đầu tiên
        selectedRoomId = roomIds.get(0);
        actvRoom.setText(roomNames.get(0), false);

        // Cập nhật selectedRoomId khi người dùng đổi lựa chọn
        actvRoom.setOnItemClickListener((parent, view, position, id) -> {
            selectedRoomId = roomIds.get(position);
        });
    }

    private void loadRentedRoomsFromFirestore() {
        if (currentTenantId == null || currentTenantId.isEmpty()) return;

        db.collection("rooms")
                .whereEqualTo("tenantId", currentTenantId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<String> roomIds = new ArrayList<>();
                    List<String> roomNames = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Room room = doc.toObject(Room.class);
                        if (room != null) {
                            String roomId = doc.getId();
                            String roomName = room.getName() != null ? room.getName() : "Phòng " + roomId;
                            roomIds.add(roomId);
                            roomNames.add(roomName);
                        }
                    }

                    if (!roomIds.isEmpty()) {
                        populateRoomDropdown(roomIds, roomNames);
                    } else {
                        Toast.makeText(this, "Không tìm thấy phòng bạn đang thuê!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi tải phòng: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void uploadImageAndSaveIssue() {
        String title = edtTitle.getText() != null ? edtTitle.getText().toString().trim() : "";
        String description = edtDescription.getText() != null ? edtDescription.getText().toString().trim() : "";
        String type = actvIssueType.getText() != null ? actvIssueType.getText().toString().trim() : "";

        if (selectedRoomId.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn phòng gặp sự cố", Toast.LENGTH_SHORT).show();
            return;
        }

        if (title.isEmpty() || description.isEmpty() || type.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đủ tiêu đề, loại sự cố và mô tả", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSubmit.setEnabled(false);
        btnSubmit.setText("Đang gửi báo cáo...");
        if (pbSubmitting != null) pbSubmitting.setVisibility(View.VISIBLE);

        String issueId = UUID.randomUUID().toString();

        if (imageUri == null) {
            saveIssueToFirestore(issueId, selectedRoomId, type, title, description, "");
            return;
        }

        try {
            StorageReference storageRef = storage.getReference().child("issue_images/" + issueId + ".jpg");
            storageRef.putFile(imageUri).continueWithTask(task -> {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }
                return storageRef.getDownloadUrl();
            }).addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    String imageUrl = task.getResult().toString();
                    saveIssueToFirestore(issueId, selectedRoomId, type, title, description, imageUrl);
                } else {
                    Toast.makeText(CreateIssueActivity.this, "Bỏ qua tải ảnh, đang lưu thông tin sự cố...", Toast.LENGTH_SHORT).show();
                    saveIssueToFirestore(issueId, selectedRoomId, type, title, description, "");
                }
            });
        } catch (Exception e) {
            saveIssueToFirestore(issueId, selectedRoomId, type, title, description, "");
        }
    }

    private void saveIssueToFirestore(String issueId, String roomId, String type, String title, String description, String imageUrl) {
        String tenantId = currentTenantId;

        // Truy vấn thông tin phòng để lấy chính xác landlordId gán vào sự cố
        db.collection("rooms").document(roomId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    String landlordId = "";
                    if (documentSnapshot.exists()) {
                        landlordId = documentSnapshot.getString("landlordId");
                        if (landlordId == null) landlordId = "";
                    }

                    Issue newIssue = new Issue(
                            issueId, roomId, tenantId, landlordId, type, title, description, imageUrl,
                            "PENDING",
                            Timestamp.now(),
                            "",
                            "TRUNG BÌNH"
                    );

                    db.collection("issues").document(issueId).set(newIssue)
                            .addOnSuccessListener(aVoid -> {
                                if (pbSubmitting != null) pbSubmitting.setVisibility(View.GONE);
                                Toast.makeText(this, "Gửi báo cáo thành công!", Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                if (pbSubmitting != null) pbSubmitting.setVisibility(View.GONE);
                                Toast.makeText(this, "Lỗi lưu dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                btnSubmit.setEnabled(true);
                                btnSubmit.setText("Gửi Báo Cáo Sự Cố");
                            });
                })
                .addOnFailureListener(e -> {
                    Issue newIssue = new Issue(
                            issueId, roomId, tenantId, "", type, title, description, imageUrl,
                            "PENDING",
                            Timestamp.now(),
                            "",
                            "TRUNG BÌNH"
                    );

                    db.collection("issues").document(issueId).set(newIssue)
                            .addOnSuccessListener(aVoid -> {
                                if (pbSubmitting != null) pbSubmitting.setVisibility(View.GONE);
                                Toast.makeText(this, "Gửi báo cáo thành công!", Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(err -> {
                                if (pbSubmitting != null) pbSubmitting.setVisibility(View.GONE);
                                Toast.makeText(this, "Lỗi lưu dữ liệu: " + err.getMessage(), Toast.LENGTH_SHORT).show();
                                btnSubmit.setEnabled(true);
                                btnSubmit.setText("Gửi Báo Cáo Sự Cố");
                            });
                });
    }
}
