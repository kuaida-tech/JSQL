package tech.kuaida.sqlbuilder;

import org.springframework.jdbc.core.PreparedStatementCreator;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Abstract base class of SQL creator classes.
 */
public abstract class AbstractSqlCreator implements PreparedStatementCreator, Serializable  {

    private int paramIndex;

    private ParameterizedPreparedStatementCreator ppsc = new ParameterizedPreparedStatementCreator();

    public AbstractSqlCreator() {

    }

    /**
     * Copy constructor. Used by cloneable creators.
     *
     * @param other
     *            AbstractSqlCreator being cloned.
     */
    public AbstractSqlCreator(AbstractSqlCreator other) {
        this.paramIndex = other.paramIndex;
        this.ppsc = other.ppsc.clone();
    }

    /**
     * @return a new parameter that is unique within this SelectCreator.
     */
    public String allocateParameter() {
        return "param" + paramIndex++;
    }

    public PreparedStatement createPreparedStatement(Connection conn) throws SQLException {
        return ppsc.setSql(getBuilder().toString()).createPreparedStatement(conn);
    }

    /**
     * @return  builder for this creator.
     */
    protected abstract AbstractSqlBuilder getBuilder();

    /**
     * @return  prepared statement creator for this creator.
     */
    protected ParameterizedPreparedStatementCreator getPreparedStatementCreator() {
        return ppsc;
    }

    /**
     * @param name key
     * @param value value
     * @return AbstractSqlCreator
     */
    public AbstractSqlCreator setParameter(String name, Object value) {
        ppsc.setParameter(name, value);
        return this;
    }

    @Override
    public String toString() {
        return ppsc.setSql(getBuilder().toString()).toString();
    }

}
