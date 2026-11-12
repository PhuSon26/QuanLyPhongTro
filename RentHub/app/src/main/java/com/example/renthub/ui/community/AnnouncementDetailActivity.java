package com.example.renthub.ui.community;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.renthub.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Màn hình xem chi tiết thông báo Bảng tin cộng đồng:
 * - Hiển thị đầy đủ tiêu đề, thể loại, phạm vi, nội dung và người gửi.
 * - Cho phép Chủ trọ đã đăng bài thực hiện Xóa thông báo khi cần thiết.
 */
public class AnnouncementDetailActivity extends AppCompatActivity {

    public static final String EXTRA_ANNOUNCEMENT_ID = "EXTRA_ANNOUNCEMENT_ID";
    public static final String EXTRA_ANNOUNCEMENT = "EXTRA_ANNOUNCEMENT";

    private ImageButton btnBack, btnDelete;
    private ProgressBar pbLoading;
    private ScrollView scrollViewContent;

    private TextView tvCategoryBadge, tvTargetScopeBadge, tvTitle, tvPosterName, tvDate, tvContent;
    private MaterialButton btnBottomDelete;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String announcementId = "";
    private Announcement currentAnnouncement;
    private boolean isOwner = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_announcement_detail);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        initViews();
        getIntentData();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnDelete = findViewById(R.id.btnDelete);
        pbLoading = findViewById(R.id.pbLoading);
        scrollViewContent = findViewById(R.id.scrollViewContent);

        tvCategoryBadge = findViewById(R.id.tvCategoryBadge);
        tvTargetScopeBadge = findViewById(R.id.tvTargetScopeBadge);
        tvTitle = findViewById(R.id.tvTitle);
        tvPosterName = findViewById(R.id.tvPosterName);
        tvDate = findViewById(R.id.tvDate);
        tvContent = findViewById(R.id.tvContent);
        btnBottomDelete = findViewById(R.id.btnBottomDelete);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        View.OnClickListener deleteListener = v -> confirmDeleteAnnouncement();
        if (btnDelete != null) btnDelete.setOnClickListener(deleteListener);
        if (btnBottomDelete != null) btnBottomDelete.setOnClickListener(deleteListener);
    }

    private void getIntentData() {
        if (getIntent().hasExtra(EXTRA_ANNOUNCEMENT)) {
            currentAnnouncement = (Announcement) getIntent().getSerializableExtra(EXTRA_ANNOUNCEMENT);
            if (currentAnnouncement != null) {
                announcementId = currentAnnouncement.getId();
                bindDataToViews(currentAnnouncement);
                return;
            }
        }

        if (getIntent().hasExtra(EXTRA_ANNOUNCEMENT_ID)) {
            announcementId = getIntent().getStringExtra(EXTRA_ANNOUNCEMENT_ID);
        }

        if (announcementId != null && !announcementId.trim().isEmpty()) {
            loadAnnouncementFromFirestore(announcementId);
        } else {
            Toast.makeText(this, "Không tìm thấy thông tin bài đăng!", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void loadAnnouncementFromFirestore(String id) {
        pbLoading.setVisibility(View.VISIBLE);
        scrollViewContent.setVisibility(View.GONE);

        db.collection("announcements").document(id).get()
                .addOnSuccessListener(doc -> {
                    pbLoading.setVisibility(View.GONE);
                    if (doc.exists()) {
                        try {
                            currentAnnouncement = doc.toObject(Announcement.class);
                        } catch (Exception ex) {
                            android.util.Log.e("AnnouncementDetail", "Lỗi nạp bài đăng", ex);
                        }
                        if (currentAnnouncement != null) {
                            currentAnnouncement.setId(doc.getId());
                            scrollViewContent.setVisibility(View.VISIBLE);
                            bindDataToViews(currentAnnouncement);
                        } else {
                            Toast.makeText(this, "Không thể đọc dữ liệu bài đăng này!", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    } else {
                        Toast.makeText(this, "Bài đăng này đã bị xóa hoặc không tồn tại!", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    pbLoading.setVisibility(View.GONE);
                    Toast.makeText(this, "Lỗi tải thông báo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void bindDataToViews(Announcement announcement) {
        if (announcement == null) return;

        tvTitle.setText(announcement.getTitle() != null ? announcement.getTitle() : "Thông báo");
        tvContent.setText(announcement.getContent() != null ? announcement.getContent() : "");

        String poster = announcement.getLandlordName() != null && !announcement.getLandlordName().isEmpty()
                ? announcement.getLandlordName()
                : "Chủ trọ";
        tvPosterName.setText("Đăng bởi: " + poster);

        if (announcement.getCreatedAt() > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            tvDate.setText(sdf.format(new Date(announcement.getCreatedAt())));
        } else {
            tvDate.setText("");
        }

        // Thể loại
        String category = announcement.getCategory() != null ? announcement.getCategory() : "Thông báo chung";
        tvCategoryBadge.setText(category.toUpperCase(Locale.getDefault()));
        styleCategoryBadge(category);

        // Phạm vi
        if (Announcement.TARGET_ALL.equalsIgnoreCase(announcement.getTargetType())) {
            tvTargetScopeBadge.setText("Toàn bộ người thuê");
            tvTargetScopeBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#EFF6FF")));
            tvTargetScopeBadge.setTextColor(Color.parseColor("#2563EB"));
        } else {
            List<String> roomNames = announcement.getTargetRoomNames();
            if (roomNames != null && !roomNames.isEmpty()) {
                tvTargetScopeBadge.setText("Phòng: " + android.text.TextUtils.join(", ", roomNames));
            } else {
                tvTargetScopeBadge.setText("Phòng chỉ định");
            }
            tvTargetScopeBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FEF3C7")));
            tvTargetScopeBadge.setTextColor(Color.parseColor("#D97706"));
        }

        // Kiểm tra quyền sở hữu bài đăng
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null && currentUser.getUid().equals(announcement.getLandlordId())) {
            isOwner = true;
            btnDelete.setVisibility(View.VISIBLE);
            btnBottomDelete.setVisibility(View.VISIBLE);
        } else {
            isOwner = false;
            btnDelete.setVisibility(View.GONE);
            btnBottomDelete.setVisibility(View.GONE);
        }
    }

    private void styleCategoryBadge(String category) {
        String catLower = category.toLowerCase(Locale.getDefault());
        if (catLower.contains("khẩn")) {
            tvCategoryBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FEE2E2")));
            tvCategoryBadge.setTextColor(Color.parseColor("#DC2626"));
        } else if (catLower.contains("bảo trì") || catLower.contains("sửa")) {
            tvCategoryBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFEDD5")));
            tvCategoryBadge.setTextColor(Color.parseColor("#EA580C"));
        } else if (catLower.contains("nhắc") || catLower.contains("tiền")) {
            tvCategoryBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F3E8FF")));
            tvCategoryBadge.setTextColor(Color.parseColor("#7C3AED"));
        } else if (catLower.contains("quy")) {
            tvCategoryBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E0E7FF")));
            tvCategoryBadge.setTextColor(Color.parseColor("#4338CA"));
        } else {
            tvCategoryBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E0F2EF")));
            tvCategoryBadge.setTextColor(Color.parseColor("#148369"));
        }
    }

    /**
     * Xác nhận và xóa thông báo khỏi Cloud Firestore
     */
    private void confirmDeleteAnnouncement() {
        if (!isOwner || currentAnnouncement == null) return;

        new AlertDialog.Builder(this)
                .setTitle("Xác nhận xóa thông báo")
                .setMessage("Bạn có chắc chắn muốn xóa bài đăng này? Sau khi xóa, người thuê sẽ không còn thấy thông báo này trên Bảng tin.")
                .setPositiveButton("Xóa vĩnh viễn", (dialog, which) -> deleteAnnouncementFromFirestore())
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void deleteAnnouncementFromFirestore() {
        if (announcementId == null || announcementId.trim().isEmpty()) return;

        pbLoading.setVisibility(View.VISIBLE);
        db.collection("announcements").document(announcementId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    pbLoading.setVisibility(View.GONE);
                    Toast.makeText(this, "Đã xóa thông báo thành công!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e -> {
                    pbLoading.setVisibility(View.GONE);
                    Toast.makeText(this, "Lỗi xóa thông báo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}

