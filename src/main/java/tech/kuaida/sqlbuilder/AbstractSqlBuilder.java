package tech.kuaida.sqlbuilder;

import java.util.List;

/**
 * Abstract base class for builders. Contains helper methods.
 */
public abstract class AbstractSqlBuilder {

    /**
     * Constructs a list of items with given separators.
     *
     * @param sql
     *            StringBuilder to which the constructed string will be
     *            appended.
     * @param list
     *            List of objects (usually strings) to join.
     * @param init
     *            String to be added to the start of the list, before any of the
     *            items.
     * @param sep
     *            Separator string to be added between items in the list.
     */
    protected void appendList(StringBuilder sql, List<?> list, String init, String sep) {

        boolean first = true;

        for (Object s : list) {
            if (first) {
                sql.append(init);
            } else {
                sql.append(sep);
            }
            sql.append(s);
            first = false;
        }
    }

    protected  void appendWhereList(StringBuilder sql, List<?> list, String init, String sep) {
        boolean first = true;

        for (Object s : list) {
            String str = (String)s;
            if (first) {
                if (init == null) {
                    sql.append(sep);
                } else {
                    sql.append(init);
                }
            } else {
                if (str.endsWith("$or")) {
                    sql.append(" or ");
                    str = str.replace("$or", "");
                } else {
                    sql.append(sep);
                }
            }
            sql.append(str.replace("$or", ""));
            first = false;
        }
    }

}
