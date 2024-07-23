package tech.kuaida.sqlbuilder;

/**
 * Interface used by suppliers of primary keys to DAOs.
 */
public interface Supplier<T> {

    public T get();

}
