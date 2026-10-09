package helpdesk.repository.jdbc;

import helpdesk.config.DatabaseConfig;
import helpdesk.model.Ticket;
import helpdesk.model.TicketPriority;
import helpdesk.model.TicketStatus;
import helpdesk.model.TicketStatusHistory;
import helpdesk.repository.DataAccessException;
import helpdesk.repository.TicketRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static helpdesk.repository.jdbc.JdbcSupport.*;

public class JdbcTicketRepository implements TicketRepository {

    private static final String COLUMNS =
            "id, title, description, status, priority, customer_id, assignee_id, "
                    + "category_id, created_at, updated_at, closed_at";

    private final JdbcStatusHistoryRepository historyRepository = new JdbcStatusHistoryRepository();

    @Override
    public Ticket save(Ticket ticket) {
        String sql = """
                INSERT INTO tickets(title, description, status, priority,
                                    customer_id, assignee_id, category_id)
                VALUES (?, ?, ?::ticket_status, ?::ticket_priority, ?, ?, ?)
                RETURNING id, created_at, updated_at
                """;
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setString(1, ticket.getTitle());
            ps.setString(2, ticket.getDescription());
            ps.setString(3, ticket.getStatus().name());
            ps.setString(4, ticket.getPriority().name());
            ps.setLong(5, ticket.getCustomerId());
            setNullableLong(ps, 6, ticket.getAssigneeId());
            setNullableLong(ps, 7, ticket.getCategoryId());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ticket.markSaved(
                            rs.getLong("id"),
                            toLocalDateTime(rs.getTimestamp("created_at")),
                            toLocalDateTime(rs.getTimestamp("updated_at")));
                }
            }
            return ticket;
        } catch (SQLException e) {
            throw new DataAccessException("Не удалось сохранить заявку", e);
        }
    }

    @Override
    public Optional<Ticket> findById(long id) {
        return queryOne("SELECT " + COLUMNS + " FROM tickets WHERE id = ?",
                "Ошибка поиска заявки по id",
                ps -> ps.setLong(1, id),
                JdbcTicketRepository::mapRow);
    }

    @Override
    public List<Ticket> findAll() {
        return queryList("SELECT " + COLUMNS + " FROM tickets ORDER BY id",
                "Ошибка чтения заявок",
                NO_PARAMS,
                JdbcTicketRepository::mapRow);
    }

    @Override
    public List<Ticket> findByStatus(TicketStatus status) {
        String sql = "SELECT " + COLUMNS + " FROM tickets "
                + "WHERE status = ?::ticket_status ORDER BY created_at DESC";
        return queryList(sql, "Ошибка поиска заявок по статусу",
                ps -> ps.setString(1, status.name()),
                JdbcTicketRepository::mapRow);
    }

    @Override
    public List<Ticket> findByStatusAndPriority(TicketStatus status, TicketPriority priority) {
        String sql = "SELECT " + COLUMNS + " FROM tickets "
                + "WHERE status = ?::ticket_status AND priority = ?::ticket_priority "
                + "ORDER BY created_at DESC";
        return queryList(sql, "Ошибка поиска заявок по статусу и приоритету",
                ps -> {
                    ps.setString(1, status.name());
                    ps.setString(2, priority.name());
                },
                JdbcTicketRepository::mapRow);
    }

    @Override
    public List<Ticket> findByCustomerId(long customerId) {
        String sql = "SELECT " + COLUMNS + " FROM tickets "
                + "WHERE customer_id = ? ORDER BY created_at DESC";
        return queryList(sql, "Ошибка поиска заявок клиента",
                ps -> ps.setLong(1, customerId),
                JdbcTicketRepository::mapRow);
    }

    @Override
    public List<Ticket> findOpenByAssigneeId(long assigneeId) {
        String sql = "SELECT " + COLUMNS + " FROM tickets "
                + "WHERE assignee_id = ? AND status NOT IN ('CLOSED', 'CANCELLED') "
                + "ORDER BY created_at DESC";
        return queryList(sql, "Ошибка поиска заявок исполнителя",
                ps -> ps.setLong(1, assigneeId),
                JdbcTicketRepository::mapRow);
    }

    @Override
    public void update(Ticket ticket) {
        String sql = """
                UPDATE tickets
                SET title = ?, description = ?, priority = ?::ticket_priority,
                    category_id = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                RETURNING updated_at
                """;
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setString(1, ticket.getTitle());
            ps.setString(2, ticket.getDescription());
            ps.setString(3, ticket.getPriority().name());
            setNullableLong(ps, 4, ticket.getCategoryId());
            ps.setLong(5, ticket.getId());

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalArgumentException("Заявка не найдена: " + ticket.getId());
                }
                ticket.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Ошибка обновления заявки", e);
        }
    }

    @Override
    public boolean deleteById(long id) {
        String sql = "DELETE FROM tickets WHERE id = ?";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;   // комментарии и история удалятся каскадом
        } catch (SQLException e) {
            throw new DataAccessException("Ошибка удаления заявки", e);
        }
    }

    @Override
    public void changeStatus(long ticketId, TicketStatus newStatus, Long changedByUserId) {
        inTransaction("Ошибка изменения статуса заявки #" + ticketId, connection -> {
            TicketStatus oldStatus = lockAndReadStatus(connection, ticketId);
            if (!oldStatus.canTransitionTo(newStatus)) {
                throw new IllegalStateException("Недопустимый переход: " + oldStatus + " -> " + newStatus);
            }
            updateStatus(connection, ticketId, newStatus);
            historyRepository.insert(connection,
                    new TicketStatusHistory(ticketId, oldStatus, newStatus, changedByUserId));
            return null;
        });
    }

    @Override
    public void assign(long ticketId, long assigneeId, Long changedByUserId) {
        inTransaction("Ошибка назначения исполнителя заявки #" + ticketId, connection -> {
            TicketStatus oldStatus = lockAndReadStatus(connection, ticketId);

            if (oldStatus != TicketStatus.NEW && oldStatus != TicketStatus.IN_PROGRESS) {
                throw new IllegalStateException(
                        "Назначить исполнителя можно только для заявки NEW или IN_PROGRESS, сейчас: " + oldStatus);
            }
            TicketStatus newStatus = (oldStatus == TicketStatus.NEW) ? TicketStatus.IN_PROGRESS : oldStatus;

            String sql = """
                    UPDATE tickets
                    SET assignee_id = ?, status = ?::ticket_status, updated_at = CURRENT_TIMESTAMP
                    WHERE id = ?
                    """;
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setLong(1, assigneeId);
                ps.setString(2, newStatus.name());
                ps.setLong(3, ticketId);
                ps.executeUpdate();
            }

            if (newStatus != oldStatus) {
                historyRepository.insert(connection,
                        new TicketStatusHistory(ticketId, oldStatus, newStatus, changedByUserId));
            }
            return null;
        });
    }

    private static TicketStatus lockAndReadStatus(Connection connection, long ticketId) throws SQLException {
        String sql = "SELECT status FROM tickets WHERE id = ? FOR UPDATE";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, ticketId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalArgumentException("Заявка не найдена: " + ticketId);
                }
                return TicketStatus.valueOf(rs.getString("status"));
            }
        }
    }

    private static void updateStatus(Connection connection, long ticketId, TicketStatus newStatus)
            throws SQLException {
        String sql = """
                UPDATE tickets
                SET status = ?::ticket_status,
                    updated_at = CURRENT_TIMESTAMP,
                    closed_at = CASE
                        WHEN ?::ticket_status = 'CLOSED' THEN CURRENT_TIMESTAMP
                        ELSE closed_at
                    END
                WHERE id = ?
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, newStatus.name());
            ps.setString(2, newStatus.name());
            ps.setLong(3, ticketId);
            ps.executeUpdate();
        }
    }

    private static Ticket mapRow(ResultSet rs) throws SQLException {
        return new Ticket(
                rs.getLong("id"),
                rs.getString("title"),
                rs.getString("description"),
                TicketStatus.valueOf(rs.getString("status")),
                TicketPriority.valueOf(rs.getString("priority")),
                rs.getLong("customer_id"),
                getNullableLong(rs, "assignee_id"),
                getNullableLong(rs, "category_id"),
                toLocalDateTime(rs.getTimestamp("created_at")),
                toLocalDateTime(rs.getTimestamp("updated_at")),
                toLocalDateTime(rs.getTimestamp("closed_at"))
        );
    }
}
