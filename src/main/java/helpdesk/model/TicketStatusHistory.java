package helpdesk.model;

import java.time.LocalDateTime;

public class TicketStatusHistory {
    private long id;
    private final long ticketId;
    private final TicketStatus oldStatus;   
    private final TicketStatus newStatus;
    private final Long changedById;         
    private final LocalDateTime changedAt;

    public TicketStatusHistory(long ticketId, TicketStatus oldStatus,
                               TicketStatus newStatus, Long changedById) {
        this(0L, ticketId, oldStatus, newStatus, changedById, LocalDateTime.now());
    }

    public TicketStatusHistory(long id, long ticketId, TicketStatus oldStatus,
                               TicketStatus newStatus, Long changedById,
                               LocalDateTime changedAt) {
        this.id = id;
        this.ticketId = ticketId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.changedById = changedById;
        this.changedAt = changedAt;
    }

    public long getId() { return id; }
    public long getTicketId() { return ticketId; }
    public TicketStatus getOldStatus() { return oldStatus; }
    public TicketStatus getNewStatus() { return newStatus; }
    public Long getChangedById() { return changedById; }
    public LocalDateTime getChangedAt() { return changedAt; }

    public void setId(long id) { this.id = id; }

    @Override
    public String toString() {
        String from = (oldStatus == null) ? "—" : oldStatus.name();
        String who = (changedById == null) ? "система" : "user#" + changedById;
        return String.format("%s: %s → %s (изменил: %s)",
                changedAt.toLocalDate() + " " + changedAt.toLocalTime().withNano(0),
                from, newStatus, who);
    }
}
