package helpdesk.repository.jdbc;

import helpdesk.model.Department;
import helpdesk.repository.DepartmentRepository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static helpdesk.repository.jdbc.JdbcSupport.*;

public class JdbcDepartmentRepository implements DepartmentRepository {

    private static final String COLUMNS = "id, name, description, created_at";

    @Override
    public List<Department> findAll() {
        return queryList("SELECT " + COLUMNS + " FROM departments ORDER BY id",
                "Ошибка чтения подразделений",
                NO_PARAMS,
                JdbcDepartmentRepository::mapRow);
    }

    @Override
    public Optional<Department> findById(long id) {
        return queryOne("SELECT " + COLUMNS + " FROM departments WHERE id = ?",
                "Ошибка поиска подразделения по id",
                ps -> ps.setLong(1, id),
                JdbcDepartmentRepository::mapRow);
    }

    private static Department mapRow(ResultSet rs) throws SQLException {
        return new Department(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("description"),
                toLocalDateTime(rs.getTimestamp("created_at")));
    }
}
