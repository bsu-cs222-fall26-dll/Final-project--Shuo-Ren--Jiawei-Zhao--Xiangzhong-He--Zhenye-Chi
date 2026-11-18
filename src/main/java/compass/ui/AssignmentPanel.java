package compass.ui;
import compass.model.WhatIfScoreCalculator;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.math.BigDecimal;
import compass.canvas.CanvasAssignment;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Pattern;

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

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }

        @Override
        public Class<?> getColumnClass(int column) {
            return column == 3 ? BigDecimal.class : String.class;
        }
    };
    private final JTable table = new JTable(model);
    // The row filter is runtime UI state, matching JTable's transient sorter.
    private final transient TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);

    private JPanel searchControls() {
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        JLabel label = new JLabel("Search course or assignment:");
        label.setLabelFor(search);
        controls.add(label);
        controls.add(search);
        controls.add(count);
        return controls;
    }
    public AssignmentPanel() {
        super(new BorderLayout(5, 5));
        search.setName("assignmentSearch"); score.setName("whatIfScore");
        calculate.setName("calculateWhatIf"); result.setName("whatIfResult");
        table.setName("assignmentTable");
        table.setRowSorter(sorter);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        table.setRowHeight(24);
        table.getTableHeader().setReorderingAllowed(false);
        table.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) selectionChanged();
        });
        onTextChange(search, this::filter);
        onTextChange(score, () -> result.setText("Enter an expected score, then Calculate."));
        calculate.addActionListener(event -> calculate());
        score.addActionListener(event -> calculate());
        add(searchControls(), BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(scoreControls(), BorderLayout.SOUTH);
        setAssignments(List.of());
    }
    public void setAssignments(List<CanvasAssignment> assignments) {
        table.clearSelection();
        model.setRowCount(0);
        DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        for (CanvasAssignment item : assignments) {
            model.addRow(new Object[]{item.courseName(), item.name(),
                    item.dueDate() == null ? "No deadline" : format.format(item.dueDate()), item.pointsPossible()});
        }
        filter();
    }
    private JPanel scoreControls() {
        JPanel summary = new JPanel(new BorderLayout(5, 0));
        summary.add(selected, BorderLayout.CENTER);
        summary.add(possible, BorderLayout.EAST);
        JPanel input = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        JLabel label = new JLabel("What-if score:");
        label.setLabelFor(score);
        input.add(label);
        input.add(score);
        input.add(calculate);
        input.add(new JLabel("Assignment percentage only"));
        JPanel controls = new JPanel(new GridLayout(3, 1, 0, 4));
        controls.add(summary);
        controls.add(input);
        controls.add(result);
        return controls;
    }
    private void filter() {
        String query = search.getText().trim();
        sorter.setRowFilter(query.isEmpty() ? null : RowFilter.regexFilter("(?iu)" + Pattern.quote(query), 0, 1));
        count.setText("Showing " + table.getRowCount() + " of " + model.getRowCount());
        selectionChanged();
    }

    private void selectionChanged() {
        score.setText("");
        int row = selectedModelRow();
        selected.setText(row < 0 ? "Selected: none" : "Selected: " + model.getValueAt(row, 1));
        selected.setToolTipText(selected.getText());
        BigDecimal points = row < 0 ? null : (BigDecimal) model.getValueAt(row, 3);
        possible.setText(points == null ? "Max points: --" : "Max points: " + points.toPlainString());
        boolean usable = points != null && points.signum() > 0;
        score.setEnabled(usable); calculate.setEnabled(usable);
        result.setText(row < 0 ? "Select an assignment to try a what-if score."
                : usable ? "Enter an expected score, then Calculate." : "This assignment has no positive points possible.");
    }
    private void calculate() {
        int row = selectedModelRow();
        if (row < 0 || !calculate.isEnabled()) return;
        try {
            BigDecimal earned = new BigDecimal(score.getText().trim());
            BigDecimal points = (BigDecimal) model.getValueAt(row, 3);
            BigDecimal percentage = WhatIfScoreCalculator.percentage(earned, points);
            result.setText("What-if: " + earned.stripTrailingZeros().toPlainString() + " / "
                    + points.stripTrailingZeros().toPlainString() + " = "
                    + percentage.stripTrailingZeros().toPlainString() + "%");
        } catch (NumberFormatException exception) {
            result.setText("Enter a valid numeric score.");
        } catch (IllegalArgumentException exception) {
            result.setText(exception.getMessage());
        }
    }


    private int selectedModelRow() {
        int row = table.getSelectedRow();
        return row < 0 ? -1 : table.convertRowIndexToModel(row);
    }

    private static void onTextChange(JTextField field, Runnable action) {
        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                action.run();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                action.run();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                action.run();
            }
        });
    }
}
