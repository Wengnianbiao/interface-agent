package com.helianhealth.agent.remote.database;

import com.helianhealth.agent.enums.OperationType;
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
        return buildSelectSql(tableName, params);
    }

    /**
     * 构建SQL查询语句，直接将参数值拼接到SQL中
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
                        // LIKE操作需要处理通配符
                        sql.append(paramKey).append(" LIKE '").append((paramValue)).append("' AND ");
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
                                sql.append("'").append((valueList.get(i))).append("'");
                            }
                            sql.append(") AND ");
                        } else {
                            sql.append(paramKey).append(" ").append(operationType.getOperator()).append(" ('")
                                    .append((paramValue)).append("') AND ");
                        }
                        break;
                    case IS_NULL:
                    case IS_NOT_NULL:
                        // IS NULL和IS NOT NULL不需要参数值
                        sql.append(paramKey).append(" ").append(operationType.getOperator()).append(" AND ");
                        break;
                    default:
                        // 其他操作符使用标准格式
                        sql.append(paramKey).append(" ").append(operationType.getOperator()).append(" '")
                                .append((paramValue)).append("' AND ");
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
            rs = ps.executeQuery();
            return convertResultSetToMap(rs);
        } catch (SQLException e) {
            throw new RuntimeException("SQL执行失败: " + e.getMessage(), e);
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
     * 将结果集转换为JSON字符串
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
