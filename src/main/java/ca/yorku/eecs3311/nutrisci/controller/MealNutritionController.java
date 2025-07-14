package ca.yorku.eecs3311.nutrisci.controller;

import ca.yorku.eecs3311.nutrisci.dao.ConversionFactorDAO;
import ca.yorku.eecs3311.nutrisci.dao.NutrientAmountDAO;
import ca.yorku.eecs3311.nutrisci.model.MealItem;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MealNutritionController {

    private final ConversionFactorDAO factorDAO = new ConversionFactorDAO();
    private final NutrientAmountDAO nutrientDAO = new NutrientAmountDAO();

    public double calculateTotalCalories(List<MealItem> items) throws SQLException {
        double total = 0.0;
        for (MealItem mi : items) {
            double weight = mi.getQuantity() * factorDAO.getFactor(mi.getFoodId(), mi.getMeasureId());
            Map<Integer, Double> nutrients = nutrientDAO.getNutrientMapByFoodId(mi.getFoodId());
            Double kcal = nutrients.get(208); // Energy (kcal)
            System.out.println("DEBUG: foodId=" + mi.getFoodId() + ", measureId=" + mi.getMeasureId() + ", quantity=" + mi.getQuantity() + ", factor=" + weight + ", kcal=" + kcal);
            if (kcal != null) {
                double itemKcal = kcal * weight / 100.0;
                System.out.println("DEBUG: Calculated item kcal = " + itemKcal);
                total += itemKcal;
            }
        }
        System.out.println("DEBUG: Total calculated calories = " + total);
        return total;
    }

    public Map<Integer, Double> calculateNutrientSummary(List<MealItem> items) throws SQLException {
        Map<Integer, Double> result = new HashMap<>();
        for (MealItem mi : items) {
            double weight = mi.getQuantity() * factorDAO.getFactor(mi.getFoodId(), mi.getMeasureId());
            Map<Integer, Double> nutrients = nutrientDAO.getNutrientMapByFoodId(mi.getFoodId());
            for (Map.Entry<Integer, Double> entry : nutrients.entrySet()) {
                double added = entry.getValue() * weight / 100.0;
                result.merge(entry.getKey(), added, Double::sum);
            }
        }
        return result;
    }
}
