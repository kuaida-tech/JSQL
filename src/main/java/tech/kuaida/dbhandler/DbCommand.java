package tech.kuaida.dbhandler;

public class DbCommand {
    private String code;
    private String alias;
    private String function;
    private String sort;
    private String reference;
    private int joinType = 0; //1. join; 2. left join; 3. right join; 4.inner join; 5.full join
    private String joinFiled;
    private boolean ignore = false;
    private boolean groupBy = false;

    public DbCommand() {

    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getAlias() {
        if (alias != null)
            return alias;
        else
            return code;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getFunction() {
        return function;
    }

    public void setFunction(String function) {
        this.function = function;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }

    public int getJoinType() {
        return joinType;
    }

    public void setJoinType(int joinType) {
        this.joinType = joinType;
    }

    public String getJoinFiled() {
        return joinFiled;
    }

    public void setJoinFiled(String joinFiled) {
        this.joinFiled = joinFiled;
    }

    public boolean isIgnore() {
        return ignore;
    }

    public void setIgnore(boolean ignore) {
        this.ignore = ignore;
    }

    public boolean isGroupBy() {
        return groupBy;
    }

    public void setGroupBy(boolean groupBy) {
        this.groupBy = groupBy;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }
}
