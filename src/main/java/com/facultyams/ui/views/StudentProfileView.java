package com.facultyams.ui.views;

import com.facultyams.model.Student;
import com.facultyams.model.profile.StudentStatus;
import com.facultyams.service.DataService;
import com.facultyams.service.profile.InMemoryStudentProfileService;
import com.facultyams.service.profile.StudentProfileService;
import com.facultyams.service.profile.validation.PersonalEmailValidator;
import com.facultyams.service.profile.validation.ResidentialAddressValidator;
import com.facultyams.service.profile.validation.SriLankanPhoneValidator;
import com.facultyams.service.profile.validation.Validator;
import com.facultyams.ui.components.*;
import com.facultyams.ui.theme.Theme;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

/** Profile screen with aligned academic/contact details and guarded edit mode. */
public final class StudentProfileView extends JPanel {
    private static final DateTimeFormatter UPDATED_FORMAT = DateTimeFormatter.ofPattern("dd MMM uuuu, h:mm a");
    private final StudentProfileService profileService =
            new InMemoryStudentProfileService(DataService.getInstance());
    private final List<FormField> editableFields = new ArrayList<>();
    private final List<JTextField> phoneInputs = new ArrayList<>();
    private final Validator<String> addressValidator = new ResidentialAddressValidator();
    private final Validator<String> phoneValidator = new SriLankanPhoneValidator();
    private final JPanel headerActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
    private final JPanel actionBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 12));
    private final JProgressBar saveProgress = new JProgressBar();
    private final JLabel lastUpdated = new JLabel(" ");
    private final JLabel completeness = new JLabel();
    private final JProgressBar completenessBar = new JProgressBar(0, 100);
    private AvatarLabel avatar;
    private JPanel avatarHolder;
    private JButton photoButton;
    private FormField universityEmail;
    private FormField personalEmail;
    private FormField mobile;
    private FormField emergencyNumber;
    private FormField address;
    private JTextField emergencyPhoneInput;
    private JComboBox<String> relationshipBox;
    private JPanel relationshipControl;
    private CardLayout relationshipLayout;
    private JTextField relationshipDisplay;
    private boolean editing;
    private boolean saving;
    private boolean dirty;
    private boolean validating;
    private String originalEmail;
    private String originalPhone;
    private String originalEmergency;
    private String originalRelationship;
    private String originalAddress;
    private String selectedPhotoPath;
    private float transitionAlpha = 1f;
    private Timer transitionTimer;

    /** Constructs the profile screen and its responsive scrollable layout. */
    public StudentProfileView() {
        setOpaque(true);
        setBackground(Theme.TIMETABLE_BG);
        setLayout(new BorderLayout());
        ProfileContent content = new ProfileContent();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(0, 0, Theme.PROFILE_PAGE_PADDING, 0));
        JComponent header = createHeader();
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(header);
        content.add(Box.createVerticalStrut(Theme.PROFILE_CARD_GAP));
        JComponent columns = createProfileColumns();
        columns.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(columns);

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUI(new ThinScrollBarUI());
        scroll.getVerticalScrollBar().setUnitIncrement(24);
        add(scroll, BorderLayout.CENTER);
        actionBar.setOpaque(true);
        actionBar.setBackground(Color.WHITE);
        actionBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.TIMETABLE_CARD_BORDER));
        actionBar.setVisible(false);
        add(actionBar, BorderLayout.SOUTH);
        refreshFieldsFromModel();
    }

    private JComponent createHeader() {
        CardPanel card = new CardPanel(12);
        card.setLayout(new BorderLayout(16, 0));
        card.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));
        card.setPreferredSize(new Dimension(0, 88));
        card.setMinimumSize(new Dimension(0, 88));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 88));
        JPanel titleBox = new JPanel();
        titleBox.setOpaque(false);
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Student Profile");
        title.setFont(Theme.TIMETABLE_TITLE);
        title.setForeground(Theme.TEXT_MAIN);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel subtitle = new JLabel("Manage your contact information and view academic records.");
        subtitle.setFont(Theme.FONT_BODY);
        subtitle.setForeground(Theme.TEXT_MUTED);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleBox.add(title);
        titleBox.add(Box.createVerticalStrut(4));
        titleBox.add(subtitle);
        headerActions.setOpaque(false);
        rebuildHeaderActions();
        card.add(titleBox, BorderLayout.CENTER);
        card.add(headerActions, BorderLayout.EAST);
        return card;
    }

    private JPanel createProfileColumns() {
        JPanel columns = new JPanel(new GridBagLayout());
        columns.setOpaque(false);
        columns.setAlignmentX(Component.LEFT_ALIGNMENT);
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        c.gridx = 0;
        c.weightx = 0;
        c.insets = new Insets(0, 0, 0, 24);
        JPanel summary = createSummaryCard();
        summary.setPreferredSize(new Dimension(340, summary.getPreferredSize().height));
        summary.setMinimumSize(new Dimension(300, 1));
        columns.add(summary, c);
        c.gridx = 1;
        c.weightx = 1;
        c.insets = new Insets(0, 0, 0, 0);
        JPanel details = createDetailsCard();
        details.setPreferredSize(new Dimension(560, details.getPreferredSize().height));
        details.setMinimumSize(new Dimension(420, 1));
        columns.add(details, c);
        columns.setPreferredSize(new Dimension(900, Math.max(summary.getPreferredSize().height,
                details.getPreferredSize().height)));
        columns.setMinimumSize(new Dimension(820, 1));
        columns.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        return columns;
    }

    private JPanel createSummaryCard() {
        Student student = profileService.getProfile();
        CardPanel card = new CardPanel(12);
        card.setLayout(new GridBagLayout());
        card.setBorder(BorderFactory.createEmptyBorder(Theme.PROFILE_CARD_PADDING, Theme.PROFILE_CARD_PADDING,
                Theme.PROFILE_CARD_PADDING, Theme.PROFILE_CARD_PADDING));

        avatar = new AvatarLabel(student.getProfileImageUrl(), initials(student.getFullName()), 112);
        avatarHolder = new JPanel(new GridBagLayout());
        avatarHolder.setOpaque(false);
        avatarHolder.setPreferredSize(new Dimension(132, 132));
        avatarHolder.setMinimumSize(new Dimension(132, 132));
        avatarHolder.setMaximumSize(new Dimension(132, 132));
        avatarHolder.setAlignmentX(Component.CENTER_ALIGNMENT);
        GridBagConstraints avatarConstraints = new GridBagConstraints();
        avatarConstraints.gridx = 0;
        avatarConstraints.gridy = 0;
        avatarConstraints.anchor = GridBagConstraints.CENTER;
        avatarHolder.add(avatar, avatarConstraints);
        photoButton = new JButton(new ProfileIcon(ProfileIcon.Shape.CAMERA, Color.WHITE, 16)) {
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                Theme.setAntiAliasing(g);
                g.setColor(Theme.PRIMARY);
                g.fillOval(1, 1, getWidth() - 2, getHeight() - 2);
                g.dispose();
                super.paintComponent(graphics);
            }
        };
        photoButton.setToolTipText("Change profile photo");
        photoButton.setPreferredSize(new Dimension(34, 34));
        photoButton.setBorder(BorderFactory.createEmptyBorder());
        photoButton.setContentAreaFilled(false);
        photoButton.setFocusPainted(false);
        photoButton.setVisible(false);
        photoButton.addActionListener(e -> {
            JPopupMenu menu = new JPopupMenu();
            JMenuItem change = new JMenuItem("Change photo");
            change.addActionListener(event -> choosePhoto());
            JMenuItem remove = new JMenuItem("Remove photo");
            remove.addActionListener(event -> removePhoto());
            menu.add(change);
            menu.add(remove);
            menu.show(photoButton, 0, photoButton.getHeight());
        });
        avatarConstraints.anchor = GridBagConstraints.SOUTHEAST;
        avatarConstraints.insets = new Insets(0, 0, 6, 4);
        avatarHolder.add(photoButton, avatarConstraints);

        JLabel name = new JLabel(student.getFullName());
        name.setFont(Theme.PROFILE_STUDENT_NAME);
        name.setForeground(Theme.TEXT_MAIN);
        name.setHorizontalAlignment(SwingConstants.CENTER);
        JPanel idRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        idRow.setOpaque(false);
        JLabel id = new JLabel(student.getRegNo());
        id.setFont(Theme.FONT_BODY_BOLD);
        id.setForeground(Theme.PRIMARY);
        JButton copy = iconButton(ProfileIcon.Shape.COPY, "Copy student ID");
        copy.addActionListener(e -> {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(student.getRegNo()), null);
            showToast("Copied student ID", true);
        });
        idRow.add(id);
        idRow.add(copy);
        idRow.setPreferredSize(new Dimension(120, idRow.getPreferredSize().height));
        JLabel program = new JLabel(student.getDegreeProgram());
        program.setFont(Theme.FONT_BODY);
        program.setForeground(Theme.TEXT_MUTED);
        program.setHorizontalAlignment(SwingConstants.CENTER);
        StudentStatus status = student.getStatus() == null ? StudentStatus.ACTIVE : student.getStatus();
        JLabel active = new JLabel(status.name().charAt(0) + status.name().substring(1).toLowerCase()
                + (status == StudentStatus.ACTIVE ? " Student" : ""));
        active.setOpaque(true);
        active.setBackground(status == StudentStatus.ACTIVE ? Theme.ACCENT_GREEN_BG : Theme.ACCENT_GRAY_BG);
        active.setForeground(status == StudentStatus.ACTIVE ? Theme.PROFILE_SUCCESS : Theme.TEXT_MUTED);
        active.setFont(Theme.FONT_SMALL.deriveFont(Font.BOLD));
        active.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));

        JPanel summaryRows = new JPanel(new GridBagLayout());
        summaryRows.setOpaque(false);
        double cgpa = student.getCgpa();
        addSummaryRow(summaryRows, createCgpaRow(cgpa), 0, false);
        addSummaryRow(summaryRows, createSummaryRow("Department", student.getDepartment()), 1, true);
        addSummaryRow(summaryRows, createAdvisorRow(student.getAcademicAdvisor()), 2, true);
        addSummaryRow(summaryRows, createSummaryRow("Credits completed", Integer.toString(student.getCreditsCompleted())), 3, true);
        addSummaryRow(summaryRows, createSummaryRow("Attendance", String.format("%.0f%%", student.getAttendancePercentage())), 4, true);
        completeness.setFont(Theme.FONT_SMALL.deriveFont(Font.BOLD));
        completeness.setForeground(Theme.TEXT_MUTED);
        completeness.setHorizontalAlignment(SwingConstants.LEFT);
        completenessBar.setValue(completionPercent(student));
        completenessBar.setBorderPainted(false);
        completenessBar.setForeground(Theme.PRIMARY);
        completenessBar.setBackground(Theme.PRIMARY_LIGHT);
        completenessBar.setPreferredSize(new Dimension(100, 5));
        completenessBar.setMinimumSize(new Dimension(10, 5));
        completenessBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 5));
        JPanel completenessPanel = new JPanel(new BorderLayout(0, 8));
        completenessPanel.setOpaque(false);
        completenessPanel.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
        completenessPanel.add(completeness, BorderLayout.NORTH);
        completenessPanel.add(completenessBar, BorderLayout.SOUTH);

        int row = 0;
        addSummaryContent(card, avatarHolder, row++, GridBagConstraints.CENTER, 0, 0);
        addSummaryContent(card, name, row++, GridBagConstraints.CENTER, 6, 0);
        addSummaryContent(card, idRow, row++, GridBagConstraints.CENTER, 4, 0);
        addSummaryContent(card, program, row++, GridBagConstraints.CENTER, 4, 0);
        addSummaryContent(card, active, row++, GridBagConstraints.CENTER, 8, 0);
        addSummaryContent(card, createDivider(), row++, GridBagConstraints.CENTER, 12, 0);
        addSummaryContent(card, summaryRows, row++, GridBagConstraints.WEST, 0, 0);
        addSummaryContent(card, completenessPanel, row++, GridBagConstraints.WEST, 12, 0);
        GridBagConstraints spacer = new GridBagConstraints();
        spacer.gridx = 0;
        spacer.gridy = row;
        spacer.weighty = 1;
        card.add(Box.createGlue(), spacer);
        updateCompleteness(student);
        return card;
    }

    private void addSummaryContent(CardPanel card, JComponent component, int row, int anchor,
                                   int topInset, int bottomInset) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = row;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = anchor;
        c.insets = new Insets(topInset, 0, bottomInset, 0);
        card.add(component, c);
    }

    private void addSummaryRow(JPanel rows, JComponent component, int row, boolean divider) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = row;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        if (divider) {
            component.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.TIMETABLE_CARD_BORDER),
                    BorderFactory.createEmptyBorder(8, 0, 8, 0)));
        }
        rows.add(component, c);
    }

    private JComponent createCgpaRow(double cgpa) {
        JPanel row = new JPanel();
        row.setOpaque(false);
        row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
        row.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
        JPanel labels = new JPanel(new BorderLayout(6, 0));
        labels.setOpaque(false);
        JLabel title = summaryLabel("Current CGPA");
        title.setHorizontalAlignment(SwingConstants.LEFT);
        labels.add(title, BorderLayout.WEST);
        JPanel classification = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        classification.setOpaque(false);
        classification.add(summaryValue(String.format(Locale.ROOT, "%.2f / %.2f", cgpa,
                com.facultyams.util.Constants.MAX_GPA)));
        JLabel classBadge = new JLabel(new com.facultyams.model.profile.GpaClassifier().classify(cgpa));
        classBadge.setFont(Theme.FONT_TINY.deriveFont(Font.BOLD));
        classBadge.setForeground(Theme.PRIMARY);
        classBadge.setOpaque(true);
        classBadge.setBackground(Theme.PRIMARY_LIGHT);
        classBadge.setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 6));
        classification.add(classBadge);
        labels.add(classification, BorderLayout.EAST);
        labels.setAlignmentX(Component.LEFT_ALIGNMENT);
        labels.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        JComponent progress = new ThinProgressBar((int) Math.round(cgpa / com.facultyams.util.Constants.MAX_GPA * 100));
        progress.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(labels);
        row.add(Box.createVerticalStrut(8));
        row.add(progress);
        row.setPreferredSize(new Dimension(100, 56));
        row.setMinimumSize(new Dimension(80, 56));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
        return row;
    }

    private JComponent createSummaryRow(String label, String value) {
        JPanel row = new JPanel();
        row.setOpaque(false);
        row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.TIMETABLE_CARD_BORDER),
                BorderFactory.createEmptyBorder(8, 0, 8, 0)));
        row.add(summaryLabel(label));
        row.add(Box.createVerticalStrut(3));
        row.add(summaryValue(value));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setPreferredSize(new Dimension(100, 56));
        row.setMinimumSize(new Dimension(80, 56));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
        return row;
    }

    private JComponent createAdvisorRow(String advisor) {
        JPanel wrapper = (JPanel) createSummaryRow("Academic Advisor", advisor);
        JPanel valueRow = new JPanel(new BorderLayout(4, 0));
        valueRow.setOpaque(false);
        valueRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel value = summaryValue(advisor);
        valueRow.add(value, BorderLayout.CENTER);
        JButton mail = iconButton(ProfileIcon.Shape.MAIL, "Email academic advisor");
        mail.addActionListener(e -> {
            try {
                Desktop.getDesktop().mail();
            } catch (Exception ex) {
                showToast("No email application is available", false);
            }
        });
        valueRow.add(mail, BorderLayout.EAST);
        wrapper.remove(2);
        wrapper.add(valueRow);
        return wrapper;
    }

    private JLabel summaryLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.FONT_SMALL);
        label.setForeground(Theme.TEXT_MUTED);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private JLabel summaryValue(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.PROFILE_VALUE);
        label.setForeground(Theme.TEXT_MAIN);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private JPanel createDetailsCard() {
        Student student = profileService.getProfile();
        CardPanel card = new CardPanel(12);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(Theme.PROFILE_CARD_PADDING, Theme.PROFILE_CARD_PADDING,
                Theme.PROFILE_CARD_PADDING, Theme.PROFILE_CARD_PADDING));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        addSectionHeading(card, "Contact Information", "Personal contact details and university-managed email.");
        JPanel contactGrid = new JPanel(new GridBagLayout());
        contactGrid.setOpaque(false);
        contactGrid.setAlignmentX(Component.LEFT_ALIGNMENT);
        universityEmail = field("University Email", textField(student.getEmail()), true);
        personalEmail = editableField("Personal Email", textField(valueOrEmpty(student.getPersonalEmail())));
        mobile = editableField("Mobile Number", textField(valueOrEmpty(student.getPhoneNumber())));
        relationshipBox = new JComboBox<>(new String[]{"Parent", "Guardian", "Sibling", "Spouse", "Other"});
        relationshipBox.setUI(new RoundedComboBoxUI());
        relationshipBox.setFont(Theme.FONT_BODY);
        relationshipBox.setBackground(Color.WHITE);
        relationshipBox.setPreferredSize(new Dimension(128, Theme.PROFILE_FIELD_HEIGHT));
        relationshipBox.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        relationshipBox.setToolTipText("Emergency contact relationship");
        relationshipBox.setSelectedItem(student.getEmergencyRelationship());
        relationshipDisplay = textField(valueOrEmpty(student.getEmergencyRelationship()));
        relationshipDisplay.setEditable(false);
        relationshipDisplay.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 4));
        relationshipDisplay.setOpaque(false);
        relationshipLayout = new CardLayout();
        relationshipControl = new JPanel(relationshipLayout);
        relationshipControl.setOpaque(false);
        relationshipControl.setPreferredSize(new Dimension(128, Theme.PROFILE_FIELD_HEIGHT));
        relationshipControl.add(relationshipDisplay, "view");
        relationshipControl.add(relationshipBox, "edit");
        emergencyPhoneInput = textField(valueOrEmpty(student.getEmergencyContact()));
        emergencyPhoneInput.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 4));
        emergencyPhoneInput.setOpaque(false);
        emergencyPhoneInput.setEditable(false);
        JPanel emergencyInput = new JPanel(new BorderLayout(8, 0));
        emergencyInput.setOpaque(false);
        emergencyInput.setBackground(Color.WHITE);
        emergencyInput.add(emergencyPhoneInput, BorderLayout.CENTER);
        emergencyInput.add(relationshipControl, BorderLayout.EAST);
        emergencyNumber = editableField("Emergency Contact", emergencyInput);
        address = editableField("Residential Address", textField(valueOrEmpty(student.getAddress())));
        addField(contactGrid, universityEmail, 0, 0, 1);
        addField(contactGrid, personalEmail, 1, 0, 1);
        addField(contactGrid, mobile, 0, 1, 1);
        addField(contactGrid, emergencyNumber, 1, 1, 1);
        addField(contactGrid, address, 0, 2, 2);
        card.add(contactGrid);
        card.add(Box.createVerticalStrut(Theme.PROFILE_SECTION_GAP));
        addSectionHeading(card, "Academic Information", "Official academic records are managed by the university.");
        JPanel academicGrid = new JPanel(new GridBagLayout());
        academicGrid.setOpaque(false);
        academicGrid.setAlignmentX(Component.LEFT_ALIGNMENT);
        addField(academicGrid, field("Degree Program", textField(student.getDegreeProgram()), true), 0, 0, 1);
        addField(academicGrid, field("Academic Year & Level",
                textField(valueOrEmpty(student.getAcademicYear()) + " - " + valueOrEmpty(student.getCurrentSemester())), true), 1, 0, 1);
        addField(academicGrid, field("Faculty / Department", textField(student.getDepartment()), true), 0, 1, 1);
        addField(academicGrid, field("Registration Number", textField(student.getRegNo()), true), 1, 1, 1);
        card.add(academicGrid);
        lastUpdated.setFont(Theme.FONT_SMALL);
        lastUpdated.setForeground(Theme.TEXT_MUTED);
        lastUpdated.setAlignmentX(Component.LEFT_ALIGNMENT);
        lastUpdated.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        card.add(lastUpdated);

        attachValidation();
        configurePhoneField((JTextField) mobile.getInput());
        configurePhoneField(emergencyPhoneInput);
        emergencyPhoneInput.getDocument().addDocumentListener(documentListener());
        emergencyPhoneInput.addFocusListener(new FocusAdapter() {
            @Override public void focusLost(FocusEvent e) {
                if (!editing || validating) return;
                normalizePhone(emergencyPhoneInput);
                validateField(emergencyNumber);
                updateDirty();
            }
        });
        relationshipLayout.show(relationshipControl, "view");
        installEditKeyBehavior();
        return card;
    }

    private void addSectionHeading(JPanel card, String titleText, String helperText) {
        JLabel title = new JLabel(titleText);
        title.setFont(Theme.PROFILE_SECTION);
        title.setForeground(Theme.TEXT_MAIN);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel helper = new JLabel(helperText);
        helper.setFont(Theme.FONT_SMALL);
        helper.setForeground(Theme.TEXT_MUTED);
        helper.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        heading.add(title);
        heading.add(Box.createVerticalStrut(4));
        heading.add(helper);
        card.add(heading);
        card.add(Box.createVerticalStrut(16));
        card.add(createDivider());
        card.add(Box.createVerticalStrut(16));
    }

    private void addField(JPanel grid, FormField field, int x, int y, int width) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = x;
        c.gridy = y;
        c.gridwidth = width;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = new Insets(0, x == 0 ? 0 : Theme.PROFILE_COLUMN_GAP / 2,
                Theme.PROFILE_FIELD_ROW_GAP, width == 2 ? 0 : Theme.PROFILE_COLUMN_GAP / 2);
        grid.add(field, c);
    }

    private FormField editableField(String label, JComponent input) {
        FormField formField = new FormField(label, input, false);
        editableFields.add(formField);
        return formField;
    }

    private FormField field(String label, JTextField input, boolean readOnly) {
        return new FormField(label, input, readOnly);
    }

    private JTextField textField(String value) {
        JTextField field = new JTextField(value);
        field.setFont(Theme.FONT_BODY);
        field.setForeground(Theme.TEXT_MAIN);
        field.setCaretColor(Theme.PRIMARY);
        return field;
    }

    private void attachValidation() {
        for (FormField field : editableFields) {
            Component input = field.getInput();
            input.addFocusListener(new FocusAdapter() {
                @Override public void focusLost(FocusEvent e) {
                    if (!editing || validating) return;
                    normalizePhoneInputs();
                    validateField(field);
                    updateDirty();
                }
            });
            if (input instanceof JTextField text) {
                text.getDocument().addDocumentListener(new DocumentListener() {
                    @Override public void insertUpdate(DocumentEvent e) { updateDirty(); }
                    @Override public void removeUpdate(DocumentEvent e) { updateDirty(); }
                    @Override public void changedUpdate(DocumentEvent e) { updateDirty(); }
                });
            }
        }
        relationshipBox.addActionListener(e -> updateDirty());
    }

    private DocumentListener documentListener() {
        return new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { updateDirty(); }
            @Override public void removeUpdate(DocumentEvent e) { updateDirty(); }
            @Override public void changedUpdate(DocumentEvent e) { updateDirty(); }
        };
    }

    private void configurePhoneField(JTextField field) {
        phoneInputs.add(field);
        ((AbstractDocument) field.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override public void insertString(FilterBypass fb, int offset, String text, AttributeSet attrs)
                    throws BadLocationException {
                replace(fb, offset, 0, text, attrs);
            }
            @Override public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                    throws BadLocationException {
                String old = fb.getDocument().getText(0, fb.getDocument().getLength());
                String inserted = text == null ? "" : text.replaceAll("[^0-9+ ]", "");
                String next = old.substring(0, offset) + inserted + old.substring(offset + length);
                if (next.length() <= 18) fb.replace(0, fb.getDocument().getLength(), next, attrs);
            }
        });
        field.addFocusListener(new FocusAdapter() {
            @Override public void focusLost(FocusEvent e) { normalizePhone(field); }
        });
    }

    private void normalizePhoneInputs() {
        phoneInputs.forEach(this::normalizePhone);
    }

    private void normalizePhone(JTextField field) {
        String digits = field.getText().replaceAll("\\D", "");
        if (digits.startsWith("94") && digits.length() == 11) {
            field.setText("+94 " + digits.substring(2, 4) + " " + digits.substring(4, 7) + " " + digits.substring(7));
        } else if (digits.length() == 10 && digits.startsWith("0")) {
            field.setText(digits.substring(0, 3) + " " + digits.substring(3, 6) + " " + digits.substring(6));
        }
    }

    private void validateField(FormField field) {
        String message = null;
        String value = inputText(field).trim();
        if (field == personalEmail) {
            message = new PersonalEmailValidator(profileService.getProfile().getEmail()).validate(value);
        } else if (field == mobile || field == emergencyNumber) {
            message = phoneValidator.validate(value);
        } else if (field == address) {
            message = addressValidator.validate(value);
        }
        field.setError(message);
    }

    private boolean validateAll() {
        validating = true;
        normalizePhoneInputs();
        boolean valid = true;
        for (FormField field : editableFields) {
            validateField(field);
            if (field.hasError()) valid = false;
        }
        validating = false;
        return valid;
    }

    private void rebuildHeaderActions() {
        headerActions.removeAll();
        if (editing) {
            JLabel state = new JLabel("Editing");
            state.setFont(Theme.FONT_BODY_BOLD);
            state.setForeground(Theme.PRIMARY);
            state.setOpaque(true);
            state.setBackground(Theme.PRIMARY_LIGHT);
            state.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
            headerActions.add(state);
        } else {
            JButton edit = new FlatButton("Edit Profile", false);
            edit.setIcon(new ProfileIcon(ProfileIcon.Shape.EDIT, Theme.PRIMARY));
            edit.setMnemonic(KeyEvent.VK_E);
            edit.addActionListener(e -> enterEditMode());
            JButton password = new FlatButton("Change Password", false);
            password.setIcon(new ProfileIcon(ProfileIcon.Shape.LOCK, Theme.PRIMARY));
            password.setMnemonic(KeyEvent.VK_P);
            password.addActionListener(e -> showPasswordDialog());
            headerActions.add(edit);
            headerActions.add(password);
        }
        headerActions.revalidate();
        headerActions.repaint();
    }

    private void enterEditMode() {
        refreshFieldsFromModel();
        editing = true;
        dirty = false;
        for (FormField field : editableFields) field.setEditable(true);
        emergencyPhoneInput.setEditable(true);
        relationshipLayout.show(relationshipControl, "edit");
        photoButton.setVisible(true);
        actionBar.removeAll();
        JButton cancel = new FlatButton("Cancel", false);
        cancel.addActionListener(e -> cancelEdit());
        JButton save = new FlatButton("Save Changes", true);
        save.setEnabled(false);
        save.addActionListener(e -> saveChanges(save));
        actionBar.add(cancel);
        saveProgress.setIndeterminate(true);
        saveProgress.setPreferredSize(new Dimension(20, 20));
        saveProgress.setBorderPainted(false);
        saveProgress.setVisible(false);
        actionBar.add(saveProgress);
        actionBar.add(save);
        actionBar.setVisible(true);
        rebuildHeaderActions();
        revalidate();
        animateTransition();
        SwingUtilities.invokeLater(() -> ((JTextField) personalEmail.getInput()).requestFocusInWindow());
    }

    private void cancelEdit() {
        if (dirty && JOptionPane.showConfirmDialog(this, "Discard changes?", "Unsaved changes",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) return;
        finishCancel();
    }

    private void finishCancel() {
        deletePendingPhoto();
        editing = false;
        dirty = false;
        refreshFieldsFromModel();
        for (FormField field : editableFields) field.setEditable(false);
        emergencyPhoneInput.setEditable(false);
        relationshipLayout.show(relationshipControl, "view");
        photoButton.setVisible(false);
        actionBar.setVisible(false);
        rebuildHeaderActions();
        revalidate();
        repaint();
        animateTransition();
    }

    private void saveChanges(JButton saveButton) {
        if (!validateAll()) {
            showToast("Please correct the highlighted fields.", false);
            return;
        }
        setSaving(true, saveButton);
        String email = ((JTextField) personalEmail.getInput()).getText().trim();
        String phone = ((JTextField) mobile.getInput()).getText().trim();
        String emergency = inputText(emergencyNumber).trim();
        String addressValue = ((JTextField) address.getInput()).getText().trim();
        String relation = String.valueOf(relationshipBox.getSelectedItem());
        String photo = selectedPhotoPath;
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override protected Void doInBackground() {
                profileService.updateContactInfo(email, phone, addressValue, emergency, relation);
                if (photo != null) profileService.updatePhoto(photo);
                return null;
            }

            @Override protected void done() {
                try {
                    get();
                    lastUpdated.setText("Last updated: " + LocalDateTime.now().format(UPDATED_FORMAT));
                    editing = false;
                    dirty = false;
                    selectedPhotoPath = null;
                    for (FormField field : editableFields) field.setEditable(false);
                    emergencyPhoneInput.setEditable(false);
                    relationshipDisplay.setText(String.valueOf(relationshipBox.getSelectedItem()));
                    relationshipLayout.show(relationshipControl, "view");
                    photoButton.setVisible(false);
                    actionBar.setVisible(false);
                    rebuildHeaderActions();
                    updateCompleteness(profileService.getProfile());
                    showToast("Profile updated successfully", true);
                    revalidate();
                    repaint();
                    animateTransition();
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    showToast("Could not save profile: " + cause.getMessage(), false);
                } finally {
                    setSaving(false, saveButton);
                }
            }
        };
        worker.execute();
    }

    private void setSaving(boolean value, JButton save) {
        saving = value;
        saveProgress.setVisible(value);
        save.setText(value ? "Saving..." : "Save Changes");
        for (Component component : actionBar.getComponents()) {
            if (component instanceof JButton button) {
                button.setEnabled(!value && (button == save ? dirty : true));
            }
        }
    }

    private void updateDirty() {
        if (!editing || saving) return;
        Student student = profileService.getProfile();
        dirty = !text(personalEmail).equals(originalEmail)
                || !text(mobile).equals(originalPhone)
                || !text(emergencyNumber).equals(originalEmergency)
                || !String.valueOf(relationshipBox.getSelectedItem()).equals(originalRelationship)
                || !text(address).equals(originalAddress)
                || (selectedPhotoPath != null);
        for (Component component : actionBar.getComponents()) {
            if (component instanceof JButton button && button.getText().startsWith("Save")) button.setEnabled(dirty);
        }
    }

    private String text(FormField field) {
        return inputText(field).trim();
    }

    private String inputText(FormField field) {
        return field == emergencyNumber ? emergencyPhoneInput.getText() : ((JTextField) field.getInput()).getText();
    }

    private void refreshFieldsFromModel() {
        Student student = profileService.getProfile();
        ((JTextField) universityEmail.getInput()).setText(student.getEmail());
        ((JTextField) personalEmail.getInput()).setText(valueOrEmpty(student.getPersonalEmail()));
        ((JTextField) mobile.getInput()).setText(valueOrEmpty(student.getPhoneNumber()));
        emergencyPhoneInput.setText(valueOrEmpty(student.getEmergencyContact()).replaceAll("\\s*\\([^)]*\\)$", ""));
        ((JTextField) address.getInput()).setText(valueOrEmpty(student.getAddress()));
        if (relationshipBox != null) relationshipBox.setSelectedItem(student.getEmergencyRelationship());
        if (relationshipDisplay != null) relationshipDisplay.setText(valueOrEmpty(student.getEmergencyRelationship()));
        originalEmail = valueOrEmpty(student.getPersonalEmail());
        originalPhone = valueOrEmpty(student.getPhoneNumber());
        originalEmergency = valueOrEmpty(student.getEmergencyContact()).replaceAll("\\s*\\([^)]*\\)$", "");
        originalRelationship = valueOrEmpty(student.getEmergencyRelationship());
        originalAddress = valueOrEmpty(student.getAddress());
        selectedPhotoPath = null;
        if (avatar != null) avatar.setImagePath(student.getProfileImageUrl());
    }

    private void choosePhoto() {
        try {
            BufferedImage image = ProfilePhotoCropDialog.chooseAndCrop(this);
            if (image == null) return;
            Path directory = Path.of(System.getProperty("user.home"), ".srms");
            Files.createDirectories(directory);
            deletePendingPhoto();
            Path destination = directory.resolve("student-profile-edit-" + java.util.UUID.randomUUID() + ".png");
            ImageIO.write(image, "png", destination.toFile());
            selectedPhotoPath = destination.toAbsolutePath().toString();
            avatar.setImagePath(selectedPhotoPath);
            updateDirty();
        } catch (IOException ex) {
            showToast("Could not use that photo: " + ex.getMessage(), false);
        }
    }

    private void removePhoto() {
        if (!editing) return;
        deletePendingPhoto();
        selectedPhotoPath = "";
        avatar.setImagePath(null);
        updateDirty();
    }

    private void showPasswordDialog() {
        Window owner = SwingUtilities.getWindowAncestor(this);
        ChangePasswordDialog.show(owner, profileService, () -> showToast("Password updated successfully", true));
    }

    private void installEditKeyBehavior() {
        setFocusable(true);
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel-edit");
        getActionMap().put("cancel-edit", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                if (editing) cancelEdit();
            }
        });
        List<JTextField> focusOrder = new ArrayList<>();
        focusOrder.add((JTextField) personalEmail.getInput());
        focusOrder.add((JTextField) mobile.getInput());
        focusOrder.add(emergencyPhoneInput);
        focusOrder.add(address.getInput() instanceof JTextField text ? text : null);
        focusOrder.removeIf(java.util.Objects::isNull);
        for (JTextField text : focusOrder) {
                text.addKeyListener(new KeyAdapter() {
                    @Override public void keyPressed(KeyEvent e) {
                        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                            e.consume();
                            text.transferFocus();
                        }
                    }
                });
        }
    }

    private void showToast(String message, boolean success) {
        Window owner = SwingUtilities.getWindowAncestor(this);
        if (owner == null || !owner.isShowing()) return;
        JWindow toast = new JWindow(owner);
        JLabel label = new JLabel(message);
        label.setFont(Theme.FONT_BODY_BOLD);
        label.setForeground(Color.WHITE);
        label.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        JPanel surface = new JPanel(new BorderLayout());
        surface.setBackground(success ? Theme.PROFILE_SUCCESS : Theme.PROFILE_ERROR);
        surface.add(label, BorderLayout.CENTER);
        toast.setBackground(new Color(0, 0, 0, 0));
        toast.add(surface);
        toast.pack();
        toast.setLocation(owner.getX() + owner.getWidth() - toast.getWidth() - 32,
                owner.getY() + owner.getHeight() - toast.getHeight() - 48);
        toast.setVisible(true);
        Timer timer = new Timer(2200, e -> toast.dispose());
        timer.setRepeats(false);
        timer.start();
    }

    private void updateCompleteness(Student student) {
        int complete = 0;
        int total = 6;
        if (student.getPersonalEmail() != null && !student.getPersonalEmail().isBlank()) complete++;
        if (student.getPhoneNumber() != null && !student.getPhoneNumber().isBlank()) complete++;
        if (student.getEmergencyContact() != null && !student.getEmergencyContact().isBlank()) complete++;
        if (student.getAddress() != null && !student.getAddress().isBlank()) complete++;
        if (student.getAcademicAdvisor() != null && !student.getAcademicAdvisor().isBlank()) complete++;
        if (student.getProfileImageUrl() != null && !student.getProfileImageUrl().isBlank()) complete++;
        int percent = (int) Math.round(complete * 100.0 / total);
        completeness.setText("Profile " + percent + "% complete");
        completenessBar.setValue(percent);
    }

    private int completionPercent(Student student) {
        int complete = 0;
        if (student.getPersonalEmail() != null && !student.getPersonalEmail().isBlank()) complete++;
        if (student.getPhoneNumber() != null && !student.getPhoneNumber().isBlank()) complete++;
        if (student.getEmergencyContact() != null && !student.getEmergencyContact().isBlank()) complete++;
        if (student.getAddress() != null && !student.getAddress().isBlank()) complete++;
        if (student.getAcademicAdvisor() != null && !student.getAcademicAdvisor().isBlank()) complete++;
        if (student.getProfileImageUrl() != null && !student.getProfileImageUrl().isBlank()) complete++;
        return complete * 100 / 6;
    }

    private JButton iconButton(ProfileIcon.Shape shape, String tooltip) {
        JButton button = new JButton(new ProfileIcon(shape, Theme.PRIMARY, 14));
        button.setToolTipText(tooltip);
        button.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private JComponent createDivider() {
        JPanel line = new JPanel() {
            @Override protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                Graphics2D g = (Graphics2D) graphics.create();
                Theme.setAntiAliasing(g);
                g.setColor(Theme.TIMETABLE_CARD_BORDER);
                g.fillRect(0, 0, getWidth(), 1);
                g.dispose();
            }
        };
        line.setOpaque(false);
        line.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        line.setMinimumSize(new Dimension(1, 1));
        line.setPreferredSize(new Dimension(100, 1));
        line.setAlignmentX(Component.LEFT_ALIGNMENT);
        return line;
    }

    private String initials(String name) {
        String[] parts = name.trim().split("\\s+");
        return parts.length == 1 ? parts[0].substring(0, 1).toUpperCase()
                : (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    /** Returns false when a dirty edit should keep the user on this screen. */
    public boolean confirmNavigation() {
        if (!editing) return true;
        if (!dirty) {
            finishCancel();
            return true;
        }
        if (saving || JOptionPane.showConfirmDialog(this, "Discard changes?", "Unsaved changes",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) return false;
        finishCancel();
        return true;
    }

    private void deletePendingPhoto() {
        if (selectedPhotoPath == null || selectedPhotoPath.isBlank()) return;
        try {
            Files.deleteIfExists(Path.of(selectedPhotoPath));
        } catch (IOException ex) {
            showToast("Could not remove temporary profile photo: " + ex.getMessage(), false);
        }
    }

    private void animateTransition() {
        if (transitionTimer != null) transitionTimer.stop();
        long started = System.nanoTime();
        transitionAlpha = 0.72f;
        transitionTimer = new Timer(15, event -> {
            float progress = Math.min(1f, (System.nanoTime() - started) / 150_000_000f);
            transitionAlpha = 0.72f + 0.28f * progress;
            repaint();
            if (progress >= 1f) ((Timer) event.getSource()).stop();
        });
        transitionTimer.start();
    }

    @Override protected void paintChildren(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setComposite(AlphaComposite.SrcOver.derive(transitionAlpha));
        super.paintChildren(g);
        g.dispose();
    }

    private final class ProfileContent extends JPanel implements Scrollable {
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) { return 24; }
        @Override public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return Math.max(24, visibleRect.height - 24);
        }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }

    private static final class ThinProgressBar extends JComponent {
        private final int value;

        private ThinProgressBar(int value) {
            this.value = Math.max(0, Math.min(100, value));
            setOpaque(false);
            setPreferredSize(new Dimension(100, 8));
            setMinimumSize(new Dimension(40, 8));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 8));
        }

        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            Theme.setAntiAliasing(g);
            int height = Math.min(8, getHeight());
            int arc = height;
            g.setColor(Theme.PRIMARY_LIGHT);
            g.fillRoundRect(0, (getHeight() - height) / 2, getWidth(), height, arc, arc);
            int filledWidth = (int) Math.round(getWidth() * value / 100.0);
            if (filledWidth > 0) {
                g.setColor(Theme.PRIMARY);
                g.fillRoundRect(0, (getHeight() - height) / 2, filledWidth, height, arc, arc);
            }
            g.dispose();
        }
    }
}
