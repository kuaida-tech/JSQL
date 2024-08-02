package tech.kuaida.dbhandler;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.SQLQuery;
import org.hibernate.transform.AliasToEntityMapResultTransformer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import tech.kuaida.sqlbuilder.SelectBuilder;
import tech.kuaida.utils.NcStringUtils;

import javax.persistence.EntityManager;
import javax.persistence.JoinColumn;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigInteger;
import java.net.URL;
import java.util.*;

public class DbBuilder {
    private String modulesPath;
    private Map<String, Map<String, Map<String, String>>> classInfo; //保存类相关信息

    /**
     * @param modulesPath 模块所在的包路径，例如: tech.tech.kuaida.modules
     * */
    public DbBuilder(String modulesPath) {
        this.modulesPath = modulesPath;
        classInfo = new HashMap<>();
    }

    public Page<Map<String, Object>> subQuery(EntityManager entityManager, Map<String, Map<String, Map<String, String>>> classInfo, String parentId, DbCommand parentCommand, DbCommand command, Map map, JSONObject jsonObject) {
        SelectBuilder selectBuilder = new SelectBuilder();

        buildClassInfo(command, null);
        String tableName = getClassInfo("_table", command, null);

        selectBuilder.from(tableName + " AS " + (command.getAlias() != null?command.getAlias():command.getCode()));

        if (!jsonObject.containsKey("id")) {
            jsonObject.put("id", "");
        }

        Iterator<String> iterator = jsonObject.keySet().iterator();

        JSONObject waitJson = new JSONObject();

        int page = 0;
        int size = Integer.MAX_VALUE;

        Pageable pageable = PageRequest.of(page, size);

        while (iterator.hasNext()) {
            String key = iterator.next();

            DbCommand fieldCommand = extractCommand(key);

            if (key.equals("$LIMIT")) {
                JSONObject limit = jsonObject.getJSONObject("$LIMIT");
                page = limit.getInt("$NO");
                size = limit.getInt("$SIZE");
                pageable = PageRequest.of(page, size);

                limitBuilder(selectBuilder, jsonObject.getJSONObject(key));
            } else if (key.equals("$HAVING")) {
                whereBuilder(command, selectBuilder, key, key, jsonObject.getJSONObject(key), true);
            } else if (key.equals("$OR") || key.equals("$AND")) {
                whereBuilder(command, selectBuilder, key, key, jsonObject.getJSONObject(key), false);
            } else if (jsonObject.get(key) instanceof JSONObject) {
                waitJson.put(key, jsonObject.getJSONObject(key));
                if (getClassInfo( "_relationship", command, fieldCommand) != null) {
                    String relationship = getClassInfo("_relationship", command, fieldCommand);
                    com.alibaba.fastjson.JSONObject relationshipJson = com.alibaba.fastjson.JSONObject.parseObject(relationship);
                    switch (relationshipJson.getString("type")) {
                        case "1": {
                            selectBuilder.column(fieldCommand, command.getCode() + "." + getClassInfo("_column", command, fieldCommand), relationshipJson.getString("field"));
                        }
                        break;
                        case "3": {
                            selectBuilder.column(fieldCommand, command.getCode() + "." + getClassInfo("_column", command, fieldCommand), relationshipJson.getString("field"));
                        }
                        break;
                    }
                }
            } else {
                if (getClassInfo("_column", command, fieldCommand) != null) {
                    selectBuilder.column(fieldCommand, command.getCode() + "." + getClassInfo("_column", command, fieldCommand), (fieldCommand.getAlias() != null?command.getAlias():command.getCode()));
                }
            }
        }

        if (parentId != null) {
            String relationship = getClassInfo("_relationship", parentCommand, command);
            if (relationship != null) {
                com.alibaba.fastjson.JSONObject relationshipJson = com.alibaba.fastjson.JSONObject.parseObject(relationship);
                switch (relationshipJson.getString("type")) {
                    case "1": {
                        selectBuilder.where("id = '" + parentId + "'");
                    }
                    break;
                    case "2": {
                        selectBuilder.where(relationshipJson.getString("field") + " = '" + parentId + "'");
                    }
                    break;
                    case "3": {
                        selectBuilder.where("id = '" + parentId + "'");
                    }
                    break;
                    case "4": {
                        if (parentCommand.getCode().equals(relationshipJson.getString("joinTable"))) {
                            selectBuilder.leftJoin(relationshipJson.getString("table"),  relationshipJson.getString("table") + "." + relationshipJson.getString("inverseJoinColumn") + " = " + command.getCode() + ".id");
                            selectBuilder.where(relationshipJson.getString("table") + "." + relationshipJson.getString("joinColumn") + " = '" + parentId + "'");
                        } else if (parentCommand.getCode().equals(relationshipJson.getString("inverseJoinTable"))) {
                            selectBuilder.leftJoin(getClassInfo("_table", command, null),  relationshipJson.getString("table") + "." + relationshipJson.getString("joinColumn") + " = " + command.getCode() + ".id");
                            selectBuilder.where(relationshipJson.getString("table") + "." + relationshipJson.getString("inverseJoinColumn") + " = '" + parentId + "'");
                        }
                    }
                    break;
                }
            }
        }

        List<Map<String, Object>> list = entityManager.createNativeQuery(selectBuilder.toString()).unwrap(SQLQuery.class).setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE).getResultList();
        BigInteger total = (BigInteger) entityManager.createNativeQuery(selectBuilder.toCountString()).getSingleResult();

