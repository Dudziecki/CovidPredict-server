package com.example.server.model;

import java.sql.Date;
import java.sql.Timestamp;

public class Forecast {
    private int id;
    private int regionId;
    private Date forecastDate;
    private int predictedCases;
    private int createdBy;
    private Timestamp createdAt;

    public Forecast(int id, int regionId, Date forecastDate, int predictedCases, int createdBy) {
        this.id = id;
        this.regionId = regionId;
        this.forecastDate = forecastDate;
        this.predictedCases = predictedCases;
        this.createdBy = createdBy;
    }

    public int getId() {
        return id;
    }

    public int getRegionId() {
        return regionId;
    }

    public Date getForecastDate() {
        return forecastDate;
    }

    public int getPredictedCases() {
        return predictedCases;
    }

    public int getCreatedBy() {
        return createdBy;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return "Forecast{id=" + id + ", regionId=" + regionId + ", forecastDate=" + forecastDate + ", predictedCases=" + predictedCases + ", createdBy=" + createdBy + ", createdAt=" + createdAt + "}";
    }
}