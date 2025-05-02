package com.example.server.service;

import com.example.server.model.EpidemicData;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ForecastService {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static class ForecastResult {
        private final LocalDate forecastDate;
        private final double predictedCases;
        private final String createdAt;
        private final String regionName; // Добавляем поле regionName

        public ForecastResult(LocalDate forecastDate, double predictedCases, String regionName) {
            this.forecastDate = forecastDate;
            this.predictedCases = predictedCases;
            this.createdAt = LocalDate.now().format(DATE_FORMATTER);
            this.regionName = regionName; // Инициализируем regionName
        }

        public String getForecastDate() {
            return forecastDate.format(DATE_FORMATTER);
        }

        public double getPredictedCases() {
            return predictedCases;
        }

        public String getCreatedAt() {
            return createdAt;
        }

        public String getRegionName() {
            return regionName;
        }
    }

    public List<ForecastResult> forecastInfections(List<EpidemicData> historicalData, int daysToForecast) {
        if (historicalData == null || historicalData.isEmpty()) {
            return new ArrayList<>();
        }

        // Если только одна запись, возвращаем её значение как прогноз
        if (historicalData.size() == 1) {
            double lastInfected = historicalData.get(0).getInfected();
            LocalDate lastDate = LocalDate.parse(historicalData.get(0).getDate(), DATE_FORMATTER);
            List<ForecastResult> forecastResults = new ArrayList<>();
            String regionName = historicalData.get(0).getRegion(); // Берем регион из первой записи
            for (int i = 1; i <= daysToForecast; i++) {
                LocalDate forecastDate = lastDate.plusDays(i);
                forecastResults.add(new ForecastResult(forecastDate, lastInfected, regionName));
            }
            return forecastResults;
        }

        // Линейная регрессия: y = a + b*x
        int n = historicalData.size();
        double sumX = 0, sumY = 0, sumXY = 0, sumXX = 0;
        for (int i = 0; i < n; i++) {
            sumX += i;
            sumY += historicalData.get(i).getInfected();
            sumXY += i * historicalData.get(i).getInfected();
            sumXX += i * i;
        }

        double b = (n * sumXY - sumX * sumY) / (n * sumXX - sumX * sumX);
        double a = (sumY - b * sumX) / n;

        LocalDate lastDate = LocalDate.parse(historicalData.get(historicalData.size() - 1).getDate(), DATE_FORMATTER);
        List<ForecastResult> forecastResults = new ArrayList<>();
        String regionName = historicalData.get(0).getRegion(); // Берем регион из первой записи
        for (int i = 1; i <= daysToForecast; i++) {
            LocalDate forecastDate = lastDate.plusDays(i);
            double predictedCases = a + b * (n + i - 1);
            if (predictedCases < 0) {
                predictedCases = historicalData.get(historicalData.size() - 1).getInfected(); // Возвращаем последнее значение
            }
            forecastResults.add(new ForecastResult(forecastDate, predictedCases, regionName));
        }

        return forecastResults;
    }
}