package tech.kuaida.sqlbuilder;

import javax.sql.DataSource;
import java.io.Serializable;

/**
 * Dialect for PostgreSQL.
 */
public class PostgresqlDialect implements Dialect, Serializable {

    private static final long serialVersionUID = 1;

    public String createCountSelect(String sql) {
        return "select count(*) from (" + sql + ") a";
    }

    public String createPageSelect(String sql, int limit, int offset) {
        return String.format("%s limit %d offset %d", sql, limit, offset);
    }

    @Override
    public Supplier<Integer> getSequence(DataSource dataSource, String sequenceName) {
        return new PostgresqlSequence(dataSource, sequenceName);
    }
}