        if (map != null) {
            map.put( (command.getAlias() != null?command.getAlias():command.getCode()), list);
        }

        for (int i = 0; i < list.size(); i++) {
            Map<String, Object> result = list.get(i);

            Iterator<String> waitIter = waitJson.keySet().iterator();
            while (waitIter.hasNext()) {
                String key = waitIter.next();
                DbCommand fieldCommand = extractCommand(key);
                buildClassInfo(fieldCommand, command);

                String field = "id";
                String relationship = getClassInfo("_relationship", command, fieldCommand);
                //判定是否有关联关系，如果有则进行子查询
                if (relationship != null) {
                    com.alibaba.fastjson.JSONObject relationshipJson = com.alibaba.fastjson.JSONObject.parseObject(relationship);
                    switch (relationshipJson.getString("type")) {
                        case "1": {
                            field = relationshipJson.getString("field");
                        }
                        break;
                        case "2": {
                            field = "id";
                        }
                        break;
                        case "3": {
                            field = relationshipJson.getString("field");
                        }
                        break;
                        case "4": {
                            field = "id";
                        }
                        break;
                    }

                    String id = (String)result.get(field);

                    //关联ID不为空则进行子查询
                    if (id != null) {
                        subQuery(entityManager, classInfo, id, command, fieldCommand, result, waitJson.getJSONObject(key));
                    }
                }
            }
        }

        Page<Map<String, Object>> result = new PageImpl(list, pageable, total.longValue());

