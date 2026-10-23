package com.example.renthub.ui.room;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.renthub.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AddEditRoomActivity extends AppCompatActivity {

    public static final String EXTRA_ROOM = "EXTRA_ROOM";
    public static final String EXTRA_IS_VIEW_ONLY = "EXTRA_IS_VIEW_ONLY";

    private ImageButton btnBack;
    private TextView tvTitle;
    private TextInputEditText edtRoomName, edtRoomLocation, edtRoomArea, edtRoomPrice;
    private TextInputEditText edtTenantName, edtTenantPhone;
    private RadioGroup rgStatus;
    private RadioButton rbAvailable, rbOccupied, rbMaintenance;
    private MaterialButton btnSaveRoom;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private Room currentRoom = null;
    private boolean isViewOnly = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_room);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        initViews();
        checkIntentData();
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tvTitle = findViewById(R.id.tvTitle);
        edtRoomName = findViewById(R.id.edtRoomName);
        edtRoomLocation = findViewById(R.id.edtRoomLocation);
        edtRoomArea = findViewById(R.id.edtRoomArea);
        edtRoomPrice = findViewById(R.id.edtRoomPrice);

        edtTenantName = findViewById(R.id.edtTenantName);
        edtTenantPhone = findViewById(R.id.edtTenantPhone);

        rgStatus = findViewById(R.id.rgStatus);
        rbAvailable = findViewById(R.id.rbAvailable);
        rbOccupied = findViewById(R.id.rbOccupied);
        rbMaintenance = findViewById(R.id.rbMaintenance);
        btnSaveRoom = findViewById(R.id.btnSaveRoom);
    }

    private void checkIntentData() {
        if (getIntent() != null) {
            isViewOnly = getIntent().getBooleanExtra(EXTRA_IS_VIEW_ONLY, false);

            if (getIntent().hasExtra(EXTRA_ROOM)) {
                currentRoom = (Room) getIntent().getSerializableExtra(EXTRA_ROOM);
                if (currentRoom != null) {
                    tvTitle.setText("Chỉnh sửa phòng trọ");
                    btnSaveRoom.setText("Cập nhật phòng");

                    edtRoomName.setText(currentRoom.getName());
                    edtRoomLocation.setText(currentRoom.getLocation());

                    String area = currentRoom.getArea() != null ? currentRoom.getArea().replace(" m²", "").replace("m²", "").trim() : "";
                    String price = currentRoom.getPrice() != null ? currentRoom.getPrice().replace("đ/tháng", "").replace(".", "").replace(",", "").trim() : "";
                    edtRoomArea.setText(area);
                    edtRoomPrice.setText(price);

                    edtTenantName.setText(currentRoom.getTenantName() != null ? currentRoom.getTenantName() : "");
                    edtTenantPhone.setText(currentRoom.getTenantPhone() != null ? currentRoom.getTenantPhone() : "");

                    String status = currentRoom.getStatus() != null ? currentRoom.getStatus() : "";
                    if (status.equalsIgnoreCase("Đang thuê") || status.equalsIgnoreCase("OCCUPIED")) {
                        rbOccupied.setChecked(true);
                    } else if (status.equalsIgnoreCase("Bảo trì") || status.equalsIgnoreCase("MAINTENANCE")) {
                        rbMaintenance.setChecked(true);
                    } else {
                        rbAvailable.setChecked(true);
                    }
                }
            }
        }

        // Bật chế độ chỉ xem
        if (isViewOnly) {
            tvTitle.setText("Chi tiết phòng trọ");
            btnSaveRoom.setVisibility(View.GONE); // Ẩn nút Lưu
            disableEditing(); // Khóa tất cả các ô nhập
        }
    }

    /**
     * KHÓA FORM KHÔNG CHO PHÉP NHẬP DỮ LIỆU
     */
    private void disableEditing() {
        edtRoomName.setEnabled(false);
        edtRoomLocation.setEnabled(false);
        edtRoomArea.setEnabled(false);
        edtRoomPrice.setEnabled(false);
        edtTenantName.setEnabled(false);
        edtTenantPhone.setEnabled(false);

        rbAvailable.setEnabled(false);
        rbOccupied.setEnabled(false);
        rbMaintenance.setEnabled(false);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnSaveRoom.setOnClickListener(v -> saveRoomToFirestore());
    }

    private void saveRoomToFirestore() {
        String name = edtRoomName.getText().toString().trim();
        String location = edtRoomLocation.getText().toString().trim();
        String area = edtRoomArea.getText().toString().trim();
        String price = edtRoomPrice.getText().toString().trim();
        String tenantName = edtTenantName.getText().toString().trim();
        String tenantPhone = edtTenantPhone.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            edtRoomName.setError("Vui lòng nhập tên phòng");
            return;
        }

        if (TextUtils.isEmpty(price)) {
            edtRoomPrice.setError("Vui lòng nhập giá thuê");
            return;
        }

        String status = "Còn trống";
        int checkedId = rgStatus.getCheckedRadioButtonId();
        if (checkedId == R.id.rbOccupied) {
            status = "Đang thuê";
        } else if (checkedId == R.id.rbMaintenance) {
            status = "Bảo trì";
        }

        String landlordId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";

        Map<String, Object> roomData = new HashMap<>();
        roomData.put("name", name);
        roomData.put("location", location);
        roomData.put("area", area);
        roomData.put("price", price);
        roomData.put("status", status);
        roomData.put("landlordId", landlordId);
        roomData.put("tenantName", tenantName);
        roomData.put("tenantPhone", tenantPhone);

        btnSaveRoom.setEnabled(false);

        if (currentRoom != null && currentRoom.getId() != null) {
            db.collection("rooms").document(currentRoom.getId())
                    .update(roomData)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Cập nhật thông tin thành công!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        btnSaveRoom.setEnabled(true);
                        Toast.makeText(this, "Lỗi cập nhật: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            db.collection("rooms")
                    .add(roomData)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(this, "Thêm phòng mới thành công!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        btnSaveRoom.setEnabled(true);
                        Toast.makeText(this, "Lỗi thêm phòng: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }
}