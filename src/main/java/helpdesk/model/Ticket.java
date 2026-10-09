package helpdesk.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class Ticket {
    private long id;                          
    private String title;
    private String description;
    private TicketPriority priority;
    private TicketStatus status;
    private final long customerId;
    private Long assigneeId;                  
    private final Long categoryId;            
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime closedAt;           

    public Ticket(String title, String description, TicketPriority priority,
                  long customerId, Long categoryId) {
        this(0L, title, description, TicketStatus.NEW, priority,
                customerId, null, categoryId,
                LocalDateTime.now(), LocalDateTime.now(), null);
    }

    public Ticket(String title, String description, TicketPriority priority, long customerId) {
        this(title, description, priority, customerId, null);
    }

    public Ticket(long id, String title, String description,
                  TicketStatus status, TicketPriority priority,
                  long customerId, Long assigneeId, Long categoryId,
                  LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime closedAt) {
        this.id = id;
        this.title = requireTitle(title);
        this.description = requireDescription(description);
        this.status = Objects.requireNonNull(status, "status");
        this.priority = Objects.requireNonNull(priority, "priority");
        this.customerId = customerId;
        this.assigneeId = assigneeId;
        this.categoryId = categoryId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.closedAt = closedAt;
    }

    private static String requireTitle(String title) {
        if (title == null || title.trim().length() < 3) {
            throw new IllegalArgumentException("Тема заявки должна содержать минимум 3 символа");
        }
        return title.trim();
    }

    private static String requireDescription(String description) {
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Описание заявки не может быть пустым");
        }
        return description;
    }

    public long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public TicketStatus getStatus() { return status; }
    public TicketPriority getPriority() { return priority; }
    public long getCustomerId() { return customerId; }
    public Long getAssigneeId() { return assigneeId; }
    public Long getCategoryId() { return categoryId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public LocalDateTime getClosedAt() { return closedAt; }

    public void markSaved(long id, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void edit(String title, String description, TicketPriority priority) {
        this.title = requireTitle(title);
        this.description = requireDescription(description);
        this.priority = Objects.requireNonNull(priority, "priority");
        touch();
    }

    public void changeStatus(TicketStatus next) {
        if (!status.canTransitionTo(next)) {
            throw new IllegalStateException("Недопустимый переход: " + status + " -> " + next);
        }
        this.status = next;
        touch();
        if (next == TicketStatus.CLOSED) {
            this.closedAt = LocalDateTime.now();
        }
    }

    public void assignTo(long agentId) {
        if (status != TicketStatus.NEW && status != TicketStatus.IN_PROGRESS) {
            throw new IllegalStateException(
                    "Назначить исполнителя можно только для заявки NEW или IN_PROGRESS, сейчас: " + status);
        }
        this.assigneeId = agentId;
        if (status == TicketStatus.NEW) {
            this.status = TicketStatus.IN_PROGRESS;
        }
        touch();
    }

    public void startProcessing() { changeStatus(TicketStatus.IN_PROGRESS); }
    public void resolve()         { changeStatus(TicketStatus.RESOLVED); }
    public void close()           { changeStatus(TicketStatus.CLOSED); }
    public void cancel()          { changeStatus(TicketStatus.CANCELLED); }
    public void reopen()          { changeStatus(TicketStatus.IN_PROGRESS); }

    private void touch() {
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return String.format("#%d [%s] %s | Приоритет: %s | Статус: %s",
                id, createdAt == null ? "—" : createdAt.toLocalTime().withNano(0),
                title, priority, status);
    }
}
