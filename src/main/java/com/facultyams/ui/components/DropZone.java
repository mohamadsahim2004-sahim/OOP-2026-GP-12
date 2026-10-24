package com.facultyams.ui.components;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.facultyams.Theme;

/**
 * Modern dashed-border Drag-and-Drop file upload zone with removable file chips,
 * file-picker button, and extension filter support.
 */
public class DropZone extends JPanel {
    private final List<String> allowedTypes;
    private final int maxFiles;
    private final int maxFileMb;
    private final List<File> selectedFiles = new ArrayList<>();
    private final JPanel chipsContainer = new JPanel();
    private final JProgressBar uploadProgress = new JProgressBar(0, 100);
    private Consumer<String> onError;
    private boolean isDragOver = false;

    public DropZone(List<String> allowedTypes, int maxFiles, int maxFileMb) {
        this.allowedTypes = allowedTypes != null ? allowedTypes : List.of();
        this.maxFiles = maxFiles > 0 ? maxFiles : 5;
        this.maxFileMb = maxFileMb > 0 ? maxFileMb : 20;

        setLayout(new BorderLayout(10, 10));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        setPreferredSize(new Dimension(480, 150));
        setMaximumSize(new Dimension(Short.MAX_VALUE, 160));

        initUI();
        initTransferHandler();
    }

    public void setOnError(Consumer<String> onError) {
        this.onError = onError;
    }

