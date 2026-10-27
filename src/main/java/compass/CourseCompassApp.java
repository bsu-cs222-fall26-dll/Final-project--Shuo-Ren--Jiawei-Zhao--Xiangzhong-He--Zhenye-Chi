package compass;

import javax.swing.*;

public class CourseCompassApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Course Compass");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(850,650);
            frame.setVisible(true);
        });
    }
}