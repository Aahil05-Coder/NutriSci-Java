package ca.yorku.eecs3311.nutrisci.model;

public class MealItem {
    private int id;
    private int mealId;
    private int foodId;
    private int measureId;
    private double quantity;

    public MealItem() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getMealId() { return mealId; }
    public void setMealId(int mealId) { this.mealId = mealId; }

    public int getFoodId() { return foodId; }
    public void setFoodId(int foodId) { this.foodId = foodId; }

    public int getMeasureId() { return measureId; }
    public void setMeasureId(int measureId) { this.measureId = measureId; }

    public double getQuantity() { return quantity; }
    public void setQuantity(double quantity) { this.quantity = quantity; }
}
