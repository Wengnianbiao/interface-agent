package com.helianhealth.agent.remote.resolver;

import com.helianhealth.agent.enums.MappingType;
import com.helianhealth.agent.model.domain.NodeParamConfigDO;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class SingleMapResolveService implements ValueResolver {
    @Override
    public Object resolveValue(NodeParamConfigDO config, Map<String, Object> businessData, Map<String, Object> sourceBusinessData) {
        // 返回businessData中第一个元素的value
        if (businessData != null && !businessData.isEmpty()) {
            return businessData.values().iterator().next();
        }
        return null;
    }

    @Override
    public boolean supports(MappingType mappingType) {
        return MappingType.SINGLE_MAP == mappingType;
    }
}
