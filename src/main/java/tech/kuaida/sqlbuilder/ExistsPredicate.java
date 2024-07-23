package tech.kuaida.sqlbuilder;

/**
 * Predicate used to add an EXISTS clause to the where clause.
 */
public interface ExistsPredicate extends Predicate {

    public ExistsPredicate and(Predicate predicate);

    public ExistsPredicate and(String predicate);

    public ExistsPredicate join(String join);

    public ExistsPredicate where(Predicate predicate);

    public ExistsPredicate where(String predicate);

}
