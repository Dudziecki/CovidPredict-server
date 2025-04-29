package com.example.server.dto;

public class ForecastDTO {
    private String forecastDate;
    private int predictedCases;
    private String createdAt;

    public ForecastDTO(String forecastDate, int predictedCases, String createdAt) {
        this.forecastDate = forecastDate;
        this.predictedCases = predictedCases;
        this.createdAt = createdAt;
    }

    public String getForecastDate() {
        return forecastDate;
    }

    public void setForecastDate(String forecastDate) {
        this.forecastDate = forecastDate;
    }

    public int getPredictedCases() {
        return predictedCases;
    }

    public void setPredictedCases(int predictedCases) {
        this.predictedCases = predictedCases;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}