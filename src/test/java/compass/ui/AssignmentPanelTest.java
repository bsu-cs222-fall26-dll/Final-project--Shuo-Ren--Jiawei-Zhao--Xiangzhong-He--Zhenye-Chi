package compass.ui;

import compass.canvas.CanvasAssignment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.Component;
import java.awt.Container;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AssignmentPanelTest {
    private AssignmentPanel panel;
    private JTable table;
    private JTextField search;
    private JTextField score;
    private JButton calculate;
    private JLabel result;
    @BeforeEach
    void loadAssignmentsOnTheSwingThread() throws Exception {
        onEdt(() -> {
            panel = new AssignmentPanel();
            panel.setAssignments(List.of(item(1, "Essay", "English", "100"),
                    item(2, "[Lab] Setup", "CS222", "50"), item(3, "Practice", "CS222", "0")));
            table = (JTable) named(panel, "assignmentTable");
            search = (JTextField) named(panel, "assignmentSearch");
            score = (JTextField) named(panel, "whatIfScore");
            calculate = (JButton) named(panel, "calculateWhatIf");
            result = (JLabel) named(panel, "whatIfResult");
            return null;
        });
    }

    @Test void searchesCourseNamesIgnoringCase() throws Exception {
        assertEquals(2, onEdt(() -> { search.setText("cs222"); return table.getRowCount(); }));
    }

    @Test void searchesAssignmentNamesIgnoringCase() throws Exception {
        assertEquals("Essay", onEdt(() -> { search.setText("ESS"); return table.getValueAt(0, 1); }));
    }

    @Test void treatsSearchAsLiteralTextRatherThanARegularExpression() throws Exception {
        assertEquals(1, onEdt(() -> { search.setText("[Lab]"); return table.getRowCount(); }));
    }

    @Test void clearingTheSearchRestoresAllAssignments() throws Exception {
        assertEquals(3, onEdt(() -> { search.setText("English"); search.setText(""); return table.getRowCount(); }));
    }

    @Test void aSearchWithNoMatchesShowsAnEmptyTable() throws Exception {
        assertEquals(0, onEdt(() -> { search.setText("not found"); return table.getRowCount(); }));
    }

    private CanvasAssignment item(long id, String name, String course, String points) {
        return new CanvasAssignment(id, id, name, course, null, new BigDecimal(points));
    }

    private Component named(Container parent, String name) {
        for (Component child : parent.getComponents()) {
            if (name.equals(child.getName())) return child;
            if (child instanceof Container container) {
                Component found = named(container, name);
                if (found != null) return found;
            }
        }
        return null;
    }

    private <T> T onEdt(Callable<T> action) throws Exception {
        FutureTask<T> task = new FutureTask<>(action);
        SwingUtilities.invokeAndWait(task);
        return task.get();
    }
}
