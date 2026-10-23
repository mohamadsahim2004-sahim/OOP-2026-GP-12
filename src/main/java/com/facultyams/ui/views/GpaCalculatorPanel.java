package com.facultyams.ui.views;

import com.facultyams.model.profile.GpaClassifier;
import com.facultyams.model.results.LetterGrade;
import com.facultyams.service.GpaCalculator;
import com.facultyams.service.ResultService;
import com.facultyams.service.ResultService.Summary;
import com.facultyams.ui.theme.Theme;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Arc2D;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/** Styled GPA calculator panel used by the Results screen. */
public final class GpaCalculatorPanel extends JPanel {
    private enum Mode { SEMESTER, PROJECTED, TARGET }

    private final GpaCalculator calculator = new GpaCalculator();
    private final Summary profile = ResultService.getInstance().getSummary();
    private final JPanel courseRows = new JPanel();
    private final JPanel modeFields = new JPanel(new GridBagLayout());
    private final JLabel validationMessage = new JLabel(" ");
    private final JLabel currentCgpaError = errorLabel();
    private final JLabel completedCreditsError = errorLabel();
    private final JLabel targetCgpaError = errorLabel();
    private final JLabel remainingCreditsError = errorLabel();
    private final StyledTextField currentCgpaField = new StyledTextField();
    private final StyledTextField completedCreditsField = new StyledTextField();
    private final StyledTextField targetCgpaField = new StyledTextField();
    private final StyledTextField remainingCreditsField = new StyledTextField();
    private final JLabel resultValue = new JLabel("--", SwingConstants.CENTER);
    private final JLabel classification = new JLabel("Add course details to calculate", SwingConstants.CENTER);
    private final JLabel resultHeading = new JLabel("Projected Result", SwingConstants.CENTER);
    private final JLabel semesterGpaValue = new JLabel("--");
    private final JLabel changeValue = new JLabel("--");
    private final JLabel totalCreditsValue = new JLabel("--");
    private final JLabel qualityPointsValue = new JLabel("--");
    private final GpaArc gpaArc = new GpaArc();
    private final List<CourseRow> rows = new ArrayList<>();
    private Mode mode = Mode.PROJECTED;
    private boolean showValidation;
    private boolean defaultsInitialized;

    /** Builds a responsive, live-updating GPA calculator. */
    public GpaCalculatorPanel() {
        setOpaque(false);
        setLayout(new GridBagLayout());
        GridBagConstraints col = new GridBagConstraints();
        col.gridy = 0;
        col.weighty = 1;
        col.fill = GridBagConstraints.BOTH;
        col.insets = new Insets(0, 0, 0, 16);
        col.gridx = 0;
        col.weightx = 0.70;
        add(createInputCard(), col);
        col.gridx = 1;
        col.weightx = 0.30;
        col.insets = new Insets(0, 0, 0, 0);
        add(createResultCard(), col);
        addCourseRow();
        installLiveUpdate(currentCgpaField);
        installLiveUpdate(completedCreditsField);
        installLiveUpdate(targetCgpaField);
        installLiveUpdate(remainingCreditsField);
        for (StyledTextField field : List.of(currentCgpaField, completedCreditsField, targetCgpaField, remainingCreditsField)) {
            field.addFocusListener(new FocusAdapter() {
                @Override public void focusLost(FocusEvent e) {
                    showValidation = true;
                    recalculate();
                }
            });
        }
        refreshModeFields();
        recalculate();
    }

