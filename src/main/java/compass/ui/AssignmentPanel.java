package compass.ui;
import compass.model.WhatIfScoreCalculator;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.math.BigDecimal;

/** Search and score simulation for the assignments currently loaded from Canvas. */
public final class AssignmentPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final JTextField search = new JTextField(25);
    private final JTextField score = new JTextField(9);
    private final JButton calculate = new JButton("Calculate");
    private final JLabel count = new JLabel();
    private final JLabel selected = new JLabel();
    private final JLabel possible = new JLabel();
    private final JLabel result = new JLabel();
    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"Course", "Assignment", "Due date", "Points"}, 0) {
        private static final long serialVersionUID = 1L;
        @Override public boolean isCellEditable(int row, int column) { return false; }
        @Override public Class<?> getColumnClass(int column) { return column == 3 ? BigDecimal.class : String.class; }
    };
    private final JTable table = new JTable(model);
    // The row filter is runtime UI state, matching JTable's transient sorter.
    private final transient TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
    private JPanel searchControls() {
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        JLabel label = new JLabel("Search course or assignment:");
        label.setLabelFor(search);
        controls.add(label); controls.add(search); controls.add(count);
        return controls;
    }
    private JPanel scoreControls() {
        JPanel summary = new JPanel(new BorderLayout(5, 0));
        summary.add(selected, BorderLayout.CENTER); summary.add(possible, BorderLayout.EAST);
        JPanel input = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        JLabel label = new JLabel("What-if score:");
        label.setLabelFor(score);
        input.add(label); input.add(score); input.add(calculate);
        input.add(new JLabel("Assignment percentage only"));
        JPanel controls = new JPanel(new GridLayout(3, 1, 0, 4));
        controls.add(summary); controls.add(input); controls.add(result);
        return controls;
    }

}