    private void initUI() {
        // Center instructions + button
        JPanel promptPanel = new JPanel();
        promptPanel.setLayout(new BoxLayout(promptPanel, BoxLayout.Y_AXIS));
        promptPanel.setOpaque(false);

        // Upload cloud/arrow icon
        ActivityIcon uploadIcon = new ActivityIcon(ActivityIcon.Type.ASSIGNMENT);
        uploadIcon.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel promptText = new JLabel("Drag and drop files here to upload");
        promptText.setFont(Theme.FONT_BODY_BOLD);
        promptText.setForeground(Theme.TEXT_MAIN);
        promptText.setAlignmentX(Component.CENTER_ALIGNMENT);

        String extStr = String.join(", ", allowedTypes).toUpperCase();
        JLabel typesText = new JLabel("Allowed formats: " + extStr + " (Max " + maxFileMb + "MB each, up to " + maxFiles + " files)");
        typesText.setFont(Theme.FONT_SMALL);
        typesText.setForeground(Theme.TEXT_MUTED);
        typesText.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton chooseBtn = new JButton("Choose file");
        chooseBtn.setFont(Theme.FONT_BODY_BOLD);
        chooseBtn.setForeground(Theme.PRIMARY);
        chooseBtn.setBackground(Theme.PRIMARY_LIGHT);
        chooseBtn.setFocusPainted(false);
        chooseBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.PRIMARY_BORDER, 1),
                BorderFactory.createEmptyBorder(6, 16, 6, 16)
        ));
        chooseBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        chooseBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        chooseBtn.addActionListener(e -> chooseFiles());

        promptPanel.add(uploadIcon);
        promptPanel.add(Box.createVerticalStrut(8));
        promptPanel.add(promptText);
        promptPanel.add(Box.createVerticalStrut(4));
        promptPanel.add(typesText);
        promptPanel.add(Box.createVerticalStrut(12));
        promptPanel.add(chooseBtn);

        add(promptPanel, BorderLayout.CENTER);

        // Chips and progress container on SOUTH
        JPanel south = new JPanel(new BorderLayout(0, 8));
        south.setOpaque(false);

        chipsContainer.setLayout(new FlowLayout(FlowLayout.CENTER, 8, 8));
        chipsContainer.setOpaque(false);
        south.add(chipsContainer, BorderLayout.CENTER);

        uploadProgress.setPreferredSize(new Dimension(200, 4));
        uploadProgress.setForeground(Theme.PRIMARY);
        uploadProgress.setBackground(Theme.CARD_BORDER);
        uploadProgress.setBorderPainted(false);
        uploadProgress.setVisible(false);
        south.add(uploadProgress, BorderLayout.SOUTH);

        add(south, BorderLayout.SOUTH);
    }

    private void chooseFiles() {
        JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(true);

        if (!allowedTypes.isEmpty()) {
            String[] exts = allowedTypes.toArray(new String[0]);
            chooser.setFileFilter(new FileNameExtensionFilter("Allowed files (" + String.join(", ", exts) + ")", exts));
        }

        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File[] files = chooser.getSelectedFiles();
            for (File f : files) {
                addFile(f);
            }
        }
    }

    private void initTransferHandler() {
        setTransferHandler(new TransferHandler() {
            @Override
            public boolean canImport(TransferSupport support) {
                if (!support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    return false;
                }
                isDragOver = true;
                repaint();
                return true;
            }

            @Override
            @SuppressWarnings("unchecked")
            public boolean importData(TransferSupport support) {
                isDragOver = false;
                repaint();
                try {
                    Transferable t = support.getTransferable();
                    List<File> files = (List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);
                    for (File f : files) {
                        addFile(f);
                    }
                    return true;
                } catch (Exception ex) {
                    if (onError != null) onError.accept("Could not read dropped file: " + ex.getMessage());
                    return false;
                }
            }
        });
    }

    public void addFile(File file) {
        if (file == null) return;

        // Validation 1: Max files
        if (selectedFiles.size() >= maxFiles) {
            if (onError != null) onError.accept("Maximum limit of " + maxFiles + " files reached.");
            return;
        }

        // Validation 2: File type
        String name = file.getName().toLowerCase();
        boolean validExt = allowedTypes.isEmpty() || allowedTypes.stream().anyMatch(name::endsWith);
        if (!validExt) {
            if (onError != null) onError.accept("Invalid file type. Allowed: " + String.join(", ", allowedTypes));
            return;
        }

        // Validation 3: File size
        long maxBytes = (long) maxFileMb * 1024 * 1024;
        if (file.length() > maxBytes) {
            if (onError != null) onError.accept("File '" + file.getName() + "' exceeds the " + maxFileMb + "MB limit.");
            return;
        }

        if (!selectedFiles.contains(file)) {
            selectedFiles.add(file);
            rebuildChips();
            if (onError != null) onError.accept(null); // clear error
        }
    }

    public void removeFile(File file) {
        selectedFiles.remove(file);
        rebuildChips();
    }

    public void clearFiles() {
        selectedFiles.clear();
        rebuildChips();
    }

    public List<File> getSelectedFiles() {
        return new ArrayList<>(selectedFiles);
    }

    public void setUploading(boolean uploading, int progress) {
        uploadProgress.setVisible(uploading);
        uploadProgress.setValue(progress);
    }

    private void rebuildChips() {
        chipsContainer.removeAll();
        for (File f : selectedFiles) {
            chipsContainer.add(createFileChip(f));
        }
        chipsContainer.revalidate();
        chipsContainer.repaint();
    }

    private JPanel createFileChip(File f) {
        JPanel chip = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        chip.setOpaque(true);
        chip.setBackground(new Color(241, 245, 249));
        chip.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.CARD_BORDER, 1),
                BorderFactory.createEmptyBorder(2, 6, 2, 6)
        ));

        String sizeStr = f.length() < 1024 * 1024
                ? String.format("%.1f KB", f.length() / 1024.0)
                : String.format("%.1f MB", f.length() / (1024.0 * 1024.0));

        JLabel nameLbl = new JLabel(f.getName() + " (" + sizeStr + ")");
        nameLbl.setFont(Theme.FONT_SMALL);
        nameLbl.setForeground(Theme.TEXT_MAIN);

        JButton removeBtn = new JButton("✕");
        removeBtn.setFont(new Font("Segoe UI", Font.BOLD, 10));
        removeBtn.setForeground(Theme.TEXT_MUTED);
        removeBtn.setBorderPainted(false);
        removeBtn.setFocusPainted(false);
        removeBtn.setContentAreaFilled(false);
        removeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        removeBtn.addActionListener(e -> removeFile(f));

        chip.add(nameLbl);
        chip.add(removeBtn);
        return chip;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.setAntiAliasing(g2);

        // Dashed border
        float[] dash = { 6.0f, 6.0f };
        Stroke dashed = new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dash, 0.0f);
        g2.setStroke(dashed);

        if (isDragOver) {
            g2.setColor(Theme.PRIMARY_LIGHT);
            g2.fillRoundRect(2, 2, getWidth() - 4, getHeight() - 4, 12, 12);
            g2.setColor(Theme.PRIMARY);
        } else {
            g2.setColor(Theme.CARD_BORDER);
        }

        g2.drawRoundRect(2, 2, getWidth() - 5, getHeight() - 5, 12, 12);
        g2.dispose();
        super.paintComponent(g);
    }
}
