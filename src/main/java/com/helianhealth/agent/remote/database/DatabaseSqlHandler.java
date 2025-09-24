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

    private String buildDeleteSql(String sqlTemplate, List<ParamTreeNode> params) {
        String finalSql = sqlTemplate;

        // 遍历所有参数节点
        for (ParamTreeNode param : params) {
            // 只处理有参数值的节点
            if (param.getParamValue() != null) {
                String formattedValue = formatValue(param.getParamValue());
                finalSql = finalSql.replace("{" + param.getParamKey() + "}", formattedValue);
            }
        }

        // 检查是否还有未替换的占位符
        if (finalSql.contains("{") && finalSql.contains("}")) {
            throw DatabaseRemoteBusinessException.DATABASE_PLACE_HOLDER_ERROR.toException();
        }
        return finalSql;
    }

    /**
     * 构建INSERT SQL语句(接口场景只存在对象和数组类型参数,单个数据的Insert不存在吧)
     */
    private String buildInsertSql(String sqlTemplate, ParamTreeNode paramNode) {
        // 检查是否包含ON DUPLICATE KEY UPDATE部分
        int onUpdateIndex = sqlTemplate.toUpperCase().indexOf("ON DUPLICATE KEY UPDATE");
        String valuesPart;
        String updatePart = "";

        if (onUpdateIndex != -1) {
            // 分离VALUES部分和ON DUPLICATE KEY UPDATE部分
            valuesPart = sqlTemplate.substring(0, onUpdateIndex).trim();
            updatePart = sqlTemplate.substring(onUpdateIndex);
        } else {
            // 没有ON DUPLICATE KEY UPDATE部分，直接使用整个模板
            valuesPart = sqlTemplate;
        }

        // 提取VALUES部分中的模板
        int valuesStart = valuesPart.indexOf("VALUES") + "VALUES".length();
        String valueTemplate = valuesPart.substring(valuesStart).trim();
        String prefix = valuesPart.substring(0, valuesStart).trim();

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
        } else if (paramNode.getParamType() == ParamType.OBJECT) {
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
        String result = hasRecords ? prefix + " " + valuesBuilder : "";

        // 如果存在ON DUPLICATE KEY UPDATE部分，直接附加到结果后面
        if (!updatePart.isEmpty()) {
            result += " " + updatePart;
        }

        // 检查是否还有未替换的占位符
        if (result.contains("{") && result.contains("}")) {
            throw DatabaseRemoteBusinessException.DATABASE_PLACE_HOLDER_ERROR.toException();
        }

        return result;
    }

    /**
     * 构建SQL查询语句
     */
    private String buildSelectSql(String sqlTemplate, List<ParamTreeNode> params) {
        String finalSql = sqlTemplate;

        for (ParamTreeNode param : params) {
            String formattedValue;
            // 处理数组类型参数，用于IN语句
            if (param.getParamType() == ParamType.ARRAY || param.getParamType() == ParamType.PURE_ARRAY) {
                formattedValue = formatArrayValue(param.getParamValue());
            } else {
                // 处理普通类型参数
                formattedValue = formatValue(param.getParamValue());
            }
            finalSql = finalSql.replace("{" + param.getParamKey() + "}", formattedValue);
        }

        // 检查是否还有未替换的占位符
        if (finalSql.contains("{") && finalSql.contains("}")) {
            throw DatabaseRemoteBusinessException.DATABASE_PLACE_HOLDER_ERROR.toException();
        }

        return finalSql;
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
     * 数组格式处理,只会在in语句中出现
     * 将数组格式 ["117","102","94","87"] 转换为 SQL 的 ('117','102','94','87') 格式
     */
    private String formatArrayValue(Object value) {
        if (value == null) {
            return "NULL";
        }

        // 直接将字符串形式的数组转换为SQL格式
        String stringValue = value.toString();

        // 移除方括号并添加圆括号，将双引号转换为单引号
        return stringValue
                .replace("[", "(")
                .replace("]", ")")
                .replace("\"", "'");
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
