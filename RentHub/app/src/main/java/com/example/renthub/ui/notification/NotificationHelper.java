package com.example.renthub.ui.notification;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.renthub.R;
import com.example.renthub.ui.community.Announcement;
import com.example.renthub.ui.community.AnnouncementDetailActivity;
import com.example.renthub.ui.invoice.Invoice;
import com.example.renthub.ui.invoice.TenantInvoiceDetailActivity;
import com.example.renthub.ui.issue.Issue;
import com.example.renthub.ui.issue.IssueDetailActivity;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.DecimalFormat;
import java.util.List;
import java.util.UUID;

/**
 * Lớp tiện ích hỗ trợ tạo, lưu trữ và phát thông báo hệ thống RentHub:
 * - Lưu thông báo vào Cloud Firestore (collection "notifications").
 * - Hỗ trợ cơ chế Fallback thông minh: tự động tìm kiếm người nhận qua thông tin phòng hoặc phân quyền (Role) nếu mã người nhận bị thiếu.
 * - Hiển thị thông báo đẩy cục bộ (Android System Tray Notification) khi có sự kiện mới.
 * - Đảm bảo tuân thủ tiêu chuẩn lập trình của RentHub và chú thích hoàn toàn bằng tiếng Việt.
 */
public class NotificationHelper {

    private static final String TAG = "NotificationHelper";
    public static final String CHANNEL_ID = "renthub_notifications_channel";
    public static final String CHANNEL_NAME = "Thông báo RentHub";
    public static final String CHANNEL_DESC = "Kênh nhận thông báo về sự cố, hóa đơn và tin tức phòng trọ";

    /**
     * Khởi tạo kênh thông báo (NotificationChannel) cho Android 8.0 (API 26) trở lên
     */
    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(CHANNEL_DESC);
            channel.enableVibration(true);

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * Gửi thông báo khi người thuê báo sự cố mới -> Gửi đến Chủ trọ
     */
    public static void sendNewIssueNotification(Context context, Issue issue, String roomName, String tenantName) {
        if (issue == null) return;

        String title = "Sự cố mới: " + (roomName != null && !roomName.isEmpty() ? roomName : "Phòng trọ");
        String body = (tenantName != null && !tenantName.isEmpty() ? tenantName : "Người thuê")
                + " vừa báo sự cố: '" + (issue.getTitle() != null ? issue.getTitle() : "Cần sửa chữa") + "'.";

        String targetLandlordId = issue.getLandlordId();
        if (targetLandlordId != null && !targetLandlordId.trim().isEmpty()) {
            dispatchIssueNotification(context, targetLandlordId, issue, title, body, roomName, tenantName);
        } else {
            // Cơ chế dự phòng: Tìm landlordId qua document phòng tương ứng
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            if (issue.getRoomId() != null && !issue.getRoomId().trim().isEmpty()) {
                db.collection("rooms").document(issue.getRoomId().trim()).get()
                        .addOnSuccessListener(rDoc -> {
                            String foundLandlordId = rDoc.getString("landlordId");
                            if (foundLandlordId != null && !foundLandlordId.trim().isEmpty()) {
                                issue.setLandlordId(foundLandlordId);
                                dispatchIssueNotification(context, foundLandlordId, issue, title, body, roomName, tenantName);
                            } else {
                                broadcastIssueToLandlords(context, issue, title, body, roomName, tenantName);
                            }
                        })
                        .addOnFailureListener(e -> broadcastIssueToLandlords(context, issue, title, body, roomName, tenantName));
            } else {
                broadcastIssueToLandlords(context, issue, title, body, roomName, tenantName);
            }
        }
    }

    private static void broadcastIssueToLandlords(Context context, Issue issue, String title, String body, String roomName, String tenantName) {
        FirebaseFirestore.getInstance().collection("users").get()
                .addOnSuccessListener(querySnapshot -> {
                    boolean sent = false;
                    for (DocumentSnapshot userDoc : querySnapshot.getDocuments()) {
                        String role = userDoc.getString("role");
                        if (role != null && (role.equalsIgnoreCase("LANDLORD")
                                || role.equalsIgnoreCase("CHỦ TRỌ")
                                || role.toLowerCase().contains("chủ"))) {
                            dispatchIssueNotification(context, userDoc.getId(), issue, title, body, roomName, tenantName);
                            sent = true;
                        }
                    }
                    if (!sent) {
                        Log.w(TAG, "Không tìm thấy chủ trọ nào để gửi thông báo sự cố.");
                    }
                });
    }

