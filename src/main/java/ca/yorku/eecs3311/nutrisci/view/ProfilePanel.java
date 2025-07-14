package ca.yorku.eecs3311.nutrisci.view;

import ca.yorku.eecs3311.nutrisci.dao.UserProfileDAO;
import ca.yorku.eecs3311.nutrisci.model.UserProfile;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class ProfilePanel extends JPanel {
    private final UserProfile user;
    private final JTextField usernameField;
    private final JRadioButton maleRb, femaleRb;
    private final JTextField birthField, heightField, weightField;
    private final JComboBox<String> heightUnitCb, weightUnitCb;
    private final JButton saveBtn;

    public ProfilePanel(UserProfile user) {
        this.user = user;
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8,8,8,8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        add(new JLabel("User："), gbc);
        usernameField = new JTextField(user.getUsername());
        usernameField.setEditable(false);
        gbc.gridx = 1;
        add(usernameField, gbc);

        gbc.gridx = 0; gbc.gridy++;
        add(new JLabel("Gender："), gbc);
        maleRb = new JRadioButton("Male");
        femaleRb = new JRadioButton("Female");
        ButtonGroup bg = new ButtonGroup();
        bg.add(maleRb); bg.add(femaleRb);
        JPanel sexPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        sexPanel.add(maleRb); sexPanel.add(femaleRb);
        gbc.gridx = 1;
        add(sexPanel, gbc);

        gbc.gridx = 0; gbc.gridy++;
        add(new JLabel("Date of Birth："), gbc);
        birthField = new JTextField(user.getBirthdate().toString());
        gbc.gridx = 1;
        add(birthField, gbc);

        gbc.gridx = 0; gbc.gridy++;
        add(new JLabel("Height："), gbc);
        JPanel hPanel = new JPanel(new BorderLayout(5,0));
        heightField = new JTextField(String.valueOf(user.getHeight()));
        heightUnitCb = new JComboBox<>(new String[]{"cm","in"});
        hPanel.add(heightField, BorderLayout.CENTER);
        hPanel.add(heightUnitCb, BorderLayout.EAST);
        gbc.gridx = 1;
        add(hPanel, gbc);

        gbc.gridx = 0; gbc.gridy++;
        add(new JLabel("Weight："), gbc);
        JPanel wPanel = new JPanel(new BorderLayout(5,0));
        weightField = new JTextField(String.valueOf(user.getWeight()));
        weightUnitCb = new JComboBox<>(new String[]{"kg","lb"});
        wPanel.add(weightField, BorderLayout.CENTER);
        wPanel.add(weightUnitCb, BorderLayout.EAST);
        gbc.gridx = 1;
        add(wPanel, gbc);

        saveBtn = new JButton("Save Changes");
        gbc.gridx = 0; gbc.gridy++;
        gbc.gridwidth = 2;
        add(saveBtn, gbc);

        initValues();
        bindEvents();
    }

    private void initValues() {
        if (user.getSex() == 'M') maleRb.setSelected(true);
        else femaleRb.setSelected(true);
        heightUnitCb.setSelectedItem(user.getHeightUnit());
        weightUnitCb.setSelectedItem(user.getWeightUnit());
    }

    private void bindEvents() {
        saveBtn.addActionListener(e -> {
            try {
                char sex = maleRb.isSelected() ? 'M' : 'F';

                birthField.setText(birthField.getText().trim());
                LocalDate bd = java.time.LocalDate.parse(birthField.getText().trim(),
                        DateTimeFormatter.ISO_LOCAL_DATE);
                double h = Double.parseDouble(heightField.getText().trim());
                double w = Double.parseDouble(weightField.getText().trim());
                String hu = (String) heightUnitCb.getSelectedItem();
                String wu = (String) weightUnitCb.getSelectedItem();

                user.setSex(sex);
                user.setBirthdate(bd);
                user.setHeight(h);
                user.setHeightUnit(hu);
                user.setWeight(w);
                user.setWeightUnit(wu);

                new UserProfileDAO().update(user);
                JOptionPane.showMessageDialog(this, "Saved successfully!");
                } catch (DateTimeParseException dtpe) {
                    JOptionPane.showMessageDialog(this, "Invalid date of birth format. Expected: YYYY-MM-DD.");
                } catch (NumberFormatException nfe) {
                    JOptionPane.showMessageDialog(this, "Height and weight must be numeric values.");
                } catch (SQLException se) {
                    JOptionPane.showMessageDialog(this, "Database error: " + se.getMessage());
                    se.printStackTrace();
                }

        });
    }
}
