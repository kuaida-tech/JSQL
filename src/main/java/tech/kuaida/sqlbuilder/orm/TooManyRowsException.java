package tech.kuaida.sqlbuilder.orm;

import tech.kuaida.sqlbuilder.SelectCreator;

/**
 * Exception thrown when more than one record is returned in response to a query
 * expected to produce only one result
 */
public class TooManyRowsException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    TooManyRowsException(int count, SelectCreator creator) {
        super("Expected single result, found " + count + " rows for this query: " + creator);
    }

}
