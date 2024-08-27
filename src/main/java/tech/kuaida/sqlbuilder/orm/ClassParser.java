package tech.kuaida.sqlbuilder.orm;

import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class ClassParser {

    private Map<String, FieldInfo> fieldMapping;
    private String tableName;

    public ClassParser(Class clazz) {
        fieldMapping = new HashMap<>();

        javax.persistence.Table tableAnnotation = (javax.persistence.Table)clazz.getAnnotation(javax.persistence.Table.class);
        this.tableName = tableAnnotation.name();

        Field[] fields = clazz.getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            Field field = fields[i];

            FieldInfo fieldInfo = new FieldInfo();

            if ("id".equals(field.getName())) {
                fieldMapping.put("id", new FieldInfo(null, null, "id", "id", "String", FieldInfo.RELATION_NONE, null, null));
            } else {
                javax.persistence.Column columnAnnotation = field.getAnnotation(javax.persistence.Column.class);

                if (columnAnnotation != null) {
                    fieldInfo.setRelationType(FieldInfo.RELATION_NONE);
                    fieldInfo.setColumnName(columnAnnotation.name());
                } else {
                    javax.persistence.OneToOne oneToOneAnnotation = field.getAnnotation(javax.persistence.OneToOne.class);
                    javax.persistence.OneToMany oneToManyAnnotation = field.getAnnotation(javax.persistence.OneToMany.class);
                    javax.persistence.ManyToOne manyToOneAnnotation = field.getAnnotation(javax.persistence.ManyToOne.class);
                    javax.persistence.ManyToMany manyToManyAnnotation = field.getAnnotation(javax.persistence.ManyToMany.class);

                    if (oneToOneAnnotation != null) {
                        fieldInfo.setRelationType(FieldInfo.RELATION_ONETOONE);
                    } else if (oneToManyAnnotation != null) {
                        fieldInfo.setRelationType(FieldInfo.RELATION_ONETOMANY);
                    } else if (manyToOneAnnotation != null) {
                        fieldInfo.setRelationType(FieldInfo.RELATION_MANYTOONE);
                    } else if (manyToManyAnnotation != null) {
                        fieldInfo.setRelationType(FieldInfo.RELATION_MANYTOMANY);
                    }

                    if (fieldInfo.getRelationType() == FieldInfo.RELATION_ONETOONE || fieldInfo.getRelationType() == FieldInfo.RELATION_MANYTOONE) {
                        javax.persistence.JoinColumn joinColumnAnnotation = field.getAnnotation(javax.persistence.JoinColumn.class);
                        fieldInfo.setColumnName(joinColumnAnnotation.name());
                        try {
                            if (fieldInfo.getRelationType() == FieldInfo.RELATION_ONETOONE || fieldInfo.getRelationType() == FieldInfo.RELATION_MANYTOONE) {
                                fieldInfo.setClassType(field.getType().getName());
                            } else if (fieldInfo.getRelationType() == FieldInfo.RELATION_ONETOMANY) {
                                ParameterizedType listGenericType = (ParameterizedType) field.getGenericType();
                                Type[] listActualTypeArguments = listGenericType.getActualTypeArguments();
                                fieldInfo.setClassType(listActualTypeArguments[0].getTypeName());
                            }
                            Class fieldClass = Class.forName(field.getType().getName(), true, Thread.currentThread().getContextClassLoader());
                            javax.persistence.Table fieldTableAnnotation = (javax.persistence.Table)fieldClass.getAnnotation(javax.persistence.Table.class);
                            fieldInfo.setTableName(fieldTableAnnotation.name());
                        } catch (ClassNotFoundException e) {
                            e.printStackTrace();
                        }
                    } else if (fieldInfo.getRelationType() == FieldInfo.RELATION_ONETOMANY) {
                        String mappedBy = oneToManyAnnotation.mappedBy();
                        fieldInfo.setMappedBy(mappedBy);

                        ParameterizedType listGenericType = (ParameterizedType) field.getGenericType();
                        Type[] listActualTypeArguments = listGenericType.getActualTypeArguments();
                        fieldInfo.setClassType(listActualTypeArguments[0].getTypeName());

                        Class fieldClass = null;
                        try {
                            fieldClass = Class.forName(listActualTypeArguments[0].getTypeName(), true, Thread.currentThread().getContextClassLoader());
                        } catch (ClassNotFoundException e) {
                            e.printStackTrace();
                        }
                        javax.persistence.Table fieldTableAnnotation = (javax.persistence.Table)fieldClass.getAnnotation(javax.persistence.Table.class);
                        fieldInfo.setTableName(fieldTableAnnotation.name());
                    } else if (fieldInfo.getRelationType() == FieldInfo.RELATION_MANYTOMANY) {
                        ParameterizedType listGenericType = (ParameterizedType) field.getGenericType();
                        Type[] listActualTypeArguments = listGenericType.getActualTypeArguments();
                        fieldInfo.setClassType(listActualTypeArguments[0].getTypeName());

                        if (StringUtils.isNotEmpty(manyToManyAnnotation.mappedBy())) {
                            Class relationClass = manyToManyAnnotation.targetEntity();
                            String mappedBy = manyToManyAnnotation.mappedBy();
                            Field[] relationClassFields = relationClass.getDeclaredFields();
                            for (int i1 = 0; i1 < relationClassFields.length; i1++) {
                                if (mappedBy.toLowerCase().equals(relationClassFields[i1].getName().toLowerCase())) {
                                    javax.persistence.JoinTable joinTableAnnotation = relationClassFields[i1].getAnnotation(javax.persistence.JoinTable.class);
                                    fieldInfo.setRelationTable(joinTableAnnotation.name());
                                    break;
                                }
                            }
                        } else {
                            javax.persistence.JoinTable joinTableAnnotation = field.getAnnotation(javax.persistence.JoinTable.class);
                            fieldInfo.setRelationTable(joinTableAnnotation.name());
                        }

                        try {
                            Class fieldClass = Class.forName(fieldInfo.getClassType(), true, Thread.currentThread().getContextClassLoader());
                            javax.persistence.Table fieldTableAnnotation = (javax.persistence.Table)fieldClass.getAnnotation(javax.persistence.Table.class);
                            fieldInfo.setTableName(fieldTableAnnotation.name());
                        } catch (ClassNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                }

                fieldInfo.setFieldType(fields[i].getType().getSimpleName());
                fieldInfo.setFieldName(fields[i].getName());

                fieldMapping.put(fields[i].getName(), fieldInfo);
            }
        }
    }

    public String getTableName() {
        return tableName;
    }

    public FieldInfo getFieldInfo(String fieldName) {
        if (fieldMapping.containsKey(fieldName) && fieldMapping.get(fieldName) != null) {
            return fieldMapping.get(fieldName);
        } else {
            return null;
        }
    }

    public String getColumnName(String fieldName) {
        if (fieldMapping.containsKey(fieldName) && fieldMapping.get(fieldName) != null) {
            return fieldMapping.get(fieldName).getColumnName();
        } else {
            return null;
        }
    }

    public String getFieldType(String fieldName) {
        if (fieldMapping.containsKey(fieldName) && fieldMapping.get(fieldName) != null) {
            return fieldMapping.get(fieldName).getFieldType();
        } else {
            return null;
        }
    }

    public String getFieldTableName(String fieldName) {
        if (fieldMapping.containsKey(fieldName) && fieldMapping.get(fieldName) != null) {
            return fieldMapping.get(fieldName).getTableName();
        } else {
            return null;
        }
    }

    public Class getFieldClass(String fieldName) {
        if (fieldMapping.containsKey(fieldName) && fieldMapping.get(fieldName) != null) {
            try {
                return Class.forName(fieldMapping.get(fieldName).getClassType(), true, Thread.currentThread().getContextClassLoader());
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                return null;
            }
        } else {
            return null;
        }
    }

    public String getFieldRelationType(String fieldName) {
        if (fieldMapping.containsKey(fieldName) && fieldMapping.get(fieldName) != null) {
            return fieldMapping.get(fieldName).getRelationType();
        } else {
            return null;
        }
    }

    public String getFieldRelationTable(String fieldName) {
        if (fieldMapping.containsKey(fieldName) && fieldMapping.get(fieldName) != null) {
            return fieldMapping.get(fieldName).getRelationTable();
        } else {
            return null;
        }
    }

    public String getFieldMappedBy(String fieldName) {
        if (fieldMapping.containsKey(fieldName) && fieldMapping.get(fieldName) != null) {
            return fieldMapping.get(fieldName).getMappedBy();
        } else {
            return null;
        }
    }

}
