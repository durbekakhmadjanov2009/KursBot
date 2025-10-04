package org.example;

public class Course {
    private int id;
    private String name;
    private String duration;
    private String price;
    private String description;
    private String detailsUrl;

    public Course() {}

    public Course(int id, String name, String duration, String price,
                  String description, String detailsUrl) {
        this.id = id;
        this.name = name;
        this.duration = duration;
        this.price = price;
        this.description = description;
        this.detailsUrl = detailsUrl;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDetailsUrl() {
        return detailsUrl;
    }

    public void setDetailsUrl(String detailsUrl) {
        this.detailsUrl = detailsUrl;
    }

    // Getters and setters
    // ...
}
