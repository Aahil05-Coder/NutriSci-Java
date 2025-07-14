package ca.yorku.eecs3311.nutrisci.view;

import ca.yorku.eecs3311.nutrisci.dao.UserProfileDAO;
import ca.yorku.eecs3311.nutrisci.model.UserProfile;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

public class ProfileDialog extends JDialog {
    private JComboBox<String> userCombo;
    private JButton selectBtn, newBtn, deleteBtn;
    private UserProfile selectedProfile;

    public ProfileDialog(Frame owner) {
    	super(owner, "Select or Create User", true);
        setSize(400, 220);
        setLocationRelativeTo(owner);
        initUI();
        loadUsers();
    }

    private void initUI() {
        userCombo = new JComboBox<>();
        selectBtn = new JButton("Select");
        newBtn    = new JButton("Create New User");
        deleteBtn = new JButton("Delete User");

        selectBtn.addActionListener(e -> onSelect());
        newBtn.addActionListener(e -> onCreate());
        deleteBtn.addActionListener(e -> onDelete());

        JPanel top = new JPanel();
        top.add(new JLabel("Existing Users:"));
        top.add(userCombo);

        JPanel bottom = new JPanel();
        bottom.add(selectBtn);
        bottom.add(newBtn);
        bottom.add(deleteBtn);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(top, BorderLayout.CENTER);
        getContentPane().add(bottom, BorderLayout.SOUTH);
    }

    private void loadUsers() {
        try {
            UserProfileDAO dao = new UserProfileDAO();
            List<String> names = dao.findAllUsernames();
            userCombo.removeAllItems();

            if (names.isEmpty()) {
                userCombo.setVisible(false);
                deleteBtn.setVisible(false);
                selectBtn.setEnabled(false);
            } else {
                userCombo.setVisible(true);
                deleteBtn.setVisible(true);
                selectBtn.setEnabled(true);
                for (String n : names) userCombo.addItem(n);
                userCombo.setSelectedIndex(0);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void onSelect() {
        String username = (String) userCombo.getSelectedItem();
        if (username == null) {
        	JOptionPane.showMessageDialog(this, "Please select a user first!");
            return;
        }
        try {
            selectedProfile = new UserProfileDAO().findByUsername(username);
            dispose();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void onCreate() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5,5,5,5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        p.add(new JLabel("User:"), gbc);
        JTextField tfUser = new JTextField();
        gbc.gridx = 1; gbc.weightx = 1.0;
        p.add(tfUser, gbc);

        gbc.weightx = 0; gbc.gridx = 0; gbc.gridy++;
        p.add(new JLabel("Gender:"), gbc);
        JRadioButton rbM = new JRadioButton("Male"); 
        JRadioButton rbF = new JRadioButton("Female");
        ButtonGroup bgSex = new ButtonGroup();
        bgSex.add(rbM); bgSex.add(rbF);
        JPanel sexPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        sexPanel.add(rbM); sexPanel.add(rbF);
        gbc.gridx = 1;
        p.add(sexPanel, gbc);

        gbc.gridx = 0; gbc.gridy++;
        p.add(new JLabel("Date of Birth:"), gbc);
        JTextField tfBirth = new JTextField("yyyy-MM-dd");
        gbc.gridx = 1;
        p.add(tfBirth, gbc);

        gbc.gridx = 0; gbc.gridy++;
        p.add(new JLabel("Height:"), gbc);
        JPanel heightPanel = new JPanel(new BorderLayout(5,0));
        JTextField tfH = new JTextField();
        JComboBox<String> cbHUnit = new JComboBox<>(new String[]{"cm","in"});
        heightPanel.add(tfH, BorderLayout.CENTER);
        heightPanel.add(cbHUnit, BorderLayout.EAST);
        gbc.gridx = 1;
        p.add(heightPanel, gbc);

        gbc.gridx = 0; gbc.gridy++;
        p.add(new JLabel("Weight:"), gbc);
        JPanel weightPanel = new JPanel(new BorderLayout(5,0));
        JTextField tfW = new JTextField();
        JComboBox<String> cbWUnit = new JComboBox<>(new String[]{"kg","lb"});
        weightPanel.add(tfW, BorderLayout.CENTER);
        weightPanel.add(cbWUnit, BorderLayout.EAST);
        gbc.gridx = 1;
        p.add(weightPanel, gbc);

        int res = JOptionPane.showConfirmDialog(this, p, "Create New User",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res == JOptionPane.OK_OPTION) {
            try {
                String username = tfUser.getText().trim();
                char sex = rbM.isSelected() ? 'M' : (rbF.isSelected() ? 'F' : '?');
                if (sex == '?') {
                    JOptionPane.showMessageDialog(this, "Please select your gender.");
                    return;
                }
                LocalDate birth = LocalDate.parse(tfBirth.getText().trim());
                double height = Double.parseDouble(tfH.getText().trim());
                double weight = Double.parseDouble(tfW.getText().trim());
                String hUnit = (String) cbHUnit.getSelectedItem();
                String wUnit = (String) cbWUnit.getSelectedItem();

                UserProfile profile = new UserProfile(username, sex, birth, height, hUnit, weight, wUnit);
                new UserProfileDAO().insert(profile);
                loadUsers(); // 创建后刷新用户列表
            } catch (DateTimeParseException dtpe) {
                JOptionPane.showMessageDialog(this, "Invalid date of birth format. It should be yyyy-MM-dd.");
            } catch (NumberFormatException nfe) {
                JOptionPane.showMessageDialog(this, "Invalid format for height or weight. Please enter numbers.");
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    private void onDelete() {
        String username = (String) userCombo.getSelectedItem();
        if (username == null) return;
        int confirm = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to delete user " + username + "?", "Confirm Deletion",
            JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                new UserProfileDAO().softDeleteByUsername(username);
                loadUsers();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }


    public UserProfile showDialog() {
        setVisible(true);
        return selectedProfile;
    }
}