    private JComponent createInputCard() {
        RoundedPanel card = new RoundedPanel();
        card.setLayout(new GridBagLayout());
        card.setBorder(new EmptyBorder(24, 24, 20, 24));
        GridBagConstraints c = constraints();
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 4;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        JLabel title = new JLabel("GPA Calculator");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(Theme.TEXT_MAIN);
        card.add(title, c);

        c.gridy++;
        c.insets = new Insets(4, 0, 16, 0);
        JLabel description = new JLabel("Estimate your semester GPA and see how it affects your cumulative result.");
        description.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        description.setForeground(Theme.TEXT_MUTED);
        card.add(description, c);

        c.gridy++;
        c.insets = new Insets(0, 0, 20, 0);
        JPanel modeTabs = new JPanel(new GridLayout(1, 3, 8, 0));
        modeTabs.setOpaque(false);
        addModeTab(modeTabs, "Semester GPA", Mode.SEMESTER);
        addModeTab(modeTabs, "Projected CGPA", Mode.PROJECTED);
        addModeTab(modeTabs, "Target Planner", Mode.TARGET);
        card.add(modeTabs, c);

        c.gridy++;
        c.insets = new Insets(0, 0, 8, 0);
        card.add(createCourseHeader(), c);

        c.gridy++;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(0, 0, 8, 0);
        courseRows.setOpaque(false);
        courseRows.setLayout(new BoxLayout(courseRows, BoxLayout.Y_AXIS));
        JScrollPane rowsScroll = new JScrollPane(courseRows);
        rowsScroll.setBorder(BorderFactory.createEmptyBorder());
        rowsScroll.setOpaque(false);
        rowsScroll.getViewport().setOpaque(false);
        rowsScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        rowsScroll.getVerticalScrollBar().setUnitIncrement(24);
        rowsScroll.setPreferredSize(new Dimension(600, 230));
        card.add(rowsScroll, c);

        c.gridy++;
        c.weighty = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 12, 0);
        DashedAddButton addButton = new DashedAddButton();
        addButton.addActionListener(e -> addCourseRow());
        card.add(addButton, c);

        c.gridy++;
        c.insets = new Insets(0, 0, 12, 0);
        validationMessage.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        validationMessage.setForeground(Theme.PROFILE_ERROR);
        card.add(validationMessage, c);

        c.gridy++;
        c.insets = new Insets(0, 0, 12, 0);
        JSeparator divider = new JSeparator();
        divider.setForeground(Theme.TIMETABLE_CARD_BORDER);
        card.add(divider, c);

        c.gridy++;
        c.insets = new Insets(0, 0, 8, 0);
        modeFields.setOpaque(false);
        card.add(modeFields, c);

