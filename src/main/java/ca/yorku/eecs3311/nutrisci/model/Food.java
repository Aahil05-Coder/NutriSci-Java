package ca.yorku.eecs3311.nutrisci.model;

public class Food {
    private final int id;
    private final String description;

    public Food(int id, String description) {
        this.id = id;
        this.description = description;
    }

    public int getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return description;
    }
}
