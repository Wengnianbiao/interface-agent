package com.helianhealth.agent.schedule;

import com.helianhealth.agent.enums.ScheduleParamSourceType;
import com.helianhealth.agent.exception.WorkflowBusinessException;
import com.helianhealth.agent.model.domain.InterfaceWorkflowNodeDO;
import com.helianhealth.agent.service.InterfaceWorkflowNodeService;
import com.helianhealth.agent.utils.ExpressionMapperUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 工作流引擎调度器
 * 负责编排整个工作流
 * 对于工作流整个处理都是线性的，但节点之间是可以并行的
 */
@Component
@Slf4j
@AllArgsConstructor
public class WorkFlowEngineScheduler {

    private final InterfaceWorkflowNodeService flowNodeService;
    private final BusinessDataPostProcessor businessDataPostProcessor;

    /**
     * 工作流调度
     * @param flowNodes 待调度节点
     * @param businessData 业务数据,业务数据一定是Map<String, Object>结构,而且这个Object得是JSON格式
     * @return 统一出参，出参怎么适配是工作流节点自己的事情，你只需要帮我处理好返回统一出参即可
     */
    public Map<String, Object> schedule(List<Integer> flowNodes, Map<String, Object> businessData) {
        Map<String, Object> flowNodeResponse;
        if (flowNodes != null && flowNodes.size() == 1) {
            InterfaceWorkflowNodeDO flowNode = flowNodeService.selectByNodeId(flowNodes.get(0));
            // 单节点处理
            flowNodeResponse = processSingleNode(flowNode, businessData);
        } else if (flowNodes != null && flowNodes.size() > 1) {
            // 多个节点,则说明这是个并行业务,譬如申请单,节点工作完成后需要有个聚合操作
            flowNodeResponse = processParallelNode(flowNodes, businessData);
        } else {
            log.error(WorkflowBusinessException.FLOW_NOT_FOUND.getMessage());
            throw WorkflowBusinessException.FLOW_NOT_FOUND.toException();
        }
        return flowNodeResponse;
    }

    private Map<String, Object> processSingleNode(InterfaceWorkflowNodeDO flowNode, Map<String, Object> businessData) {
        // 1、根据规则表达式对入参进行预处理
        Map<String, Object> businessDataAfterPost = businessDataPostProcessor.postParamProcessor(
                flowNode.getParamFilterExpr(), businessData);
        if (businessDataAfterPost == null || businessDataAfterPost.isEmpty()) {
            // 经过过滤后如果为Null就不进行处理
            return new HashMap<>();
        }
        //  todo 单节点可能需要拆分为多节点调用,e.g.入参为列表但是接口只支持对象

        // 2、执行节点
        Map<String, Object> nodeResponse = flowNodeService.executeFlowNode(flowNode, businessDataAfterPost);

        // 3、根据表达式调度下一个节点
        List<Integer> nextNodes = determineNextNode(flowNode.getScheduleExpr(), nodeResponse);

        if (nextNodes == null || nextNodes.isEmpty()) {
            return nodeResponse;
        }

        // 4、调度节点的入参来源
        Map<String, Object> nextNodeParam = determineNextNodeParam(flowNode.getScheduleParamSourceType(),
                businessData, nodeResponse);

        return schedule(nextNodes, nextNodeParam);
    }

    private Map<String, Object> determineNextNodeParam(ScheduleParamSourceType paramSourceType,
                                                       Map<String, Object> businessData,
                                                       Map<String, Object> preFlowNodeResponse) {

        if (Objects.requireNonNull(paramSourceType) == ScheduleParamSourceType.ORIGINAL) {
            return businessData;
        }
        return preFlowNodeResponse;
    }

    @SuppressWarnings("unchecked")
    private List<Integer> determineNextNode(String scheduleExpr, Map<String, Object> preFlowNodeResponse) {
        if (StringUtils.isEmpty(scheduleExpr)) {
            return null;
        }
        // 根据表达式获取下个流转节点,一定是根据上个节点的调用结果去处理的
        // 并且规定流转的结果是List<Integer>或者为null
        // 这里会在页面后台进行配置不会涉及到人工导致的错误
        Object parser = ExpressionMapperUtils.parser(scheduleExpr, preFlowNodeResponse, null);

        if (parser != null && !(parser instanceof List)) {
            log.error("表达式解析结果类型错误，期望List<Integer>，实际类型: {}", parser.getClass().getSimpleName());
            throw WorkflowBusinessException.EXPRESSION_PARSING_ERROR.toException();
        }

        if (parser != null) {
            List<?> resultList = (List<?>) parser;
            for (Object item : resultList) {
                if (!(item instanceof Integer)) {
                    log.error("表达式解析结果元素类型错误，Integer，实际类型: {}", item != null ? item.getClass().getSimpleName() : "null");
                    throw WorkflowBusinessException.EXPRESSION_PARSING_ERROR.toException();
                }
            }
        }

        return (List<Integer>) parser;
    }

    private Map<String, Object> processParallelNode(List<Integer> nodeIds, Map<String, Object> businessData) {
        // 1. 收集所有并行节点的处理结果
        List<Map<String, Object>> nodeResultList = new ArrayList<>();
        nodeIds.forEach(nodeId -> {
            InterfaceWorkflowNodeDO flowNode = flowNodeService.selectByNodeId(nodeId);
            Map<String, Object> nodeResult = processSingleNode(flowNode, businessData);
            if (nodeResult == null || nodeResult.isEmpty()) {
                return;
            }
            nodeResultList.add(nodeResult);
        });

        // 2. 合并所有结果：相同key的值合并（支持List合并，非List类型转为List）
        return mergeResults(nodeResultList);
    }

    /**
     * 核心合并逻辑：递归处理嵌套结构，只有当key对应的值是List类型时才合并为List，否则只保留第一个节点的值
     * @param resultList 所有节点的结果列表
     * @return 合并后的Map
     */
    private Map<String, Object> mergeResults(List<Map<String, Object>> resultList) {
        Map<String, Object> merged = new HashMap<>();

        for (Map<String, Object> nodeResult : resultList) {
            mergeMap(nodeResult, merged);
        }

        return merged;
    }

    /**
     * 递归合并两个Map，处理嵌套结构
     * @param source 源Map
     * @param target 目标Map
     */
    private void mergeMap(Map<String, Object> source, Map<String, Object> target) {
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (target.containsKey(key)) {
                Object existingValue = target.get(key);
                // 如果现有值是List，则合并
                if (existingValue instanceof List) {
                    List<Object> mergedValue = mergeValues(existingValue, value);
                    target.put(key, mergedValue);
                } else if (existingValue instanceof Map && value instanceof Map) {
                    // 如果都是Map，则递归合并
                    mergeMap((Map<String, Object>) value, (Map<String, Object>) existingValue);
                }
                // 如果现有值不是List也不是Map，则保持不变（即不覆盖）
            } else {
                // 新key：直接放入，不需要特殊处理
                target.put(key, value);
            }
        }
    }

    /**
     * 合并两个值为List（处理原值和新值是否为List的情况）
     */
    private List<Object> mergeValues(Object existingValue, Object newValue) {
        List<Object> merged = new ArrayList<>();

        // 添加已存在的值
        if (existingValue instanceof List) {
            merged.addAll((List<?>) existingValue);
        } else {
            merged.add(existingValue);
        }

        // 添加新值
        if (newValue instanceof List) {
            merged.addAll((List<?>) newValue);
        } else {
            merged.add(newValue);
        }

        return merged;
    }

}
