package helpdesk.repository.jdbc;

import helpdesk.config.DatabaseConfig;
import helpdesk.repository.DataAccessException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class JdbcSupport {

    private JdbcSupport() {
    }

    @FunctionalInterface
    interface ParamSetter {
        void set(PreparedStatement ps) throws SQLException;
    }

    @FunctionalInterface
    interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    @FunctionalInterface
    interface SqlWork<T> {
        T apply(Connection connection) throws SQLException;
    }

    static final ParamSetter NO_PARAMS = ps -> { };

    static <T> List<T> queryList(String sql, String errorMessage,
                                 ParamSetter params, RowMapper<T> mapper) {
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            params.set(ps);
            try (ResultSet rs = ps.executeQuery()) {
                List<T> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(mapper.map(rs));
                }
                return result;
            }
        } catch (SQLException e) {
            throw new DataAccessException(errorMessage, e);
        }
    }

    static <T> Optional<T> queryOne(String sql, String errorMessage,
                                    ParamSetter params, RowMapper<T> mapper) {
        List<T> rows = queryList(sql, errorMessage, params, mapper);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    static <T> T inTransaction(String errorMessage, SqlWork<T> work) {
        try (Connection connection = DatabaseConfig.getConnection()) {
            connection.setAutoCommit(false);
            try {
                T result = work.apply(connection);
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackError) {
                    e.addSuppressed(rollbackError);
                }
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException(errorMessage, e);
        }
    }

    static LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    static void setNullableLong(PreparedStatement ps, int index, Long value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.BIGINT);
        } else {
            ps.setLong(index, value);
        }
    }

    static Long getNullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }
}
