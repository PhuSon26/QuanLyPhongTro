package com.example.renthub.ui.community;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Model đại diện cho một thông báo trên Bảng tin cộng đồng (Community).
 * Chủ trọ có thể gửi cho toàn bộ người thuê hoặc chọn danh sách các phòng cụ
 * thể.
 */
public class Announcement implements Serializable {

    public static final String TARGET_ALL = "ALL"; // Gửi đến toàn bộ người thuê
    public static final String TARGET_SPECIFIC_ROOMS = "SPECIFIC_ROOMS"; // Gửi đến danh sách phòng được chọn

    private String id;
    private String landlordId; // UID của chủ trọ đăng bài
    private String landlordName; // Tên chủ trọ / Ban quản lý
    private String title; // Tiêu đề thông báo
    private String content; // Nội dung chi tiết thông báo
    private String category; // Thể loại thông báo
    private String targetType; // ALL hoặc SPECIFIC_ROOMS
    private List<String> targetRoomIds; // Danh sách ID phòng được chỉ định (khi targetType là SPECIFIC_ROOMS)
    private List<String> targetRoomNames; // Danh sách tên phòng (để hiển thị nhanh)
    private long createdAt; // Thời điểm đăng (mili giây, kiểu long đảm bảo Serializable)

    public Announcement() {
        targetRoomIds = new ArrayList<>();
        targetRoomNames = new ArrayList<>();
    }

    public Announcement(String id, String landlordId, String landlordName, String title,
            String content, String category, String targetType,
            List<String> targetRoomIds, List<String> targetRoomNames, long createdAt) {
        this.id = id;
        this.landlordId = landlordId;
        this.landlordName = landlordName;
        this.title = title;
        this.content = content;
        this.category = category;
        this.targetType = targetType;
        this.targetRoomIds = targetRoomIds != null ? targetRoomIds : new ArrayList<>();
        this.targetRoomNames = targetRoomNames != null ? targetRoomNames : new ArrayList<>();
        this.createdAt = createdAt;
    }

    // --- GETTERS VÀ SETTERS ---

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLandlordId() {
        return landlordId;
    }

    public void setLandlordId(String landlordId) {
        this.landlordId = landlordId;
    }

    public String getLandlordName() {
        return landlordName;
    }

    public void setLandlordName(String landlordName) {
        this.landlordName = landlordName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public List<String> getTargetRoomIds() {
        if (targetRoomIds == null)
            targetRoomIds = new ArrayList<>();
        return targetRoomIds;
    }

    public void setTargetRoomIds(List<String> targetRoomIds) {
        this.targetRoomIds = targetRoomIds;
    }

    public List<String> getTargetRoomNames() {
        if (targetRoomNames == null)
            targetRoomNames = new ArrayList<>();
        return targetRoomNames;
    }

    public void setTargetRoomNames(List<String> targetRoomNames) {
        this.targetRoomNames = targetRoomNames;
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

    /**
     * Kiểm tra xem thông báo này có áp dụng cho phòng có mã roomId không
     */
    public boolean appliesToRoom(String roomId) {
        if (TARGET_ALL.equalsIgnoreCase(targetType)) {
            return true;
        }
        return roomId != null && targetRoomIds != null && targetRoomIds.contains(roomId);
    }
}
