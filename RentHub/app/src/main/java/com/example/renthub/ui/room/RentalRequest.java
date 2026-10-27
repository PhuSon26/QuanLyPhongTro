package com.example.renthub.ui.room;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class RentalRequest implements Serializable {
    private String id;
    private String roomId;
    private String roomName;
    private String roomPrice;
    private String roomLocation;

    private String landlordId;
    private String landlordName;
    private String landlordPhone;

    private String tenantId;
    private String tenantName;
    private String tenantPhone;
    private String tenantIdCard;

    private String startDate;
    private String duration;
    private String note;
    private List<String> occupants = new ArrayList<>();

    // PENDING (Đang chờ duyệt), APPROVED (Đã chấp thuận), REJECTED (Bị từ chối)
    private String status = "PENDING";
    private String rejectReason;
    private Date createdAt;

    public RentalRequest() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }

    public String getRoomPrice() { return roomPrice; }
    public void setRoomPrice(String roomPrice) { this.roomPrice = roomPrice; }

    public String getRoomLocation() { return roomLocation; }
    public void setRoomLocation(String roomLocation) { this.roomLocation = roomLocation; }

    public String getLandlordId() { return landlordId; }
    public void setLandlordId(String landlordId) { this.landlordId = landlordId; }

    public String getLandlordName() { return landlordName; }
    public void setLandlordName(String landlordName) { this.landlordName = landlordName; }

    public String getLandlordPhone() { return landlordPhone; }
    public void setLandlordPhone(String landlordPhone) { this.landlordPhone = landlordPhone; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getTenantName() { return tenantName; }
    public void setTenantName(String tenantName) { this.tenantName = tenantName; }

    public String getTenantPhone() { return tenantPhone; }
    public void setTenantPhone(String tenantPhone) { this.tenantPhone = tenantPhone; }

    public String getTenantIdCard() { return tenantIdCard; }
    public void setTenantIdCard(String tenantIdCard) { this.tenantIdCard = tenantIdCard; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public List<String> getOccupants() {
        if (occupants == null) occupants = new ArrayList<>();
        return occupants;
    }
    public void setOccupants(List<String> occupants) { this.occupants = occupants; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}
