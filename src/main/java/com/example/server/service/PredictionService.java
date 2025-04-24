//package com.example.server.service;
//
//import com.example.server.dao.CaseDAO;
//import com.example.server.dao.ForecastDAO;
//import com.example.server.model.Case;
//
//import java.util.List;
//
//public class PredictionService {
//    private final CaseDAO caseDAO;
//    private final ForecastDAO forecastDAO;
//
//    public PredictionService(CaseDAO caseDAO, ForecastDAO forecastDAO) {
//        this.caseDAO = caseDAO;
//        this.forecastDAO = forecastDAO;
//    }
//
//    public double predictCases(int regionId) throws java.sql.SQLException {
//        List<Case> cases = caseDAO.getCasesByRegion(regionId);
//        if (cases.isEmpty()) {
//            return 0;
//        }
//
//        // Простая линейная регрессия: y = a + b*x
//        int n = cases.size();
//        double sumX = 0, sumY = 0, sumXY = 0, sumXX = 0;
//        for (int i = 0; i < n; i++) {
//            sumX += i;
//            sumY += cases.get(i).getConfirmed();
//            sumXY += i * cases.get(i).getConfirmed();
//            sumXX += i * i;
//        }
//
//        double b = (n * sumXY - sumX * sumY) / (n * sumXX - sumX * sumX);
//        double a = (sumY - b * sumX) / n;
//
//        // Прогноз на следующий день
//        return a + b * (n + 1);
//    }
//}