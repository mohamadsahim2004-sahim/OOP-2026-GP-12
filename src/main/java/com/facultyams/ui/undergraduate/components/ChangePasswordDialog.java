package com.facultyams.ui.components;

import com.facultyams.service.profile.StudentProfileService;
import com.facultyams.service.profile.validation.PasswordStrengthEvaluator;
import com.facultyams.ui.theme.Theme;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.List;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/** Modal password editor with live strength and requirement feedback. */
public final class ChangePasswordDialog {
    private ChangePasswordDialog() { }

    /** Opens the password editor and updates the password on a background worker. */
    public static void show(Window owner, StudentProfileService service, Runnable onSuccess) {
        JRootPane root = owner instanceof RootPaneContainer container ? container.getRootPane() : null;
        Component previousGlass = root == null ? null : root.getGlassPane();
        boolean previousVisible = previousGlass != null && previousGlass.isVisible();
        if (root != null) {
            JPanel dim = new JPanel() {
                @Override protected void paintComponent(Graphics graphics) {
                    Graphics2D g = (Graphics2D) graphics.create();
                    Theme.setAntiAliasing(g);
                    g.setColor(Theme.PROFILE_OVERLAY);
                    g.fillRect(0, 0, getWidth(), getHeight());
                    g.dispose();
                }
            };
            dim.setOpaque(false);
            dim.addMouseListener(new java.awt.event.MouseAdapter() { });
            root.setGlassPane(dim);
            dim.setVisible(true);
        }
        PasswordDialog dialog = new PasswordDialog(owner, service, onSuccess);
        dialog.setVisible(true);
        if (root != null && previousGlass != null) {
            root.setGlassPane(previousGlass);
            previousGlass.setVisible(previousVisible);
        }
    }

    private static final class PasswordDialog extends JDialog {
        private final StudentProfileService service;
        private final Runnable onSuccess;
        private final JPasswordField current = new JPasswordField();
        private final JPasswordField next = new JPasswordField();
        private final JPasswordField confirm = new JPasswordField();
        private final FormField currentField = new FormField("Current Password", passwordInput(current), false);
        private final FormField newField = new FormField("New Password", passwordInput(next), false);
        private final FormField confirmField = new FormField("Confirm New Password", passwordInput(confirm), false);
        private final JLabel match = new JLabel(" ");
        private final JLabel strengthText = new JLabel("Enter a password");
        private final StrengthMeter meter = new StrengthMeter();
        private final PasswordStrengthEvaluator evaluator = new PasswordStrengthEvaluator();
        private final List<Requirement> requirements;
        private final FlatButton update = new FlatButton("Update Password", true);
        private final JProgressBar spinner = new JProgressBar();
        private JButton cancelButton;
        private JButton closeButton;
        private boolean saving;
        private float surfaceAlpha = 0.65f;
        private float dialogScale = 0.96f;
        private final JPanel surface = new JPanel(new BorderLayout(0, 0)) {
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                Theme.setAntiAliasing(g);
                g.setColor(Theme.PROFILE_SHADOW_SOFT);
                g.fillRoundRect(0, 8, getWidth() - 1, getHeight() - 10, 22, 22);
                g.setComposite(AlphaComposite.SrcOver.derive(surfaceAlpha));
                g.setColor(Color.WHITE);
                g.fillRoundRect(2, 2, getWidth() - 5, getHeight() - 7, 20, 20);
                g.setColor(Theme.TIMETABLE_CARD_BORDER);
                g.drawRoundRect(2, 2, getWidth() - 5, getHeight() - 7, 20, 20);
                g.dispose();
                super.paintComponent(graphics);
            }
        };
        private final JLayeredPane layered = new JLayeredPane() {
            @Override public void doLayout() {
                int width = Math.round(getWidth() * dialogScale);
                int height = Math.round(getHeight() * dialogScale);
                surface.setBounds((getWidth() - width) / 2, (getHeight() - height) / 2, width, height);
            }
            @Override public Dimension getPreferredSize() { return surface.getPreferredSize(); }
        };

