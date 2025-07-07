package com.helianhealth.agent.remote.database;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.helianhealth.agent.enums.MappingSource;
import com.helianhealth.agent.enums.MappingType;
import com.helianhealth.agent.enums.OperationType;
import com.helianhealth.agent.enums.ParamType;
import com.helianhealth.agent.exception.InvokeBusinessException;
import com.helianhealth.agent.mapper.agent.NodeParamConfigMapper;
import com.helianhealth.agent.model.domain.InterfaceWorkflowNodeDO;
import com.helianhealth.agent.model.domain.NodeParamConfigDO;
import com.helianhealth.agent.model.dto.ParamTreeNode;
import com.helianhealth.agent.remote.AbstractClientProxy;
import com.helianhealth.agent.utils.JsonUtils;
import com.helianhealth.agent.utils.ParamNodeUtils;
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
@Slf4j
public class DatabaseClientProxy extends AbstractClientProxy {

    private final DatabaseApiClientManager clientManager;

    public DatabaseClientProxy(NodeParamConfigMapper nodeMapper, DatabaseApiClientManager clientManager) {
        super(nodeMapper);
        this.clientManager = clientManager;
    }

    @Override
    public Map<String, Object> doInvoke(InterfaceWorkflowNodeDO flowNode, List<ParamTreeNode> params) {
        // 数据库中的数据的处理
        Map<String, Object> metaInfo = JsonUtils.toMap(flowNode.getMetaInfo());
        String viewName = (String) metaInfo.get("viewName");
        String sql = buildSelectSql(viewName, params);

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = clientManager.getConnection(flowNode.getMetaInfo());
            stmt = conn.prepareStatement(sql);

            int paramIndex = 1;
            for (ParamTreeNode param : params) {
                Object value = param.getParamValue();
                OperationType operationType = param.getOperationType();

                if (value == null || operationType == null) {
                    continue;
                }

                switch (operationType) {
                    case LIKE:
                        stmt.setObject(paramIndex++, "%" + value + "%");
                        break;
                    case IN:
                    case NOT_IN:
                        if (value instanceof List) {
                            List<?> list = (List<?>) value;
                            StringBuilder inClause = new StringBuilder();
                            for (int i = 0; i < list.size(); i++) {
                                if (i > 0) {
                                    inClause.append(",");
                                }
                                inClause.append("?");
                                stmt.setObject(paramIndex++, list.get(i));
                            }
                            String sqlString = stmt.toString();
                            sqlString = sqlString.replaceFirst("\\?", inClause.toString());
                            // 先关闭原来的stmt
                            stmt.close();
                            // 重新创建PreparedStatement
                            stmt = conn.prepareStatement(sqlString);
                        }
                        break;
                    case IS_NULL:
                    case IS_NOT_NULL:
                        break;
                    default:
                        stmt.setObject(paramIndex++, value);
                }
            }

            rs = stmt.executeQuery();
            return convertResultSetToMap(rs);
        } catch (Exception e) {
            log.error("数据库查询异常", e);
            throw InvokeBusinessException.DATABASE_EXECUTE_ERROR.toException();
        } finally {
            // 手动关闭资源，避免内存泄漏
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (SQLException ex) {
                log.error("关闭数据库资源异常", ex);
            }
        }
    }

    /**
     * 构建SQL查询语句
     */
    /**
     * 构建SQL查询语句，使用节点的operationType作为条件操作符
     */
    private String buildSelectSql(String viewName, List<ParamTreeNode> params) {
        StringBuilder sql = new StringBuilder("SELECT * FROM ").append(viewName);

        // 过滤出有值且有操作类型的参数节点
        List<ParamTreeNode> validParams = params.stream()
                .filter(node -> node.getParamValue() != null)
                .filter(node -> node.getOperationType() != null)
                .collect(Collectors.toList());

        if (!validParams.isEmpty()) {
            sql.append(" WHERE ");

            for (ParamTreeNode param : validParams) {
                String paramKey = param.getParamKey();
                OperationType operationType = param.getOperationType();

                // 根据不同的操作类型添加相应的条件表达式
                switch (operationType) {
                    case LIKE:
                        // LIKE操作需要处理通配符
                        sql.append(paramKey).append(" LIKE ? AND ");
                        break;
                    case IN:
                    case NOT_IN:
                        // IN和NOT IN需要处理值列表
                        sql.append(paramKey).append(" ").append(operationType.getOperator()).append(" (?) AND ");
                        break;
                    case IS_NULL:
                    case IS_NOT_NULL:
                        // IS NULL和IS NOT NULL不需要参数值
                        sql.append(paramKey).append(" ").append(operationType.getOperator()).append(" AND ");
                        break;
                    default:
                        // 其他操作符使用标准格式
                        sql.append(paramKey).append(" ").append(operationType.getOperator()).append(" ? AND ");
                }
            }

            // 移除最后的 " AND "
            sql.delete(sql.length() - 5, sql.length());
        }

        return sql.toString();
    }

    /**
     * 将结果集转换为JSON字符串
     */
    private Map<String, Object> convertResultSetToMap(ResultSet rs) throws SQLException {
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

    @Override
    public void processObjectNodeType(NodeParamConfigDO config, List<NodeParamConfigDO> allNodes, Map<String, Object> businessData, Map<String, Object> rootBusinessData, ParamTreeNode node) {
        node.setChildren(buildParamTree(allNodes,
                config.getConfigId(),
                processSourceParamWhenTargetIsObject(config, businessData),
                rootBusinessData));
    }

    private Map<String, Object> processSourceParamWhenTargetIsObject(NodeParamConfigDO config, Map<String, Object> businessData) {
        ParamType sourceParamType = config.getSourceParamType();
        Object sourceValue = businessData.get(config.getSourceParamKey());
        switch(sourceParamType) {
            case NONE:
                return businessData;
            // 若为对象直接返回
            case OBJECT:
                return JsonUtils.toMap(sourceValue);
            // 若为数组则需要转换成当前数组的首个索引对应的对象的key-value格式
            // 默认获取索引为0的元素
            case ARRAY:
                if (sourceValue == null) {
                    return null;
                }
                if (!(sourceValue instanceof List)) {
                    throw new IllegalArgumentException("Expected List for ARRAY type, but got: " + sourceValue.getClass().getSimpleName());
                }

                List<?> list = (List<?>) sourceValue;
                if (list.isEmpty()) {
                    return new HashMap<>();
                }

                Object firstItem = list.get(0);
                if (!(firstItem instanceof Map)) {
                    throw new IllegalArgumentException("First element of array must be a Map, but got: " + firstItem.getClass().getSimpleName());
                }

                // 安全转换并获取第一个元素
                return JsonUtils.toMap(list.get(0));
            default:
                return new HashMap<>();
        }
    }

    @Override
    public void processArrayNodeType(NodeParamConfigDO config, List<NodeParamConfigDO> allNodes, Map<String, Object> businessData, Map<String, Object> rootBusinessData, ParamTreeNode node) {
        doProcessArrayNodeType(config, allNodes, businessData, rootBusinessData, node);
    }

    private void doProcessArrayNodeType(NodeParamConfigDO config, List<NodeParamConfigDO> allNodes, Map<String, Object> businessData, Map<String, Object> rootBusinessData, ParamTreeNode node) {
        // 根据映射源进入迭代
        Object sourceValue = config.getMappingSource() == MappingSource.INPUT ?
                rootBusinessData.get(config.getSourceParamKey()) :
                businessData.get(config.getSourceParamKey());

        JSONArray jsonArray = new JSONArray();

        if (config.getSourceParamType() == ParamType.OBJECT && sourceValue != null) {
            // 情况1: 源参数是Object，包装成大小为1的数组
            List<ParamTreeNode> arrayChildren = buildParamTree(allNodes,
                    config.getConfigId(),
                    JsonUtils.toMap(sourceValue),
                    rootBusinessData);
            jsonArray.add(ParamNodeUtils.convertNodesToMap(arrayChildren));
            node.setChildren(arrayChildren);
        } else if (config.getSourceParamType() == ParamType.ARRAY && sourceValue instanceof List) {
            // 情况2: 源参数是数组，需要处理数组中的每个元素
            List<?> sourceList = (List<?>) sourceValue;
            // 如果是直连映射，也就是数组元素是基本数据类型就直接映射
            if (config.getMappingType().equals(MappingType.DIRECT)) {
                node.setParamValue(sourceList);
            } else {
                for (Object item : sourceList) {
                    // 如果数组是一个对象数组配置的时候需要增加一个虚拟节点!
                    List<ParamTreeNode> arrayChildren = buildParamTree(allNodes,
                            config.getConfigId(),
                            JsonUtils.toMap(item),
                            rootBusinessData);
                    jsonArray.add(ParamNodeUtils.convertNodesToMap(arrayChildren));
                }
                // 数组是嵌套结构只能通过value去保持这种嵌套结构
                node.setParamValue(jsonArray);
            }
        } else {
            // 兼容单个Object转化成List的场景
            List<ParamTreeNode> paramNodeDTOS = buildParamTree(allNodes,
                    config.getConfigId(),
                    businessData,
                    rootBusinessData);
            jsonArray.add(ParamNodeUtils.convertNodesToMap(paramNodeDTOS));
            node.setParamValue(jsonArray);
        }
    }
}
