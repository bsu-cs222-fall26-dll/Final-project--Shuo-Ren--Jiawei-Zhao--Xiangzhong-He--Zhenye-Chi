package compass;

import javax.swing.*;
import java.awt.*;

public class CourseCompassApp {
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

        JTable assignmentTable = new JTable();
        JScrollPane tableScrollPane = new JScrollPane(assignmentTable);
        rootPanel.add(tableScrollPane, BorderLayout.CENTER);

        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel statusLabel = new JLabel("Ready");
        statusBar.add(statusLabel);
        rootPanel.add(statusBar, BorderLayout.SOUTH);

        mainFrame.add(rootPanel);
        mainFrame.setVisible(true);
    }
}