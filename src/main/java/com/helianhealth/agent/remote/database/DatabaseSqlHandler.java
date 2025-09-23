package com.helianhealth.agent.remote.database;

import com.helianhealth.agent.enums.ParamType;
import com.helianhealth.agent.exception.DatabaseRemoteBusinessException;
import com.helianhealth.agent.model.domain.InterfaceWorkflowNodeDO;
import com.helianhealth.agent.model.dto.ParamTreeNode;
import com.helianhealth.agent.remote.ParamResolver;
import com.helianhealth.agent.utils.JsonUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

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

        Map<String, Object> metaInfo = JsonUtils.toMap(flowNode.getMetaInfo());
        String sqlTemplate = (String) metaInfo.get("sqlTemplate");
        String operation = (String) metaInfo.get("operation");

        // 检查是否有SQL模板
        if (!StringUtils.hasText(sqlTemplate)) {
            throw DatabaseRemoteBusinessException.DATABASE_TEMPLATE_NOT_FOUND.toException();
        }
        if (!StringUtils.hasText(operation)) {
            throw DatabaseRemoteBusinessException.DATABASE_OPERATION_NOT_FOUND.toException();
        }

        switch (operation.toUpperCase()) {
            case "INSERT":
                return buildInsertSql(sqlTemplate, params.get(0));
            case "UPDATE":
                return buildUpdateSql(sqlTemplate, params.get(0));
            case "DELETE":
                return buildDeleteSql(sqlTemplate, params);
            case "SELECT":
            default:
                return buildSelectSql(sqlTemplate, params);
        }
    }

    private String buildUpdateSql(String tableName, ParamTreeNode paramTreeNode) {
        return null;
    }

    private String buildDeleteSql(String tableName, List<ParamTreeNode> params) {
        return null;
    }

    /**
     * 构建INSERT SQL语句(接口场景只存在对象和数组类型参数,单个数据的Insert不存在吧)
     */
    private String buildInsertSql(String sqlTemplate, ParamTreeNode paramNode) {
        // 提取模板中的VALUES部分
        int valuesStart = sqlTemplate.indexOf("VALUES") + "VALUES".length();
        String valueTemplate = sqlTemplate.substring(valuesStart).trim();
        String prefix = sqlTemplate.substring(0, valuesStart).trim();

        StringBuilder valuesBuilder = new StringBuilder();
        boolean hasRecords = false;
        int recordCount = 0;

        // 处理数组/纯数组类型
        if (paramNode.getParamType() == ParamType.ARRAY || paramNode.getParamType() == ParamType.PURE_ARRAY) {
            if (paramNode.getChildren() != null) {
                for (ParamTreeNode childNode : paramNode.getChildren()) {
                    // 只处理对象类型的子节点，且对象必须包含字段信息
                    if (childNode.getParamType() == ParamType.OBJECT && childNode.getChildren() != null && !childNode.getChildren().isEmpty()) {
                        hasRecords = true;
                        // 替换当前记录的占位符
                        String recordValues = valueTemplate;
                        for (ParamTreeNode field : childNode.getChildren()) {
                            String formattedValue = formatValue(field.getParamValue());
                            recordValues = recordValues.replace("{" + field.getParamKey() + "}", formattedValue);
                        }
                        // 添加分隔符
                        if (recordCount > 0) {
                            valuesBuilder.append(", ");
                        }
                        valuesBuilder.append(recordValues);
                        recordCount++;
                    }
                }
            }
        }
        // 处理对象类型
        else if (paramNode.getParamType() == ParamType.OBJECT) {
            if (paramNode.getChildren() != null && !paramNode.getChildren().isEmpty()) {
                hasRecords = true;
                // 替换单个对象的占位符
                String recordValues = valueTemplate;
                for (ParamTreeNode field : paramNode.getChildren()) {
                    String formattedValue = formatValue(field.getParamValue());
                    recordValues = recordValues.replace("{" + field.getParamKey() + "}", formattedValue);
                }
                valuesBuilder.append(recordValues);
            }
        }
        // 不支持的类型
        else {
            throw new RuntimeException("不支持的参数类型：" + paramNode.getParamType());
        }

        // 没有记录时返回空字符串
        return hasRecords ? prefix + " " + valuesBuilder.toString() : "";
    }

    /**
     * 根据值的类型格式化SQL中的值
     */
    private String formatValue(Object value) {
        if (value == null) {
            return "NULL";
        }
        if (value instanceof String || value instanceof Character) {
            // 转义单引号，防止SQL注入
            return "'" + value.toString().replace("'", "''") + "'";
        }
        // 数字类型直接返回字符串表示
        return value.toString();
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
        return null;
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
