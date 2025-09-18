package com.helianhealth.agent.utils;

import com.helianhealth.agent.mapper.hems.HemsMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * SpEL表达式解析工具类
 */
@Component
public class ExpressionMapperUtils implements ApplicationContextAware {

    private static final SpelExpressionParser PARSER = new SpelExpressionParser();

    private static ApplicationContext applicationContext;

    private static final Logger logger = LoggerFactory.getLogger(ExpressionMapperUtils.class);

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        ExpressionMapperUtils.applicationContext = applicationContext;
    }


    /**
     * 解析表达式并返回结果
     * @param expression SpEL表达式（如："#data.idCard == null ? #data.phone + 'hl' : #data.idCard"）
     * @param businessData 业务数据Map（键为字段名，值为字段值）
     * @return 表达式执行结果
     */
    public static Object parser(String expression, Object businessData, Map<String, Object> sourceBusinessData) {
        try {
            // 创建表达式上下文，将业务数据放入上下文（变量名为"data"）
            StandardEvaluationContext context = new StandardEvaluationContext();
            context.setVariable("data", businessData);
            context.setVariable("source", sourceBusinessData);

            // 解析并执行表达式
            return PARSER.parseExpression(expression).getValue(context);
        } catch (Exception e) {
            logger.error("表达式[{}]解析异常：", expression, e);
            return null;
        }
    }

    /**
     * 业务数据过滤处理
     * @param expression 过滤表达式
     * @param businessData 业务数据
     * @return 过滤后的业务数据
     */
    public static Map<String, Object> businessDataFilter(String expression, Map<String, Object> businessData) {
        try {
            if (businessData == null) {
                return null;
            }
            // 过滤操作必须是通过创建深度复制的Map来进行, 因为SpEL表达式执行过程中会修改原始Map中的数据
            Map<String, Object> deepCopyMap = JsonUtils.deepCopyMap(businessData);

            StandardEvaluationContext context = new StandardEvaluationContext();
            context.setVariable("data", deepCopyMap);
            PARSER.parseExpression(expression).getValue(context);

            return deepCopyMap;
        } catch (Exception e) {
            logger.error("表达式[{}]解析异常：", expression, e);
            return null;
        }
    }

    /**
     * 解析表达式并返回结果（支持访问Spring Bean）
     * @param expression SpEL表达式（如："@interfaceFlowNodeMapper.selectByNodeId(#nodeId)"）
     * @param businessData 业务数据Map（键为字段名，值为字段值）
     * @return 表达式执行结果
     */
    public static Object parserWithBeanAccess(String expression, Object businessData, Map<String, Object> sourceBusinessData) {
        try {
            // 创建表达式上下文，将业务数据放入上下文
            StandardEvaluationContext context = new StandardEvaluationContext();
            context.setVariable("data", businessData);
            context.setVariable("source", sourceBusinessData);

            // 默认将HemsMapper注入context
            if (applicationContext != null) {
                HemsMapper hemsMapper = applicationContext.getBean(HemsMapper.class);
                context.setVariable("hemsMapper", hemsMapper);
            }

            // 解析并执行表达式
            return PARSER.parseExpression(expression).getValue(context);
        } catch (Exception e) {
            logger.error("表达式[{}]解析异常：", expression, e);
            return null;
        }
    }
}
