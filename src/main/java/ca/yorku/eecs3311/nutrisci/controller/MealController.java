package ca.yorku.eecs3311.nutrisci.controller;

import ca.yorku.eecs3311.nutrisci.dao.MealDAO;
import ca.yorku.eecs3311.nutrisci.dao.MealItemDAO;
import ca.yorku.eecs3311.nutrisci.model.Meal;
import ca.yorku.eecs3311.nutrisci.model.MealItem;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class MealController {

    private final MealDAO mealDAO = new MealDAO();
    private final MealItemDAO itemDAO = new MealItemDAO();

    public void saveMeal(int userId, LocalDate date, String mealType, List<MealItem> items) throws SQLException {
        if (!"SNACK".equalsIgnoreCase(mealType)) {
            List<Meal> existingMeals = mealDAO.findMealsByUser(userId);
            for (Meal meal : existingMeals) {
                if (meal.getMealDate().equals(date) && meal.getMealType().equalsIgnoreCase(mealType)) {
                    throw new SQLException("You have already logged " + mealType + " on " + date);
                }
            }
        }

        int mealId = mealDAO.insertMeal(userId, date, mealType);
        for (MealItem mi : items) {
            mi.setMealId(mealId);
        }
        itemDAO.insertBatch(items);
    }

    public List<Meal> getMeals(int userId) throws SQLException {
        return mealDAO.findMealsByUser(userId);
    }

    public void deleteMeal(int mealId) throws SQLException {
        mealDAO.deleteMeal(mealId);
    }


    public List<MealItem> getMealItems(int mealId) throws SQLException {
        return itemDAO.findByMealId(mealId);
    }
}
