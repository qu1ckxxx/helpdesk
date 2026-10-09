package helpdesk.model;

import java.time.LocalDateTime;

public abstract class User {
    private long id;                      
    private final String name;
    private final String email;
    private final Long departmentId;      
    private final boolean active;
    private LocalDateTime createdAt;

    protected User(long id, String name, String email) {
        this(id, name, email, null, true, null);
    }

    protected User(long id, String name, String email,
                   Long departmentId, boolean active, LocalDateTime createdAt) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Имя пользователя не может быть пустым");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email не может быть пустым");
        }
        this.id = id;
        this.name = name.trim();
        this.email = email.trim();
        this.departmentId = departmentId;
        this.active = active;
        this.createdAt = createdAt;
    }

    public long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public Long getDepartmentId() { return departmentId; }
    public boolean isActive() { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void markSaved(long id, LocalDateTime createdAt) {
        this.id = id;
        this.createdAt = createdAt;
    }

    public abstract UserRole getRole();

    public abstract void performAction();

    @Override
    public String toString() {
        return String.format("#%d %s <%s> [%s]", id, name, email, getRole());
    }
}
