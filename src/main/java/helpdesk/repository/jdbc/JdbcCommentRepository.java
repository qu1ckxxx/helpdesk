package helpdesk.repository.jdbc;

import helpdesk.config.DatabaseConfig;
import helpdesk.model.TicketComment;
import helpdesk.model.UserRole;
import helpdesk.repository.CommentRepository;
import helpdesk.repository.DataAccessException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static helpdesk.repository.jdbc.JdbcSupport.*;

public class JdbcCommentRepository implements CommentRepository {

    private static final String SELECT_WITH_AUTHOR = """
            SELECT c.id, c.ticket_id, c.author_id, c.text, c.internal, c.created_at,
                   u.name AS author_name,
                   u.role AS author_role
            FROM ticket_comments c
            JOIN users u ON u.id = c.author_id
            WHERE c.ticket_id = ?
            """;

    private static final String ORDER = " ORDER BY c.created_at, c.id";

    @Override
    public TicketComment add(TicketComment comment) {
        String sql = """
                INSERT INTO ticket_comments(ticket_id, author_id, text, internal)
                VALUES (?, ?, ?, ?)
                RETURNING id, created_at
                """;
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setLong(1, comment.getTicketId());
            ps.setLong(2, comment.getAuthorId());
            ps.setString(3, comment.getText());
            ps.setBoolean(4, comment.isInternal());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    comment.markSaved(rs.getLong("id"), toLocalDateTime(rs.getTimestamp("created_at")));
                }
            }
            return comment;
        } catch (SQLException e) {
            throw new DataAccessException("Не удалось сохранить комментарий", e);
        }
    }

    @Override
    public List<TicketComment> findByTicketId(long ticketId) {
        return queryList(SELECT_WITH_AUTHOR + ORDER,
                "Ошибка чтения комментариев",
                ps -> ps.setLong(1, ticketId),
                JdbcCommentRepository::mapRow);
    }

    @Override
    public List<TicketComment> findPublicByTicketId(long ticketId) {
        return queryList(SELECT_WITH_AUTHOR + " AND c.internal = FALSE" + ORDER,
                "Ошибка чтения внешних комментариев",
                ps -> ps.setLong(1, ticketId),
                JdbcCommentRepository::mapRow);
    }

    private static TicketComment mapRow(ResultSet rs) throws SQLException {
        return new TicketComment(
                rs.getLong("id"),
                rs.getLong("ticket_id"),
                rs.getLong("author_id"),
                rs.getString("text"),
                rs.getBoolean("internal"),
                toLocalDateTime(rs.getTimestamp("created_at")),
                rs.getString("author_name"),
                UserRole.valueOf(rs.getString("author_role")));
    }
}
