package com.example.renthub.ui.issue;

import com.google.firebase.Timestamp;

/**
 * Model đại diện cho một sự cố báo cáo phòng trọ
 */
public class Issue {
    private String issueId;
    private String roomId;
    private String tenantId;
    private String landlordId; // Mã chủ trọ quản lý phòng gặp sự cố
    private String type; // Loại sự cố (Điện, Nước, Thiết bị, Mạng, Khác)
    private String title;
    private String description;
    private String imageUrl;
    private String status; // Giá trị: "PENDING", "PROCESSING", "DONE"
    private Timestamp createdAt;
    private String landlordNote;
    private String priority; // Mức độ ưu tiên: "KHẨN CẤP", "CAO", "TRUNG BÌNH", "THẤP"

    // Constructor rỗng bắt buộc cho Firebase Firestore
    public Issue() {
    }

    // Constructor cơ bản
    public Issue(String issueId, String roomId, String tenantId, String type, String title,
                 String description, String imageUrl, String status, Timestamp createdAt,
                 String landlordNote) {
        this.issueId = issueId;
        this.roomId = roomId;
        this.tenantId = tenantId;
        this.type = type;
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
        this.status = status;
        this.createdAt = createdAt;
        this.landlordNote = landlordNote;
    }

    // Constructor mở rộng có landlordId và priority
    public Issue(String issueId, String roomId, String tenantId, String landlordId, String type,
                 String title, String description, String imageUrl, String status,
                 Timestamp createdAt, String landlordNote, String priority) {
        this.issueId = issueId;
        this.roomId = roomId;
        this.tenantId = tenantId;
        this.landlordId = landlordId;
        this.type = type;
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
        this.status = status;
        this.createdAt = createdAt;
        this.landlordNote = landlordNote;
        this.priority = priority;
    }

    // Getter và Setter cho các trường
    public String getIssueId() { return issueId; }
    public void setIssueId(String issueId) { this.issueId = issueId; }

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getLandlordId() { return landlordId; }
    public void setLandlordId(String landlordId) { this.landlordId = landlordId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public String getLandlordNote() { return landlordNote; }
    public void setLandlordNote(String landlordNote) { this.landlordNote = landlordNote; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
}
