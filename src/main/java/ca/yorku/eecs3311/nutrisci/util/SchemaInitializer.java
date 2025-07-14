package ca.yorku.eecs3311.nutrisci.util;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class SchemaInitializer {
 public static void createApplicationTables(Connection conn) throws SQLException {
     try (Statement stmt = conn.createStatement()) {
    	 stmt.executeUpdate(
		    "CREATE TABLE IF NOT EXISTS users (" +
		    "id INT AUTO_INCREMENT PRIMARY KEY, " +
		    "username VARCHAR(50) NOT NULL UNIQUE, " +
		    "sex ENUM('M','F') NOT NULL, " +
		    "birthdate DATE NOT NULL, " +
		    "height DOUBLE NOT NULL, " +
		    "height_unit VARCHAR(10) NOT NULL, " +
		    "weight DOUBLE NOT NULL, " +
		    "weight_unit VARCHAR(10) NOT NULL, " +
		    "deleted TINYINT(1) DEFAULT 0, " +
		    "created_at DATETIME DEFAULT CURRENT_TIMESTAMP)"
		);

         stmt.executeUpdate(
             "CREATE TABLE IF NOT EXISTS meals (" +
             "id INT AUTO_INCREMENT PRIMARY KEY, " +
             "user_id INT NOT NULL, " +
             "meal_date DATE NOT NULL, " +
             "meal_type ENUM('BREAKFAST','LUNCH','DINNER','SNACK') NOT NULL, " +
             "created_at DATETIME DEFAULT CURRENT_TIMESTAMP, " +
             "FOREIGN KEY (user_id) REFERENCES users(id))"
         );
         stmt.executeUpdate(
             "CREATE TABLE IF NOT EXISTS meal_items (" +
             "id INT AUTO_INCREMENT PRIMARY KEY, " +
             "meal_id INT NOT NULL, " +
             "food_id INT NOT NULL, " +
             "measure_id INT NOT NULL, " +
             "quantity DOUBLE NOT NULL, " +
             "FOREIGN KEY (meal_id) REFERENCES meals(id), " +
             "FOREIGN KEY (food_id) REFERENCES food_name(foodid), " +
             "FOREIGN KEY (measure_id) REFERENCES measure_name(measureid))"
         );
         stmt.executeUpdate(
             "CREATE TABLE IF NOT EXISTS swap_goals (" +
             "id INT AUTO_INCREMENT PRIMARY KEY, " +
             "user_id INT NOT NULL, " +
             "nutrient_id INT NOT NULL, " +
             "direction ENUM('INCREASE','DECREASE') NOT NULL, " +
             "amount DOUBLE NOT NULL, " +
             "unit VARCHAR(20) NOT NULL, " +
             "intensity VARCHAR(20), " +
             "created_at DATETIME DEFAULT CURRENT_TIMESTAMP, " +
             "FOREIGN KEY (user_id) REFERENCES users(id), " +
             "FOREIGN KEY (nutrient_id) REFERENCES nutrient_name(nutrientid))"
         );
         stmt.executeUpdate(
             "CREATE TABLE IF NOT EXISTS recommendations (" +
             "id INT AUTO_INCREMENT PRIMARY KEY, " +
             "goal_id INT NOT NULL, " +
             "original_item_id INT NOT NULL, " +
             "suggested_food_id INT NOT NULL, " +
             "expected_change DOUBLE, " +
             "score DOUBLE, " +
             "created_at DATETIME DEFAULT CURRENT_TIMESTAMP, " +
             "FOREIGN KEY (goal_id) REFERENCES swap_goals(id), " +
             "FOREIGN KEY (original_item_id) REFERENCES meal_items(id), " +
             "FOREIGN KEY (suggested_food_id) REFERENCES food_name(foodid))"
         );
         stmt.executeUpdate(
             "CREATE TABLE IF NOT EXISTS applied_swaps (" +
             "id INT AUTO_INCREMENT PRIMARY KEY, " +
             "recommendation_id INT NOT NULL, " +
             "applied_at DATETIME DEFAULT CURRENT_TIMESTAMP, " +
             "FOREIGN KEY (recommendation_id) REFERENCES recommendations(id))"
         );
         stmt.executeUpdate(
             "CREATE TABLE IF NOT EXISTS daily_summary (" +
             "id INT AUTO_INCREMENT PRIMARY KEY, " +
             "user_id INT NOT NULL, " +
             "summary_date DATE NOT NULL, " +
             "total_calories DOUBLE NOT NULL, " +
             "carbs_pct DOUBLE, " +
             "proteins_pct DOUBLE, " +
             "fats_pct DOUBLE, " +
             "others_pct DOUBLE, " +
             "FOREIGN KEY (user_id) REFERENCES users(id))"
         );
         stmt.executeUpdate(
             "CREATE TABLE IF NOT EXISTS food_group_guide (" +
             "group_id INT AUTO_INCREMENT PRIMARY KEY, " +
             "group_name VARCHAR(50) NOT NULL, " +
             "recommended_pct DOUBLE NOT NULL)"
         );
     }
 }
}

