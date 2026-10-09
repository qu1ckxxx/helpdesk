package helpdesk.model;

import java.time.LocalDateTime;

public class TicketComment {
    private long id;
    private final long ticketId;
    private final long authorId;
    private final String text;
    private final boolean internal;
    private LocalDateTime createdAt;
    private final String authorName;        
    private final UserRole authorRole;      

    public TicketComment(long ticketId, long authorId, String text, boolean internal) {
        this(0L, ticketId, authorId, text, internal, null, null, null);
    }

    public TicketComment(long id, long ticketId, long authorId, String text, boolean internal,
                         LocalDateTime createdAt, String authorName, UserRole authorRole) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Комментарий не может быть пустым");
        }
        this.id = id;
        this.ticketId = ticketId;
        this.authorId = authorId;
        this.text = text.trim();
        this.internal = internal;
        this.createdAt = createdAt;
        this.authorName = authorName;
        this.authorRole = authorRole;
    }

    public long getId() { return id; }
    public long getTicketId() { return ticketId; }
    public long getAuthorId() { return authorId; }
    public String getText() { return text; }
    public boolean isInternal() { return internal; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getAuthorName() { return authorName; }
    public UserRole getAuthorRole() { return authorRole; }

    public void markSaved(long id, LocalDateTime createdAt) {
        this.id = id;
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        String author = authorName == null ? "user#" + authorId : authorName + " (" + authorRole + ")";
        return String.format("[%s] %s%s: %s",
                createdAt == null ? "—" : createdAt.toLocalTime().withNano(0),
                internal ? "[внутренний] " : "", author, text);
    }
}
