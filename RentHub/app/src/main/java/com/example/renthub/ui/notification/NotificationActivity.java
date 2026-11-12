package com.example.renthub.ui.notification;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
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
import com.example.renthub.ui.community.AnnouncementDetailActivity;
import com.example.renthub.ui.invoice.Invoice;
import com.example.renthub.ui.invoice.TenantInvoiceDetailActivity;
import com.example.renthub.ui.issue.IssueDetailActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Màn hình Trung tâm Thông báo (Notification Center):
 * - Hiển thị danh sách toàn bộ thông báo gộp chung theo thời gian thực (Realtime SnapshotListener).
 * - Không phân chia hạng mục, hiển thị tất cả thông báo theo thứ tự thời gian mới nhất.
 * - Điều hướng trực tiếp đến màn hình chi tiết tương ứng khi bấm vào thông báo.
 * - Hỗ trợ đánh dấu đọc tất cả và xóa từng thông báo.
 */
public class NotificationActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private TextView tvToolbarTitle;
    private TextView btnMarkAllRead;

    private ProgressBar pbLoading;
    private RecyclerView rvNotifications;
    private LinearLayout layoutEmpty;

    private NotificationAdapter adapter;
    private final List<NotificationModel> allNotifications = new ArrayList<>();
    private final List<NotificationModel> displayedNotifications = new ArrayList<>();

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String currentUserId = "";
    private ListenerRegistration notificationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

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
        setupRecyclerView();
        setupListeners();
        listenToNotifications();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tvToolbarTitle = findViewById(R.id.tvToolbarTitle);
        btnMarkAllRead = findViewById(R.id.btnMarkAllRead);

        pbLoading = findViewById(R.id.pbLoading);
        rvNotifications = findViewById(R.id.rvNotifications);
        layoutEmpty = findViewById(R.id.layoutEmpty);
    }

    private void setupRecyclerView() {
        adapter = new NotificationAdapter(displayedNotifications, new NotificationAdapter.OnNotificationClickListener() {
            @Override
            public void onItemClick(NotificationModel notification) {
                handleNotificationClick(notification);
            }

            @Override
            public void onDeleteClick(NotificationModel notification) {
                confirmDeleteNotification(notification);
            }
        });

        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        rvNotifications.setAdapter(adapter);
    }

    private void setupListeners() {
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (btnMarkAllRead != null) {
            btnMarkAllRead.setOnClickListener(v -> markAllAsRead());
        }
    }

    /**
     * Lắng nghe thông báo thời gian thực từ Firestore dành riêng cho người dùng hiện tại
     */
    private void listenToNotifications() {
        if (currentUserId.isEmpty()) return;

        pbLoading.setVisibility(View.VISIBLE);

        notificationListener = db.collection("notifications")
                .whereEqualTo("receiverId", currentUserId)
                .addSnapshotListener((querySnapshot, error) -> {
                    pbLoading.setVisibility(View.GONE);

                    if (error != null) {
                        Toast.makeText(this, "Lỗi nạp thông báo: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    allNotifications.clear();
                    if (querySnapshot != null) {
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            try {
                                NotificationModel model = doc.toObject(NotificationModel.class);
                                if (model != null) {
                                    model.setId(doc.getId());
                                    allNotifications.add(model);
                                }
                            } catch (Exception ex) {
                                android.util.Log.e("NotificationActivity", "Bỏ qua thông báo lỗi: " + doc.getId(), ex);
                            }
                        }
                    }

                    // Sắp xếp mới nhất lên đầu trong bộ nhớ để không yêu cầu Composite Index trên Firestore
                    Collections.sort(allNotifications, (a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));

                    displayedNotifications.clear();
                    displayedNotifications.addAll(allNotifications);

                    if (adapter != null) {
                        adapter.notifyDataSetChanged();
                    }

                    if (layoutEmpty != null) {
                        layoutEmpty.setVisibility(displayedNotifications.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                });
    }

    /**
     * Xử lý điều hướng khi người dùng nhấn vào từng thông báo
     */
    private void handleNotificationClick(NotificationModel notification) {
        if (notification == null) return;

        // 1. Đánh dấu đã đọc trên Firestore
        if (!notification.isRead()) {
            notification.setRead(true);
            db.collection("notifications").document(notification.getId())
                    .update("read", true);
            adapter.notifyDataSetChanged();
        }

        String type = notification.getType();
        String refId = notification.getReferenceId();

        if (type == null) type = NotificationModel.TYPE_SYSTEM;

        switch (type) {
            case NotificationModel.TYPE_ISSUE_NEW:
            case NotificationModel.TYPE_ISSUE_UPDATE:
                if (refId != null && !refId.isEmpty()) {
                    Intent intent = new Intent(this, IssueDetailActivity.class);
                    intent.putExtra("ISSUE_ID", refId);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Không tìm thấy mã sự cố liên quan", Toast.LENGTH_SHORT).show();
                }
                break;

            case NotificationModel.TYPE_INVOICE_NEW:
            case NotificationModel.TYPE_INVOICE_REMINDER:
                if (refId != null && !refId.isEmpty()) {
                    // Tải dữ liệu hóa đơn trước khi mở TenantInvoiceDetailActivity
                    pbLoading.setVisibility(View.VISIBLE);
                    db.collection("invoices").document(refId).get()
                            .addOnSuccessListener(doc -> {
                                pbLoading.setVisibility(View.GONE);
                                if (doc.exists()) {
                                    Invoice invoice = doc.toObject(Invoice.class);
                                    if (invoice != null) {
                                        invoice.setId(doc.getId());
                                        Intent intent = new Intent(NotificationActivity.this, TenantInvoiceDetailActivity.class);
                                        intent.putExtra(TenantInvoiceDetailActivity.EXTRA_INVOICE, invoice);
                                        startActivity(intent);
                                    }
                                } else {
                                    Toast.makeText(this, "Hóa đơn này không còn tồn tại!", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .addOnFailureListener(e -> {
                                pbLoading.setVisibility(View.GONE);
                                Toast.makeText(this, "Lỗi tải hóa đơn: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                } else {
                    Toast.makeText(this, "Không tìm thấy mã hóa đơn liên quan", Toast.LENGTH_SHORT).show();
                }
                break;

            case NotificationModel.TYPE_ANNOUNCEMENT:
                if (refId != null && !refId.isEmpty()) {
                    Intent intent = new Intent(this, AnnouncementDetailActivity.class);
                    intent.putExtra("EXTRA_ANNOUNCEMENT_ID", refId);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Không tìm thấy mã bài đăng thông báo", Toast.LENGTH_SHORT).show();
                }
                break;

            case NotificationModel.TYPE_SYSTEM:
            default:
                new AlertDialog.Builder(this)
                        .setTitle(notification.getTitle())
                        .setMessage(notification.getBody())
                        .setPositiveButton("Đóng", null)
                        .show();
                break;
        }
    }

    /**
     * Xác nhận xóa thông báo
     */
    private void confirmDeleteNotification(NotificationModel notification) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa thông báo")
                .setMessage("Bạn có chắc chắn muốn xóa thông báo này?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    db.collection("notifications").document(notification.getId())
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Đã xóa thông báo", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Lỗi khi xóa: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    /**
     * Đánh dấu toàn bộ thông báo của người dùng là đã đọc
     */
    private void markAllAsRead() {
        List<NotificationModel> unreadList = new ArrayList<>();
        for (NotificationModel item : allNotifications) {
            if (!item.isRead()) {
                unreadList.add(item);
            }
        }

        if (unreadList.isEmpty()) {
            Toast.makeText(this, "Tất cả thông báo đã được đọc!", Toast.LENGTH_SHORT).show();
            return;
        }

        WriteBatch batch = db.batch();
        for (NotificationModel item : unreadList) {
            batch.update(db.collection("notifications").document(item.getId()), "read", true);
        }

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    for (NotificationModel item : unreadList) {
                        item.setRead(true);
                    }
                    adapter.notifyDataSetChanged();
                    Toast.makeText(this, "Đã đánh dấu đọc tất cả!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi cập nhật: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (notificationListener != null) {
            notificationListener.remove();
        }
    }
}
