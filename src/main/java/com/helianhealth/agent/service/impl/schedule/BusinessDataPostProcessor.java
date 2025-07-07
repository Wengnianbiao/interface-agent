package com.helianhealth.agent.service.impl.schedule;

import com.helianhealth.agent.utils.ExpressionMapperUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;

@Component
@Slf4j
public class BusinessDataPostProcessor {

    public Map<String, Object> postParamProcessor(String paramFilterExpr, Map<String, Object> businessData) {
        if (StringUtils.isEmpty(paramFilterExpr)) {
            return businessData;
        }
        return ExpressionMapperUtils.businessDataFilter(paramFilterExpr, businessData);
    }
}
