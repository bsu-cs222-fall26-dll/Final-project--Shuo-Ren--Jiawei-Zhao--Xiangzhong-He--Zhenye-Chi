package compass;

import compass.model.Assignment;
import compass.model.Planner;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.time.LocalDateTime;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class CourseCompassApp {

    private static final Planner planner = new Planner();

    private static JTable assignmentTable;
    private static DefaultTableModel assignmentTableModel;
    private static JLabel statusLabel;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(CourseCompassApp::buildMainWindow);
    }

    private static void buildMainWindow() {
        JFrame mainFrame = new JFrame("Course Compass");
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setSize(900, 650);
        mainFrame.setLocationRelativeTo(null);

        JPanel rootPanel = new JPanel(new BorderLayout(8,8));
        rootPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        JPanel topBarPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnAddManual = new JButton("Add Manual Assignment");
        JButton btnImportCanvas = new JButton("Import From Canvas");
        JTextField searchInput = new JTextField(22);
        searchInput.setToolTipText("Search by course / assignment name");

        topBarPanel.add(btnAddManual);
        topBarPanel.add(btnImportCanvas);
        topBarPanel.add(new JLabel("Search:"));
        topBarPanel.add(searchInput);
        rootPanel.add(topBarPanel, BorderLayout.NORTH);

        assignmentTableModel = new DefaultTableModel(new Object[]{"ID","Name","Course","Due Date"},0);
        assignmentTable = new JTable(assignmentTableModel);
        JScrollPane tableScrollPane = new JScrollPane(assignmentTable);
        rootPanel.add(tableScrollPane, BorderLayout.CENTER);

        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusLabel = new JLabel("Ready");
        statusBar.add(statusLabel);
        rootPanel.add(statusBar, BorderLayout.SOUTH);

        mainFrame.add(rootPanel);
        mainFrame.setVisible(true);

        btnAddManual.addActionListener(CourseCompassApp::onAddManualAssignment);
        refreshAssignmentTable();
    }

    private static void onAddManualAssignment(ActionEvent e) {
        String name = JOptionPane.showInputDialog("Enter assignment name:");
        if(name == null || name.isBlank()){
            statusLabel.setText("Add cancelled or empty name");
            return;
        }
        String course = JOptionPane.showInputDialog("Enter course name:");
        if(course == null || course.isBlank()){
            statusLabel.setText("Add cancelled or empty course");
            return;
        }

        planner.createManualAssignment(name.trim(), course.trim(), LocalDateTime.now());
        statusLabel.setText("Manual assignment added");
        refreshAssignmentTable();
    }

    private static void refreshAssignmentTable() {
        assignmentTableModel.setRowCount(0);
        List<Assignment> allAssignments = planner.getAllAssignments();
        for(Assignment a : allAssignments){
            assignmentTableModel.addRow(new Object[]{
                    a.getId(),
                    a.getName(),
                    a.getCourseName(),
                    a.getDueDate()
            });
        }
    }
}