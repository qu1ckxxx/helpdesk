package helpdesk.repository.jdbc;

import helpdesk.config.DatabaseConfig;
import helpdesk.model.Administrator;
import helpdesk.model.Customer;
import helpdesk.model.SupportAgent;
import helpdesk.model.User;
import helpdesk.model.UserRole;
import helpdesk.repository.DataAccessException;
import helpdesk.repository.UserRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static helpdesk.repository.jdbc.JdbcSupport.*;

public class JdbcUserRepository implements UserRepository {

    private static final String COLUMNS = "id, name, email, role, department_id, active, created_at";

    private static final String UNIQUE_VIOLATION = "23505";

    @Override
    public User save(User user) {
        String sql = """
                INSERT INTO users(name, email, role, department_id, active)
                VALUES (?, ?, ?::user_role, ?, ?)
                RETURNING id, created_at
                """;
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setString(1, user.getName());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getRole().name());
            setNullableLong(ps, 4, user.getDepartmentId());
            ps.setBoolean(5, user.isActive());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    user.markSaved(rs.getLong("id"), toLocalDateTime(rs.getTimestamp("created_at")));
                }
            }
            return user;
        } catch (SQLException e) {
            if (UNIQUE_VIOLATION.equals(e.getSQLState())) {
                throw new IllegalArgumentException(
                        "Пользователь с email " + user.getEmail() + " уже существует", e);
            }
            throw new DataAccessException("Не удалось сохранить пользователя", e);
        }
    }

    @Override
    public Optional<User> findById(long id) {
        return queryOne("SELECT " + COLUMNS + " FROM users WHERE id = ?",
                "Ошибка поиска пользователя по id",
                ps -> ps.setLong(1, id),
                JdbcUserRepository::mapRow);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return queryOne("SELECT " + COLUMNS + " FROM users WHERE email = ?",
                "Ошибка поиска пользователя по email",
                ps -> ps.setString(1, email),
                JdbcUserRepository::mapRow);
    }

    @Override
    public List<User> findAll() {
        return queryList("SELECT " + COLUMNS + " FROM users ORDER BY id",
                "Ошибка чтения пользователей",
                NO_PARAMS,
                JdbcUserRepository::mapRow);
    }

    private static User mapRow(ResultSet rs) throws SQLException {
        UserRole role = UserRole.valueOf(rs.getString("role"));
        long id = rs.getLong("id");
        String name = rs.getString("name");
        String email = rs.getString("email");
        Long departmentId = getNullableLong(rs, "department_id");
        boolean active = rs.getBoolean("active");
        LocalDateTime createdAt = toLocalDateTime(rs.getTimestamp("created_at"));

        return switch (role) {
            case CUSTOMER -> new Customer(id, name, email, departmentId, active, createdAt);
            case SUPPORT_AGENT -> new SupportAgent(id, name, email, departmentId, active, createdAt);
            case ADMIN -> new Administrator(id, name, email, departmentId, active, createdAt);
        };
    }
}
