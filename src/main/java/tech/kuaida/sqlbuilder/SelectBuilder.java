package tech.kuaida.sqlbuilder;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import tech.kuaida.dbhandler.DbCommand;
import tech.kuaida.sqlbuilder.orm.ClassParser;
import tech.kuaida.sqlbuilder.orm.FieldInfo;
import tech.kuaida.utils.NcStringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Tool for programmatically constructing SQL select statements. This class aims
 * to simplify the task of juggling commas and SQL keywords when building SQL
 * statements from scratch, but doesn't attempt to do much beyond that. Here are
 * some relatively complex examples:
 *
 * <pre>
 * String sql = new SelectBuilder()
 * .column(&quot;e.id&quot;)
 * .column(&quot;e.name as empname&quot;)
 * .column(&quot;d.name as deptname&quot;)
 * .column(&quot;e.salary&quot;)
 * .from((&quot;Employee e&quot;)
 * .join(&quot;Department d on e.dept_id = d.id&quot;)
 * .where(&quot;e.salary &gt; 100000&quot;)
 * .orderBy(&quot;e.salary desc&quot;)
 * .toString();
 * </pre>
 *
 * <pre>
 * String sql = new SelectBuilder()
 *         .column(&quot;d.id&quot;)
 *         .column(&quot;d.name&quot;)
 *         .column(&quot;sum(e.salary) as total&quot;)
 *         .from(&quot;Department d&quot;)
 *         .join(&quot;Employee e on e.dept_id = d.id&quot;)
 *         .groupBy(&quot;d.id&quot;)
 *         .groupBy(&quot;d.name&quot;)
 *         .having(&quot;total &gt; 1000000&quot;).toString();
 * </pre>
 *
 * Note that the methods can be called in any order. This is handy when a base
 * class wants to create a simple query but allow subclasses to augment it.
 *
 * It's similar to the Squiggle SQL library
 * (http://code.google.com/p/squiggle-sql/), but makes fewer assumptions about
 * the internal structure of the SQL statement, which I think makes for simpler,
 * cleaner code. For example, in Squiggle you would write...
 *
 * <pre>
 * select.addCriteria(new MatchCriteria(orders, &quot;status&quot;, MatchCriteria.EQUALS, &quot;processed&quot;));
 * </pre>
 *
 * With SelectBuilder, we assume you know how to write SQL expressions, so
 * instead you would write...
 *
 * <pre>
 * select.where(&quot;status = 'processed'&quot;);
 * </pre>
 */
public class SelectBuilder extends AbstractSqlBuilder implements Cloneable, Serializable {

    private static final long serialVersionUID = 1;

    private boolean distinct;

    private List<Object> columns = new ArrayList<Object>();

    private List<String> tables = new ArrayList<String>();

    private List<String> joins = new ArrayList<String>();

    private List<String> leftJoins = new ArrayList<String>();

    private List<String> rightJoins = new ArrayList<String>();

    private List<String> innerJoins = new ArrayList<String>();

    private List<String> fullJoins = new ArrayList<String>();

    private List<String> wheres = new ArrayList<String>();

    private List<String> ands = new ArrayList<>();

    private List<String> ors = new ArrayList<>();

    private List<String> groupBys = new ArrayList<String>();

    private List<String> havings = new ArrayList<String>();

    private List<SelectBuilder> unions = new ArrayList<SelectBuilder>();

    private List<String> orderBys = new ArrayList<String>();

    private int limit = -1;

    private int offset = -1;

    private boolean forUpdate;

    private boolean noWait;

    public SelectBuilder() {

    }

    public SelectBuilder(String table) {
        tables.add(table);
    }

    /**
     * Copy constructor. Used by {@link #clone()}.
     *
     * @param other
     *              SelectBuilder being cloned.
     */
    protected SelectBuilder(SelectBuilder other) {

        this.distinct = other.distinct;
        this.forUpdate = other.forUpdate;
        this.noWait = other.noWait;

        for (Object column : other.columns) {
            if (column instanceof SubSelectBuilder) {
                this.columns.add(((SubSelectBuilder) column).clone());
            } else {
                this.columns.add(column);
            }
        }

        this.tables.addAll(other.tables);
        this.joins.addAll(other.joins);
        this.leftJoins.addAll(other.leftJoins);
        this.rightJoins.addAll(other.rightJoins);
        this.innerJoins.addAll(other.innerJoins);
        this.fullJoins.addAll(other.fullJoins);
        this.wheres.addAll(other.wheres);
        this.ands.addAll(other.ands);
        this.ors.addAll(other.ors);
        this.groupBys.addAll(other.groupBys);
        this.havings.addAll(other.havings);

        for (SelectBuilder sb : other.unions) {
            this.unions.add(sb.clone());
        }

        this.orderBys.addAll(other.orderBys);
    }

    public SelectBuilder column(DbCommand command, String name, String alias) {
        if (StringUtils.isNotEmpty(command.getFunction())) {
            String parameters = "";

            if (command.getParameters() != null) {
                for (int i = 0; i < command.getParameters().size(); i++) {
                    parameters = parameters + "," + command.getParameters().get(i);
                }
            }

            columns.add(command.getFunction() + "(" + name + parameters + ") AS \"" + alias + "\"");
        } else if (command.isIgnore() == false) {
            columns.add(name + " AS \"" + alias + "\"");
        }

        if (command.isGroupBy()) {
            groupBys.add(name);
        }

        if (StringUtils.isNotEmpty(command.getSort())) {
            if ("ASC".equals(command.getSort().toUpperCase())) {
                orderBy(name, true);
            } else if ("DESC".equals(command.getSort().toUpperCase())) {
                orderBy(name, false);
            }
        }

        return this;
    }

    public SelectBuilder column(String name) {
        columns.add(name);
        return this;
    }

    public SelectBuilder column(SubSelectBuilder subSelect) {
        columns.add(subSelect);
        return this;
    }

    public SelectBuilder column(String name, boolean groupBy) {
        columns.add(name);
        if (groupBy) {
            groupBys.add(name);
        }
        return this;
    }

    public SelectBuilder limit(int limit, int offset) {
        if (this.limit == -1)
            this.limit = limit;
        if (this.offset == -1)
            this.offset = offset;
        return this;
    }

    public SelectBuilder limit(int limit) {
        return limit(limit, 0);
    }

    @Override
    public SelectBuilder clone() {
        return new SelectBuilder(this);
    }

    public SelectBuilder distinct() {
        this.distinct = true;
        return this;
    }

    public SelectBuilder forUpdate() {
        forUpdate = true;
        return this;
    }

    public SelectBuilder from(String table) {
        tables.add(table);
        return this;
    }

    public SelectBuilder removeFrom(String table) {
        tables.remove(table);
        return this;
    }

    public List<SelectBuilder> getUnions() {
        return unions;
    }

    public SelectBuilder groupBy(String expr) {
        groupBys.add(expr);
        return this;
    }

    public SelectBuilder having(String expr) {
        havings.add(expr);
        return this;
    }

    public SelectBuilder join(String join) {
        joins.add(join);
        return this;
    }

    public SelectBuilder leftJoin(String join, String expr) {
        leftJoins.add(join + " ON " + expr);
        return this;
    }

    public SelectBuilder rightJoin(String join, String expr) {
        rightJoins.add(join + " ON " + expr);
        return this;
    }

    public SelectBuilder innerJoin(String join, String expr) {
        innerJoins.add(join + " ON " + expr);
        return this;
    }

    public SelectBuilder fullJoin(String join, String expr) {
        fullJoins.add(join + " ON " + expr);
        return this;
    }

    public SelectBuilder noWait() {
        if (!forUpdate) {
            throw new RuntimeException("noWait without forUpdate cannot be called");
        }
        noWait = true;
        return this;
    }

    public SelectBuilder orderBy(String name) {
        orderBys.add(name);
        return this;
    }

    /**
     * Adds an ORDER BY item with a direction indicator.
     *
     * @param name
     *                  Name of the column by which to sort.
     * @param ascending
     *                  If true, specifies the direction "asc", otherwise, specifies
     *                  the direction "desc".
     * @return SelectBuilder
     */
    public SelectBuilder orderBy(String name, boolean ascending) {
        if (ascending) {
            orderBys.add(name + " asc");
        } else {
            orderBys.add(name + " desc");
        }
        return this;
    }

    public void parseJsonObject(Class clazz, String table, String alias, String join, JSONObject jsonObject) {
        ClassParser classParser = new ClassParser(clazz);

        if (!jsonObject.containsKey("id")) {
            jsonObject.put("id", JSON.parseObject("{\"field\": true}"));
        }

        // join字段不为空则表示当前为多对多查询
        if (join != null) {
            this.leftJoin(join, classParser.getTableName() + ".id = " + join + "." + classParser.getTableName());

            List<String> keys = new ArrayList<>();
            Iterator<String> keyIter = jsonObject.keySet().iterator();

            while (keyIter.hasNext()) {
                keys.add(keyIter.next());
            }

            for (int i = 0; i < keys.size(); i++) {
                String key = keys.get(i);
                if (jsonObject.getJSONObject(key).containsKey("where")) {
                    String _whereCondition = jsonObject.getJSONObject(key).getString("where");
                    this.where(join + "." + key + _whereCondition);
                    jsonObject.remove(key);
                }
            }
        }

        Iterator<String> keyIterator = jsonObject.keySet().iterator();
        while (keyIterator.hasNext()) {
            String fieldName = keyIterator.next();
            String columnName = classParser.getColumnName(fieldName);

            Object object = jsonObject.get(fieldName);

            // 当前字段为数组形态
            if (object instanceof JSONArray) {
                JSONArray array = jsonObject.getJSONArray(fieldName);
                if (array.size() > 0) {
                    JSONObject queryJson = new JSONObject();
                    queryJson.put("relationType", classParser.getFieldRelationType(fieldName));
                    queryJson.put("class", classParser.getFieldClass(fieldName));
                    queryJson.put("mappedBy", classParser.getFieldMappedBy(fieldName));
                    queryJson.put("data", array.getJSONObject(0));

                    if (FieldInfo.RELATION_MANYTOMANY.equals(classParser.getFieldRelationType(fieldName))) {
                        queryJson.put("join", classParser.getFieldRelationTable(fieldName));
                    }

                    if (FieldInfo.RELATION_ONETOMANY == classParser.getFieldRelationType(fieldName)) {
                        this.column(("'$query=" + queryJson.toJSONString() + "' AS " + "'" + alias + "." + fieldName
                                + "'"));
                    } else if (FieldInfo.RELATION_MANYTOMANY == classParser.getFieldRelationType(fieldName)) {
                        this.column(("'$query=" + queryJson.toJSONString() + "' AS " + "'" + alias + "." + fieldName
                                + "'"));
                    }
                }
            } else if (object instanceof JSONObject) {
                JSONObject query = jsonObject.getJSONObject(fieldName);

                boolean fieldExist = query.containsKey("field");
                boolean aliasExist = query.containsKey("alias");
                boolean whereExist = query.containsKey("where");
                boolean ascExist = query.containsKey("asc");

                if (fieldExist || whereExist || ascExist) {
                    if (fieldExist && query.getBooleanValue("field")) {
                        this.column(table + "." + columnName + " AS " + "'" + alias + "."
                                + (aliasExist ? query.getString("alias") : fieldName) + "'");
                    }

                    if (whereExist) {
                        String where = query.getString("where");

                        Pattern pattern = Pattern.compile("=|!=|<|<=|>|>=|like|not like|is null|is not null|in");

                        if (pattern.matcher(where).lookingAt()) {
                            this.where(table + "." + columnName + " " + where);
                        } else {
                            String type = classParser.getFieldType(fieldName);
                            switch (type) {
                                case "String": {
                                    if ("[".equals(where.substring(0, 1))
                                            && "]".equals(where.substring(where.length() - 1, where.length()))) {
                                        this.where(table + "." + columnName + " in "
                                                + where.replace("[", "(").replace("]", ")"));
                                    } else {
                                        String[] keywords = where.split(" ");
                                        int count = keywords.length;
                                        if (keywords[keywords.length - 1].equals("$or")) {
                                            count = count - 1;
                                        }

                                        for (int i = 0; i < count; i++) {
                                            if (!keywords[i].trim().equals("")) {
                                                this.where(table + "." + columnName + " like '%" + keywords[i] + "%'"
                                                        + (count < keywords.length ? " " + keywords[keywords.length - 1]
                                                        : ""));
                                            }
                                        }
                                    }
                                }
                                break;
                                case "Integer":
                                case "Float": {
                                    this.where(table + "." + columnName + " = " + NcStringUtils.toUnderlineCase(where));
                                }
                                break;
                                case "Date": {
                                    if ("[".equals(where.substring(0, 1))
                                            && "]".equals(where.substring(where.length() - 1, where.length()))) {
                                        JSONArray jsonArray = JSON.parseArray(where);
                                        if (jsonArray.getString(0).equals(jsonArray.getString(1))) {
                                            this.where(
                                                    table + "." + columnName + " = '" + jsonArray.getString(0) + "'");
                                        } else {
                                            this.where(table + "." + columnName + " > '" + jsonArray.getString(0)
                                                    + "' AND " + columnName + " < '" + jsonArray.getString(1) + "'");
                                        }
                                    } else {
                                        this.where(table + "." + columnName + " = '"
                                                + NcStringUtils.toUnderlineCase(where) + "'");
                                    }
                                }
                                break;
                                default: {
                                    this.where(table + "." + columnName + " like '%"
                                            + NcStringUtils.toUnderlineCase(where) + "%'");
                                }
                                break;
                            }
                        }
                    }

                    if (ascExist) {
                        this.orderBy(table + "." + columnName, query.getBooleanValue("asc"));
                    }
                } else {
                    this.leftJoin(classParser.getFieldTableName(fieldName) + " " + fieldName,
                            fieldName + ".id=" + classParser.getTableName() + "." + columnName);

                    String _alias = alias + "." + fieldName;

                    this.parseJsonObject(classParser.getFieldClass(fieldName), fieldName, _alias, null, query);
                }
            }
        }
    }

    public String toCountString() {
        StringBuilder sql = new StringBuilder("select ");

        if (distinct) {
            sql.append("distinct ");
        }

        if (havings.size() > 0) {
            appendList(sql, columns, "", ", ");
        } else {
            sql.append("count(1)");
        }

        appendList(sql, tables, " from ", ", ");
        appendList(sql, joins, " join ", " join ");
        appendList(sql, leftJoins, " left join ", " left join ");
        appendList(sql, rightJoins, " right join ", " left join ");
        appendList(sql, innerJoins, " inner join ", " inner join ");
        appendList(sql, fullJoins, " full join ", " full join ");
        appendWhereList(sql, wheres, " where ", " and ");
        appendWhereList(sql, ands, (wheres.size() > 0 ? null : " where "), " and ");
        appendWhereList(sql, ors, (wheres.size() > 0 || ands.size() > 0 ? null : " where "), " or ");
        appendList(sql, groupBys, " group by ", ", ");
        appendList(sql, havings, " having ", " and ");
        appendList(sql, unions, " union ", " union ");
        appendList(sql, orderBys, " order by ", ", ");

        if (forUpdate) {
            sql.append(" for update");
            if (noWait) {
                sql.append(" nowait");
            }
        }

        if (groupBys.size() > 0 || havings.size() > 0) {
            return "SELECT COUNT(1) FROM ( " + sql.toString() + ") AS count";
        }

        return sql.toString();
    }

    @Override
    public String toString() {

        StringBuilder sql = new StringBuilder("select ");

        if (distinct) {
            sql.append("distinct ");
        }

        if (columns.size() == 0) {
            sql.append("*");
        } else {
            appendList(sql, columns, "", ", ");
        }

        appendList(sql, tables, " from ", ", ");
        appendList(sql, joins, " join ", " join ");
        appendList(sql, leftJoins, " left join ", " left join ");
        appendList(sql, rightJoins, " right join ", " left join ");
        appendList(sql, innerJoins, " inner join ", " inner join ");
        appendList(sql, fullJoins, " full join ", " full join ");
        appendWhereList(sql, wheres, " where ", " and ");
        appendWhereList(sql, ands, (wheres.size() > 0 ? null : " where "), " and ");
        appendWhereList(sql, ors, (wheres.size() > 0 || ands.size() > 0 ? null : " where "), " or ");
        appendList(sql, groupBys, " group by ", ", ");
        appendList(sql, havings, " having ", " and ");
        appendList(sql, unions, " union ", " union ");
        appendList(sql, orderBys, " order by ", ", ");

        if (forUpdate) {
            sql.append(" for update");
            if (noWait) {
                sql.append(" nowait");
            }
        }

        if (limit >= 0)
            sql.append(" limit " + limit);
        if (offset > 0)
            sql.append(", " + offset);

        return sql.toString();
    }

    /**
     * Adds a "union" select builder. The generated SQL will union this query
     * with the result of the main query. The provided builder must have the
     * same columns as the parent select builder and must not use "order by" or
     * "for update".
     *
     * @param unionBuilder a "union" select builder
     * @return SelectBuilder
     */
    public SelectBuilder union(SelectBuilder unionBuilder) {
        unions.add(unionBuilder);
        return this;
    }

    /*
     * @param expr a expression
     * @return SelectBuilder
     */
    public SelectBuilder where(String expr) {
        wheres.add(expr);
        return this;
    }

    /*
     * @param expr a expression
     * @return SelectBuilder
     */
    public SelectBuilder and(String expr) {
        ands.add(expr);
        return this;
    }

    /*
     * @param expr a expression
     * @return SelectBuilder
     */
    public SelectBuilder or(String expr) {
        ors.add(expr);
        return this;
    }

    /*
     * @return List<Object>
     */
    public List<Object> getColumns() {
        return columns;
    }
}
