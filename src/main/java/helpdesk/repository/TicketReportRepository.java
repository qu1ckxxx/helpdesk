package helpdesk.repository;

import helpdesk.model.TicketDetails;
import helpdesk.model.TicketStatus;

import java.util.List;
import java.util.Optional;

public interface TicketReportRepository {
    List<TicketDetails> findAllDetails();

    Optional<TicketDetails> findDetailsById(long ticketId);

    List<StatusCount> countByStatus();

    List<CategoryCount> countByCategory();

    List<AgentWorkload> agentWorkload();

    record StatusCount(TicketStatus status, long count) { }

    record CategoryCount(String categoryName, long ticketCount) { }

    record AgentWorkload(String agentName, long activeTickets) { }
}
