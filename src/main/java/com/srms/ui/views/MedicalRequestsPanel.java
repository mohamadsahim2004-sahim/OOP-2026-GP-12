package com.srms.ui.views;

import com.srms.Theme;
import com.srms.Ui;
import com.srms.model.MedicalRequest;
import com.srms.model.MedicalStatus;
import com.srms.service.MedicalService;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import javax.swing.*;

/**
 * MedicalRequestsPanel showing submitted requests, status pills, details,
 * withdrawal, and demo approval triggers.
 */
public class MedicalRequestsPanel extends JPanel {
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);

    private final MedicalService medicalService;
    private final String studentId;
    private final JPanel listContainer = new JPanel();

    public MedicalRequestsPanel(MedicalService medicalService, String studentId) {
        this.medicalService = medicalService;
        this.studentId = studentId;

        setLayout(new BorderLayout());
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(10, 0, 16, 0));

        initUI();
        loadData();
    }

    private void initUI() {
        listContainer.setLayout(new BoxLayout(listContainer, BoxLayout.Y_AXIS));
        listContainer.setOpaque(false);

        JScrollPane scrollPane = new JScrollPane(listContainer);
        Ui.applyThinScrollBar(scrollPane);
        add(scrollPane, BorderLayout.CENTER);
    }

    public void loadData() {
        new SwingWorker<List<MedicalRequest>, Void>() {
            @Override
            protected List<MedicalRequest> doInBackground() {
                return medicalService.list(studentId);
            }

            @Override
            protected void done() {
                try {
                    List<MedicalRequest> reqs = get();
                    buildList(reqs);
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    private void buildList(List<MedicalRequest> requests) {
        listContainer.removeAll();

        if (requests.isEmpty()) {
            JPanel empty = new JPanel(new GridBagLayout());
            empty.setOpaque(false);
            empty.setBorder(BorderFactory.createEmptyBorder(60, 20, 60, 20));
            JLabel lbl = new JLabel("No medical requests submitted yet.");
            lbl.setFont(Theme.FONT_BODY);
            lbl.setForeground(Theme.TEXT_MUTED);
            empty.add(lbl);
            listContainer.add(empty);
        } else {
            for (MedicalRequest mr : requests) {
                listContainer.add(createRequestCard(mr));
                listContainer.add(Box.createVerticalStrut(14));
            }
        }

        listContainer.revalidate();
        listContainer.repaint();
    }

    private JPanel createRequestCard(MedicalRequest mr) {
        Ui.Card card = new Ui.Card();
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        // Header Row
        JPanel header = new JPanel(new BorderLayout(14, 0));
        header.setOpaque(false);

        // Left: Ref + Dates
        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);

        JLabel refLbl = new JLabel(mr.referenceNo());
        refLbl.setFont(Theme.FONT_BODY_BOLD);
        refLbl.setForeground(Theme.PRIMARY);

        String rangeStr = mr.from().format(DATE_FMT) + " – " + mr.to().format(DATE_FMT) +
                " • " + mr.sessionIds().size() + " sessions covered";
        JLabel rangeLbl = new JLabel(rangeStr);
        rangeLbl.setFont(Theme.FONT_SMALL);
        rangeLbl.setForeground(Theme.TEXT_MAIN);

        left.add(refLbl);
        left.add(Box.createVerticalStrut(2));
        left.add(rangeLbl);
        header.add(left, BorderLayout.WEST);

        // Right: Status Pill + Actions
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);

        Ui.Pill pill = getStatusPill(mr.status());
        right.add(pill);

        if (mr.isPending()) {
            JButton withdrawBtn = new JButton("Withdraw");
            withdrawBtn.setFont(Theme.FONT_TINY);
            withdrawBtn.setForeground(Theme.DANGER);
            withdrawBtn.setBackground(Theme.DANGER_BG);
            withdrawBtn.setFocusPainted(false);
            withdrawBtn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(254, 202, 202), 1),
                    BorderFactory.createEmptyBorder(4, 10, 4, 10)
            ));
            withdrawBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            withdrawBtn.addActionListener(e -> {
                int res = JOptionPane.showConfirmDialog(this,
                        "Are you sure you want to withdraw " + mr.referenceNo() + "?",
                        "Confirm Withdrawal", JOptionPane.YES_NO_OPTION);
                if (res == JOptionPane.YES_OPTION) {
                    medicalService.withdraw(mr.id());
                }
            });
            right.add(withdrawBtn);

            // Test Demo Approval Button
            JButton approveDemoBtn = new JButton("Approve (Demo)");
            approveDemoBtn.setFont(Theme.FONT_TINY);
            approveDemoBtn.setForeground(Theme.SUCCESS);
            approveDemoBtn.setBackground(Theme.SUCCESS_BG);
            approveDemoBtn.setFocusPainted(false);
            approveDemoBtn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(187, 247, 208), 1),
                    BorderFactory.createEmptyBorder(4, 10, 4, 10)
            ));
            approveDemoBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            approveDemoBtn.addActionListener(e -> medicalService.approve(mr.id()));
            right.add(approveDemoBtn);
        }

        header.add(right, BorderLayout.EAST);
        card.add(header, BorderLayout.NORTH);

        // Expanded Details
        JPanel details = new JPanel();
        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
        details.setOpaque(true);
        details.setBackground(new Color(249, 250, 251));
        details.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        JLabel reasonLbl = new JLabel("Reason: " + mr.reason() + " • Hospital/Doctor: " + mr.doctor() + " (Cert #" + mr.certificateNo() + ")");
        reasonLbl.setFont(Theme.FONT_SMALL);
        reasonLbl.setForeground(Theme.TEXT_MAIN);
        details.add(reasonLbl);

        if (mr.notes() != null && !mr.notes().isBlank()) {
            JLabel notesLbl = new JLabel("Notes: " + mr.notes());
            notesLbl.setFont(Theme.FONT_SMALL);
            notesLbl.setForeground(Theme.TEXT_MUTED);
            details.add(Box.createVerticalStrut(4));
            details.add(notesLbl);
        }

        if (mr.reviewerRemarks() != null) {
            JLabel remarksLbl = new JLabel("Reviewer: " + mr.reviewerRemarks());
            remarksLbl.setFont(Theme.FONT_SMALL);
            remarksLbl.setForeground(mr.status() == MedicalStatus.APPROVED ? Theme.SUCCESS : Theme.TEXT_MUTED);
            details.add(Box.createVerticalStrut(4));
            details.add(remarksLbl);
        }

        // Attached files row
        if (mr.files() != null && !mr.files().isEmpty()) {
            JPanel filesRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
            filesRow.setOpaque(false);
            JLabel filesTitle = new JLabel("Attachments:");
            filesTitle.setFont(Theme.FONT_TINY);
            filesTitle.setForeground(Theme.TEXT_MUTED);
            filesRow.add(filesTitle);

            for (String fn : mr.files()) {
                JLabel fLbl = new JLabel("📄 " + fn);
                fLbl.setFont(Theme.FONT_SMALL);
                fLbl.setForeground(Theme.PRIMARY);
                fLbl.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                fLbl.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        File f = new File("data/medical/" + studentId + "/" + mr.id() + "/" + fn);
                        if (f.exists() && Desktop.isDesktopSupported()) {
                            try { Desktop.getDesktop().open(f); } catch (Exception ignored) {}
                        } else {
                            JOptionPane.showMessageDialog(MedicalRequestsPanel.this,
                                    "File opened: " + fn, "Attachment", JOptionPane.INFORMATION_MESSAGE);
                        }
                    }
                });
                filesRow.add(fLbl);
            }
            details.add(Box.createVerticalStrut(4));
            details.add(filesRow);
        }

        card.add(details, BorderLayout.CENTER);
        return card;
    }

    private Ui.Pill getStatusPill(MedicalStatus st) {
        if (st == null) st = MedicalStatus.PENDING;
        return switch (st) {
            case APPROVED -> new Ui.Pill("Approved", Theme.SUCCESS_BG, Theme.SUCCESS);
            case REJECTED -> new Ui.Pill("Rejected", Theme.DANGER_BG, Theme.DANGER);
            case WITHDRAWN -> new Ui.Pill("Withdrawn", new Color(241, 245, 249), Theme.TEXT_MUTED);
            case PENDING -> new Ui.Pill("Pending Review", Theme.WARNING_BG, Theme.WARNING);
        };
    }
}
