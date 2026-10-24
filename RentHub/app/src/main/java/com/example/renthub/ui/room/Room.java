package com.example.renthub.ui.room;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Room implements Serializable {
    private String id;
    private String name;
    private String location;
    private String area;
    private String price;
    private String status;
    private String tenantName;
    private String tenantPhone;

    private String landlordId;
    private String landlordName;
    private String landlordPhone;

    // THÔNG TIN SỐ NGƯỜI Ở
    private int maxOccupants = 1; // Mặc định tối đa 1 người
    private List<String> occupants = new ArrayList<>(); // Danh sách người ở cùng (Họ tên)

    private String imageUrl;
    private List<String> images;

    public Room() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }

    public String getPrice() { return price; }
    public void setPrice(String price) { this.price = price; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getTenantName() { return tenantName; }
    public void setTenantName(String tenantName) { this.tenantName = tenantName; }

    public String getTenantPhone() { return tenantPhone; }
    public void setTenantPhone(String tenantPhone) { this.tenantPhone = tenantPhone; }

    public String getLandlordId() { return landlordId; }
    public void setLandlordId(String landlordId) { this.landlordId = landlordId; }

    public String getLandlordName() { return landlordName; }
    public void setLandlordName(String landlordName) { this.landlordName = landlordName; }

    public String getLandlordPhone() { return landlordPhone; }
    public void setLandlordPhone(String landlordPhone) { this.landlordPhone = landlordPhone; }

    public int getMaxOccupants() { return maxOccupants <= 0 ? 1 : maxOccupants; }
    public void setMaxOccupants(int maxOccupants) { this.maxOccupants = maxOccupants; }

    public List<String> getOccupants() {
        if (occupants == null) occupants = new ArrayList<>();
        return occupants;
    }
    public void setOccupants(List<String> occupants) { this.occupants = occupants; }

    // TÍNH TỔNG SỐ NGƯỜI ĐANG Ở (NGƯỜI THUÊ CHÍNH + NGƯỜI Ở CÙNG)
    public int getCurrentOccupantCount() {
        int count = 0;
        if (tenantName != null && !tenantName.trim().isEmpty()) {
            count++;
        }
        if (occupants != null) {
            count += occupants.size();
        }
        return count;
    }

    public boolean isFull() {
        return getCurrentOccupantCount() >= getMaxOccupants();
    }

    public String getImageUrl() {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            if (images != null && !images.isEmpty()) return images.get(0);
            return null;
        }
        return imageUrl;
    }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public List<String> getImages() {
        if (images == null) return new ArrayList<>();
        return images;
    }

    public void setImages(List<String> images) {
        this.images = images;
        if (images != null && !images.isEmpty()) {
            this.imageUrl = images.get(0);
        } else {
            this.imageUrl = null;
        }
    }
}