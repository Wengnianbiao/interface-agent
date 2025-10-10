package com.helianhealth.agent.remote.database;

import com.helianhealth.agent.enums.MappingType;
import com.helianhealth.agent.enums.ParamType;
import com.helianhealth.agent.model.domain.InterfaceWorkflowNodeDO;
import com.helianhealth.agent.model.domain.NodeParamConfigDO;
import com.helianhealth.agent.model.dto.ParamTreeNode;
import com.helianhealth.agent.remote.AbstractClientProxy;
import com.helianhealth.agent.remote.ProxyConvertHelper;
import com.helianhealth.agent.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class DatabaseClientProxy extends AbstractClientProxy {

    @Autowired
    private DatabaseSqlHandler databaseSqlHandler;

    @Autowired
    private ProxyConvertHelper proxyConvertHelper;

    @Override
    public Map<String, Object> doInvoke(InterfaceWorkflowNodeDO flowNode, List<ParamTreeNode> params) {
        try {
            // 构建SQL语句
            String sql = databaseSqlHandler.resolveParamNodes(flowNode, params);
            // 执行SQL并转化为Map
            return databaseSqlHandler.invokeSqlAndConvertResult(flowNode, sql);
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

    /**
     * database模式下的数组构建规则会存在直接映射的场景
     * @param config 节点参数配置
     * @param allNodes 所有节点参数配置
     * @param businessData 业务数据
     * @param rootBusinessData 根业务数据
     * @param node 当前节点
     */
    private void doProcessArrayNodeType(NodeParamConfigDO config, List<NodeParamConfigDO> allNodes, Map<String, Object> businessData, Map<String, Object> rootBusinessData, ParamTreeNode node) {
        // 根据映射源进入迭代
        Object sourceValue = proxyConvertHelper.convertSourceValue(config, businessData, rootBusinessData);

        if (config.getSourceParamType() == ParamType.OBJECT && sourceValue != null) {
            // 源参数是Object，包装成大小为1的数组
            List<ParamTreeNode> arrayChildren = buildParamTree(allNodes,
                    config.getConfigId(),
                    JsonUtils.toMap(sourceValue),
                    rootBusinessData);
            node.setChildren(arrayChildren);
        } else if (config.getSourceParamType() == ParamType.ARRAY ||
                config.getSourceParamType() == ParamType.PURE_ARRAY
                && sourceValue != null) {
            // 直接映射的场景直接赋值
            if (config.getMappingType().equals(MappingType.DIRECT)) {
                node.setParamValue(sourceValue);
            } else {
                List<ParamTreeNode> allArrayChildren = new ArrayList<>();
                if (sourceValue instanceof List) {
                    List<?> sourceList = (List<?>) sourceValue;
                    for (Object item : sourceList) {
                        List<ParamTreeNode> arrayChildren = buildParamTree(allNodes,
                                config.getConfigId(),
                                JsonUtils.toMap(item),
                                rootBusinessData);
                        allArrayChildren.addAll(arrayChildren);
                    }
                }

                node.setChildren(allArrayChildren);
            }
        } else {
            // 其他情况创建空数组
            node.setChildren(new ArrayList<>());
        }
    }
}
