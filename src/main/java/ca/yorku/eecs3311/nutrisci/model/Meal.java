package ca.yorku.eecs3311.nutrisci.model;

import java.time.LocalDate;

public class Meal {
    private int id;
    private int userId;
    private LocalDate mealDate;
    private String mealType;

    public Meal() {}

    // getters/setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public LocalDate getMealDate() { return mealDate; }
    public void setMealDate(LocalDate mealDate) { this.mealDate = mealDate; }

    public String getMealType() { return mealType; }
    public void setMealType(String mealType) { this.mealType = mealType; }
}