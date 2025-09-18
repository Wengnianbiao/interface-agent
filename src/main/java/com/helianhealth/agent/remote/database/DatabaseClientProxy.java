package com.helianhealth.agent.remote.database;

import com.alibaba.fastjson2.JSONArray;
import com.helianhealth.agent.enums.MappingSource;
import com.helianhealth.agent.enums.MappingType;
import com.helianhealth.agent.enums.ParamType;
import com.helianhealth.agent.mapper.agent.NodeParamConfigMapper;
import com.helianhealth.agent.model.domain.InterfaceWorkflowNodeDO;
import com.helianhealth.agent.model.domain.NodeParamConfigDO;
import com.helianhealth.agent.model.dto.ParamTreeNode;
import com.helianhealth.agent.remote.AbstractClientProxy;
import com.helianhealth.agent.utils.JsonUtils;
import com.helianhealth.agent.utils.ParamNodeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class DatabaseClientProxy extends AbstractClientProxy {

    private final DatabaseSqlHandler sqlHandler;

    public DatabaseClientProxy(NodeParamConfigMapper nodeMapper, DatabaseSqlHandler sqlHandler) {
        super(nodeMapper);
        this.sqlHandler = sqlHandler;
    }

    @Override
    public Map<String, Object> doInvoke(InterfaceWorkflowNodeDO flowNode, List<ParamTreeNode> params) {
        try {
            // 构建SQL查询语句
            String sql = sqlHandler.resolveParamNodes(flowNode, params);
            // 执行SQL查询并转化为Map
            return sqlHandler.invokeSqlAndConvertResult(flowNode, sql);
        } catch (Exception e) {
            log.error("数据库调用失败", e);
            throw new RuntimeException("数据库调用失败: " + e.getMessage(), e);
        }
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
                getSourceValue(config.getSourceParamKey(), rootBusinessData) :
                getSourceValue(config.getSourceParamKey(), businessData);

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

    @SuppressWarnings("unchecked")
    private Object getSourceValue(String sourceParamKey, Map<String, Object> businessData) {
        if (businessData == null || StringUtils.isEmpty(sourceParamKey)) {
            return null;
        }
        String[] keys = sourceParamKey.split("\\.");
        Object current = businessData;

        for (String key : keys) {
            if (!(current instanceof Map)) {
                return null; // 非 Map 类型无法继续深入
            }

            Map<String, Object> currentMap = (Map<String, Object>) current;
            current = currentMap.get(key);
            if (current == null) {
                return null;
            }
        }

        return current;
    }
}
