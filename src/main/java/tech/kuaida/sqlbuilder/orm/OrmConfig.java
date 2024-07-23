package tech.kuaida.sqlbuilder.orm;

import tech.kuaida.sqlbuilder.Dialect;
import tech.kuaida.sqlbuilder.Supplier;

import javax.sql.DataSource;

/**
 * Configuration of the ORM system. Each mapping must be constructed with one of
 * these objects.
 */
public class OrmConfig {

    private DataSource dataSource;

    private Dialect dialect;

    private ConverterFactory converterFactory = new DefaultConverterFactory();

    public OrmConfig(DataSource dataSource, Dialect dialect) {
        super();
        this.dataSource = dataSource;
        this.dialect = dialect;
    }

    public ConverterFactory getConverterFactory() {
        return converterFactory;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public Dialect getDialect() {
        return dialect;
    }

    public Supplier<Integer> getSequence(String sequenceName) {
        return dialect.getSequence(dataSource, sequenceName);
    }

    public OrmConfig setConverterFactory(ConverterFactory converterFactory) {
        this.converterFactory = converterFactory;
        return this;
    }

}
