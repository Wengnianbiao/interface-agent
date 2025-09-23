package com.helianhealth.agent.remote.resolver;

import com.helianhealth.agent.enums.MappingType;
import com.helianhealth.agent.model.domain.NodeParamConfigDO;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ConstantValueResolver implements ValueResolver {
    @Override
    public Object resolveValue(NodeParamConfigDO config, Map<String, Object> businessData, Map<String, Object> sourceBusinessData) {
        return config.getMappingRule();
    }

    @Override
    public boolean supports(MappingType mappingType) {
        return MappingType.CONSTANT == mappingType;
    }
}
