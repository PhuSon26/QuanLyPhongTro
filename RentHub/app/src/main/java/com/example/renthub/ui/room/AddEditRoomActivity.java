package com.example.renthub.ui.room;

import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.renthub.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddEditRoomActivity extends AppCompatActivity {

    public static final String EXTRA_ROOM = "EXTRA_ROOM";
    public static final String EXTRA_IS_VIEW_ONLY = "EXTRA_IS_VIEW_ONLY";

    private ImageButton btnBack;
    private TextView tvTitle, tvOccupantCountTitle;
    private TextInputEditText edtRoomName, edtRoomLocation, edtRoomArea, edtRoomPrice, edtMaxOccupants;
    private TextInputEditText edtLandlordName, edtLandlordPhone;
    private TextInputEditText edtTenantName, edtTenantPhone;
    private RadioGroup rgStatus;
    private RadioButton rbAvailable, rbOccupied, rbMaintenance;
    private MaterialButton btnSaveRoom, btnAddRoomImages, btnAddOccupant;
    private RecyclerView rvAddImages;
    private LinearLayout containerOccupants;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private Room currentRoom = null;
    private boolean isViewOnly = false;

    private AddRoomImageAdapter imageAdapter;
    private final List<String> roomImageList = new ArrayList<>();
    private final List<String> occupantNames = new ArrayList<>();

    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetMultipleContents(), uris -> {
                if (uris != null && !uris.isEmpty()) {
                    for (Uri uri : uris) roomImageList.add(uri.toString());
                    if (imageAdapter != null) imageAdapter.notifyDataSetChanged();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_room);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        initViews();
        lockLandlordInfoFields();
        setupImageRecyclerView();
        checkIntentData();
        loadLandlordProfileInfo();
        setupListeners();
        updateOccupantUI();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tvTitle = findViewById(R.id.tvTitle);
        tvOccupantCountTitle = findViewById(R.id.tvOccupantCountTitle);
        edtRoomName = findViewById(R.id.edtRoomName);
        edtRoomLocation = findViewById(R.id.edtRoomLocation);
        edtRoomArea = findViewById(R.id.edtRoomArea);
        edtRoomPrice = findViewById(R.id.edtRoomPrice);
        edtMaxOccupants = findViewById(R.id.edtMaxOccupants);

        edtLandlordName = findViewById(R.id.edtLandlordName);
        edtLandlordPhone = findViewById(R.id.edtLandlordPhone);

        edtTenantName = findViewById(R.id.edtTenantName);
        edtTenantPhone = findViewById(R.id.edtTenantPhone);

        rgStatus = findViewById(R.id.rgStatus);
        rbAvailable = findViewById(R.id.rbAvailable);
        rbOccupied = findViewById(R.id.rbOccupied);
        rbMaintenance = findViewById(R.id.rbMaintenance);
        btnSaveRoom = findViewById(R.id.btnSaveRoom);

        btnAddRoomImages = findViewById(R.id.btnAddRoomImages);
        btnAddOccupant = findViewById(R.id.btnAddOccupant);
        rvAddImages = findViewById(R.id.rvAddImages);
        containerOccupants = findViewById(R.id.containerOccupants);
    }

    private void lockLandlordInfoFields() {
        if (edtLandlordName != null) edtLandlordName.setEnabled(false);
        if (edtLandlordPhone != null) edtLandlordPhone.setEnabled(false);
    }

    private void setupImageRecyclerView() {
        imageAdapter = new AddRoomImageAdapter(roomImageList, position -> {
            roomImageList.remove(position);
            imageAdapter.notifyItemRemoved(position);
        });

        rvAddImages.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvAddImages.setAdapter(imageAdapter);
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
                    edtMaxOccupants.setText(String.valueOf(currentRoom.getMaxOccupants()));

                    edtTenantName.setText(currentRoom.getTenantName() != null ? currentRoom.getTenantName() : "");
                    edtTenantPhone.setText(currentRoom.getTenantPhone() != null ? currentRoom.getTenantPhone() : "");

                    if (currentRoom.getOccupants() != null) {
                        occupantNames.clear();
                        occupantNames.addAll(currentRoom.getOccupants());
                    }

                    String status = currentRoom.getStatus() != null ? currentRoom.getStatus() : "";
                    if (status.contains("thuê") || status.contains("OCCUPIED") || status.contains("Đã đủ")) {
                        rbOccupied.setChecked(true);
                    } else if (status.contains("trì") || status.contains("MAINTENANCE")) {
                        rbMaintenance.setChecked(true);
                    } else {
                        rbAvailable.setChecked(true);
                    }

                    if (currentRoom.getImages() != null && !currentRoom.getImages().isEmpty()) {
                        roomImageList.clear();
                        roomImageList.addAll(currentRoom.getImages());
                        if (imageAdapter != null) imageAdapter.notifyDataSetChanged();
                    }
                }
            }
        }

        if (isViewOnly) {
            tvTitle.setText("Chi tiết phòng trọ");
            btnSaveRoom.setVisibility(View.GONE);
            btnAddRoomImages.setVisibility(View.GONE);
            btnAddOccupant.setVisibility(View.GONE);
            disableEditing();
        }
    }

    private void loadLandlordProfileInfo() {
        if (mAuth.getCurrentUser() != null) {
            String uid = mAuth.getCurrentUser().getUid();
            db.collection("users").document(uid).get().addOnSuccessListener(snapshot -> {
                if (snapshot.exists()) {
                    String name = snapshot.getString("fullName");
                    String phone = snapshot.getString("phone");

                    if (edtLandlordName != null) edtLandlordName.setText(name != null ? name : "Chủ trọ");
                    if (edtLandlordPhone != null) edtLandlordPhone.setText(phone != null ? phone : "");
                }
            });
        }
    }

    private void disableEditing() {
        edtRoomName.setEnabled(false);
        edtRoomLocation.setEnabled(false);
        edtRoomArea.setEnabled(false);
        edtRoomPrice.setEnabled(false);
        edtMaxOccupants.setEnabled(false);
        edtTenantName.setEnabled(false);
        edtTenantPhone.setEnabled(false);

        rbAvailable.setEnabled(false);
        rbOccupied.setEnabled(false);
        rbMaintenance.setEnabled(false);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnAddRoomImages.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        btnSaveRoom.setOnClickListener(v -> saveRoomToFirestore());

        btnAddOccupant.setOnClickListener(v -> {
            int max = getMaxOccupants();
            int current = getCurrentTotalOccupants();

            if (current >= max) {
                Toast.makeText(this, "Phòng đã đạt sức chứa tối đa (" + max + " người)!", Toast.LENGTH_SHORT).show();
                return;
            }

            occupantNames.add("");
            renderOccupantItem(occupantNames.size() - 1, "");
            updateOccupantUI();
        });

        edtTenantName.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { updateOccupantUI(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        edtMaxOccupants.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { updateOccupantUI(); }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private int getMaxOccupants() {
        try {
            String maxStr = edtMaxOccupants.getText().toString().trim();
            return Math.max(1, Integer.parseInt(maxStr));
        } catch (Exception e) {
            return 1;
        }
    }

    private int getCurrentTotalOccupants() {
        int count = 0;
        String tenantName = edtTenantName.getText().toString().trim();
        if (!tenantName.isEmpty()) count++;
        count += occupantNames.size();
        return count;
    }

    private void updateOccupantUI() {
        containerOccupants.removeAllViews();
        for (int i = 0; i < occupantNames.size(); i++) {
            renderOccupantItem(i, occupantNames.get(i));
        }

        int max = getMaxOccupants();
        int totalCurrent = getCurrentTotalOccupants();
        int coOccupantCount = occupantNames.size();

        tvOccupantCountTitle.setText("Người ở cùng (" + coOccupantCount + " người) • Tổng trong phòng: " + totalCurrent + "/" + max);

        if (totalCurrent >= max) {
            btnAddOccupant.setEnabled(false);
            btnAddOccupant.setAlpha(0.5f);
        } else {
            btnAddOccupant.setEnabled(true);
            btnAddOccupant.setAlpha(1.0f);
        }
    }

    private void renderOccupantItem(int index, String initialValue) {
        LinearLayout itemLayout = new LinearLayout(this);
        itemLayout.setOrientation(LinearLayout.HORIZONTAL);
        itemLayout.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        itemLayout.setPadding(0, 8, 0, 8);

        EditText edtOccupant = new EditText(this);
        LinearLayout.LayoutParams edtParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        edtOccupant.setLayoutParams(edtParams);
        edtOccupant.setHint("Họ tên người ở thứ " + (index + 2));
        edtOccupant.setText(initialValue);
        edtOccupant.setTextSize(14);
        if (isViewOnly) edtOccupant.setEnabled(false);

        edtOccupant.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (index < occupantNames.size()) {
                    occupantNames.set(index, s.toString().trim());
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        itemLayout.addView(edtOccupant);

        if (!isViewOnly) {
            ImageButton btnDelete = new ImageButton(this);
            btnDelete.setLayoutParams(new LinearLayout.LayoutParams(80, 80));
            btnDelete.setImageResource(R.drawable.rh_ic_add);
            btnDelete.setRotation(45f);
            btnDelete.setBackground(null);

            btnDelete.setOnClickListener(v -> {
                if (index < occupantNames.size()) {
                    occupantNames.remove(index);
                    updateOccupantUI();
                }
            });

            itemLayout.addView(btnDelete);
        }

        containerOccupants.addView(itemLayout);
    }

    private void saveRoomToFirestore() {
        String name = edtRoomName.getText().toString().trim();
        String location = edtRoomLocation.getText().toString().trim();
        String area = edtRoomArea.getText().toString().trim();
        String price = edtRoomPrice.getText().toString().trim();
        String landlordName = edtLandlordName.getText().toString().trim();
        String landlordPhone = edtLandlordPhone.getText().toString().trim();
        String tenantName = edtTenantName.getText().toString().trim();
        String tenantPhone = edtTenantPhone.getText().toString().trim();
        int maxOccupants = getMaxOccupants();

        if (TextUtils.isEmpty(name)) {
            edtRoomName.setError("Vui lòng nhập tên phòng");
            return;
        }

        if (TextUtils.isEmpty(price)) {
            edtRoomPrice.setError("Vui lòng nhập giá thuê");
            return;
        }

        int currentTotal = getCurrentTotalOccupants();

        if (currentTotal > maxOccupants) {
            Toast.makeText(this, "Tổng số người (" + currentTotal + " người) vượt quá sức chứa tối đa (" + maxOccupants + " người)!", Toast.LENGTH_LONG).show();
            return;
        }

        String status;
        if (currentTotal >= maxOccupants) {
            status = "Đã đủ người";
        } else {
            int checkedId = rgStatus.getCheckedRadioButtonId();
            if (checkedId == R.id.rbOccupied) {
                status = "Đang thuê";
            } else if (checkedId == R.id.rbMaintenance) {
                status = "Bảo trì";
            } else {
                status = "Còn trống";
            }
        }

        String landlordId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";

        Map<String, Object> roomData = new HashMap<>();
        roomData.put("name", name);
        roomData.put("location", location);
        roomData.put("area", area);
        roomData.put("price", price);
        roomData.put("status", status);
        roomData.put("maxOccupants", maxOccupants);
        roomData.put("occupants", occupantNames);

        roomData.put("landlordId", landlordId);
        roomData.put("landlordName", landlordName.isEmpty() ? "Chủ trọ" : landlordName);
        roomData.put("landlordPhone", landlordPhone);

        roomData.put("tenantName", tenantName);
        roomData.put("tenantPhone", tenantPhone);

        roomData.put("images", roomImageList);
        if (!roomImageList.isEmpty()) {
            roomData.put("imageUrl", roomImageList.get(0));
        } else {
            roomData.put("imageUrl", "");
        }

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

    private static class AddRoomImageAdapter extends RecyclerView.Adapter<AddRoomImageAdapter.ViewHolder> {
        private final List<String> images;
        private final OnRemoveListener listener;

        interface OnRemoveListener { void onRemove(int position); }

        AddRoomImageAdapter(List<String> images, OnRemoveListener listener) {
            this.images = images;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            int dp90 = (int) (90 * parent.getContext().getResources().getDisplayMetrics().density);
            int dp8 = (int) (8 * parent.getContext().getResources().getDisplayMetrics().density);

            MaterialCardView cardView = new MaterialCardView(parent.getContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp90, dp90);
            params.setMargins(0, 0, dp8, 0);
            cardView.setLayoutParams(params);
            cardView.setRadius(24f);
            cardView.setStrokeColor(android.graphics.Color.parseColor("#D3DFDA"));
            cardView.setStrokeWidth(2);

            ImageView imageView = new ImageView(parent.getContext());
            imageView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            cardView.addView(imageView);

            return new ViewHolder(cardView, imageView);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Glide.with(holder.itemView.getContext())
                    .load(images.get(position))
                    .centerCrop()
                    .into(holder.itemView.getContext() != null ? holder.imageView : holder.imageView);

            holder.itemView.setOnLongClickListener(v -> {
                if (listener != null) listener.onRemove(holder.getAdapterPosition());
                return true;
            });
        }

        @Override
        public int getItemCount() { return images != null ? images.size() : 0; }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;
            ViewHolder(@NonNull ViewGroup itemView, ImageView img) {
                super(itemView);
                this.imageView = img;
            }
        }
    }
}