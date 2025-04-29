package com.example.server.dto;

import com.example.server.model.EpidemicData;

import java.util.List;

public class CompareDataResponse {
    private List<EpidemicData> historicalData;
    private List<ForecastDTO> forecasts;

    // Конструктор по умолчанию (нужен для сериализации Jackson)
    public CompareDataResponse() {}

    // Геттеры и сеттеры
    public List<EpidemicData> getHistoricalData() {
        return historicalData;
    }

    public void setHistoricalData(List<EpidemicData> historicalData) {
        this.historicalData = historicalData;
    }

    public List<ForecastDTO> getForecasts() {
        return forecasts;
    }

    public void setForecasts(List<ForecastDTO> forecasts) {
        this.forecasts = forecasts;
    }
}