    private static void dispatchIssueNotification(Context context, String landlordId, Issue issue, String title, String body, String roomName, String tenantName) {
        NotificationModel notification = new NotificationModel(
                UUID.randomUUID().toString(),
                landlordId,
                issue.getTenantId(),
                tenantName != null ? tenantName : "Người thuê",
                NotificationModel.TYPE_ISSUE_NEW,
                title,
                body,
                issue.getIssueId(),
                roomName,
                System.currentTimeMillis(),
                false
        );

        saveNotificationToFirestore(notification);

        if (context != null) {
            Intent intent = new Intent(context, IssueDetailActivity.class);
            intent.putExtra("ISSUE_ID", issue.getIssueId());
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            showSystemTrayNotification(context, title, body, intent, (int) System.currentTimeMillis());
        }
    }

    /**
     * Gửi thông báo khi chủ trọ cập nhật trạng thái sự cố -> Gửi đến Người thuê
     */
    public static void sendIssueStatusUpdateNotification(Context context, String tenantId, Issue issue, String newStatusCode, String landlordNote) {
        if (issue == null) return;

        String statusDisplay = "Đang xử lý";
        if ("DONE".equalsIgnoreCase(newStatusCode)) {
            statusDisplay = "Đã xử lý xong";
        } else if ("PENDING".equalsIgnoreCase(newStatusCode)) {
            statusDisplay = "Chờ xử lý";
        }

        String title = "Cập nhật sự cố: " + (issue.getTitle() != null ? issue.getTitle() : "Phòng trọ");
        StringBuilder bodyBuilder = new StringBuilder("Trạng thái: " + statusDisplay);
        if (landlordNote != null && !landlordNote.trim().isEmpty()) {
            bodyBuilder.append(" - Ghi chú: ").append(landlordNote.trim());
        }
        String body = bodyBuilder.toString();

        String targetTenantId = (tenantId != null && !tenantId.trim().isEmpty()) ? tenantId : issue.getTenantId();

        if (targetTenantId != null && !targetTenantId.trim().isEmpty()) {
            dispatchIssueUpdateNotification(context, targetTenantId, issue, title, body, newStatusCode);
        } else {
            // Cơ chế dự phòng: Thử tìm tenantId từ bảng phòng
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            if (issue.getRoomId() != null && !issue.getRoomId().trim().isEmpty()) {
                db.collection("rooms").document(issue.getRoomId().trim()).get()
                        .addOnSuccessListener(rDoc -> {
                            String foundTenantId = rDoc.getString("tenantId");
                            if (foundTenantId != null && !foundTenantId.trim().isEmpty()) {
                                dispatchIssueUpdateNotification(context, foundTenantId, issue, title, body, newStatusCode);
                            }
                        });
            }
        }
    }

    private static void dispatchIssueUpdateNotification(Context context, String tenantId, Issue issue, String title, String body, String newStatusCode) {
        NotificationModel notification = new NotificationModel(
                UUID.randomUUID().toString(),
                tenantId,
                issue.getLandlordId(),
                "Chủ trọ",
                NotificationModel.TYPE_ISSUE_UPDATE,
                title,
                body,
                issue.getIssueId(),
                newStatusCode,
                System.currentTimeMillis(),
                false
        );

        saveNotificationToFirestore(notification);

        if (context != null) {
            Intent intent = new Intent(context, IssueDetailActivity.class);
            intent.putExtra("ISSUE_ID", issue.getIssueId());
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            showSystemTrayNotification(context, title, body, intent, (int) System.currentTimeMillis());
        }
    }

    /**
     * Gửi thông báo khi phát hành hóa đơn mới -> Gửi đến Người thuê
     */
    public static void sendNewInvoiceNotification(Context context, String tenantId, Invoice invoice) {
        if (invoice == null) return;

        DecimalFormat formatter = new DecimalFormat("#,###");
        String formattedTotal = formatter.format(invoice.getTotalAmount()) + " đ";

        String title = "Hóa đơn mới: Kỳ " + (invoice.getMonthYear() != null ? invoice.getMonthYear() : "");
        String body = "Phòng " + (invoice.getRoomName() != null ? invoice.getRoomName() : "") +
                " đã có hóa đơn tiền trọ: " + formattedTotal + ". Vui lòng kiểm tra và thanh toán.";

        String targetTenantId = (tenantId != null && !tenantId.trim().isEmpty()) ? tenantId : invoice.getTenantId();

        if (targetTenantId != null && !targetTenantId.trim().isEmpty()) {
            dispatchInvoiceNotification(context, targetTenantId, invoice, NotificationModel.TYPE_INVOICE_NEW, title, body, formattedTotal);
        } else {
            resolveTenantAndSendInvoiceNotification(context, invoice, NotificationModel.TYPE_INVOICE_NEW, title, body, formattedTotal);
        }
    }

