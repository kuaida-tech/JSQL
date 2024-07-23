package tech.kuaida.sqlbuilder.orm;

public class FieldInfo {
    public static final String RELATION_NONE = "none";
    public static final String RELATION_ONETOONE = "onttoone";
    public static final String RELATION_ONETOMANY = "onetomany";
    public static final String RELATION_MANYTOONE = "manytoone";
    public static final String RELATION_MANYTOMANY = "manytomany";


    private String tableName;
    private String classType;
    private String columnName;
    private String fieldName;
    private String fieldType;
    private String relationType;
    private String relationTable;
    private String mappedBy;

    public FieldInfo() {}

    public FieldInfo(String tableName, String classType, String columnName, String fieldName, String fieldType, String relationType, String relationTable, String mappedBy) {
        this.tableName = tableName;
        this.classType = classType;
        this.columnName = columnName;
        this.fieldName = fieldName;
        this.fieldType = fieldType;
        this.relationType = relationType;
        this.relationTable = relationTable;
        this.mappedBy = mappedBy;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getClassType() {
        return classType;
    }

    public void setClassType(String classType) {
        this.classType = classType;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getFieldType() {
        return fieldType;
    }

    public void setFieldType(String fieldType) {
        this.fieldType = fieldType;
    }

    public String getRelationType() {
        return relationType;
    }

    public void setRelationType(String relationType) {
        this.relationType = relationType;
    }

    public String getRelationTable() {
        return relationTable;
    }

    public void setRelationTable(String relationTable) {
        this.relationTable = relationTable;
    }

    public String getMappedBy() {
        return mappedBy;
    }

    public void setMappedBy(String mappedBy) {
        this.mappedBy = mappedBy;
    }
}
