package ca.yorku.eecs3311.nutrisci.model;

import java.time.LocalDate;
import java.util.List;

public class MealRecord {
    private int recordId;
    private int userId;
    private LocalDate date;
    private String mealType;
    private List<MealItem> items;

    public MealRecord() {}

    public MealRecord(int recordId, int userId, LocalDate date, String mealType, List<MealItem> items) {
        this.recordId = recordId;
        this.userId = userId;
        this.date = date;
        this.mealType = mealType;
        this.items = items;
    }

    public int getRecordId() {
        return recordId;
    }
    public void setRecordId(int recordId) {
        this.recordId = recordId;
    }

    public int getUserId() {
        return userId;
    }
    public void setUserId(int userId) {
        this.userId = userId;
    }

    public LocalDate getDate() {
        return date;
    }
    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getMealType() {
        return mealType;
    }
    public void setMealType(String mealType) {
        this.mealType = mealType;
    }

    public List<MealItem> getItems() {
        return items;
    }
    public void setItems(List<MealItem> items) {
        this.items = items;
    }
}