// file: src/main/java/ca/yorku/eecs3311/nutrisci/model/Measure.java
package ca.yorku.eecs3311.nutrisci.model;

public class Measure {
    private int measureId;
    private String description;

    public Measure() {}

    public Measure(int measureId, String description) {
        this.measureId = measureId;
        this.description = description;
    }

    public int getMeasureId() {
        return measureId;
    }

    public void setMeasureId(int measureId) {
        this.measureId = measureId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return description;
    }
}
