// File: SwapGoal.java
package ca.yorku.eecs3311.nutrisci.model;

public class SwapGoal {
    private int id;
    private int userId;
    private String nutrient;
    private String direction;  // "INCREASE" or "DECREASE"
    private double amount;
    private String intensity;
    private String unit; 

    public SwapGoal() {}

    public SwapGoal(int userId, String nutrient, String direction, double amount, String intensity, String unit) {
        this.userId = userId;
        this.nutrient = nutrient;
        this.direction = direction;
        this.amount = amount;
        this.intensity = intensity;
        this.unit = unit;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getNutrient() { return nutrient; }
    public void setNutrient(String nutrient) { this.nutrient = nutrient; }

    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getIntensity() { return intensity; }
    public void setIntensity(String intensity) { this.intensity = intensity; }
    
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    @Override
    public String toString() {
        String amountStr = unit != null && !unit.isEmpty() ? amount + unit : amount + "";
        String intensityStr = (intensity != null && !intensity.isEmpty()) ? " (" + intensity + ")" : "";
        return direction + " " + nutrient + " by " + amountStr + intensityStr;
    }
}
