package com.helianhealth.agent.remote.database;

import com.helianhealth.agent.enums.OperationType;
import com.helianhealth.agent.enums.ParamType;
import com.helianhealth.agent.model.domain.InterfaceWorkflowNodeDO;
import com.helianhealth.agent.model.dto.ParamTreeNode;
import com.helianhealth.agent.remote.ParamResolver;
import com.helianhealth.agent.utils.JsonUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
@Slf4j
public class DatabaseSqlHandler implements ParamResolver {

    private final DatabaseApiClientManager clientManager;

    @Override
    public String resolveParamNodes(InterfaceWorkflowNodeDO flowNode, List<ParamTreeNode> params) {
        // 数据库中的数据的处理
        Map<String, Object> metaInfo = JsonUtils.toMap(flowNode.getMetaInfo());
        String tableName = (String) metaInfo.get("tableName");
        String operation = (String) metaInfo.get("operation");

        if ("INSERT".equalsIgnoreCase(operation)) {
            return buildInsertSql(tableName, params.get(0));
        } else {
            // 默认处理SELECT操作
            return buildSelectSql(tableName, params);
        }
    }

    /**
     * 构建INSERT SQL语句(支持对象和数组类型参数)
     * 优化点：将多个INSERT合并为一个批量INSERT语句
     */
    private String buildInsertSql(String tableName, ParamTreeNode paramNode) {
        // 收集所有要插入的记录
        List<List<ParamTreeNode>> allRecords = new ArrayList<>();

        // 如果参数节点是数组类型，收集数组中的每个元素
        if (paramNode.getParamType() == ParamType.ARRAY || paramNode.getParamType() == ParamType.PURE_ARRAY) {
            if (paramNode.getChildren() != null) {
                for (ParamTreeNode childNode : paramNode.getChildren()) {
                    // 只处理有效的对象类型子节点
                    if (childNode.getParamType() == ParamType.OBJECT && childNode.getChildren() != null) {
                        List<ParamTreeNode> validFields = childNode.getChildren().stream()
                                .filter(node -> node.getParamValue() != null)
                                .collect(Collectors.toList());
                        if (!validFields.isEmpty()) {
                            allRecords.add(validFields);
                        }
                    }
                }
            }
        } else if (paramNode.getParamType() == ParamType.OBJECT) {
            // 对象类型直接添加为一条记录
            if (paramNode.getChildren() != null) {
                List<ParamTreeNode> validFields = paramNode.getChildren().stream()
                        .filter(node -> node.getParamValue() != null)
                        .collect(Collectors.toList());
                if (!validFields.isEmpty()) {
                    allRecords.add(validFields);
                }
            }
        }

        // 如果没有记录要插入，返回空字符串
        if (allRecords.isEmpty()) {
            return "";
        }

        // 构建批量INSERT语句
        return buildBatchInsertSql(tableName, allRecords);
    }

    /**
     * 构建批量INSERT SQL语句
     * 格式：INSERT INTO table (col1, col2, ...) VALUES (val1, val2, ...), (val1, val2, ...), ...;
     */
    private String buildBatchInsertSql(String tableName, List<List<ParamTreeNode>> allRecords) {
        // 所有记录必须有相同的字段，取第一条记录的字段作为基准
        List<ParamTreeNode> firstRecord = allRecords.get(0);
        List<String> fieldNames = firstRecord.stream()
                .map(ParamTreeNode::getParamKey)
                .collect(Collectors.toList());

        StringBuilder sql = new StringBuilder("INSERT INTO ").append(tableName).append(" (");

        // 拼接字段名
        for (int i = 0; i < fieldNames.size(); i++) {
            if (i > 0) {
                sql.append(", ");
            }
            sql.append(fieldNames.get(i));
        }
        sql.append(") VALUES ");

        // 拼接所有记录的values部分
        for (int i = 0; i < allRecords.size(); i++) {
            List<ParamTreeNode> record = allRecords.get(i);

            // 验证当前记录的字段与第一条记录是否一致
            List<String> currentFieldNames = record.stream()
                    .map(ParamTreeNode::getParamKey)
                    .collect(Collectors.toList());
            if (!currentFieldNames.equals(fieldNames)) {
                log.warn("批量插入时发现字段不一致，跳过异常记录");
                continue;
            }

            // 拼接一条记录的values
            if (i > 0) {
                sql.append(", ");
            }
            sql.append("(");

            for (int j = 0; j < record.size(); j++) {
                ParamTreeNode field = record.get(j);
                Object fieldValue = field.getParamValue();

                if (j > 0) {
                    sql.append(", ");
                }

                // 处理不同类型的值
                if (fieldValue instanceof String || fieldValue instanceof java.util.Date) {
                    // 转义单引号，防止SQL注入和语法错误
                    String escapedValue = String.valueOf(fieldValue).replace("'", "''");
                    sql.append("'").append(escapedValue).append("'");
                } else {
                    sql.append(fieldValue);
                }
            }
            sql.append(")");
        }

        sql.append(";");
        return sql.toString();
    }

