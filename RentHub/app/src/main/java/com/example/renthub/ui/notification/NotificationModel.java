package com.example.renthub.ui.notification;

import java.io.Serializable;

/**
 * Model đại diện cho một thông báo trong hệ thống RentHub.
 * Phục vụ lưu trữ và hiển thị thông báo cá nhân cho Chủ trọ và Người thuê.
 */
public class NotificationModel implements Serializable {

    // Các hằng số định danh loại thông báo
    public static final String TYPE_ISSUE_NEW = "ISSUE_NEW";               // Người thuê báo sự cố mới
    public static final String TYPE_ISSUE_UPDATE = "ISSUE_UPDATE";         // Trạng thái sự cố được cập nhật
    public static final String TYPE_INVOICE_NEW = "INVOICE_NEW";           // Có hóa đơn tiền trọ mới
    public static final String TYPE_INVOICE_REMINDER = "INVOICE_REMINDER"; // Nhắc nhở thanh toán hóa đơn
    public static final String TYPE_ANNOUNCEMENT = "ANNOUNCEMENT";         // Thông báo mới từ chủ trọ trong Community
    public static final String TYPE_SYSTEM = "SYSTEM";                     // Thông báo chung từ hệ thống

    private String id;
    private String receiverId;     // UID của người nhận (Landlord hoặc Tenant)
    private String senderId;       // UID của người gửi (hoặc SYSTEM)
    private String senderName;     // Tên hiển thị người gửi
    private String type;           // Loại thông báo (khớp các hằng số ở trên)
    private String title;          // Tiêu đề thông báo
    private String body;           // Nội dung tóm tắt của thông báo
    private String referenceId;    // Mã đối tượng liên quan (issueId, invoiceId, announcementId)
    private String extraData;      // Dữ liệu mở rộng (ví dụ Tên phòng, Số tiền...)
    private long createdAt;        // Thời điểm tạo thông báo (mili giây, kiểu long đảm bảo Serializable)
    private boolean read;          // Trạng thái đã đọc hay chưa

    public NotificationModel() {
        // Constructor rỗng bắt buộc cho Firestore serialize/deserialize
    }

    public NotificationModel(String id, String receiverId, String senderId, String senderName,
                             String type, String title, String body, String referenceId,
                             String extraData, long createdAt, boolean read) {
        this.id = id;
        this.receiverId = receiverId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.type = type;
        this.title = title;
        this.body = body;
        this.referenceId = referenceId;
        this.extraData = extraData;
        this.createdAt = createdAt;
        this.read = read;
    }

    // --- GETTERS VÀ SETTERS ---

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getExtraData() {
        return extraData;
    }

    public void setExtraData(String extraData) {
        this.extraData = extraData;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Object val) {
        if (val instanceof com.google.firebase.Timestamp) {
            this.createdAt = ((com.google.firebase.Timestamp) val).toDate().getTime();
        } else if (val instanceof Number) {
            this.createdAt = ((Number) val).longValue();
        } else if (val instanceof java.util.Date) {
            this.createdAt = ((java.util.Date) val).getTime();
        } else {
            this.createdAt = 0L;
        }
    }

    @com.google.firebase.firestore.PropertyName("read")
    public boolean isRead() {
        return read;
    }

    @com.google.firebase.firestore.PropertyName("read")
    public void setRead(boolean read) {
        this.read = read;
    }
}