        private PasswordDialog(Window owner, StudentProfileService service, Runnable onSuccess) {
            super(owner, "Change Password", ModalityType.APPLICATION_MODAL);
            this.service = service;
            this.onSuccess = onSuccess;
            setUndecorated(true);
            setBackground(new Color(0, 0, 0, 0));
            surface.setOpaque(false);
            surface.setBorder(BorderFactory.createEmptyBorder(18, 22, 20, 22));
            layered.add(surface, JLayeredPane.DEFAULT_LAYER);
            setContentPane(layered);
            requirements = evaluator.ruleLabels().stream().map(Requirement::new).toList();
            buildContent();
            installListeners();
            getRootPane().registerKeyboardAction(e -> requestClose(),
                    KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
            setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
            addWindowListener(new java.awt.event.WindowAdapter() {
                @Override public void windowClosing(java.awt.event.WindowEvent e) { requestClose(); }
            });
            setSize(460, 710);
            setLocationRelativeTo(owner);
            SwingUtilities.invokeLater(this::animateIn);
        }

        private void animateIn() {
            long started = System.nanoTime();
            Timer animation = new Timer(15, event -> {
                float progress = Math.min(1f, (System.nanoTime() - started) / 150_000_000f);
                surfaceAlpha = 0.65f + 0.35f * progress;
                dialogScale = 0.96f + 0.04f * progress;
                layered.doLayout();
                surface.repaint();
                if (progress >= 1f) ((Timer) event.getSource()).stop();
            });
            animation.start();
        }

        private void buildContent() {
            JPanel header = new JPanel(new BorderLayout(12, 0));
            header.setOpaque(false);
            JLabel lock = new JLabel(new ProfileIcon(ProfileIcon.Shape.LOCK, Theme.PRIMARY, 20));
            lock.setHorizontalAlignment(SwingConstants.CENTER);
            lock.setPreferredSize(new Dimension(40, 40));
            lock.setOpaque(true);
            lock.setBackground(Theme.PRIMARY_LIGHT);
            lock.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            JPanel titleBox = new JPanel();
            titleBox.setOpaque(false);
            titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
            JLabel title = new JLabel("Change Password");
            title.setFont(Theme.PROFILE_DIALOG_TITLE);
            title.setForeground(Theme.TEXT_MAIN);
            JLabel subtitle = new JLabel("Choose a strong password you don't use elsewhere.");
            subtitle.setFont(Theme.FONT_BODY);
            subtitle.setForeground(Theme.TEXT_MUTED);
            titleBox.add(title);
            titleBox.add(Box.createVerticalStrut(3));
            titleBox.add(subtitle);
            closeButton = iconButton(ProfileIcon.Shape.CLOSE, "Close");
            closeButton.addActionListener(e -> requestClose());
            header.add(lock, BorderLayout.WEST);
            header.add(titleBox, BorderLayout.CENTER);
            header.add(closeButton, BorderLayout.EAST);

            JPanel body = new JPanel();
            body.setOpaque(false);
            body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
            body.add(Box.createVerticalStrut(20));
            body.add(currentField);
            body.add(Box.createVerticalStrut(16));
            body.add(newField);
            JPanel meterLine = new JPanel(new BorderLayout(8, 0));
            meterLine.setOpaque(false);
            meter.setPreferredSize(new Dimension(210, 8));
            meterLine.add(meter, BorderLayout.CENTER);
            strengthText.setFont(Theme.FONT_SMALL);
            strengthText.setForeground(Theme.TEXT_MUTED);
            strengthText.setHorizontalAlignment(SwingConstants.RIGHT);
            meterLine.add(strengthText, BorderLayout.EAST);
            body.add(Box.createVerticalStrut(8));
            body.add(meterLine);
            JPanel rules = new JPanel(new GridLayout(3, 2, 8, 6));
            rules.setOpaque(false);
            requirements.forEach(rules::add);
            body.add(Box.createVerticalStrut(12));
            body.add(rules);
            body.add(Box.createVerticalStrut(16));
            body.add(confirmField);
            match.setFont(Theme.FONT_SMALL);
            match.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
            confirmField.add(match);
            body.add(Box.createVerticalStrut(16));

            JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 12));
            footer.setOpaque(false);
            footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.TIMETABLE_CARD_BORDER));
            cancelButton = new FlatButton("Cancel", false);
            cancelButton.addActionListener(e -> requestClose());
            spinner.setIndeterminate(true);
            spinner.setPreferredSize(new Dimension(22, 22));
            spinner.setBorderPainted(false);
            spinner.setVisible(false);
            update.setMnemonic(KeyEvent.VK_U);
            update.setEnabled(false);
            update.addActionListener(e -> save());
            footer.add(cancelButton);
            footer.add(spinner);
            footer.add(update);

            JPanel headerBlock = new JPanel();
            headerBlock.setOpaque(false);
            headerBlock.setLayout(new BoxLayout(headerBlock, BoxLayout.Y_AXIS));
            header.setAlignmentX(Component.LEFT_ALIGNMENT);
            headerBlock.add(header);
            headerBlock.add(Box.createVerticalStrut(16));
            headerBlock.add(divider());

            JPanel content = new JPanel(new BorderLayout());
            content.setOpaque(false);
            content.add(headerBlock, BorderLayout.NORTH);
            content.add(body, BorderLayout.CENTER);
            content.add(footer, BorderLayout.SOUTH);
            surface.add(content, BorderLayout.CENTER);
        }

        private void installListeners() {
            DocumentListener listener = new DocumentListener() {
                private void changed() {
                    if (currentField.hasError()) currentField.setError(null);
                    updateState();
                }
                @Override public void insertUpdate(DocumentEvent e) { changed(); }
                @Override public void removeUpdate(DocumentEvent e) { changed(); }
                @Override public void changedUpdate(DocumentEvent e) { changed(); }
            };
            current.getDocument().addDocumentListener(listener);
            next.getDocument().addDocumentListener(listener);
            confirm.getDocument().addDocumentListener(listener);
            updateState();
        }

        private void updateState() {
            char[] old = current.getPassword();
            char[] value = next.getPassword();
            char[] confirmation = confirm.getPassword();
            boolean[] satisfied = evaluator.evaluate(value, old);
            int passed = 0;
            int strengthPassed = 0;
            for (int i = 0; i < requirements.size(); i++) {
                requirements.get(i).setSatisfied(satisfied[i]);
                if (satisfied[i]) passed++;
                if (i < 5 && satisfied[i]) strengthPassed++;
            }
            meter.setLevel(strengthPassed);
            strengthText.setText(strengthPassed < 2 ? "Weak" : strengthPassed < 3 ? "Fair"
                    : strengthPassed < 5 ? "Good" : "Strong");
            strengthText.setForeground(strengthPassed < 2 ? Theme.PROFILE_ERROR : strengthPassed < 3
                    ? Theme.PROFILE_WARNING : strengthPassed < 5 ? Theme.PRIMARY : Theme.PROFILE_SUCCESS);
            boolean matches = Arrays.equals(value, confirmation) && confirmation.length > 0;
            match.setText(confirmation.length == 0 ? " " : matches ? "Passwords match" : "Passwords do not match");
            match.setForeground(matches ? Theme.PROFILE_SUCCESS : Theme.PROFILE_ERROR);
            boolean ready = old.length > 0 && passed == requirements.size() && matches;
            update.setEnabled(ready);
            Arrays.fill(old, '\0');
            Arrays.fill(value, '\0');
            Arrays.fill(confirmation, '\0');
        }

        private void save() {
            char[] old = current.getPassword();
            char[] value = next.getPassword();
            setBusy(true);
            new SwingWorker<Void, Void>() {
                @Override protected Void doInBackground() {
                    service.changePassword(old, value);
                    return null;
                }

                @Override protected void done() {
                    try {
                        get();
                        onSuccess.run();
                        dispose();
                    } catch (Exception ex) {
                        Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                        currentField.setError(cause.getMessage());
                        setBusy(false);
                    } finally {
                        Arrays.fill(old, '\0');
                        Arrays.fill(value, '\0');
                        Arrays.fill(current.getPassword(), '\0');
                        Arrays.fill(next.getPassword(), '\0');
                        Arrays.fill(confirm.getPassword(), '\0');
                    }
                }
            }.execute();
        }

        private void setBusy(boolean busy) {
            saving = busy;
            current.setEnabled(!busy);
            next.setEnabled(!busy);
            confirm.setEnabled(!busy);
            update.setEnabled(false);
            cancelButton.setEnabled(!busy);
            closeButton.setEnabled(!busy);
            spinner.setVisible(busy);
            if (!busy) updateState();
        }

        private void requestClose() {
            if (saving) return;
            char[] old = current.getPassword();
            char[] value = next.getPassword();
            char[] repeat = confirm.getPassword();
            boolean changed = old.length > 0 || value.length > 0 || repeat.length > 0;
            Arrays.fill(old, '\0');
            Arrays.fill(value, '\0');
            Arrays.fill(repeat, '\0');
            if (changed && JOptionPane.showConfirmDialog(this, "Discard password changes?",
                    "Discard changes", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) {
                return;
            }
            Arrays.fill(current.getPassword(), '\0');
            Arrays.fill(next.getPassword(), '\0');
            Arrays.fill(confirm.getPassword(), '\0');
            dispose();
        }

        private static JPanel passwordInput(JPasswordField field) {
            JPanel row = new JPanel(new BorderLayout());
            row.setOpaque(false);
            row.setBackground(Color.WHITE);
            field.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 4));
            field.setOpaque(false);
            field.setFont(Theme.FONT_BODY);
            JButton eye = iconButton(ProfileIcon.Shape.EYE, "Show password");
            eye.addActionListener(e -> {
                boolean visible = field.getEchoChar() != 0;
                field.setEchoChar(visible ? (char) 0 : '\u2022');
                eye.setIcon(new ProfileIcon(visible ? ProfileIcon.Shape.EYE_OFF : ProfileIcon.Shape.EYE,
                        Theme.TEXT_MUTED, 16));
                eye.setToolTipText(visible ? "Hide password" : "Show password");
            });
            row.add(field, BorderLayout.CENTER);
            row.add(eye, BorderLayout.EAST);
            return row;
        }

        private static JButton iconButton(ProfileIcon.Shape shape, String tooltip) {
            JButton button = new JButton(new ProfileIcon(shape, Theme.TEXT_MUTED, 16));
            button.setToolTipText(tooltip);
            button.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
            button.setContentAreaFilled(false);
            button.setFocusPainted(false);
            return button;
        }

        private static JComponent divider() {
            JPanel line = new JPanel();
            line.setOpaque(true);
            line.setBackground(Theme.TIMETABLE_CARD_BORDER);
            line.setPreferredSize(new Dimension(100, 1));
            line.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
            return line;
        }

    }

    private static final class Requirement extends JPanel {
        private boolean satisfied;

        private Requirement(String text) {
            super(new BorderLayout(6, 0));
            setOpaque(false);
            JLabel icon = new JLabel(new RequirementIcon(false));
            JLabel label = new JLabel(text);
            label.setFont(Theme.FONT_SMALL);
            label.setForeground(Theme.TEXT_MUTED);
            add(icon, BorderLayout.WEST);
            add(label, BorderLayout.CENTER);
        }

        private void setSatisfied(boolean satisfied) {
            this.satisfied = satisfied;
            JLabel icon = (JLabel) getComponent(0);
            icon.setIcon(new RequirementIcon(satisfied));
            ((JLabel) getComponent(1)).setForeground(satisfied ? Theme.PROFILE_SUCCESS : Theme.TEXT_MUTED);
        }
    }

    private static final class RequirementIcon implements Icon {
        private final boolean checked;
        private RequirementIcon(boolean checked) { this.checked = checked; }
        @Override public int getIconWidth() { return 14; }
        @Override public int getIconHeight() { return 14; }
        @Override public void paintIcon(Component c, Graphics graphics, int x, int y) {
            Graphics2D g = (Graphics2D) graphics.create();
            Theme.setAntiAliasing(g);
            if (checked) {
                g.setColor(Theme.PROFILE_SUCCESS);
                g.fillOval(x, y, 14, 14);
                g.setColor(Color.WHITE);
                g.setStroke(new BasicStroke(1.6f));
                g.drawLine(x + 3, y + 7, x + 6, y + 10);
                g.drawLine(x + 6, y + 10, x + 11, y + 4);
            } else {
                g.setColor(Theme.TEXT_SUBTLE);
                g.drawOval(x + 1, y + 1, 12, 12);
            }
            g.dispose();
        }
    }

    private static final class StrengthMeter extends JComponent {
        private int level;
        private StrengthMeter() { setOpaque(false); }
        private void setLevel(int level) { this.level = Math.max(0, Math.min(6, level)); repaint(); }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            Theme.setAntiAliasing(g);
            int gap = 4;
            int segmentWidth = (getWidth() - gap * 3) / 4;
            Color[] colors = {Theme.PROFILE_ERROR, Theme.PROFILE_WARNING, Theme.PRIMARY, Theme.PROFILE_SUCCESS};
            int filled = level == 0 ? 0 : level <= 2 ? 1 : level <= 3 ? 2 : level <= 5 ? 3 : 4;
            for (int i = 0; i < 4; i++) {
                int x = i * (segmentWidth + gap);
                g.setColor(i < filled ? colors[i] : Theme.ACCENT_GRAY_BG);
                g.fillRoundRect(x, (getHeight() - 8) / 2, segmentWidth, 8, 8, 8);
            }
            g.dispose();
        }
    }
}
