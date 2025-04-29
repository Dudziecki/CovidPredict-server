package com.example.server.dto;

public class CompareDataRequest {
    private String region;
    private String startDate;
    private String endDate;

    // Конструктор по умолчанию (нужен для десериализации Jackson)
    public CompareDataRequest() {}

    // Конструктор с параметрами
    public CompareDataRequest(String region, String startDate, String endDate) {
        this.region = region;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    // Геттеры и сеттеры
    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }
}