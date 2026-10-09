package helpdesk.repository.memory;

import helpdesk.model.Ticket;
import helpdesk.model.TicketPriority;
import helpdesk.model.TicketStatus;
import helpdesk.model.TicketStatusHistory;
import helpdesk.repository.StatusHistoryRepository;
import helpdesk.repository.TicketRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryTicketRepository implements TicketRepository, StatusHistoryRepository {
    private final List<Ticket> tickets = new ArrayList<>();
    private final List<TicketStatusHistory> history = new ArrayList<>();
    private final AtomicLong nextId = new AtomicLong(1);
    private final AtomicLong nextHistoryId = new AtomicLong(1);

    @Override
    public Ticket save(Ticket ticket) {
        LocalDateTime now = LocalDateTime.now();
        ticket.markSaved(nextId.getAndIncrement(), now, now);   // id выдаём сами, как это делает БД
        tickets.add(ticket);
        return ticket;
    }

    @Override
    public Optional<Ticket> findById(long id) {
        return tickets.stream().filter(t -> t.getId() == id).findFirst();
    }

    @Override
    public List<Ticket> findAll() {
        return new ArrayList<>(tickets);
    }

    @Override
    public List<Ticket> findByStatus(TicketStatus status) {
        return tickets.stream().filter(t -> t.getStatus() == status).toList();
    }

    @Override
    public List<Ticket> findByStatusAndPriority(TicketStatus status, TicketPriority priority) {
        return tickets.stream()
                .filter(t -> t.getStatus() == status && t.getPriority() == priority)
                .toList();
    }

    @Override
    public List<Ticket> findByCustomerId(long customerId) {
        return tickets.stream().filter(t -> t.getCustomerId() == customerId).toList();
    }

    @Override
    public List<Ticket> findOpenByAssigneeId(long assigneeId) {
        return tickets.stream()
                .filter(t -> t.getAssigneeId() != null && t.getAssigneeId() == assigneeId)
                .filter(t -> t.getStatus() != TicketStatus.CLOSED && t.getStatus() != TicketStatus.CANCELLED)
                .toList();
    }

    @Override
    public void update(Ticket ticket) {
        if (findById(ticket.getId()).isEmpty()) {
            throw new IllegalArgumentException("Заявка не найдена: " + ticket.getId());
        }
        ticket.setUpdatedAt(LocalDateTime.now());   
    }

    @Override
    public boolean deleteById(long id) {
        history.removeIf(h -> h.getTicketId() == id);
        return tickets.removeIf(t -> t.getId() == id);
    }

    @Override
    public void changeStatus(long ticketId, TicketStatus newStatus, Long changedByUserId) {
        Ticket ticket = require(ticketId);
        TicketStatus oldStatus = ticket.getStatus();
        ticket.changeStatus(newStatus);                       
        addHistory(ticketId, oldStatus, newStatus, changedByUserId);
    }

    @Override
    public void assign(long ticketId, long assigneeId, Long changedByUserId) {
        Ticket ticket = require(ticketId);
        TicketStatus oldStatus = ticket.getStatus();
        ticket.assignTo(assigneeId);
        if (ticket.getStatus() != oldStatus) {
            addHistory(ticketId, oldStatus, ticket.getStatus(), changedByUserId);
        }
    }

    @Override
    public List<TicketStatusHistory> findByTicketId(long ticketId) {
        return history.stream().filter(h -> h.getTicketId() == ticketId).toList();
    }

    private Ticket require(long ticketId) {
        return findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Заявка не найдена: " + ticketId));
    }

    private void addHistory(long ticketId, TicketStatus oldStatus, TicketStatus newStatus, Long userId) {
        TicketStatusHistory record = new TicketStatusHistory(ticketId, oldStatus, newStatus, userId);
        record.setId(nextHistoryId.getAndIncrement());
        history.add(record);
    }
}