        return result;

    }

    public void fromBuilder(DbCommand command, SelectBuilder selectBuilder, JSONObject jsonObject) {
        buildClassInfo(command, null);

        if (!jsonObject.containsKey("id")) {
            jsonObject.put("id", "");
        }

        String tableName = getClassInfo("_table", command, null);

        Iterator<String> iterator = jsonObject.keySet().iterator();

        while (iterator.hasNext()) {
            String key = iterator.next();

            DbCommand fieldCommand = extractCommand(key);

            if ("$LIMIT".equals(key)) {
                limitBuilder(selectBuilder, jsonObject.getJSONObject(key));
            } else if ("$HAVING".equals(key)) {
                whereBuilder( command, selectBuilder, key, key, jsonObject.getJSONObject(key), true);
            } else if ("$AND".equals(key) || "$OR".equals(key)) {
                whereBuilder( command, selectBuilder, key, key, jsonObject.getJSONObject(key), false);
            } else if (jsonObject.get(key) instanceof JSONObject) {
                JSONObject subJsonObject = jsonObject.getJSONObject(key);
                Iterator<String> subIterator = subJsonObject.keySet().iterator();

                //判定是自动关键还是人为关联
                boolean foundJoin = false;

                while (subIterator.hasNext()) {
                    String subKey = subIterator.next();
                    if (subKey.indexOf("$J") != -1) {
                        foundJoin = true;
                        break;
                    }
                }

                if (!foundJoin) {
                    getJoinColumn( command, selectBuilder, fieldCommand);
                }

                fromBuilder(fieldCommand, selectBuilder, jsonObject.getJSONObject(key));
            } else {
                if (fieldCommand.getJoinType() != 0) {
                    selectBuilder.removeFrom(tableName);

                    switch (fieldCommand.getJoinType()) {
                        case 1:
                            selectBuilder.join(tableName + " AS " + (command.getAlias() != null?command.getAlias():command.getCode()) + " ON " + command.getCode() + "." + getClassInfo("_column", command, fieldCommand) + " = " + fieldCommand.getJoinFiled());
                            break;
                        case 2:
                            selectBuilder.leftJoin(tableName + " AS " + (command.getAlias() != null?command.getAlias():command.getCode()),command.getCode() + "." + getClassInfo("_column", command, fieldCommand) + " = " + fieldCommand.getJoinFiled());
                            break;
                        case 3:
                            selectBuilder.rightJoin(tableName + " AS " + (command.getAlias() != null?command.getAlias():command.getCode()),command.getCode() + "." + getClassInfo("_column", command, fieldCommand) + " = " + fieldCommand.getJoinFiled());
                            break;
                        case 4:
                            selectBuilder.innerJoin(tableName + " AS " + (command.getAlias() != null?command.getAlias():command.getCode()),command.getCode() + "." + getClassInfo("_column", command, fieldCommand) + " = " + fieldCommand.getJoinFiled());
                            break;
                        case 5:
                            selectBuilder.fullJoin(tableName + " AS " + (command.getAlias() != null?command.getAlias():command.getCode()),command.getCode() + "." + getClassInfo("_column", command, fieldCommand) + " = " + fieldCommand.getJoinFiled());
                            break;
                    }
                }

                if (getClassInfo("_column", command, fieldCommand) != null) {
                    if (fieldCommand.getAlias() != null) {
                        selectBuilder.column(fieldCommand, command.getCode() + "." + getClassInfo( "_column", command, fieldCommand), fieldCommand.getAlias());
                    } else {
                        selectBuilder.column(fieldCommand, command.getCode() + "." + getClassInfo( "_column", command, fieldCommand), getClassInfo( "_level", command, null) + "." + fieldCommand.getCode());
                    }
                }
            }
        }
    }


    public void whereBuilder(DbCommand parentCommand, SelectBuilder selectBuilder, String superiorCondition, String condition, JSONObject parentJson, boolean isHaving) {
        StringBuffer sb = new StringBuffer();
        sb.append("( ");

        int count = 0;

        Iterator<String> iterator = parentJson.keySet().iterator();

        while (iterator.hasNext()) {
            String key = iterator.next();

            String[] expression = key.split("[$]");

            DbCommand fieldCommand = extractCommand(key);
            if (key.equals("$OR") || key.equals("$AND")) {
                whereBuilder( parentCommand, selectBuilder, condition, key, parentJson.getJSONObject(key), isHaving);
            } else {
                Object object = parentJson.get(key);
                if (object instanceof JSONObject) {
                    JSONObject json = (JSONObject) object;

                    if (!"null".equals(json.getString(key)) && StringUtils.isNotEmpty(json.getString(key))) {
                        count++;

                        String[] mExpression = matchExpression(expression[1], json.getString(key));

                        String replaceStr = expression[0].replaceAll("[.]", "_");

                        if (expression[0].indexOf(".") != -1) {
                            List<Object> columns = selectBuilder.getColumns();
                            for (int j = 0; j < columns.size(); j++) {
                                if (columns.get(j) instanceof String) {
                                    String column = (String)columns.get(j);
                                    String findStr = "\"" + expression[0] + "\"";
                                    if (column.indexOf(findStr) != -1) {
                                        selectBuilder.column(column.replace(findStr, replaceStr));
                                        break;
                                    }
                                }
                            }
                            sb.append(replaceStr + " " + mExpression[0] + " '" + mExpression[1] + "'");
                        } else if (getClassInfo("_column", parentCommand, fieldCommand) == null) {
                            sb.append(expression[0] + " " + mExpression[0] + " '" + mExpression[1] + "'");
                        } else {
                            sb.append(parentCommand.getCode() + "." + getClassInfo("_column", parentCommand, fieldCommand) + " " + mExpression[0] + " '" + mExpression[1] + "'");
                        }

                        if (condition.equals("$OR") && iterator.hasNext()) {
                            sb.append(" OR ");
                        } else if (condition.equals("$AND") && iterator.hasNext()) {
                            sb.append(" AND ");
                        }
                    }
                } else if (object instanceof JSONArray) {
                    JSONArray json = (JSONArray) object;

                    for (int i = 0; i < json.size(); i++) {
                        if (!"null".equals(json.getString(i)) && StringUtils.isNotEmpty(json.getString(i))) {
                            count++;

                            String[] mExpression = matchExpression(expression[1], json.getString(i));

                            String replaceStr = expression[0].replaceAll("[.]", "_");

                            if (expression[0].indexOf(".") != -1) {
                                List<Object> columns = selectBuilder.getColumns();
                                for (int j = 0; j < columns.size(); j++) {
                                    if (columns.get(j) instanceof String) {
                                        String column = (String)columns.get(j);
                                        String findStr = "\"" + expression[0] + "\"";
                                        if (column.indexOf(findStr) != -1) {
                                            selectBuilder.column(column.replace(findStr, replaceStr));
                                            break;
                                        }
                                    }
                                }
                                sb.append(replaceStr + " " + mExpression[0] + " '" + mExpression[1] + "'");
                            } else if (getClassInfo("_column", parentCommand, fieldCommand) == null) {
                                sb.append(expression[0] + " " + mExpression[0] + " '" + mExpression[1] + "'");
                            } else {
                                sb.append(parentCommand.getCode() + "." + getClassInfo("_column", parentCommand, fieldCommand) + " " + mExpression[0] + " '" + mExpression[1] + "'");
                            }

                            if (condition.equals("$OR") && i != json.size() - 1) {
                                sb.append(" OR ");
                            } else if (condition.equals("$AND") && i != json.size() - 1) {
                                sb.append(" AND ");
                            }
                        }
                    }
                } else {
                    if (!"null".equals(parentJson.getString(key)) && StringUtils.isNotEmpty(parentJson.getString(key))) {
                        count++;

                        String[] mExpression = matchExpression(expression[1], parentJson.getString(key));

                        String replaceStr = expression[0].replaceAll("[.]", "_");

                        if (expression[0].indexOf(".") != -1) {
                            List<Object> columns = selectBuilder.getColumns();
                            for (int j = 0; j < columns.size(); j++) {
                                if (columns.get(j) instanceof String) {
                                    String column = (String)columns.get(j);
                                    String findStr = "\"" + expression[0] + "\"";
                                    if (column.indexOf(findStr) != -1) {
                                        selectBuilder.column(column.replace(findStr, replaceStr));
                                        break;
                                    }
                                }
                            }
                            sb.append(replaceStr + " " + mExpression[0] + " '" + mExpression[1] + "'");
                        } else if (getClassInfo("_column", parentCommand, fieldCommand) == null) {
                            sb.append(expression[0] + " " + mExpression[0] + " '" + mExpression[1] + "'");
                        } else {
                            sb.append(parentCommand.getCode() + "." + getClassInfo("_column", parentCommand, fieldCommand) + " " + mExpression[0] + " '" + mExpression[1] + "'");
                        }

                        if (condition.equals("$OR") && iterator.hasNext()) {
                            sb.append(" OR ");
                        } else if (condition.equals("$AND") && iterator.hasNext()) {
                            sb.append(" AND ");
                        }
                    }

                }
            }

        }

        if (count == 0) {
            sb.append("1=1");
        }

        String isAnd = sb.substring(sb.length()-5);
        String isOr = sb.substring(sb.length()-4);

        if (isAnd.equals(" AND ")) {
            sb.delete(sb.length() - 5, sb.length());
        } else if (isOr.equals(" OR ")) {
            sb.delete(sb.length() - 4, sb.length());
        }

        sb.append(" )");

        if (isHaving) {
            selectBuilder.having(sb.toString());
        } else {
            if (superiorCondition != null) {
                if (superiorCondition.equals("$OR")) {
                    selectBuilder.or(sb.toString());
                } else if (superiorCondition.equals("$AND")) {
                    selectBuilder.and(sb.toString());
                }
            }
        }

    }

    public void limitBuilder(SelectBuilder selectBuilder, JSONObject jsonObject) {
        int no = 0;
        int size = Integer.MAX_VALUE;

        Iterator<String> iterator = jsonObject.keySet().iterator();
        while (iterator.hasNext()) {
            String key = iterator.next();
            if (key.toUpperCase().equals("$NO")) {
                no = jsonObject.getInt("$NO") * jsonObject.getInt("$SIZE");
            } else if (key.toUpperCase().equals("$SIZE")) {
                size = jsonObject.getInt("$SIZE");
            }
        }

        selectBuilder.limit(no, size);
    }

    public void getJoinColumn(DbCommand parentCommand, SelectBuilder selectBuilder, DbCommand command) {
        try {
            buildClassInfo( command, parentCommand);

            String parentTableName = getClassInfo("_table", parentCommand, null);
            String tableName = getClassInfo("_table", command, null);

            boolean isRefrence = false;

            String classType = command.getCode();

            if (command.getReference() != null) {
                classType = command.getReference();
            } else if (parentCommand != null) {
                classType = getClassInfo("_type", parentCommand, command);
            }

            if (classType == null) {
                classType = command.getCode();
                isRefrence = true;
            }

            Class clazz = Class.forName(modulesPath + "." + classType.toLowerCase() + "." + NcStringUtils.upperFirstCase(classType));
            Field[] fields = clazz.getDeclaredFields();
            for (int i = 0; i < fields.length; i++) {
                Field field = fields[i];

                javax.persistence.OneToOne oneToOneAnnotation = field.getAnnotation(javax.persistence.OneToOne.class);
                javax.persistence.OneToMany oneToManyAnnotation = field.getAnnotation(javax.persistence.OneToMany.class);
                javax.persistence.ManyToOne manyToOneAnnotation = field.getAnnotation(javax.persistence.ManyToOne.class);
                javax.persistence.ManyToMany manyToManyAnnotation = field.getAnnotation(javax.persistence.ManyToMany.class);

                if (manyToOneAnnotation != null && isRefrence) {
                    JoinColumn joinColumnAnnotation = field.getAnnotation(JoinColumn.class);
                    String parentType = getClassInfo( "_type", parentCommand, null);
                    if (field.getType().getSimpleName().equals(parentType)) {
                        selectBuilder.leftJoin(tableName + " AS " + (command.getAlias() != null?command.getAlias():command.getCode()), parentCommand.getCode() + ".id" + " = " + command.getCode()  + "." + joinColumnAnnotation.name());
                    }
                }

                if (oneToOneAnnotation != null || manyToOneAnnotation != null) {
                    try {
                        Class targetClass = Class.forName(field.getType().getName());
                        javax.persistence.Table targetTableAnnotation = (javax.persistence.Table)targetClass.getAnnotation(javax.persistence.Table.class);
                        if (parentTableName.equals(targetTableAnnotation.name())) {
                            JoinColumn joinColumnAnnotation = field.getAnnotation(JoinColumn.class);
                            selectBuilder.leftJoin(tableName + " AS " + (command.getAlias() != null?command.getAlias():command.getCode()), parentCommand.getCode() + ".id" + " = " + command.getCode()  + "." + joinColumnAnnotation.name());
                            break;
                        }
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                } else if (oneToManyAnnotation != null) {
                    String mappedBy = oneToManyAnnotation.mappedBy();

                    Class targetClass = oneToManyAnnotation.targetEntity();
                    javax.persistence.Table targetTableAnnotation = (javax.persistence.Table)targetClass.getAnnotation(javax.persistence.Table.class);

                    if (parentTableName.equals(targetTableAnnotation.name())) {
                        Field[] targetFields = targetClass.getDeclaredFields();

                        for (int i1 = 0; i1 < targetFields.length; i1++) {
                            Field targetField = targetFields[i1];
                            if (targetField.getName().equals(mappedBy)) {
                                JoinColumn joinColumn = targetField.getAnnotation(JoinColumn.class);
                                javax.persistence.Column column = targetField.getAnnotation(javax.persistence.Column.class);

                                if (joinColumn != null) {
                                    selectBuilder.leftJoin(tableName + " AS " + (command.getAlias() != null?command.getAlias():command.getCode()), parentCommand.getCode() + "." + joinColumn.name() + " = " + command.getAlias() + ".id");
                                } else if (column != null) {
                                    selectBuilder.leftJoin(tableName + " AS " + (command.getAlias() != null?command.getAlias():command.getCode()), parentCommand.getCode() + "." + column.name() + " = " + command.getAlias() + ".id");
                                }

                                break;
                            }
                        }

                        break;
                    }

                } else if (manyToManyAnnotation != null) {
                    ParameterizedType listGenericType = (ParameterizedType) field.getGenericType();
                    Type[] listActualTypeArguments = listGenericType.getActualTypeArguments();
                    String typeName =  listActualTypeArguments[0].getTypeName();
                    typeName = typeName.substring(typeName.lastIndexOf(".") + 1);

                    String parentTypeName = getClassInfo("_type", parentCommand, null);

                    javax.persistence.JoinTable joinTable = field.getAnnotation(javax.persistence.JoinTable.class);

                    if (typeName.equals(parentTypeName)) {
                        if (joinTable != null) {
                            selectBuilder.leftJoin(joinTable.name(), joinTable.name() + "." + joinTable.inverseJoinColumns()[0].name() + " = " + parentTableName + ".id");
                            selectBuilder.leftJoin(tableName  + " AS " + (command.getAlias() != null?command.getAlias():command.getCode()), joinTable.name() + "." + joinTable.joinColumns()[0].name() + " = " + command.getAlias() + ".id");
                        } else {
                            String mappedBy = manyToManyAnnotation.mappedBy();

                            Class targetClass = manyToManyAnnotation.targetEntity();
                            javax.persistence.Table targetTableAnnotation = (javax.persistence.Table) targetClass.getAnnotation(javax.persistence.Table.class);

                            Field[] targetFields = targetClass.getDeclaredFields();

                            for (int i1 = 0; i1 < targetFields.length; i1++) {
                                Field targetField = targetFields[i1];
                                if (targetField.getName().equals(mappedBy)) {
                                    javax.persistence.JoinTable joinTable1 = targetField.getAnnotation(javax.persistence.JoinTable.class);

                                    selectBuilder.leftJoin(joinTable1.name(), joinTable1.name() + "." + joinTable1.joinColumns()[0].name() + " = " + targetTableAnnotation.name() + ".id");
                                    selectBuilder.leftJoin(tableName  + " AS " + (command.getAlias() != null?command.getAlias():command.getCode()), joinTable1.name() + "." + joinTable1.inverseJoinColumns()[0].name() + " = " + command.getAlias() + ".id");
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

    }

    public DbCommand extractCommand(String key) {
        String[] commands = key.split("[$]");
        DbCommand command = new DbCommand();
        command.setCode(commands[0]);
        for (int i = 1; i < commands.length; i++) {
            String firstChar = commands[i].substring(0, 1);
            if (firstChar.equals("A")) {
                command.setAlias(commands[i].substring(1));
            } else if (firstChar.equals("R")) {
                command.setReference(commands[i].substring(1));
            } else if (firstChar.equals("I")) {
                command.setIgnore(true);
            } else if (firstChar.equals("G")) {
                command.setGroupBy(true);
            } else if (firstChar.equals("F")) {
                String function = commands[i].substring(1);

                if (function.indexOf("__") != -1) {
                    String[] fun = function.split("__");
                    command.setFunction(fun[0]);

                    //放置function所需参数
                    List<String> list = new ArrayList<>();
                    for (int j = 1; j < fun.length; j++) {
                        list.add(fun[j]);
                    }
                    command.setParameters(list);
                } else {
                    command.setFunction(function);
                }
            } else if (firstChar.equals("S")) {
                command.setSort(commands[i].substring(1));
            } else if (firstChar.equals("J")) {
                String startChar = commands[i].substring(0, 2);

                String[] expression = null;
                int index = 2;

                if (startChar.equals("JL")) {
                    command.setJoinType(2);
                } else if (startChar.equals("JR")) {
                    command.setJoinType(3);
                } else if (startChar.equals("JI")) {
                    command.setJoinType(4);
                } else if (startChar.equals("JF")) {
                    command.setJoinType(5);
                } else {
                    index = 1;
                    command.setJoinType(1);
                }

                expression = commands[i].substring(index).split("__");

                DbCommand tableCommand = extractCommand(expression[0]);
                DbCommand columnCommand = extractCommand(expression[1]);

                buildClassInfo( tableCommand, null);
                String columnName = getClassInfo("_column", tableCommand, columnCommand);
                command.setJoinFiled(tableCommand.getCode() + "." + columnName);
            }
        }
        return command;
    }

    /**
     * 根据编码构建类信息
     *
     * @param command    当前编码命令对象
     * @param parentCommand   当前编码父级命令对象
     *
     * */
    public void buildClassInfo(DbCommand command, DbCommand parentCommand) {
        if (classInfo.containsKey(command.getCode())) {
            return;
        }

        boolean isRefrence = false;

        Map<String, Map<String, String>> map = new HashMap<>();
        Map<String, String> classMap = new HashMap<>();
        map.put("_class", classMap);

        String parentClassType = getClassInfo( "_type", parentCommand, null);
        String classType = command.getCode();
        try {
            //根据指令指定的引用类型来加载类
            if (command != null && command.getReference() != null) {
                classType = command.getReference();
                isRefrence = true;
            }

            //判断当前类型是否存在
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            String packagePath = (modulesPath + "." + classType.toLowerCase()).replace(".", "/");
            try{
                Enumeration<URL> urls = loader.getResources(packagePath);
                if(!urls.hasMoreElements() && parentCommand != null) {
                    //当前类不存在，则从父类中查找
                    classType = getClassInfo( "_type", parentCommand, command);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            Class<?> clazz = Class.forName(modulesPath + "." + classType.toLowerCase() + "." + NcStringUtils.upperFirstCase(classType));

            javax.persistence.Table tableAnnotation = (javax.persistence.Table)clazz.getAnnotation(javax.persistence.Table.class);
            classMap.put("_table", tableAnnotation.name());
            classMap.put("_type", clazz.getSimpleName());

            String _level = getClassInfo("_level", parentCommand, null);
            classMap.put("_level", _level == null? command.getCode():_level + "." + command.getCode());

            Field[] fields = clazz.getDeclaredFields();
            for (int i = 0; i < fields.length; i++) {
                Field field = fields[i];

                Map<String, String> fieldMap = new HashMap();
                map.put(field.getName(), fieldMap);

                javax.persistence.Column columnAnnotation = field.getAnnotation(javax.persistence.Column.class);
                JoinColumn joinColumnAnnotation = field.getAnnotation(JoinColumn.class);

                javax.persistence.OneToOne oneToOneAnnotation = field.getAnnotation(javax.persistence.OneToOne.class);
                javax.persistence.OneToMany oneToManyAnnotation = field.getAnnotation(javax.persistence.OneToMany.class);
                javax.persistence.ManyToOne manyToOneAnnotation = field.getAnnotation(javax.persistence.ManyToOne.class);
                javax.persistence.ManyToMany manyToManyAnnotation = field.getAnnotation(javax.persistence.ManyToMany.class);

                if ("id".equals(field.getName())) {
                    fieldMap.put("_column", "id");
                } else {
                    if (columnAnnotation != null) {
                        fieldMap.put("_column", columnAnnotation.name());
                    } else if (joinColumnAnnotation != null) {
                        fieldMap.put("_column", joinColumnAnnotation.name());
                    } else {
                        fieldMap.put("_column", null);
                    }
                }

                if (field.getType().getSimpleName().equals("List")) {
                    ParameterizedType listGenericType = (ParameterizedType) field.getGenericType();
                    Type[] listActualTypeArguments = listGenericType.getActualTypeArguments();
                    String typeName =  listActualTypeArguments[0].getTypeName();
                    fieldMap.put("_type",typeName.substring(typeName.lastIndexOf(".") + 1));
                } else {
                    fieldMap.put("_type", field.getType().getSimpleName());
                }

                if (oneToOneAnnotation != null) {
                    fieldMap.put("_relationship", "{\"type\":\"1\",\"field\":\"" + joinColumnAnnotation.name() + "\"}");
                } else if (oneToManyAnnotation != null) {
                    Class aClass = oneToManyAnnotation.targetEntity();
                    Field[] declaredFields = aClass.getDeclaredFields();
                    for (int i1 = 0; i1 < declaredFields.length; i1++) {
                        if (declaredFields[i1].getName().equals(oneToManyAnnotation.mappedBy())) {
                            JoinColumn annotation = declaredFields[i1].getAnnotation(JoinColumn.class);
                            fieldMap.put("_relationship", "{\"type\":\"2\",\"field\":\"" + annotation.name() + "\"}");
                            break;
                        }
                    }
                } else if (manyToOneAnnotation != null) {
                    fieldMap.put("_relationship", "{\"type\":\"3\",\"field\":\"" + joinColumnAnnotation.name() + "\"}");

                    if (isRefrence || field.getType().getSimpleName().equals(parentClassType)) {
                        Map<String, Map<String, String>> parentMap = classInfo.get(parentCommand.getCode());
                        Map<String, String> insertMap = new HashMap<>();
                        insertMap.put("_type", NcStringUtils.upperFirstCase(classType));
                        insertMap.put("_relationship", "{\"type\":\"2\", \"field\":\"" + joinColumnAnnotation.name() + "\"}");
                        parentMap.put(command.getCode(), insertMap);
                    }
                } else if (manyToManyAnnotation != null) {
                    javax.persistence.JoinTable joinTable = field.getAnnotation(javax.persistence.JoinTable.class);
                    fieldMap.put("_relationship", "4");

                    if (joinTable != null) {
                        fieldMap.put("_relationship", "{\"type\":\"4\",\"table\":\"" + joinTable.name() + "\",\"joinColumn\":\"" + joinTable.joinColumns()[0].name() + "\", \"inverseJoinColumn\":\"" + joinTable.inverseJoinColumns()[0].name() + "\", \"joinTable\":\"" + command.getCode() + "\"}");
                    } else {
                        Class aClass = manyToManyAnnotation.targetEntity();
                        Field[] declaredFields = aClass.getDeclaredFields();
                        for (int i1 = 0; i1 < declaredFields.length; i1++) {
                            if (declaredFields[i1].getName().equals(manyToManyAnnotation.mappedBy())) {
                                joinTable = declaredFields[i1].getAnnotation(javax.persistence.JoinTable.class);
                                fieldMap.put("_relationship", "{\"type\":\"4\",\"table\":\"" + joinTable.name() + "\",\"joinColumn\":\"" + joinTable.joinColumns()[0].name() + "\", \"inverseJoinColumn\":\"" + joinTable.inverseJoinColumns()[0].name() + "\", \"inverseJoinTable\":\"" + command.getCode() + "\"}");
                                break;
                            }
                        }
                    }
                }
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        classInfo.put(command.getCode(), map);
    }

    public String getClassInfo(String type, DbCommand parentCommand, DbCommand command) {
        switch (type) {
            case "_table": {
                if (classInfo.get(parentCommand.getCode()) != null) {
                    return classInfo.get(parentCommand.getCode()).get("_class").get("_table");
                }
            }
            break;
            case "_column": {
                if (classInfo.get(parentCommand.getCode()) != null && classInfo.get(parentCommand.getCode()).get(command.getCode()) != null) {
                    return classInfo.get(parentCommand.getCode()).get(command.getCode()).get("_column");
                }
            }
            break;
            case "_type": {
                if (parentCommand != null && command != null && classInfo.get(parentCommand.getCode()) != null && classInfo.get(parentCommand.getCode()).get(command.getCode()) != null) {
                    //在父类信息中获取字段的类型
                    return classInfo.get(parentCommand.getCode()).get(command.getCode()).get("_type");
                } else if (parentCommand != null && command == null && classInfo.get(parentCommand.getCode()) != null){
                    //获取父类的类型
                    return classInfo.get(parentCommand.getCode()).get("_class").get("_type");
                } else if (parentCommand != null && command != null && classInfo.get(parentCommand.getCode()) != null && classInfo.get(parentCommand.getCode()).get(command.getCode()) == null) {
                    return null;
                }
            }
            break;
            case "_level": {
                if (parentCommand != null && classInfo.get(parentCommand.getCode()) != null) {
                    return classInfo.get(parentCommand.getCode()).get("_class").get("_level");
                }
            }
            break;
            case "_relationship": {
                if (parentCommand != null && command != null && classInfo.get(parentCommand.getCode()) != null && classInfo.get(parentCommand.getCode()).get(command.getCode()) != null) {
                    return classInfo.get(parentCommand.getCode()).get(command.getCode()).get("_relationship");
                }
            }
            break;
        }

        return null;
    }

    public List<Map<String, Object>> listToJSONArray(List<Map<String, Object>> list){
        List<Map<String, Object>> array = new ArrayList<>();

        if (list != null && list.size() > 0) {
            for (int i = 0; i < list.size(); i++) {
                Map<String, Object> root = new HashMap<>();
                array.add(root);

                Map<String, Object> child = null;
                Map<String, Object> lastObject = null;

                Map map = list.get(i);

                Iterator iter = map.keySet().iterator();
                while (iter.hasNext()) {
                    String key = String.valueOf(iter.next());
                    String value = "";

                    if (map.get(key) != null) {
                        value = String.valueOf(map.get(key));
                    }

                    String[] split = key.split("\\.");

                    if (split.length > 1) {
                        //层级大于1级则进行遍历
                        if (split.length > 2) {
                            for (int j = 1; j < split.length; j++) {
                                //第一级的Key放入到JSON的根层级下
                                if (j == 1) {
                                    //当前根目录是否存在该KEY
                                    if (!root.containsKey(split[j])) {
                                        //当前根目录未存在该Key，则创建一个JSON对象存入该Key中
                                        child = new HashMap<>();
                                        root.put(split[j], child);
                                        lastObject = (Map<String, Object>) root.get(split[j]);
                                    } else {
                                        //根目录已存在该Key，则取出该Key所存储的JSON对象
                                        lastObject = (Map<String, Object>) root.get(split[j]);
                                    }
                                } else if (j != split.length - 1) {
                                    if (!lastObject.containsKey(split[j])) {
                                        child = new HashMap<>();
                                        lastObject.put(split[j], child);
                                        lastObject = (Map<String, Object>) lastObject.get(split[j]);
                                    } else {
                                        //当前层级已有该Key，则直接取出该Key所映射的JSON对象
                                        lastObject = (Map<String, Object>) lastObject.get(split[j]);
                                    }
                                } else {
                                    lastObject.put(split[j], value);
                                }
                            }
                        } else {
                            root.put(split[1], value);
                        }
                    }
                }
            }
        }
        return array;
    }

    public String[] matchExpression(String expression, String value) {
        String[] mExpression = new String[2];
        mExpression[1] = value;
        switch (expression) {
            case "EQ": {
                mExpression[0] = "=";
            }
            break;
            case "NE": {
                mExpression[0] = "<>";
            }
            break;
            case "GT": {
                mExpression[0] = ">";
            }
            break;
            case "GE": {
                mExpression[0] = ">=";
            }
            break;
            case "LT": {
                mExpression[0] = "<";
            }
            break;
            case "LE": {
                mExpression[0] = "<=";
            }
            break;
            case "ISNULL": {
                mExpression[0] = "IS NULL";
            }
            break;
            case "NOTNULL": {
                mExpression[0] = "IS NOT NULL";
            }
            break;
            case "LIKE": {
                mExpression[0] = "LIKE";
                mExpression[1] = "%" + value + "%";
            }
            break;
            case "NOTLIKE": {
                mExpression[0] = "NOT LIKE";
                mExpression[1] = "%" + value + "%";
            }
            break;
        }
        return mExpression;
    }

}
