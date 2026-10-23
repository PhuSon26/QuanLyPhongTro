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

    public String getImageUrl() {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            if (images != null && !images.isEmpty()) return images.get(0);
            return null; // Không gán ảnh giả
        }
        return imageUrl;
    }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public List<String> getImages() {
        if (images == null) {
            return new ArrayList<>(); // Trả về danh sách rỗng nếu chưa thêm ảnh
        }
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