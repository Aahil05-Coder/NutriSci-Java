package ca.yorku.eecs3311.nutrisci;

import javax.swing.SwingUtilities;
import ca.yorku.eecs3311.nutrisci.util.DatabaseInitializer;
import ca.yorku.eecs3311.nutrisci.model.UserProfile;
import ca.yorku.eecs3311.nutrisci.view.ProfileDialog;
import ca.yorku.eecs3311.nutrisci.view.MainFrame;
import java.util.Locale;
import javax.swing.*;
import java.awt.*;
import java.util.Enumeration;
public class Main {
    public static void main(String[] args) {
    	 Locale.setDefault(Locale.ENGLISH);
    	 setUIFont(new Font("SansSerif", Font.PLAIN, 18));
    	 UIManager.put("Table.rowHeight", 35); 

        try {
        	
            DatabaseInitializer.initializeDatabase();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        SwingUtilities.invokeLater(() -> {
            UserProfile profile = new ProfileDialog(null).showDialog();
            if (profile == null) {
                System.exit(0);
            }
            MainFrame mainFrame = new MainFrame(profile);
            mainFrame.setVisible(true);
        });
    }
    private static void setUIFont(Font font) {
        Enumeration<Object> keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value instanceof Font) {
                UIManager.put(key, font);
            }
        }
    }
}
