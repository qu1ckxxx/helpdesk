package helpdesk.repository;

import helpdesk.model.Ticket;
import helpdesk.model.TicketPriority;
import helpdesk.model.TicketStatus;

import java.util.List;
import java.util.Optional;

public interface TicketRepository {
    Ticket save(Ticket ticket);

    Optional<Ticket> findById(long id);

    List<Ticket> findAll();

    List<Ticket> findByStatus(TicketStatus status);

    List<Ticket> findByStatusAndPriority(TicketStatus status, TicketPriority priority);

    List<Ticket> findByCustomerId(long customerId);

    List<Ticket> findOpenByAssigneeId(long assigneeId);

    void update(Ticket ticket);

    boolean deleteById(long id);

    void changeStatus(long ticketId, TicketStatus newStatus, Long changedByUserId);

    void assign(long ticketId, long assigneeId, Long changedByUserId);
}
