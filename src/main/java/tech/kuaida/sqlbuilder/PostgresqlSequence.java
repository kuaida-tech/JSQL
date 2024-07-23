package tech.kuaida.sqlbuilder;

import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

public class PostgresqlSequence implements Supplier<Integer> {

    private JdbcTemplate jdbcTemplate;

    private String sequenceName;

    public PostgresqlSequence(DataSource dataSource, String sequenceName) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.sequenceName = sequenceName;
    }

    public Integer get() {
        return jdbcTemplate.queryForObject("select nextval(?)", Integer.class, sequenceName);
    }

}