    /**
     * Gửi thông báo nhắc nhở thanh toán hóa đơn -> Gửi đến Người thuê
     */
    public static void sendInvoiceReminderNotification(Context context, String tenantId, Invoice invoice) {
        if (invoice == null) return;

        DecimalFormat formatter = new DecimalFormat("#,###");
        String formattedTotal = formatter.format(invoice.getTotalAmount()) + " đ";

        String title = "Nhắc nhở thanh toán hóa đơn: Kỳ " + (invoice.getMonthYear() != null ? invoice.getMonthYear() : "");
        String body = "Hóa đơn phòng " + (invoice.getRoomName() != null ? invoice.getRoomName() : "") +
                " tổng tiền " + formattedTotal + " đang chờ thanh toán. Vui lòng thanh toán sớm.";

        String targetTenantId = (tenantId != null && !tenantId.trim().isEmpty()) ? tenantId : invoice.getTenantId();

        if (targetTenantId != null && !targetTenantId.trim().isEmpty()) {
            dispatchInvoiceNotification(context, targetTenantId, invoice, NotificationModel.TYPE_INVOICE_REMINDER, title, body, formattedTotal);
        } else {
            resolveTenantAndSendInvoiceNotification(context, invoice, NotificationModel.TYPE_INVOICE_REMINDER, title, body, formattedTotal);
        }
    }

