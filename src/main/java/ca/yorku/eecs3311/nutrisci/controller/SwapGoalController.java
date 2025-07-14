package ca.yorku.eecs3311.nutrisci.controller;

import ca.yorku.eecs3311.nutrisci.dao.NutrientNameDAO;
import ca.yorku.eecs3311.nutrisci.dao.SwapGoalDAO;
import ca.yorku.eecs3311.nutrisci.model.SwapGoal;
import ca.yorku.eecs3311.nutrisci.recommendation.SwapRecommender;

import java.sql.SQLException;
import java.util.*;


public class SwapGoalController {

    private final NutrientNameDAO nutrientDAO = new NutrientNameDAO();
    private final SwapGoalDAO goalDAO = new SwapGoalDAO();
    private final SwapRecommender recommender = new SwapRecommender();

    private Map<String, Integer> nutrientNameToId;
    private Map<Integer, String> nutrientUnits;

    public Map<String, Integer> getNutrientNameToIdMap() throws SQLException {
        if (nutrientNameToId == null) {
            nutrientNameToId = nutrientDAO.getAllNutrientNames();
        }
        return nutrientNameToId;
    }

    public Map<Integer, String> getAllNutrientUnits() throws SQLException {
        if (nutrientUnits == null) {
            nutrientUnits = new HashMap<>();
            Map<String, Integer> map = getNutrientNameToIdMap();
            for (Integer id : map.values()) {
                String unit = nutrientDAO.getUnitByNutrientId(id);
                if (unit != null) {
                    nutrientUnits.put(id, unit);
                }
            }
        }
        return nutrientUnits;
    }

    public String getUnitByNutrientName(String nutrientName) throws SQLException {
        Integer id = getNutrientNameToIdMap().get(nutrientName);
        if (id == null) return null;
        return getAllNutrientUnits().get(id);
    }

    public List<SwapGoal> getGoalsForUser(int userId) throws SQLException {
        return goalDAO.findByUserId(userId);
    }

    public void insertGoal(SwapGoal goal) throws SQLException {
        goalDAO.insert(goal);
    }

    public void deleteGoal(SwapGoal goal) throws SQLException {
        goalDAO.deleteById(goal.getId());
    }

    public List<SwapRecommender.SwapSuggestion> generateSuggestions(List<SwapGoal> goals, List<ca.yorku.eecs3311.nutrisci.model.MealItem> mealItems) {
        return recommender.suggestSwaps(goals, mealItems);
    }
}
