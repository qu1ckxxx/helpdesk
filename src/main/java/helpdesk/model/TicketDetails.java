package helpdesk.model;

import java.time.LocalDateTime;

public record TicketDetails(long id,
                            String title,
                            TicketStatus status,
                            TicketPriority priority,
                            String categoryName,
                            String customerName,
                            String assigneeName,
                            LocalDateTime createdAt) {

    @Override
    public String toString() {
        return String.format("#%d | %s | %s / %s | клиент: %s | исполнитель: %s | категория: %s | создана: %s",
                id, title, status, priority, customerName,
                assigneeName == null ? "не назначен" : assigneeName,
                categoryName == null ? "—" : categoryName,
                createdAt.toLocalDate() + " " + createdAt.toLocalTime().withNano(0));
    }
}