        c.gridy++;
        c.anchor = GridBagConstraints.EAST;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(8, 0, 0, 0);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        SecondaryButton reset = new SecondaryButton("Reset");
        reset.addActionListener(e -> resetCalculator());
        PrimaryButton calculateButton = new PrimaryButton("Calculate");
        calculateButton.addActionListener(e -> {
            showValidation = true;
            recalculate();
        });
        actions.add(reset);
        actions.add(calculateButton);
        card.add(actions, c);
        return card;
    }

    private JComponent createCourseHeader() {
        JPanel header = new JPanel(new GridBagLayout());
        header.setOpaque(false);
        addGridCell(header, label("Course Name"), 0, 0, 1, 1, 1, GridBagConstraints.WEST,
                new Insets(0, 0, 0, 8));
        JLabel creditsLabel = label("Credits");
        creditsLabel.setPreferredSize(new Dimension(90, 20));
        creditsLabel.setHorizontalAlignment(SwingConstants.CENTER);
        addGridCell(header, creditsLabel, 1, 0, 1, 1, 0, GridBagConstraints.CENTER,
                new Insets(0, 0, 0, 8));
        JLabel gradeLabel = label("Grade");
        gradeLabel.setPreferredSize(new Dimension(110, 20));
        gradeLabel.setHorizontalAlignment(SwingConstants.CENTER);
        addGridCell(header, gradeLabel, 2, 0, 1, 1, 0, GridBagConstraints.CENTER,
                new Insets(0, 0, 0, 8));
        JLabel actionLabel = label("");
        actionLabel.setPreferredSize(new Dimension(40, 20));
        addGridCell(header, actionLabel, 3, 0, 1, 1, 0, GridBagConstraints.CENTER, new Insets(0, 0, 0, 0));
        return header;
    }

    private JComponent createResultCard() {
        RoundedPanel card = new RoundedPanel();
        card.setLayout(new GridBagLayout());
        card.setBorder(new EmptyBorder(24, 20, 24, 20));
        GridBagConstraints c = constraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        resultHeading.setFont(new Font("Segoe UI", Font.BOLD, 18));
        resultHeading.setForeground(Theme.TEXT_MAIN);
        card.add(resultHeading, c);

        c.gridy++;
        c.weighty = 0;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(20, 0, 4, 0);
        card.add(gpaArc, c);

        c.gridy++;
        c.insets = new Insets(4, 0, 0, 0);
        resultValue.setFont(new Font("Segoe UI", Font.BOLD, 48));
        resultValue.setForeground(Theme.PRIMARY);
        card.add(resultValue, c);

        c.gridy++;
        c.insets = new Insets(8, 0, 24, 0);
        classification.setFont(new Font("Segoe UI", Font.BOLD, 13));
        classification.setForeground(Theme.PRIMARY);
        classification.setOpaque(true);
        classification.setBackground(Theme.PRIMARY_LIGHT);
        classification.setBorder(new EmptyBorder(7, 12, 7, 12));
        card.add(classification, c);

        c.gridy++;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 0, 0);
        JPanel summary = new JPanel(new GridBagLayout());
        summary.setOpaque(false);
        addResultLine(summary, "Semester GPA", semesterGpaValue, 0);
        addResultLine(summary, "Change from current", changeValue, 1);
        addResultLine(summary, "Total credits after semester", totalCreditsValue, 2);
        addResultLine(summary, "Quality points this semester", qualityPointsValue, 3);
        card.add(summary, c);

        c.gridy++;
        c.weighty = 1;
        card.add(Box.createGlue(), c);
        return card;
    }

    private void addResultLine(JPanel panel, String text, JLabel value, int row) {
        GridBagConstraints left = constraints();
        left.gridx = 0;
        left.gridy = row * 2;
        left.weightx = 1;
        left.fill = GridBagConstraints.HORIZONTAL;
        left.insets = new Insets(10, 0, 10, 0);
        JLabel name = new JLabel(text);
        name.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        name.setForeground(Theme.TEXT_MUTED);
        panel.add(name, left);
        GridBagConstraints right = (GridBagConstraints) left.clone();
        right.gridx = 1;
        right.weightx = 0;
        right.anchor = GridBagConstraints.EAST;
        value.setFont(new Font("Segoe UI", Font.BOLD, 13));
        value.setForeground(Theme.TEXT_MAIN);
        panel.add(value, right);
        JPanel divider = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.setAntiAliasing(g2);
                g2.setColor(Theme.TIMETABLE_CARD_BORDER);
                g2.drawLine(0, 0, getWidth(), 0);
                g2.dispose();
            }
        };
        divider.setOpaque(false);
        GridBagConstraints line = constraints();
        line.gridx = 0;
        line.gridy = row * 2 + 1;
        line.gridwidth = 2;
        line.fill = GridBagConstraints.HORIZONTAL;
        line.weightx = 1;
        line.insets = new Insets(0, 0, 0, 0);
        if (row < 3) panel.add(divider, line);
    }

    private void addModeTab(JPanel parent, String text, Mode targetMode) {
        TabButton button = new TabButton(text, targetMode == mode);
        button.addActionListener(e -> {
            mode = targetMode;
            for (Component component : parent.getComponents()) {
                if (component instanceof TabButton tab) tab.setActive(tab == button);
            }
            refreshModeFields();
            updateResultHeading();
            recalculate();
        });
        parent.add(button);
    }

    private void refreshModeFields() {
        modeFields.removeAll();
        modeFields.setLayout(new GridBagLayout());
        if (mode == Mode.PROJECTED) {
            addLabeledInput(modeFields, "Current CGPA", currentCgpaField, currentCgpaError, 0);
            addLabeledInput(modeFields, "Credits Completed", completedCreditsField, completedCreditsError, 1);
        } else if (mode == Mode.TARGET) {
            addLabeledInput(modeFields, "Current CGPA", currentCgpaField, currentCgpaError, 0);
            addLabeledInput(modeFields, "Credits Completed", completedCreditsField, completedCreditsError, 1);
            addLabeledInput(modeFields, "Target CGPA", targetCgpaField, targetCgpaError, 2);
            addLabeledInput(modeFields, "Remaining Credits", remainingCreditsField, remainingCreditsError, 3);
        }
        if (!defaultsInitialized) {
            currentCgpaField.setText(profile.cgpa().toPlainString());
            completedCreditsField.setText(Integer.toString(profile.earnedCredits()));
            targetCgpaField.setText("3.70");
            remainingCreditsField.setText(Integer.toString(Math.max(0, profile.plannedCredits() - profile.earnedCredits())));
            defaultsInitialized = true;
        }
        modeFields.revalidate();
        modeFields.repaint();
    }

    private void addLabeledInput(JPanel parent, String text, StyledTextField field, JLabel error, int column) {
        JPanel group = new JPanel(new GridBagLayout());
        group.setOpaque(false);
        GridBagConstraints c = constraints();
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        JLabel title = label(text);
        group.add(title, c);
        c.gridy++;
        c.insets = new Insets(6, 0, 0, 0);
        group.add(field, c);
        c.gridy++;
        c.insets = new Insets(2, 0, 0, 0);
        group.add(error, c);
        int x = column % 2;
        int y = column / 2;
        addGridCell(parent, group, x, y, 1, 1, 1, GridBagConstraints.WEST,
                new Insets(y == 0 ? 0 : 12, x == 0 ? 0 : 12, 0, x == 0 ? 12 : 0));
    }

    private void installLiveUpdate(JTextField field) {
        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { recalculate(); }
            @Override public void removeUpdate(DocumentEvent e) { recalculate(); }
            @Override public void changedUpdate(DocumentEvent e) { recalculate(); }
        });
    }

    private void addCourseRow() {
        CourseRow row = new CourseRow();
        rows.add(row);
        courseRows.add(row);
        courseRows.add(Box.createVerticalStrut(8));
        courseRows.revalidate();
        courseRows.repaint();
        recalculate();
    }

    private void removeCourseRow(CourseRow row) {
        rows.remove(row);
        courseRows.remove(row);
        for (int i = courseRows.getComponentCount() - 1; i >= 0; i--) {
            if (courseRows.getComponent(i) instanceof Box.Filler) {
                courseRows.remove(i);
                break;
            }
        }
        courseRows.revalidate();
        courseRows.repaint();
        recalculate();
    }

    private void recalculate() {
        try {
            BigDecimal current = parseGpa(currentCgpaField.getText(), profile.cgpa());
            int completedCredits = parseInteger(completedCreditsField.getText(), profile.earnedCredits());
            List<GpaCalculator.CourseInput> inputs = new ArrayList<>();
            boolean courseFieldsValid = true;
            for (CourseRow row : rows) {
                String name = row.nameField.getText().trim();
                row.nameError.setText(showValidation && name.isEmpty() ? "Enter a course name." : " ");
                row.nameField.setInvalid(showValidation && name.isEmpty());
                if (name.isEmpty()) {
                    courseFieldsValid = false;
                } else {
                    inputs.add(new GpaCalculator.CourseInput(name,
                            (Integer) row.credits.getValue(), (LetterGrade) row.grade.getSelectedItem()));
                }
            }
            BigDecimal semesterGpa = inputs.isEmpty() ? BigDecimal.ZERO : calculator.semesterGpa(inputs);
            GpaCalculator.Calculation calculation = inputs.isEmpty() ? null
                    : calculator.calculate(inputs, current, completedCredits);
            if (mode == Mode.TARGET) {
                updateTargetResult(current, completedCredits, calculation);
            } else if (calculation != null && courseFieldsValid) {
                showCalculation(calculation, mode);
                validationMessage.setText(" ");
            } else {
                showEmptyResult();
                if (showValidation && !courseFieldsValid) {
                    validationMessage.setText("Complete each course name to calculate the result.");
                } else {
                    validationMessage.setText(" ");
                }
            }
            refreshInputErrors();
        } catch (IllegalArgumentException ex) {
            showEmptyResult();
            validationMessage.setText(showValidation ? ex.getMessage() : " ");
            refreshInputErrors();
        }
    }

    private void refreshInputErrors() {
        boolean currentInvalid = showValidation && mode != Mode.SEMESTER && !validGpa(currentCgpaField.getText());
        boolean creditsInvalid = showValidation && mode != Mode.SEMESTER
                && !validNonNegativeInteger(completedCreditsField.getText());
        boolean targetInvalid = showValidation && mode == Mode.TARGET && !validGpa(targetCgpaField.getText());
        boolean remainingInvalid = showValidation && mode == Mode.TARGET
                && (!validNonNegativeInteger(remainingCreditsField.getText())
                || safeParseInteger(remainingCreditsField.getText()) <= 0);
        currentCgpaError.setText(currentInvalid ? "Enter a number from 0.00 to 4.00." : " ");
        completedCreditsError.setText(creditsInvalid ? "Enter zero or more credits." : " ");
        targetCgpaError.setText(targetInvalid ? "Enter a number from 0.00 to 4.00." : " ");
        remainingCreditsError.setText(remainingInvalid ? "Enter a positive credit total." : " ");
        currentCgpaField.setInvalid(currentInvalid);
        completedCreditsField.setInvalid(creditsInvalid);
        targetCgpaField.setInvalid(targetInvalid);
        remainingCreditsField.setInvalid(remainingInvalid);
    }

    private int safeParseInteger(String value) {
        try { return Integer.parseInt(value.trim()); }
        catch (NumberFormatException ex) { return 0; }
    }

    private void showCalculation(GpaCalculator.Calculation calculation, Mode calculationMode) {
        BigDecimal value = calculationMode == Mode.SEMESTER
                ? calculation.semesterGpa() : calculation.projectedCgpa();
        resultValue.setText(String.format(Locale.ROOT, "%.2f", value));
        gpaArc.setGpa(value.doubleValue());
        classification.setText(new GpaClassifier().classify(value.doubleValue()));
        classification.setForeground(Theme.PRIMARY);
        classification.setBackground(Theme.PRIMARY_LIGHT);
        semesterGpaValue.setText(String.format(Locale.ROOT, "%.2f", calculation.semesterGpa()));
        BigDecimal change = calculation.change();
        changeValue.setText(String.format(Locale.ROOT, "%s%.2f", change.signum() > 0 ? "+" : "", change));
        changeValue.setForeground(change.signum() >= 0 ? Theme.ACCENT_GREEN : Theme.ACCENT_ROSE);
        totalCreditsValue.setText(Integer.toString(calculation.totalCredits()));
        qualityPointsValue.setText(calculation.qualityPoints().stripTrailingZeros().toPlainString());
    }

    private void updateTargetResult(BigDecimal current, int creditsCompleted,
                                    GpaCalculator.Calculation calculation) {
        try {
            BigDecimal target = parseGpa(targetCgpaField.getText(), new BigDecimal("3.70"));
            int remaining = parseInteger(remainingCreditsField.getText(), 0);
            if (!validGpa(currentCgpaField.getText()) || !validNonNegativeInteger(completedCreditsField.getText())
                    || !validGpa(targetCgpaField.getText()) || remaining <= 0) {
                showEmptyResult();
                validationMessage.setText(showValidation ? "Enter valid GPA values and positive remaining credits." : " ");
                return;
            }
            BigDecimal required = calculator.requiredAverage(current, creditsCompleted, target, remaining);
            boolean impossible = required.compareTo(BigDecimal.valueOf(4)) > 0;
            resultValue.setText(impossible ? "> 4.00" : String.format(Locale.ROOT, "%.2f", required));
            gpaArc.setGpa(Math.min(4, required.doubleValue()));
            classification.setText(impossible ? "Not achievable at this GPA scale" : "Required average GPA");
            classification.setForeground(impossible ? Theme.ACCENT_ROSE : Theme.ACCENT_GREEN);
            classification.setBackground(impossible ? Theme.ACCENT_ROSE_BG : Theme.ACCENT_GREEN_BG);
            semesterGpaValue.setText(calculation == null ? "--" : String.format(Locale.ROOT, "%.2f", calculation.semesterGpa()));
            changeValue.setText(impossible ? "Above 4.00 maximum" : "Across " + remaining + " credits");
            changeValue.setForeground(impossible ? Theme.ACCENT_ROSE : Theme.TEXT_MUTED);
            totalCreditsValue.setText(Integer.toString(creditsCompleted + remaining));
            qualityPointsValue.setText("--");
            validationMessage.setText(" ");
        } catch (IllegalArgumentException ex) {
            showEmptyResult();
            validationMessage.setText(showValidation ? ex.getMessage() : " ");
        }
    }

    private void showEmptyResult() {
        resultValue.setText("--");
        gpaArc.setGpa(0);
        classification.setText("Add course details to calculate");
        classification.setForeground(Theme.TEXT_MUTED);
        classification.setBackground(Theme.ACCENT_GRAY_BG);
        semesterGpaValue.setText("--");
        changeValue.setText("--");
        changeValue.setForeground(Theme.TEXT_MAIN);
        totalCreditsValue.setText("--");
        qualityPointsValue.setText("--");
    }

    private void updateResultHeading() {
        resultHeading.setText(switch (mode) {
            case SEMESTER -> "Semester GPA";
            case PROJECTED -> "Projected CGPA";
            case TARGET -> "Target Planner";
        });
    }

    private void resetCalculator() {
        showValidation = false;
        rows.clear();
        courseRows.removeAll();
        currentCgpaField.setText(profile.cgpa().toPlainString());
        completedCreditsField.setText(Integer.toString(profile.earnedCredits()));
        targetCgpaField.setText("3.70");
        remainingCreditsField.setText(Integer.toString(Math.max(0, profile.plannedCredits() - profile.earnedCredits())));
        validationMessage.setText(" ");
        refreshInputErrors();
        addCourseRow();
    }

    private boolean validGpa(String value) {
        try {
            BigDecimal parsed = new BigDecimal(value.trim());
            return parsed.compareTo(BigDecimal.ZERO) >= 0 && parsed.compareTo(BigDecimal.valueOf(4)) <= 0;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private boolean validNonNegativeInteger(String value) {
        try { return Integer.parseInt(value.trim()) >= 0; }
        catch (NumberFormatException ex) { return false; }
    }

    private BigDecimal parseGpa(String value, BigDecimal fallback) {
        if (value == null || value.isBlank()) return fallback;
        try {
            BigDecimal parsed = new BigDecimal(value.trim());
            if (parsed.compareTo(BigDecimal.ZERO) < 0 || parsed.compareTo(BigDecimal.valueOf(4)) > 0) {
                throw new IllegalArgumentException("GPA must be between 0.00 and 4.00.");
            }
            return parsed;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Enter a valid GPA between 0.00 and 4.00.");
        }
    }

    private int parseInteger(String value, int fallback) {
        if (value == null || value.isBlank()) return fallback;
        try {
            int parsed = Integer.parseInt(value.trim());
            if (parsed < 0) throw new IllegalArgumentException("Credits cannot be negative.");
            return parsed;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Enter a valid non-negative credit value.");
        }
    }

    private JLabel label(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        label.setForeground(Theme.TEXT_MUTED);
        return label;
    }

    private static JLabel errorLabel() {
        JLabel label = new JLabel(" ");
        label.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        label.setForeground(Theme.PROFILE_ERROR);
        return label;
    }

    private GridBagConstraints constraints() {
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 0, 0);
        return c;
    }

    private void addGridCell(JPanel parent, Component component, int x, int y, int width, int height,
                             double weight, int anchor, Insets insets) {
        GridBagConstraints c = constraints();
        c.gridx = x;
        c.gridy = y;
        c.gridwidth = width;
        c.gridheight = height;
        c.weightx = weight;
        c.anchor = anchor;
        c.insets = insets;
        parent.add(component, c);
    }

    private final class CourseRow extends JPanel {
        private final StyledTextField nameField = new StyledTextField();
        private final JSpinner credits = new StyledSpinner();
        private final JComboBox<LetterGrade> grade = new StyledComboBox<>(LetterGrade.values());
        private final JLabel nameError = errorLabel();

        private CourseRow() {
            setOpaque(false);
            setLayout(new GridBagLayout());
            GridBagConstraints c = constraints();
            c.gridy = 0;
            c.weightx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = new Insets(0, 0, 2, 8);
            add(nameField, c);
            c.gridx = 1;
            c.weightx = 0;
            c.insets = new Insets(0, 0, 2, 8);
            add(credits, c);
            c.gridx = 2;
            c.insets = new Insets(0, 0, 2, 8);
            add(grade, c);
            c.gridx = 3;
            c.insets = new Insets(0, 0, 2, 0);
            add(new RemoveButton(() -> removeCourseRow(this)), c);
            c.gridx = 0;
            c.gridy = 1;
            c.gridwidth = 4;
            c.weightx = 1;
            c.insets = new Insets(0, 0, 0, 0);
            add(nameError, c);
            nameField.setToolTipText("Enter a course name");
            nameField.getDocument().addDocumentListener(new DocumentListener() {
                @Override public void insertUpdate(DocumentEvent e) { recalculate(); }
                @Override public void removeUpdate(DocumentEvent e) { recalculate(); }
                @Override public void changedUpdate(DocumentEvent e) { recalculate(); }
            });
            nameField.addFocusListener(new FocusAdapter() {
                @Override public void focusLost(FocusEvent e) {
                    showValidation = true;
                    recalculate();
                }
            });
            credits.addChangeListener(e -> recalculate());
            grade.addActionListener(e -> recalculate());
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));
        }
    }

    private static final class RoundedPanel extends JPanel {
        private RoundedPanel() {
            setOpaque(false);
        }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            Theme.setAntiAliasing(g);
            g.setColor(new Color(15, 23, 42, 8));
            g.fillRoundRect(2, 3, getWidth() - 4, getHeight() - 4, 16, 16);
            g.setColor(Color.WHITE);
            g.fillRoundRect(0, 0, getWidth() - 3, getHeight() - 4, 16, 16);
            g.setColor(new Color(229, 231, 235));
            g.drawRoundRect(0, 0, getWidth() - 4, getHeight() - 5, 16, 16);
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    private static final class StyledTextField extends JTextField {
        private boolean invalid;
        private StyledTextField() {
            setFont(new Font("Segoe UI", Font.PLAIN, 14));
            setForeground(Theme.TEXT_MAIN);
            setCaretColor(Theme.PRIMARY);
            setBorder(new EmptyBorder(8, 10, 8, 10));
            setPreferredSize(new Dimension(120, 40));
            setMinimumSize(new Dimension(70, 40));
            setOpaque(false);
            addFocusListener(new FocusAdapter() {
                @Override public void focusGained(FocusEvent e) { repaint(); }
                @Override public void focusLost(FocusEvent e) { repaint(); }
            });
        }
        private void setInvalid(boolean invalid) {
            this.invalid = invalid;
            repaint();
        }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            Theme.setAntiAliasing(g);
            Color border = invalid ? Theme.PROFILE_ERROR : isFocusOwner() ? Theme.PRIMARY : new Color(229, 231, 235);
            g.setColor(Color.WHITE);
            g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
            g.setColor(border);
            g.setStroke(new BasicStroke(isFocusOwner() ? 1.5f : 1f));
            g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    private static final class StyledComboBox<T> extends JComboBox<T> {
        private StyledComboBox(T[] items) {
            super(items);
            setFont(new Font("Segoe UI", Font.PLAIN, 14));
            setBackground(Color.WHITE);
            setForeground(Theme.TEXT_MAIN);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(229, 231, 235)),
                    new EmptyBorder(0, 8, 0, 8)));
            setPreferredSize(new Dimension(110, 40));
            setMinimumSize(new Dimension(90, 40));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
    }

    private static final class StyledSpinner extends JSpinner {
        private StyledSpinner() {
            super(new SpinnerNumberModel(3, 1, 6, 1));
            setFont(new Font("Segoe UI", Font.PLAIN, 14));
            setPreferredSize(new Dimension(90, 40));
            setMinimumSize(new Dimension(90, 40));
            setBorder(BorderFactory.createLineBorder(new Color(229, 231, 235)));
            JComponent editor = getEditor();
            editor.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            if (editor instanceof JSpinner.DefaultEditor defaultEditor) {
                defaultEditor.getTextField().setHorizontalAlignment(SwingConstants.CENTER);
                defaultEditor.getTextField().setBorder(new EmptyBorder(4, 4, 4, 4));
            }
        }
    }

    private static final class TabButton extends JButton {
        private boolean active;
        private TabButton(String text, boolean active) {
            super(text);
            this.active = active;
            setFont(new Font("Segoe UI", Font.BOLD, 14));
            setFocusPainted(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
        private void setActive(boolean active) {
            this.active = active;
            repaint();
        }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            Theme.setAntiAliasing(g);
            boolean hover = getModel().isRollover();
            g.setColor(active ? Theme.PRIMARY : hover ? Theme.PRIMARY_LIGHT : Color.WHITE);
            g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
            if (!active) {
                g.setColor(new Color(229, 231, 235));
                g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
            }
            setForeground(active ? Color.WHITE : Theme.TEXT_MUTED);
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    private abstract static class PaintedButton extends JButton {
        private final boolean primary;
        private final Color normal;
        private PaintedButton(String text, boolean primary, Color normal) {
            super(text);
            this.primary = primary;
            this.normal = normal;
            setFont(new Font("Segoe UI", Font.BOLD, 13));
            setFocusPainted(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(112, 40));
        }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            Theme.setAntiAliasing(g);
            Color fill = primary ? getModel().isPressed() ? Theme.PRIMARY_HOVER : getModel().isRollover()
                    ? Theme.PRIMARY_HOVER : Theme.PRIMARY : getModel().isRollover()
                    ? new Color(249, 250, 251) : Color.WHITE;
            g.setColor(fill);
            g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
            if (!primary) {
                g.setColor(new Color(229, 231, 235));
                g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
            }
            setForeground(primary ? Color.WHITE : normal);
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    private static final class PrimaryButton extends PaintedButton {
        private PrimaryButton(String text) { super(text, true, Color.WHITE); }
    }

    private static final class SecondaryButton extends PaintedButton {
        private SecondaryButton(String text) { super(text, false, Theme.TEXT_MAIN); }
    }

    private static final class RemoveButton extends JButton {
        private final Runnable action;
        private RemoveButton(Runnable action) {
            this.action = action;
            setToolTipText("Remove course");
            setPreferredSize(new Dimension(40, 40));
            setBorderPainted(false);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addActionListener(e -> action.run());
        }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            Theme.setAntiAliasing(g);
            boolean hover = getModel().isRollover();
            g.setColor(hover ? Theme.ACCENT_ROSE_BG : new Color(255, 255, 255, 0));
            g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
            g.setColor(hover ? Theme.ACCENT_ROSE : Theme.TEXT_MUTED);
            g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int x = getWidth() / 2;
            g.drawRoundRect(x - 7, 12, 14, 15, 2, 2);
            g.drawLine(x - 9, 10, x + 9, 10);
            g.drawLine(x - 3, 8, x + 3, 8);
            g.drawLine(x - 3, 15, x - 3, 24);
            g.drawLine(x + 3, 15, x + 3, 24);
            g.dispose();
        }
    }

    private static final class DashedAddButton extends JButton {
        private DashedAddButton() {
            super("+  Add Course");
            setFont(new Font("Segoe UI", Font.BOLD, 14));
            setForeground(Theme.PRIMARY);
            setPreferredSize(new Dimension(200, 44));
            setFocusPainted(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            Theme.setAntiAliasing(g);
            g.setColor(getModel().isRollover() ? Theme.PRIMARY_LIGHT : new Color(255, 255, 255, 0));
            g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
            g.setColor(Theme.PRIMARY_BORDER);
            g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 1, new float[]{5, 4}, 0));
            g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    private static final class GpaArc extends JComponent {
        private double value;
        private double displayedValue;
        private Timer animation;
        private GpaArc() {
            setPreferredSize(new Dimension(116, 116));
        }
        private void setGpa(double value) {
            this.value = Math.max(0, Math.min(4, value));
            if (animation != null) animation.stop();
            double start = displayedValue;
            long started = System.currentTimeMillis();
            animation = new Timer(15, e -> {
                double progress = Math.min(1, (System.currentTimeMillis() - started) / 260.0);
                displayedValue = start + (this.value - start) * progress;
                repaint();
                if (progress >= 1) animation.stop();
            });
            animation.start();
        }
        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            Theme.setAntiAliasing(g);
            int diameter = Math.min(getWidth(), getHeight()) - 16;
            int x = (getWidth() - diameter) / 2;
            int y = (getHeight() - diameter) / 2;
            g.setStroke(new BasicStroke(7, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(226, 232, 240));
            g.draw(new Arc2D.Double(x, y, diameter, diameter, 90, -360, Arc2D.OPEN));
            g.setColor(Theme.PRIMARY);
            g.draw(new Arc2D.Double(x, y, diameter, diameter, 90, -360 * displayedValue / 4, Arc2D.OPEN));
            g.dispose();
        }
    }
}
