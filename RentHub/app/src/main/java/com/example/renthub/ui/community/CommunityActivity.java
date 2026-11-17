package com.example.renthub.ui.community;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.renthub.R;
import com.example.renthub.ui.home.LandlordHomeActivity;
import com.example.renthub.ui.home.TenantHomeActivity;
import com.example.renthub.ui.invoice.Invoice;
import com.example.renthub.ui.invoice.InvoiceListActivity;
import com.example.renthub.ui.invoice.TenantInvoiceDetailActivity;
import com.example.renthub.ui.issue.IssueListActivity;
import com.example.renthub.ui.notification.NotificationActivity;
import com.example.renthub.ui.room.Room;
import com.example.renthub.ui.settings.SettingsActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Màn hình Bảng tin cộng đồng (Community):
 * - Phân quyền rõ ràng:
 *   + Chủ trọ: Xem toàn bộ thông báo mình đã đăng, đăng thông báo mới, xóa thông báo đã đăng.
 *   + Người thuê: Xem danh sách thông báo chung từ chủ trọ hoặc thông báo gửi riêng cho phòng mình đang thuê.
 * - Bộ lọc danh mục thông báo: Tất cả, Khẩn cấp, Bảo trì, Nhắc nhở, Nội quy.
 * - Thanh tìm kiếm thông báo tức thời.
 * - Đồng bộ thanh điều hướng phía dưới (Bottom Navigation) chuẩn RentHub.
 */
public class CommunityActivity extends AppCompatActivity {

    private enum CategoryFilter {
        ALL, EMERGENCY, MAINTENANCE, REMINDER, RULES
    }

    private TextView tvCommunityTitle, tvCommunitySubtitle;
    private ImageButton btnCommunityNotification;
    private View badgeNotificationDot;

    private EditText edtSearchAnnouncement;
    private MaterialButton btnFilterAll, btnFilterEmergency, btnFilterMaintenance, btnFilterReminder, btnFilterRules;

    private ProgressBar pbLoading;
    private RecyclerView rvAnnouncements;
    private LinearLayout layoutEmpty;
    private TextView tvEmptyTitle, tvEmptySubtitle;
    private ExtendedFloatingActionButton fabCreateAnnouncement;

    private AnnouncementAdapter adapter;
    private final List<Announcement> rawAnnouncements = new ArrayList<>();
    private final List<Announcement> displayedAnnouncements = new ArrayList<>();

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String currentUserId = "";
    private boolean isLandlord = false;

    // Danh sách các phòng mà người thuê đang thuê
    private final Set<String> myRentedRoomIds = new HashSet<>();
    private final Set<String> myLandlordIds = new HashSet<>();

