package com.facultyams.ui.technical;

import com.facultyams.security.Session;
import com.facultyams.service.UserService;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.KeyEvent;

public class TechnicalProfilePanel extends TechnicalPage {

    private static final long serialVersionUID = 1L;

    private final transient UserService userService = new UserService();
    private final JTextField usernameField = new JTextField(25);
    private final JTextField departmentField = new JTextField(25);
    private final JTextField nameField = new JTextField(25);
    private final JTextField emailField = new JTextField(25);
    private final JTextField contactField = new JTextField(25);
    private String profilePicture;

    public TechnicalProfilePanel() {
        usernameField.setEditable(false);
        departmentField.setEditable(false);

        JPanel form = new JPanel(new GridLayout(0, 2, 10, 10));
        form.add(new JLabel("Username (cannot be changed)"));
        form.add(usernameField);
        form.add(new JLabel("Department"));
        form.add(departmentField);
        form.add(new JLabel("Full name"));
        form.add(nameField);
        form.add(new JLabel("Email"));
        form.add(emailField);
        form.add(new JLabel("Contact number"));
        form.add(contactField);

        JButton save = button("Save Changes", "Save your name, email and contact number",
                KeyEvent.VK_S, this::save);
        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        buttonRow.add(save);

        JPanel box = new JPanel(new BorderLayout(0, 12));
        box.add(form, BorderLayout.NORTH);
        box.add(buttonRow, BorderLayout.CENTER);

        JPanel wrap = new JPanel(new FlowLayout(FlowLayout.LEFT));
        wrap.add(box);

        add(statusArea(), BorderLayout.NORTH);
        add(wrap, BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        int myId = Session.getCurrentUser().getUserId();
        load("Loading profile...", () -> userService.getUserById(myId), me -> {
            usernameField.setText(me.getUsername());
            departmentField.setText(me.getDepartmentName() == null ? "-" : me.getDepartmentName());
            nameField.setText(me.getFullName());
            emailField.setText(me.getEmail());
            contactField.setText(me.getContactNumber() == null ? "" : me.getContactNumber());
            profilePicture = me.getProfilePicture();
        });
    }

    private void save() {
        String name = nameField.getText();
        String email = emailField.getText();
        String contact = contactField.getText();
        act("Saving profile...", () -> {
            userService.updateOwnProfile(name, email, contact, profilePicture);
            return Boolean.TRUE;
        }, done -> showSuccess("Profile updated."));
    }
}