    private static void resolveTenantAndSendInvoiceNotification(Context context, Invoice invoice, String type, String title, String body, String formattedTotal) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 1. Thử lấy từ roomId
        if (invoice.getRoomId() != null && !invoice.getRoomId().trim().isEmpty()) {
            db.collection("rooms").document(invoice.getRoomId().trim()).get()
                    .addOnSuccessListener(rDoc -> {
                        String tId = rDoc.getString("tenantId");
                        if (tId != null && !tId.trim().isEmpty()) {
                            invoice.setTenantId(tId);
                            dispatchInvoiceNotification(context, tId, invoice, type, title, body, formattedTotal);
                        } else {
                            findTenantByNameOrRole(context, invoice, type, title, body, formattedTotal);
                        }
                    })
                    .addOnFailureListener(e -> findTenantByNameOrRole(context, invoice, type, title, body, formattedTotal));
        } else {
            findTenantByNameOrRole(context, invoice, type, title, body, formattedTotal);
        }
    }

    private static void findTenantByNameOrRole(Context context, Invoice invoice, String type, String title, String body, String formattedTotal) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 2. Thử khớp theo tên người thuê
        if (invoice.getTenantName() != null && !invoice.getTenantName().trim().isEmpty()) {
            db.collection("users").whereEqualTo("fullName", invoice.getTenantName().trim()).get()
                    .addOnSuccessListener(qSnapshot -> {
                        if (!qSnapshot.isEmpty()) {
                            String foundTenantId = qSnapshot.getDocuments().get(0).getId();
                            dispatchInvoiceNotification(context, foundTenantId, invoice, type, title, body, formattedTotal);
                        } else {
                            broadcastInvoiceToTenants(context, invoice, type, title, body, formattedTotal);
                        }
                    })
                    .addOnFailureListener(e -> broadcastInvoiceToTenants(context, invoice, type, title, body, formattedTotal));
        } else {
            broadcastInvoiceToTenants(context, invoice, type, title, body, formattedTotal);
        }
    }

    private static void broadcastInvoiceToTenants(Context context, Invoice invoice, String type, String title, String body, String formattedTotal) {
        FirebaseFirestore.getInstance().collection("users").get()
                .addOnSuccessListener(qSnapshot -> {
                    for (DocumentSnapshot doc : qSnapshot.getDocuments()) {
                        String role = doc.getString("role");
                        if (role != null && (role.equalsIgnoreCase("TENANT")
                                || role.equalsIgnoreCase("NGƯỜI THUÊ")
                                || role.toLowerCase().contains("thuê"))) {
                            dispatchInvoiceNotification(context, doc.getId(), invoice, type, title, body, formattedTotal);
                        }
                    }
                });
    }

    private static void dispatchInvoiceNotification(Context context, String tenantId, Invoice invoice, String type, String title, String body, String formattedTotal) {
        NotificationModel notification = new NotificationModel(
                UUID.randomUUID().toString(),
                tenantId,
                invoice.getLandlordId(),
                "Chủ trọ",
                type,
                title,
                body,
                invoice.getId(),
                formattedTotal,
                System.currentTimeMillis(),
                false
        );

        saveNotificationToFirestore(notification);

        if (context != null) {
            Intent intent = new Intent(context, TenantInvoiceDetailActivity.class);
            intent.putExtra(TenantInvoiceDetailActivity.EXTRA_INVOICE, invoice);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            showSystemTrayNotification(context, title, body, intent, (int) System.currentTimeMillis());
        }
    }

    /**
     * Gửi thông báo bài đăng Bảng tin mới đến danh sách người thuê
     */
    public static void sendAnnouncementNotification(Context context, Announcement announcement, List<String> recipientTenantIds) {
        if (announcement == null) return;

        final String title = "Thông báo mới từ chủ trọ: " + (announcement.getTitle() != null ? announcement.getTitle() : "Tin mới");
        String rawBody = announcement.getContent();
        final String body = (rawBody != null && rawBody.length() > 100) ? rawBody.substring(0, 97) + "..." : (rawBody != null ? rawBody : "");

        if (recipientTenantIds != null && !recipientTenantIds.isEmpty()) {
            for (String tenantId : recipientTenantIds) {
                if (tenantId == null || tenantId.trim().isEmpty()) continue;
                dispatchAnnouncementNotification(context, tenantId, announcement, title, body);
            }
        } else {
            // Nếu không có danh sách người nhận cụ thể: Gửi đến tất cả tài khoản người thuê trong hệ thống
            FirebaseFirestore.getInstance().collection("users").get()
                    .addOnSuccessListener(qSnapshot -> {
                        for (DocumentSnapshot doc : qSnapshot.getDocuments()) {
                            String role = doc.getString("role");
                            if (role != null && (role.equalsIgnoreCase("TENANT")
                                    || role.equalsIgnoreCase("NGƯỜI THUÊ")
                                    || role.toLowerCase().contains("thuê"))) {
                                dispatchAnnouncementNotification(context, doc.getId(), announcement, title, body);
                            }
                        }
                    });
        }

        if (context != null) {
            Intent intent = new Intent(context, AnnouncementDetailActivity.class);
            intent.putExtra("EXTRA_ANNOUNCEMENT_ID", announcement.getId());
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            showSystemTrayNotification(context, title, body, intent, (int) System.currentTimeMillis());
        }
    }

    private static void dispatchAnnouncementNotification(Context context, String tenantId, Announcement announcement, String title, String body) {
        NotificationModel notification = new NotificationModel(
                UUID.randomUUID().toString(),
                tenantId,
                announcement.getLandlordId(),
                announcement.getLandlordName() != null ? announcement.getLandlordName() : "Chủ trọ",
                NotificationModel.TYPE_ANNOUNCEMENT,
                title,
                body,
                announcement.getId(),
                announcement.getCategory(),
                System.currentTimeMillis(),
                false
        );

        saveNotificationToFirestore(notification);
    }

    /**
     * Lưu thông báo vào Cloud Firestore
     */
    public static void saveNotificationToFirestore(NotificationModel notification) {
        if (notification == null || notification.getId() == null) return;

        FirebaseFirestore.getInstance()
                .collection("notifications")
                .document(notification.getId())
                .set(notification)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Đã lưu thông báo Firestore: " + notification.getId()))
                .addOnFailureListener(e -> Log.e(TAG, "Lỗi lưu thông báo Firestore: " + e.getMessage()));
    }

    /**
     * Hiển thị thông báo trên thanh trạng thái thiết bị (System Tray)
     */
    public static void showSystemTrayNotification(Context context, String title, String message, Intent intent, int notificationId) {
        if (context == null) return;

        // Kiểm tra cài đặt người dùng có bật nhận thông báo không
        SharedPreferences prefs = context.getSharedPreferences("RentHubSettings", Context.MODE_PRIVATE);
        boolean isNotificationEnabled = prefs.getBoolean("PUSH_NOTIFICATIONS_ENABLED", true);
        if (!isNotificationEnabled) {
            Log.d(TAG, "Người dùng đã tắt nhận thông báo đẩy trong Cài đặt");
            return;
        }

        // Tạo kênh thông báo
        createNotificationChannel(context);

        // Kiểm tra quyền POST_NOTIFICATIONS trên Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "Chưa được cấp quyền POST_NOTIFICATIONS, bỏ qua hiển thị thanh trạng thái");
                return;
            }
        }

        PendingIntent pendingIntent = null;
        if (intent != null) {
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags |= PendingIntent.FLAG_IMMUTABLE;
            }
            pendingIntent = PendingIntent.getActivity(context, notificationId, intent, flags);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.rh_ic_bell)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        if (pendingIntent != null) {
            builder.setContentIntent(pendingIntent);
        }

        try {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.notify(notificationId, builder.build());
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException khi hiển thị thông báo: " + e.getMessage());
        }
    }
}
