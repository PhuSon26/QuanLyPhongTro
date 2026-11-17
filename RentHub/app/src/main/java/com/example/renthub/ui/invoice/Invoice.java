package com.example.renthub.ui.invoice;

import java.io.Serializable;

public class Invoice implements Serializable {
    private String id;
    private String roomId;
    private String roomName;
    private String landlordId;
    private String tenantId;
    private String tenantName;
    private String monthYear;
    private double roomPrice;
    private String status;
    private long createdAt;
    private long paidAt;
    private int electricOld;
    private int electricNew;
    private double electricPrice;
    private int waterOld;
    private int waterNew;
    private double waterPrice;

    public Invoice() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }

    public String getLandlordId() { return landlordId; }
    public void setLandlordId(String landlordId) { this.landlordId = landlordId; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getTenantName() { return tenantName; }
    public void setTenantName(String tenantName) { this.tenantName = tenantName; }

    public String getMonthYear() { return monthYear; }
    public void setMonthYear(String monthYear) { this.monthYear = monthYear; }

    public double getRoomPrice() { return roomPrice; }
    public void setRoomPrice(double roomPrice) { this.roomPrice = roomPrice; }

    public int getElectricOld() { return electricOld; }
    public void setElectricOld(int electricOld) { this.electricOld = electricOld; }

    public int getElectricNew() { return electricNew; }
    public void setElectricNew(int electricNew) { this.electricNew = electricNew; }

    public double getElectricPrice() { return electricPrice; }
    public void setElectricPrice(double electricPrice) { this.electricPrice = electricPrice; }

    public int getWaterOld() { return waterOld; }
    public void setWaterOld(int waterOld) { this.waterOld = waterOld; }

    public int getWaterNew() { return waterNew; }
    public void setWaterNew(int waterNew) { this.waterNew = waterNew; }

    public double getWaterPrice() { return waterPrice; }
    public void setWaterPrice(double waterPrice) { this.waterPrice = waterPrice; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

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

    public long getPaidAt() {
        return paidAt;
    }
    public void setPaidAt(Object val) {
        if (val instanceof com.google.firebase.Timestamp) {
            this.paidAt = ((com.google.firebase.Timestamp) val).toDate().getTime();
        } else if (val instanceof Number) {
            this.paidAt = ((Number) val).longValue();
        } else if (val instanceof java.util.Date) {
            this.paidAt = ((java.util.Date) val).getTime();
        } else {
            this.paidAt = 0L;
        }
    }

    // HÀM TÍNH TOÁN (ĐÃ SỬA DẤU TRỪ)
    public int getElectricUsage() {
        return Math.max(0, electricNew - electricOld);
    }

    public double getTotalElectric() {
        return getElectricUsage() * electricPrice;
    }

    public int getWaterUsage() {
        return Math.max(0, waterNew - waterOld);
    }

    public double getTotalWater() {
        return getWaterUsage() * waterPrice;
    }

    public double getTotalAmount() {
        return roomPrice + getTotalElectric() + getTotalWater();
    }
}