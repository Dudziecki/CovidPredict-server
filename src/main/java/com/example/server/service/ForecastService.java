package com.example.server.service;

import com.example.server.model.EpidemicData;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ForecastService {
    private static final double ALPHA = 0.3; // Коэффициент сглаживания
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static class ForecastResult {
        private final LocalDate date;
        private final double predictedInfected;

        public ForecastResult(LocalDate date, double predictedInfected) {
            this.date = date;
            this.predictedInfected = predictedInfected;
        }

        public String getDate() {
            return date.format(DATE_FORMATTER);
        }

        public double getPredictedInfected() {
            return predictedInfected;
        }
    }

    public List<ForecastResult> forecastInfections(List<EpidemicData> historicalData, int daysToForecast) {
        if (historicalData == null || historicalData.isEmpty()) {
            return new ArrayList<>();
        }

        // Выполняем экспоненциальное сглаживание
        List<Double> smoothedValues = new ArrayList<>();
        smoothedValues.add((double) historicalData.get(0).getInfected()); // Начальное значение

        for (int i = 1; i < historicalData.size(); i++) {
            double currentValue = historicalData.get(i).getInfected();
            double previousSmoothed = smoothedValues.get(i - 1);
            double smoothed = ALPHA * currentValue + (1 - ALPHA) * previousSmoothed;
            smoothedValues.add(smoothed);
        }

        // Последнее сглаженное значение
        double lastSmoothed = smoothedValues.get(smoothedValues.size() - 1);

        // Получаем последнюю дату из исторических данных
        LocalDate lastDate = LocalDate.parse(historicalData.get(historicalData.size() - 1).getDate(), DATE_FORMATTER);

        // Прогнозируем на daysToForecast дней вперёд
        List<ForecastResult> forecastResults = new ArrayList<>();
        for (int i = 1; i <= daysToForecast; i++) {
            LocalDate forecastDate = lastDate.plusDays(i);
            // Для простоты используем последнее сглаженное значение как прогноз
            forecastResults.add(new ForecastResult(forecastDate, lastSmoothed));
        }

        return forecastResults;
    }

}