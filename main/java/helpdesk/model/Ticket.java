package helpdesk.model;

import java.time.LocalDateTime;

public class Ticket {
    private final long id;
    private final String title;
    private final String description;
    private TicketStatus status;
    private final TicketPriority priority;
    private final LocalDateTime createdAt;

    public Ticket(long id, String title, String description, TicketPriority priority) {
        // Валидация title (Самостоятельная работа №3)
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Название заявки не может быть пустым");
        }
        this.id = id;
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.status = TicketStatus.NEW;
        this.createdAt = LocalDateTime.now();
    }

    public long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public TicketStatus getStatus() { return status; }
    public TicketPriority getPriority() { return priority; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void startProcessing() {
        if (status != TicketStatus.NEW) {
            System.out.println("Ошибка: В работу можно взять только новую заявку!");
            return;
        }
        this.status = TicketStatus.IN_PROGRESS;
    }

    public void resolve() {
        if (status != TicketStatus.IN_PROGRESS) {
            System.out.println("Ошибка: Решить можно только заявку, находящуюся в работе!");
            return;
        }
        this.status = TicketStatus.RESOLVED;
    }

    public void close() {
        if (status != TicketStatus.RESOLVED) {
            System.out.println("Ошибка: Закрыть можно только решенную заявку!");
            return;
        }
        this.status = TicketStatus.CLOSED;
    }

    public void cancel() {
        if (status == TicketStatus.CLOSED) {
            System.out.println("Ошибка: Нельзя отменить уже закрытую заявку!");
            return;
        }
        this.status = TicketStatus.CANCELLED;
    }

    public void reopen() {
        if (status != TicketStatus.RESOLVED) {
            System.out.println("Ошибка: переоткрыть можно только решенную заявку!");
            return;
        }
        this.status = TicketStatus.REOPENED;
    }

    @Override
    public String toString() {
        return String.format("#%d [%s] %s | Приоритет: %s | Статус: %s", 
                id, createdAt.toLocalTime().withNano(0), title, priority, status);
    }
}