package com.example.server.dto;

public class ForecastDTO {
    private String forecastDate;
    private int predictedCases;
    private String createdAt;
    private String regionName;

    public ForecastDTO(String forecastDate, int predictedCases, String createdAt, String regionName) {
        this.forecastDate = forecastDate;
        this.predictedCases = predictedCases;
        this.createdAt = createdAt;
        this.regionName = regionName;
    }

    public String getForecastDate() { return forecastDate; }
    public int getPredictedCases() { return predictedCases; }
    public String getCreatedAt() { return createdAt; }
    public String getRegionName() { return regionName; }
}