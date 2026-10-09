package helpdesk.repository.jdbc;

import helpdesk.model.TicketStatus;
import helpdesk.model.TicketStatusHistory;
import helpdesk.repository.StatusHistoryRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

import static helpdesk.repository.jdbc.JdbcSupport.*;

public class JdbcStatusHistoryRepository implements StatusHistoryRepository {

    @Override
    public List<TicketStatusHistory> findByTicketId(long ticketId) {
        String sql = """
                SELECT id, ticket_id, old_status, new_status, changed_by_id, changed_at
                FROM ticket_status_history
                WHERE ticket_id = ?
                ORDER BY changed_at, id
                """;
        return queryList(sql, "Ошибка чтения истории статусов",
                ps -> ps.setLong(1, ticketId),
                JdbcStatusHistoryRepository::mapRow);
    }

    void insert(Connection connection, TicketStatusHistory history) throws SQLException {
        String sql = """
                INSERT INTO ticket_status_history(ticket_id, old_status, new_status, changed_by_id)
                VALUES (?, ?::ticket_status, ?::ticket_status, ?)
                RETURNING id
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, history.getTicketId());
            if (history.getOldStatus() != null) {
                ps.setString(2, history.getOldStatus().name());
            } else {
                ps.setNull(2, Types.VARCHAR);
            }
            ps.setString(3, history.getNewStatus().name());
            setNullableLong(ps, 4, history.getChangedById());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    history.setId(rs.getLong("id"));
                }
            }
        }
    }

    private static TicketStatusHistory mapRow(ResultSet rs) throws SQLException {
        String oldStatus = rs.getString("old_status");
        return new TicketStatusHistory(
                rs.getLong("id"),
                rs.getLong("ticket_id"),
                oldStatus == null ? null : TicketStatus.valueOf(oldStatus),
                TicketStatus.valueOf(rs.getString("new_status")),
                getNullableLong(rs, "changed_by_id"),
                toLocalDateTime(rs.getTimestamp("changed_at"))
        );
    }
}
