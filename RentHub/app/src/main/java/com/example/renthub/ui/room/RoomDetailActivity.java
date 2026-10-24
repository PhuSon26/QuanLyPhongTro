package com.example.renthub.ui.room;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.example.renthub.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class RoomDetailActivity extends AppCompatActivity {

    public static final String EXTRA_ROOM = "EXTRA_ROOM";

    private ImageButton btnBack, btnCallTenant, btnCallLandlord;
    private MaterialButton btnEditRoomBottom;
    private TextView tvDetailRoomName, tvDetailLocation, tvDetailStatus, tvDetailPrice, tvDetailArea, tvDetailOccupancy;
    private TextView tvDetailLandlordName, tvDetailLandlordPhone;
    private TextView tvDetailTenantName, tvDetailTenantPhone, tvImageCounter, tvOccupantsTitle;
    private MaterialCardView cardStatusBadge;
    private ViewPager2 vpRoomImages;
    private FrameLayout flImageContainer;
    private LinearLayout containerDetailOccupants;

    private Room currentRoom;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_detail);

        initViews();
        getIntentData();
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnEditRoomBottom = findViewById(R.id.btnEditRoomBottom);
        btnCallTenant = findViewById(R.id.btnCallTenant);
        btnCallLandlord = findViewById(R.id.btnCallLandlord);

        tvDetailRoomName = findViewById(R.id.tvDetailRoomName);
        tvDetailLocation = findViewById(R.id.tvDetailLocation);
        tvDetailStatus = findViewById(R.id.tvDetailStatus);
        tvDetailPrice = findViewById(R.id.tvDetailPrice);
        tvDetailArea = findViewById(R.id.tvDetailArea);
        tvDetailOccupancy = findViewById(R.id.tvDetailOccupancy);

        tvDetailLandlordName = findViewById(R.id.tvDetailLandlordName);
        tvDetailLandlordPhone = findViewById(R.id.tvDetailLandlordPhone);

        tvDetailTenantName = findViewById(R.id.tvDetailTenantName);
        tvDetailTenantPhone = findViewById(R.id.tvDetailTenantPhone);
        tvImageCounter = findViewById(R.id.tvImageCounter);
        cardStatusBadge = findViewById(R.id.cardStatusBadge);

        vpRoomImages = findViewById(R.id.vpRoomImages);
        flImageContainer = findViewById(R.id.flImageContainer);

        tvOccupantsTitle = findViewById(R.id.tvOccupantsTitle);
        containerDetailOccupants = findViewById(R.id.containerDetailOccupants);
    }

    private void getIntentData() {
        if (getIntent() != null && getIntent().hasExtra(EXTRA_ROOM)) {
            currentRoom = (Room) getIntent().getSerializableExtra(EXTRA_ROOM);
            if (currentRoom != null) {
                displayRoomData();
                setupImageSlider();
            }
        }
    }

    private void setupImageSlider() {
        List<String> imageList = currentRoom.getImages();
        if (imageList != null && !imageList.isEmpty()) {
            flImageContainer.setVisibility(View.VISIBLE);
            ImageSliderAdapter adapter = new ImageSliderAdapter(imageList);
            vpRoomImages.setAdapter(adapter);

            tvImageCounter.setText("1/" + imageList.size());

            vpRoomImages.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    super.onPageSelected(position);
                    tvImageCounter.setText((position + 1) + "/" + imageList.size());
                }
            });
        } else {
            flImageContainer.setVisibility(View.GONE);
        }
    }

    private void displayRoomData() {
        tvDetailRoomName.setText(currentRoom.getName() != null ? currentRoom.getName() : "Phòng trọ");
        tvDetailLocation.setText(currentRoom.getLocation() != null ? currentRoom.getLocation() : "Chưa có vị trí");

        String area = currentRoom.getArea() != null ? currentRoom.getArea() : "0";
        if (!area.contains("m²")) area += " m²";
        tvDetailArea.setText(area);

        String price = currentRoom.getPrice() != null ? currentRoom.getPrice() : "0";
        if (!price.contains("đ")) price += "đ/tháng";
        tvDetailPrice.setText(price);

        // HIỂN THỊ TỈ LỆ SỐ NGƯỜI Ở
        int currentCount = currentRoom.getCurrentOccupantCount();
        int maxCount = currentRoom.getMaxOccupants();
        tvDetailOccupancy.setText(currentCount + "/" + maxCount + " người");

        // HIỂN THỊ TRẠNG THÁI PHÒNG (BAO GỒM NẾU ĐÃ ĐỦ NGƯỜI)
        String status = currentRoom.getStatus() != null ? currentRoom.getStatus() : "Còn trống";
        String statusLower = status.toLowerCase();

        if (statusLower.contains("đủ") || currentRoom.isFull()) {
            tvDetailStatus.setText("Đã đủ người");
            tvDetailStatus.setTextColor(Color.parseColor("#C74444"));
            cardStatusBadge.setCardBackgroundColor(Color.parseColor("#FCECEC"));
        } else if (statusLower.contains("thuê") || statusLower.contains("occupied")) {
            tvDetailStatus.setText("Đang thuê");
            tvDetailStatus.setTextColor(Color.parseColor("#2563A6"));
            cardStatusBadge.setCardBackgroundColor(Color.parseColor("#EAF1FC"));
        } else if (statusLower.contains("trì") || statusLower.contains("maintenance")) {
            tvDetailStatus.setText("Bảo trì");
            tvDetailStatus.setTextColor(Color.parseColor("#B7791F"));
            cardStatusBadge.setCardBackgroundColor(Color.parseColor("#FFF3DF"));
        } else {
            tvDetailStatus.setText("Còn trống");
            tvDetailStatus.setTextColor(Color.parseColor("#148369"));
            cardStatusBadge.setCardBackgroundColor(Color.parseColor("#E3F5ED"));
        }

        // THÔNG TIN CHỦ TRỌ
        if (currentRoom.getLandlordName() != null && !currentRoom.getLandlordName().trim().isEmpty()) {
            tvDetailLandlordName.setText(currentRoom.getLandlordName());
        } else {
            tvDetailLandlordName.setText("Chủ trọ");
        }

        if (currentRoom.getLandlordPhone() != null && !currentRoom.getLandlordPhone().trim().isEmpty()) {
            tvDetailLandlordPhone.setText(currentRoom.getLandlordPhone());
        } else {
            tvDetailLandlordPhone.setText("---");
        }

        // THÔNG TIN NGƯỜI THUÊ CHÍNH
        if (currentRoom.getTenantName() != null && !currentRoom.getTenantName().trim().isEmpty()) {
            tvDetailTenantName.setText(currentRoom.getTenantName());
        } else {
            tvDetailTenantName.setText("Chưa có người thuê");
        }

        if (currentRoom.getTenantPhone() != null && !currentRoom.getTenantPhone().trim().isEmpty()) {
            tvDetailTenantPhone.setText(currentRoom.getTenantPhone());
        } else {
            tvDetailTenantPhone.setText("---");
        }

        // HIỂN THỊ DANH SÁCH NGƯỜI Ở CÙNG
        if (containerDetailOccupants != null && tvOccupantsTitle != null) {
            containerDetailOccupants.removeAllViews();
            List<String> occupants = currentRoom.getOccupants();

            if (occupants != null && !occupants.isEmpty()) {
                tvOccupantsTitle.setText("Người ở cùng (" + occupants.size() + " người)");
                for (int i = 0; i < occupants.size(); i++) {
                    TextView tvName = new TextView(this);
                    tvName.setText((i + 1) + ". " + occupants.get(i));
                    tvName.setTextSize(14);
                    tvName.setTextColor(Color.parseColor("#14344B"));
                    tvName.setPadding(0, 8, 0, 8);
                    containerDetailOccupants.addView(tvName);
                }
            } else {
                tvOccupantsTitle.setText("Người ở cùng (0 người)");
                TextView tvEmpty = new TextView(this);
                tvEmpty.setText("Chưa có người ở cùng");
                tvEmpty.setTextSize(14);
                tvEmpty.setTextColor(Color.parseColor("#718096"));
                containerDetailOccupants.addView(tvEmpty);
            }
        }
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnEditRoomBottom.setOnClickListener(v -> openEditScreen());

        btnCallLandlord.setOnClickListener(v -> {
            if (currentRoom != null && currentRoom.getLandlordPhone() != null && !currentRoom.getLandlordPhone().trim().isEmpty()) {
                Intent callIntent = new Intent(Intent.ACTION_DIAL);
                callIntent.setData(Uri.parse("tel:" + currentRoom.getLandlordPhone()));
                startActivity(callIntent);
            } else {
                Toast.makeText(this, "Chưa có số điện thoại chủ trọ", Toast.LENGTH_SHORT).show();
            }
        });

        btnCallTenant.setOnClickListener(v -> {
            if (currentRoom != null && currentRoom.getTenantPhone() != null && !currentRoom.getTenantPhone().trim().isEmpty()) {
                Intent callIntent = new Intent(Intent.ACTION_DIAL);
                callIntent.setData(Uri.parse("tel:" + currentRoom.getTenantPhone()));
                startActivity(callIntent);
            } else {
                Toast.makeText(this, "Chưa có số điện thoại người thuê", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openEditScreen() {
        Intent intent = new Intent(RoomDetailActivity.this, AddEditRoomActivity.class);
        intent.putExtra(AddEditRoomActivity.EXTRA_ROOM, currentRoom);
        startActivity(intent);
        finish();
    }

    private static class ImageSliderAdapter extends RecyclerView.Adapter<ImageSliderAdapter.ViewHolder> {
        private final List<String> images;

        ImageSliderAdapter(List<String> images) { this.images = images; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ImageView imageView = new ImageView(parent.getContext());
            imageView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            return new ViewHolder(imageView);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Glide.with(holder.itemView.getContext())
                    .load(images.get(position))
                    .centerCrop()
                    .into(holder.imageView);
        }

        @Override
        public int getItemCount() { return images != null ? images.size() : 0; }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;
            ViewHolder(@NonNull ImageView itemView) {
                super(itemView);
                this.imageView = itemView;
            }
        }
    }
}