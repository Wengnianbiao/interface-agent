package com.helianhealth.agent.remote.resolver;

import com.helianhealth.agent.enums.MappingType;
import com.helianhealth.agent.model.domain.NodeParamConfigDO;

import java.util.Map;

/**
 * 值解析器策略接口
 */
public interface ValueResolver {

    Object resolveValue(NodeParamConfigDO config, Map<String, Object> businessData, Map<String, Object> sourceBusinessData);

    boolean supports(MappingType mappingType);
}
