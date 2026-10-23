package com.example.renthub.ui.room;

import java.io.Serializable;

public class Room implements Serializable {
    private String id;
    private String name;
    private String location;
    private String area;
    private String price;
    private String status; // "AVAILABLE" (Còn trống), "OCCUPIED" (Đang thuê), "MAINTENANCE" (Bảo trì)
    private String landlordId;
    private String tenantName;
    private String tenantPhone;

    public Room() { }

    public Room(String id, String name, String location, String area, String price, String status, String landlordId, String tenantName, String tenantPhone) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.area = area;
        this.price = price;
        this.status = status;
        this.landlordId = landlordId;
        this.tenantName = tenantName;
        this.tenantPhone = tenantPhone;
    }

    // Getters & Setters
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

    public String getLandlordId() { return landlordId; }
    public void setLandlordId(String landlordId) { this.landlordId = landlordId; }

    public String getTenantName() { return tenantName; }
    public void setTenantName(String tenantName) { this.tenantName = tenantName; }

    public String getTenantPhone() { return tenantPhone; }
    public void setTenantPhone(String tenantPhone) { this.tenantPhone = tenantPhone; }
}