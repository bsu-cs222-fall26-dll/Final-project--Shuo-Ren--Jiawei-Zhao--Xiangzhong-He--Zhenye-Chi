package compass;

import compass.canvas.CanvasApiService;
import compass.canvas.CanvasAssignment;
import compass.canvas.CanvasCourse;
import compass.ui.AssignmentPanel;


import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import java.awt.*;
import java.io.IOException;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/** Basic read-only Canvas viewer. Credentials and results are not saved. */
public final class CourseCompassApp {
    private final JFrame frame = new JFrame("Course Compass - Canvas");
    private final JTextField urlField = new JTextField(32);
    private final JPasswordField tokenField = new JPasswordField(25);
    private final JButton connectButton = new JButton("Connect");
    private final JComboBox<String> courseBox = new JComboBox<>();
    private final JButton loadButton = new JButton("Load assignments");
    private final JLabel status = new JLabel("Enter your Canvas URL and token, then click Connect.");

    private final AssignmentPanel assignmentPanel = new AssignmentPanel();
    private List<CanvasCourse> courses = List.of();
    private CanvasApiService service;
    private SwingWorker<?, ?> worker;
    private boolean busy;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CourseCompassApp().show());
    }

    private void show() {
        JPanel root = new JPanel(new BorderLayout(5, 5));
        root.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel inputs = new JPanel(new GridLayout(2, 2, 5, 3));
        inputs.add(new JLabel("Canvas URL:"));
        inputs.add(new JLabel("Token:"));
        inputs.add(urlField);
        inputs.add(tokenField);
        JPanel connection = new JPanel(new BorderLayout(5, 0));
        connection.add(inputs, BorderLayout.CENTER);
        connection.add(connectButton, BorderLayout.EAST);

        JPanel selection = new JPanel(new BorderLayout(5, 0));
        selection.add(new JLabel("Course:"), BorderLayout.WEST);
        selection.add(courseBox, BorderLayout.CENTER);
        selection.add(loadButton, BorderLayout.EAST);
        JPanel top = new JPanel(new BorderLayout(0, 8));
        top.add(connection, BorderLayout.NORTH);
        top.add(selection, BorderLayout.SOUTH);
        root.add(top, BorderLayout.NORTH);

        root.add(assignmentPanel, BorderLayout.CENTER);
        root.add(status, BorderLayout.SOUTH);

        connectButton.addActionListener(event -> connect());
        loadButton.addActionListener(event -> loadAssignments());
        courseBox.addActionListener(event -> {
            assignmentPanel.setAssignments(List.of());
            updateControls();
        });
        listen(urlField, this::credentialsChanged);
        listen(tokenField, this::credentialsChanged);
        updateControls();

        frame.setContentPane(root);
        frame.setSize(900, 550);
        frame.setMinimumSize(new Dimension(700, 400));
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    private void connect() {
        char[] token = tokenField.getPassword();
        try {
            CanvasApiService candidate = new CanvasApiService(urlField.getText(), new String(token));
            service = null;
            runRequest("Connecting...", candidate::connect, data -> {
                service = candidate;
                courses = data.courses();
                courseBox.removeAllItems();
                for (CanvasCourse course : courses) {
                    courseBox.addItem(course.name() + " (" + course.term() + ")");
                }
                assignmentPanel.setAssignments(List.of());
                status.setText("Connected: " + data.account().name()
                        + " / ID " + data.account().userId()
                        + " / " + courses.size() + " courses. Choose a course.");
                if (courses.isEmpty()) status.setText("Connected. No accessible student courses found.");
                updateControls();
            });
        } catch (IllegalArgumentException exception) {
            showError(exception.getMessage());
        } finally {
            Arrays.fill(token, '\0');
        }
    }

    private void loadAssignments() {
        int index = courseBox.getSelectedIndex();
        if (service == null || index < 0 || index >= courses.size()) return;
        CanvasCourse course = courses.get(index);
        CanvasApiService connected = service;
        assignmentPanel.setAssignments(List.of());
        runRequest("Reading assignments...", () ->
                connected.fetchAssignmentsForCourses(List.of(course)), this::displayAssignments);
    }

    private void displayAssignments(List<CanvasAssignment> assignments) {
        assignmentPanel.setAssignments(assignments);
        status.setText("Loaded " + assignments.size() + " assignments. Display only; not saved.");
    }

    private void credentialsChanged() {
        service = null;
        courses = List.of();
        courseBox.removeAllItems();
        status.setText("URL or token changed. Connect again.");
        updateControls();
    }

    private void updateControls() {
        urlField.setEnabled(!busy);
        tokenField.setEnabled(!busy);
        connectButton.setEnabled(!busy);
        courseBox.setEnabled(!busy && service != null && !courses.isEmpty());
        loadButton.setEnabled(!busy && service != null && courseBox.getSelectedIndex() >= 0);
    }

    @FunctionalInterface
    private interface Request<T> {
        T run() throws Exception;
    }

    private <T> void runRequest(String message, Request<T> request, Consumer<T> onSuccess) {
        if (busy) return;
        busy = true;
        updateControls();
        status.setText(message);
        worker = new SwingWorker<T, Void>() {
            @Override protected T doInBackground() throws Exception {
                return request.run();
            }

            @Override protected void done() {
                if (!frame.isDisplayable()) return;
                busy = false;
                worker = null;
                updateControls();
                try {
                    onSuccess.accept(get());
                } catch (CancellationException ignored) {
                    status.setText("Request cancelled.");
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showError("Request interrupted. Please try again.");
                } catch (ExecutionException exception) {
                    Throwable cause = exception.getCause();
                    showError(cause instanceof IOException || cause instanceof IllegalArgumentException
                            ? cause.getMessage() : "Canvas request failed. Please try again.");
                }
            }
        };
        worker.execute();
    }

    private void showError(String message) {
        status.setText("Request failed.");
        JOptionPane.showMessageDialog(frame, message, "Course Compass", JOptionPane.ERROR_MESSAGE);
    }

    private static void listen(JTextField field, Runnable action) {
        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent event) { action.run(); }
            @Override public void removeUpdate(DocumentEvent event) { action.run(); }
            @Override public void changedUpdate(DocumentEvent event) { action.run(); }
        });
    }

}
