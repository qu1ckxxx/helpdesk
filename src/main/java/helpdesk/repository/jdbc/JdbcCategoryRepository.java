package helpdesk.repository.jdbc;

import helpdesk.model.Category;
import helpdesk.model.TicketPriority;
import helpdesk.repository.CategoryRepository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static helpdesk.repository.jdbc.JdbcSupport.*;

public class JdbcCategoryRepository implements CategoryRepository {

    private static final String COLUMNS = "id, name, description, default_priority, active";

    @Override
    public List<Category> findAll() {
        return queryList("SELECT " + COLUMNS + " FROM categories ORDER BY id",
                "Ошибка чтения категорий",
                NO_PARAMS,
                JdbcCategoryRepository::mapRow);
    }

    @Override
    public Optional<Category> findById(long id) {
        return queryOne("SELECT " + COLUMNS + " FROM categories WHERE id = ?",
                "Ошибка поиска категории по id",
                ps -> ps.setLong(1, id),
                JdbcCategoryRepository::mapRow);
    }

    private static Category mapRow(ResultSet rs) throws SQLException {
        return new Category(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("description"),
                TicketPriority.valueOf(rs.getString("default_priority")),
                rs.getBoolean("active"));
    }
}
