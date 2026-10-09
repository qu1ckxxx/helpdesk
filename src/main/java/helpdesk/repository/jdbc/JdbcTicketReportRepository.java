package helpdesk.repository.jdbc;

import helpdesk.model.TicketDetails;
import helpdesk.model.TicketPriority;
import helpdesk.model.TicketStatus;
import helpdesk.repository.TicketReportRepository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static helpdesk.repository.jdbc.JdbcSupport.*;

public class JdbcTicketReportRepository implements TicketReportRepository {

    private static final String DETAILS_SELECT = """
            SELECT t.id, t.title, t.status, t.priority,
                   c.name        AS category_name,
                   customer.name AS customer_name,
                   assignee.name AS assignee_name,
                   t.created_at
            FROM tickets t
            JOIN users customer      ON customer.id = t.customer_id
            LEFT JOIN users assignee ON assignee.id = t.assignee_id
            LEFT JOIN categories c   ON c.id = t.category_id
            """;

    @Override
    public List<TicketDetails> findAllDetails() {
        return queryList(DETAILS_SELECT + " ORDER BY t.created_at DESC, t.id DESC",
                "Ошибка чтения заявок с JOIN",
                NO_PARAMS,
                JdbcTicketReportRepository::mapDetails);
    }

    @Override
    public Optional<TicketDetails> findDetailsById(long ticketId) {
        return queryOne(DETAILS_SELECT + " WHERE t.id = ?",
                "Ошибка чтения заявки с JOIN",
                ps -> ps.setLong(1, ticketId),
                JdbcTicketReportRepository::mapDetails);
    }

    @Override
    public List<StatusCount> countByStatus() {
        String sql = """
                SELECT status, COUNT(*) AS ticket_count
                FROM tickets
                GROUP BY status
                ORDER BY status
                """;
        return queryList(sql, "Ошибка отчёта по статусам", NO_PARAMS,
                rs -> new StatusCount(
                        TicketStatus.valueOf(rs.getString("status")),
                        rs.getLong("ticket_count")));
    }

    @Override
    public List<CategoryCount> countByCategory() {
        String sql = """
                SELECT c.name, COUNT(t.id) AS ticket_count
                FROM categories c
                LEFT JOIN tickets t ON t.category_id = c.id
                GROUP BY c.id, c.name
                ORDER BY ticket_count DESC, c.name
                """;
        return queryList(sql, "Ошибка отчёта по категориям", NO_PARAMS,
                rs -> new CategoryCount(rs.getString("name"), rs.getLong("ticket_count")));
    }

    @Override
    public List<AgentWorkload> agentWorkload() {
        String sql = """
                SELECT u.name,
                       COUNT(t.id) FILTER (
                           WHERE t.status IN ('NEW', 'IN_PROGRESS')
                       ) AS active_tickets
                FROM users u
                LEFT JOIN tickets t ON t.assignee_id = u.id
                WHERE u.role = 'SUPPORT_AGENT'
                GROUP BY u.id, u.name
                ORDER BY active_tickets DESC, u.name
                """;
        return queryList(sql, "Ошибка отчёта о нагрузке сотрудников", NO_PARAMS,
                rs -> new AgentWorkload(rs.getString("name"), rs.getLong("active_tickets")));
    }

    private static TicketDetails mapDetails(ResultSet rs) throws SQLException {
        return new TicketDetails(
                rs.getLong("id"),
                rs.getString("title"),
                TicketStatus.valueOf(rs.getString("status")),
                TicketPriority.valueOf(rs.getString("priority")),
                rs.getString("category_name"),
                rs.getString("customer_name"),
                rs.getString("assignee_name"),
                toLocalDateTime(rs.getTimestamp("created_at")));
    }
}