    /**
     * 构建单条INSERT SQL语句 (用于兼容单个对象插入的情况)
     */
    private String buildSingleInsertSql(String tableName, ParamTreeNode paramNode) {
        StringBuilder sql = new StringBuilder("INSERT INTO ").append(tableName);

        // 对于对象类型，从children中获取字段信息
        if (paramNode.getParamType() == ParamType.OBJECT && paramNode.getChildren() != null) {
            List<ParamTreeNode> fields = paramNode.getChildren();
            List<ParamTreeNode> validFields = fields.stream()
                    .filter(node -> node.getParamValue() != null)
                    .collect(Collectors.toList());

            if (!validFields.isEmpty()) {
                sql.append(" (");
                StringBuilder valuesPart = new StringBuilder(" VALUES (");

                for (int i = 0; i < validFields.size(); i++) {
                    ParamTreeNode field = validFields.get(i);
                    String fieldKey = field.getParamKey();
                    Object fieldValue = field.getParamValue();

                    if (i > 0) {
                        sql.append(", ");
                        valuesPart.append(", ");
                    }

                    sql.append(fieldKey);
                    // 处理不同类型的值，转义单引号
                    if (fieldValue instanceof String || fieldValue instanceof java.util.Date) {
                        String escapedValue = String.valueOf(fieldValue).replace("'", "''");
                        valuesPart.append("'").append(escapedValue).append("'");
                    } else {
                        valuesPart.append(fieldValue);
                    }
                }

                sql.append(")");
                valuesPart.append(")");
                sql.append(valuesPart);
            }
        }

        sql.append(";");
        return sql.toString();
    }

    /**
     * 构建SQL查询语句
     */
    private String buildSelectSql(String tableName, List<ParamTreeNode> params) {
        StringBuilder sql = new StringBuilder("SELECT * FROM ").append(tableName);

        // 过滤出有值且有操作类型的参数节点
        List<ParamTreeNode> validParams = params.stream()
                .filter(node -> node.getParamValue() != null)
                .filter(node -> node.getOperationType() != null)
                .collect(Collectors.toList());

        if (!validParams.isEmpty()) {
            sql.append(" WHERE ");

            for (ParamTreeNode param : validParams) {
                String paramKey = param.getParamKey();
                Object paramValue = param.getParamValue();
                OperationType operationType = param.getOperationType();

                // 根据不同的操作类型添加相应的条件表达式和值
                switch (operationType) {
                    case LIKE:
                        // LIKE操作需要处理通配符，转义单引号
                        String likeValue = String.valueOf(paramValue).replace("'", "''");
                        sql.append(paramKey).append(" LIKE '").append(likeValue).append("' AND ");
                        break;
                    case IN:
                    case NOT_IN:
                        // IN和NOT IN需要处理值列表
                        if (paramValue instanceof List) {
                            List<?> valueList = (List<?>) paramValue;
                            sql.append(paramKey).append(" ").append(operationType.getOperator()).append(" (");
                            for (int i = 0; i < valueList.size(); i++) {
                                if (i > 0) {
                                    sql.append(",");
                                }
                                // 转义单引号
                                String inValue = String.valueOf(valueList.get(i)).replace("'", "''");
                                sql.append("'").append(inValue).append("'");
                            }
                            sql.append(") AND ");
                        } else {
                            // 转义单引号
                            String inValue = String.valueOf(paramValue).replace("'", "''");
                            sql.append(paramKey).append(" ").append(operationType.getOperator()).append(" ('")
                                    .append(inValue).append("') AND ");
                        }
                        break;
                    case IS_NULL:
                    case IS_NOT_NULL:
                        // IS NULL和IS NOT NULL不需要参数值
                        sql.append(paramKey).append(" ").append(operationType.getOperator()).append(" AND ");
                        break;
                    default:
                        // 其他操作符使用标准格式，转义单引号
                        String paramVal = String.valueOf(paramValue).replace("'", "''");
                        sql.append(paramKey).append(" ").append(operationType.getOperator()).append(" '")
                                .append(paramVal).append("' AND ");
                }
            }

            // 移除最后的 " AND "
            sql.delete(sql.length() - 5, sql.length());
        }

        return sql.toString();
    }

    public Map<String, Object> invokeSqlAndConvertResult(InterfaceWorkflowNodeDO flowNode, String sql) {
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            connection = clientManager.getConnection(flowNode.getMetaInfo());
            ps = connection.prepareStatement(sql);
            // 根据SQL类型选择执行方法
            String sqlType = sql.trim().toUpperCase();
            if (sqlType.startsWith("SELECT")) {
                rs = ps.executeQuery();
                return convertResultSetToMap(rs);
            } else {
                // 处理INSERT, UPDATE, DELETE等更新操作
                int affectedRows = ps.executeUpdate();
                Map<String, Object> result = new HashMap<>();
                result.put("affectedRows", affectedRows);
                return result;
            }
        } catch (SQLException e) {
            throw new RuntimeException("SQL执行失败: " + e.getMessage() + ", SQL: " + sql, e);
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    log.warn("关闭ResultSet失败", e);
                }
            }
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException e) {
                    log.warn("关闭PreparedStatement失败", e);
                }
            }
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    log.warn("关闭Connection失败", e);
                }
            }
        }
    }

    /**
     * 将结果集转换为Map
     */
    public Map<String, Object> convertResultSetToMap(ResultSet rs) throws SQLException {

        // 用于存储所有行数据的列表
        List<Map<String, Object>> dataList = new ArrayList<>();
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        while (rs.next()) {
            // 存储单行数据的Map
            Map<String, Object> rowMap = new HashMap<>();
            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnName(i);
                Object value = rs.getObject(i);
                rowMap.put(columnName, value);
            }
            dataList.add(rowMap);
        }

        // 构建最终要返回的Map，包含数据列表和总行数
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("data", dataList);
        resultMap.put("total", dataList.size());
        return resultMap;
    }
}