    private ListenerRegistration announcementListener;
    private ListenerRegistration notificationBadgeListener;
    private CategoryFilter currentCategoryFilter = CategoryFilter.ALL;
    private String searchKeyword = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community);

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

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("USER_ROLE")) {
            String roleExtra = intent.getStringExtra("USER_ROLE");
            isLandlord = "LANDLORD".equalsIgnoreCase(roleExtra);
        }

        initViews();
        setupRecyclerView();
        setupFiltersAndSearch();
        setupBottomNav();
        checkUserRoleAndLoadAnnouncements();
    }

    @Override
    protected void onResume() {
        super.onResume();
        listenToUnreadNotifications();
    }

    private void initViews() {
        tvCommunityTitle = findViewById(R.id.tvCommunityTitle);
        tvCommunitySubtitle = findViewById(R.id.tvCommunitySubtitle);
        btnCommunityNotification = findViewById(R.id.btnCommunityNotification);
        badgeNotificationDot = findViewById(R.id.badgeNotificationDot);

        edtSearchAnnouncement = findViewById(R.id.edtSearchAnnouncement);
        btnFilterAll = findViewById(R.id.btnFilterAll);
        btnFilterEmergency = findViewById(R.id.btnFilterEmergency);
        btnFilterMaintenance = findViewById(R.id.btnFilterMaintenance);
        btnFilterReminder = findViewById(R.id.btnFilterReminder);
        btnFilterRules = findViewById(R.id.btnFilterRules);

        pbLoading = findViewById(R.id.pbLoading);
        rvAnnouncements = findViewById(R.id.rvAnnouncements);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        tvEmptyTitle = findViewById(R.id.tvEmptyTitle);
        tvEmptySubtitle = findViewById(R.id.tvEmptySubtitle);
        fabCreateAnnouncement = findViewById(R.id.fabCreateAnnouncement);

        if (btnCommunityNotification != null) {
            btnCommunityNotification.setOnClickListener(v -> {
                Intent intent = new Intent(CommunityActivity.this, NotificationActivity.class);
                startActivity(intent);
            });
        }

        if (fabCreateAnnouncement != null) {
            fabCreateAnnouncement.setOnClickListener(v -> {
                Intent intent = new Intent(CommunityActivity.this, CreateAnnouncementActivity.class);
                startActivity(intent);
            });
        }
    }

    private void setupRecyclerView() {
        adapter = new AnnouncementAdapter(displayedAnnouncements, isLandlord, new AnnouncementAdapter.OnAnnouncementClickListener() {
            @Override
            public void onItemClick(Announcement announcement) {
                Intent intent = new Intent(CommunityActivity.this, AnnouncementDetailActivity.class);
                intent.putExtra(AnnouncementDetailActivity.EXTRA_ANNOUNCEMENT, announcement);
                startActivity(intent);
            }

            @Override
            public void onDeleteClick(Announcement announcement) {
                confirmDeleteAnnouncement(announcement);
            }
        });

        rvAnnouncements.setLayoutManager(new LinearLayoutManager(this));
        rvAnnouncements.setAdapter(adapter);
    }

    private void setupFiltersAndSearch() {
        btnFilterAll.setOnClickListener(v -> setCategoryFilter(CategoryFilter.ALL));
        btnFilterEmergency.setOnClickListener(v -> setCategoryFilter(CategoryFilter.EMERGENCY));
        btnFilterMaintenance.setOnClickListener(v -> setCategoryFilter(CategoryFilter.MAINTENANCE));
        btnFilterReminder.setOnClickListener(v -> setCategoryFilter(CategoryFilter.REMINDER));
        btnFilterRules.setOnClickListener(v -> setCategoryFilter(CategoryFilter.RULES));

        edtSearchAnnouncement.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchKeyword = s != null ? s.toString().trim().toLowerCase(Locale.getDefault()) : "";
                applyFilterAndSearch();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setCategoryFilter(CategoryFilter filter) {
        currentCategoryFilter = filter;
        updateFilterButtonStyles();
        applyFilterAndSearch();
    }

    private void updateFilterButtonStyles() {
        styleFilterButton(btnFilterAll, currentCategoryFilter == CategoryFilter.ALL);
        styleFilterButton(btnFilterEmergency, currentCategoryFilter == CategoryFilter.EMERGENCY);
        styleFilterButton(btnFilterMaintenance, currentCategoryFilter == CategoryFilter.MAINTENANCE);
        styleFilterButton(btnFilterReminder, currentCategoryFilter == CategoryFilter.REMINDER);
        styleFilterButton(btnFilterRules, currentCategoryFilter == CategoryFilter.RULES);
    }

    private void styleFilterButton(MaterialButton button, boolean isSelected) {
        if (button == null) return;
        if (isSelected) {
            button.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#148369")));
            button.setTextColor(Color.WHITE);
            button.setStrokeWidth(0);
        } else {
            button.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
            button.setTextColor(Color.parseColor("#6A7B8A"));
            button.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#CBD5E1")));
            button.setStrokeWidth(2);
        }
    }

    /**
     * Xác định vai trò của người dùng (Chủ trọ hay Người thuê) để nạp dữ liệu phù hợp
     */
    private void checkUserRoleAndLoadAnnouncements() {
        pbLoading.setVisibility(View.VISIBLE);

        db.collection("users").document(currentUserId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String role = doc.getString("role");
                        isLandlord = role != null && (role.equalsIgnoreCase("LANDLORD")
                                || role.equalsIgnoreCase("CHỦ TRỌ")
                                || role.toLowerCase().contains("chủ"));
                    }

                    setupRoleUI();

                    if (isLandlord) {
                        loadLandlordAnnouncements();
                    } else {
                        loadTenantRentedRoomsAndAnnouncements();
                    }
                })
                .addOnFailureListener(e -> {
                    // Nếu lỗi mặc định nạp thông báo chung
                    loadTenantRentedRoomsAndAnnouncements();
                });
    }

    private void setupRoleUI() {
        if (isLandlord) {
            tvCommunityTitle.setText("Quản lý thông báo");
            tvCommunitySubtitle.setText("Đăng và theo dõi các thông báo gửi đến người thuê");
            fabCreateAnnouncement.setVisibility(View.VISIBLE);
        } else {
            tvCommunityTitle.setText("Bảng tin cộng đồng");
            tvCommunitySubtitle.setText("Thông báo & tin tức từ chủ trọ của bạn");
            fabCreateAnnouncement.setVisibility(View.GONE);
        }

        // Cập nhật adapter với quyền chủ trọ
        setupRecyclerView();
    }

    /**
     * Nạp các thông báo do chính Chủ trọ này đăng
     */
    private void loadLandlordAnnouncements() {
        if (announcementListener != null) announcementListener.remove();

        announcementListener = db.collection("announcements")
                .addSnapshotListener((querySnapshot, error) -> {
                    pbLoading.setVisibility(View.GONE);

                    if (error != null) {
                        Toast.makeText(this, "Lỗi tải thông báo: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    rawAnnouncements.clear();
                    if (querySnapshot != null) {
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            try {
                                Announcement item = doc.toObject(Announcement.class);
                                if (item != null) {
                                    item.setId(doc.getId());
                                    if (item.getLandlordId() == null || item.getLandlordId().isEmpty() || currentUserId.equals(item.getLandlordId())) {
                                        rawAnnouncements.add(item);
                                    }
                                }
                            } catch (Exception ex) {
                                android.util.Log.e("CommunityActivity", "Bỏ qua thông báo lỗi: " + doc.getId(), ex);
                            }
                        }
                    }

                    // Sắp xếp giảm dần theo thời gian trong bộ nhớ (tránh cần composite index)
                    java.util.Collections.sort(rawAnnouncements, (a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));

                    applyFilterAndSearch();
                });
    }

    /**
     * Đối với Người thuê: trước hết tải các phòng người thuê đang ở để xác định landlordId và roomId
     */
    private void loadTenantRentedRoomsAndAnnouncements() {
        db.collection("rooms")
                .whereEqualTo("tenantId", currentUserId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    myRentedRoomIds.clear();
                    myLandlordIds.clear();

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Room room = doc.toObject(Room.class);
                        if (room != null) {
                            myRentedRoomIds.add(doc.getId());
                            if (room.getLandlordId() != null && !room.getLandlordId().isEmpty()) {
                                myLandlordIds.add(room.getLandlordId());
                            }
                        }
                    }

                    listenToTenantAnnouncements();
                })
                .addOnFailureListener(e -> {
                    listenToTenantAnnouncements();
                });
    }

    /**
     * Lắng nghe thông báo dành cho Người thuê
     */
    private void listenToTenantAnnouncements() {
        if (announcementListener != null) announcementListener.remove();

        announcementListener = db.collection("announcements")
                .addSnapshotListener((querySnapshot, error) -> {
                    pbLoading.setVisibility(View.GONE);

                    if (error != null) {
                        Toast.makeText(this, "Lỗi tải thông báo: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    rawAnnouncements.clear();
                    if (querySnapshot != null) {
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            try {
                                Announcement item = doc.toObject(Announcement.class);
                                if (item != null) {
                                    item.setId(doc.getId());

                                    // Kiểm tra xem thông báo này có gửi cho người thuê này không
                                    boolean isTargetAll = item.getTargetType() == null || Announcement.TARGET_ALL.equalsIgnoreCase(item.getTargetType());

                                    boolean isTargetMyRoom = false;
                                    if (item.getTargetRoomIds() != null && !item.getTargetRoomIds().isEmpty()) {
                                        for (String roomId : myRentedRoomIds) {
                                            if (item.getTargetRoomIds().contains(roomId)) {
                                                isTargetMyRoom = true;
                                                break;
                                            }
                                        }
                                    }

                                    if (isTargetAll || isTargetMyRoom) {
                                        rawAnnouncements.add(item);
                                    }
                                }
                            } catch (Exception ex) {
                                android.util.Log.e("CommunityActivity", "Bỏ qua thông báo lỗi: " + doc.getId(), ex);
                            }
                        }
                    }

                    // Sắp xếp giảm dần theo thời gian trong bộ nhớ (tránh cần composite index)
                    java.util.Collections.sort(rawAnnouncements, (a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));

                    applyFilterAndSearch();
                });
    }

    private void applyFilterAndSearch() {
        displayedAnnouncements.clear();

        for (Announcement item : rawAnnouncements) {
            // 1. Kiểm tra bộ lọc thể loại
            boolean matchCategory = false;
            String cat = item.getCategory() != null ? item.getCategory().toLowerCase(Locale.getDefault()) : "";

            switch (currentCategoryFilter) {
                case ALL:
                    matchCategory = true;
                    break;
                case EMERGENCY:
                    matchCategory = cat.contains("khẩn");
                    break;
                case MAINTENANCE:
                    matchCategory = cat.contains("bảo trì") || cat.contains("sửa");
                    break;
                case REMINDER:
                    matchCategory = cat.contains("nhắc") || cat.contains("tiền");
                    break;
                case RULES:
                    matchCategory = cat.contains("quy");
                    break;
            }

            if (!matchCategory) continue;

            // 2. Kiểm tra từ khóa tìm kiếm
            boolean matchSearch = true;
            if (!searchKeyword.isEmpty()) {
                String title = item.getTitle() != null ? item.getTitle().toLowerCase(Locale.getDefault()) : "";
                String content = item.getContent() != null ? item.getContent().toLowerCase(Locale.getDefault()) : "";
                matchSearch = title.contains(searchKeyword) || content.contains(searchKeyword);
            }

            if (matchSearch) {
                displayedAnnouncements.add(item);
            }
        }

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }

        if (layoutEmpty != null) {
            layoutEmpty.setVisibility(displayedAnnouncements.isEmpty() ? View.VISIBLE : View.GONE);
            if (isLandlord) {
                tvEmptyTitle.setText("Chưa có thông báo nào");
                tvEmptySubtitle.setText("Nhấn nút 'Đăng thông báo' ở góc dưới để gửi thông báo đầu tiên đến người thuê.");
            } else {
                tvEmptyTitle.setText("Chưa có thông báo nào");
                tvEmptySubtitle.setText("Chủ trọ chưa đăng thông báo nào trong khu trọ này.");
            }
        }
    }

    /**
     * Xác nhận xóa thông báo (dành cho Chủ trọ)
     */
    private void confirmDeleteAnnouncement(Announcement announcement) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa bài đăng thông báo")
                .setMessage("Bạn có chắc chắn muốn xóa thông báo '" + announcement.getTitle() + "'?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    db.collection("announcements").document(announcement.getId())
                            .delete()
                            .addOnSuccessListener(aVoid -> Toast.makeText(this, "Đã xóa thông báo thành công!", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(this, "Lỗi xóa: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    /**
     * Lắng nghe số lượng thông báo chưa đọc để hiển thị chấm đỏ trên biểu tượng chuông
     */
    private void listenToUnreadNotifications() {
        if (currentUserId.isEmpty()) return;

        if (notificationBadgeListener != null) notificationBadgeListener.remove();

        notificationBadgeListener = db.collection("notifications")
                .whereEqualTo("receiverId", currentUserId)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null || querySnapshot == null) {
                        badgeNotificationDot.setVisibility(View.GONE);
                        return;
                    }
                    boolean hasUnread = false;
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Boolean isRead = doc.getBoolean("read");
                        if (isRead == null || !isRead) {
                            hasUnread = true;
                            break;
                        }
                    }
                    badgeNotificationDot.setVisibility(hasUnread ? View.VISIBLE : View.GONE);
                });
    }

    /**
     * Thiết lập các sự kiện cho thanh điều hướng phía dưới (Bottom Navigation)
     */
    private void setupBottomNav() {
        // 1. TRANG CHỦ
        View btnNavRoom = findViewById(R.id.btnNavRoom);
        if (btnNavRoom != null) {
            btnNavRoom.setOnClickListener(v -> {
                if (isLandlord) {
                    Intent intent = new Intent(CommunityActivity.this, LandlordHomeActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                } else {
                    Intent intent = new Intent(CommunityActivity.this, TenantHomeActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                }
                finish();
            });
        }

        // 2. HÓA ĐƠN
        View btnNavInvoice = findViewById(R.id.btnNavInvoice);
        if (btnNavInvoice != null) {
            btnNavInvoice.setOnClickListener(v -> {
                if (isLandlord) {
                    Intent intent = new Intent(CommunityActivity.this, InvoiceListActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    loadTenantInvoiceAndNavigate();
                }
            });
        }

        // 3. SỰ CỐ
        View btnNavIssue = findViewById(R.id.btnNavIssue);
        if (btnNavIssue != null) {
            btnNavIssue.setOnClickListener(v -> {
                Intent intent = new Intent(CommunityActivity.this, IssueListActivity.class);
                intent.putExtra("USER_ROLE", isLandlord ? "LANDLORD" : "TENANT");
                startActivity(intent);
                finish();
            });
        }

        // 4. BẢNG TIN (Đang ở màn hình này nên không làm gì)

        // 5. CÀI ĐẶT
        View btnNavSettings = findViewById(R.id.btnNavSettings);
        if (btnNavSettings != null) {
            btnNavSettings.setOnClickListener(v -> {
                Intent intent = new Intent(CommunityActivity.this, SettingsActivity.class);
                startActivity(intent);
                finish();
            });
        }
    }

    private void loadTenantInvoiceAndNavigate() {
        if (!myRentedRoomIds.isEmpty()) {
            String firstRoomId = myRentedRoomIds.iterator().next();
            db.collection("invoices")
                    .whereEqualTo("roomId", firstRoomId)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (!querySnapshot.isEmpty()) {
                            DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                            Invoice invoice = doc.toObject(Invoice.class);
                            if (invoice != null) {
                                invoice.setId(doc.getId());
                                Intent intent = new Intent(CommunityActivity.this, TenantInvoiceDetailActivity.class);
                                intent.putExtra(TenantInvoiceDetailActivity.EXTRA_INVOICE, invoice);
                                startActivity(intent);
                                finish();
                                return;
                            }
                        }
                        Toast.makeText(this, "Phòng của bạn chưa có hóa đơn nào!", Toast.LENGTH_SHORT).show();
                    });
        } else {
            Toast.makeText(this, "Bạn chưa thuê phòng nào!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (announcementListener != null) {
            announcementListener.remove();
        }
        if (notificationBadgeListener != null) {
            notificationBadgeListener.remove();
        }
    }
}

