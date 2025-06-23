package ca.yorku.eecs3311.nutrisci.model;

import java.time.LocalDate;

public class UserProfile {
    private int id;
    private String username;
    private char sex;
    private LocalDate birthdate;
    private double height;
    private double weight;
    private String heightUnit;  
    private String weightUnit; 

    public UserProfile() {}
    public UserProfile(String username, char sex, LocalDate birthdate,
            double height, String heightUnit,
            double weight, String weightUnit) {
		this.username = username;
		this.sex = sex;
		this.birthdate = birthdate;
		this.height = height;
		this.heightUnit = heightUnit;
		this.weight = weight;
		this.weightUnit = weightUnit;
		}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public char getSex() { return sex; }
    public void setSex(char sex) { this.sex = sex; }

    public LocalDate getBirthdate() { return birthdate; }
    public void setBirthdate(LocalDate birthdate) { this.birthdate = birthdate; }

    public double getHeight() { return height; }
    public void setHeight(double height) { this.height = height; }

    public double getWeight() { return weight; }
    public void setWeight(double weight) { this.weight = weight; }

    public String getHeightUnit() { return heightUnit; }
    public void setHeightUnit(String heightUnit) { this.heightUnit = heightUnit; }

    public String getWeightUnit() { return weightUnit; }
    public void setWeightUnit(String weightUnit) { this.weightUnit = weightUnit; }
}
