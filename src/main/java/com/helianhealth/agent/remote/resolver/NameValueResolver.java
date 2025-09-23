package com.helianhealth.agent.remote.resolver;

import com.helianhealth.agent.enums.MappingType;
import com.helianhealth.agent.model.domain.NodeParamConfigDO;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class NameValueResolver implements ValueResolver {
    @Override
    public Object resolveValue(NodeParamConfigDO config, Map<String, Object> businessData, Map<String, Object> sourceBusinessData) {
        return businessData.get(config.getSourceParamKey());
    }

    @Override
    public boolean supports(MappingType mappingType) {
        return MappingType.NAME == mappingType;
    }
}
