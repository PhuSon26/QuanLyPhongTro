package com.example.renthub.ui.room;

public class Room {

    private final String name;
    private final String location;
    private final String area;
    private final String price;
    private final String status;

    public Room(String name, String location,
                String area, String price, String status) {

        this.name = name;
        this.location = location;
        this.area = area;
        this.price = price;
        this.status = status;
    }

    public String getName() {
        return name;
    }

    public String getLocation() {
        return location;
    }

    public String getArea() {
        return area;
    }

    public String getPrice() {
        return price;
    }

    public String getStatus() {
        return status;
    }
}