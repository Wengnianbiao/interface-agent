package com.helianhealth.agent.remote.resolver;

import com.helianhealth.agent.model.domain.NodeParamConfigDO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ValueResolveService {
    private final List<ValueResolver> resolvers;

    public ValueResolveService(List<ValueResolver> resolvers) {
        this.resolvers = resolvers;
    }

    public Object resolveValue(NodeParamConfigDO config, Map<String, Object> businessData, Map<String, Object> sourceBusinessData) {
        return resolvers.stream()
                .filter(resolver -> resolver.supports(config.getMappingType()))
                .findFirst()
                .map(resolver -> resolver.resolveValue(config, businessData, sourceBusinessData))
                .orElse(null);
    }
}
