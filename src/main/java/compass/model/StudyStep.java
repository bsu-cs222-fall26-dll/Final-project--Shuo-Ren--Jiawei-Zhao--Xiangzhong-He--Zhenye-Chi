package compass.model;

public final class StudyStep {

    private final long id;
    private final long assignmentId;
    private final String text;
    private boolean completed;

    public StudyStep(long id, long assignmentId, String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException(
                    "Study step text must not be blank."
            );
        }

        this.id = id;
        this.assignmentId = assignmentId;
        this.text = text;
        this.completed = false;
    }

    public long getId() {
        return id;
    }

    public long getAssignmentId() {
        return assignmentId;
    }

    public String getText() {
        return text;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
}